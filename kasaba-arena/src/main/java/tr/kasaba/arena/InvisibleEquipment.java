package tr.kasaba.arena;

import io.netty.channel.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/** Masks outgoing equipment, including tracking bundles; the actual inventory is never changed. */
final class InvisibleEquipment implements Listener, AutoCloseable {
    private static final String HANDLER="kasaba_x_equipment";
    private final JavaPlugin plugin;
    private final Predicate<Player> active;
    private final Set<Integer> hidden=ConcurrentHashMap.newKeySet();
    private final Map<UUID,Integer> actors=new HashMap<>();
    private final Map<UUID,Channel> channels=new HashMap<>();
    private final Class<?> equipment,bundle;
    private final Method entity,slots,first,pair,subPackets;
    private final Constructor<?> equipmentConstructor,bundleConstructor;
    private final Object empty;

    InvisibleEquipment(JavaPlugin plugin,Predicate<Player> active) {
        this.plugin=plugin;this.active=active;
        try {
            equipment=Class.forName("net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket");
            bundle=Class.forName("net.minecraft.network.protocol.game.ClientboundBundlePacket");
            Class<?> pairClass=Class.forName("com.mojang.datafixers.util.Pair");
            entity=equipment.getMethod("getEntity");slots=equipment.getMethod("getSlots");
            first=pairClass.getMethod("getFirst");pair=pairClass.getMethod("of",Object.class,Object.class);
            subPackets=bundle.getMethod("subPackets");
            equipmentConstructor=equipment.getConstructor(int.class,List.class);
            bundleConstructor=bundle.getConstructor(Iterable.class);
            empty=Class.forName("net.minecraft.world.item.ItemStack").getField("EMPTY").get(null);
        }catch(ReflectiveOperationException e){throw new IllegalStateException("X Arena ekipman gizleme Paper 1.21.10 üzerinde başlatılamadı",e);}
        Bukkit.getPluginManager().registerEvents(this,plugin);
        for(Player p:Bukkit.getOnlinePlayers())install(p);
        Bukkit.getScheduler().runTaskTimer(plugin,this::refresh,1,2);
    }
    private static Object field(Object object,String name)throws ReflectiveOperationException {
        for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(name);
    }
    private void install(Player viewer) {
        try {
            Object handle=viewer.getClass().getMethod("getHandle").invoke(viewer);
            Channel channel=(Channel)field(field(field(handle,"connection"),"connection"),"channel");
            channels.put(viewer.getUniqueId(),channel);int self=viewer.getEntityId();
            channel.eventLoop().execute(()->{if(channel.pipeline().get(HANDLER)!=null)channel.pipeline().remove(HANDLER);channel.pipeline().addBefore("packet_handler",HANDLER,new ChannelDuplexHandler(){
                @Override public void write(ChannelHandlerContext ctx,Object message,ChannelPromise promise)throws Exception{super.write(ctx,mask(message,self),promise);}
            });});
        }catch(ReflectiveOperationException|RuntimeException e){plugin.getLogger().log(java.util.logging.Level.SEVERE,"Ekipman gizleme bağlantısı kurulamadı: "+viewer.getName(),e);}
    }
    // Called only on a channel event loop. No Bukkit access here.
    private Object mask(Object message,int self)throws ReflectiveOperationException {
        if(hidden.isEmpty())return message;
        if(equipment.isInstance(message)){
            int id=(int)entity.invoke(message);if(id==self||!hidden.contains(id))return message;
            List<Object> replacement=new ArrayList<>();
            for(Object slot:(List<?>)slots.invoke(message))replacement.add(pair.invoke(null,first.invoke(slot),empty));
            return equipmentConstructor.newInstance(id,replacement);
        }
        if(bundle.isInstance(message)){
            List<Object> replacement=new ArrayList<>();boolean changed=false;
            for(Object packet:(Iterable<?>)subPackets.invoke(message)){Object masked=mask(packet,self);replacement.add(masked);changed|=masked!=packet;}
            if(changed)return bundleConstructor.newInstance(replacement);
        }
        return message;
    }
    private void refresh(){
        for(Player p:Bukkit.getOnlinePlayers()){
            boolean invisible=active.test(p)&&!p.isDead()&&(p.isInvisible()||p.hasPotionEffect(PotionEffectType.INVISIBILITY));
            if(invisible){if(actors.putIfAbsent(p.getUniqueId(),p.getEntityId())==null){hidden.add(p.getEntityId());send(p,true);}}
            else clear(p);
        }
    }
    private void send(Player actor,boolean invisible){
        Map<EquipmentSlot,ItemStack> gear=new EnumMap<>(EquipmentSlot.class);
        for(EquipmentSlot slot:List.of(EquipmentSlot.HAND,EquipmentSlot.OFF_HAND,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))
            gear.put(slot,invisible?new ItemStack(Material.AIR):actor.getInventory().getItem(slot).clone());
        for(Player viewer:actor.getWorld().getPlayers())if(viewer!=actor&&viewer.canSee(actor))viewer.sendEquipmentChange(actor,gear);
    }
    void clear(Player p){Integer id=actors.remove(p.getUniqueId());if(id!=null){hidden.remove(id);if(p.isOnline())send(p,false);}}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void potion(EntityPotionEffectEvent e){
        if(e.getEntity() instanceof Player p&&active.test(p)&&e.getNewEffect()!=null&&e.getNewEffect().getType().equals(PotionEffectType.INVISIBILITY)){
            actors.put(p.getUniqueId(),p.getEntityId());hidden.add(p.getEntityId());
            Bukkit.getScheduler().runTask(plugin,()->{if(p.isOnline())send(p,true);});
        }
    }
    @EventHandler public void join(PlayerJoinEvent e){install(e.getPlayer());}
    @EventHandler public void quit(PlayerQuitEvent e){clear(e.getPlayer());remove(e.getPlayer().getUniqueId());}
    private void remove(UUID id){Channel channel=channels.remove(id);if(channel!=null)channel.eventLoop().execute(()->{if(channel.pipeline().get(HANDLER)!=null)channel.pipeline().remove(HANDLER);});}
    @Override public void close(){for(Player p:Bukkit.getOnlinePlayers())clear(p);hidden.clear();for(UUID id:List.copyOf(channels.keySet()))remove(id);}
}

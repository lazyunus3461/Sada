package tr.kasabatr.vampir;

import io.netty.channel.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** Paper 1.21.10: masks outgoing hand equipment before encoding, including spawn bundles. */
final class EquipmentPrivacy implements AutoCloseable {
    private static final String HOOK="kasaba_vk_privacy",TEAM_PREFIX="vk_n_";
    private final KasabaVampirPlugin plugin;
    private final Map<Integer,Actor> actors=new ConcurrentHashMap<>();
    private final Set<String> names=ConcurrentHashMap.newKeySet();
    private final Map<UUID,Channel> channels=new ConcurrentHashMap<>();
    private final Class<?> equipment=type("net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket"),bundle=type("net.minecraft.network.protocol.game.ClientboundBundlePacket"),teamPacket=type("net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket"),packet=type("net.minecraft.network.protocol.Packet"),team=type("net.minecraft.world.scores.PlayerTeam"),scoreboard=type("net.minecraft.world.scores.Scoreboard"),visibility=type("net.minecraft.world.scores.Team$Visibility"),pair=type("com.mojang.datafixers.util.Pair");
    private final Method entity,slots,subPackets,first,second,pairOf,teamName,teamPlayers,parameters,addTeam,removeTeam,joinTeam;
    private final Constructor<?> equipmentConstructor,bundleConstructor,teamConstructor,teamPacketConstructor;
    private final Field teamMethod;
    private final Object air,addAction;
    private boolean failed,closed;
    private record Actor(UUID uuid,String name,Object team){}
    EquipmentPrivacy(KasabaVampirPlugin plugin){this.plugin=plugin;try{
        entity=equipment.getMethod("getEntity");slots=equipment.getMethod("getSlots");equipmentConstructor=equipment.getConstructor(int.class,List.class);
        subPackets=bundle.getMethod("subPackets");bundleConstructor=bundle.getConstructor(Iterable.class);
        first=pair.getMethod("getFirst");second=pair.getMethod("getSecond");pairOf=pair.getMethod("of",Object.class,Object.class);
        teamName=teamPacket.getMethod("getName");teamPlayers=teamPacket.getMethod("getPlayers");parameters=teamPacket.getMethod("getParameters");teamMethod=teamPacket.getDeclaredField("method");teamMethod.setAccessible(true);
        teamPacketConstructor=teamPacket.getDeclaredConstructor(String.class,int.class,Optional.class,Collection.class);teamPacketConstructor.setAccessible(true);
        teamConstructor=team.getConstructor(scoreboard,String.class);addTeam=teamPacket.getMethod("createAddOrModifyPacket",team,boolean.class);removeTeam=teamPacket.getMethod("createRemovePacket",team);Class<?> action=type("net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket$Action");joinTeam=teamPacket.getMethod("createPlayerPacket",team,String.class,action);addAction=action.getField("ADD").get(null);
        Class<?> craft=type(Bukkit.getServer().getClass().getPackageName()+".inventory.CraftItemStack");air=craft.getMethod("asNMSCopy",ItemStack.class).invoke(null,new ItemStack(Material.AIR));
    }catch(ReflectiveOperationException e){throw new IllegalStateException("Gizli eşya görünümü Paper 1.21.10 üzerinde başlatılamadı",e);}}
    private static Class<?> type(String name){try{return Class.forName(name);}catch(ClassNotFoundException e){throw new IllegalStateException(e);}}
    private Object handle(Player p)throws ReflectiveOperationException{return p.getClass().getMethod("getHandle").invoke(p);}
    private Object listener(Player p)throws ReflectiveOperationException{Object h=handle(p);return h.getClass().getField("connection").get(h);}
    void install(Player p){try{Object l=listener(p);Object connection=l.getClass().getField("connection").get(l);Channel channel=(Channel)connection.getClass().getField("channel").get(connection);channels.put(p.getUniqueId(),channel);UUID viewer=p.getUniqueId();
        channel.eventLoop().execute(()->{if(closed||channel.pipeline().get(HOOK)!=null)return;channel.pipeline().addBefore("packet_handler",HOOK,new ChannelDuplexHandler(){@Override public void write(ChannelHandlerContext ctx,Object message,ChannelPromise promise)throws Exception{try{super.write(ctx,filter(message,viewer),promise);}catch(ReflectiveOperationException e){if(!failed){failed=true;plugin.getLogger().log(Level.SEVERE,"Gizli ekipman paketi engellendi",e);}promise.trySuccess();}}});});
        for(Actor a:actors.values())send(p,addTeam.invoke(null,a.team,true));
    }catch(ReflectiveOperationException e){throw new IllegalStateException("Oyuncu gizlilik bağlantısı kurulamadı: "+p.getName(),e);}}
    void hide(Player p){if(closed)return;install(p);try{Object t=teamConstructor.newInstance(scoreboard.getConstructor().newInstance(),TEAM_PREFIX+p.getEntityId());Object never=visibility.getField("NEVER").get(null);team.getMethod("setNameTagVisibility",visibility).invoke(t,never);@SuppressWarnings("unchecked")Collection<String> members=(Collection<String>)team.getMethod("getPlayers").invoke(t);members.add(p.getName());Actor actor=new Actor(p.getUniqueId(),p.getName(),t);names.add(actor.name);actors.put(p.getEntityId(),actor);for(Player viewer:Bukkit.getOnlinePlayers())send(viewer,addTeam.invoke(null,t,true));}catch(ReflectiveOperationException e){throw new IllegalStateException("Gizli oyuncu görünümü kurulamadı",e);}}
    void release(Player p){if(closed||!plugin.isEnabled())return;Actor actor=actors.get(p.getEntityId());if(actor==null)return;Bukkit.getScheduler().runTaskLater(plugin,()->{if(actors.get(p.getEntityId())==actor)reveal(p.getEntityId(),actor);},2);}
    private void reveal(int id,Actor actor){actors.remove(id,actor);names.remove(actor.name);for(Player viewer:Bukkit.getOnlinePlayers())try{send(viewer,removeTeam.invoke(null,actor.team));Object bukkit=viewer.getScoreboard();Object board=bukkit.getClass().getMethod("getHandle").invoke(bukkit);Object old=board.getClass().getMethod("getPlayersTeam",String.class).invoke(board,actor.name);if(old!=null){send(viewer,addTeam.invoke(null,old,false));send(viewer,joinTeam.invoke(null,old,actor.name,addAction));}Player owner=Bukkit.getPlayer(actor.uuid);if(owner!=null)viewer.sendEquipmentChange(owner,Map.of(EquipmentSlot.HAND,owner.getInventory().getItemInMainHand(),EquipmentSlot.OFF_HAND,owner.getInventory().getItemInOffHand()));}catch(ReflectiveOperationException e){plugin.getLogger().log(Level.WARNING,"Oyuncu görünümü geri yüklenemedi",e);}}
    private void send(Player p,Object message)throws ReflectiveOperationException{Object l=listener(p);l.getClass().getMethod("send",packet).invoke(l,message);}
    Object filter(Object message,UUID viewer)throws ReflectiveOperationException{
        if(equipment.isInstance(message)){Actor actor=actors.get((Integer)entity.invoke(message));if(actor==null||actor.uuid.equals(viewer))return message;List<Object> list=new ArrayList<>();boolean changed=false;for(Object item:(List<?>)slots.invoke(message)){Object slot=first.invoke(item);String name=((Enum<?>)slot).name();boolean hand=name.equals("MAINHAND")||name.equals("OFFHAND");list.add(hand?pairOf.invoke(null,slot,air):item);changed|=hand;}return changed?equipmentConstructor.newInstance((Integer)entity.invoke(message),list):message;}
        if(bundle.isInstance(message)){List<Object> packets=new ArrayList<>();boolean changed=false;for(Object child:(Iterable<?>)subPackets.invoke(message)){Object replacement=filter(child,viewer);packets.add(replacement);changed|=replacement!=child;}return changed?bundleConstructor.newInstance(packets):message;}
        if(teamPacket.isInstance(message)&&!((String)teamName.invoke(message)).startsWith(TEAM_PREFIX)){Collection<?> original=(Collection<?>)teamPlayers.invoke(message);List<String> kept=new ArrayList<>();for(Object player:original)if(!names.contains((String)player))kept.add((String)player);if(kept.size()!=original.size())return teamPacketConstructor.newInstance(teamName.invoke(message),teamMethod.getInt(message),parameters.invoke(message),kept);}
        return message;
    }
    void forgetConnection(UUID id){Channel channel=channels.remove(id);if(channel!=null)channel.eventLoop().execute(()->{if(channel.pipeline().get(HOOK)!=null)channel.pipeline().remove(HOOK);});}
    @Override public void close(){closed=true;for(var entry:List.copyOf(actors.entrySet()))reveal(entry.getKey(),entry.getValue());for(UUID id:List.copyOf(channels.keySet()))forgetConnection(id);names.clear();}
}

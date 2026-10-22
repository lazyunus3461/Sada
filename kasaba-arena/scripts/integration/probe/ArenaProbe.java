package tr.kasaba.test;

import com.google.gson.Gson;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import tr.kasaba.arena.api.ArenaEconomy;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Local test fixture only. This plugin is never included in the distribution. */
public final class ArenaProbe extends JavaPlugin {
    private JavaPlugin arena;private final Gson gson=new Gson();
    private final Map<UUID,BigDecimal> balances=new HashMap<>();private final Map<String,Boolean> payments=new HashMap<>();
    private int charges;private boolean failAfterDebit;
    private final ArenaEconomy bank=new ArenaEconomy(){
        public BigDecimal balance(UUID id){return balances.computeIfAbsent(id,key->new BigDecimal("1000"));}
        public boolean withdraw(UUID id,BigDecimal price,String transaction){if(payments.containsKey(transaction))return payments.get(transaction);boolean success=balance(id).compareTo(price)>=0;payments.put(transaction,success);if(success){balances.put(id,balance(id).subtract(price));charges++;if(failAfterDebit){failAfterDebit=false;throw new IllegalStateException("Synthetic provider failure after debit");}}return success;}
    };
    public void onEnable(){
        if(!Bukkit.getIp().equals("127.0.0.1"))throw new IllegalStateException("Loopback test server required");arena=(JavaPlugin)Bukkit.getPluginManager().getPlugin("KasabaArena");
        Bukkit.getMessenger().registerIncomingPluginChannel(this,"kasaba_test:arena",(channel,p,bytes)->{
            if(bytes.length>1024||p.getAddress()==null||!p.getAddress().getAddress().isLoopbackAddress())return;String op=new String(bytes,StandardCharsets.UTF_8);
            try{String[] a=op.split("\\|");switch(a[0]){
                case "SETUP"->setup(p);
                case "ADMIN"->{p.setOp(true);p.setGravity(false);}
                case "PLAYER"->{String cmd=op.substring(7);if(!cmd.startsWith("arena")&&!cmd.startsWith("oyunlar ")&&!cmd.startsWith("akce")&&!cmd.startsWith("akçe")&&!cmd.equals("lobi"))throw new IllegalArgumentException("Fixture command forbidden");Bukkit.dispatchCommand(p,cmd);}
                case "STATE"->state(p);
                case "POSE"->{p.teleport(new Location(Bukkit.getWorld("world"),Double.parseDouble(a[1]),a.length>4?Double.parseDouble(a[4]):-60,Double.parseDouble(a[2]),Float.parseFloat(a[3]),a.length>5?Float.parseFloat(a[5]):0));p.setGravity(false);p.setVelocity(new org.bukkit.util.Vector());}
                case "READY"->{Object combat=get(arena,"combat");((Map<?,?>)get(combat,"protection")).clear();((Map<?,?>)get(combat,"sharedUntil")).clear();((Map<?,?>)get(combat,"lastMelee")).clear();for(Object raw:((Map<?,?>)get(combat,"cooldowns")).values())Arrays.fill((long[])raw,0L);for(Player other:Bukkit.getOnlinePlayers())other.setNoDamageTicks(0);}
                case "HIT"->{Player victim=Objects.requireNonNull(Bukkit.getPlayerExact(a[1]));victim.setNoDamageTicks(0);victim.damage(7,p);}
                case "UNOP"->p.setOp(false);
                case "REGEN_READY"->{Object combat=get(arena,"combat");((Map<UUID,Long>)get(combat,"lastCombat")).put(p.getUniqueId(),0L);}
                case "SHARED"->{arena.getConfig().set("ayarlar.ortak-oda-numaralari",Boolean.parseBoolean(a[1]));arena.saveConfig();}
                case "HEALTH"->{p.setHealth(Double.parseDouble(a[1]));p.setNoDamageTicks(0);}
                case "BUFF"->{p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,1000,3));p.setAbsorptionAmount(8);p.getInventory().setItemInOffHand(p.getInventory().getItem(4));}
                case "KILL"->{Player victim=Objects.requireNonNull(Bukkit.getPlayerExact(a[1]));victim.setHealth(0);}
                case "ITEM"->{p.getInventory().setItem(0,new ItemStack(Material.valueOf(a[1]),a.length>2?Integer.parseInt(a[2]):1));p.getInventory().setHeldItemSlot(0);}
                case "BANK"->Bukkit.getServicesManager().register(ArenaEconomy.class,bank,this,ServicePriority.Highest);
                case "BANK_FAIL_AFTER_DEBIT"->failAfterDebit=true;
                case "RESUME"->invoke(get(arena,"market"),"resume");
                case "NOBANK"->Bukkit.getServicesManager().unregister(ArenaEconomy.class,bank);
                case "BUY"->{Object market=get(arena,"market");List<?> offers=(List<?>)invoke(market,"products");Object offer=offers.stream().filter(o->{try{return invoke(o,"id").equals(a[1]);}catch(Exception e){return false;}}).findFirst().orElseThrow();invoke(market,"buy",p,offer);}
                case "CONSUME"->{for(int slot=0;slot<p.getInventory().getSize();slot++){ItemStack item=p.getInventory().getItem(slot);if(market(item)&&Objects.equals(item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey("kasabaarena","market-product"),PersistentDataType.STRING),a[1])){PlayerItemConsumeEvent event=new PlayerItemConsumeEvent(p,item.clone(),EquipmentSlot.HAND);Bukkit.getPluginManager().callEvent(event);if(!event.isCancelled())item.setAmount(item.getAmount()-1);break;}}}
                case "EXPIRE"->{Object market=get(arena,"market");YamlConfiguration data=(YamlConfiguration)get(market,"data");data.set("owned."+p.getUniqueId()+"."+a[1]+".until",System.currentTimeMillis()-1000);invoke(market,"save");invoke(market,"expire");}
                case "BREAK"->{var block=p.getWorld().getBlockAt((int)p.getLocation().getX(),-61,(int)p.getLocation().getZ());BlockBreakEvent event=new BlockBreakEvent(block,p);Bukkit.getPluginManager().callEvent(event);say(p,"break="+event.isCancelled());}
                case "PLACE"->{var block=p.getWorld().getBlockAt((int)p.getLocation().getX(),-60,(int)p.getLocation().getZ());BlockPlaceEvent event=new BlockPlaceEvent(block,block.getState(),block.getRelative(0,-1,0),new ItemStack(Material.STONE),p,true,EquipmentSlot.HAND);Bukkit.getPluginManager().callEvent(event);say(p,"place="+event.isCancelled());}
                default->throw new IllegalArgumentException("Unknown fixture operation");
            }say(p,"done:"+op);}catch(Throwable ex){Throwable cause=ex instanceof InvocationTargetException t?t.getCause():ex;getLogger().warning("Fixture: "+cause);say(p,"error:"+cause.getClass().getSimpleName()+":"+cause.getMessage());}
        });
    }
    private void setup(Player p)throws Exception{
        p.setOp(true);p.setGravity(false);World world=Bukkit.getWorld("world");var cfg=arena.getConfig();cfg.set("odalar",null);cfg.set("regions",null);cfg.set("market",null);cfg.set("ayarlar.ortak-oda-numaralari",true);point(cfg,"spawns.lobi",80.5,80.5);List<Map<String,Object>> locations=new ArrayList<>();for(int i=0;i<3;i++)locations.add(location(120.5+i*4,120.5));cfg.set("spawns.xarena",locations);
        for(String type:List.of("1v1","2v2"))for(int id=1;id<=5;id++){double x=200.5+id*30,z=200.5;point(cfg,"odalar."+type+"."+id+".kirmizi",x,z);point(cfg,"odalar."+type+"."+id+".mavi",x+6,z);for(int dx=-2;dx<=8;dx++)for(int dz=-3;dz<=3;dz++)world.getBlockAt((int)x+dx,-61,(int)z+dz).setType(Material.STONE);}
        for(int x=75;x<135;x++)for(int z=75;z<135;z++)world.getBlockAt(x,-61,z).setType(Material.STONE);arena.saveConfig();invoke(arena,"loadLocations");
        Object market=get(arena,"market");YamlConfiguration data=(YamlConfiguration)get(market,"data");data.set("owned",null);data.set("pending",null);invoke(market,"save");
        p.teleport(new Location(world,35.5,-60,35.5));p.getInventory().clear();p.getInventory().setItem(0,new ItemStack(Material.DIAMOND,7));
    }
    private Map<String,Object> location(double x,double z){World world=Bukkit.getWorld("world");return Map.of("world",world.getUID().toString(),"worldName","world","x",x,"y",-60.0,"z",z,"yaw",-90.0,"pitch",0.0);}
    private void point(org.bukkit.configuration.ConfigurationSection cfg,String path,double x,double z){cfg.set(path,location(x,z));}
    private boolean ability(ItemStack item){return item!=null&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey("kasabaarena","arena-ability"));}
    private boolean market(ItemStack item){return item!=null&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey("kasabaarena","market-product"));}
    private void state(Player p)throws Exception{
        UUID id=p.getUniqueId();Object combat=get(arena,"combat"),market=get(arena,"market");Map<?,?> places=(Map<?,?>)get(arena,"places"),matches=(Map<?,?>)get(arena,"matches");Map<String,Object> result=new LinkedHashMap<>();result.put("place",String.valueOf(places.get(id)));result.put("pendingSkills",((Set<?>)((Map<?,?>)get(combat,"tasks")).getOrDefault(id,null))==null?0:((Set<?>)((Map<?,?>)get(combat,"tasks")).get(id)).size());result.put("hp",p.getHealth());result.put("absorption",p.getAbsorptionAmount());result.put("mode",p.getGameMode().name());result.put("skills",Arrays.stream(p.getInventory().getContents()).filter(this::ability).count());result.put("effects",p.getActivePotionEffects().stream().map(e->e.getType().getKey().getKey()).toList());result.put("cooldowns",((Map<?,?>)get(combat,"cooldowns")).get(id));result.put("regions",((Map<?,?>)get(arena,"regions")).keySet());result.put("x",p.getLocation().getX());result.put("z",p.getLocation().getZ());List<String> slots=new ArrayList<>();for(int slot=0;slot<9;slot++){ItemStack item=p.getInventory().getItem(slot);slots.add(item==null?"AIR":item.getType()+":"+(ability(item)?"skill":"item"));}result.put("slots",slots);result.put("armor",Arrays.stream(p.getInventory().getArmorContents()).map(item->item==null?"AIR":item.getType()+":"+item.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.PROTECTION)).toList());result.put("items",Arrays.stream(p.getInventory().getContents()).filter(Objects::nonNull).filter(i->!i.getType().isAir()).map(item->item.getType()+":"+item.getAmount()+":"+(market(item)?item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey("kasabaarena","market-product"),PersistentDataType.STRING):"normal")).toList());
        Object match=matches.get(id);if(match!=null){Object room=get(match,"arena");result.put("room",invoke(room,"key"));result.put("code",get(match,"code"));result.put("started",get(match,"started"));result.put("paused",get(match,"paused"));result.put("round",get(match,"round"));result.put("redScore",get(match,"redScore"));result.put("blueScore",get(match,"blueScore"));}
        YamlConfiguration data=(YamlConfiguration)get(market,"data");var owned=data.getConfigurationSection("owned."+id);Map<String,Integer> stock=new TreeMap<>();if(owned!=null)for(String key:owned.getKeys(false))stock.put(key,owned.getInt(key+".remaining"));result.put("stock",stock);Map<String,Long> leases=new TreeMap<>();if(owned!=null)for(String key:owned.getKeys(false))leases.put(key,(owned.getLong(key+".until")-System.currentTimeMillis())/1000);result.put("leases",leases);ArenaEconomy provider=Bukkit.getServicesManager().load(ArenaEconomy.class);result.put("balance",provider==null?-1:provider.balance(id).doubleValue());result.put("charges",charges);say(p,"state:"+gson.toJson(result));
    }
    private Object get(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private Object invoke(Object object,String name,Object... args)throws Exception{Method method=Arrays.stream(object.getClass().getDeclaredMethods()).filter(m->m.getName().equals(name)&&m.getParameterCount()==args.length).findFirst().orElseThrow();method.setAccessible(true);return method.invoke(object,args);}
    private void say(Player p,String value){p.sendMessage(Component.text("arena-probe:"+value));}
}

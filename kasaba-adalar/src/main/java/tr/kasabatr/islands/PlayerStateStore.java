package tr.kasabatr.islands;

import org.bukkit.GameMode;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Keeps 1.2.0 player-data compatible and prevents creative items escaping into other games. */
final class PlayerStateStore {
    private final Path directory;
    PlayerStateStore(JavaPlugin plugin)throws IOException{directory=plugin.getDataFolder().toPath().resolve("player-data");Files.createDirectories(directory);}
    private Path path(Player p){return directory.resolve(p.getUniqueId()+".yml");}
    private YamlConfiguration read(Player p)throws IOException{YamlConfiguration data=new YamlConfiguration();try{if(Files.exists(path(p)))data.load(path(p).toFile());}catch(Exception ex){throw new IOException("Oyuncu envanteri okunamadı; dosya korundu.",ex);}return data;}
    private void write(Player p,YamlConfiguration data)throws IOException{Path temp=Files.createTempFile(directory,"player-",".tmp");try{Files.writeString(temp,data.saveToString());Files.move(temp,path(p),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(temp);}}
    private static ItemStack clean(ItemStack item){if(item==null)return null;if(item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(new org.bukkit.NamespacedKey("kasabahub","lobi-menusu")))return null;return item.clone();}
    private static void capture(YamlConfiguration data,String key,Player p){
        data.set(key+".storage",Arrays.stream(p.getInventory().getStorageContents()).map(PlayerStateStore::clean).toList());data.set(key+".armor",Arrays.stream(p.getInventory().getArmorContents()).map(PlayerStateStore::clean).toList());data.set(key+".offhand",clean(p.getInventory().getItemInOffHand()));data.set(key+".cursor",clean(p.getItemOnCursor()));data.set(key+".slot",p.getInventory().getHeldItemSlot());data.set(key+".level",p.getLevel());data.set(key+".experience",p.getExp());data.set(key+".total-experience",p.getTotalExperience());data.set(key+".game-mode",p.getGameMode().name());data.set(key+".health",p.getHealth());data.set(key+".food",p.getFoodLevel());data.set(key+".saturation",p.getSaturation());data.set(key+".effects",new ArrayList<>(p.getActivePotionEffects()));data.set(key+".absorption",p.getAbsorptionAmount());
    }
    private static ItemStack[] items(YamlConfiguration data,String key,int length)throws IOException{List<?> list=data.getList(key);ItemStack[] items=new ItemStack[length];if(list==null)return items;if(list.size()!=length)throw new IOException("Envanter boyutu bozuk: "+key);for(int i=0;i<length;i++){Object value=list.get(i);if(value!=null&&!(value instanceof ItemStack))throw new IOException("Envanter eşyası bozuk: "+key);items[i]=clean((ItemStack)value);}return items;}
    private static void apply(YamlConfiguration data,String key,Player p)throws IOException{
        ItemStack[] storage=items(data,key+".storage",36),armor=items(data,key+".armor",4);List<PotionEffect> effects=new ArrayList<>();for(Object value:data.getList(key+".effects",List.of())){if(!(value instanceof PotionEffect effect))throw new IOException("İksir kaydı bozuk.");effects.add(effect);}GameMode mode;try{mode=GameMode.valueOf(data.getString(key+".game-mode",key.equals("island")?"CREATIVE":"SURVIVAL"));}catch(RuntimeException ex){throw new IOException("Oyun modu kaydı bozuk.",ex);}
        p.closeInventory();p.setItemOnCursor(null);p.getInventory().setStorageContents(storage);p.getInventory().setArmorContents(armor);p.getInventory().setItemInOffHand(clean(data.getItemStack(key+".offhand")));p.getInventory().setHeldItemSlot(Math.max(0,Math.min(8,data.getInt(key+".slot",0))));p.setTotalExperience(Math.max(0,data.getInt(key+".total-experience",0)));p.setLevel(Math.max(0,data.getInt(key+".level",0)));p.setExp((float)Math.max(0,Math.min(0.9999,data.getDouble(key+".experience",0))));for(PotionEffect effect:p.getActivePotionEffects())p.removePotionEffect(effect.getType());p.addPotionEffects(effects);p.setAbsorptionAmount(Math.max(0,data.getDouble(key+".absorption",0)));p.setFireTicks(0);p.setFallDistance(0);if(!p.isDead())p.setHealth(Math.max(1,Math.min(p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue(),data.getDouble(key+".health",20))));p.setFoodLevel(Math.max(0,Math.min(20,data.getInt(key+".food",20))));p.setSaturation((float)Math.max(0,Math.min(20,data.getDouble(key+".saturation",10))));p.setGameMode(mode);p.setItemOnCursor(clean(data.getItemStack(key+".cursor")));p.updateInventory();
    }
    boolean active(Player p)throws IOException{return read(p).getBoolean("active",false);}
    void enter(Player p)throws IOException{YamlConfiguration data=read(p);if(data.getBoolean("active",false)){p.setGameMode(GameMode.CREATIVE);return;}items(data,"island.storage",36);items(data,"island.armor",4);capture(data,"outside",p);data.set("active",true);write(p,data);try{apply(data,"island",p);p.setGameMode(GameMode.CREATIVE);}catch(IOException ex){apply(data,"outside",p);data.set("active",false);write(p,data);throw ex;}}
    void leave(Player p)throws IOException{YamlConfiguration data=read(p);if(!data.getBoolean("active",false))return;if(!data.contains("outside.storage")||!data.contains("outside.game-mode"))throw new IOException("Normal envanter yedeği eksik.");capture(data,"island",p);write(p,data);apply(data,"outside",p);data.set("active",false);write(p,data);}
    void recoverOutside(Player p)throws IOException{YamlConfiguration data=read(p);if(!data.getBoolean("active",false))return;if(!data.contains("outside.storage"))throw new IOException("Normal envanter yedeği eksik.");apply(data,"outside",p);data.set("active",false);write(p,data);}
    void saveIsland(Player p)throws IOException{YamlConfiguration data=read(p);if(!data.getBoolean("active",false))return;capture(data,"island",p);write(p,data);}
}

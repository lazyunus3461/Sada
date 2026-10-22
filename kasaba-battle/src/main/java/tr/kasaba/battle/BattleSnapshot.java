package tr.kasaba.battle;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

/** Write-ahead outside snapshots. A failed read/write never clears the player. */
final class BattleSnapshot {
    private final Path folder;
    BattleSnapshot(Path folder)throws IOException{this.folder=folder;Files.createDirectories(folder);}
    private Path path(Player p){return folder.resolve(p.getUniqueId()+".yml");}
    boolean pending(Player p)throws IOException{return AtomicYaml.read(path(p)).getBoolean("active");}
    private ItemStack clean(ItemStack item){if(item==null)return null;if(item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey("kasabahub","lobi-menusu")))return null;return item.clone();}
    void save(Player p)throws IOException{
        if(pending(p))throw new IOException("Önceki BR yedeği henüz geri yüklenmedi.");
        YamlConfiguration d=new YamlConfiguration();d.set("active",true);d.set("location",p.getLocation());
        d.set("storage",Arrays.stream(p.getInventory().getStorageContents()).map(this::clean).toList());d.set("armor",Arrays.stream(p.getInventory().getArmorContents()).map(this::clean).toList());d.set("offhand",clean(p.getInventory().getItemInOffHand()));d.set("cursor",clean(p.getItemOnCursor()));d.set("slot",p.getInventory().getHeldItemSlot());
        d.set("mode",p.getGameMode().name());d.set("level",p.getLevel());d.set("exp",p.getExp());d.set("total",p.getTotalExperience());d.set("health",p.getHealth());d.set("food",p.getFoodLevel());d.set("saturation",p.getSaturation());d.set("exhaustion",p.getExhaustion());d.set("effects",new ArrayList<>(p.getActivePotionEffects()));d.set("absorption",p.getAbsorptionAmount());d.set("invulnerable",p.isInvulnerable());d.set("allow-flight",p.getAllowFlight());d.set("flying",p.isFlying());AtomicYaml.write(path(p),d);
    }
    private ItemStack[] items(YamlConfiguration d,String key,int count)throws IOException{List<?> a=d.getList(key);if(a==null||a.size()!=count)throw new IOException("Envanter yedeği geçersiz: "+key);ItemStack[] out=new ItemStack[count];for(int i=0;i<count;i++){if(a.get(i)!=null&&!(a.get(i) instanceof ItemStack))throw new IOException("Eşya kaydı geçersiz.");out[i]=clean((ItemStack)a.get(i));}return out;}
    Location location(Player p)throws IOException{return AtomicYaml.read(path(p)).getLocation("location");}
    void restore(Player p)throws IOException{
        YamlConfiguration d=AtomicYaml.read(path(p));if(!d.getBoolean("active"))return;
        ItemStack[] storage=items(d,"storage",36),armor=items(d,"armor",4);GameMode mode;try{mode=GameMode.valueOf(d.getString("mode"));}catch(Exception ex){throw new IOException("Oyun modu yedeği geçersiz.",ex);}
        List<PotionEffect> effects=new ArrayList<>();for(Object raw:d.getList("effects",List.of())){if(!(raw instanceof PotionEffect e))throw new IOException("İksir yedeği geçersiz.");effects.add(e);}
        p.closeInventory();p.setItemOnCursor(null);p.getInventory().setStorageContents(storage);p.getInventory().setArmorContents(armor);p.getInventory().setItemInOffHand(clean(d.getItemStack("offhand")));p.getInventory().setHeldItemSlot(Math.clamp(d.getInt("slot"),0,8));p.setTotalExperience(d.getInt("total"));p.setLevel(d.getInt("level"));p.setExp((float)d.getDouble("exp"));
        for(PotionEffect e:p.getActivePotionEffects())p.removePotionEffect(e.getType());p.addPotionEffects(effects);p.setAbsorptionAmount(d.getDouble("absorption"));p.setInvulnerable(d.getBoolean("invulnerable"));p.setFireTicks(0);p.setFallDistance(0);if(!p.isDead())p.setHealth(Math.clamp(d.getDouble("health",20),1,p.getAttribute(Attribute.MAX_HEALTH).getValue()));p.setFoodLevel(d.getInt("food",20));p.setSaturation((float)d.getDouble("saturation"));p.setExhaustion((float)d.getDouble("exhaustion"));p.setGameMode(mode);p.setAllowFlight(d.getBoolean("allow-flight"));if(p.getAllowFlight())p.setFlying(d.getBoolean("flying"));p.setItemOnCursor(clean(d.getItemStack("cursor")));p.updateInventory();
        d.set("active",false);AtomicYaml.write(path(p),d);
    }
}

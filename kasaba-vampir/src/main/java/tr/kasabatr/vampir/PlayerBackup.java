package tr.kasabatr.vampir;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

/** A pending snapshot is never overwritten or deleted until restoration succeeds. */
final class PlayerBackup {
    private PlayerBackup(){}
    private static File file(File dir,UUID id){return new File(new File(dir,"recoveries"),id+".yml");}
    static boolean exists(File dir,UUID id){return file(dir,id).isFile();}
    static void save(File dir,Player p)throws IOException{
        File target=file(dir,p.getUniqueId());if(target.exists())throw new IOException("Bekleyen envanter yedeği var: "+p.getName());Files.createDirectories(target.toPath().getParent());
        YamlConfiguration y=new YamlConfiguration();ItemStack[] contents=p.getInventory().getContents();NamespacedKey hubKey=new NamespacedKey("kasabahub","lobi-menusu");
        for(int i=0;i<contents.length;i++)if(contents[i]!=null&&contents[i].hasItemMeta()&&contents[i].getItemMeta().getPersistentDataContainer().has(hubKey))contents[i]=null;
        y.set("inventory",Arrays.asList(contents));y.set("cursor",p.getItemOnCursor());y.set("held-slot",p.getInventory().getHeldItemSlot());y.set("game-mode",p.getGameMode().name());y.set("health",p.getHealth());y.set("food",p.getFoodLevel());y.set("saturation",p.getSaturation());y.set("exhaustion",p.getExhaustion());y.set("level",p.getLevel());y.set("exp",p.getExp());y.set("total-exp",p.getTotalExperience());y.set("allow-flight",p.getAllowFlight());y.set("flying",p.isFlying());y.set("invulnerable",p.isInvulnerable());y.set("absorption",p.getAbsorptionAmount());y.set("effects",new ArrayList<>(p.getActivePotionEffects()));y.set("location",p.getLocation());
        Path temp=Files.createTempFile(target.toPath().getParent(),target.getName(),".tmp");try{y.save(temp.toFile());try{Files.move(temp,target.toPath(),StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temp,target.toPath());}}finally{Files.deleteIfExists(temp);}
    }
    static boolean restore(File dir,Player p,boolean restoreLocation)throws Exception{
        File source=file(dir,p.getUniqueId());if(!source.isFile())return false;YamlConfiguration y=new YamlConfiguration();y.load(source);
        List<?> stored=y.getList("inventory");if(stored==null||stored.size()!=p.getInventory().getContents().length)throw new IOException("Envanter kaydı bozuk: "+source);
        ItemStack[] items=new ItemStack[stored.size()];for(int i=0;i<items.length;i++){Object value=stored.get(i);if(value!=null&&!(value instanceof ItemStack))throw new IOException("Geçersiz eşya kaydı");items[i]=value==null?null:((ItemStack)value).clone();}
        Object cursor=y.get("cursor");if(cursor!=null&&!(cursor instanceof ItemStack))throw new IOException("Geçersiz imleç kaydı");
        List<PotionEffect> effects=new ArrayList<>();for(Object effect:y.getList("effects",List.of())){if(!(effect instanceof PotionEffect e))throw new IOException("Geçersiz etki kaydı");effects.add(e);}
        GameMode mode=GameMode.valueOf(y.getString("game-mode","SURVIVAL"));Location old=y.getLocation("location");if(restoreLocation&&(old==null||old.getWorld()==null))throw new IOException("Eski dünya yüklü değil");
        double health=y.getDouble("health",20),absorption=y.getDouble("absorption",0);if(!Double.isFinite(health)||health<=0||!Double.isFinite(absorption)||absorption<0)throw new IOException("Geçersiz can kaydı");
        p.closeDialog();p.closeInventory();p.setItemOnCursor(null);p.getInventory().setContents(items);p.getInventory().setHeldItemSlot(Math.clamp(y.getInt("held-slot",0),0,8));p.getActivePotionEffects().stream().map(PotionEffect::getType).toList().forEach(p::removePotionEffect);p.setGameMode(mode);p.addPotionEffects(effects);p.setAbsorptionAmount(absorption);p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(),health));p.setFoodLevel(Math.clamp(y.getInt("food",20),0,20));p.setSaturation((float)Math.clamp(y.getDouble("saturation",5),0,20));p.setExhaustion((float)Math.clamp(y.getDouble("exhaustion",0),0,40));p.setTotalExperience(Math.max(0,y.getInt("total-exp",0)));p.setLevel(Math.max(0,y.getInt("level",0)));p.setExp((float)Math.clamp(y.getDouble("exp",0),0,.999999));p.setAllowFlight(y.getBoolean("allow-flight"));p.setFlying(y.getBoolean("flying")&&p.getAllowFlight());p.setInvulnerable(y.getBoolean("invulnerable"));p.setFireTicks(0);p.setFallDistance(0);if(restoreLocation&&!p.teleport(old))throw new IOException("Eski konuma dönülemedi");p.setItemOnCursor((ItemStack)cursor);Files.delete(source.toPath());return true;
    }
}

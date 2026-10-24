package tr.kasabatr.esya;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

/** Compatible with old recoveries; never discard the only copy on a failed restore. */
final class PlayerBackup {
    private static File file(File folder,UUID id){return new File(new File(folder,"recoveries"),id+".yml");}
    static boolean exists(File folder,UUID id){return file(folder,id).isFile();}
    static void save(File folder,Player p)throws IOException{
        File target=file(folder,p.getUniqueId());if(target.exists())throw new IOException("Önceki eşya yedeği kurtarılmalı.");
        Files.createDirectories(target.toPath().getParent());var y=new YamlConfiguration();
        y.set("collidable",p.isCollidable());y.set("inventory",Arrays.asList(p.getInventory().getContents()));y.set("held-slot",p.getInventory().getHeldItemSlot());y.set("game-mode",p.getGameMode().name());
        y.set("health",p.getHealth());y.set("food",p.getFoodLevel());y.set("saturation",p.getSaturation());y.set("exhaustion",p.getExhaustion());y.set("absorption",p.getAbsorptionAmount());
        y.set("level",p.getLevel());y.set("exp",p.getExp());y.set("total-exp",p.getTotalExperience());y.set("fire",p.getFireTicks());
        y.set("allow-flight",p.getAllowFlight());y.set("flying",p.isFlying());y.set("gravity",p.hasGravity());y.set("invulnerable",p.isInvulnerable());
        y.set("effects",new ArrayList<>(p.getActivePotionEffects()));y.set("location",p.getLocation());
        Path tmp=target.toPath().resolveSibling(target.getName()+".tmp");y.save(tmp.toFile());
        try{Files.move(tmp,target.toPath(),StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,target.toPath());}
    }
    static boolean restore(File folder,Player p,boolean teleport){
        File target=file(folder,p.getUniqueId());if(!target.isFile())return false;
        var y=YamlConfiguration.loadConfiguration(target);var rows=y.getList("inventory");int capacity=p.getInventory().getContents().length;if(rows==null||rows.size()>capacity)throw new IllegalStateException("Eşya yedeği okunamadı.");
        ItemStack[] items=new ItemStack[capacity];for(int i=0;i<Math.min(rows.size(),items.length);i++){Object item=rows.get(i);if(item!=null&&!(item instanceof ItemStack))throw new IllegalStateException("Eşya yedeği bozuk.");items[i]=item==null?null:((ItemStack)item).clone();}
        GameMode mode=GameMode.valueOf(y.getString("game-mode","SURVIVAL"));Location point=y.getLocation("location");
        var effects=new ArrayList<PotionEffect>();for(Object effect:y.getList("effects",List.of())){if(!(effect instanceof PotionEffect pfx))throw new IllegalStateException("İksir yedeği bozuk.");effects.add(pfx);}
        if(teleport&&(point==null||point.getWorld()==null||!p.teleport(point)))throw new IllegalStateException("Önceki konuma dönülemedi.");
        p.closeInventory();p.setItemOnCursor(null);p.setCollidable(y.getBoolean("collidable",true));p.getInventory().setContents(items);p.getInventory().setHeldItemSlot(Math.clamp(y.getInt("held-slot"),0,8));
        p.setGameMode(mode);p.setFoodLevel(Math.clamp(y.getInt("food",20),0,20));
        p.setSaturation((float)Math.clamp(y.getDouble("saturation",5),0,20));p.setExhaustion((float)Math.max(0,y.getDouble("exhaustion")));
        p.setTotalExperience(Math.max(0,y.getInt("total-exp")));p.setLevel(Math.max(0,y.getInt("level")));p.setExp((float)Math.clamp(y.getDouble("exp"),0,1));p.setFireTicks(y.getInt("fire"));
        p.setAllowFlight(y.getBoolean("allow-flight"));p.setFlying(y.getBoolean("flying")&&p.getAllowFlight());p.setGravity(y.getBoolean("gravity",true));p.setInvulnerable(y.getBoolean("invulnerable"));p.setFallDistance(0);
        for(var effect:p.getActivePotionEffects())p.removePotionEffect(effect.getType());effects.forEach(p::addPotionEffect);
        p.setHealth(Math.clamp(y.getDouble("health",20),.1,p.getMaxHealth()));p.setAbsorptionAmount(Math.max(0,y.getDouble("absorption")));
        try{Files.delete(target.toPath());}catch(IOException e){throw new IllegalStateException("Yedek tamamlandı fakat dosya kaldırılamadı.",e);}return true;
    }
}

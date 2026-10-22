package tr.kasaba.arena;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/** Owns only X Arena combat. No modifiers, effects, projectiles or skill items enter duels. */
final class XCombat implements Listener {
    private record Intent(UUID attacker,double damage) {}
    private final JavaPlugin plugin;
    private final Predicate<Player> active;
    private final Function<Player,ArenaCharacter> character;
    private final Function<Player,Double> marketBonus;
    private final NamespacedKey abilityKey, weaponKey, arrowDamageKey, arrowLifeKey;
    private final Map<UUID,long[]> cooldowns=new HashMap<>();
    private final Map<UUID,Long> sharedUntil=new HashMap<>();
    private final Map<UUID,Long> warCry=new HashMap<>(),shields=new HashMap<>(),protection=new HashMap<>(),lastMelee=new HashMap<>(),lastCombat=new HashMap<>();
    private final Map<UUID,Integer> lives=new HashMap<>();
    private final Map<UUID,Intent> intents=new HashMap<>();
    private final Map<UUID,Set<BukkitTask>> tasks=new HashMap<>();
    private final Map<UUID,Set<Projectile>> projectiles=new HashMap<>();
    private final Set<UUID> mayChoose=new HashSet<>();

    XCombat(JavaPlugin plugin,Predicate<Player> active,Function<Player,ArenaCharacter> character,Function<Player,Double> marketBonus) {
        this.plugin=plugin;this.active=active;this.character=character;
        this.marketBonus=marketBonus;
        abilityKey=new NamespacedKey(plugin,"arena-ability");weaponKey=new NamespacedKey(plugin,"x-weapon");arrowDamageKey=new NamespacedKey(plugin,"x-arrow-damage");arrowLifeKey=new NamespacedKey(plugin,"x-arrow-life");
        Bukkit.getPluginManager().registerEvents(this,plugin);
        Bukkit.getScheduler().runTaskTimer(plugin,()->{for(Player p:Bukkit.getOnlinePlayers())if(active(p))updateSkills(p);},10,10);
        Bukkit.getScheduler().runTaskTimer(plugin,()->{long now=System.currentTimeMillis();for(Player p:Bukkit.getOnlinePlayers())if(active(p)&&lastCombat.getOrDefault(p.getUniqueId(),0L)+8000<=now&&p.getHealth()<Math.min(20,maxHealth(p)))heal(p,1);},40,40);
    }
    boolean active(Player p){return p.isOnline()&&!p.isDead()&&active.test(p);}
    boolean mayChoose(Player p){return mayChoose.contains(p.getUniqueId());}
    void lockChoice(Player p){mayChoose.remove(p.getUniqueId());}
    void clear(Player p) {
        UUID id=p.getUniqueId();lives.merge(id,1,Integer::sum);cooldowns.remove(id);lastCombat.remove(id);sharedUntil.remove(id);warCry.remove(id);shields.remove(id);protection.remove(id);lastMelee.remove(id);intents.remove(id);mayChoose.remove(id);
        for(BukkitTask task:tasks.getOrDefault(id,Set.of()))task.cancel();tasks.remove(id);
        for(Projectile projectile:projectiles.getOrDefault(id,Set.of()))projectile.remove();projectiles.remove(id);
        for(int slot=0;slot<p.getInventory().getSize();slot++)if(temporary(p.getInventory().getItem(slot)))p.getInventory().setItem(slot,null);
        if(temporary(p.getItemOnCursor()))p.setItemOnCursor(null);
    }
    boolean temporary(ItemStack item){return item!=null&&item.hasItemMeta()&&(item.getItemMeta().getPersistentDataContainer().has(abilityKey)||item.getItemMeta().getPersistentDataContainer().has(weaponKey));}
    void loadout(Player p) {
        clear(p);p.getInventory().clear();p.getInventory().setArmorContents(new ItemStack[4]);p.getInventory().setItemInOffHand(null);p.setItemOnCursor(null);
        for(PotionEffect effect:p.getActivePotionEffects())p.removePotionEffect(effect.getType());
        p.setInvulnerable(false);p.setAbsorptionAmount(0);p.setFireTicks(0);p.setFallDistance(0);p.setNoDamageTicks(0);p.setGameMode(GameMode.SURVIVAL);p.setHealth(Math.min(20,maxHealth(p)));p.setFoodLevel(20);p.setSaturation(10);
        ArenaCharacter kind=character.apply(p);
        p.getInventory().setItem(4,weapon(kind==ArenaCharacter.TANK?Material.DIAMOND_AXE:Material.DIAMOND_SWORD,kind.label));
        if(kind==ArenaCharacter.ARCHER){p.getInventory().setItem(5,weapon(Material.BOW,"Okçu • Yay"));p.getInventory().setItem(9,new ItemStack(Material.ARROW,64));}
        boolean tank=kind==ArenaCharacter.TANK;
        p.getInventory().setHelmet(armor(tank?Material.NETHERITE_HELMET:Material.DIAMOND_HELMET));p.getInventory().setChestplate(armor(tank?Material.NETHERITE_CHESTPLATE:Material.DIAMOND_CHESTPLATE));p.getInventory().setLeggings(armor(tank?Material.NETHERITE_LEGGINGS:Material.DIAMOND_LEGGINGS));p.getInventory().setBoots(armor(tank?Material.NETHERITE_BOOTS:Material.DIAMOND_BOOTS));
        for(int index=0;index<4;index++) {
            final int skill=index;ArenaCharacter.Skill spec=kind.skills.get(index);ItemStack item=new ItemStack(switch(index){case 0->Material.BLAZE_POWDER;case 1->Material.AMETHYST_SHARD;case 2->Material.PRISMARINE_CRYSTALS;default->Material.NETHER_STAR;});
            item.editMeta(meta->{meta.displayName(Component.text((skill+1)+" • "+spec.name(),NamedTextColor.GOLD));meta.lore(List.of(Component.text(spec.description(),NamedTextColor.GRAY),Component.text("Bekleme: "+spec.cooldownSeconds()+" saniye",NamedTextColor.YELLOW),Component.text("Sağ tık veya /arena yetenek "+(skill+1),NamedTextColor.AQUA)));meta.getPersistentDataContainer().set(abilityKey,PersistentDataType.STRING,kind.name()+":"+(skill+1));});
            p.getInventory().setItem(index,item);
        }
        p.getInventory().setHeldItemSlot(4);lastCombat.put(p.getUniqueId(),System.currentTimeMillis());mayChoose.add(p.getUniqueId());protect(p);updateSkills(p);
    }
    private ItemStack weapon(Material type,String label){ItemStack item=new ItemStack(type);item.editMeta(meta->{meta.displayName(Component.text(label,NamedTextColor.WHITE));meta.setUnbreakable(true);meta.getPersistentDataContainer().set(weaponKey,PersistentDataType.BYTE,(byte)1);});return item;}
    private ItemStack armor(Material type){ItemStack item=weapon(type,"X Arena • Koruyucu Zırh");item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION,2);return item;}
    private void heal(Player p,double amount){if(active(p))p.setHealth(Math.min(Math.min(20,maxHealth(p)),p.getHealth()+amount));}
    private double maxHealth(Player p){var attr=p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);return attr==null?20:attr.getValue();}
    private void protect(Player p){long seconds=Math.max(0,plugin.getConfig().getLong("ayarlar.yeniden-dogma-koruma-saniye",3));protection.put(p.getUniqueId(),System.currentTimeMillis()+seconds*1000);}
    private boolean alive(UUID id,int life){Player p=Bukkit.getPlayer(id);return p!=null&&active(p)&&lives.getOrDefault(id,0)==life;}
    private void later(Player owner,long delay,Runnable action){UUID id=owner.getUniqueId();int life=lives.getOrDefault(id,0);BukkitTask[] slot=new BukkitTask[1];slot[0]=Bukkit.getScheduler().runTaskLater(plugin,()->{tasks.getOrDefault(id,new HashSet<>()).remove(slot[0]);if(alive(id,life))action.run();},delay);tasks.computeIfAbsent(id,k->new HashSet<>()).add(slot[0]);}
    private void effect(Player p,PotionEffectType type,int ticks,int strength){p.addPotionEffect(new PotionEffect(type,ticks,strength,false,true,true));}
    private void tell(Player p,String message){p.sendActionBar(Component.text(message,NamedTextColor.YELLOW));}

    void useItem(Player p,ItemStack item){String code=item.getItemMeta().getPersistentDataContainer().get(abilityKey,PersistentDataType.STRING);String prefix=character.apply(p).name()+":";if(code==null||!code.startsWith(prefix))throw new IllegalArgumentException("Eski karakter yeteneği kullanılamaz.");try{use(p,Integer.parseInt(code.substring(prefix.length())));}catch(NumberFormatException ex){throw new IllegalArgumentException("Geçersiz yetenek eşyası.");}}
    void use(Player p,int number) {
        if(!active(p))throw new IllegalArgumentException("Karakter yetenekleri sadece X Arena'da kullanılabilir.");
        if(number<1||number>4)throw new IllegalArgumentException("/arena yetenek 1, 2, 3 veya 4");
        UUID id=p.getUniqueId();int index=number-1;long now=System.currentTimeMillis();long[] ready=cooldowns.computeIfAbsent(id,k->new long[4]);ArenaCharacter kind=character.apply(p);ArenaCharacter.Skill skill=kind.skills.get(index);
        long available=Math.max(ready[index],sharedUntil.getOrDefault(id,0L));
        if(available>now)throw new IllegalArgumentException(skill.name()+" için "+((available-now+999)/1000)+" saniye bekle.");
        switch(kind) {
            case WARRIOR -> warrior(p,number);
            case ASSASSIN -> assassin(p,number);
            case TANK -> tank(p,number);
            case ARCHER -> archer(p,number);
            case MAGE -> mage(p,number);
            case HEALER -> healer(p,number);
        }
        ready[index]=now+skill.cooldownSeconds()*1000L;sharedUntil.put(id,now+skill.recoveryTicks()*50L);lastCombat.put(id,now);mayChoose.remove(id);protection.remove(id);updateSkills(p);tell(p,skill.name()+" kullanıldı • "+skill.cooldownSeconds()+" sn bekleme");
    }
    private void warrior(Player p,int skill){switch(skill){
        case 1->{Player target=target(p,4.5);hit(p,target,6);knock(p,target,0.35);}
        case 2->{List<Player> targets=around(p,p.getLocation(),4);requireTargets(targets);for(Player target:targets)hit(p,target,4);p.getWorld().spawnParticle(Particle.SWEEP_ATTACK,p.getLocation().add(0,1,0),12,2,0.4,2,0);}
        case 3->{warCry.put(p.getUniqueId(),System.currentTimeMillis()+5000);effect(p,PotionEffectType.GLOWING,100,0);}
        case 4->{List<Player> targets=around(p,p.getLocation(),5);requireTargets(targets);for(Player target:targets){hit(p,target,7);effect(target,PotionEffectType.SLOWNESS,40,1);knock(p,target,0.55);}p.getWorld().spawnParticle(Particle.EXPLOSION,p.getLocation().add(0,0.5,0),8,2,0,2,0);}
    }}
    private void assassin(Player p,int skill){switch(skill){
        case 1->{Vector direction=p.getLocation().getDirection().setY(0).normalize();p.setVelocity(direction.multiply(1.35).setY(0.2));effect(p,PotionEffectType.SPEED,35,1);later(p,4,()->{List<Player> targets=around(p,p.getLocation(),3);if(!targets.isEmpty())hit(p,targets.getFirst(),4);});}
        case 2->{Player target=target(p,8);UUID victim=target.getUniqueId();int life=lives.getOrDefault(victim,0);hit(p,target,2);for(int pulse=1;pulse<=3;pulse++)later(p,pulse*20L,()->{if(alive(victim,life))hit(p,target,1);});}
        case 3->{effect(p,PotionEffectType.INVISIBILITY,60,0);effect(p,PotionEffectType.SPEED,60,1);p.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,p.getLocation().add(0,1,0),18,0.7,0.6,0.7,0.01);}
        case 4->{Player target=target(p,4);hit(p,target,target.getHealth()<8?8:6);}
    }}
    private void tank(Player p,int skill){switch(skill){
        case 1->{shields.put(p.getUniqueId(),System.currentTimeMillis()+3000);effect(p,PotionEffectType.GLOWING,60,0);}
        case 2->{Player target=target(p,4);hit(p,target,4);knock(p,target,1.1);effect(target,PotionEffectType.SLOWNESS,30,1);}
        case 3->{heal(p,4);p.getWorld().spawnParticle(Particle.HEART,p.getLocation().add(0,1.7,0),6,0.5,0.4,0.5,0);}
        case 4->{List<Player> targets=around(p,p.getLocation(),5);requireTargets(targets);for(Player target:targets){hit(p,target,5);effect(target,PotionEffectType.SLOWNESS,60,1);}p.getWorld().spawnParticle(Particle.EXPLOSION,p.getLocation(),8,2,0.2,2,0);}
    }}
    private void archer(Player p,int skill){switch(skill){
        case 1->shoot(p,6,2.6);
        case 2->{Player target=target(p,18);Location center=target.getLocation().clone();for(int pulse=0;pulse<3;pulse++)later(p,pulse*12L,()->{p.getWorld().spawnParticle(Particle.CRIT,center.clone().add(0,2,0),30,2.5,1,2.5,0.15);for(Player victim:around(p,center,3))hit(p,victim,2);});}
        case 3->{p.setVelocity(p.getLocation().getDirection().setY(0).normalize().multiply(-1.2).setY(0.4));effect(p,PotionEffectType.SPEED,40,1);}
        case 4->shoot(p,8,3.2);
    }}
    private void mage(Player p,int skill){switch(skill){
        case 1->{Player target=target(p,16);hit(p,target,4.5);p.getWorld().spawnParticle(Particle.FLAME,target.getLocation().add(0,1,0),20,0.4,0.5,0.4,0.01);}
        case 2->{List<Player> targets=around(p,p.getLocation(),5);requireTargets(targets);for(Player target:targets){hit(p,target,3);effect(target,PotionEffectType.SLOWNESS,40,1);}p.getWorld().spawnParticle(Particle.SNOWFLAKE,p.getLocation().add(0,1,0),50,3,0.4,3,0.01);}
        case 3->blink(p);
        case 4->{Location center=target(p,18).getLocation().clone();p.getWorld().spawnParticle(Particle.FLAME,center.clone().add(0,3,0),30,1,1,1,0.02);later(p,16,()->{p.getWorld().spawnParticle(Particle.EXPLOSION,center.clone().add(0,1,0),6,1.5,0.5,1.5,0);for(Player target:around(p,center,3.5))hit(p,target,7);});}
    }}
    private void healer(Player p,int skill){switch(skill){
        case 1->{Player target=target(p,12);hit(p,target,3.5);p.getWorld().spawnParticle(Particle.END_ROD,target.getLocation().add(0,1,0),15,0.3,0.6,0.3,0.01);}
        case 2->{heal(p,4);p.getWorld().spawnParticle(Particle.HEART,p.getLocation().add(0,1.5,0),8,0.5,0.5,0.5,0);}
        case 3->{shields.put(p.getUniqueId(),System.currentTimeMillis()+3000);effect(p,PotionEffectType.GLOWING,60,0);}
        case 4->{heal(p,3);for(Player target:around(p,p.getLocation(),5))hit(p,target,4);p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,p.getLocation().add(0,1,0),40,3,0.4,3,0.01);}
    }}
    private void blink(Player p){Location origin=p.getLocation(),safe=null;Vector direction=origin.getDirection().setY(0);if(direction.lengthSquared()<0.01)throw new IllegalArgumentException("İleriye bakarak kullan.");direction.normalize();for(double distance=0.5;distance<=5;distance+=0.5){Location at=origin.clone().add(direction.clone().multiply(distance));if(!at.getBlock().isPassable()||!at.clone().add(0,1,0).getBlock().isPassable()||!at.getWorld().getWorldBorder().isInside(at))break;if(at.clone().subtract(0,1,0).getBlock().getType().isSolid())safe=at;}if(safe==null||safe.distanceSquared(origin)<1)throw new IllegalArgumentException("Önünde güvenli ışınlanma noktası yok.");if(!p.teleport(safe,org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN))throw new IllegalArgumentException("Işınlanma engellendi.");p.setFallDistance(0);}
    private void requireTargets(List<Player> targets){if(targets.isEmpty())throw new IllegalArgumentException("Bu yeteneğin menzilinde rakip yok.");}
    private Player target(Player p,double range){return around(p,p.getLocation(),range).stream().filter(other->{Vector delta=other.getEyeLocation().toVector().subtract(p.getEyeLocation().toVector());return delta.lengthSquared()<0.01||delta.normalize().dot(p.getEyeLocation().getDirection())>0.45;}).findFirst().orElseThrow(()->new IllegalArgumentException("Önünde, görüş alanında bir rakip olmalı."));}
    private List<Player> around(Player p,Location center,double radius){return center.getWorld().getNearbyPlayers(center,radius).stream().filter(other->other!=p&&active(other)&&p.hasLineOfSight(other)).sorted(Comparator.comparingDouble(other->other.getLocation().distanceSquared(center))).toList();}
    private void knock(Player p,Player target,double amount){if(!active(target))return;Vector delta=target.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);if(delta.lengthSquared()<0.01)delta=p.getLocation().getDirection().setY(0);target.setVelocity(delta.normalize().multiply(amount).setY(0.25));}
    private void hit(Player source,Player target,double damage){if(!active(source)||!active(target)||source==target)return;UUID id=target.getUniqueId();Intent previous=intents.put(id,new Intent(source.getUniqueId(),damage));try{target.damage(damage,source);}finally{if(previous==null)intents.remove(id);else intents.put(id,previous);}}
    private void shoot(Player p,double damage,double speed){Arrow arrow=p.launchProjectile(Arrow.class,p.getEyeLocation().getDirection().multiply(speed));arrow.setCritical(true);arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);arrow.getPersistentDataContainer().set(arrowDamageKey,PersistentDataType.DOUBLE,damage);track(p,arrow);}
    private void track(Player p,Projectile arrow){arrow.getPersistentDataContainer().set(arrowLifeKey,PersistentDataType.INTEGER,lives.getOrDefault(p.getUniqueId(),0));projectiles.computeIfAbsent(p.getUniqueId(),k->new HashSet<>()).add(arrow);later(p,100,()->{arrow.remove();projectiles.getOrDefault(p.getUniqueId(),new HashSet<>()).remove(arrow);});}

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void projectile(ProjectileLaunchEvent e){if(e.getEntity() instanceof AbstractArrow arrow&&arrow.getShooter() instanceof Player p&&active(p)){if(!arrow.getPersistentDataContainer().has(arrowDamageKey))arrow.getPersistentDataContainer().set(arrowDamageKey,PersistentDataType.DOUBLE,2.5);arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);track(p,arrow);}}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void arrowHit(ProjectileHitEvent e){Projectile arrow=e.getEntity();if(arrow.getPersistentDataContainer().has(arrowLifeKey)&&!(e.getHitEntity() instanceof Player)){arrow.remove();}}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void hunger(FoodLevelChangeEvent e){if(e.getEntity() instanceof Player p&&active(p)){e.setCancelled(true);p.setFoodLevel(20);}}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void regeneration(EntityRegainHealthEvent e){if(e.getEntity() instanceof Player p&&active(p)&&(e.getRegainReason()==EntityRegainHealthEvent.RegainReason.SATIATED||e.getRegainReason()==EntityRegainHealthEvent.RegainReason.REGEN))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void damage(EntityDamageEvent event){
        if(!(event.getEntity() instanceof Player victim)||!active(victim))return;
        UUID victimId=victim.getUniqueId();long now=System.currentTimeMillis();
        if(protection.getOrDefault(victimId,0L)>now){event.setCancelled(true);return;}
        if(!(event instanceof EntityDamageByEntityEvent e))return;
        Player attacker=e.getDamager() instanceof Player p?p:e.getDamager() instanceof Projectile projectile&&projectile.getShooter() instanceof Player p?p:null;
        if(attacker==null||!active(attacker))return;
        UUID attackerId=attacker.getUniqueId();Intent intent=intents.get(victimId);boolean skill=intent!=null&&intent.attacker().equals(attackerId);double base;
        if(skill)base=intent.damage();
        else if(e.getDamager() instanceof Projectile projectile){Integer life=projectile.getPersistentDataContainer().get(arrowLifeKey,PersistentDataType.INTEGER);if(life==null||life!=lives.getOrDefault(attackerId,0)){e.setCancelled(true);return;}base=projectile.getPersistentDataContainer().getOrDefault(arrowDamageKey,PersistentDataType.DOUBLE,4.0);}
        else{if(lastMelee.getOrDefault(attackerId,0L)+900>now||temporaryAbility(attacker.getInventory().getItemInMainHand())){e.setCancelled(true);return;}lastMelee.put(attackerId,now);base=character.apply(attacker).meleeDamage+marketBonus.apply(attacker);}
        mayChoose.remove(attackerId);mayChoose.remove(victimId);protection.remove(attackerId);lastCombat.put(attackerId,now);lastCombat.put(victimId,now);
        double desired=ArenaCharacter.finalDamage(base,character.apply(victim),warCry.getOrDefault(attackerId,0L)>now,shields.getOrDefault(victimId,0L)>now);
        if(!skill&&!(e.getDamager() instanceof Projectile))desired=Math.min(2.0,desired);
        setFinalDamage(e,desired);
    }
    private boolean temporaryAbility(ItemStack item){return item!=null&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(abilityKey);}
    private void updateSkills(Player p){UUID id=p.getUniqueId();long now=System.currentTimeMillis();long[] times=cooldowns.getOrDefault(id,new long[4]);List<String> status=new ArrayList<>();for(int index=0;index<4;index++){ItemStack item=p.getInventory().getItem(index);long ready=Math.max(times[index],sharedUntil.getOrDefault(id,0L));boolean available=ready<=now;if(temporaryAbility(item))item.editMeta(meta->meta.setEnchantmentGlintOverride(available));status.add((index+1)+": "+(available?"HAZIR":((ready-now+999)/1000)+" sn"));}tell(p,String.join(" • ",status));}
    @SuppressWarnings("deprecation") private void setFinalDamage(EntityDamageEvent event,double damage){event.setDamage(damage);for(EntityDamageEvent.DamageModifier modifier:EntityDamageEvent.DamageModifier.values())if(modifier!=EntityDamageEvent.DamageModifier.BASE&&modifier!=EntityDamageEvent.DamageModifier.ABSORPTION&&event.isApplicable(modifier))event.setDamage(modifier,0);if(event.isApplicable(EntityDamageEvent.DamageModifier.ABSORPTION))event.setDamage(EntityDamageEvent.DamageModifier.ABSORPTION,-Math.min(((Player)event.getEntity()).getAbsorptionAmount(),damage));}
}

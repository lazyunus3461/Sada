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
import java.util.function.Consumer;

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
        p.getInventory().setItem(4,weapon(Material.DIAMOND_SWORD,kind.label));
        if(kind==ArenaCharacter.ARCHER){p.getInventory().setItem(5,weapon(Material.BOW,"Okçu • Yay"));p.getInventory().setItem(9,new ItemStack(Material.ARROW,64));}
        boolean tank=kind==ArenaCharacter.TANK||tr.kasaba.hub.api.NetworkFeatures.isVip(p);
        p.getInventory().setHelmet(armor(tank?Material.NETHERITE_HELMET:Material.DIAMOND_HELMET));p.getInventory().setChestplate(armor(tank?Material.NETHERITE_CHESTPLATE:Material.DIAMOND_CHESTPLATE));p.getInventory().setLeggings(armor(tank?Material.NETHERITE_LEGGINGS:Material.DIAMOND_LEGGINGS));p.getInventory().setBoots(armor(tank?Material.NETHERITE_BOOTS:Material.DIAMOND_BOOTS));
        if(tr.kasaba.hub.api.NetworkFeatures.isVip(p)){ItemStack[] pieces=p.getInventory().getArmorContents();for(ItemStack piece:pieces)if(piece!=null)piece.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION,3);p.getInventory().setArmorContents(pieces);}
        for(int index=0;index<4;index++) {
            final int skill=index;ArenaCharacter.Skill spec=kind.skills.get(index);ItemStack item=new ItemStack(switch(index){case 0->Material.BLAZE_POWDER;case 1->Material.AMETHYST_SHARD;case 2->Material.PRISMARINE_CRYSTALS;default->Material.NETHER_STAR;});
            item.editMeta(meta->{meta.displayName(Component.text((skill+1)+" • "+spec.name(),NamedTextColor.GOLD));meta.lore(List.of(Component.text(spec.description(),NamedTextColor.GRAY),Component.text("Bekleme: "+tr.kasaba.hub.api.NetworkFeatures.cooldown(p,spec.cooldownSeconds())+" saniye",NamedTextColor.YELLOW),Component.text("Sağ tık veya /arena yetenek "+(skill+1),NamedTextColor.AQUA)));meta.getPersistentDataContainer().set(abilityKey,PersistentDataType.STRING,kind.name()+":"+(skill+1));});
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
        // Every accepted cast spends its cooldown, including a miss or blocked movement.
        int seconds=tr.kasaba.hub.api.NetworkFeatures.cooldown(p,skill.cooldownSeconds());ready[index]=now+seconds*1000L;sharedUntil.put(id,now+skill.recoveryTicks()*50L);lastCombat.put(id,now);mayChoose.remove(id);protection.remove(id);updateSkills(p);tell(p,skill.name()+" kullanıldı • "+seconds+" sn bekleme");
        switch(kind) {
            case WARRIOR -> warrior(p,number);
            case ASSASSIN -> assassin(p,number);
            case TANK -> tank(p,number);
            case ARCHER -> archer(p,number);
            case MAGE -> mage(p,number);
            case HEALER -> healer(p,number);
        }
    }
    private void warrior(Player p,int skill){switch(skill){
        case 1->strike(p,4.5,6,target->{hit(p,target,6);knock(p,target,0.35);});
        case 2->{Location center=p.getLocation().clone();warning(center,4,Particle.SWEEP_ATTACK);later(p,10,()->{for(Player target:around(p,center,4))hit(p,target,4);burst(center,Particle.SWEEP_ATTACK);});}
        case 3->{warCry.put(p.getUniqueId(),System.currentTimeMillis()+5000);effect(p,PotionEffectType.GLOWING,100,0);}
        case 4->{Location center=p.getLocation().clone();warning(center,5,Particle.FLAME);later(p,16,()->{for(Player target:around(p,center,5)){hit(p,target,7);effect(target,PotionEffectType.SLOWNESS,40,1);knock(p,target,0.55);}burst(center,Particle.EXPLOSION);});}
    }}
    private void assassin(Player p,int skill){switch(skill){
        case 1->{Vector direction=p.getLocation().getDirection().setY(0);if(direction.lengthSquared()>0.01)p.setVelocity(direction.normalize().multiply(1.35).setY(0.2));effect(p,PotionEffectType.SPEED,35,1);strike(p,4.5,4,target->hit(p,target,4));}
        case 2->bolt(p,8,0.8,Particle.CRIT,target->{UUID victim=target.getUniqueId();int life=lives.getOrDefault(victim,0);hit(p,target,2);for(int pulse=1;pulse<=3;pulse++)later(p,pulse*20L,()->{if(alive(victim,life))hit(p,target,1);});});
        case 3->{effect(p,PotionEffectType.INVISIBILITY,60,0);effect(p,PotionEffectType.SPEED,60,1);burst(p.getLocation(),Particle.CAMPFIRE_COSY_SMOKE);}
        case 4->strike(p,4,8,target->hit(p,target,target.getHealth()<8?8:6));
    }}
    private void tank(Player p,int skill){switch(skill){
        case 1->{shields.put(p.getUniqueId(),System.currentTimeMillis()+3000);effect(p,PotionEffectType.GLOWING,60,0);}
        case 2->strike(p,4,8,target->{hit(p,target,4);knock(p,target,1.1);effect(target,PotionEffectType.SLOWNESS,30,1);});
        case 3->{heal(p,4);burst(p.getLocation(),Particle.HEART);}
        case 4->{Location center=p.getLocation().clone();warning(center,5,Particle.CRIT);later(p,16,()->{for(Player target:around(p,center,5)){hit(p,target,5);effect(target,PotionEffectType.SLOWNESS,60,1);}burst(center,Particle.EXPLOSION);});}
    }}
    private void archer(Player p,int skill){switch(skill){
        case 1->shoot(p,6,2.6);
        case 2->{Location center=aim(p,18);warning(center,3,Particle.CRIT);for(int pulse=0;pulse<3;pulse++)later(p,16+pulse*12L,()->{burst(center,Particle.CRIT);for(Player victim:around(p,center,3))hit(p,victim,2);});}
        case 3->{Vector direction=p.getLocation().getDirection().setY(0);if(direction.lengthSquared()>0.01)p.setVelocity(direction.normalize().multiply(-1.2).setY(0.4));effect(p,PotionEffectType.SPEED,40,1);}
        case 4->shoot(p,8,3.2);
    }}
    private void mage(Player p,int skill){switch(skill){
        case 1->bolt(p,16,0.8,Particle.FLAME,target->hit(p,target,4.5));
        case 2->{Location center=p.getLocation().clone();warning(center,5,Particle.SNOWFLAKE);later(p,12,()->{for(Player target:around(p,center,5)){hit(p,target,3);effect(target,PotionEffectType.SLOWNESS,40,1);}burst(center,Particle.SNOWFLAKE);});}
        case 3->blink(p);
        case 4->{Location center=aim(p,18);warning(center,3.5,Particle.FLAME);later(p,20,()->{burst(center,Particle.EXPLOSION);for(Player target:around(p,center,3.5))hit(p,target,7);});}
    }}
    private void healer(Player p,int skill){switch(skill){
        case 1->bolt(p,12,0.8,Particle.END_ROD,target->hit(p,target,3.5));
        case 2->{heal(p,4);burst(p.getLocation(),Particle.HEART);}
        case 3->{shields.put(p.getUniqueId(),System.currentTimeMillis()+3000);effect(p,PotionEffectType.GLOWING,60,0);}
        case 4->{Location center=p.getLocation().clone();heal(p,3);warning(center,5,Particle.HAPPY_VILLAGER);later(p,12,()->{for(Player target:around(p,center,5))hit(p,target,4);burst(center,Particle.HAPPY_VILLAGER);});}
    }}
    /** Fixed aim, narrow collision and a visible wind-up; no player is locked as a target. */
    private void strike(Player p,double range,int windup,Consumer<Player> impact){Location origin=p.getEyeLocation().clone();Vector direction=origin.getDirection().normalize();for(double d=0.5;d<=range;d+=0.5)origin.getWorld().spawnParticle(Particle.CRIT,origin.clone().add(direction.clone().multiply(d)),1,0,0,0,0);later(p,windup,()->{var ray=origin.getWorld().rayTrace(origin,direction,range,FluidCollisionMode.NEVER,true,0.35,entity->entity instanceof Player other&&other!=p&&active(other));if(ray!=null&&ray.getHitEntity() instanceof Player target)impact.accept(target);});}
    /** Travelling, non-homing spell. Blocks stop it and opponents can sidestep it. */
    private void bolt(Player p,double range,double speed,Particle particle,Consumer<Player> impact){Location origin=p.getEyeLocation().clone();Vector direction=origin.getDirection().normalize();boolean[] done={false};origin.getWorld().spawnParticle(particle,origin,8,0.15,0.15,0.15,0);for(int step=0;step<Math.ceil(range/speed);step++){double distance=step*speed,length=Math.min(speed,range-distance);later(p,6+step,()->{if(done[0])return;Location from=origin.clone().add(direction.clone().multiply(distance));var ray=from.getWorld().rayTrace(from,direction,length,FluidCollisionMode.NEVER,true,0.3,entity->entity instanceof Player other&&other!=p&&active(other));Location visual=ray==null?from.clone().add(direction.clone().multiply(length)):ray.getHitPosition().toLocation(from.getWorld());from.getWorld().spawnParticle(particle,visual,4,0.08,0.08,0.08,0);if(ray!=null){done[0]=true;if(ray.getHitEntity() instanceof Player target)impact.accept(target);}});}}
    private Location aim(Player p,double range){Location eye=p.getEyeLocation();Vector direction=eye.getDirection().normalize();var hit=eye.getWorld().rayTraceBlocks(eye,direction,range,FluidCollisionMode.NEVER,true);Location center=hit==null?eye.clone().add(direction.multiply(range)):hit.getHitPosition().toLocation(eye.getWorld()).subtract(direction.multiply(0.25));int y=center.getBlockY();for(int dy=2;dy>=-8;dy--){Location floor=new Location(center.getWorld(),center.getX(),y+dy,center.getZ());if(floor.getBlock().getType().isSolid()&&floor.clone().add(0,1,0).getBlock().isPassable()){center.setY(y+dy+1);break;}}return center;}
    private void warning(Location center,double radius,Particle particle){for(int i=0;i<32;i++){double a=2*Math.PI*i/32;center.getWorld().spawnParticle(particle,center.clone().add(Math.cos(a)*radius,0.2,Math.sin(a)*radius),1,0,0,0,0);}center.getWorld().playSound(center,Sound.BLOCK_NOTE_BLOCK_PLING,0.7f,0.7f);}
    private void burst(Location center,Particle particle){center.getWorld().spawnParticle(particle,center.clone().add(0,1,0),12,1,0.5,1,0);}
    private void blink(Player p){Location origin=p.getLocation(),safe=null;Vector direction=origin.getDirection().setY(0);if(direction.lengthSquared()<0.01){tell(p,"Işın Adımı boşa gitti; ileriye bakarak kullan.");return;}direction.normalize();for(double distance=0.5;distance<=5;distance+=0.5){Location at=origin.clone().add(direction.clone().multiply(distance));if(!at.getBlock().isPassable()||!at.clone().add(0,1,0).getBlock().isPassable()||!at.getWorld().getWorldBorder().isInside(at))break;if(at.clone().subtract(0,1,0).getBlock().getType().isSolid())safe=at;}if(safe==null||safe.distanceSquared(origin)<1){tell(p,"Işın Adımı boşa gitti; önünde güvenli nokta yok.");return;}if(!p.teleport(safe,org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN)){tell(p,"Işın Adımı engellendi; bekleme süresi başladı.");return;}p.setFallDistance(0);}
    private List<Player> around(Player p,Location center,double radius){return center.getWorld().getNearbyPlayers(center,radius).stream().filter(other->other!=p&&active(other)&&other.getLocation().distanceSquared(center)<=radius*radius&&clearSight(center.clone().add(0,1,0),other.getEyeLocation())).sorted(Comparator.comparingDouble(other->other.getLocation().distanceSquared(center))).toList();}
    private boolean clearSight(Location from,Location to){Vector delta=to.toVector().subtract(from.toVector());double distance=delta.length();return distance<0.01||from.getWorld().rayTraceBlocks(from,delta.normalize(),distance,FluidCollisionMode.NEVER,true)==null;}
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
        if(tr.kasaba.hub.api.NetworkFeatures.isVip(victim))desired*=0.8;
        setFinalDamage(e,desired);
    }
    private boolean temporaryAbility(ItemStack item){return item!=null&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(abilityKey);}
    private void updateSkills(Player p){UUID id=p.getUniqueId();long now=System.currentTimeMillis();long[] times=cooldowns.getOrDefault(id,new long[4]);List<String> status=new ArrayList<>();for(int index=0;index<4;index++){ItemStack item=p.getInventory().getItem(index);long ready=Math.max(times[index],sharedUntil.getOrDefault(id,0L));boolean available=ready<=now;if(temporaryAbility(item))item.editMeta(meta->meta.setEnchantmentGlintOverride(available));status.add((index+1)+": "+(available?"HAZIR":((ready-now+999)/1000)+" sn"));}tell(p,String.join(" • ",status));}
    @SuppressWarnings("deprecation") private void setFinalDamage(EntityDamageEvent event,double damage){event.setDamage(damage);for(EntityDamageEvent.DamageModifier modifier:EntityDamageEvent.DamageModifier.values())if(modifier!=EntityDamageEvent.DamageModifier.BASE&&modifier!=EntityDamageEvent.DamageModifier.ABSORPTION&&event.isApplicable(modifier))event.setDamage(modifier,0);if(event.isApplicable(EntityDamageEvent.DamageModifier.ABSORPTION))event.setDamage(EntityDamageEvent.DamageModifier.ABSORPTION,-Math.min(((Player)event.getEntity()).getAbsorptionAmount(),damage));}
}

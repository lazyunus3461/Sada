package tr.kasabatr.esya;

import java.util.*;
import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.entity.*;
import org.bukkit.potion.*;

/** One isolated match; all transitions and disguise ownership remain on the main thread. */
final class PropRoom {
    enum Phase { HIDING,HUNTING,ENDING,CLOSED }
    final KasabaEsyaPlugin plugin;
    final int id;
    final ToyMap map;
    final boolean practice;
    final UUID resultId=UUID.randomUUID();
    final Set<UUID> players=new LinkedHashSet<>(),hiders=new LinkedHashSet<>(),seekers=new LinkedHashSet<>(),firstSeekers=new LinkedHashSet<>();
    final Map<UUID,BlockDisplay> displays=new HashMap<>();
    final Map<UUID,Interaction> hitboxes=new HashMap<>();
    final Map<UUID,UUID> hitboxOwner=new HashMap<>();
    final Map<UUID,Material> disguises=new HashMap<>();
    final Map<UUID,Long> guessCooldown=new HashMap<>(),changeCooldown=new HashMap<>();
    final Map<UUID,Location> lastPosition=new HashMap<>();
    final Map<UUID,Long> stillSince=new HashMap<>();
    final Set<ToyMap.Pos> activeDecoys=new HashSet<>();
    final BossBar bar=Bukkit.createBossBar("§dEşya Saklambacı",BarColor.PURPLE,BarStyle.SOLID);
    World world;
    Phase phase=Phase.HIDING;
    long startedAt,finishAt;
    int seconds,nextHint=65;
    PropRoom(KasabaEsyaPlugin plugin,int id,ToyMap map,boolean practice){this.plugin=plugin;this.id=id;this.map=map;this.practice=practice;}
    boolean active(){return phase==Phase.HIDING||phase==Phase.HUNTING;}
    boolean contains(Location at){return world!=null&&at.getWorld()==world&&map.contains(at);}
    void start(){
        activeDecoys.addAll(plugin.placeDecoys(map,world));List<UUID> order=new ArrayList<>(players);Collections.shuffle(order,plugin.random);
        int count=EsyaRules.seekerCount(order.size());
        // Assign every role before creating any disguise or sending player tracking packets.
        firstSeekers.addAll(order.subList(0,count));seekers.addAll(firstSeekers);hiders.addAll(order.subList(count,order.size()));
        int seat=0;
        for(UUID id:order){Player p=Bukkit.getPlayer(id);if(p==null||!plugin.auth(p))throw new IllegalArgumentException("Oyuncu ayrıldı veya giriş yapmamış.");
            boolean seeker=seekers.contains(id);plugin.prepare(p,seeker?map.seekerWait.at(world):map.hiderSpawns.get(seat++%map.hiderSpawns.size()).at(world));bar.addPlayer(p);plugin.tools(p);
            if(seeker)p.sendTitle("§c§lEBE","§e"+map.hideSeconds+" saniye sonra serbestsin",5,60,10);
            else{p.setAllowFlight(true);p.setFlySpeed(.055f);setDisguise(p,map.materials.get(plugin.random.nextInt(map.materials.size())));p.sendTitle("§d§lSAKLAN","§fPusuladan kılık seç; yerini bul",5,60,10);}
        }
        startedAt=System.currentTimeMillis();nextHint=map.hideSeconds+45;
        say("§e"+plugin.mapName(map)+" §8• §f"+map.hideSeconds+" saniye saklanma, ardından "+map.matchSeconds/60+" dakika arama.");
    }
    void tick(long now){
        if(phase==Phase.CLOSED)return;
        if(phase==Phase.ENDING){bar.setTitle("§aSonuçlar §8• §eLobiye "+Math.max(0,(finishAt-now+999)/1000)+" sn");if(now>=finishAt)close();return;}
        seconds=(int)Math.max(0,(now-startedAt)/1000);
        for(UUID id:List.copyOf(players)){
            Player p=Bukkit.getPlayer(id);if(p==null||!p.isOnline()){plugin.abandon(id);plugin.leave(id);continue;}
            if(!contains(p.getLocation())){plugin.abandon(id);plugin.toLobby(p);continue;}
            if(p.isDead()){plugin.toLobby(p);continue;}
            if(hiders.contains(id)){p.setAllowFlight(true);p.setFlySpeed(.055f);align(p,now);}else{p.setFlying(false);p.setAllowFlight(false);}p.setGravity(true);p.setCollidable(false);
            if(hiders.contains(id))follow(p,p.getLocation());
        }
        if(!active())return;
        if(phase==Phase.HIDING&&seconds>=map.hideSeconds){phase=Phase.HUNTING;bar.setColor(BarColor.RED);for(UUID id:seekers){Player p=Bukkit.getPlayer(id);if(p!=null)plugin.teleport(p,map.seekerRelease.at(world));}say("§c§lEBELER SERBEST! §fEşyaları tahmin ederek saklananları bul.");}
        if(phase==Phase.HUNTING&&seconds>=nextHint&&!hiders.isEmpty()){
            nextHint=seconds+45;UUID id=hiders.stream().skip(plugin.random.nextInt(hiders.size())).findFirst().orElseThrow();Player hidden=Bukkit.getPlayer(id);
            if(hidden!=null)for(UUID seekerId:seekers){Player p=Bukkit.getPlayer(seekerId);if(p!=null){int x=Math.floorDiv(hidden.getLocation().getBlockX(),8)*8,z=Math.floorDiv(hidden.getLocation().getBlockZ(),8)*8;plugin.tell(p,"§eİpucu: bir saklanan yaklaşık X "+x+" / Z "+z+" çevresinde.");p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_BELL,.6f,1.4f);}}
        }
        int remaining=phase==Phase.HIDING?Math.max(0,map.hideSeconds-seconds):Math.max(0,map.matchSeconds-(seconds-map.hideSeconds));
        bar.setTitle((practice?"§eDENEME §8• ":"")+(phase==Phase.HIDING?"§dSaklanma: ":"§cArama: ")+"§e"+remaining+" sn §8• §a"+hiders.size()+" saklanan §8/ §c"+seekers.size()+" ebe");bar.setProgress(Math.clamp(remaining/(double)(phase==Phase.HIDING?map.hideSeconds:map.matchSeconds),0,1));
        if(phase==Phase.HUNTING&&remaining==0)finish(false,"Süre doldu; saklananlar kazandı.");
    }
    void setDisguise(Player p,Material material){
        UUID id=p.getUniqueId();if(!active()||!hiders.contains(id)||!map.materials.contains(material))throw new IllegalArgumentException("Bu kılık şu anda seçilemez.");
        removeProp(id);disguises.put(id,material);Location at=propLocation(p.getLocation());
        BlockDisplay prop=world.spawn(at,BlockDisplay.class,e->{e.setBlock(material.createBlockData());e.setPersistent(false);e.setTeleportDuration(1);});
        Interaction box=world.spawn(at.clone().add(.5,0,.5),Interaction.class,e->{e.setInteractionWidth(1);e.setInteractionHeight(1);e.setResponsive(true);e.setPersistent(false);});
        displays.put(id,prop);hitboxes.put(id,box);hitboxOwner.put(box.getUniqueId(),id);
        for(Player viewer:Bukkit.getOnlinePlayers())if(viewer!=p)viewer.hidePlayer(plugin,p);
        plugin.tell(p,"§dKılığın: §f"+KasabaEsyaPlugin.label(material)+" §8• §ePusula veya /esya kılık");
    }
    // Slow flight is available only to hidden players, within three blocks above the map floor.
    boolean canFloat(Location at){return contains(at)&&at.getY()>=map.minY+2&&at.getY()<=map.minY+5.01&&clear(at);}
    boolean clear(Location at){for(double dx:new double[]{-.29,.29})for(double dz:new double[]{-.29,.29})for(double dy:new double[]{.01,.9,1.79})if(!world.getBlockAt((int)Math.floor(at.getX()+dx),(int)Math.floor(at.getY()+dy),(int)Math.floor(at.getZ()+dz)).isPassable())return false;return true;}
    void align(Player p,long now){
        UUID id=p.getUniqueId();Location at=p.getLocation(),last=lastPosition.put(id,at.clone());
        if(!p.isFlying()||last==null||last.distanceSquared(at)>.0004){stillSince.put(id,now);return;}
        if(now-stillSince.getOrDefault(id,now)<350)return;
        Location aligned=at.clone();aligned.setX(at.getBlockX()+.5);aligned.setZ(at.getBlockZ()+.5);aligned.setY(Math.round(at.getY()));
        if(Math.abs(aligned.getY()-at.getY())>.45||!canFloat(aligned))return;
        int x=aligned.getBlockX(),y=aligned.getBlockY(),z=aligned.getBlockZ();boolean beside=false;
        for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})if(world.getBlockAt(x+d[0],y,z+d[1]).getType().isSolid())beside=true;
        if(beside&&aligned.distanceSquared(at)>.0001){plugin.teleport(p,aligned);p.setFlying(true);p.setVelocity(new org.bukkit.util.Vector());follow(p,aligned);lastPosition.put(id,aligned.clone());}
    }
    Location propLocation(Location at){return new Location(world,at.getBlockX(),at.getBlockY(),at.getBlockZ());}
    void follow(Player p,Location at){BlockDisplay prop=displays.get(p.getUniqueId());Interaction box=hitboxes.get(p.getUniqueId());if(prop==null||box==null||at.getWorld()!=world)return;Location next=propLocation(at);if(prop.getLocation().distanceSquared(next)>.01){prop.teleport(next);box.teleport(next.clone().add(.5,0,.5));}}
    void removeProp(UUID id){BlockDisplay prop=displays.remove(id);if(prop!=null)prop.remove();Interaction box=hitboxes.remove(id);if(box!=null){hitboxOwner.remove(box.getUniqueId());box.remove();}disguises.remove(id);}
    void change(Player p,Material material){
        if(!hiders.contains(p.getUniqueId())||!active())throw new IllegalArgumentException("Yalnız saklananlar kılık seçebilir.");
        long now=System.currentTimeMillis(),until=changeCooldown.getOrDefault(p.getUniqueId(),0L);
        if(until>now)throw new IllegalArgumentException("Kılık için "+((until-now+999)/1000)+" saniye bekle.");
        if(material==disguises.get(p.getUniqueId())){plugin.tell(p,"Zaten bu kılıktasın.");return;}
        setDisguise(p,material);changeCooldown.put(p.getUniqueId(),now+20000);
    }
    void guess(Player seeker,UUID target){
        UUID id=seeker.getUniqueId();if(phase!=Phase.HUNTING||!seekers.contains(id)||seeker.getWorld()!=world)return;
        long now=System.currentTimeMillis();if(guessCooldown.getOrDefault(id,0L)>now)return;
        if(target!=null){
            Player hidden=Bukkit.getPlayer(target);if(hidden==null||!hiders.contains(target)||hidden.getWorld()!=world)return;
            Location center=propLocation(hidden.getLocation()).add(.5,.5,.5),eye=seeker.getEyeLocation();var vector=center.toVector().subtract(eye.toVector());double length=vector.length();
            if(length>4.5||length>.05&&world.rayTraceBlocks(eye,vector.normalize(),Math.max(0,length-.05),FluidCollisionMode.NEVER,true)!=null)return;
            guessCooldown.put(id,now+1200);removeProp(target);hiders.remove(target);seekers.add(target);plugin.reveal(hidden);plugin.prepare(hidden,hidden.getLocation());plugin.tools(hidden);
            say("§c"+seeker.getName()+" §f→ §e"+hidden.getName()+" §fbulundu! Ebelere yardım edecek.");hidden.playSound(hidden.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,.8f,1.3f);
            if(hiders.isEmpty())finish(true,"Bütün saklananlar bulundu; ebeler kazandı.");
        }else{guessCooldown.put(id,now+1200);seeker.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,60,1,false,false));seeker.sendActionBar(tr.kasaba.hub.api.MenuStyle.line("§cYanlış tahmin! §e3 saniye yavaşladın."));}
    }
    void remove(UUID id){
        players.remove(id);hiders.remove(id);seekers.remove(id);firstSeekers.remove(id);removeProp(id);guessCooldown.remove(id);changeCooldown.remove(id);lastPosition.remove(id);stillSince.remove(id);Player p=Bukkit.getPlayer(id);if(p!=null){bar.removePlayer(p);plugin.reveal(p);}
        if(plugin.stopping||!active()){if(players.isEmpty())close();return;}
        if(players.isEmpty())close();else if(hiders.isEmpty())finish(true,"Saklanan kalmadı; ebeler kazandı.");else if(seekers.isEmpty())finish(false,"Ebe kalmadı; saklananlar kazandı.");
    }
    void finish(boolean seekersWin,String reason){
        if(!active())return;phase=Phase.ENDING;finishAt=System.currentTimeMillis()+5000;
        if(!practice&&!plugin.stopping)plugin.hub.recordGameResult(plugin,KasabaEsyaPlugin.ID,resultId,EsyaRules.winners(seekersWin,firstSeekers,hiders,players));
        say("§6"+reason);for(UUID id:List.copyOf(players)){removeProp(id);Player p=Bukkit.getPlayer(id);if(p!=null){plugin.reveal(p);p.setFlying(false);p.setAllowFlight(false);p.setFlySpeed(.1f);p.sendTitle(seekersWin?"§c§lEBELER KAZANDI":"§a§lSAKLANANLAR KAZANDI","§e5 saniye sonra Eşya Saklambacı lobisi",5,70,10);}}
        bar.setColor(BarColor.GREEN);bar.setProgress(1);
    }
    void close(){
        if(phase==Phase.CLOSED)return;phase=Phase.CLOSED;bar.removeAll();plugin.rooms.remove(id,this);
        for(UUID playerId:List.copyOf(players)){plugin.membership.remove(playerId,this);removeProp(playerId);Player p=Bukkit.getPlayer(playerId);if(p!=null){plugin.reveal(p);if(!plugin.stopping&&plugin.visitors.contains(playerId))plugin.returnGameLobby(p,0);}}
        players.clear();hiders.clear();seekers.clear();if(world!=null)plugin.closeWhenEmpty(world,0);
    }
    void say(String text){for(UUID id:players){Player p=Bukkit.getPlayer(id);if(p!=null)plugin.tell(p,text);}}
}

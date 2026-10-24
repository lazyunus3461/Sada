package tr.kasabatr.ispanya;

import java.io.IOException;
import java.util.*;
import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

final class GameRoom {
    final KasabaIspanyaPlugin plugin;final int id;
    final LinkedHashSet<UUID> members=new LinkedHashSet<>();
    final Map<UUID,Integer> positions=new LinkedHashMap<>(),points=new LinkedHashMap<>(),skippedTurns=new HashMap<>(),missedRolls=new HashMap<>();
    final List<UUID> finished=new ArrayList<>();final Map<UUID,Scoreboard> previousBoards=new HashMap<>();
    final Random random=new Random();final BossBar bar;final Scoreboard board;final Objective sidebar;
    UUID owner,current,movingId,pendingShortcut;World world;BoardLayout layout;
    boolean active,closing,crossing,effectMove,skipping,ending;int turnLeft,matchLeft,waitLeft=-1,effectSeconds,endCountdown;
    int movementTarget,movementStart,movementSubstep,bridgeFrom,bridgeTo,bridgeStep;Runnable pendingEffect;String endReason;
    BukkitTask movementTask;
    GameRoom(KasabaIspanyaPlugin plugin,int id,UUID owner){this.plugin=plugin;this.id=id;this.owner=owner;bar=Bukkit.createBossBar("§6Şans Yolu §7• Oda "+id,BarColor.YELLOW,BarStyle.SOLID);board=Bukkit.getScoreboardManager().getNewScoreboard();sidebar=board.registerNewObjective("ispanya","dummy","§6Şans Yolu");sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);}
    void start()throws IOException{
        if(active||closing)throw new IOException("Maç zaten başladı.");if(members.size()<plugin.minimum())throw new IOException("En az "+plugin.minimum()+" oyuncu gerekli.");for(UUID id:members){Player p=Bukkit.getPlayer(id);if(p==null||!plugin.auth(p))throw new IOException("Grupta giriş yapmamış oyuncu var.");}
        world=plugin.instances().create(id);active=true;
        try{layout=BoardLayout.create(plugin.track().tiles().stream().map(t->new BoardLayout.Node(t.x(),t.z(),t.kind())).toList(),plugin.track().shortcuts().size(),random);buildLayout();
            for(UUID id:members){Player p=Bukkit.getPlayer(id);positions.put(id,0);points.put(id,0);previousBoards.put(id,p.getScoreboard());plugin.prepare(p,plugin.track().tile(0).location(world),true);p.setScoreboard(board);bar.addPlayer(p);}
            matchLeft=Math.clamp(plugin.getConfig().getInt("match-max-minutes",25),1,120)*60;beginTurnAfter(null);updateSidebar();announce("Maç başladı! Renkler ve kestirmeler yeniden karıştırıldı. Zar otomatik atılmaz; maçta 3 kez zar atlamada lobiye dönersin.");
        }catch(Exception e){plugin.finish(this,"Maç başlatılamadı.",false);throw new IOException("Maç başlatılamadı.",e);}
    }
    private void buildLayout(){
        for(var link:plugin.track().shortcuts().entrySet()){Location a=plugin.track().tile(link.getKey()).location(world),b=plugin.track().tile(link.getValue()).location(world);int length=(int)Math.round(Math.abs(b.getZ()-a.getZ())),direction=b.getZ()>a.getZ()?1:-1;for(int d=2;d<length-1;d++)for(int dx=-2;dx<=2;dx++)for(int y=65;y<=69;y++)world.getBlockAt(a.getBlockX()+dx,y,a.getBlockZ()+direction*d).setType(Material.AIR,false);}
        for(int i=1;i<plugin.track().size()-1;i++){Location at=plugin.track().tile(i).location(world);Material color=switch(layout.kinds.get(i)){case "red"->Material.RED_CONCRETE;case "green"->Material.LIME_CONCRETE;case "purple"->Material.PURPLE_CONCRETE;case "yellow"->Material.YELLOW_CONCRETE;case "black"->Material.BLACK_CONCRETE;default->Material.GRAY_TERRACOTTA;};for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)world.getBlockAt(at.getBlockX()+dx,at.getBlockY()-1,at.getBlockZ()+dz).setType(color,false);}
        for(var link:layout.shortcuts.entrySet()){Location a=plugin.track().tile(link.getKey()).location(world),b=plugin.track().tile(link.getValue()).location(world);int length=(int)Math.round(Math.abs(b.getZ()-a.getZ())),direction=b.getZ()>a.getZ()?1:-1;for(int d=2;d<=length-2;d++){int rise=Math.max(0,Math.min(2,Math.min(d-1,length-d-1)));world.getBlockAt(a.getBlockX(),65+rise,a.getBlockZ()+direction*d).setType(Material.YELLOW_STAINED_GLASS,false);}}
    }
    private boolean racing(UUID id){return members.contains(id)&&!finished.contains(id)&&Bukkit.getPlayer(id)!=null;}
    private void beginTurnAfter(UUID after){
        if(closing||ending)return;List<UUID> seats=new ArrayList<>(members);if(seats.stream().noneMatch(this::racing)){beginEnding("Tüm oyuncular bitirdi.");return;}int start=after==null?0:Math.floorMod(seats.indexOf(after)+1,seats.size());
        for(int offset=0;offset<seats.size();offset++){UUID candidate=seats.get((start+offset)%seats.size());if(!racing(candidate))continue;current=candidate;
            if(skippedTurns.getOrDefault(candidate,0)>0){skipping=true;turnLeft=5;announce("§c"+name(candidate)+" bu tur kötü şans yüzünden 5 saniye bekliyor; zar atamaz.");title(candidate,"§cBU TUR BEKLİYORSUN","§fKötü şans • 5 saniye sonra sıra geçecek");updateBar();return;}
            skipping=false;turnLeft=Math.clamp(plugin.getConfig().getInt("turn-seconds",25),5,120);announce("Sıra: §e"+name(current)+" §f• "+turnLeft+" saniye • Zar atlaman: "+missedRolls.getOrDefault(current,0)+"/3");Player p=Bukkit.getPlayer(current);p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_BELL,1,1.2f);updateBar();return;
        }beginEnding("Yarışacak oyuncu kalmadı.");
    }
    void tick(){
        if(closing)return;if(ending){bar.setTitle("§6Sonuçlar §7• Şans Yolu lobisine "+endCountdown+" sn");bar.setProgress(Math.clamp(endCountdown/10.0,0,1));if(--endCountdown<=0)plugin.finish(this,endReason,true);return;}
        if(!active){if(members.size()<plugin.minimum()){waitLeft=-1;return;}if(waitLeft<0){waitLeft=Math.clamp(plugin.getConfig().getInt("countdown-seconds",15),3,60);announce("Maç "+waitLeft+" saniye sonra başlayacak.");}else if(--waitLeft<=0)try{start();}catch(IOException e){waitLeft=-1;announce("Başlatılamadı: "+e.getMessage());}return;}
        if(--matchLeft<=0){beginEnding("Süre doldu.");return;}
        if(pendingEffect!=null){if(--effectSeconds<=0){Runnable effect=pendingEffect;pendingEffect=null;effect.run();}updateSidebar();return;}
        if(movingId==null){
            if(--turnLeft<=0){UUID id=current;
                if(skipping){skipping=false;skippedTurns.computeIfPresent(id,(k,v)->v>1?v-1:null);announce(name(id)+" beklemesini tamamladı; sıra geçti.");beginTurnAfter(id);}
                else if(pendingShortcut!=null){pendingShortcut=null;Player p=Bukkit.getPlayer(id);if(p!=null)plugin.removeShortcut(p);announce(name(id)+" kestirme çubuğunu kullanmadı; hakkı bu tur geçti.");beginTurnAfter(id);}
                else{int missed=missedRolls.merge(id,1,Integer::sum);announce(name(id)+" zar atmadı: "+missed+"/3. Otomatik zar atılmadı.");if(missed>=3){Player p=Bukkit.getPlayer(id);if(p!=null)plugin.inactiveLobby(p);else remove(id);}else beginTurnAfter(id);}
            }if(movingId==null&&!ending)updateBar();
        }updateSidebar();
    }
    boolean roll(UUID id,boolean automatic){
        if(automatic||!active||closing||ending||skipping||pendingEffect!=null||pendingShortcut!=null||movingId!=null||id==null||!id.equals(current)||!racing(id))return false;
        int first=random.nextInt(6)+1,second=random.nextInt(6)+1,start=positions.getOrDefault(id,0),target=RaceRules.destination(start,first,second,plugin.track().size()-1,plugin.getConfig().getBoolean("finish-on-exact-roll"));announce("Zar: "+name(id)+" §e"+first+"+"+second+" §f• hedef kare "+(target+1)+"/"+plugin.track().size());if(target==start){announce("Tam sayı gelmedi; yerinde kaldın.");beginTurnAfter(id);return true;}startMove(id,target,false);return true;
    }
    private void startMove(UUID id,int target,boolean effect){movingId=id;movementStart=positions.getOrDefault(id,0);movementTarget=target;movementSubstep=0;crossing=false;effectMove=effect;bar.setTitle("§6Şans Yolu §7• §e"+name(id)+(target<movementStart?" §cgeri gidiyor":" §filerliyor"));bar.setProgress(0);scheduleMovement();}
    private void scheduleMovement(){movementTask=Bukkit.getScheduler().runTaskTimer(plugin,()->{try{moveStep();}catch(RuntimeException e){plugin.getLogger().log(java.util.logging.Level.SEVERE,"Şans Yolu hareketi durduruldu.",e);plugin.finish(this,"Harita hareketi engellendi.",false);}},0,2);}
    void moveStep(){
        if(!active||closing||ending||movingId==null){stopAnimation();return;}Player p=Bukkit.getPlayer(movingId);if(p==null){UUID left=movingId;stopAnimation();remove(left);return;}
        if(crossing){double progress=++bridgeStep/20.0;Location a=plugin.track().tile(bridgeFrom).location(world),b=plugin.track().tile(bridgeTo).location(world);double distance=a.distance(b);Location point=a.clone().add(b.toVector().subtract(a.toVector()).multiply(progress));point.add(0,Math.max(0,Math.min(2,Math.min(distance*progress-1,distance*(1-progress)-1))),0);point.setDirection(b.toVector().subtract(a.toVector()));plugin.teleport(p,point);bar.setProgress(progress);
            if(bridgeStep>=20){UUID id=movingId;positions.put(id,bridgeTo);stopAnimation();plugin.removeShortcut(p);announce("§e"+name(id)+" kestirmeden geçti! Yeni konum: "+(bridgeTo+1)+". kare. Kestirme çubuğu kullanıldı.");title(id,"§eKESTİRMEDEN GEÇTİN","§fKestirme çubuğun kullanıldı");finishIfReached(id);beginTurnAfter(id);}return;
        }
        int tile=positions.getOrDefault(movingId,0);if(tile==movementTarget){completeMovement();return;}int next=tile+Integer.signum(movementTarget-tile);Location a=plugin.track().tile(tile).location(world),b=plugin.track().tile(next).location(world);double progress=++movementSubstep/8.0;Location point=a.clone().add(b.toVector().subtract(a.toVector()).multiply(progress));point.setDirection(b.toVector().subtract(a.toVector()));plugin.teleport(p,point);
        if(movementSubstep==8){movementSubstep=0;positions.put(movingId,next);bar.setProgress(Math.clamp(Math.abs(next-movementStart)/(double)Math.max(1,Math.abs(movementTarget-movementStart)),0,1));if(next==movementTarget)completeMovement();}
    }
    private void completeMovement(){UUID id=movingId;if(id==null)return;boolean wasEffect=effectMove;stopAnimation();Player p=Bukkit.getPlayer(id);if(p!=null)p.playSound(p.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,.7f,1.3f);boolean done=finishIfReached(id);if(!done&&!wasEffect)applyEffect(id);if(movingId==null&&pendingEffect==null&&pendingShortcut==null&&!closing)beginTurnAfter(id);}
    private boolean finishIfReached(UUID id){if(positions.getOrDefault(id,0)<plugin.track().size()-1)return false;if(!finished.contains(id)){finished.add(id);skippedTurns.remove(id);announce("§6"+finished.size()+". sırada bitirdi: "+name(id));title(id,"§6BİTİRDİN!","§fDiğer oyuncuların bitişini bekle");}return true;}
    private void delayEffect(int seconds,Runnable action){effectSeconds=seconds;pendingEffect=action;}
    private void applyEffect(UUID id){
        int tile=positions.get(id);String kind=layout.kinds.get(tile),effect=plugin.getConfig().getString("tile-effects."+kind,"none").trim().toLowerCase(Locale.ROOT);
        if(effect.equals("bad_luck")){if(random.nextBoolean()){skippedTurns.merge(id,1,Integer::sum);announce("§cKötü şans! "+name(id)+" sonraki tur zar atamayacak.");title(id,"§cKÖTÜ ŞANS","§fSonraki tur 5 saniye bekleyeceksin");delayEffect(2,()->beginTurnAfter(id));return;}effect="back:"+Math.clamp(plugin.getConfig().getInt("red-back-steps",3),1,12);}
        if(effect.equals("random")){int step=random.nextInt(Math.clamp(plugin.getConfig().getInt("purple-random-max-steps",6),1,12))+1;effect=(random.nextBoolean()?"advance:":"back:")+step;}
        if(effect.equals("shortcut")){Integer to=layout.shortcuts.get(tile);if(to==null)return;pendingShortcut=id;bridgeFrom=tile;bridgeTo=to;turnLeft=Math.clamp(plugin.getConfig().getInt("turn-seconds",25),5,120);Player p=Bukkit.getPlayer(id);plugin.giveShortcut(p);announce("§eKestirme geldi! "+name(id)+" sarı karede kestirme çubuğu kazandı; sağ tıkla köprüden geç.");title(id,"§eKESTİRME GELDİ!","§fElindeki çubuğa sağ tıkla • /sansyolu kestirme");return;}
        try{if(effect.startsWith("score:")){int amount=Integer.parseInt(effect.substring(6));points.merge(id,amount,Integer::sum);announce(name(id)+" puan değişimi: "+amount);}
            else if(effect.startsWith("advance:")||effect.startsWith("back:")){int steps=Math.clamp(Integer.parseInt(effect.substring(effect.indexOf(':')+1)),0,plugin.track().size()),change=effect.startsWith("back:")?-steps:steps,target=RaceRules.effectDestination(tile,change,plugin.track().size()-1);announce((change<0?"§cKötü şans! ":"§aİyi şans! ")+name(id)+" "+steps+" kare "+(change<0?"geri":"ileri")+" gidecek; 2 saniye sonra hareket edecek.");title(id,change<0?"§cKÖTÜ ŞANS":"§aİYİ ŞANS",steps+" kare "+(change<0?"geri gideceksin":"ileri gideceksin"));delayEffect(2,()->startMove(id,target,true));}
        }catch(NumberFormatException e){plugin.getLogger().warning("Geçersiz kare etkisi: "+effect);}
    }
    boolean useShortcut(UUID id){if(closing||ending||!active||!Objects.equals(pendingShortcut,id)||!Objects.equals(current,id)||movingId!=null)return false;pendingShortcut=null;movingId=id;crossing=true;bridgeStep=0;bar.setTitle("§e"+name(id)+" kestirmeden geçiyor");bar.setProgress(0);scheduleMovement();return true;}
    private void beginEnding(String reason){if(ending||closing)return;ending=true;endReason=reason;endCountdown=10;stopAnimation();pendingEffect=null;pendingShortcut=null;for(UUID id:members){Player p=Bukkit.getPlayer(id);if(p!=null){plugin.removeShortcut(p);p.sendTitle("§6MAÇ BİTTİ","§f10 saniye sonra Şans Yolu lobisine döneceksin",5,120,10);}}announce(reason+" Sıralama: "+ranking()+" • 10 saniye sonra oyun lobisine dönülecek.");}
    void stopAnimation(){if(movementTask!=null)movementTask.cancel();movementTask=null;movingId=null;crossing=false;}
    void remove(UUID id){if(!members.contains(id))return;List<UUID> order=new ArrayList<>(members);UUID predecessor=order.get(Math.floorMod(order.indexOf(id)-1,order.size()));boolean turn=id.equals(current);if(id.equals(movingId))stopAnimation();if(turn){pendingEffect=null;pendingShortcut=null;skipping=false;}members.remove(id);positions.remove(id);points.remove(id);skippedTurns.remove(id);missedRolls.remove(id);finished.remove(id);Player p=Bukkit.getPlayer(id);if(p!=null){plugin.removeShortcut(p);bar.removePlayer(p);restoreBoard(p);}if(id.equals(owner)&&!members.isEmpty())owner=members.iterator().next();if(members.isEmpty())plugin.finish(this,"Oyuncu kalmadı.",false);else if(active&&turn&&!ending)beginTurnAfter(predecessor);}
    List<UUID> orderedFinish(){var result=new ArrayList<>(finished);members.stream().filter(id->!result.contains(id)).sorted(Comparator.<UUID>comparingInt(id->positions.getOrDefault(id,0)).reversed().thenComparing(Comparator.<UUID>comparingInt(id->points.getOrDefault(id,0)).reversed())).forEach(result::add);return List.copyOf(result);}
    String ranking(){var ranks=new ArrayList<String>();List<UUID> ids=orderedFinish();for(int i=0;i<ids.size();i++)ranks.add((i+1)+". "+name(ids.get(i))+" (kare "+(positions.getOrDefault(ids.get(i),0)+1)+", puan "+points.getOrDefault(ids.get(i),0)+")");return String.join(" §7| §f",ranks);}
    void restoreBoard(Player p){Scoreboard old=previousBoards.remove(p.getUniqueId());if(old!=null)p.setScoreboard(old);}
    void announce(String message){for(UUID id:members){Player p=Bukkit.getPlayer(id);if(p!=null)plugin.tell(p,"§7[Oda "+this.id+"] §f"+message);}}
    private String name(UUID id){Player p=Bukkit.getPlayer(id);return p==null?id.toString().substring(0,8):p.getName();}
    private void title(UUID id,String title,String text){Player p=Bukkit.getPlayer(id);if(p!=null)p.sendTitle(title,text,3,50,5);}
    private void updateSidebar(){for(String entry:board.getEntries())board.resetScores(entry);int rank=1;for(UUID id:orderedFinish()){sidebar.getScore("§e"+rank+". §f"+name(id)+" §7"+(positions.getOrDefault(id,0)+1)+"/"+plugin.track().size()+" §a"+points.getOrDefault(id,0)).setScore(5-rank);rank++;}}
    private void updateBar(){if(current==null)return;bar.setTitle("§6Şans Yolu §7• §e"+name(current)+(skipping?" §cbekliyor":pendingShortcut!=null?" §ekestirme çubuğunu kullan":" §fzar atmalı")+" §7("+Math.max(0,turnLeft)+" sn)");bar.setProgress(Math.clamp(turnLeft/(double)Math.clamp(plugin.getConfig().getInt("turn-seconds",25),5,120),0,1));}
}

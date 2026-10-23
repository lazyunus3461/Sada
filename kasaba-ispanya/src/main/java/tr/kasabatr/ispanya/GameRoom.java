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
    final Map<UUID,Integer> positions=new LinkedHashMap<>(),points=new LinkedHashMap<>(),skippedTurns=new HashMap<>();
    final List<UUID> finished=new ArrayList<>();final Map<UUID,Scoreboard> previousBoards=new HashMap<>();
    final Random random=new Random();final BossBar bar;final Scoreboard board;final Objective sidebar;
    UUID owner,current,movingId;World world;boolean active,closing;int turnLeft,matchLeft,waitLeft=-1;
    int movementTarget,movementStart,movementSubstep,bridgeFrom,bridgeTo,bridgeStep;boolean crossing;
    BukkitTask movementTask;
    GameRoom(KasabaIspanyaPlugin plugin,int id,UUID owner){
        this.plugin=plugin;this.id=id;this.owner=owner;
        bar=Bukkit.createBossBar("§6İspanya Adası §7• §fOda "+id,BarColor.YELLOW,BarStyle.SOLID);
        board=Bukkit.getScoreboardManager().getNewScoreboard();sidebar=board.registerNewObjective("ispanya","dummy","§6İspanya Adası");sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
    }
    void start()throws IOException{
        if(active||closing)throw new IOException("Maç zaten başladı.");
        if(members.size()<plugin.minimum())throw new IOException("En az "+plugin.minimum()+" oyuncu gerekli.");
        for(UUID id:members){Player p=Bukkit.getPlayer(id);if(p==null||!plugin.auth(p))throw new IOException("Grupta giriş yapmamış oyuncu var.");}
        world=plugin.instances().create(id);active=true;
        try{
            for(UUID id:members){Player p=Bukkit.getPlayer(id);positions.put(id,0);points.put(id,0);previousBoards.put(id,p.getScoreboard());plugin.prepare(p,plugin.track().tile(0).location(world),true);p.setScoreboard(board);bar.addPlayer(p);}
            matchLeft=Math.clamp(plugin.getConfig().getInt("match-max-minutes",25),1,120)*60;
            beginTurnAfter(null);updateSidebar();announce("Maç başladı! Sıran gelince zar eşyasına sağ tıkla veya /ispanya zar yaz.");
        }catch(Exception e){plugin.finish(this,"Maç başlatılamadı.",false);throw new IOException("Maç başlatılamadı.",e);}
    }
    private boolean racing(UUID id){return members.contains(id)&&!finished.contains(id)&&Bukkit.getPlayer(id)!=null;}
    private void beginTurnAfter(UUID after){
        List<UUID> seats=new ArrayList<>(members);if(seats.stream().noneMatch(this::racing)){plugin.finish(this,"Tüm oyuncular bitirdi.",true);return;}
        int start=after==null?0:Math.floorMod(seats.indexOf(after)+1,seats.size());
        for(int offset=0;offset<seats.size()*2;offset++){
            UUID candidate=seats.get((start+offset)%seats.size());if(!racing(candidate))continue;
            int skip=skippedTurns.getOrDefault(candidate,0);if(skip>0){if(skip==1)skippedTurns.remove(candidate);else skippedTurns.put(candidate,skip-1);announce("§c"+name(candidate)+" §fbu tur kötü şans nedeniyle bekliyor.");continue;}
            current=candidate;turnLeft=Math.clamp(plugin.getConfig().getInt("turn-seconds",25),5,120);announce("Sıra: §e"+name(current)+" §f• "+turnLeft+" saniye");Player p=Bukkit.getPlayer(current);p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_BELL,1,1.2f);updateBar();return;
        }
        throw new IllegalStateException("Zar sırası seçilemedi.");
    }
    void tick(){
        if(closing)return;
        if(!active){
            if(members.size()<plugin.minimum()){waitLeft=-1;return;}
            if(waitLeft<0){waitLeft=Math.clamp(plugin.getConfig().getInt("countdown-seconds",15),3,60);announce("Maç "+waitLeft+" saniye sonra başlayacak.");}
            else if(--waitLeft<=0)try{start();}catch(IOException e){waitLeft=-1;announce("Başlatılamadı: "+e.getMessage());}
            return;
        }
        if(--matchLeft<=0){plugin.finish(this,"Süre doldu.",true);return;}
        if(movingId==null){if(--turnLeft<=0)roll(current,true);if(movingId==null)updateBar();}updateSidebar();
    }
    boolean roll(UUID id,boolean automatic){
        if(!active||closing||movingId!=null||id==null||!id.equals(current)||!racing(id))return false;
        int first=random.nextInt(6)+1,second=random.nextInt(6)+1,start=positions.getOrDefault(id,0);
        int target=RaceRules.destination(start,first,second,plugin.track().size()-1,plugin.getConfig().getBoolean("finish-on-exact-roll"));
        announce((automatic?"Otomatik zar: ":"Zar: ")+name(id)+" §e"+first+"+"+second+" §f• hedef kare "+(target+1)+"/"+plugin.track().size());
        if(target==start){announce("Tam sayı gelmedi; yerinde kaldın.");beginTurnAfter(id);return true;}
        movingId=id;movementStart=start;movementTarget=target;movementSubstep=0;crossing=false;
        bar.setTitle("§6İspanya Adası §7• §e"+name(id)+" §filerliyor");bar.setProgress(0);scheduleMovement();return true;
    }
    private void scheduleMovement(){movementTask=Bukkit.getScheduler().runTaskTimer(plugin,()->{try{moveStep();}catch(RuntimeException e){plugin.getLogger().log(java.util.logging.Level.SEVERE,"İspanya hareketi durduruldu.",e);plugin.finish(this,"Harita hareketi engellendi.",false);}},0,2);}
    void moveStep(){
        if(!active||closing||movingId==null){stopAnimation();return;}Player p=Bukkit.getPlayer(movingId);if(p==null){UUID left=movingId;stopAnimation();remove(left);return;}
        if(crossing){
            double fraction=++bridgeStep/20.0;travel(p,plugin.track().tile(bridgeFrom).location(world),plugin.track().tile(bridgeTo).location(world),fraction);bar.setProgress(fraction);
            if(bridgeStep>=20){UUID id=movingId;positions.put(id,bridgeTo);stopAnimation();announce(name(id)+" köprü kestirmesinden "+(bridgeTo+1)+". kareye geçti.");finishIfReached(id);beginTurnAfter(id);}return;
        }
        int tile=positions.getOrDefault(movingId,0);if(tile>=movementTarget){completeMovement();return;}
        travel(p,plugin.track().tile(tile).location(world),plugin.track().tile(tile+1).location(world),++movementSubstep/8.0);
        if(movementSubstep==8){movementSubstep=0;positions.put(movingId,tile+1);bar.setProgress(Math.clamp((tile+1-movementStart)/(double)(movementTarget-movementStart),0,1));if(tile+1>=movementTarget)completeMovement();}
    }
    private void travel(Player p,Location from,Location to,double progress){Location point=from.clone().add(to.toVector().subtract(from.toVector()).multiply(progress));point.setDirection(to.toVector().subtract(from.toVector()));plugin.teleport(p,point);}
    private void completeMovement(){UUID id=movingId;if(id==null)return;stopAnimation();Player p=Bukkit.getPlayer(id);if(p!=null)p.playSound(p.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,.7f,1.3f);if(!finishIfReached(id))applyEffect(id);if(movingId==null&&!closing)beginTurnAfter(id);}
    private boolean finishIfReached(UUID id){if(positions.getOrDefault(id,0)<plugin.track().size()-1)return false;if(!finished.contains(id)){finished.add(id);skippedTurns.remove(id);announce("§6"+finished.size()+". sırada bitirdi: "+name(id));}return true;}
    private void applyEffect(UUID id){
        int tile=positions.get(id);String kind=plugin.track().tile(tile).kind();String effect=plugin.getConfig().getString("tile-effects."+kind,"none").trim().toLowerCase(Locale.ROOT);
        if(effect.equals("bad_luck")){
            if(random.nextBoolean()){skippedTurns.merge(id,1,Integer::sum);announce("§c"+name(id)+" §fkırmızı kare: sonraki tur bekleyecek.");return;}
            effect="back:"+Math.clamp(plugin.getConfig().getInt("red-back-steps",3),1,12);
        }
        if(effect.equals("random")){int step=random.nextInt(Math.clamp(plugin.getConfig().getInt("purple-random-max-steps",6),1,12))+1;effect=(random.nextBoolean()?"advance:":"back:")+step;}
        if(effect.equals("shortcut")){
            int to=plugin.track().shortcutDestination(tile);if(to<0)return;movingId=id;bridgeFrom=tile;bridgeTo=to;bridgeStep=0;crossing=true;bar.setTitle("§e"+name(id)+" §fsarı köprüden geçiyor");bar.setProgress(0);scheduleMovement();return;
        }
        try{
            if(effect.startsWith("score:")){int change=Integer.parseInt(effect.substring(6));points.merge(id,change,Integer::sum);announce("§e"+name(id)+" §fpuanı "+(change>=0?"+":"")+change+" değişti.");}
            else if(effect.startsWith("advance:")||effect.startsWith("back:")){
                int step=Math.clamp(Integer.parseInt(effect.substring(effect.indexOf(':')+1)),0,plugin.track().size());int target=RaceRules.effectDestination(tile,effect.startsWith("back:")?-step:step,plugin.track().size()-1);
                positions.put(id,target);plugin.teleport(Bukkit.getPlayer(id),plugin.track().tile(target).location(world));announce("§e"+name(id)+" §f"+kind+" kare etkisiyle "+(target+1)+". kareye geçti.");finishIfReached(id);
            }
        }catch(NumberFormatException e){plugin.getLogger().warning("Geçersiz kare etkisi: "+effect);}
    }
    void stopAnimation(){if(movementTask!=null)movementTask.cancel();movementTask=null;movingId=null;crossing=false;}
    void remove(UUID id){
        if(!members.contains(id))return;
        List<UUID> order=new ArrayList<>(members);UUID predecessor=order.get(Math.floorMod(order.indexOf(id)-1,order.size()));boolean leavingTurn=id.equals(current),leavingMove=id.equals(movingId);
        if(leavingMove)stopAnimation();members.remove(id);positions.remove(id);points.remove(id);skippedTurns.remove(id);finished.remove(id);Player p=Bukkit.getPlayer(id);if(p!=null){bar.removePlayer(p);restoreBoard(p);}
        if(id.equals(owner)&&!members.isEmpty())owner=members.iterator().next();
        if(members.isEmpty()){plugin.finish(this,"Oyuncu kalmadı.",false);return;}
        if(active&&(leavingMove||leavingTurn))beginTurnAfter(predecessor);
    }
    List<UUID> orderedFinish(){var result=new ArrayList<>(finished);members.stream().filter(id->!result.contains(id)).sorted(Comparator.<UUID>comparingInt(id->positions.getOrDefault(id,0)).reversed().thenComparing(Comparator.<UUID>comparingInt(id->points.getOrDefault(id,0)).reversed())).forEach(result::add);return List.copyOf(result);}
    String ranking(){var ranks=new ArrayList<String>();List<UUID> ids=orderedFinish();for(int i=0;i<ids.size();i++)ranks.add((i+1)+". "+name(ids.get(i))+" (kare "+(positions.getOrDefault(ids.get(i),0)+1)+", puan "+points.getOrDefault(ids.get(i),0)+")");return String.join(" §7| §f",ranks);}
    void restoreBoard(Player p){Scoreboard old=previousBoards.remove(p.getUniqueId());if(old!=null)p.setScoreboard(old);}
    void announce(String message){for(UUID id:members){Player p=Bukkit.getPlayer(id);if(p!=null)plugin.tell(p,"§7[Oda "+this.id+"] §f"+message);}}
    private String name(UUID id){Player p=Bukkit.getPlayer(id);return p==null?id.toString().substring(0,8):p.getName();}
    private void updateSidebar(){for(String entry:board.getEntries())board.resetScores(entry);int rank=1;for(UUID id:orderedFinish()){sidebar.getScore("§e"+rank+". §f"+name(id)+" §7"+(positions.getOrDefault(id,0)+1)+"/"+plugin.track().size()+" §a"+points.getOrDefault(id,0)).setScore(5-rank);rank++;}}
    private void updateBar(){if(current==null)return;bar.setTitle("§6İspanya §7• §e"+name(current)+" §fzar atıyor §7("+turnLeft+" sn)");bar.setProgress(Math.clamp(turnLeft/(double)Math.clamp(plugin.getConfig().getInt("turn-seconds",25),5,120),0,1));}
}

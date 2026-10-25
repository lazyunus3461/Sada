package tr.kasabatr.tac;

import java.util.*;
import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.*;
import net.kyori.adventure.text.Component;

/** One independent castle chase. No changes are ever made to the permanent map. */
final class CrownRoom {
    enum Phase { RUNNING,ENDING,CLOSED }
    final KasabaTacPlugin plugin;final int id;final CrownMap map;final boolean practice;
    final UUID resultId=UUID.randomUUID();final Set<UUID> players=new LinkedHashSet<>();
    final Map<UUID,Integer> scores=new LinkedHashMap<>();
    final BossBar bar=Bukkit.createBossBar("§6Lanetli Taç",BarColor.YELLOW,BarStyle.SOLID);
    World world;Phase phase=Phase.RUNNING;UUID crown;
    long startedAt,finishAt,lastTransfer;int awardedSeconds,gateStep;boolean northOpen=true,eastOpen=true;
    CrownRoom(KasabaTacPlugin p,int id,CrownMap m,boolean practice){plugin=p;this.id=id;map=m;this.practice=practice;}
    boolean active(){return phase==Phase.RUNNING;}
    boolean contains(Location at){return at!=null&&at.getWorld()==world&&map.contains(at);}
    int duration(){return Math.clamp(plugin.getConfig().getInt("match-seconds",240),60,1800);}
    int remaining(){return Math.max(0,duration()-(int)((System.currentTimeMillis()-startedAt)/1000));}
    void start(){
        gates(true,true);List<UUID> order=new ArrayList<>(players);Collections.shuffle(order,plugin.random);int seat=0;
        for(UUID id:order){Player p=Bukkit.getPlayer(id);if(p==null||!plugin.auth(p))throw new IllegalArgumentException("Oyuncu ayrıldı.");scores.put(id,0);plugin.prepare(p,map.spawns.get(seat++).at(world));plugin.tools(p);bar.addPlayer(p);p.sendTitle("§6§lLANETLİ TAÇ","§eTacı kap, taşı ve puan topla",5,50,10);}
        startedAt=System.currentTimeMillis();assignCrown(order.get(plugin.random.nextInt(order.size())),false);say("§eTacı taşıyan saniyede 1 puan toplar. Yakınlaşıp tacı kap!");
    }
    void tick(long now){
        if(phase==Phase.CLOSED)return;if(phase==Phase.ENDING){bar.setTitle("§aSonuçlar §8• §eLobiye "+Math.max(0,(finishAt-now+999)/1000)+" sn");if(now>=finishAt)close();return;}
        for(UUID id:List.copyOf(players)){Player p=Bukkit.getPlayer(id);if(p==null||!p.isOnline()){plugin.leave(id);continue;}if(!contains(p.getLocation())){plugin.toLobby(p);continue;}p.setFlying(false);p.setAllowFlight(false);}
        if(!active())return;
        if(crown==null||!players.contains(crown)){assignCrown(players.iterator().next(),false);}
        Player holder=Bukkit.getPlayer(crown);long cooldown=Math.clamp(plugin.getConfig().getLong("tag-cooldown-millis",2500),1000,10000);
        // Score intervals before transfer: a new holder cannot collect the previous holder's elapsed time.
        int elapsed=Math.min(duration(),(int)Math.max(0,(now-startedAt)/1000));int delta=elapsed-awardedSeconds;
        if(delta>0){if(crown!=null)scores.computeIfPresent(crown,(k,v)->v+delta);awardedSeconds=elapsed;}
        if(elapsed>=duration()){finish("Süre doldu.");return;}
        if(holder!=null&&now-lastTransfer>=cooldown){List<Player> chasers=players.stream().filter(x->!x.equals(crown)).map(Bukkit::getPlayer).filter(Objects::nonNull).filter(p->p.getWorld()==world&&p.getLocation().distanceSquared(holder.getLocation())<2.25&&p.hasLineOfSight(holder)).sorted(Comparator.comparingDouble(p->p.getLocation().distanceSquared(holder.getLocation()))).toList();if(!chasers.isEmpty())assignCrown(chasers.getFirst().getUniqueId(),true);}
        int step=elapsed/Math.clamp(plugin.getConfig().getInt("gate-seconds",30),10,120);if(step!=gateStep){gateStep=step;rotateGates();}
        bar.setTitle((practice?"§eDENEME §8• ":"")+"§6Taç: "+name(crown)+" §8• §b"+remaining()+" sn");bar.setProgress(Math.clamp(remaining()/(double)duration(),0,1));
        for(UUID id:players){Player p=Bukkit.getPlayer(id);if(p!=null)p.sendActionBar(tr.kasaba.hub.api.MenuStyle.line("§6♛ "+name(crown)+" §8• §ePuanın: "+scores.getOrDefault(id,0)+" §8• §b"+remaining()+" sn"));}
    }
    void assignCrown(UUID next,boolean stolen){
        if(!players.contains(next))return;UUID old=crown;if(old!=null&&!old.equals(next))clearCrown(old);
        crown=next;lastTransfer=System.currentTimeMillis();Player p=Bukkit.getPlayer(next);if(p!=null){ItemStack helm=new ItemStack(Material.GOLDEN_HELMET);helm.editMeta(m->{m.setUnbreakable(true);m.displayName(Component.text("Lanetli Taç"));});p.getInventory().setHelmet(helm);p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,120000,0,false,false));p.playSound(p.getLocation(),Sound.UI_TOAST_CHALLENGE_COMPLETE,.6f,1.5f);p.sendTitle("§6§lTAÇ SENDE!","§eKaç ve puan topla",0,25,10);}
        if(stolen)say("§e"+name(next)+" §ftacı §7"+name(old)+" §foyuncusundan kaptı!");
    }
    void clearCrown(UUID id){Player p=Bukkit.getPlayer(id);if(p!=null){p.removePotionEffect(PotionEffectType.GLOWING);p.getInventory().setHelmet(null);}}
    String name(UUID id){Player p=id==null?null:Bukkit.getPlayer(id);return p==null?"—":p.getName();}
    boolean occupied(List<CrownMap.Cell> cells){for(UUID id:players){Player p=Bukkit.getPlayer(id);if(p==null||p.getWorld()!=world)continue;var box=p.getBoundingBox().expand(.3);for(var c:cells)if(box.overlaps(new org.bukkit.util.BoundingBox(c.x(),c.y(),c.z(),c.x()+1,c.y()+1,c.z()+1)))return true;}return false;}
    void rotateGates(){boolean n=gateStep%3!=1,e=gateStep%3!=2;if(!n&&occupied(map.gates.get("KUZEY")))n=true;if(!e&&occupied(map.gates.get("DOGU")))e=true;gates(n,e);say("§dŞato kapıları değişti! §7Kuzey: "+(n?"§aaçık":"§ckapalı")+" §8• §7Doğu: "+(e?"§aaçık":"§ckapalı"));}
    void gates(boolean n,boolean e){northOpen=n;eastOpen=e;for(var gate:map.gates.entrySet()){boolean open=gate.getKey().equals("KUZEY")?n:e;for(var c:gate.getValue())world.getBlockAt(c.x(),c.y(),c.z()).setType(open?Material.AIR:Material.IRON_BARS,false);}}
    void remove(UUID id){players.remove(id);scores.remove(id);if(id.equals(crown)){clearCrown(id);crown=null;}Player p=Bukkit.getPlayer(id);if(p!=null)bar.removePlayer(p);if(plugin.stopping||!active()){if(players.isEmpty())close();return;}if(players.isEmpty())close();else if(players.size()==1){UUID last=players.iterator().next();scores.compute(last,(k,v)->Math.max(1,v==null?0:v));finish("Diğer oyuncular ayrıldı; son kalan kazandı.");}}
    void finish(String reason){
        if(!active())return;phase=Phase.ENDING;finishAt=System.currentTimeMillis()+5000;gates(true,true);Set<UUID> winners=CrownRules.winners(scores);
        if(!practice&&!plugin.stopping)plugin.hub.recordGameResult(plugin,KasabaTacPlugin.ID,resultId,winners);
        say("§6"+reason);for(var entry:scores.entrySet().stream().sorted(Map.Entry.<UUID,Integer>comparingByValue().reversed()).limit(3).toList())say("§e"+name(entry.getKey())+" §8• §6"+entry.getValue()+" puan");
        for(UUID id:players){clearCrown(id);Player p=Bukkit.getPlayer(id);if(p!=null)p.sendTitle(winners.contains(id)?"§6§lKAZANDIN!":"§eMAÇ BİTTİ","§e5 saniye sonra Lanetli Taç lobisi",5,70,10);}crown=null;bar.setColor(BarColor.GREEN);bar.setProgress(1);
    }
    void close(){if(phase==Phase.CLOSED)return;phase=Phase.CLOSED;bar.removeAll();plugin.rooms.remove(id,this);for(UUID id:List.copyOf(players)){clearCrown(id);plugin.membership.remove(id,this);Player p=Bukkit.getPlayer(id);if(p!=null&&!plugin.stopping&&plugin.visitors.contains(id))plugin.returnGameLobby(p,0);}crown=null;players.clear();scores.clear();if(world!=null)plugin.closeWhenEmpty(world,0);}
    void say(String text){for(UUID id:players){Player p=Bukkit.getPlayer(id);if(p!=null)plugin.tell(p,text);}}
}

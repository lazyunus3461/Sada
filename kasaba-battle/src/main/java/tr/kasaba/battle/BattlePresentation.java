package tr.kasaba.battle;

import java.util.*;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

/** Match-local presentation. World rules, global borders and role-owned teams are untouched. */
final class BattlePresentation {
    private record View(Scoreboard previousBoard,WorldBorder previousBorder,Scoreboard board,WorldBorder border,BossBar bar,double initialRadius){}
    private final Map<UUID,View> views=new HashMap<>();
    private final NamespacedKey hiddenWaypoints;
    BattlePresentation(JavaPlugin plugin){hiddenWaypoints=new NamespacedKey(plugin,"br-hidden-waypoints");}

    Scoreboard board(Collection<Player> players){
        Scoreboard board=Bukkit.getScoreboardManager().getNewScoreboard();
        Team hidden=board.registerNewTeam("br_hidden");
        hidden.setOption(Team.Option.NAME_TAG_VISIBILITY,Team.OptionStatus.NEVER);
        hidden.setAllowFriendlyFire(true);
        hidden.setCanSeeFriendlyInvisibles(false);
        for(Player p:players)hidden.addEntry(p.getName());
        for(var entry:views.entrySet()){
            Player p=Bukkit.getPlayer(entry.getKey());if(p!=null)hidden.addEntry(p.getName());
            Team existing=entry.getValue().board.getTeam("br_hidden");
            if(existing!=null)for(Player joining:players)existing.addEntry(joining.getName());
        }
        return board;
    }
    void enter(Player p,Scoreboard board,Location center,double radius){
        clear(p);
        WorldBorder border=Bukkit.createWorldBorder();border.setCenter(center.getX(),center.getZ());border.setSize(radius*2);
        border.setWarningDistance(5);border.setWarningTime(5);border.setDamageAmount(0);border.setDamageBuffer(0);
        BossBar bar=BossBar.bossBar(Component.text("Güvenli alan"),1,BossBar.Color.BLUE,BossBar.Overlay.PROGRESS);
        View view=new View(p.getScoreboard(),p.getWorldBorder(),board,border,bar,radius);views.put(p.getUniqueId(),view);
        p.setScoreboard(board);p.setWorldBorder(border);
        for(Attribute type:List.of(Attribute.WAYPOINT_RECEIVE_RANGE,Attribute.WAYPOINT_TRANSMIT_RANGE)){
            AttributeInstance attribute=p.getAttribute(type);if(attribute!=null){attribute.removeModifier(hiddenWaypoints);attribute.addTransientModifier(new AttributeModifier(hiddenWaypoints,-1,AttributeModifier.Operation.MULTIPLY_SCALAR_1));}
        }
        p.showBossBar(bar);
    }
    void update(Player p,double radius,double nextRadius,int untilShrink,int untilEnd){
        View view=views.get(p.getUniqueId());if(view==null)return;
        if(p.getScoreboard()!=view.board)p.setScoreboard(view.board);
        view.border.setSize(radius*2);
        boolean shrinking=nextRadius<radius-0.00001;
        if(shrinking)view.border.setSize(nextRadius*2,TimeUnit.SECONDS,1);
        int width=(int)Math.ceil(radius*2);
        String timer=untilShrink>0?"Daralmaya "+time(untilShrink):untilEnd>0?"Daralıyor • "+time(untilEnd):"Son alan";
        view.bar.name(Component.text("Güvenli alan "+width+" × "+width+" • "+timer));
        view.bar.color(shrinking?BossBar.Color.RED:BossBar.Color.BLUE);
        view.bar.progress((float)Math.clamp(radius/view.initialRadius,0,1));
    }
    private static String time(int seconds){return "%d:%02d".formatted(seconds/60,seconds%60);}
    void clear(Player p){
        for(Attribute type:List.of(Attribute.WAYPOINT_RECEIVE_RANGE,Attribute.WAYPOINT_TRANSMIT_RANGE)){
            AttributeInstance attribute=p.getAttribute(type);if(attribute!=null)attribute.removeModifier(hiddenWaypoints);
        }
        View view=views.remove(p.getUniqueId());if(view==null)return;
        p.hideBossBar(view.bar);
        if(p.getScoreboard()==view.board)p.setScoreboard(view.previousBoard);
        if(p.getWorldBorder()==view.border){WorldBorder previous=view.previousBorder;p.setWorldBorder(previous==null||previous.getWorld()==null||previous.getWorld().equals(p.getWorld())?previous:null);}
        for(View other:views.values()){Team hidden=other.board.getTeam("br_hidden");if(hidden!=null)hidden.removeEntry(p.getName());}
    }
}

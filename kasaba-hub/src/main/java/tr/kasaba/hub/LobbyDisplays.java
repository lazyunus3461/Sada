package tr.kasaba.hub;

import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
import tr.kasaba.hub.api.MenuStyle;

/** Non-persistent world text: no duplicate holograms or copied public boards in match worlds. */
final class LobbyDisplays {
    record Card(String game,String title,List<String> lines,String metric,Location info,Location top){}
    private final LeaderboardStore store;
    private final Map<String,Card> cards=new HashMap<>();
    private final Map<String,TextDisplay> entities=new HashMap<>();
    LobbyDisplays(LeaderboardStore store){this.store=store;}
    void present(String game,Location spawn,String title,List<String> lines,String metric){
        Vector front=spawn.getDirection().setY(0);if(front.lengthSquared()<0.01)front=new Vector(0,0,-1);front.normalize();Vector right=new Vector(-front.getZ(),0,front.getX());
        Location info=spawn.clone().add(front.clone().multiply(7)).add(0,3.7,0),top=info.clone().add(right.multiply(9.5)).add(0,-.8,0);
        Card card=new Card(game,title,List.copyOf(lines),metric,info,top),old=cards.put(game,card);
        if(old==null||old.info.getWorld()!=info.getWorld()||old.info.distanceSquared(info)>0.01){removeEntity(game+"/info");removeEntity(game+"/top");}
        refresh();
    }
    void refresh(){for(Card card:cards.values()){
        if(Bukkit.getWorld(card.info.getWorld().getUID())!=card.info.getWorld())continue;
        Component information=MenuStyle.title(card.title);for(String line:card.lines)information=information.append(Component.newline()).append(MenuStyle.line(line));
        render(card.game+"/info",card.info,information);
        Component top=Component.text("İLK 5 • "+card.metric,NamedTextColor.AQUA,TextDecoration.BOLD);List<LeaderboardStore.Row> rows=store.top(card.game);
        for(int i=0;i<5;i++){NamedTextColor color=i==0?NamedTextColor.GOLD:i==1?NamedTextColor.WHITE:i==2?NamedTextColor.YELLOW:NamedTextColor.GRAY;top=top.append(Component.newline()).append(Component.text((i+1)+". ",color)).append(i<rows.size()?Component.text(rows.get(i).name(),NamedTextColor.WHITE).append(Component.text(" • "+rows.get(i).score(),NamedTextColor.GREEN)):Component.text("—",NamedTextColor.DARK_GRAY));}
        render(card.game+"/top",card.top,top);
    }}
    private void render(String key,Location at,Component content){
        if(!at.getWorld().isChunkLoaded(at.getBlockX()>>4,at.getBlockZ()>>4))return;
        TextDisplay text=entities.get(key);
        if(text==null||!text.isValid()){text=at.getWorld().spawn(at,TextDisplay.class,e->{e.addScoreboardTag("kasaba-lobby-display");e.addScoreboardTag("kasaba-board:"+key);e.setPersistent(false);e.setBillboard(Display.Billboard.CENTER);e.setSeeThrough(false);e.setShadowed(true);e.setLineWidth(450);e.setBackgroundColor(Color.fromARGB(100,12,17,25));e.setViewRange(1.2f);});entities.put(key,text);}
        if(!text.text().equals(content))text.text(content);
    }
    private void removeEntity(String key){TextDisplay entity=entities.remove(key);if(entity!=null)entity.remove();}
    void remove(String game){cards.remove(game);removeEntity(game+"/info");removeEntity(game+"/top");}
    void close(){for(TextDisplay entity:entities.values())entity.remove();entities.clear();cards.clear();}
}

package tr.kasaba.bilgi;

import io.netty.channel.*;
import java.lang.reflect.*;
import java.util.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

/** Per-client sidebar only. Bukkit scoreboards and all role/privacy teams stay owned by their games. */
final class SidebarPackets implements AutoCloseable {
    static final String OBJECTIVE="kasaba_bilgi";
    private static final String HOOK="kasaba_bilgi_sidebar";
    private final JavaPlugin plugin;
    private final Map<UUID,View> views=new HashMap<>();
    private final Class<?> packet,displayPacket,bundle;
    private final Constructor<?> scoreboard,objectivePacket,displayConstructor,scorePacket,resetPacket,bundleConstructor;
    private final Method addObjective,asVanilla,displaySlot,displayName,subPackets;
    private final Object dummy,integer,blank,sidebar;
    private volatile boolean closed;
    static final class View {
        final Player player;final Channel channel;volatile boolean visible;Object objective;PanelView.Frame frame;
        View(Player player,Channel channel){this.player=player;this.channel=channel;}
    }
    SidebarPackets(JavaPlugin plugin){this.plugin=plugin;try{
        packet=type("net.minecraft.network.protocol.Packet");Class<?> board=type("net.minecraft.world.scores.Scoreboard"),objective=type("net.minecraft.world.scores.Objective"),criteria=type("net.minecraft.world.scores.criteria.ObjectiveCriteria"),render=type("net.minecraft.world.scores.criteria.ObjectiveCriteria$RenderType"),component=type("net.minecraft.network.chat.Component"),number=type("net.minecraft.network.chat.numbers.NumberFormat"),slot=type("net.minecraft.world.scores.DisplaySlot");
        scoreboard=board.getConstructor();dummy=criteria.getField("DUMMY").get(null);integer=Enum.valueOf((Class)render,"INTEGER");blank=type("net.minecraft.network.chat.numbers.BlankFormat").getField("INSTANCE").get(null);sidebar=Enum.valueOf((Class)slot,"SIDEBAR");
        addObjective=board.getMethod("addObjective",String.class,criteria,component,render,boolean.class,number);
        asVanilla=type("io.papermc.paper.adventure.PaperAdventure").getMethod("asVanilla",Component.class);
        objectivePacket=type("net.minecraft.network.protocol.game.ClientboundSetObjectivePacket").getConstructor(objective,int.class);
        displayPacket=type("net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket");displayConstructor=displayPacket.getConstructor(slot,objective);displaySlot=displayPacket.getMethod("getSlot");displayName=displayPacket.getMethod("getObjectiveName");
        scorePacket=type("net.minecraft.network.protocol.game.ClientboundSetScorePacket").getConstructor(String.class,String.class,int.class,Optional.class,Optional.class);
        resetPacket=type("net.minecraft.network.protocol.game.ClientboundResetScorePacket").getConstructor(String.class,String.class);
        bundle=type("net.minecraft.network.protocol.game.ClientboundBundlePacket");subPackets=bundle.getMethod("subPackets");bundleConstructor=bundle.getConstructor(Iterable.class);
    }catch(ReflectiveOperationException e){throw new IllegalStateException("Bilgi tablosu Paper 1.21.10 üzerinde açılamadı.",e);}}
    private static Class<?> type(String name)throws ClassNotFoundException{return Class.forName(name);}
    private Object listener(Player p)throws ReflectiveOperationException{Object handle=p.getClass().getMethod("getHandle").invoke(p);return handle.getClass().getField("connection").get(handle);}
    private void send(Player p,Object value)throws ReflectiveOperationException{Object listener=listener(p);listener.getClass().getMethod("send",packet).invoke(listener,value);}
    private Object objective(String name,Component title)throws ReflectiveOperationException{return addObjective.invoke(scoreboard.newInstance(),name,dummy,asVanilla.invoke(null,title),integer,false,blank);}
    void install(Player p)throws ReflectiveOperationException{
        if(closed||views.containsKey(p.getUniqueId()))return;
        Object listener=listener(p),connection=listener.getClass().getField("connection").get(listener);
        Channel channel=(Channel)connection.getClass().getField("channel").get(connection);View view=new View(p,channel);views.put(p.getUniqueId(),view);
        channel.eventLoop().execute(()->{if(closed||channel.pipeline().get(HOOK)!=null)return;channel.pipeline().addBefore("packet_handler",HOOK,new ChannelDuplexHandler(){
            @Override public void write(ChannelHandlerContext ctx,Object message,ChannelPromise promise)throws Exception{
                Object result=filter(message,view);
                if(result==null)promise.trySuccess();else super.write(ctx,result,promise);
            }
        });});
    }
    private Object filter(Object message,View view)throws ReflectiveOperationException{
        if(closed||!view.visible)return message;
        if(displayPacket.isInstance(message)&&sidebar.equals(displaySlot.invoke(message))&&!OBJECTIVE.equals(displayName.invoke(message)))return null;
        if(bundle.isInstance(message)){
            List<Object> kept=new ArrayList<>();boolean changed=false;
            for(Object item:(Iterable<?>)subPackets.invoke(message)){Object filtered=filter(item,view);if(filtered!=item)changed=true;if(filtered!=null)kept.add(filtered);}
            if(changed)return kept.isEmpty()?null:bundleConstructor.newInstance(kept);
        }
        return message;
    }
    void show(Player p,PanelView.Frame frame)throws ReflectiveOperationException{
        install(p);View view=views.get(p.getUniqueId());if(view==null||closed)return;boolean first=view.frame==null;
        view.visible=true;
        if(first||!view.frame.title().equals(frame.title())){
            view.objective=objective(OBJECTIVE,frame.title());send(p,objectivePacket.newInstance(view.objective,first?0:2));
        }
        List<Component> old=first?List.of():view.frame.lines();
        for(int i=frame.lines().size();i<old.size();i++)send(p,resetPacket.newInstance(entry(i),OBJECTIVE));
        for(int i=0;i<frame.lines().size();i++){
            if(first||old.size()!=frame.lines().size()||i>=old.size()||!old.get(i).equals(frame.lines().get(i)))
                send(p,scorePacket.newInstance(entry(i),OBJECTIVE,frame.lines().size()-i,Optional.of(asVanilla.invoke(null,frame.lines().get(i))),Optional.of(blank)));
        }
        if(first)send(p,displayConstructor.newInstance(sidebar,view.objective));
        view.frame=frame;
    }
    private static String entry(int index){return "kasaba:bilgi:"+index;}
    void hide(Player p)throws ReflectiveOperationException{
        View view=views.get(p.getUniqueId());if(view==null)return;view.visible=false;
        if(view.frame!=null){
            Objective actual=p.getScoreboard().getObjective(DisplaySlot.SIDEBAR);
            Object previous=actual==null?null:objective(actual.getName(),actual.displayName());
            send(p,displayConstructor.newInstance(sidebar,previous));send(p,objectivePacket.newInstance(view.objective,1));view.frame=null;view.objective=null;
        }
    }
    void forget(Player p,boolean restore){View view=views.get(p.getUniqueId());if(view==null)return;
        if(restore)try{hide(p);}catch(ReflectiveOperationException e){plugin.getLogger().warning("Önceki sağ tablo geri yüklenemedi: "+p.getName());}
        view.visible=false;views.remove(p.getUniqueId());view.channel.eventLoop().execute(()->{if(view.channel.pipeline().get(HOOK)!=null)view.channel.pipeline().remove(HOOK);});
    }
    @Override public void close(){closed=true;for(View view:List.copyOf(views.values()))forget(view.player,true);}
}

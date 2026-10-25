package tr.kasaba.test;
import com.google.gson.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.scoreboard.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import tr.kasaba.roles.api.ClanLabels;
import tr.kasaba.akce.api.AkceApi;

/** Loopback-only adapter. Never included in the production download. */
public final class BilgiProbe extends JavaPlugin implements org.bukkit.plugin.messaging.PluginMessageListener {
 final Gson json=new GsonBuilder().serializeNulls().create();Object game;UUID clanPlayer;ClanLabels clans;
 public void onEnable(){if(!Bukkit.getIp().equals("127.0.0.1"))throw new IllegalStateException("Loopback only");game=Bukkit.getPluginManager().getPlugin("KasabaBilgi");getServer().getMessenger().registerIncomingPluginChannel(this,"kasaba_test:bilgi",this);}
 public void onPluginMessageReceived(String ch,Player p,byte[] data){if(p.getAddress()==null||!p.getAddress().getAddress().isLoopbackAddress())return;String op=new String(data,StandardCharsets.UTF_8);Bukkit.getScheduler().runTask(this,()->{try{String[] a=op.split("\\|");switch(a[0]){
 case "STATE"->{Map<String,Object>d=new LinkedHashMap<>();d.put("uuid",p.getUniqueId());d.put("world",p.getWorld().getName());d.put("board",System.identityHashCode(p.getScoreboard()));var sidebar=p.getScoreboard().getObjective(DisplaySlot.SIDEBAR);d.put("nativeSidebar",sidebar==null?null:sidebar.getName());var team=p.getScoreboard().getEntryTeam(p.getName());d.put("team",team==null?null:team.getName());d.put("nameVisibility",team==null?null:team.getOption(Team.Option.NAME_TAG_VISIBILITY));d.put("statTicks",p.getStatistic(Statistic.PLAY_ONE_MINUTE));Object bridge=get(game,"packets");Object view=((Map<?,?>)get(bridge,"views")).get(p.getUniqueId());Object frame=view==null?null:get(view,"frame");d.put("visible",view!=null&&get(view,"visible").equals(true));if(frame!=null){d.put("bright",call(frame,"bright"));d.put("title",plain((Component)call(frame,"title")));List<?> rows=(List<?>)call(frame,"lines");d.put("lines",rows.stream().map(c->plain((Component)c)).toList());d.put("colors",rows.stream().map(c->((Component)c).color()==null?"none":((Component)c).color().asHexString()).toList());}say(p,"state:"+json.toJson(d));}
 case "TIME"->p.setStatistic(Statistic.PLAY_ONE_MINUTE,Integer.parseInt(a[1]));
 case "CREDIT"->Bukkit.getServicesManager().load(AkceApi.class).deposit(p.getUniqueId(),new BigDecimal(a[1]));
 case "CLAN"->{clanPlayer=p.getUniqueId();clans=name->name.equals(p.getName())?Component.text("[KSB] "):Component.empty();Bukkit.getServicesManager().register(ClanLabels.class,clans,this,ServicePriority.Highest);}
 case "NO_CLAN"->{if(clans!=null)Bukkit.getServicesManager().unregister(ClanLabels.class,clans);}
 case "DISABLE"->Bukkit.getPluginManager().disablePlugin((org.bukkit.plugin.Plugin)game);
 case "ENABLE"->Bukkit.getPluginManager().enablePlugin((org.bukkit.plugin.Plugin)game);
 case "NATIVE"->{Scoreboard board=p.getScoreboard();Objective objective=board.getObjective("fixture_hud");if(objective==null)objective=board.registerNewObjective("fixture_hud",Criteria.DUMMY,Component.text("Test oyunu"));objective.setDisplaySlot(DisplaySlot.SIDEBAR);objective.getScore("Oyun puanı").setScore(17);}
 case "NATIVE_CLEAR"->{var o=p.getScoreboard().getObjective("fixture_hud");if(o!=null)o.unregister();}
 default->throw new IllegalArgumentException(op);}
 say(p,"done:"+op);}catch(Throwable e){say(p,"error:"+e);getLogger().log(java.util.logging.Level.SEVERE,"Bilgi fixture",e);}});}
 String plain(Component c){return PlainTextComponentSerializer.plainText().serialize(c);}
 Object get(Object target,String field)throws Exception{var f=target.getClass().getDeclaredField(field);f.setAccessible(true);return f.get(target);}
 Object call(Object target,String name)throws Exception{var m=target.getClass().getDeclaredMethod(name);m.setAccessible(true);return m.invoke(target);}
 void say(Player p,String text){p.sendMessage(Component.text("bilgi-probe:"+text));}
}

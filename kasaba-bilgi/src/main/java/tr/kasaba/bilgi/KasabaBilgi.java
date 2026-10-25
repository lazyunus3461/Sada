package tr.kasaba.bilgi;

import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;
import tr.kasaba.auth.api.AuthState;
import tr.kasaba.hub.api.HubApi;
import tr.kasaba.akce.api.AkceApi;
import tr.kasaba.roles.api.*;

/** Live personal HUD that never replaces a match's Bukkit board or exposes private roles. */
public final class KasabaBilgi extends JavaPlugin implements Listener,TabExecutor {
    private HubApi hub;private AkceApi bank;private RoleAccess roles;private SidebarPackets packets;
    private final Set<UUID> hidden=new HashSet<>();private final Map<UUID,Long> warnings=new HashMap<>();
    @Override public void onEnable(){
        try{hub=Objects.requireNonNull(Bukkit.getServicesManager().load(HubApi.class));bank=Objects.requireNonNull(Bukkit.getServicesManager().load(AkceApi.class));roles=Objects.requireNonNull(Bukkit.getServicesManager().load(RoleAccess.class));packets=new SidebarPackets(this);}
        catch(Exception|LinkageError e){getLogger().log(java.util.logging.Level.SEVERE,"Bilgi tablosu açılamadı. Paper 1.21.10 ve KasabaRoles 1.3.2 gereklidir.",e);Bukkit.getPluginManager().disablePlugin(this);return;}
        Bukkit.getPluginManager().registerEvents(this,this);Objects.requireNonNull(getCommand("kasababilgi")).setTabCompleter(this);
        Bukkit.getScheduler().runTaskTimer(this,this::tick,1,20);
    }
    @Override public void onDisable(){if(packets!=null)packets.close();hidden.clear();warnings.clear();}
    private boolean auth(Player p){AuthState state=Bukkit.getServicesManager().load(AuthState.class);return p.isOnline()&&state!=null&&state.authenticated(p.getUniqueId());}
    private void tick(){for(Player p:Bukkit.getOnlinePlayers())refresh(p);}
    private void refresh(Player p){try{
        if(!auth(p)||hidden.contains(p.getUniqueId())){packets.hide(p);return;}
        boolean lobby=hub.socialArea(p.getUniqueId());PanelView.GameRows game=gameRows(p);
        Component rank=roles.rank(p);ClanLabels clans=Bukkit.getServicesManager().load(ClanLabels.class);String clan=clans==null?"":PanelView.plain(clans.prefix(p.getName())).trim();if(clan.isBlank())clan="Yok";
        long seconds=Math.max(0,p.getStatistic(Statistic.PLAY_ONE_MINUTE))/20L;
        var data=new PanelView.Data(p.getName(),rank,bank.balance(p.getUniqueId()),clan,seconds);
        packets.show(p,PanelView.render(data,lobby&&(game==null||game.rows().isEmpty()),game));warnings.remove(p.getUniqueId());
    }catch(Exception|LinkageError e){long now=System.currentTimeMillis();if(now-warnings.getOrDefault(p.getUniqueId(),0L)>60000){warnings.put(p.getUniqueId(),now);getLogger().log(java.util.logging.Level.WARNING,"Oyuncu bilgi tablosu yenilenemedi: "+p.getName(),e);}try{packets.hide(p);}catch(Exception ignored){}}}
    private PanelView.GameRows gameRows(Player p){
        Scoreboard board=p.getScoreboard();Objective game=board.getObjective(DisplaySlot.SIDEBAR);if(game==null)return null;
        List<PanelView.NativeRow> rows=new ArrayList<>();
        for(String entry:board.getEntries()){
            Score score=game.getScore(entry);if(!score.isScoreSet()||entry.startsWith("#"))continue;
            Component text=score.customName();if(text==null){text=LegacyComponentSerializer.legacySection().deserialize(entry);Team team=board.getEntryTeam(entry);if(team!=null)text=team.prefix().append(text).append(team.suffix());}
            rows.add(new PanelView.NativeRow(text,score.getScore()));
        }
        rows.sort(Comparator.comparingInt(PanelView.NativeRow::score).reversed().thenComparing(row->PanelView.plain(row.name()),Comparator.reverseOrder()));
        return new PanelView.GameRows(game.getName(),game.displayName(),List.copyOf(rows));
    }
    @EventHandler public void joined(PlayerJoinEvent event){try{packets.install(event.getPlayer());}catch(ReflectiveOperationException e){getLogger().warning("Bilgi bağlantısı kurulamadı: "+event.getPlayer().getName());}}
    @EventHandler public void quit(PlayerQuitEvent event){Player p=event.getPlayer();packets.forget(p,false);hidden.remove(p.getUniqueId());warnings.remove(p.getUniqueId());}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(!(sender instanceof Player p))return true;if(!auth(p)){p.sendMessage("§eÖnce kayıt/giriş işlemini tamamla.");return true;}
        String action=args.length==0?"":args[0].toLowerCase(Locale.ROOT).replace('ç','c').replace('ö','o');
        switch(action){case "ac","goster"->hidden.remove(p.getUniqueId());case "kapat","gizle"->hidden.add(p.getUniqueId());default->{p.sendMessage("§e/tablo aç §7veya §e/tablo kapat");return true;}}
        refresh(p);p.sendMessage(hidden.contains(p.getUniqueId())?"§7Kişisel bilgi tablosu kapatıldı.":"§aKişisel bilgi tablosu açıldı.");return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){return args.length==1?List.of("aç","kapat"):List.of();}
}

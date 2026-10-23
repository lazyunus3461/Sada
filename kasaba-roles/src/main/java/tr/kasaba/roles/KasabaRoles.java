package tr.kasaba.roles;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;
import tr.kasaba.roles.api.ClanLabels;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class KasabaRoles extends JavaPlugin implements Listener, TabCompleter, tr.kasaba.roles.api.RoleAccess {
    private RoleFiles.Configuration configuration;
    private VipTerms vipTerms;
    private Map<String,String> assignments=new LinkedHashMap<>();
    private final Map<UUID,Applied> applied=new HashMap<>();
    private Path configPath,playersPath;
    private record ChatSettings(boolean enabled, Map<String,Role> players, Role defaultRole) {}
    private volatile ChatSettings chatSettings;
    private NameTags nameTags;
    private volatile ClanLabels clanLabels;
    private static final class Applied {
        final Component originalName;final int originalOrder;
        PermissionAttachment attachment;Component lastName;int lastOrder;boolean tabWritten;
        Applied(Player p){originalName=p.playerListName();originalOrder=p.getPlayerListOrder();}
    }
    @Override public void onEnable() {
        nameTags=new NameTags(getLogger());
        saveDefaultConfig();
        configPath=getDataFolder().toPath().resolve("config.yml");
        playersPath=getDataFolder().toPath().resolve("oyuncular.yml");
        try {
            if(!Files.exists(playersPath))RoleFiles.writeAssignments(playersPath,Map.of());
            load();vipTerms=new VipTerms(getDataFolder().toPath().resolve("vip-sureleri.yml"));
        } catch(IOException|IllegalArgumentException e){
            getLogger().log(Level.SEVERE,"Rol verileri yüklenemedi; eklenti etkinleşmedi.",e);
            getServer().getPluginManager().disablePlugin(this);return;
        }
        Objects.requireNonNull(getCommand("rol")).setTabCompleter(this);
        getServer().getPluginManager().registerEvents(this,this);
        Bukkit.getServicesManager().register(tr.kasaba.roles.api.RoleAccess.class,this,this,org.bukkit.plugin.ServicePriority.Normal);
        Bukkit.getScheduler().runTaskTimer(this,this::applyAll,20,200);
        Bukkit.getScheduler().runTaskTimer(this,this::refreshNameTags,20,40);
    }
    @Override public void onDisable() {
        Bukkit.getServicesManager().unregisterAll(this);
        for(Player p:Bukkit.getOnlinePlayers())remove(p);
        applied.clear();
        if(nameTags!=null)nameTags.clear();
    }
    private Role assigned(String name){return configuration.catalog().get(assignments.getOrDefault(Role.playerKey(name),configuration.catalog().defaultRole().id()));}
    private Role role(String name){Role current=assigned(name);Player player=Bukkit.getPlayerExact(name);Role vip=configuration.catalog().get("vip");return player!=null&&vip!=null&&vipTerms!=null&&vipTerms.until(player.getUniqueId())>System.currentTimeMillis()&&current.priority()<vip.priority()?vip:current;}
    @Override public boolean vip(Player p){Role role=assigned(p.getName());return role.id().equals("vip")||Boolean.TRUE.equals(configuration.catalog().permissions(role.id()).get("kasaba.vip"))||vipUntil(p.getUniqueId())>System.currentTimeMillis();}
    @Override public boolean guide(Player p){return Boolean.TRUE.equals(configuration.catalog().permissions(assigned(p.getName()).id()).get("kasaba.fly"));}
    @Override public long vipUntil(UUID id){return vipTerms==null?0:vipTerms.until(id);}
    @Override public void grantVip(UUID id,long until){try{vipTerms.grant(id,until);applyAll();}catch(IOException e){throw new IllegalStateException("VIP süresi kaydedilemedi.",e);}}
    private void load() throws IOException {
        var yaml=RoleFiles.read(configPath);
        boolean migrated=yaml.getInt("network-perks-version",0)<1;
        if(migrated){for(String id:List.of("kurucu","admin","moderator","rehber","vip")){String key="roles."+id;if(yaml.isConfigurationSection(key)&&"white".equals(yaml.getString(key+".name-color","white")))yaml.set(key+".name-color",yaml.getString(key+".color","white"));}if(yaml.isConfigurationSection("roles.vip"))yaml.set("roles.vip.permissions.kasaba.vip",true);if(yaml.isConfigurationSection("roles.kurucu"))yaml.set("roles.kurucu.permissions.kasaba.fly",true);if(yaml.isConfigurationSection("roles.rehber"))yaml.set("roles.rehber.permissions.kasaba.fly",true);yaml.set("network-perks-version",1);}
        var next=RoleFiles.configuration(yaml);
        var nextAssignments=RoleFiles.assignments(RoleFiles.read(playersPath),next.catalog());
        if(migrated){Path backup=configPath.resolveSibling("config-before-vip-1.3.0.yml");if(!Files.exists(backup))Files.copy(configPath,backup);RoleFiles.write(configPath,yaml.saveToString());}
        configuration=next;assignments=nextAssignments;
        refreshChat();
    }
    private void refreshChat() {
        Map<String,Role> roles=new HashMap<>();
        assignments.forEach((name,id)->roles.put(name,configuration.catalog().get(id)));
        for(Player player:Bukkit.getOnlinePlayers())roles.put(Role.playerKey(player.getName()),role(player.getName()));
        chatSettings=new ChatSettings(configuration.manageChat(),Map.copyOf(roles),configuration.catalog().defaultRole());
    }
    private void applyAll(){if(configuration!=null){
        var provider=Bukkit.getServicesManager().getRegistration(ClanLabels.class);clanLabels=provider==null?null:provider.getProvider();
        refreshChat();for(Player p:Bukkit.getOnlinePlayers())apply(p);refreshNameTags();
    }}
    /** Called on the main thread after a clan membership/tag changes. */
    public void refreshClanLabels(){if(Bukkit.isPrimaryThread())applyAll();else Bukkit.getScheduler().runTask(this,this::applyAll);}
    private Component clan(String name){ClanLabels provider=clanLabels;return provider==null?Component.empty():provider.prefix(name);}
    private void refreshNameTags() {
        if(configuration==null)return;
        if(configuration.manageNameTags())nameTags.refresh(Bukkit.getOnlinePlayers(),this::role,this::clan);
        else nameTags.clear();
    }
    private void apply(Player p) {
        Applied state=applied.computeIfAbsent(p.getUniqueId(),id->new Applied(p));
        if(state.attachment!=null)p.removeAttachment(state.attachment);
        state.attachment=p.addAttachment(this);
        Collection<String> registered=Bukkit.getPluginManager().getPermissions().stream().map(x->x.getName()).toList();
        Role role=role(p.getName());
        PermissionRules.expand(configuration.catalog().permissions(role.id()),registered)
                .forEach(state.attachment::setPermission);
        if(configuration.manageTab()) {
            Component tag=RoleDisplay.tag(role,p.getName(),clan(p.getName()));
            p.playerListName(tag);p.setPlayerListOrder(role.priority());
            state.lastName=tag;state.lastOrder=role.priority();state.tabWritten=true;
        } else restoreTab(p,state);
    }
    private void restoreTab(Player p,Applied state) {
        if(!state.tabWritten)return;
        if(p.playerListName().equals(state.lastName))p.playerListName(state.originalName);
        if(p.getPlayerListOrder()==state.lastOrder)p.setPlayerListOrder(state.originalOrder);
        state.tabWritten=false;
    }
    private void remove(Player p) {
        Applied state=applied.remove(p.getUniqueId());if(state==null)return;
        if(state.attachment!=null)p.removeAttachment(state.attachment);
        restoreTab(p,state);
        nameTags.remove(p.getUniqueId());
    }
    @EventHandler(priority=EventPriority.MONITOR) public void join(PlayerJoinEvent e){apply(e.getPlayer());refreshNameTags();}
    @EventHandler public void quit(PlayerQuitEvent e){remove(e.getPlayer());}
    @EventHandler public void pluginEnabled(PluginEnableEvent e){
        if(e.getPlugin()!=this)Bukkit.getScheduler().runTask(this,this::applyAll);
    }
    @EventHandler public void registered(ServiceRegisterEvent e){if(e.getProvider().getService()==ClanLabels.class)Bukkit.getScheduler().runTask(this,this::applyAll);}
    @EventHandler public void unregistered(ServiceUnregisterEvent e){if(e.getProvider().getService()==ClanLabels.class)Bukkit.getScheduler().runTask(this,this::applyAll);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void chat(AsyncChatEvent event) {
        ChatSettings snapshot=chatSettings;
        if(snapshot==null||!snapshot.enabled())return;
        String name=event.getPlayer().getName();
        Role role=snapshot.players().getOrDefault(Role.playerKey(name),snapshot.defaultRole());
        Component clan=clan(name);
        event.renderer(ChatRenderer.viewerUnaware((source,displayName,message)->RoleDisplay.chat(role,name,message,clan)));
    }
    private void tell(CommandSender sender,String text){sender.sendMessage(Component.text(text,NamedTextColor.YELLOW));}
    private void changeRole(String name,String id) throws IOException {
        String player=Role.playerKey(name);
        Map<String,String> next=new LinkedHashMap<>(assignments);
        if(id==null)next.remove(player);
        else {
            id=Role.normalizeId(id);
            if(configuration.catalog().get(id)==null)throw new IllegalArgumentException("Rol bulunamadı: "+id);
            next.put(player,id);
        }
        RoleFiles.writeAssignments(playersPath,next);assignments=next;
        refreshChat();
        for(Player p:Bukkit.getOnlinePlayers())if(Role.playerKey(p.getName()).equals(player))apply(p);
        refreshNameTags();
    }
    private void edit(Consumer<YamlConfiguration> change) throws IOException {
        YamlConfiguration next=new YamlConfiguration();
        try{next.loadFromString(configuration.yaml().saveToString());}
        catch(InvalidConfigurationException e){throw new IOException("Rol ayarları kopyalanamadı",e);}
        change.accept(next);
        var checked=RoleFiles.configuration(next);
        for(String id:assignments.values())if(checked.catalog().get(id)==null)
            throw new IllegalArgumentException("Önce bu rolü oyunculardan kaldır: "+id);
        RoleFiles.write(configPath,next.saveToString());configuration=checked;applyAll();
    }
    static TextColor color(String text) {
        if(text==null)throw new IllegalArgumentException("Renk boş olamaz");
        String key=text.toLowerCase(Locale.ROOT);
        key=switch(key){
            case "kırmızı","kirmizi"->"red";case "koyu_kırmızı","koyu_kirmizi"->"dark_red";
            case "yeşil","yesil"->"green";case "koyu_yeşil","koyu_yesil"->"dark_green";
            case "altın","altin"->"gold";case "sarı","sari"->"yellow";case "turkuaz"->"aqua";
            case "mavi"->"blue";case "mor"->"light_purple";case "beyaz"->"white";
            case "gri"->"gray";case "siyah"->"black";default->key;
        };
        TextColor value=key.startsWith("#")?TextColor.fromHexString(key):NamedTextColor.NAMES.value(key);
        if(value==null)throw new IllegalArgumentException("Renk geçersiz. Örnek: kırmızı, yeşil, turkuaz, altın, #FF4545");
        return value;
    }
    private void requireRole(String id){if(configuration.catalog().get(id)==null)throw new IllegalArgumentException("Rol bulunamadı: "+id);}
    private void length(String[] args,int min,String usage){if(args.length<min)throw new IllegalArgumentException("Kullanım: "+usage);}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(configuration==null){tell(sender,"Rol sistemi kullanılamıyor; sunucu konsolunu kontrol et.");return true;}
        String sub=args.length==0?"bilgi":args[0].toLowerCase(Locale.ROOT);
        if(!Set.of("bilgi","liste").contains(sub)&&!sender.hasPermission("kasabaroles.admin")) {
            tell(sender,"Bu komut için yetkin yok.");return true;
        }
        if(Set.of("bilgi","liste").contains(sub)&&!sender.hasPermission("kasabaroles.info")){
            tell(sender,"Rol bilgisine erişim yetkin yok.");return true;
        }
        try {
            switch(sub) {
                case "bilgi" -> {
                    String name=args.length>1?args[1]:sender instanceof Player p?p.getName():null;
                    if(name==null){tell(sender,"Kullanım: /rol bilgi oyuncu");break;}
                    Role r=role(name);tell(sender,name+" → "+r.label()+" ("+r.id()+")");
                }
                case "liste" -> {
                    tell(sender,"Roller: "+String.join(", ",configuration.catalog().all().stream()
                            .sorted(Comparator.comparingInt(Role::priority).reversed()).map(r->r.id()+" ["+r.label()+"]").toList()));
                    if(sender.hasPermission("kasabaroles.admin"))tell(sender,"Komutlar: ver, al, oluştur, isim, renk, sıra, yetki, sil, yenile");
                }
                case "ver" -> {
                    length(args,3,"/rol ver oyuncu rol");changeRole(args[1],args[2]);tell(sender,args[1]+" oyuncusuna "+role(args[1]).label()+" rolü verildi.");
                }
                case "al","kaldır","kaldir" -> {
                    length(args,2,"/rol al oyuncu");changeRole(args[1],null);tell(sender,args[1]+" varsayılan role döndü.");
                }
                case "yenile" -> {load();applyAll();tell(sender,"Roller, oyuncular ve yetkiler yenilendi.");}
                case "oluştur","olustur" -> {
                    length(args,3,"/rol oluştur kimlik renk [görünen ad]");String id=Role.normalizeId(args[1]);
                    if(configuration.catalog().get(id)!=null)throw new IllegalArgumentException("Bu rol zaten var");
                    String hex=color(args[2]).asHexString();String display=args.length>3?String.join(" ",Arrays.copyOfRange(args,3,args.length)):id;
                    edit(y->{String base="roles."+id; y.set(base+".label",display);y.set(base+".color",hex);y.set(base+".name-color","white");
                        y.set(base+".priority",1);y.set(base+".inherits",List.of(configuration.catalog().defaultRole().id()));y.createSection(base+".permissions");});
                    tell(sender,"Rol oluşturuldu: "+id);
                }
                case "isim","renk","sıra","sira" -> {
                    length(args,3,"/rol "+sub+" rol değer");String id=Role.normalizeId(args[1]);requireRole(id);
                    String path="roles."+id+".";
                    switch(sub){
                        case "isim" -> {String display=String.join(" ",Arrays.copyOfRange(args,2,args.length));edit(y->y.set(path+"label",display));}
                        case "renk" -> {String hex=color(args[2]).asHexString();edit(y->y.set(path+"color",hex));}
                        default -> {int priority=Integer.parseInt(args[2]);edit(y->y.set(path+"priority",priority));}
                    }
                    tell(sender,"Rol güncellendi: "+id);
                }
                case "yetki" -> {
                    length(args,4,"/rol yetki rol yetki.düğümü ver|yasakla|sil");String id=Role.normalizeId(args[1]);requireRole(id);
                    String node=Role.permissionNode(args[2]);Boolean value=switch(args[3].toLowerCase(Locale.ROOT)){
                        case "ver"->true;case "yasakla"->false;case "sil"->null;default->throw new IllegalArgumentException("İşlem ver, yasakla veya sil olmalı");};
                    edit(y->y.set("roles."+id+".permissions."+node,value));tell(sender,"Rol yetkisi güncellendi: "+id+" → "+node);
                }
                case "sil" -> {
                    length(args,2,"/rol sil rol");String id=Role.normalizeId(args[1]);requireRole(id);
                    if(id.equals(configuration.catalog().defaultRole().id()))throw new IllegalArgumentException("Varsayılan rol silinemez");
                    edit(y->y.set("roles."+id,null));tell(sender,"Rol silindi: "+id);
                }
                default -> tell(sender,"Kullanım: /rol liste | bilgi | ver | al | oluştur | isim | renk | sıra | yetki | sil | yenile");
            }
        } catch(NumberFormatException e){tell(sender,"Sıra bir sayı olmalı.");}
        catch(IllegalArgumentException e){tell(sender,e.getMessage());}
        catch(IOException e){getLogger().log(Level.SEVERE,"Rol dosyası işlemi başarısız",e);tell(sender,"Dosya okunamadı veya kaydedilemedi. Konsolu kontrol et.");}
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args) {
        if(configuration==null)return List.of();
        List<String> values=new ArrayList<>();
        boolean admin=sender.hasPermission("kasabaroles.admin");
        if(args.length==1)values=admin?List.of("ver","al","bilgi","liste","oluştur","isim","renk","sıra","yetki","sil","yenile"):List.of("bilgi","liste");
        else if(args.length==2){
            if(Set.of("ver","al","bilgi").contains(args[0]))values=Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            else if(admin&&Set.of("isim","renk","sıra","sira","yetki","sil").contains(args[0]))values=configuration.catalog().all().stream().map(Role::id).toList();
        } else if(args.length==3&&admin){
            if(args[0].equals("ver"))values=configuration.catalog().all().stream().map(Role::id).toList();
            else if(Set.of("renk","oluştur","olustur").contains(args[0]))values=List.of("kırmızı","koyu_kırmızı","yeşil","altın","turkuaz","mavi","mor","beyaz","gri");
        } else if(args.length==4&&admin&&args[0].equals("yetki"))values=List.of("ver","yasakla","sil");
        String prefix=args[args.length-1].toLowerCase(Locale.ROOT);
        return values.stream().filter(v->v.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}

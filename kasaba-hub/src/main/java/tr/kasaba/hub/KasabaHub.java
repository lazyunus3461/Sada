package tr.kasaba.hub;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import tr.kasaba.auth.api.*;
import tr.kasaba.hub.api.*;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;

public final class KasabaHub extends JavaPlugin implements Listener,TabCompleter,HubApi {
    private record Entry(Plugin owner,HubGame game,GameInfo info){}
    private final PartyBook parties=new PartyBook();
    private final Map<String,Entry> games=new TreeMap<>();
    private final Map<UUID,Entry> playing=new HashMap<>();
    private final Set<UUID> editors=new HashSet<>(),leaving=new HashSet<>();
    private final Map<UUID,Long> pages=new HashMap<>(),travel=new HashMap<>(),navigation=new HashMap<>();
    private long sequence,lobbyRevision;
    private HubSettings settings;
    private LobbyStore store;
    private LobbyStore.Point lobby;
    private DestinationStore destinations;
    private Map<String,DestinationStore.Destination> savedGames=Map.of();
    private LobbyItems items;
    private LeaderboardStore leaderboards;private LobbyDisplays lobbyDisplays;private boolean leaderboardReady;
    private boolean storageFailed,stopping,penaltyStorageReady;private MatchPenalties penalties;private final Set<UUID> forcedLeaves=new HashSet<>(),transferring=new HashSet<>();

    @Override public void onEnable(){
        saveDefaultConfig();store=new LobbyStore(getDataFolder().toPath().resolve("lobi.yml"));destinations=new DestinationStore(getDataFolder().toPath().resolve("oyunlar.yml"));
        leaderboards=new LeaderboardStore(getDataFolder().toPath().resolve("liderlik.properties"));try{leaderboards.load();leaderboardReady=true;}catch(IOException e){getLogger().log(Level.SEVERE,"Liderlik kaydı bozuk; oyunlar açık, dosya korunuyor.",e);}lobbyDisplays=new LobbyDisplays(leaderboards);
        penalties=new MatchPenalties(getDataFolder().toPath().resolve("mac-cezalari.properties"));
        try{penalties.load();penaltyStorageReady=true;settings=HubSettings.read(getDataFolder().toPath().resolve("config.yml"));lobby=store.load();replaceDestinations(destinations.load());}
        catch(IOException e){storageFailed=true;getLogger().log(Level.SEVERE,"Hub ayarları okunamadı; dosyalar korundu. Düzeltip /merkez yenile kullan.",e);}
        items=new LobbyItems(this);Bukkit.getPluginManager().registerEvents(this,this);Bukkit.getPluginManager().registerEvents(new LobbyProtection(this),this);Bukkit.getPluginManager().registerEvents(items,this);
        for(String name:List.of("merkez","oyunlar","grup","lobi","rehber","oyunceza"))Objects.requireNonNull(getCommand(name)).setTabCompleter(this);
        Bukkit.getServicesManager().register(HubApi.class,this,this,ServicePriority.Normal);
        Bukkit.getScheduler().runTaskTimer(this,parties::prune,100,100);
        Bukkit.getScheduler().runTaskTimer(this,lobbyDisplays::refresh,40,40);
        Bukkit.getScheduler().runTaskTimer(this,()->{try{if(leaderboardReady)leaderboards.save();}catch(IOException e){getLogger().log(Level.SEVERE,"Liderlik kaydı yeniden denenecek.",e);}},1200,1200);
        Bukkit.getScheduler().runTaskTimer(this,()->{try{if(penaltyStorageReady)penalties.save();}catch(IOException e){getLogger().log(Level.WARNING,"Maç cezası dosyası kaydedilemedi; bellek kaydı korunuyor.",e);}},1200,1200);
        Bukkit.getScheduler().runTaskTimer(this,()->Bukkit.getOnlinePlayers().forEach(items::refresh),1,20);
    }
    @Override public void onDisable(){stopping=true;if(lobbyDisplays!=null)lobbyDisplays.close();try{if(leaderboardReady)leaderboards.save();}catch(IOException e){getLogger().log(Level.SEVERE,"Liderlik kaydı saklanamadı.",e);}try{if(penaltyStorageReady)penalties.save();}catch(IOException e){getLogger().log(Level.SEVERE,"Maç cezaları kaydedilemedi.",e);}
        Set<UUID> ownedPages=Set.copyOf(pages.keySet());pages.clear();travel.clear();navigation.clear();editors.clear();Bukkit.getServicesManager().unregisterAll(this);
        for(UUID id:new ArrayList<>(playing.keySet()))detach(id);games.clear();
        if(items!=null)Bukkit.getOnlinePlayers().forEach(items::forget);
        for(Player p:Bukkit.getOnlinePlayers())if(ownedPages.contains(p.getUniqueId())&&authenticated(p))p.closeDialog();
    }
    private static Component text(String value,NamedTextColor color){return Component.text(value,color);}
    void tell(CommandSender sender,String message){sender.sendMessage(text(message,NamedTextColor.YELLOW));}
    boolean authenticated(Player p){var auth=Bukkit.getServicesManager().getRegistration(AuthState.class);return auth!=null&&auth.getProvider().authenticated(p.getUniqueId());}
    private void ready(Player p){if(!authenticated(p))throw new IllegalArgumentException("Önce kayıt veya giriş işlemini tamamla.");var guard=Bukkit.getServicesManager().load(tr.kasaba.hub.api.GameAccessGuard.class);if(guard!=null&&guard.blocked(p.getUniqueId()))throw new IllegalArgumentException(guard.message(p.getUniqueId()));if(storageFailed||settings==null)throw new IllegalArgumentException("Merkez dosyası okunamadı; yönetici /merkez yenile kullanmalı.");if(!p.hasPermission("kasabahub.kullan"))throw new IllegalArgumentException("Oyun merkezi yetkin yok.");}
    boolean admin(CommandSender s){return s.hasPermission("kasabahub.yonet")||s.hasPermission("kasabaroles.admin");}
    boolean editing(Player p){return editors.contains(p.getUniqueId())&&admin(p);}
    boolean inLobby(Location l){return storageFailed||(settings!=null&&lobby!=null&&l!=null&&l.getWorld()!=null&&lobby.contains(l.getWorld().getUID(),l.getX(),l.getZ(),settings.radius()));}
    boolean protectedPlayer(Player p){return !playing(p.getUniqueId())&&inLobby(p.getLocation());}
    boolean menuItemAllowed(Player p){return !storageFailed&&settings!=null&&lobby!=null&&p.isOnline()&&!p.isDead()&&authenticated(p)&&p.hasPermission("kasabahub.kullan")&&protectedPlayer(p);}
    void openGuide(Player p){try{ready(p);pages.remove(p.getUniqueId());navigation.put(p.getUniqueId(),++sequence);p.closeDialog();items.openGuide(p);}catch(IllegalArgumentException ex){tell(p,ex.getMessage());}}
    void openItemMenu(Player p){try{showMain(p);}catch(IllegalArgumentException e){tell(p,e.getMessage());}}
    private Player player(CommandSender s){if(s instanceof Player p)return p;throw new IllegalArgumentException("Bu komutu oyunda kullan.");}
    private Player online(String name){Player p=Bukkit.getPlayerExact(name);if(p==null||!authenticated(p))throw new IllegalArgumentException("Oyuncu çevrimiçi ve giriş yapmış olmalı.");return p;}
    private String name(UUID id){Player p=Bukkit.getPlayer(id);return p==null?"Ayrılmış oyuncu":p.getName();}
    private void need(String[] a,int n,String usage){if(a.length<n)throw new IllegalArgumentException("Kullanım: "+usage);}
    private String verb(String s){return s.toLowerCase(Locale.ROOT).replace("\u0307","").replace('ı','i').replace('ş','s').replace('ç','c').replace('ğ','g').replace('ü','u').replace('ö','o');}
    private void broadcast(PartyBook.Party party,String message){for(UUID id:party.members()){Player p=Bukkit.getPlayer(id);if(p!=null)tell(p,message);}}
    private void free(PartyBook.Party party){if(party!=null&&party.members().stream().anyMatch(this::gameplay))throw new IllegalArgumentException("Gruptaki oyunlar bitmeden grup değiştirilemez. Önce /lobi ile oyundan çık.");}
    private void free(Player p){if(gameplay(p.getUniqueId()))throw new IllegalArgumentException("Önce /lobi ile oyundan çık.");free(parties.party(p.getUniqueId()));}
    private void invite(Player p,String name){free(p);Player target=online(name);free(target);PartyBook.Party group=parties.invite(p.getUniqueId(),target.getUniqueId(),settings.capacity(),settings.invitation());tell(target,p.getName()+" seni grubuna davet etti. /grup katıl "+p.getName()+" • "+settings.invitation()+" saniye");tell(p,"Grup daveti gönderildi: "+target.getName());}
    @Override public boolean onCommand(CommandSender s,Command cmd,String label,String[] a){
        if(cmd.getName().equals("oyunceza"))return penaltyCommand(s,a);
        try{
            if(cmd.getName().equals("merkez")&&a.length>0&&verb(a[0]).equals("yenile")){
                if(!admin(s))throw new IllegalArgumentException("Yönetim yetkin yok.");HubSettings next=HubSettings.read(getDataFolder().toPath().resolve("config.yml"));LobbyStore.Point point=store.load();replaceDestinations(destinations.load());if(!penaltyStorageReady){penalties.load();penaltyStorageReady=true;}settings=next;lobby=point;storageFailed=false;lobbyRevision++;travel.clear();Bukkit.getOnlinePlayers().forEach(items::refresh);tell(s,"Merkez ayarları, oyun girişleri ve lobi yenilendi. Gruplar korunuyor.");return true;
            }
            Player p=player(s);ready(p);
            switch(cmd.getName()){
                case "oyunlar" -> gameCommand(p,a);
                case "rehber" -> openGuide(p);
                case "lobi" -> returnLobby(p,false);
                case "grup" -> groupCommand(p,a);
                case "merkez" -> {
                    if(a.length==0){showMain(p);break;}
                    switch(verb(a[0])){
                        case "yardim" -> tell(p,"/merkez • /oyunlar • /grup • /lobi • Yönetim: /merkez lobi ayarla|bilgi, düzenleme aç|kapat, yenile");
                        case "lobi" -> {
                            if(!admin(p))throw new IllegalArgumentException("Lobi yönetim yetkin yok.");need(a,2,"/merkez lobi ayarla|bilgi");
                            if(verb(a[1]).equals("ayarla")){
                                LobbyStore.Point next=currentPoint(p);validate(next);store.save(next);lobby=next;lobbyRevision++;travel.clear();Bukkit.getOnlinePlayers().forEach(items::refresh);tell(p,"Ana lobi ayarlandı. Giriş yapanlar artık burada başlayacak.");
                            }else if(verb(a[1]).equals("bilgi"))tell(p,lobby==null?"Lobi ayarlı değil. /merkez lobi ayarla":String.format(Locale.ROOT,"Lobi %s: %.2f, %.2f, %.2f • Koruma: %d blok",lobby.worldName(),lobby.x(),lobby.y(),lobby.z(),settings.radius()));
                            else throw new IllegalArgumentException("/merkez lobi ayarla veya /merkez lobi bilgi");
                        }
                        case "duzenleme" -> {
                            if(!admin(p))throw new IllegalArgumentException("Düzenleme yetkin yok.");need(a,2,"/merkez düzenleme aç|kapat");String mode=verb(a[1]);if(!Set.of("ac","kapat").contains(mode))throw new IllegalArgumentException("/merkez düzenleme aç veya kapat");
                            if(mode.equals("ac"))editors.add(p.getUniqueId());else editors.remove(p.getUniqueId());tell(p,"Lobi düzenleme modu "+(mode.equals("ac")?"açıldı.":"kapatıldı."));getLogger().info(p.getName()+" lobi düzenleme: "+mode);
                        }
                        default -> throw new IllegalArgumentException("/merkez yardım");
                    }
                }
            }
        }catch(IllegalArgumentException e){tell(s,e.getMessage());}catch(IOException e){getLogger().log(Level.SEVERE,"Hub dosyası işlemi başarısız; mevcut veri korundu.",e);tell(s,"Dosya okunamadı/kaydedilemedi; değişiklik uygulanmadı.");}
        return true;
    }
    private boolean penaltyCommand(CommandSender sender,String[] args){try{if(sender instanceof Player actor)ready(actor);if(!penaltyStorageReady)throw new IllegalArgumentException("Maç cezası kaydı okunamadı; önce dosyayı düzeltip /merkez yenile kullan.");if(args.length==0&&sender instanceof Player p){var active=penalties.active(p.getUniqueId(),System.currentTimeMillis());if(active.isEmpty())tell(p,"Oyun giriş cezan yok.");else active.forEach((game,left)->tell(p,(games.containsKey(game)?games.get(game).info().name():game)+" • "+((left+59999)/60000)+" dakika kaldı."));return true;}if(!admin(sender))throw new IllegalArgumentException("Bu işlem için yetkin yok.");if(args.length<2||!verb(args[0]).equals("kaldir"))throw new IllegalArgumentException("/oyunceza kaldır Oyuncu [oyun]");var target=Bukkit.getOfflinePlayer(args[1]);String game=args.length<3?null:switch(verb(args[2])){case "sansyolu","sans"->"ispanya";case "at"->"atyarisi";case "tekne"->"tekneyarisi";case "br"->"battle";case "vk"->"vampirkoylu";default->verb(args[2]);};int count=penalties.clear(target.getUniqueId(),game);tell(sender,"Kaldırılan oyun cezası: "+count);}catch(Exception e){tell(sender,e.getMessage()==null?"Ceza işlemi tamamlanamadı.":e.getMessage());}return true;}
    private LobbyStore.Point currentPoint(Player p){Location l=p.getLocation();return new LobbyStore.Point(p.getWorld().getUID(),p.getWorld().getName(),l.getX(),l.getY(),l.getZ(),l.getYaw(),l.getPitch());}
    private final class DestinationGame implements HubGame {
        private final DestinationStore.Destination destination;
        DestinationGame(DestinationStore.Destination destination){this.destination=destination;}
        public GameInfo info(){return new GameInfo(destination.id(),destination.name(),"Kayıtlı giriş noktası • Oyun kuralları ayrıca eklenir.",1,20);}
        public boolean available(){try{validate(destination.point());return true;}catch(IllegalArgumentException e){return false;}}
        public boolean join(List<Player> team){throw new IllegalStateException("Giriş noktası asenkron hazırlanmalı.");}
        public boolean lobby(UUID id){Player p=Bukkit.getPlayer(id);try{return p!=null&&p.getWorld()==validate(destination.point()).getWorld()&&p.getLocation().distanceSquared(validate(destination.point()))<=256;}catch(RuntimeException ex){return false;}}
        public void leave(UUID id){}
    }
    private void checkDestinations(Map<String,DestinationStore.Destination> next){
        for(String id:next.keySet()){Entry existing=games.get(id);if(existing!=null&&!(existing.game() instanceof DestinationGame))throw new IllegalArgumentException("Bu kimlik bir oyun eklentisine ait: "+id);}
        for(Entry entry:games.values())if(entry.game() instanceof DestinationGame game&&!Objects.equals(game.destination,next.get(entry.info().id()))&&playing.containsValue(entry))throw new IllegalArgumentException("Bu giriş kullanılıyor; önce oyuncular /lobi ile çıksın: "+entry.info().id());
    }
    private void replaceDestinations(Map<String,DestinationStore.Destination> next){
        checkDestinations(next);
        games.entrySet().removeIf(e->e.getValue().game() instanceof DestinationGame game&&!Objects.equals(game.destination,next.get(e.getKey())));
        for(var d:next.values())if(!games.containsKey(d.id())){DestinationGame game=new DestinationGame(d);games.put(d.id(),new Entry(this,game,game.info()));}
        savedGames=Map.copyOf(next);
    }
    private void gameCommand(Player p,String[] a) throws IOException {
        if(a.length==0){showGames(p,0);return;}
        String action=verb(a[0]);
        if(action.equals("yardim")){tell(p,"/oyunlar • /oyunlar git <kimlik> • Yönetim: /oyunlar ekle <kimlik> <görünen ad>, konum <kimlik>, sil <kimlik>");return;}
        if(action.equals("git")){need(a,2,"/oyunlar git <kimlik>");Entry entry=games.get(verb(a[1]));if(entry==null)throw new IllegalArgumentException("Bu oyun bulunamadı.");join(p,entry);return;}
        if(!admin(p))throw new IllegalArgumentException("Oyun ekleme ve düzenleme yetkin yok.");
        need(a,2,"/oyunlar ekle <kimlik> <görünen ad> | konum <kimlik> | sil <kimlik>");String id=verb(a[1]);
        Map<String,DestinationStore.Destination> next=new TreeMap<>(savedGames);String message;
        switch(action){
            case "ekle" -> {
                if(games.containsKey(id))throw new IllegalArgumentException("Bu oyun kimliği zaten var. Konumu değiştirmek için /oyunlar konum "+id);
                String title=a.length>2?String.join(" ",Arrays.copyOfRange(a,2,a.length)):a[1];LobbyStore.Point point=currentPoint(p);validate(point);
                next.put(id,new DestinationStore.Destination(id,title,point));message="Oyun girişi eklendi: "+title+". Bulunduğun konum kaydedildi; /oyunlar ile açabilirsin.";
            }
            case "konum" -> {var old=next.get(id);if(old==null)throw new IllegalArgumentException("Komutla eklenmiş oyun bulunamadı.");LobbyStore.Point point=currentPoint(p);validate(point);next.put(id,new DestinationStore.Destination(id,old.name(),point));message="Oyun giriş konumu güncellendi: "+old.name();}
            case "sil" -> {if(next.remove(id)==null)throw new IllegalArgumentException("Komutla eklenmiş oyun bulunamadı; eklenti oyunlarını kendi eklentisinden yönet.");message="Oyun girişi silindi: "+id;}
            default -> throw new IllegalArgumentException("/oyunlar yardım");
        }
        checkDestinations(next);destinations.save(next);replaceDestinations(next);tell(p,message);
    }
    private void groupCommand(Player p,String[] a){
        if(!socialArea(p.getUniqueId()))throw new IllegalArgumentException("Arkadaş ve grup işlemlerini yalnız spawn ve lobilerde kullanabilirsin.");
        if(a.length==0){showGroup(p);return;}String action=verb(a[0]);UUID id=p.getUniqueId();
        if(action.equals("yardim")){tell(p,"/grup oluştur | davet <oyuncu> | katıl <lider> | çıkar <oyuncu> | lider <oyuncu> | ayrıl | dağıt onayla | sohbet <mesaj>");return;}
        if(action.equals("sohbet")){need(a,2,"/grup sohbet <mesaj>");PartyBook.Party group=parties.party(id);if(group==null)throw new IllegalArgumentException("Bir grupta değilsin.");String message=String.join(" ",Arrays.copyOfRange(a,1,a.length));if(message.length()>256)throw new IllegalArgumentException("Mesaj en fazla 256 karakter olabilir.");for(UUID member:group.members()){Player target=Bukkit.getPlayer(member);if(target!=null&&socialArea(member))tell(target,"[Grup] "+p.getName()+" » "+message);}return;}
        free(p);
        switch(action){
            case "olustur" -> {parties.create(id);tell(p,"Arkadaş grubun oluşturuldu. /grup davet <oyuncu>");}
            case "davet" -> {need(a,2,"/grup davet <oyuncu>");invite(p,a[1]);}
            case "katil" -> {need(a,2,"/grup katıl <lider>");Player leader=online(a[1]);free(leader);PartyBook.Party result=parties.accept(id,leader.getUniqueId(),settings.capacity());broadcast(result,p.getName()+" gruba katıldı.");}
            case "cikar" -> {need(a,2,"/grup çıkar <oyuncu>");Player target=online(a[1]);PartyBook.Party old=parties.leader(id);parties.kick(id,target.getUniqueId());broadcast(old,target.getName()+" gruptan çıkarıldı.");}
            case "lider" -> {need(a,2,"/grup lider <oyuncu>");PartyBook.Party next=parties.transfer(id,online(a[1]).getUniqueId());broadcast(next,"Grup liderliği devredildi: "+a[1]);}
            case "ayril" -> {PartyBook.Party old=parties.party(id);if(old==null)throw new IllegalArgumentException("Bir grupta değilsin.");PartyBook.Party next=parties.leave(id);broadcast(old,p.getName()+" gruptan ayrıldı.");if(next!=null)broadcast(next,"Grup lideri: "+name(next.leader()));}
            case "dagit" -> {if(a.length<2||!verb(a[1]).equals("onayla"))throw new IllegalArgumentException("Onaylamak için /grup dağıt onayla yaz.");broadcast(parties.disband(id),"Grup dağıtıldı.");}
            default -> throw new IllegalArgumentException("/grup yardım");
        }
    }
    private interface FormAction {void run(io.papermc.paper.dialog.DialogResponseView response);}
    private record Button(String label,FormAction action){}
    private void show(Player p,String title,List<String> lines,List<DialogInput> inputs,List<Button> buttons){
        ready(p);long token=++sequence;pages.put(p.getUniqueId(),token);navigation.put(p.getUniqueId(),token);List<ActionButton> actions=new ArrayList<>();
        for(Button button:buttons){DialogAction click=DialogAction.customClick((response,audience)->{
            if(!(audience instanceof Player actor)||!actor.getUniqueId().equals(p.getUniqueId())||!isEnabled())return;
            Bukkit.getScheduler().runTask(this,()->{
                if(!p.isOnline()||!Objects.equals(pages.get(p.getUniqueId()),token))return;
                try{ready(p);pages.remove(p.getUniqueId());navigation.put(p.getUniqueId(),++sequence);p.closeDialog();button.action().run(response);}catch(IllegalArgumentException e){tell(p,e.getMessage());}
            });
        },ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(2)).build());actions.add(ActionButton.create(tr.kasaba.hub.api.MenuStyle.button(button.label()),null,150,click));}
        List<DialogBody> body=lines.stream().<DialogBody>map(line->DialogBody.plainMessage(tr.kasaba.hub.api.MenuStyle.line(line),320)).toList();
        p.showDialog(Dialog.create(builder->builder.empty().base(DialogBase.builder(tr.kasaba.hub.api.MenuStyle.title(title)).canCloseWithEscape(true).pause(false).afterAction(DialogBase.DialogAfterAction.NONE).body(body).inputs(inputs).build()).type(DialogType.multiAction(actions).columns(2).build())));
    }
    private Button button(String label,Runnable action){return new Button(label,response->action.run());}
    private Button close(){return button("Kapat",()->{});}
    private void showMain(Player p){PartyBook.Party group=parties.party(p.getUniqueId());show(p,"Kasaba • Oyun Merkezi",List.of("Arkadaşlarınla buluş, grubunu kur ve oyunlara birlikte katıl.","Çevrimiçi: "+Bukkit.getOnlinePlayers().size()+" • Grubun: "+(group==null?"Yok":group.members().size()+" kişi"),games.isEmpty()?"İlk oyun yakında. Şimdilik lobide arkadaşlarınla buluşabilirsin.":"Oyunlar sayfasından hazır bir oyun seç."),List.of(),List.of(button("Oyunlar",()->showGames(p,0)),button("Arkadaşlar",()->{var network=NetworkFeatures.get();if(network==null)showGroup(p);else network.socialMenu(p);}),button("Grubum",()->showGroup(p)),button("Akçe nasıl kazanılır?",()->{var network=NetworkFeatures.get();if(network!=null)network.earnInfo(p);}),button("VIP satın al",()->{var network=NetworkFeatures.get();if(network==null)tell(p,"Market şu anda açılmıyor.");else network.market(p);}),button("VIP özellikleri",()->{var network=NetworkFeatures.get();if(network!=null)network.vipInfo(p);}),button("Lobiye dön",()->returnLobby(p,false)),close()));}
    private void showGroup(Player p){
        if(!socialArea(p.getUniqueId()))throw new IllegalArgumentException("Grup menüsü yalnız spawn ve lobilerde açılır.");
        PartyBook.Party group=parties.party(p.getUniqueId());List<String> lines=new ArrayList<>();List<Button> buttons=new ArrayList<>();
        if(group==null){lines.add("Arkadaşlarınla aynı oyuna girmek için bir grup oluştur.");buttons.add(button("Grup oluştur",()->{free(p);parties.create(p.getUniqueId());showGroup(p);}));lines.add("Davet aldıysan /grup katıl <lider> kullan.");}
        else{lines.add("Lider: "+name(group.leader())+" • "+group.members().size()+"/"+settings.capacity());for(UUID member:group.members())lines.add((member.equals(group.leader())?"★ ":"• ")+name(member));
            if(group.leader().equals(p.getUniqueId())){buttons.add(button("Arkadaş davet et",()->showInvite(p)));buttons.add(button("Grubu dağıt",()->show(p,"Grubu dağıt?",List.of("Grubundaki herkes ayrılacak."),List.of(),List.of(button("Evet, dağıt",()->{free(p);broadcast(parties.disband(p.getUniqueId()),"Grup dağıtıldı.");showGroup(p);}),button("Vazgeç",()->showGroup(p))))));}
            buttons.add(button("Gruptan ayrıl",()->{free(p);PartyBook.Party old=parties.party(p.getUniqueId());if(old!=null){PartyBook.Party next=parties.leave(p.getUniqueId());broadcast(old,p.getName()+" gruptan ayrıldı.");if(next!=null)broadcast(next,"Grup lideri: "+name(next.leader()));}showGroup(p);}));}
        buttons.add(button("Merkeze dön",()->showMain(p)));buttons.add(close());show(p,"Kasaba • Arkadaş Grubu",lines,List.of(),buttons);
    }
    private void showInvite(Player p){parties.leader(p.getUniqueId());show(p,"Arkadaşını davet et",List.of("Çevrimiçi ve giriş yapmış arkadaşının Minecraft adını yaz."),List.of(DialogInput.text("oyuncu",text("Oyuncu adı",NamedTextColor.WHITE)).maxLength(16).width(280).build()),List.of(new Button("Davet gönder",response->{String value=response.getText("oyuncu");if(value==null||!value.matches("[A-Za-z0-9_]{1,16}"))throw new IllegalArgumentException("Geçerli bir oyuncu adı yaz.");invite(p,value);showGroup(p);}),button("Vazgeç",()->showGroup(p))));}
    private boolean available(Entry entry){try{return entry.owner().isEnabled()&&entry.game().available();}catch(RuntimeException e){getLogger().log(Level.WARNING,"Oyun durumu okunamadı: "+entry.info().id(),e);return false;}}
    private void showGames(Player p,int requested){
        List<Entry> entries=new ArrayList<>(games.values());int last=Math.max(0,(entries.size()-1)/8),page=Math.clamp(requested,0,last);List<String> lines=new ArrayList<>();List<Button> buttons=new ArrayList<>();
        if(entries.isEmpty())lines.add("Oyunlar yakında! Henüz oynanabilir bir oyun eklenmedi.");
        else{for(int i=page*8;i<Math.min(entries.size(),page*8+8);i++){Entry entry=entries.get(i);GameInfo info=entry.info();buttons.add(button(info.name()+(available(entry)?"":" • Bakımda")+(penalties.remaining(p.getUniqueId(),info.id(),System.currentTimeMillis())>0?" • "+((penalties.remaining(p.getUniqueId(),info.id(),System.currentTimeMillis())+59999)/60000)+" dk ceza":""),()->join(p,entry)));}}
        if(page>0)buttons.add(button("Önceki",()->showGames(p,page-1)));if(page<last)buttons.add(button("Sonraki",()->showGames(p,page+1)));buttons.add(button("Merkeze dön",()->showMain(p)));buttons.add(close());show(p,"Kasaba • Oyunlar",lines,List.of(),buttons);
    }
    private void join(Player p,Entry entry){
        ready(p);if(games.get(entry.info().id())!=entry||!available(entry))throw new IllegalArgumentException("Bu oyun şu anda hazır değil.");
        PartyBook.Party group=parties.party(p.getUniqueId());if(group!=null)parties.leader(p.getUniqueId());List<UUID> ids=group==null?List.of(p.getUniqueId()):java.util.stream.Stream.concat(java.util.stream.Stream.of(p.getUniqueId()),group.members().stream().filter(id->!id.equals(p.getUniqueId()))).toList();
        if(ids.size()<entry.info().minimum()||ids.size()>entry.info().maximum())throw new IllegalArgumentException("Bu oyun "+entry.info().minimum()+"–"+entry.info().maximum()+" kişilik grup kabul ediyor.");
        List<Player> team=new ArrayList<>();for(UUID id:ids){Player member=Bukkit.getPlayer(id);if(member==null)throw new IllegalArgumentException("Gruptaki bir oyuncu ayrıldı.");ready(member);requireGameEntry(id,entry.info().id());if(gameplay(id))throw new IllegalArgumentException("Gruptaki bir oyuncu zaten oyunda.");team.add(member);}
        for(UUID id:ids)if(playing(id)){transferring.add(id);try{detach(id);}finally{transferring.remove(id);}}
        ids.forEach(id->playing.put(id,entry));
        for(Player member:team){if(NetworkFeatures.get()!=null)NetworkFeatures.get().beforeGame(member);items.remove(member);pages.remove(member.getUniqueId());travel.remove(member.getUniqueId());navigation.put(member.getUniqueId(),++sequence);member.closeDialog();}
        if(entry.game() instanceof DestinationGame destination){joinDestination(List.copyOf(team),entry,destination);return;}
        boolean success=false;
        try{success=entry.game().join(List.copyOf(team));}catch(RuntimeException e){getLogger().log(Level.SEVERE,"Oyun katılımı başarısız: "+entry.info().id(),e);}
        if(!success||games.get(entry.info().id())!=entry||ids.stream().anyMatch(id->playing.get(id)!=entry)){
            for(UUID id:ids)if(playing.get(id)==entry)detach(id);team.forEach(items::refresh);throw new IllegalArgumentException("Oyun grubu kabul edemedi; tekrar dene.");
        }
        for(Player member:team){pages.remove(member.getUniqueId());travel.remove(member.getUniqueId());member.closeDialog();tell(member,entry.info().name()+" oyununa grubunla katıldın.");}
    }
    private void joinDestination(List<Player> team,Entry entry,DestinationGame game){
        Location target;
        try{target=validate(game.destination.point());}catch(IllegalArgumentException e){rejectDestination(team,entry,e.getMessage());return;}
        java.util.concurrent.CompletableFuture<Chunk> prepared;
        try{prepared=target.getWorld().getChunkAtAsync(target);}catch(RuntimeException e){getLogger().log(Level.WARNING,"Oyun bölgesi hazırlığı başlatılamadı.",e);rejectDestination(team,entry,"Oyun bölgesi hazırlanamadı; tekrar dene.");return;}
        boolean[] finished={false};
        prepared.whenComplete((chunk,error)->{
            if(!isEnabled())return;Bukkit.getScheduler().runTask(this,()->{
                if(finished[0])return;finished[0]=true;
                if(error!=null||games.get(entry.info().id())!=entry||team.stream().anyMatch(p->!p.isOnline()||!authenticated(p)||playing.get(p.getUniqueId())!=entry)){
                    rejectDestination(team,entry,"Oyun girişine geçiş iptal edildi; tekrar dene.");return;
                }
                try{for(Player member:team)ready(member);validate(game.destination.point());}catch(IllegalArgumentException e){rejectDestination(team,entry,e.getMessage());return;}
                Map<Player,Location> moved=new LinkedHashMap<>();boolean success=true;
                try{
                    for(Player member:team){Location before=member.getLocation();
                        if(playing.get(member.getUniqueId())!=entry||!member.teleport(target,PlayerTeleportEvent.TeleportCause.PLUGIN)){success=false;break;}
                        moved.put(member,before);member.setFallDistance(0);
                    }
                    if(team.stream().anyMatch(p->!p.isOnline()||playing.get(p.getUniqueId())!=entry))success=false;
                }catch(RuntimeException e){success=false;getLogger().log(Level.WARNING,"Oyun girişine ışınlanma başarısız.",e);}
                if(!success){
                    for(var previous:moved.entrySet())if(previous.getKey().isOnline())try{
                        if(!previous.getKey().teleport(previous.getValue(),PlayerTeleportEvent.TeleportCause.PLUGIN))getLogger().warning("Oyun girişini geri alma başka eklenti tarafından engellendi: "+previous.getKey().getName());
                    }catch(RuntimeException e){getLogger().log(Level.WARNING,"Oyun girişini geri alma başarısız: "+previous.getKey().getName(),e);}
                    rejectDestination(team,entry,"Işınlanma engellendi; grubun oyun girişi iptal edildi.");return;
                }
                for(Player member:team){items.refresh(member);tell(member,entry.info().name()+" girişine ışınlandın. Dönüş: /lobi • Menü: /oyunlar");}
            });
        });
        Bukkit.getScheduler().runTaskLater(this,()->{if(!finished[0]){finished[0]=true;rejectDestination(team,entry,"Oyun bölgesi zamanında hazırlanamadı; tekrar dene.");}},200);
    }
    private void rejectDestination(List<Player> team,Entry entry,String message){for(Player member:team){if(playing.get(member.getUniqueId())==entry)detach(member.getUniqueId());if(member.isOnline()){items.refresh(member);tell(member,message);}}}
    private void detach(UUID id){abandon(id);Entry entry=playing.remove(id);if(entry!=null&&entry.owner().isEnabled())try{entry.game().leave(id);}catch(RuntimeException e){getLogger().log(Level.WARNING,"Oyun çıkışı başarısız: "+entry.info().id(),e);}}
    private void mainThread(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Hub API ana iş parçacığından kullanılmalı.");}
    @Override public void register(Plugin owner,HubGame game){mainThread();Objects.requireNonNull(owner);Objects.requireNonNull(game);if(!owner.isEnabled())throw new IllegalArgumentException("Oyun eklentisi etkin değil.");GameInfo info=Objects.requireNonNull(game.info());if(games.containsKey(info.id()))throw new IllegalArgumentException("Bu oyun kimliği zaten kayıtlı; komutla eklenmiş giriş varsa önce /oyunlar sil "+info.id());games.put(info.id(),new Entry(owner,game,info));}
    @Override public void unregister(Plugin owner,String id){mainThread();Entry entry=games.get(id);if(entry==null)return;if(entry.owner()!=owner)throw new IllegalArgumentException("Başka eklentinin oyunu kaldırılamaz.");games.remove(id);if(lobbyDisplays!=null)lobbyDisplays.remove(id);for(UUID player:new ArrayList<>(playing.keySet()))if(playing.get(player)==entry)returnToLobbyForced(player);}
    @Override public boolean mainLobby(UUID id){Player p=Bukkit.getPlayer(id);return p!=null&&authenticated(p)&&!playing.containsKey(id)&&inLobby(p.getLocation());}
    @Override public boolean socialArea(UUID id){Player p=Bukkit.getPlayer(id);if(p==null||!authenticated(p))return false;if(mainLobby(id))return true;Entry entry=playing.get(id);return entry!=null&&entry.game().lobby(id);}
    @Override public boolean playing(UUID player){mainThread();return playing.containsKey(player);}
    @Override public void returnToLobby(UUID id){mainThread();Player p=Bukkit.getPlayer(id);if(p!=null&&authenticated(p))returnLobby(p,false);else detach(id);}
    @Override public void abandon(UUID id){mainThread();Entry entry=playing.get(id);if(stopping||Bukkit.isStopping()||forcedLeaves.contains(id)||entry==null||!entry.owner().isEnabled()||!entry.game().penaltyEligible(id))return;try{if(penalties.ban(id,entry.game().penaltyKey(id),System.currentTimeMillis())){Player p=Bukkit.getPlayer(id);if(p!=null)tell(p,"Maçı yarıda bıraktın: bu oyuna 15 dakika giremezsin.");}}catch(IOException e){getLogger().log(Level.SEVERE,"Maç cezası bellekte korundu; disk kaydı yeniden denenecek.",e);}}
    @Override public void requireGameEntry(UUID id,String game){mainThread();long left=penalties.remaining(id,game,System.currentTimeMillis());if(left>0){long seconds=(left+999)/1000;throw new IllegalArgumentException("Bu oyunu yarıda bıraktın. Yeniden girişe "+seconds/60+" dk "+seconds%60+" sn var.");}}
    @Override public void returnToLobbyForced(UUID id){forcedLeaves.add(id);try{returnToLobby(id);}finally{forcedLeaves.remove(id);}}
    private void ownsGame(Plugin owner,String game){mainThread();Entry entry=games.get(game);if(entry==null||entry.owner()!=owner)throw new IllegalArgumentException("Bu oyunun liderliğini yalnız kendi eklentisi yönetebilir.");}
    @Override public void presentLobby(Plugin owner,String game,Location spawn,String title,List<String> lines,String metric){ownsGame(owner,game);if(spawn!=null&&spawn.getWorld()!=null)lobbyDisplays.present(game,spawn,title,lines,metric);}
    @Override public void recordGameResult(Plugin owner,String game,UUID match,Collection<UUID> winners){ownsGame(owner,game);if(!leaderboardReady||stopping||Bukkit.isStopping())return;Map<UUID,String> names=new LinkedHashMap<>();for(UUID id:winners){String name=Bukkit.getOfflinePlayer(id).getName();names.put(id,name==null?"Oyuncu"+id.toString().substring(0,8):name);}try{leaderboards.result(game,match,names);lobbyDisplays.refresh();}catch(IOException e){getLogger().log(Level.SEVERE,"Liderlik bellekte saklandı; disk kaydı yeniden denenecek.",e);}}
    @Override public void setGameScore(Plugin owner,String game,UUID player,String name,long score){ownsGame(owner,game);if(leaderboardReady){leaderboards.total(game,player,name,score);lobbyDisplays.refresh();}}
    @Override public boolean transferring(UUID id){return transferring.contains(id);}
    @Override public void openGames(UUID id){mainThread();Player p=Bukkit.getPlayer(id);if(p==null)return;ready(p);if(!socialArea(id))throw new IllegalArgumentException("Oyun lobilerini güvenli lobideyken seçebilirsin.");showGames(p,0);}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void escaped(PlayerTeleportEvent e){UUID id=e.getPlayer().getUniqueId();Entry entry=playing.get(id);if(entry==null||!entry.game().penaltyEligible(id))return;Bukkit.getScheduler().runTask(this,()->{Player p=Bukkit.getPlayer(id);if(p!=null&&playing.get(id)==entry&&entry.game().penaltyEligible(id)&&!entry.game().containsMatch(id,p.getLocation())){abandon(id);returnToLobbyForced(id);}});}
    private Location validate(LobbyStore.Point point){
        World world=Bukkit.getWorld(point.world());if(world==null)throw new IllegalArgumentException("Lobi dünyası yüklü değil.");Location target=new Location(world,point.x(),point.y(),point.z(),point.yaw(),point.pitch());
        if(target.getY()<world.getMinHeight()||target.getY()>=world.getMaxHeight()||!world.getWorldBorder().isInside(target))throw new IllegalArgumentException("Lobi konumu dünya sınırları dışında.");return target;
    }
    private void returnLobby(Player p,boolean autoPage){
        ready(p);if(lobby==null)throw new IllegalArgumentException("Lobi ayarlanmamış. Yönetici /merkez lobi ayarla kullanmalı.");Location target=validate(lobby);
        if(!leaving.add(p.getUniqueId()))return;try{detach(p.getUniqueId());}finally{leaving.remove(p.getUniqueId());}
        pages.remove(p.getUniqueId());p.closeDialog();long token=++sequence,revision=lobbyRevision;UUID id=p.getUniqueId();travel.put(id,token);navigation.put(id,token);
        target.getWorld().getChunkAtAsync(target).whenComplete((chunk,error)->{
            if(!isEnabled())return;Bukkit.getScheduler().runTask(this,()->{
                if(!p.isOnline()||!Objects.equals(travel.get(id),token)||lobbyRevision!=revision||!authenticated(p))return;
                travel.remove(id);if(error!=null){tell(p,"Lobi bölgesi hazırlanamadı; tekrar dene.");return;}
                try{validate(lobby);}catch(IllegalArgumentException e){tell(p,e.getMessage());return;}
                if(!p.teleport(target,PlayerTeleportEvent.TeleportCause.PLUGIN)){items.refresh(p);tell(p,"Lobiye dönüş başka bir eklenti tarafından engellendi.");return;}p.setFallDistance(0);items.refresh(p);tell(p,"Kasaba lobisine hoş geldin!");
                if(autoPage)Bukkit.getScheduler().runTaskLater(this,()->{if(p.isOnline()&&authenticated(p)&&!playing(id)&&inLobby(p.getLocation())&&!pages.containsKey(id)&&Objects.equals(navigation.get(id),token))try{showMain(p);}catch(IllegalArgumentException ignored){}},40);
            });
        });
        Bukkit.getScheduler().runTaskLater(this,()->{if(Objects.equals(travel.get(id),token)){travel.remove(id);if(p.isOnline())tell(p,"Lobi zamanında hazırlanamadı; /lobi ile tekrar dene.");}},200);
    }
    @EventHandler public void authenticated(PlayerAuthenticatedEvent e){Bukkit.getScheduler().runTask(this,()->{Player p=e.getPlayer();if(!p.isOnline())return;try{if(lobby!=null)returnLobby(p,settings!=null&&settings.autoPage());else{if(admin(p))tell(p,"Oyun merkezi hazır. Lobi yerinde /merkez lobi ayarla kullan.");if(settings!=null&&settings.autoPage())showMain(p);}}catch(IllegalArgumentException ex){tell(p,ex.getMessage());}});}
    @EventHandler public void quit(PlayerQuitEvent e){UUID id=e.getPlayer().getUniqueId();items.forget(e.getPlayer());pages.remove(id);navigation.remove(id);travel.remove(id);editors.remove(id);detach(id);PartyBook.Party old=parties.party(id),next=parties.leave(id);if(old!=null&&next!=null)broadcast(next,e.getPlayer().getName()+" ayrıldı. Grup lideri: "+name(next.leader()));}
    @EventHandler public void pluginDisabled(PluginDisableEvent e){for(Entry entry:new ArrayList<>(games.values()))if(entry.owner()==e.getPlugin())unregister(entry.owner(),entry.info().id());}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void voidFall(PlayerMoveEvent e){Player p=e.getPlayer();if(authenticated(p)&&!playing(p.getUniqueId())&&!travel.containsKey(p.getUniqueId())&&inLobby(p.getLocation())&&p.getY()<p.getWorld().getMinHeight()-2)try{returnLobby(p,false);}catch(IllegalArgumentException ignored){}}
    @EventHandler(priority=EventPriority.HIGHEST) public void respawn(PlayerRespawnEvent e){if(!playing(e.getPlayer().getUniqueId())&&authenticated(e.getPlayer())&&lobby!=null)try{e.setRespawnLocation(validate(lobby));}catch(IllegalArgumentException ignored){}}
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){
        if(args.length==0||!(sender instanceof Player p)||!authenticated(p))return List.of();List<String> options=new ArrayList<>();
        if(command.getName().equals("merkez")&&admin(sender)){if(args.length==1)options.addAll(List.of("lobi","düzenleme","yenile","yardım"));else if(args.length==2&&verb(args[0]).equals("lobi"))options.addAll(List.of("ayarla","bilgi"));else if(args.length==2&&verb(args[0]).equals("duzenleme"))options.addAll(List.of("aç","kapat"));}
        if(command.getName().equals("oyunlar")){if(args.length==1){options.addAll(List.of("git","yardım"));if(admin(sender))options.addAll(List.of("ekle","konum","sil"));}else if(args.length==2&&verb(args[0]).equals("git"))options.addAll(games.keySet());else if(args.length==2&&admin(sender)&&Set.of("konum","sil").contains(verb(args[0])))options.addAll(savedGames.keySet());}
        if(command.getName().equals("grup")){if(args.length==1)options.addAll(List.of("oluştur","davet","katıl","çıkar","lider","ayrıl","dağıt","sohbet","yardım"));else if(args.length==2&&Set.of("davet","katil","cikar","lider").contains(verb(args[0])))options.addAll(Bukkit.getOnlinePlayers().stream().filter(this::authenticated).map(Player::getName).toList());else if(args.length==2&&verb(args[0]).equals("dagit"))options.add("onayla");}
        String prefix=verb(args[args.length-1]);return options.stream().filter(s->verb(s).startsWith(prefix)).toList();
    }
}

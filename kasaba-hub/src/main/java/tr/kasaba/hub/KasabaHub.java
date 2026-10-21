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
    private boolean storageFailed;

    @Override public void onEnable(){
        saveDefaultConfig();store=new LobbyStore(getDataFolder().toPath().resolve("lobi.yml"));
        try{settings=HubSettings.read(getDataFolder().toPath().resolve("config.yml"));lobby=store.load();}
        catch(IOException e){storageFailed=true;getLogger().log(Level.SEVERE,"Hub ayarları okunamadı; dosyalar korundu. Düzeltip /merkez yenile kullan.",e);}
        Bukkit.getPluginManager().registerEvents(this,this);Bukkit.getPluginManager().registerEvents(new LobbyProtection(this),this);
        for(String name:List.of("merkez","oyunlar","grup","lobi"))Objects.requireNonNull(getCommand(name)).setTabCompleter(this);
        Bukkit.getServicesManager().register(HubApi.class,this,this,ServicePriority.Normal);
        Bukkit.getScheduler().runTaskTimer(this,parties::prune,100,100);
    }
    @Override public void onDisable(){
        Set<UUID> ownedPages=Set.copyOf(pages.keySet());pages.clear();travel.clear();navigation.clear();editors.clear();Bukkit.getServicesManager().unregisterAll(this);
        for(UUID id:new ArrayList<>(playing.keySet()))detach(id);games.clear();
        for(Player p:Bukkit.getOnlinePlayers())if(ownedPages.contains(p.getUniqueId())&&authenticated(p))p.closeDialog();
    }
    private static Component text(String value,NamedTextColor color){return Component.text(value,color);}
    void tell(CommandSender sender,String message){sender.sendMessage(text(message,NamedTextColor.YELLOW));}
    boolean authenticated(Player p){var auth=Bukkit.getServicesManager().getRegistration(AuthState.class);return auth!=null&&auth.getProvider().authenticated(p.getUniqueId());}
    private void ready(Player p){if(!authenticated(p))throw new IllegalArgumentException("Önce kayıt veya giriş işlemini tamamla.");if(storageFailed||settings==null)throw new IllegalArgumentException("Merkez dosyası okunamadı; yönetici /merkez yenile kullanmalı.");if(!p.hasPermission("kasabahub.kullan"))throw new IllegalArgumentException("Oyun merkezi yetkin yok.");}
    boolean admin(CommandSender s){return s.hasPermission("kasabahub.yonet")||s.hasPermission("kasabaroles.admin");}
    boolean editing(Player p){return editors.contains(p.getUniqueId())&&admin(p);}
    boolean inLobby(Location l){return storageFailed||(settings!=null&&lobby!=null&&l!=null&&l.getWorld()!=null&&lobby.contains(l.getWorld().getUID(),l.getX(),l.getZ(),settings.radius()));}
    boolean protectedPlayer(Player p){return !playing(p.getUniqueId())&&inLobby(p.getLocation());}
    private Player player(CommandSender s){if(s instanceof Player p)return p;throw new IllegalArgumentException("Bu komutu oyunda kullan.");}
    private Player online(String name){Player p=Bukkit.getPlayerExact(name);if(p==null||!authenticated(p))throw new IllegalArgumentException("Oyuncu çevrimiçi ve giriş yapmış olmalı.");return p;}
    private String name(UUID id){Player p=Bukkit.getPlayer(id);return p==null?"Ayrılmış oyuncu":p.getName();}
    private void need(String[] a,int n,String usage){if(a.length<n)throw new IllegalArgumentException("Kullanım: "+usage);}
    private String verb(String s){return s.toLowerCase(Locale.ROOT).replace("\u0307","").replace('ı','i').replace('ş','s').replace('ç','c').replace('ğ','g').replace('ü','u').replace('ö','o');}
    private void broadcast(PartyBook.Party party,String message){for(UUID id:party.members()){Player p=Bukkit.getPlayer(id);if(p!=null)tell(p,message);}}
    private void free(PartyBook.Party party){if(party!=null&&party.members().stream().anyMatch(playing::containsKey))throw new IllegalArgumentException("Gruptaki oyunlar bitmeden grup değiştirilemez. Önce /lobi ile oyundan çık.");}
    private void free(Player p){if(playing(p.getUniqueId()))throw new IllegalArgumentException("Önce /lobi ile oyundan çık.");free(parties.party(p.getUniqueId()));}
    private void invite(Player p,String name){free(p);Player target=online(name);free(target);PartyBook.Party group=parties.invite(p.getUniqueId(),target.getUniqueId(),settings.capacity(),settings.invitation());tell(target,p.getName()+" seni grubuna davet etti. /grup katıl "+p.getName()+" • "+settings.invitation()+" saniye");tell(p,"Grup daveti gönderildi: "+target.getName());}
    @Override public boolean onCommand(CommandSender s,Command cmd,String label,String[] a){
        try{
            if(cmd.getName().equals("merkez")&&a.length>0&&verb(a[0]).equals("yenile")){
                if(!admin(s))throw new IllegalArgumentException("Yönetim yetkin yok.");HubSettings next=HubSettings.read(getDataFolder().toPath().resolve("config.yml"));LobbyStore.Point point=store.load();settings=next;lobby=point;storageFailed=false;lobbyRevision++;travel.clear();tell(s,"Merkez ayarları ve lobi yenilendi. Gruplar korunuyor.");return true;
            }
            Player p=player(s);ready(p);
            switch(cmd.getName()){
                case "oyunlar" -> showGames(p,0);
                case "lobi" -> returnLobby(p,false);
                case "grup" -> groupCommand(p,a);
                case "merkez" -> {
                    if(a.length==0){showMain(p);break;}
                    switch(verb(a[0])){
                        case "yardim" -> tell(p,"/merkez • /oyunlar • /grup • /lobi • Yönetim: /merkez lobi ayarla|bilgi, düzenleme aç|kapat, yenile");
                        case "lobi" -> {
                            if(!admin(p))throw new IllegalArgumentException("Lobi yönetim yetkin yok.");need(a,2,"/merkez lobi ayarla|bilgi");
                            if(verb(a[1]).equals("ayarla")){
                                Location l=p.getLocation();LobbyStore.Point next=new LobbyStore.Point(p.getWorld().getUID(),p.getWorld().getName(),l.getX(),l.getY(),l.getZ(),l.getYaw(),l.getPitch());validate(next);store.save(next);lobby=next;lobbyRevision++;travel.clear();tell(p,"Ana lobi ayarlandı. Giriş yapanlar artık burada başlayacak.");
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
    private void groupCommand(Player p,String[] a){
        if(a.length==0){showGroup(p);return;}String action=verb(a[0]);UUID id=p.getUniqueId();
        if(action.equals("yardim")){tell(p,"/grup oluştur | davet <oyuncu> | katıl <lider> | çıkar <oyuncu> | lider <oyuncu> | ayrıl | dağıt onayla | sohbet <mesaj>");return;}
        if(action.equals("sohbet")){need(a,2,"/grup sohbet <mesaj>");PartyBook.Party group=parties.party(id);if(group==null)throw new IllegalArgumentException("Bir grupta değilsin.");String message=String.join(" ",Arrays.copyOfRange(a,1,a.length));if(message.length()>256)throw new IllegalArgumentException("Mesaj en fazla 256 karakter olabilir.");broadcast(group,"[Grup] "+p.getName()+" » "+message);return;}
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
        },ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(2)).build());actions.add(ActionButton.create(text(button.label(),NamedTextColor.AQUA),null,150,click));}
        List<DialogBody> body=lines.stream().<DialogBody>map(line->DialogBody.plainMessage(text(line,NamedTextColor.GRAY),320)).toList();
        p.showDialog(Dialog.create(builder->builder.empty().base(DialogBase.builder(text(title,NamedTextColor.GOLD)).canCloseWithEscape(true).pause(false).afterAction(DialogBase.DialogAfterAction.NONE).body(body).inputs(inputs).build()).type(DialogType.multiAction(actions).columns(2).build())));
    }
    private Button button(String label,Runnable action){return new Button(label,response->action.run());}
    private Button close(){return button("Kapat",()->{});}
    private void showMain(Player p){PartyBook.Party group=parties.party(p.getUniqueId());show(p,"Kasaba • Oyun Merkezi",List.of("Arkadaşlarınla buluş, grubunu kur ve oyunlara birlikte katıl.","Çevrimiçi: "+Bukkit.getOnlinePlayers().size()+" • Grubun: "+(group==null?"Yok":group.members().size()+" kişi"),games.isEmpty()?"İlk oyun yakında. Şimdilik lobide arkadaşlarınla buluşabilirsin.":"Oyunlar sayfasından hazır bir oyun seç."),List.of(),List.of(button("Oyunlar",()->showGames(p,0)),button("Arkadaş grubum",()->showGroup(p)),button("Lobiye dön",()->returnLobby(p,false)),close()));}
    private void showGroup(Player p){
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
        List<Entry> entries=new ArrayList<>(games.values());int last=Math.max(0,(entries.size()-1)/6),page=Math.clamp(requested,0,last);List<String> lines=new ArrayList<>();List<Button> buttons=new ArrayList<>();
        if(entries.isEmpty())lines.add("Oyunlar yakında! Henüz oynanabilir bir oyun eklenmedi.");
        else{lines.add("Grup lideri seçim yaparsa bütün grup birlikte katılır. • "+(page+1)+"/"+(last+1));for(int i=page*6;i<Math.min(entries.size(),page*6+6);i++){Entry entry=entries.get(i);GameInfo info=entry.info();lines.add(info.name()+" • "+info.minimum()+"–"+info.maximum()+" kişi • "+info.description());buttons.add(button(info.name()+(available(entry)?" • Katıl":" • Bakımda"),()->join(p,entry)));}}
        if(page>0)buttons.add(button("Önceki",()->showGames(p,page-1)));if(page<last)buttons.add(button("Sonraki",()->showGames(p,page+1)));buttons.add(button("Merkeze dön",()->showMain(p)));buttons.add(close());show(p,"Kasaba • Oyunlar",lines,List.of(),buttons);
    }
    private void join(Player p,Entry entry){
        ready(p);if(games.get(entry.info().id())!=entry||!available(entry))throw new IllegalArgumentException("Bu oyun şu anda hazır değil.");
        PartyBook.Party group=parties.party(p.getUniqueId());if(group!=null)parties.leader(p.getUniqueId());List<UUID> ids=group==null?List.of(p.getUniqueId()):group.members();
        if(ids.size()<entry.info().minimum()||ids.size()>entry.info().maximum())throw new IllegalArgumentException("Bu oyun "+entry.info().minimum()+"–"+entry.info().maximum()+" kişilik grup kabul ediyor.");
        List<Player> team=new ArrayList<>();for(UUID id:ids){Player member=Bukkit.getPlayer(id);if(member==null)throw new IllegalArgumentException("Gruptaki bir oyuncu ayrıldı.");ready(member);if(playing(id))throw new IllegalArgumentException("Gruptaki bir oyuncu zaten oyunda.");team.add(member);}
        ids.forEach(id->playing.put(id,entry));boolean success=false;
        try{success=entry.game().join(List.copyOf(team));}catch(RuntimeException e){getLogger().log(Level.SEVERE,"Oyun katılımı başarısız: "+entry.info().id(),e);}
        if(!success||games.get(entry.info().id())!=entry||ids.stream().anyMatch(id->playing.get(id)!=entry)){
            for(UUID id:ids)if(playing.get(id)==entry)detach(id);throw new IllegalArgumentException("Oyun grubu kabul edemedi; tekrar dene.");
        }
        for(Player member:team){pages.remove(member.getUniqueId());travel.remove(member.getUniqueId());member.closeDialog();tell(member,entry.info().name()+" oyununa grubunla katıldın.");}
    }
    private void detach(UUID id){Entry entry=playing.remove(id);if(entry!=null&&entry.owner().isEnabled())try{entry.game().leave(id);}catch(RuntimeException e){getLogger().log(Level.WARNING,"Oyun çıkışı başarısız: "+entry.info().id(),e);}}
    private void mainThread(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Hub API ana iş parçacığından kullanılmalı.");}
    @Override public void register(Plugin owner,HubGame game){mainThread();Objects.requireNonNull(owner);Objects.requireNonNull(game);if(!owner.isEnabled())throw new IllegalArgumentException("Oyun eklentisi etkin değil.");GameInfo info=Objects.requireNonNull(game.info());if(games.containsKey(info.id()))throw new IllegalArgumentException("Bu oyun kimliği zaten kayıtlı.");games.put(info.id(),new Entry(owner,game,info));}
    @Override public void unregister(Plugin owner,String id){mainThread();Entry entry=games.get(id);if(entry==null)return;if(entry.owner()!=owner)throw new IllegalArgumentException("Başka eklentinin oyunu kaldırılamaz.");games.remove(id);for(UUID player:new ArrayList<>(playing.keySet()))if(playing.get(player)==entry)returnToLobby(player);}
    @Override public boolean playing(UUID player){mainThread();return playing.containsKey(player);}
    @Override public void returnToLobby(UUID id){mainThread();Player p=Bukkit.getPlayer(id);if(p!=null&&authenticated(p))returnLobby(p,false);else detach(id);}
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
                if(!p.teleport(target,PlayerTeleportEvent.TeleportCause.PLUGIN)){tell(p,"Lobiye dönüş başka bir eklenti tarafından engellendi.");return;}p.setFallDistance(0);tell(p,"Kasaba lobisine hoş geldin!");
                if(autoPage)Bukkit.getScheduler().runTaskLater(this,()->{if(p.isOnline()&&authenticated(p)&&!playing(id)&&inLobby(p.getLocation())&&!pages.containsKey(id)&&Objects.equals(navigation.get(id),token))try{showMain(p);}catch(IllegalArgumentException ignored){}},40);
            });
        });
        Bukkit.getScheduler().runTaskLater(this,()->{if(Objects.equals(travel.get(id),token)){travel.remove(id);if(p.isOnline())tell(p,"Lobi zamanında hazırlanamadı; /lobi ile tekrar dene.");}},200);
    }
    @EventHandler public void authenticated(PlayerAuthenticatedEvent e){Bukkit.getScheduler().runTask(this,()->{Player p=e.getPlayer();if(!p.isOnline())return;try{if(lobby!=null)returnLobby(p,settings!=null&&settings.autoPage());else{if(admin(p))tell(p,"Oyun merkezi hazır. Lobi yerinde /merkez lobi ayarla kullan.");if(settings!=null&&settings.autoPage())showMain(p);}}catch(IllegalArgumentException ex){tell(p,ex.getMessage());}});}
    @EventHandler public void quit(PlayerQuitEvent e){UUID id=e.getPlayer().getUniqueId();pages.remove(id);navigation.remove(id);travel.remove(id);editors.remove(id);detach(id);PartyBook.Party old=parties.party(id),next=parties.leave(id);if(old!=null&&next!=null)broadcast(next,e.getPlayer().getName()+" ayrıldı. Grup lideri: "+name(next.leader()));}
    @EventHandler public void pluginDisabled(PluginDisableEvent e){for(Entry entry:new ArrayList<>(games.values()))if(entry.owner()==e.getPlugin())unregister(entry.owner(),entry.info().id());}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void voidFall(PlayerMoveEvent e){Player p=e.getPlayer();if(authenticated(p)&&!playing(p.getUniqueId())&&!travel.containsKey(p.getUniqueId())&&inLobby(p.getLocation())&&p.getY()<p.getWorld().getMinHeight()-2)try{returnLobby(p,false);}catch(IllegalArgumentException ignored){}}
    @EventHandler(priority=EventPriority.HIGHEST) public void respawn(PlayerRespawnEvent e){if(!playing(e.getPlayer().getUniqueId())&&authenticated(e.getPlayer())&&lobby!=null)try{e.setRespawnLocation(validate(lobby));}catch(IllegalArgumentException ignored){}}
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){
        if(args.length==0||!(sender instanceof Player p)||!authenticated(p))return List.of();List<String> options=new ArrayList<>();
        if(command.getName().equals("merkez")&&admin(sender)){if(args.length==1)options.addAll(List.of("lobi","düzenleme","yenile","yardım"));else if(args.length==2&&verb(args[0]).equals("lobi"))options.addAll(List.of("ayarla","bilgi"));else if(args.length==2&&verb(args[0]).equals("duzenleme"))options.addAll(List.of("aç","kapat"));}
        if(command.getName().equals("grup")){if(args.length==1)options.addAll(List.of("oluştur","davet","katıl","çıkar","lider","ayrıl","dağıt","sohbet","yardım"));else if(args.length==2&&Set.of("davet","katil","cikar","lider").contains(verb(args[0])))options.addAll(Bukkit.getOnlinePlayers().stream().filter(this::authenticated).map(Player::getName).toList());else if(args.length==2&&verb(args[0]).equals("dagit"))options.add("onayla");}
        String prefix=verb(args[args.length-1]);return options.stream().filter(s->verb(s).startsWith(prefix)).toList();
    }
}

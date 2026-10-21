package tr.kasaba.clan;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import tr.kasaba.roles.KasabaRoles;
import tr.kasaba.roles.api.ClanLabels;
import tr.kasaba.warp.api.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class KasabaKlan extends JavaPlugin implements Listener,TabCompleter {
    private record Invite(UUID clan,String issuer,long expires) {}
    private record Corner(int x,int z) {}
    private static final class Selection {UUID world;Corner first,second;boolean active;long expires;}
    private ClanState state=ClanState.empty();
    private ClaimIndex index=new ClaimIndex(state);
    private ClanStore store;
    private Settings settings;
    private boolean locked;
    private volatile Map<String,Component> labels=Map.of();
    private final Map<String,Map<UUID,Invite>> invitations=new HashMap<>();
    private final Map<UUID,Selection> selections=new HashMap<>();
    private final Set<UUID> bypass=new HashSet<>();
    private final Map<UUID,Long> warnings=new HashMap<>();

    @Override public void onEnable(){
        saveDefaultConfig();store=new ClanStore(getDataFolder().toPath().resolve("klanlar.yml"));
        try{settings=Settings.read(getDataFolder().toPath().resolve("config.yml"));publish(store.load());}
        catch(IOException|IllegalArgumentException e){locked=true;getLogger().log(Level.SEVERE,"Klan dosyası okunamadı. Veri korundu; dosya düzeltilip /klan yenile yapılana kadar normal oyuncuların inşa/etkileşimleri güvenlik için engelleniyor.",e);}
        Bukkit.getServicesManager().register(ClanLabels.class,name->labels.getOrDefault(Names.key(name),Component.empty()),this,ServicePriority.Normal);
        Bukkit.getServicesManager().register(WarpPlacementGuard.class,next->locked?Optional.of("Klan verisi okunamadığından warp konumu değiştirilemez; önce /klan yenile."):ClaimPolicy.warpRejection(index,next),this,ServicePriority.Normal);
        Bukkit.getPluginManager().registerEvents(this,this);Bukkit.getPluginManager().registerEvents(new Protection(this),this);
        Objects.requireNonNull(getCommand("klan")).setTabCompleter(this);
        Bukkit.getScheduler().runTaskTimer(this,this::prune,100,100);
        Bukkit.getScheduler().runTask(this,this::refreshRoles);
        if(!locked)ClaimPolicy.warpRejection(index,warps()).ifPresent(reason->getLogger().severe("Mevcut dosyalarda warp/arazi çakışması var; araziler silinmedi. "+reason));
    }
    @Override public void onDisable(){
        labels=Map.of();Bukkit.getServicesManager().unregisterAll(this);selections.clear();invitations.clear();bypass.clear();warnings.clear();
        refreshRoles();
    }
    private void refreshRoles(){if(Bukkit.getPluginManager().getPlugin("KasabaRoles") instanceof KasabaRoles roles&&roles.isEnabled())roles.refreshClanLabels();}
    private void publish(ClanState next){
        ClaimIndex nextIndex=new ClaimIndex(next);Map<String,Component> names=new HashMap<>();
        for(Clan clan:next.clans().values())for(String member:clan.members().keySet())names.put(member,Component.text("["+clan.tag()+"] ",settings.color()));
        state=next;index=nextIndex;labels=Map.copyOf(names);refreshRoles();
    }
    private void commit(ClanState next) throws IOException {if(locked)throw new IllegalArgumentException("Klan dosyası kilitli; yönetici /klan yenile kullanmalı.");store.save(next);publish(next);}
    private void put(Clan clan) throws IOException {commit(state.put(clan));}
    private List<WarpDestination> warps(){
        var registration=Bukkit.getServicesManager().getRegistration(WarpRegions.class);
        if(registration==null)throw new IllegalArgumentException("KasabaWarp 1.2.0 veya üstü çalışıyor olmalı; claim alınamadı.");
        return registration.getProvider().destinations();
    }
    boolean admin(CommandSender sender){return sender.hasPermission("kasabaklan.yonet")||sender.hasPermission("kasabaroles.admin");}
    private Player player(CommandSender sender){if(sender instanceof Player p)return p;throw new IllegalArgumentException("Bu komutu oyunda kullan.");}
    private Clan clan(Player player){Clan clan=state.member(player.getName());if(clan==null)throw new IllegalArgumentException("Bir klana üye değilsin. /klan oluştur <etiket> <ad>");return clan;}
    private Clan manager(Player player){Clan clan=clan(player);if(!clan.manages(player.getName()))throw new IllegalArgumentException("Bu işlem klan lideri veya yardımcısına ait.");return clan;}
    private Clan leader(Player player){Clan clan=clan(player);if(!clan.leader().equals(Names.key(player.getName())))throw new IllegalArgumentException("Bu işlem sadece klan liderine ait.");return clan;}
    private void need(String[] args,int length,String usage){if(args.length<length)throw new IllegalArgumentException("Kullanım: "+usage);}
    void tell(CommandSender sender,String message){sender.sendMessage(Component.text(message,NamedTextColor.YELLOW));}
    private void notifyClan(Clan clan,String message){for(Player p:Bukkit.getOnlinePlayers())if(clan.contains(p.getName()))tell(p,message);}
    private void requireAdmin(CommandSender sender){if(!admin(sender))throw new IllegalArgumentException("Bu komut için yönetim yetkin yok.");}
    private void info(CommandSender sender,Clan clan){
        tell(sender,"["+clan.tag()+"] "+clan.name()+" • Lider: "+clan.members().get(clan.leader()));
        tell(sender,"Üye: "+clan.members().size()+"/"+settings.members()+" • Arazi: "+clan.claims().size()+" • Alan: "+clan.area()+"/"+settings.area()+" blok");
    }
    private void prune(){
        long now=System.nanoTime();selections.values().removeIf(s->now-s.expires>=0);
        for(var iterator=invitations.entrySet().iterator();iterator.hasNext();){var entry=iterator.next();entry.getValue().values().removeIf(i->now-i.expires()>=0||!state.clans().containsKey(i.clan()));if(entry.getValue().isEmpty())iterator.remove();}
    }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        try{
            String action=args.length==0?"bilgi":Names.command(args[0]);
            if(action.equals("yenile")){
                requireAdmin(sender);Settings nextSettings=Settings.read(getDataFolder().toPath().resolve("config.yml"));ClanState next=store.load();
                settings=nextSettings;publish(next);locked=false;selections.clear();invitations.clear();
                tell(sender,"Klanlar, araziler ve ayarlar yenilendi. "+ClaimPolicy.warpRejection(index,warps()).orElse("Warp çevreleriyle çakışma yok."));return true;
            }
            if(action.equals("yonetim")){
                requireAdmin(sender);need(args,2,"/klan yönetim aç|kapat");Player p=player(sender);String mode=Names.command(args[1]);
                if(!Set.of("ac","kapat").contains(mode))throw new IllegalArgumentException("/klan yönetim aç veya /klan yönetim kapat");
                if(mode.equals("ac"))bypass.add(p.getUniqueId());else bypass.remove(p.getUniqueId());
                getLogger().info(p.getName()+" koruma geçişi: "+mode);tell(p,"Yönetim koruma geçişi "+(mode.equals("ac")?"açıldı. Başkalarının arazisini değiştirebilirsin.":"kapatıldı."));return true;
            }
            if(locked||settings==null)throw new IllegalArgumentException("Klan verileri okunamadı; koruma kilidi açık. Yönetici dosyayı düzeltip /klan yenile kullanmalı.");
            if(!Set.of("orman","yonet","yardim").contains(action)&&!sender.hasPermission("kasabaklan.kullan"))throw new IllegalArgumentException("Klan kullanma yetkin yok.");
            switch(action){
                case "yardim" -> {
                    tell(sender,"/klan oluştur <etiket> <ad> | davet <oyuncu> | katıl <etiket> | üyeler | bilgi | liste");
                    tell(sender,"/klan çıkar <oyuncu> | ayrıl | yetki <oyuncu> yardımcı|üye | lider <oyuncu> | dağıt onayla | sohbet <mesaj>");
                    tell(sender,"/klan alan seç | birinci | ikinci | al | iptal | liste | bilgi | bırak <numara>");
                    if(admin(sender))tell(sender,"Yönetim: /klan orman ayarla|bilgi • /klan yönetim aç|kapat • /klan yönet alansil <etiket> <numara> • /klan yenile");
                }
                case "bilgi" -> {Clan clan=args.length>1?state.tag(args[1]):clan(player(sender));info(sender,clan);}
                case "liste" -> tell(sender,"Klanlar: "+(state.clans().isEmpty()?"Henüz yok.":String.join(", ",state.clans().values().stream().sorted(Comparator.comparing(Clan::tag)).map(c->c.tag()+" ("+c.members().size()+")").toList())));
                case "olustur" -> {
                    need(args,2,"/klan oluştur <etiket> [klan adı]");Player p=player(sender);if(state.member(p.getName())!=null)throw new IllegalArgumentException("Zaten bir klana üyesin.");
                    String name=args.length>2?String.join(" ",Arrays.copyOfRange(args,2,args.length)):args[1]+" Klanı";
                    Clan clan=Clan.create(args[1],name,p.getName());put(clan);tell(p,"Klan oluşturuldu: ["+clan.tag()+"] "+clan.name());
                }
                case "davet" -> {
                    need(args,2,"/klan davet <oyuncu>");Player p=player(sender);Clan clan=manager(p);Player target=Bukkit.getPlayerExact(args[1]);
                    if(target==null)throw new IllegalArgumentException("Davet için oyuncu çevrimiçi olmalı.");if(state.member(target.getName())!=null)throw new IllegalArgumentException("Oyuncu zaten bir klana üye.");
                    if(clan.members().size()>=settings.members())throw new IllegalArgumentException("Klanın üye sınırı dolu.");
                    Map<UUID,Invite> invites=invitations.computeIfAbsent(Names.key(target.getName()),key->new HashMap<>());
                    if(invites.size()>=20&&!invites.containsKey(clan.id()))throw new IllegalArgumentException("Oyuncunun bekleyen davetleri dolu; biraz sonra dene.");
                    invites.put(clan.id(),new Invite(clan.id(),Names.key(p.getName()),System.nanoTime()+TimeUnit.SECONDS.toNanos(settings.invitation())));
                    tell(target,"["+clan.tag()+"] "+clan.name()+" seni davet etti. "+settings.invitation()+" saniye içinde /klan katıl "+clan.tag()+" yaz.");tell(p,"Klan daveti gönderildi: "+target.getName());
                }
                case "katil" -> {
                    need(args,2,"/klan katıl <etiket>");Player p=player(sender);if(state.member(p.getName())!=null)throw new IllegalArgumentException("Zaten bir klana üyesin.");Clan clan=state.tag(args[1]);
                    Invite invite=invitations.getOrDefault(Names.key(p.getName()),Map.of()).get(clan.id());
                    if(invite==null||System.nanoTime()-invite.expires()>=0||!clan.manages(invite.issuer()))throw new IllegalArgumentException("Bu klan için geçerli davetin yok.");
                    Clan next=clan.joined(p.getName(),settings.members());put(next);invitations.remove(Names.key(p.getName()));notifyClan(next,p.getName()+" klana katıldı.");
                }
                case "uyeler" -> {
                    Clan clan=args.length>1?state.tag(args[1]):clan(player(sender));
                    tell(sender,"["+clan.tag()+"] Üyeler: "+String.join(", ",clan.members().entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e->e.getValue()+(e.getKey().equals(clan.leader())?" (Lider)":clan.assistants().contains(e.getKey())?" (Yardımcı)":"")).toList()));
                }
                case "cikar" -> {
                    need(args,2,"/klan çıkar <oyuncu>");Player p=player(sender);Clan clan=manager(p);String key=Names.key(args[1]);
                    if(!clan.members().containsKey(key))throw new IllegalArgumentException("Oyuncu kendi klanında değil.");
                    if(!clan.leader().equals(Names.key(p.getName()))&&clan.assistants().contains(key))throw new IllegalArgumentException("Yardımcıyı yalnızca lider çıkarabilir.");
                    put(clan.removed(key));notifyClan(clan,args[1]+" klandan çıkarıldı.");
                }
                case "ayril" -> {Player p=player(sender);Clan clan=clan(p);put(clan.removed(p.getName()));notifyClan(clan,p.getName()+" klandan ayrıldı.");}
                case "yetki" -> {
                    need(args,3,"/klan yetki <oyuncu> yardımcı|üye");Clan clan=leader(player(sender));String rank=Names.command(args[2]);
                    if(!Set.of("yardimci","uye").contains(rank))throw new IllegalArgumentException("Yetki yardımcı veya üye olmalı.");
                    put(clan.promoted(args[1],rank.equals("yardimci")));notifyClan(clan,args[1]+" klan yetkisi güncellendi: "+args[2]);
                }
                case "lider" -> {need(args,2,"/klan lider <oyuncu>");Clan clan=leader(player(sender));put(clan.transferred(args[1]));notifyClan(clan,"Klan liderliği devredildi: "+args[1]);}
                case "dagit" -> {
                    Clan clan=leader(player(sender));if(args.length<2||!Names.command(args[1]).equals("onayla"))throw new IllegalArgumentException("Bütün araziler bırakılacak. Onaylamak için /klan dağıt onayla yaz.");
                    commit(state.remove(clan));notifyClan(clan,"Klan dağıtıldı ve arazileri bırakıldı: "+clan.tag());
                }
                case "sohbet" -> {
                    need(args,2,"/klan sohbet <mesaj>");Player p=player(sender);Clan clan=clan(p);String message=String.join(" ",Arrays.copyOfRange(args,1,args.length));
                    if(message.length()>256)throw new IllegalArgumentException("Klan mesajı en fazla 256 karakter olabilir.");
                    Component line=Component.text("[Klan] ["+clan.tag()+"] "+p.getName()+" » ",settings.color()).append(Component.text(message,NamedTextColor.WHITE));
                    for(Player target:Bukkit.getOnlinePlayers())if(clan.contains(target.getName()))target.sendMessage(line);
                }
                case "alan" -> area(player(sender),args);
                case "orman" -> {
                    requireAdmin(sender);need(args,2,"/klan orman ayarla|bilgi");String sub=Names.command(args[1]);
                    if(sub.equals("ayarla")){Player p=player(sender);commit(state.forest(p.getWorld().getUID(),p.getWorld().getName()));tell(p,"Claim için orman dünyası ayarlandı: "+p.getWorld().getName());}
                    else if(sub.equals("bilgi"))tell(sender,state.forest()==null?"Orman dünyası ayarlı değil. Ormanda /klan orman ayarla kullan.":"Orman dünyası: "+state.forestName()+" • "+state.forest());
                    else throw new IllegalArgumentException("/klan orman ayarla veya /klan orman bilgi");
                }
                case "yonet" -> {
                    requireAdmin(sender);need(args,3,"/klan yönet alansil <etiket> <numara> | sil <etiket> onayla");Clan clan=state.tag(args[2]);String sub=Names.command(args[1]);
                    if(sub.equals("alansil")){need(args,4,"/klan yönet alansil <etiket> <numara>");put(clan.unclaimed(Integer.parseInt(args[3])));tell(sender,"Yönetici araziyi bıraktı: "+clan.tag()+" #"+args[3]);}
                    else if(sub.equals("sil")&&args.length>3&&Names.command(args[3]).equals("onayla")){commit(state.remove(clan));notifyClan(clan,"Klan yönetici tarafından silindi.");tell(sender,"Klan silindi: "+clan.tag());}
                    else throw new IllegalArgumentException("/klan yönet alansil <etiket> <numara> veya /klan yönet sil <etiket> onayla");
                }
                default -> tell(sender,"Komut bulunamadı. /klan yardım");
            }
        }catch(NumberFormatException e){tell(sender,"Arazi numarası tam sayı olmalı. /klan alan liste");}
        catch(IllegalArgumentException e){tell(sender,e.getMessage());}
        catch(IOException e){getLogger().log(Level.SEVERE,"Klan dosyası işlemi başarısız; mevcut veriler korundu.",e);tell(sender,"Dosya okunamadı veya kaydedilemedi; değişiklik uygulanmadı. Konsolu kontrol et.");}
        return true;
    }
    private void area(Player p,String[] args) throws IOException {
        need(args,2,"/klan alan seç|birinci|ikinci|al|iptal|liste|bilgi|bırak <numara>");String action=Names.command(args[1]);
        if(action.equals("iptal")){selections.remove(p.getUniqueId());tell(p,"Arazi seçimi kapatıldı.");return;}
        if(action.equals("bilgi")){
            var owned=at(p.getLocation());tell(p,owned==null?"Bulunduğun yer sahipsiz.":"Arazi: ["+owned.clan().tag()+"] #"+owned.claim().number()+" • "+owned.claim().bounds().area()+" blok");return;
        }
        Clan clan=clan(p);
        if(action.equals("liste")){tell(p,"["+clan.tag()+"] Araziler: "+clan.claims().size());for(Claim claim:clan.claims()){Rect r=claim.bounds();tell(p,"#"+claim.number()+": "+r.minX()+", "+r.minZ()+" → "+r.maxX()+", "+r.maxZ()+" • "+r.area()+" blok");}return;}
        clan=manager(p);
        switch(action){
            case "sec" -> {Selection selection=selections.computeIfAbsent(p.getUniqueId(),id->new Selection());selection.active=true;selection.expires=System.nanoTime()+TimeUnit.MINUTES.toNanos(10);tell(p,"Seçim açıldı: bir bloğa SOL tıkla, diğer köşeye SAĞ tıkla. Sonra /klan alan al. Çıkış: /klan alan iptal");}
            case "birinci","ikinci" -> corner(p,p.getLocation().getBlock(),action.equals("birinci"));
            case "al" -> {
                Selection selection=selections.get(p.getUniqueId());if(selection==null||selection.first==null||selection.second==null||System.nanoTime()-selection.expires>=0)throw new IllegalArgumentException("Önce iki köşe seç: /klan alan seç veya /klan alan birinci ve /klan alan ikinci");
                Rect r=Rect.between(selection.first.x(),selection.first.z(),selection.second.x(),selection.second.z());List<WarpDestination> destinations=warps();
                ClaimPolicy.warpRejection(index,destinations).ifPresent(reason->{throw new IllegalArgumentException("Önce mevcut warp/arazi çakışmasını yönetici çözmeli. "+reason);});
                ClaimPolicy.check(state,clan,selection.world,r,index,destinations,settings.area(),settings.claims(),settings.side());
                World world=Bukkit.getWorld(selection.world);if(world==null)throw new IllegalArgumentException("Orman dünyası yüklü değil.");
                for(int x:new int[]{r.minX(),r.maxX()})for(int z:new int[]{r.minZ(),r.maxZ()})if(!world.getWorldBorder().isInside(new Location(world,x+.5,world.getMinHeight(),z+.5)))throw new IllegalArgumentException("Seçimin bir kısmı dünya sınırı dışında.");
                int number=clan.claims().stream().mapToInt(Claim::number).max().orElse(0)+1;put(clan.claimed(new Claim(number,selection.world,r)));selections.remove(p.getUniqueId());
                tell(p,"Arazi alındı: #"+number+" • "+r.width()+"×"+r.depth()+" = "+r.area()+" blok. Yerden gökyüzüne kadar klanına ait.");
            }
            case "birak" -> {need(args,3,"/klan alan bırak <numara>");put(clan.unclaimed(Integer.parseInt(args[2])));tell(p,"Arazi bırakıldı: #"+args[2]);}
            default -> throw new IllegalArgumentException("/klan alan seç | birinci | ikinci | al | iptal | liste | bilgi | bırak <numara>");
        }
    }
    boolean selecting(Player p){Selection s=selections.get(p.getUniqueId());return s!=null&&s.active&&System.nanoTime()-s.expires<0;}
    void corner(Player p,Block block,boolean first){
        try{
            if(locked||!p.hasPermission("kasabaklan.kullan"))throw new IllegalArgumentException("Arazi seçimi kullanılamıyor.");manager(p);
            if(state.forest()==null||!state.forest().equals(block.getWorld().getUID()))throw new IllegalArgumentException("Köşeleri ayarlanmış orman dünyasında seçmelisin.");
            Selection s=selections.computeIfAbsent(p.getUniqueId(),id->new Selection());
            if(s.world!=null&&!s.world.equals(block.getWorld().getUID()))throw new IllegalArgumentException("İki köşe aynı dünyada olmalı; önce /klan alan iptal kullan.");
            s.world=block.getWorld().getUID();Corner point=new Corner(block.getX(),block.getZ());if(first)s.first=point;else s.second=point;s.expires=System.nanoTime()+TimeUnit.MINUTES.toNanos(10);
            tell(p,(first?"Birinci":"İkinci")+" köşe: "+point.x()+", "+point.z());
            if(s.first!=null&&s.second!=null){Rect r=Rect.between(s.first.x(),s.first.z(),s.second.x(),s.second.z());tell(p,"Seçilen: "+r.width()+"×"+r.depth()+" = "+r.area()+" blok. Almak için /klan alan al");}
        }catch(IllegalArgumentException e){tell(p,e.getMessage());}
    }
    ClaimIndex.Owned at(Location l){return l==null||l.getWorld()==null?null:index.at(l.getWorld().getUID(),l.getBlockX(),l.getBlockZ());}
    UUID owner(Location l){var owned=at(l);return owned==null?null:owned.clan().id();}
    boolean forbidden(Player p,Location l){
        boolean passing=bypass.contains(p.getUniqueId())&&admin(p);if(locked)return !passing;
        var owned=at(l);return owned!=null&&!passing&&!owned.clan().contains(p.getName());
    }
    boolean blockedAutomation(Location from,Location to){
        if(locked)return true;UUID source=owner(from),target=owner(to);return (source!=null||target!=null)&&!Objects.equals(source,target);
    }
    boolean locked(){return locked;}
    void warn(Player p){long now=System.nanoTime();Long last=warnings.get(p.getUniqueId());if(last==null||now-last>=TimeUnit.SECONDS.toNanos(2)){warnings.put(p.getUniqueId(),now);tell(p,locked?"Klan veri koruma kilidi açık; yöneticiye haber ver.":"Bu arazi başka bir klana ait.");}}
    @EventHandler public void quit(PlayerQuitEvent e){UUID id=e.getPlayer().getUniqueId();selections.remove(id);bypass.remove(id);warnings.remove(id);}
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){
        if(settings==null||args.length==0)return List.of();List<String> options=new ArrayList<>();
        if(args.length==1){options.addAll(List.of("oluştur","davet","katıl","üyeler","bilgi","liste","çıkar","ayrıl","yetki","lider","dağıt","sohbet","alan","yardım"));if(admin(sender))options.addAll(List.of("orman","yenile","yönetim","yönet"));}
        else{
            String action=Names.command(args[0]);
            if(args.length==2&&Set.of("davet","cikar","yetki","lider").contains(action))options.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            if(args.length==2&&Set.of("katil","bilgi","uyeler").contains(action))options.addAll(state.clans().values().stream().map(Clan::tag).toList());
            if(action.equals("alan")){
                if(args.length==2)options.addAll(List.of("seç","birinci","ikinci","al","iptal","liste","bilgi","bırak"));
                else if(args.length==3&&Names.command(args[1]).equals("birak")&&sender instanceof Player p){Clan clan=state.member(p.getName());if(clan!=null)options.addAll(clan.claims().stream().map(c->Integer.toString(c.number())).toList());}
            }
            if(args.length==3&&action.equals("yetki"))options.addAll(List.of("yardımcı","üye"));
            if(args.length==2&&action.equals("dagit"))options.add("onayla");
            if(admin(sender)){
                if(args.length==2&&action.equals("orman"))options.addAll(List.of("ayarla","bilgi"));
                if(args.length==2&&action.equals("yonetim"))options.addAll(List.of("aç","kapat"));
                if(args.length==2&&action.equals("yonet"))options.addAll(List.of("alansil","sil"));
                if(args.length==3&&action.equals("yonet"))options.addAll(state.clans().values().stream().map(Clan::tag).toList());
            }
        }
        String prefix=Names.command(args[args.length-1]);return options.stream().filter(o->Names.command(o).startsWith(prefix)).distinct().toList();
    }
}

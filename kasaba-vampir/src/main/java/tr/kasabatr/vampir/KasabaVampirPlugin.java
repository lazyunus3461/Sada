package tr.kasabatr.vampir;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.registry.data.dialog.*;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import tr.kasaba.auth.api.AuthState;
import tr.kasaba.auth.api.PlayerAuthenticatedEvent;
import tr.kasaba.hub.api.*;

/** Hub-managed admission; room membership and physical lobby presence are separate states. */
public final class KasabaVampirPlugin extends JavaPlugin implements Listener,HubGame,TabExecutor {
    private static final String GAME_ID="vampirkoylu";
    final Map<Integer,GameRoom> rooms=new LinkedHashMap<>();
    final Map<UUID,GameRoom> membership=new ConcurrentHashMap<>();
    private final Set<UUID> lobby=new HashSet<>(),internalTeleports=new HashSet<>(),editors=new HashSet<>();
    private final Map<UUID,Long> pages=new HashMap<>();
    private WorldInstances instances;private HubApi hub;private NamespacedKey itemKey,shotKey;
    private int nextRoom=101;private long pageSequence;private boolean disabling;

    @Override public void onEnable(){
        saveDefaultConfig();itemKey=new NamespacedKey(this,"menu_item");shotKey=new NamespacedKey(this,"shot-room");instances=new WorldInstances(this);
        try{
            if(WorldNames.migrate(getConfig(),Bukkit.getWorldContainer().toPath())){
                var config=getDataFolder().toPath().resolve("config.yml");
                var backup=getDataFolder().toPath().resolve("config-before-world-rename.yml");
                try{if(java.nio.file.Files.isRegularFile(config)&&!java.nio.file.Files.exists(backup))java.nio.file.Files.copy(config,backup);getConfig().save(config.toFile());}
                catch(IOException e){throw new IllegalStateException("Dünya adı ayarı kaydedilemedi; eski dosya korundu.",e);}
                getLogger().info("Harita/lobi dünya adı otomatik ayarlandı: "+getConfig().getString("template-world"));
            }
            instances.clearStaleCopies();String name=getConfig().getString("template-world","");
            if(instances.templateReady()&&Bukkit.getWorld(name)==null)Bukkit.createWorld(new WorldCreator(name));
            World template=Bukkit.getWorld(name);if(template!=null){template.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);template.setGameRule(GameRule.DO_WEATHER_CYCLE,false);template.setGameRule(GameRule.DO_MOB_SPAWNING,false);template.setTime(1000);template.setStorm(false);template.setThundering(false);}
            hub=Bukkit.getServicesManager().load(HubApi.class);if(hub==null)throw new IllegalStateException("KasabaHub / KasabaAuth API bulunamadı.");hub.register(this,this);
        }catch(RuntimeException e){getLogger().log(Level.SEVERE,"Vampir Köylü başlatılamadı; mevcut dosyalar korundu.",e);Bukkit.getPluginManager().disablePlugin(this);return;}
        Bukkit.getPluginManager().registerEvents(this,this);getCommand("vk").setExecutor(this);getCommand("vk").setTabCompleter(this);getCommand("v").setExecutor(this);
        Bukkit.getScheduler().runTaskTimer(this,()->{for(GameRoom room:List.copyOf(rooms.values()))try{room.tick();}catch(RuntimeException e){getLogger().log(Level.SEVERE,"Oda "+room.id+" güvenli biçimde durduruluyor.",e);room.finish(null,"Oda işlemi durduruldu.");}},20,20);
        if(!instances.templateReady())getLogger().warning("Harita şablonu yok; kurulum ZIP'indeki dünya klasörünü sunucu ana klasörüne koy.");
        getLogger().info("Kasaba Vampir Köylü 1.0.2: vampirkoylu dünya adı, Hub 1.2 ve 2 kişilik yönetici denemesi.");
    }
    @Override public void onDisable(){
        disabling=true;for(UUID id:List.copyOf(lobby)){Player p=Bukkit.getPlayer(id);if(p!=null)detach(p,true);}
        for(GameRoom room:List.copyOf(rooms.values()))room.finish(null,"Eklenti kapanıyor.");
        if(hub!=null)try{hub.unregister(this,GAME_ID);}catch(RuntimeException e){getLogger().warning(e.getMessage());}
    }
    WorldInstances instances(){return instances;}
    @Override public GameInfo info(){return new GameInfo(GAME_ID,"Vampir Köylü","Gizli roller, görevler, gündüz/gece ve oylama",1,20);}
    @Override public boolean available(){return lobbySpawn()!=null&&instances.templateReady();}
    boolean authenticated(Player p){AuthState auth=Bukkit.getServicesManager().load(AuthState.class);return p.isOnline()&&auth!=null&&auth.authenticated(p.getUniqueId());}
    private void ready(Player p){if(!authenticated(p))throw new IllegalArgumentException("Önce kayıt/giriş işlemini tamamla.");if(!p.hasPermission("kasabatr.vampir.kullan"))throw new IllegalArgumentException("Vampir Köylü yetkin yok.");}
    private void admin(Player p){if(!p.hasPermission("kasabatr.vampir.admin"))throw new IllegalArgumentException("Bu komut yöneticiye özel.");}
    private static String norm(String s){return s.toLowerCase(Locale.ROOT).replace("\u0307","").replace('ı','i').replace('ş','s').replace('ç','c').replace('ğ','g').replace('ü','u').replace('ö','o');}
    private Location lobbySpawn(){World w=Bukkit.getWorld(getConfig().getString("lobby.world",""));return w==null?null:new Location(w,getConfig().getDouble("lobby.x",144.5),getConfig().getDouble("lobby.y",69),getConfig().getDouble("lobby.z",.5),(float)getConfig().getDouble("lobby.yaw",180),(float)getConfig().getDouble("lobby.pitch",0));}
    private boolean waiting(Player p){GameRoom r=membership.get(p.getUniqueId());return lobby.contains(p.getUniqueId())&&(r==null||r.phase==GameRoom.Phase.WAITING);}
    private boolean playing(Player p){GameRoom r=membership.get(p.getUniqueId());return r!=null&&r.world!=null&&r.phase!=GameRoom.Phase.WAITING&&r.phase!=GameRoom.Phase.FINISHED;}
    private boolean isInstanceWorld(World w){return w!=null&&w.getName().startsWith("vk_room_");}
    private boolean templateWorld(World w){return w!=null&&w.getName().equals(getConfig().getString("template-world"));}
    private boolean protectedLobby(Location l){Location spawn=lobbySpawn();if(spawn==null||l.getWorld()!=spawn.getWorld())return false;int r=Math.max(3,getConfig().getInt("lobby.radius",31));return l.getBlockX()>=spawn.getBlockX()-r-1&&l.getBlockX()<=spawn.getBlockX()+r&&l.getBlockZ()>=spawn.getBlockZ()-r-1&&l.getBlockZ()<=spawn.getBlockZ()+r;}
    private boolean protectedAt(Location l){return templateWorld(l.getWorld())||isInstanceWorld(l.getWorld())||protectedLobby(l);}
    private boolean editing(Player p,Location l){return editors.contains(p.getUniqueId())&&p.hasPermission("kasabatr.vampir.admin")&&!lobby.contains(p.getUniqueId())&&!isInstanceWorld(l.getWorld());}
    void tell(CommandSender p,String text){if(p!=null)p.sendMessage("§5[Kasaba Vampir] §f"+text);}
    void teleportInternal(Player p,Location l){UUID id=p.getUniqueId();internalTeleports.add(id);try{if(!p.teleport(l,PlayerTeleportEvent.TeleportCause.PLUGIN))throw new IllegalStateException("Işınlanma engellendi: "+p.getName());}finally{internalTeleports.remove(id);}}
    void closePage(Player p){pages.remove(p.getUniqueId());p.closeDialog();p.closeInventory();}
    private void clearPlayer(Player p){
        closePage(p);p.setItemOnCursor(null);p.getInventory().clear();p.getInventory().setArmorContents(new ItemStack[4]);p.getInventory().setItemInOffHand(null);
        p.getActivePotionEffects().stream().map(PotionEffect::getType).toList().forEach(p::removePotionEffect);p.setAbsorptionAmount(0);p.setInvulnerable(false);p.setGameMode(GameMode.ADVENTURE);p.setFlying(false);p.setAllowFlight(false);p.setFireTicks(0);p.setFallDistance(0);p.setFoodLevel(20);p.setSaturation(20);p.setExhaustion(0);p.setTotalExperience(0);p.setLevel(0);p.setExp(0);if(!p.isDead())p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue());
    }
    private ItemStack tagged(Material type,String label,String tag,String lore){ItemStack item=new ItemStack(type);var meta=item.getItemMeta();meta.setDisplayName(label);meta.setLore(List.of(lore));meta.getPersistentDataContainer().set(itemKey,PersistentDataType.STRING,tag);item.setItemMeta(meta);return item;}
    private String tag(ItemStack item){return item==null||!item.hasItemMeta()?"":item.getItemMeta().getPersistentDataContainer().getOrDefault(itemKey,PersistentDataType.STRING,"");}
    private void prepareLobbyPlayer(Player p){clearPlayer(p);p.getInventory().setItem(8,tagged(Material.COMPASS,"§5Vampir Köylü • Odalar","compass","§7Sağ tıkla veya /vk yaz."));p.getInventory().setHeldItemSlot(8);}
    void prepareMatchPlayer(Player p,GameRoom room,Location point){
        clearPlayer(p);teleportInternal(p,point);p.getInventory().setItem(8,tagged(Material.BELL,"§eCeset Bildir","report","§7Cesede 4 blok yakınken sağ tıkla."));p.getInventory().setItem(7,tagged(Material.BOOK,"§aGörev Defteri","tasks","§7Görev yerlerini gösterir."));
        if(room.roles.get(p.getUniqueId())==GameRoom.Role.VAMPIRE)p.getInventory().setItem(0,tagged(Material.IRON_SWORD,"§4Gizli Av","weapon","§7Gündüz yakın dövüşle avlan."));
        if(room.roles.get(p.getUniqueId())==GameRoom.Role.HUNTER){p.getInventory().setItem(0,tagged(Material.BOW,"§6Avcı Yayı","weapon","§7İsabet eden hedef elenir."));int arrows=Math.clamp(getConfig().getInt("game.hunter-arrows",1),0,64);if(arrows>0)p.getInventory().setItem(1,new ItemStack(Material.ARROW,arrows));}
    }
    @Override public boolean join(List<Player> team){
        Location destination=lobbySpawn();if(destination==null||!instances.templateReady()||team.isEmpty()||team.size()>20)return false;
        for(Player p:team)if(!authenticated(p)||p.isDead()||!p.hasPermission("kasabatr.vampir.kullan")||lobby.contains(p.getUniqueId())||PlayerBackup.exists(getDataFolder(),p.getUniqueId()))return false;
        List<Player> saved=new ArrayList<>();try{
            for(Player p:team){PlayerBackup.save(getDataFolder(),p);saved.add(p);lobby.add(p.getUniqueId());prepareLobbyPlayer(p);teleportInternal(p,destination);tell(p,"Vampir Köylü lobisine geldin. Pusulaya sağ tıkla veya /vk yaz.");}
            for(Player p:team)Bukkit.getScheduler().runTaskLater(this,()->{if(authenticated(p)&&lobby.contains(p.getUniqueId())&&!pages.containsKey(p.getUniqueId()))menu(p);},2);return true;
        }catch(Exception e){getLogger().log(Level.WARNING,"Grubun Vampir Köylü girişi geri alındı.",e);for(Player p:saved)detach(p,true);return false;}
    }
    @Override public void leave(UUID id){Player p=Bukkit.getPlayer(id);if(p!=null)detach(p,disabling);else{GameRoom r=membership.remove(id);lobby.remove(id);if(r!=null)r.leave(id);}}
    private void detach(Player p,boolean restoreLocation){
        UUID id=p.getUniqueId();GameRoom room=membership.remove(id);boolean active=lobby.remove(id);editors.remove(id);closePage(p);
        if(!active&&!PlayerBackup.exists(getDataFolder(),id)){if(room!=null)room.leave(id);return;}
        try{
            if(!restoreLocation&&room!=null&&room.world==p.getWorld()){Location target=lobbySpawn();if(target!=null)teleportInternal(p,target);else restoreLocation=true;}
            internalTeleports.add(id);try{PlayerBackup.restore(getDataFolder(),p,restoreLocation);}finally{internalTeleports.remove(id);}
        }catch(Exception e){getLogger().log(Level.SEVERE,"Oyuncu yedeği korundu: "+p.getName(),e);p.kick(Component.text("Envanter yedeğin korunuyor. Yöneticiye haber ver."));}
        finally{if(room!=null)room.leave(id);}
    }
    void roomFinished(GameRoom room){
        rooms.remove(room.id);Location spawn=lobbySpawn();for(UUID id:List.copyOf(room.members)){
            membership.remove(id,room);Player p=Bukkit.getPlayer(id);if(p==null||!p.isOnline())continue;
            if(disabling||spawn==null){detach(p,true);continue;}
            try{closePage(p);teleportInternal(p,spawn);prepareLobbyPlayer(p);}catch(RuntimeException e){getLogger().log(Level.WARNING,"Oyuncu oyun lobisine dönemedi.",e);detach(p,true);}
        }
        if(room.world!=null)instances.close(room.world);
    }
    private void enter(Player p){ready(p);if(lobby.contains(p.getUniqueId()))return;if(hub.playing(p.getUniqueId()))throw new IllegalArgumentException("Önce /lobi ile mevcut oyundan çık.");PluginCommand entrance=Bukkit.getPluginCommand("kasabahub:oyunlar");if(entrance==null)throw new IllegalArgumentException("KasabaHub oyunlar komutu yok.");entrance.execute(p,"oyunlar",new String[]{"git",GAME_ID});if(!lobby.contains(p.getUniqueId()))throw new IllegalArgumentException("Vampir Köylü lobisine girilemedi. Haritanın kurulu olduğunu kontrol et.");}
    private GameRoom own(Player p){GameRoom room=membership.get(p.getUniqueId());if(room==null)throw new IllegalArgumentException("Önce /vk oda kur veya /vk oda katıl <numara> kullan.");return room;}
    private void exit(Player p){if(!lobby.contains(p.getUniqueId())){tell(p,"Vampir Köylü'de değilsin.");return;}hub.returnToLobby(p.getUniqueId());}
    private void createRoom(Player p){if(membership.containsKey(p.getUniqueId()))throw new IllegalArgumentException("Önce mevcut odadan ayrıl.");GameRoom room=new GameRoom(this,nextRoom++,p.getUniqueId());rooms.put(room.id,room);membership.put(p.getUniqueId(),room);openRoomMenu(p,room);tell(p,"Oda "+room.id+" açıldı. Arkadaşın /vk oda katıl "+room.id+" yazabilir.");}
    private void joinRoom(Player p,int id){if(membership.containsKey(p.getUniqueId()))throw new IllegalArgumentException("Zaten bir odadasın.");GameRoom room=rooms.get(id);if(room==null||!room.add(p))throw new IllegalArgumentException("Bu oda kapandı veya dolu.");membership.put(p.getUniqueId(),room);openRoomMenu(p,room);}
    private void leaveRoom(Player p){GameRoom room=membership.remove(p.getUniqueId());if(room==null)return;Location spawn=lobbySpawn();if(room.world!=null&&spawn!=null)teleportInternal(p,spawn);room.leave(p.getUniqueId());if(lobby.contains(p.getUniqueId()))prepareLobbyPlayer(p);tell(p,"Odadan ayrıldın; Vampir Köylü lobisindesin.");}
    private void startRoom(Player p,boolean demo)throws IOException{GameRoom room=own(p);if(!room.owner.equals(p.getUniqueId()))throw new IllegalArgumentException("Maçı oda kurucusu başlatır.");if(demo){admin(p);if(room.members.size()!=2||room.phase!=GameRoom.Phase.WAITING)throw new IllegalArgumentException("Deneme için bekleyen odada tam 2 oyuncu olmalı.");room.demo=true;room.vampireCount=1;room.hunter=false;room.seer=false;room.doctor=false;room.announce("Yönetici denemesi: 1 vampir, 1 köylü. Normal maçın 6 kişilik sınırı değişmedi.");}if(!room.canStart())throw new IllegalArgumentException("Oyuncu sayısı/roller uygun değil. Normal maç en az "+Math.clamp(getConfig().getInt("game.minimum-players",6),4,20)+" kişi olmalı ve bir köylü yeri kalmalı.");room.start();}

    private record Choice(String label,Runnable action){}
    private void show(Player p,String title,List<String> lines,List<Choice> choices){
        ready(p);closePage(p);long token=++pageSequence;pages.put(p.getUniqueId(),token);List<ActionButton> buttons=new ArrayList<>();
        for(Choice choice:choices)buttons.add(ActionButton.create(Component.text(choice.label),null,150,DialogAction.customClick((view,audience)->{
            if(!(audience instanceof Player actor)||!actor.getUniqueId().equals(p.getUniqueId()))return;
            Bukkit.getScheduler().runTask(this,()->{if(!authenticated(p)||!lobby.contains(p.getUniqueId())||!Objects.equals(pages.get(p.getUniqueId()),token))return;closePage(p);try{choice.action.run();}catch(IllegalArgumentException e){tell(p,e.getMessage());}catch(RuntimeException e){getLogger().log(Level.WARNING,"Vampir Köylü sayfa işlemi başarısız.",e);tell(p,"İşlem tamamlanamadı; /vk ile sayfayı yeniden aç.");}});
        },ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(2)).build())));
        p.showDialog(Dialog.create(builder->builder.empty().base(DialogBase.builder(Component.text(title)).pause(false).canCloseWithEscape(true).body(lines.stream().<DialogBody>map(line->DialogBody.plainMessage(Component.text(line),360)).toList()).build()).type(DialogType.multiAction(buttons).columns(2).build())));
    }
    private Choice close(){return new Choice("Kapat",()->{});}
    private void menu(Player p){GameRoom room=membership.get(p.getUniqueId());if(room==null){openLobbyMenu(p);return;}switch(room.phase){case WAITING->openRoomMenu(p,room);case VOTE->openTargetMenu(p,room,"vote");case ROLE->openTargetMenu(p,room,"role");default->{room.taskBook(p);tell(p,"Oda "+room.id+" • "+phase(room)+" • Rolün: "+room.roles.get(p.getUniqueId()).label);}}}
    private String phase(GameRoom r){return switch(r.phase){case WAITING->"Bekliyor";case DAY->"Gündüz";case SPEECH->"Konuşma";case VOTE->"Oylama";case ROLE->"Gece rolleri";case FINISHED->"Bitti";};}
    private void openLobbyMenu(Player p){if(!lobby.contains(p.getUniqueId()))return;show(p,"Kasaba • Vampir Köylü",List.of("Oda kur, arkadaşlarınla katıl ve gizli vampiri bul.","Normal maç: 6–20 kişi • Yönetici denemesi: 2 kişi"),List.of(new Choice("Oda Kur",()->createRoom(p)),new Choice("Açık Odalar",()->openRooms(p,0)),new Choice("Odam",()->openRoomMenu(p,own(p))),new Choice("Ana lobiye dön",()->exit(p)),close()));}
    private void openRoomMenu(Player p,GameRoom room){
        if(room.phase!=GameRoom.Phase.WAITING||membership.get(p.getUniqueId())!=room)throw new IllegalArgumentException("Bekleyen odanda değilsin.");
        List<Choice> choices=new ArrayList<>();if(room.owner.equals(p.getUniqueId())){
            choices.add(new Choice("Vampir: "+room.vampireCount,()->setting(p,room,()->room.vampireCount=room.vampireCount==1?2:1)));
            choices.add(new Choice("Avcı: "+(room.hunter?"Açık":"Kapalı"),()->setting(p,room,()->room.hunter=!room.hunter)));
            choices.add(new Choice("Kahin: "+(room.seer?"Açık":"Kapalı"),()->setting(p,room,()->room.seer=!room.seer)));
            choices.add(new Choice("Doktor: "+(room.doctor?"Açık":"Kapalı"),()->setting(p,room,()->room.doctor=!room.doctor)));
            choices.add(new Choice("Maçı Başlat",()->{try{startRoom(p,false);}catch(IOException e){throw new IllegalArgumentException("Maç başlatılamadı: "+e.getMessage());}}));
            if(p.hasPermission("kasabatr.vampir.admin"))choices.add(new Choice("2 Kişilik Deneme",()->{try{startRoom(p,true);}catch(IOException e){throw new IllegalArgumentException(e.getMessage());}}));
        }
        choices.add(new Choice("Odadan Ayrıl",()->{leaveRoom(p);openLobbyMenu(p);}));choices.add(close());
        show(p,"Vampir Köylü • Oda "+room.id,List.of("Kurucu: "+room.ownerName()+" • Oyuncu: "+room.members.size()+"/"+Math.clamp(getConfig().getInt("game.maximum-players",20),6,20),"1/2 Vampir • Avcı "+room.hunter+" • Kahin "+room.seer+" • Doktor "+room.doctor,"Ayarları ve başlangıcı oda kurucusu yönetir."),choices);
    }
    private void setting(Player p,GameRoom room,Runnable change){if(room!=membership.get(p.getUniqueId())||room.phase!=GameRoom.Phase.WAITING||!room.owner.equals(p.getUniqueId()))throw new IllegalArgumentException("Oda ayarlarını yalnız kurucu değiştirir.");change.run();openRoomMenu(p,room);}
    private void openRooms(Player p,int requested){List<GameRoom> waiting=rooms.values().stream().filter(r->r.phase==GameRoom.Phase.WAITING).toList();int last=Math.max(0,(waiting.size()-1)/8),page=Math.clamp(requested,0,last);List<Choice> buttons=new ArrayList<>();for(GameRoom r:waiting.subList(page*8,Math.min(waiting.size(),page*8+8)))buttons.add(new Choice("Oda "+r.id+" • "+r.ownerName()+" • "+r.members.size()+" kişi",()->joinRoom(p,r.id)));if(page>0)buttons.add(new Choice("Önceki",()->openRooms(p,page-1)));if(page<last)buttons.add(new Choice("Sonraki",()->openRooms(p,page+1)));buttons.add(new Choice("Geri",()->openLobbyMenu(p)));show(p,"Açık Vampir Köylü Odaları",List.of(waiting.isEmpty()?"Bekleyen oda yok; Oda Kur seçeneğini kullan.":"Katılmak istediğin odayı seç. • "+(page+1)+"/"+(last+1)),buttons);}
    void openTargetMenu(Player p,GameRoom room,String kind){
        UUID actor=p.getUniqueId();boolean vote=kind.equals("vote");if(membership.get(actor)!=room||!room.alive.contains(actor)||room.phase!=(vote?GameRoom.Phase.VOTE:GameRoom.Phase.ROLE))return;
        GameRoom.Role role=room.roles.get(actor);if(!vote&&role!=GameRoom.Role.VAMPIRE&&role!=GameRoom.Role.SEER&&role!=GameRoom.Role.DOCTOR){tell(p,"Bu rolde gece seçimi yok.");return;}
        List<Choice> choices=new ArrayList<>();for(UUID target:room.members){if(!room.alive.contains(target)||(!vote&&role==GameRoom.Role.VAMPIRE&&room.roles.get(target)==GameRoom.Role.VAMPIRE)||(!vote&&role!=GameRoom.Role.DOCTOR&&target.equals(actor)))continue;Player player=Bukkit.getPlayer(target);if(player==null)continue;choices.add(new Choice(player.getName(),()->{if(membership.get(actor)==room){if(vote)room.vote(actor,target);else room.chooseNight(actor,target);}}));}
        if(vote)choices.add(new Choice("Pas",()->{if(membership.get(actor)==room)room.vote(actor,null);}));choices.add(close());show(p,vote?"Vampir Köylü • Oylama":"Vampir Köylü • Gece Seçimi",List.of(vote?"Bir oyuncuya oy ver veya pas geç. Eşitlikte kimse elenmez.":"Rolün: "+role.label+" • Bu gece için hedefini seç."),choices);
    }
    void openTaskMenu(Player p,GameRoom room,int station,GameRoom.Task task){
        if(membership.get(p.getUniqueId())!=room||room.phase!=GameRoom.Phase.DAY||!room.alive.contains(p.getUniqueId()))return;
        List<Choice> choices=new ArrayList<>();int[] slots=task==null?new int[]{13}:new int[]{10,13,16};String[] names=task==null?new String[]{"Bakımı Tamamla"}:task.kind==0?new String[]{"1 • Sol","2 • Orta","3 • Sağ"}:new String[]{"Demir","Erzak","Odun"};
        for(int i=0;i<slots.length;i++){int slot=slots[i];choices.add(new Choice(names[i],()->{if(membership.get(p.getUniqueId())!=room)return;room.clickTaskGui(p,station,task,slot);if(task!=null&&!task.completed&&room.phase==GameRoom.Phase.DAY&&p.getWorld()==room.world&&p.getLocation().distanceSquared(MapLayout.STATIONS.get(station).location(room.world))<=25)openTaskMenu(p,room,station,task);}));}
        choices.add(close());show(p,MapLayout.STATIONS.get(station).name(),List.of(task==null?"İlk geceden sonra açılan ortak bakım işi.":task.kind==0?"Sıra: Sol → Orta → Sağ • "+task.step+"/3":"Sıra: Odun → Demir → Erzak • "+task.step+"/3"),choices);
    }

    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        try{
            if(args.length>0&&norm(args[0]).equals("yardim")){help(sender);return true;}
            if(!(sender instanceof Player p)){help(sender);return true;}ready(p);
            if(command.getName().equals("v")){GameRoom r=own(p);if(!r.alive.contains(p.getUniqueId())||r.roles.get(p.getUniqueId())!=GameRoom.Role.VAMPIRE||r.phase==GameRoom.Phase.WAITING)throw new IllegalArgumentException("Bu sohbet yaşayan vampirlere özel.");String message=ChatColor.stripColor(String.join(" ",args)).trim();if(message.isEmpty()||message.length()>256)throw new IllegalArgumentException("/v <1–256 karakterlik mesaj>");for(UUID id:r.alive)if(r.roles.get(id)==GameRoom.Role.VAMPIRE)tell(Bukkit.getPlayer(id),"§4[Vampir] "+p.getName()+": §f"+message);return true;}
            String action=args.length==0?"menu":norm(args[0]);
            switch(action){
                case "menu"->{enter(p);menu(p);}
                case "cik"->exit(p);
                case "durum"->{if(rooms.isEmpty())tell(p,"Açık oda yok.");for(GameRoom r:rooms.values())tell(p,"Oda "+r.id+" • "+phase(r)+" • "+r.members.size()+" kişi");}
                case "oda"->{if(args.length<2)throw new IllegalArgumentException("/vk oda kur | liste | katıl <numara> | ayrıl");String sub=norm(args[1]);if(sub.equals("ayril")){leaveRoom(p);return true;}enter(p);switch(sub){case "kur"->createRoom(p);case "liste"->openRooms(p,0);case "katil"->{if(args.length!=3)throw new IllegalArgumentException("/vk oda katıl <numara>");joinRoom(p,Integer.parseInt(args[2]));}default->throw new IllegalArgumentException("/vk oda kur | liste | katıl <numara> | ayrıl");}}
                case "baslat"->startRoom(p,false);
                case "deneme"->startRoom(p,true);
                case "bitir"->{GameRoom r=own(p);if(!r.isSpeaker(p.getUniqueId()))throw new IllegalArgumentException("Şu anda söz sırası sende değil.");r.nextSpeaker();}
                case "durdur"->{admin(p);GameRoom r=args.length==2?rooms.get(Integer.parseInt(args[1])):membership.get(p.getUniqueId());if(r==null)throw new IllegalArgumentException("/vk durdur <oda numarası>");r.finish(null,"Yönetici durdurdu.");}
                case "lobi"->{if(args.length==2&&norm(args[1]).equals("ayarla"))setLobby(p);else leaveRoom(p);}
                case "dogma"->{if(args.length!=2||!norm(args[1]).equals("ayarla"))throw new IllegalArgumentException("/vk doğma ayarla");setLobby(p);}
                case "setspawn"->setLobby(p);
                case "duzenleme"->{admin(p);if(lobby.contains(p.getUniqueId())||hub.playing(p.getUniqueId()))throw new IllegalArgumentException("Önce /lobi ile oyundan çık.");if(args.length!=2)throw new IllegalArgumentException("/vk düzenleme aç | kapat");switch(norm(args[1])){case "ac"->{editors.add(p.getUniqueId());tell(p,"Harita düzenleme açıldı. Bitince /vk düzenleme kapat kullan.");}case "kapat"->{editors.remove(p.getUniqueId());tell(p,"Harita düzenleme kapandı.");}default->throw new IllegalArgumentException("/vk düzenleme aç | kapat");}}
                default->help(p);
            }
        }catch(NumberFormatException e){tell(sender,"Oda numarası sayı olmalı.");}catch(IllegalArgumentException|IOException e){tell(sender,e.getMessage());}catch(RuntimeException e){getLogger().log(Level.WARNING,"Komut tamamlanamadı.",e);tell(sender,"İşlem tamamlanamadı; yönetici sunucu kaydını kontrol etmeli.");}return true;
    }
    private void setLobby(Player p){admin(p);if(!lobby.isEmpty())throw new IllegalArgumentException("Lobi değişmeden önce tüm oyuncular Vampir Köylü'den çıkmalı.");Location at=p.getLocation();if(isInstanceWorld(at.getWorld())||!at.getBlock().isPassable()||!at.clone().add(0,1,0).getBlock().isPassable()||!at.clone().add(0,-1,0).getBlock().getType().isSolid())throw new IllegalArgumentException("Geçici oda dışında, sağlam zemin üzerinde dur.");getConfig().set("lobby.world",at.getWorld().getName());getConfig().set("lobby.x",at.getX());getConfig().set("lobby.y",at.getY());getConfig().set("lobby.z",at.getZ());getConfig().set("lobby.yaw",at.getYaw());getConfig().set("lobby.pitch",at.getPitch());saveConfig();tell(p,"Vampir Köylü lobi doğma noktası ayarlandı.");}
    private void help(CommandSender p){for(String line:List.of("/vk — sayfayı aç; /oyunlar git vampirkoylu — oyun lobisine git","/vk oda kur | liste | katıl <numara> | ayrıl","/vk başlat — normal maçı başlat (6–20 kişi)","/vk deneme — yönetici kurucunun 2 kişilik denemesi","/vk bitir — sadece kendi konuşma sıranı bitir","/vk lobi — odadan oyun lobisine dön; /lobi veya /vk çık — ana lobi","/v <mesaj> — yaşayan vampirlerin gizli sohbeti","Yönetici: /vk durdur [oda] • /vk doğma ayarla • /vk düzenleme aç|kapat","/vk durum — oda listesi; /vk yardım — komutlar"))tell(p,line);}
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){List<String> values=args.length==1?List.of("yardım","oda","başlat","deneme","bitir","lobi","çık","durum","durdur","doğma","düzenleme"):args.length==2?switch(norm(args[0])){case "oda"->List.of("kur","liste","katıl","ayrıl");case "dogma","lobi"->List.of("ayarla");case "duzenleme"->List.of("aç","kapat");default->List.of();}:args.length==3&&norm(args[0]).equals("oda")?rooms.keySet().stream().map(String::valueOf).toList():List.of();String prefix=norm(args[args.length-1]);return values.stream().filter(s->norm(s).startsWith(prefix)).toList();}

    @EventHandler(priority=EventPriority.HIGHEST) public void inventory(InventoryClickEvent e){if(lobby.contains(e.getWhoClicked().getUniqueId()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void drag(InventoryDragEvent e){if(lobby.contains(e.getWhoClicked().getUniqueId()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void interact(PlayerInteractEvent e){
        Player p=e.getPlayer();if(e.getHand()!=EquipmentSlot.HAND)return;
        if(!lobby.contains(p.getUniqueId())){if(e.getClickedBlock()!=null&&protectedAt(e.getClickedBlock().getLocation())&&!editing(p,e.getClickedBlock().getLocation()))e.setCancelled(true);return;}
        if(e.getAction()!=Action.RIGHT_CLICK_AIR&&e.getAction()!=Action.RIGHT_CLICK_BLOCK)return;
        if(!authenticated(p)){e.setCancelled(true);return;}
        String item=tag(e.getItem());if(waiting(p)){e.setCancelled(true);if(item.equals("compass"))menu(p);return;}
        GameRoom r=membership.get(p.getUniqueId());if(r==null||r.world!=p.getWorld()||!r.alive.contains(p.getUniqueId())){e.setCancelled(true);return;}
        if(item.equals("report")){e.setCancelled(true);r.reportNearest(p);return;}if(item.equals("tasks")){e.setCancelled(true);r.taskBook(p);return;}
        Block b=e.getClickedBlock();if(b==null)return;
        e.setCancelled(true);if(r.phase!=GameRoom.Phase.DAY)return;
        if(b.getX()==0&&b.getY()==70&&b.getZ()==0&&b.getType()==Material.BELL){r.ringEmergency(p);return;}
        for(int i=0;i<MapLayout.STATIONS.size();i++){var s=MapLayout.STATIONS.get(i);if(b.getX()==s.x()&&b.getY()==s.y()&&b.getZ()==s.z()){r.clickStation(p,i);return;}}
        String material=b.getType().name();if(material.endsWith("_DOOR")||material.endsWith("_TRAPDOOR")||material.endsWith("_FENCE_GATE"))e.setCancelled(false);
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void interactEntity(PlayerInteractEntityEvent e){Player p=e.getPlayer();if(lobby.contains(p.getUniqueId())){e.setCancelled(true);GameRoom r=membership.get(p.getUniqueId());if(authenticated(p)&&r!=null&&r.world==p.getWorld()&&r.corpses.containsKey(e.getRightClicked().getUniqueId()))r.reportNearest(p);}else if(protectedAt(e.getRightClicked().getLocation())&&!editing(p,e.getRightClicked().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void damage(EntityDamageByEntityEvent e){
        Player source=e.getDamager() instanceof Player p?p:e.getDamager() instanceof AbstractArrow a&&a.getShooter() instanceof Player p?p:null;
        if(lobby.contains(e.getEntity().getUniqueId())||protectedAt(e.getEntity().getLocation())||source!=null&&lobby.contains(source.getUniqueId()))e.setCancelled(true);
        if(!(e.getEntity() instanceof Player victim)||source==null||!authenticated(source)||!authenticated(victim))return;
        GameRoom r=membership.get(source.getUniqueId());if(r==null||r!=membership.get(victim.getUniqueId())||r.world!=victim.getWorld()||source.getWorld()!=r.world)return;
        if(e.getDamager() instanceof AbstractArrow arrow){Integer id=arrow.getPersistentDataContainer().get(shotKey,PersistentDataType.INTEGER);arrow.getPersistentDataContainer().remove(shotKey);arrow.remove();if(id!=null&&id==r.id)r.hunterShot(source,victim);}else if(e.getDamager() instanceof Player)r.dayKill(source,victim);
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void generalDamage(EntityDamageEvent e){if(lobby.contains(e.getEntity().getUniqueId())||protectedAt(e.getEntity().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void launch(ProjectileLaunchEvent e){if(!(e.getEntity().getShooter() instanceof Player p)||!lobby.contains(p.getUniqueId()))return;GameRoom r=membership.get(p.getUniqueId());if(!(e.getEntity() instanceof AbstractArrow)||!authenticated(p)||r==null||r.phase!=GameRoom.Phase.DAY||!r.alive.contains(p.getUniqueId())||r.roles.get(p.getUniqueId())!=GameRoom.Role.HUNTER||r.hunterShots.getOrDefault(p.getUniqueId(),0)>=Math.clamp(getConfig().getInt("game.hunter-arrows",1),0,64)){e.setCancelled(true);return;}r.hunterShots.merge(p.getUniqueId(),1,Integer::sum);e.getEntity().getPersistentDataContainer().set(shotKey,PersistentDataType.INTEGER,r.id);}
    @EventHandler public void arrowPickup(PlayerPickupArrowEvent e){if(lobby.contains(e.getPlayer().getUniqueId())||e.getArrow().getPersistentDataContainer().has(shotKey))e.setCancelled(true);}
    @EventHandler public void pickup(EntityPickupItemEvent e){if(lobby.contains(e.getEntity().getUniqueId()))e.setCancelled(true);}
    @EventHandler public void drop(PlayerDropItemEvent e){if(lobby.contains(e.getPlayer().getUniqueId()))e.setCancelled(true);}
    @EventHandler public void swap(PlayerSwapHandItemsEvent e){if(lobby.contains(e.getPlayer().getUniqueId()))e.setCancelled(true);}
    @EventHandler public void food(FoodLevelChangeEvent e){if(lobby.contains(e.getEntity().getUniqueId()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void breaking(BlockBreakEvent e){if((lobby.contains(e.getPlayer().getUniqueId())||protectedAt(e.getBlock().getLocation()))&&!editing(e.getPlayer(),e.getBlock().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void placing(BlockPlaceEvent e){if((lobby.contains(e.getPlayer().getUniqueId())||protectedAt(e.getBlock().getLocation()))&&!editing(e.getPlayer(),e.getBlock().getLocation()))e.setCancelled(true);}
    @EventHandler public void bucket(PlayerBucketEmptyEvent e){if(protectedAt(e.getBlock().getLocation())&&!editing(e.getPlayer(),e.getBlock().getLocation())||lobby.contains(e.getPlayer().getUniqueId()))e.setCancelled(true);}
    @EventHandler public void bucket(PlayerBucketFillEvent e){if(protectedAt(e.getBlock().getLocation())&&!editing(e.getPlayer(),e.getBlock().getLocation())||lobby.contains(e.getPlayer().getUniqueId()))e.setCancelled(true);}
    @EventHandler public void explode(EntityExplodeEvent e){e.blockList().removeIf(b->protectedAt(b.getLocation()));}
    @EventHandler public void explode(BlockExplodeEvent e){e.blockList().removeIf(b->protectedAt(b.getLocation()));}
    @EventHandler public void burn(BlockBurnEvent e){if(protectedAt(e.getBlock().getLocation()))e.setCancelled(true);}
    @EventHandler public void ignite(BlockIgniteEvent e){if(protectedAt(e.getBlock().getLocation())&&(e.getPlayer()==null||!editing(e.getPlayer(),e.getBlock().getLocation())))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void teleport(PlayerTeleportEvent e){
        Player p=e.getPlayer();if(internalTeleports.contains(p.getUniqueId()))return;Location to=e.getTo();if(to==null)return;GameRoom r=membership.get(p.getUniqueId());
        if(isInstanceWorld(to.getWorld())&&(r==null||to.getWorld()!=r.world)){e.setCancelled(true);return;}
        if(!lobby.contains(p.getUniqueId()))return;
        if(r!=null&&r.world!=null){if(to.getWorld()!=r.world||!MapLayout.inside(to)||r.alive.contains(p.getUniqueId())&&r.phase!=GameRoom.Phase.DAY)e.setCancelled(true);}else if(!protectedLobby(to))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void move(PlayerMoveEvent e){Player p=e.getPlayer();if(internalTeleports.contains(p.getUniqueId())||e.getTo()==null||e.getFrom().toVector().equals(e.getTo().toVector())||!lobby.contains(p.getUniqueId()))return;GameRoom r=membership.get(p.getUniqueId());if(r!=null&&r.world!=null){if(!MapLayout.inside(e.getTo())||r.alive.contains(p.getUniqueId())&&r.phase!=GameRoom.Phase.DAY)e.setTo(e.getFrom());}else if(!protectedLobby(e.getTo()))e.setTo(e.getFrom());}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void chat(AsyncChatEvent e){
        GameRoom r=membership.get(e.getPlayer().getUniqueId());if(r==null)return;e.setCancelled(true);String message=PlainTextComponentSerializer.plainText().serialize(e.message());Player p=e.getPlayer();Bukkit.getScheduler().runTask(this,()->{
            UUID id=p.getUniqueId();if(!authenticated(p)||membership.get(id)!=r)return;if(r.phase!=GameRoom.Phase.WAITING&&(!r.alive.contains(id)||r.phase!=GameRoom.Phase.DAY&&!r.isSpeaker(id))){tell(p,"Şu anda konuşamazsın.");return;}
            for(UUID target:r.members){Player q=Bukkit.getPlayer(target);if(q==null)continue;boolean receive=r.phase==GameRoom.Phase.WAITING||r.alive.contains(target)&&(r.phase!=GameRoom.Phase.DAY||q.getWorld()==p.getWorld()&&q.getLocation().distanceSquared(p.getLocation())<=100);if(receive)q.sendMessage(Component.text("[Vampir Köylü] "+p.getName()+": "+message));}
        });
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void commands(PlayerCommandPreprocessEvent e){if(!playing(e.getPlayer()))return;String root=norm(e.getMessage().split(" ",2)[0]);if(!Set.of("/vk","/vampirkoylu","/v","/lobi","/kasabahub:lobi","/kasabavampirkoylu:vk","/kasabavampirkoylu:v","/kasabavampirkoylu:vampirkoylu").contains(root)){e.setCancelled(true);tell(e.getPlayer(),"Maç sırasında /vk, /v ve /lobi kullanabilirsin.");}}
    @EventHandler(priority=EventPriority.LOWEST) public void quit(PlayerQuitEvent e){editors.remove(e.getPlayer().getUniqueId());if(lobby.contains(e.getPlayer().getUniqueId()))detach(e.getPlayer(),true);}
    @EventHandler public void join(PlayerJoinEvent e){Bukkit.getScheduler().runTask(this,()->recover(e.getPlayer()));}
    @EventHandler(priority=EventPriority.MONITOR) public void authenticated(PlayerAuthenticatedEvent e){Bukkit.getScheduler().runTask(this,()->recover(e.getPlayer()));}
    private void recover(Player p){if(!authenticated(p)||lobby.contains(p.getUniqueId())||!PlayerBackup.exists(getDataFolder(),p.getUniqueId()))return;try{internalTeleports.add(p.getUniqueId());try{PlayerBackup.restore(getDataFolder(),p,true);}finally{internalTeleports.remove(p.getUniqueId());}tell(p,"Önceki oturumdaki envanterin güvenli biçimde geri yüklendi.");}catch(Exception e){getLogger().log(Level.SEVERE,"Envanter kurtarma kaydı korundu: "+p.getName(),e);p.kick(Component.text("Envanter yedeğin korunuyor; yöneticiye haber ver."));}}
}

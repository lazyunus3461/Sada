package tr.kasaba.akce;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import tr.kasaba.akce.api.AkceApi;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

public final class KasabaAkce extends JavaPlugin implements Listener,CommandExecutor,TabCompleter {
    private AkceLedger ledger;
    private BigDecimal initial;
    private Object arenaBridge;
    @Override public void onEnable(){
        saveDefaultConfig();try{Files.createDirectories(getDataFolder().toPath());initial=AkceLedger.parse(getConfig().getString("baslangic-akcesi","0.00"));ledger=new AkceLedger(getDataFolder().toPath().resolve("hesaplar.properties"));}
        catch(Exception ex){getLogger().log(Level.SEVERE,"Akçe başlatılamadı. Mevcut hesap dosyası değiştirilmedi.",ex);Bukkit.getPluginManager().disablePlugin(this);return;}
        Objects.requireNonNull(getCommand("akce")).setExecutor(this);getCommand("akce").setTabCompleter(this);Bukkit.getPluginManager().registerEvents(this,this);
        Bukkit.getServicesManager().register(AkceApi.class,ledger,this,ServicePriority.Normal);connectArena();for(Player p:Bukkit.getOnlinePlayers())remember(p);
    }
    @Override public void onDisable(){Bukkit.getServicesManager().unregisterAll(this);arenaBridge=null;}
    private void connectArena(){if(arenaBridge==null&&Bukkit.getPluginManager().isPluginEnabled("KasabaArena")){try{arenaBridge=new ArenaBridge(this,ledger);getLogger().info("X Arena marketi Akçe hesabına bağlandı.");}catch(LinkageError ex){getLogger().log(Level.WARNING,"Arena bağlantısı için KasabaArena 1.2.0 kurulu olmalı.",ex);}}}
    @EventHandler public void enabled(PluginEnableEvent e){if(e.getPlugin().getName().equals("KasabaArena"))connectArena();}
    @EventHandler public void joined(PlayerJoinEvent e){remember(e.getPlayer());}
    private void remember(Player p){try{ledger.remember(p.getUniqueId(),p.getName(),initial);}catch(RuntimeException ex){getLogger().log(Level.SEVERE,"Oyuncu hesabı kaydedilemedi: "+p.getUniqueId(),ex);tell(p,ex.getMessage());}}
    private void tell(CommandSender sender,String text){sender.sendMessage(Component.text("["+getConfig().getString("etiket","Akçe")+"] ",NamedTextColor.GOLD).append(Component.text(text,NamedTextColor.YELLOW)));}
    private String normalize(String value){return value.toLowerCase(Locale.ROOT).replace('ç','c').replace('ö','o').replace('ı','i').replace('ş','s').replace('ü','u').replace('ğ','g');}
    private Player player(CommandSender sender){if(sender instanceof Player p)return p;throw new IllegalArgumentException("Konsoldan /akçe bak <oyuncu> kullan.");}
    private UUID target(String name){Player online=Bukkit.getPlayerExact(name);if(online!=null){ledger.remember(online.getUniqueId(),online.getName(),initial);return online.getUniqueId();}return ledger.find(name).orElseThrow(()->new IllegalArgumentException("Oyuncu bulunamadı. Oyuncu sunucuya en az bir kez girmiş olmalı."));}
    private void admin(CommandSender sender){if(!sender.hasPermission("kasabaakce.yonet"))throw new IllegalArgumentException("Akçe yönetim yetkin yok.");}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(!sender.hasPermission("kasabaakce.kullan")){tell(sender,"Bu komut için yetkin yok.");return true;}
        try{
            if(args.length==0){Player p=player(sender);tell(p,"Bakiyen: "+ledger.balance(p.getUniqueId()).toPlainString()+" Akçe");p.sendMessage(Component.text("[Akçe gönder] ",NamedTextColor.GREEN).clickEvent(ClickEvent.suggestCommand("/akçe gönder ")).append(Component.text("[Zenginler]",NamedTextColor.AQUA).clickEvent(ClickEvent.runCommand("/akçe sıralama"))));return true;}
            String action=normalize(args[0]);
            switch(action){
                case "gonder"->{if(!sender.hasPermission("kasabaakce.gonder"))throw new IllegalArgumentException("Akçe gönderme yetkin yok.");if(args.length!=3)throw new IllegalArgumentException("/akçe gönder <oyuncu> <miktar>");Player p=player(sender);UUID to=target(args[1]);BigDecimal amount=AkceLedger.parse(args[2]);if(!ledger.transfer(p.getUniqueId(),to,amount))throw new IllegalArgumentException("Bakiyen yetersiz.");tell(p,ledger.name(to)+" adlı oyuncuya "+amount.toPlainString()+" Akçe gönderdin. Kalan: "+ledger.balance(p.getUniqueId()).toPlainString());Player recipient=Bukkit.getPlayer(to);if(recipient!=null)tell(recipient,p.getName()+" sana "+amount.toPlainString()+" Akçe gönderdi.");getLogger().info(p.getName()+" -> "+ledger.name(to)+": "+amount.toPlainString()+" Akçe");}
                case "bak","bakiye"->{if(args.length!=2)throw new IllegalArgumentException("/akçe bak <oyuncu>");UUID to=target(args[1]);tell(sender,ledger.name(to)+": "+ledger.balance(to).toPlainString()+" Akçe");}
                case "siralama","zenginler"->{tell(sender,"En çok Akçesi olan 10 oyuncu:");List<UUID> top=ledger.richest();for(int i=0;i<top.size();i++){UUID id=top.get(i);tell(sender,(i+1)+". "+ledger.name(id)+" — "+ledger.balance(id).toPlainString()+" Akçe");}}
                case "ver","al","ayarla"->{admin(sender);if(args.length!=3)throw new IllegalArgumentException("/akçe "+action+" <oyuncu> <miktar>");UUID to=target(args[1]);BigDecimal amount=AkceLedger.parse(args[2]);if(action.equals("ver"))ledger.deposit(to,amount);else if(action.equals("ayarla"))ledger.set(to,amount);else if(!ledger.withdraw(to,amount,"yonetim:"+UUID.randomUUID()))throw new IllegalArgumentException("Oyuncunun bakiyesi yetersiz.");tell(sender,ledger.name(to)+" bakiyesi: "+ledger.balance(to).toPlainString()+" Akçe");Player recipient=Bukkit.getPlayer(to);if(recipient!=null&&recipient!=sender)tell(recipient,"Bakiyen yönetici tarafından güncellendi: "+ledger.balance(to).toPlainString()+" Akçe");getLogger().info(sender.getName()+" "+action+" "+ledger.name(to)+" "+amount.toPlainString()+" Akçe");}
                case "yardim"->{tell(sender,"/akçe • /akçe gönder <oyuncu> <miktar> • /akçe bak <oyuncu> • /akçe sıralama");if(sender.hasPermission("kasabaakce.yonet"))tell(sender,"Yönetim: /akçe ver|al|ayarla <oyuncu> <miktar>");}
                default->throw new IllegalArgumentException("Komut bulunamadı. /akçe yardım");
            }
        }catch(IllegalArgumentException ex){tell(sender,ex.getMessage());}catch(RuntimeException ex){getLogger().log(Level.SEVERE,"Akçe işlemi tamamlanamadı.",ex);tell(sender,"İşlem tamamlanamadı; /akçe ile bakiyeni kontrol et.");}
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){
        List<String> options=new ArrayList<>();if(args.length==1){options.addAll(List.of("gönder","bak","sıralama","yardım"));if(sender.hasPermission("kasabaakce.yonet"))options.addAll(List.of("ver","al","ayarla"));}else if(args.length==2&&Set.of("gonder","bak","bakiye","ver","al","ayarla").contains(normalize(args[0])))for(Player p:Bukkit.getOnlinePlayers())options.add(p.getName());else if(args.length==3&&Set.of("gonder","ver","al","ayarla").contains(normalize(args[0])))options.addAll(List.of("10","100","1000"));String prefix=normalize(args[args.length-1]);return options.stream().filter(v->normalize(v).startsWith(prefix)).toList();
    }
}

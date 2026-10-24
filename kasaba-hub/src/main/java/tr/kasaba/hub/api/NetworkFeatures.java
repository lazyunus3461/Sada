package tr.kasaba.hub.api;

import java.math.BigDecimal;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Optional network features, supplied by KasabaSosyal after all games load. */
public interface NetworkFeatures {
    boolean vip(Player player);
    void socialMenu(Player player);
    void market(Player player);
    void vipInfo(Player player);
    default void earnInfo(Player player){player.sendMessage("/olta • /sat balık • Aktif oyun: 15 dk = 0,20 Akçe, günlük 5 ödül.");}
    default void beforeGame(Player player){}
    boolean pay(Player player,BigDecimal amount,String transaction);
    boolean pay(java.util.UUID player,BigDecimal amount,String transaction);
    static NetworkFeatures get(){return Bukkit.getServicesManager().load(NetworkFeatures.class);}
    static boolean isVip(Player player){NetworkFeatures service=get();return service!=null&&service.vip(player);}
    static int cooldown(Player player,int seconds){return Math.max(1,seconds-(isVip(player)?2:0));}
}

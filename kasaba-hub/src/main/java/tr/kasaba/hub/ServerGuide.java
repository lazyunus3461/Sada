package tr.kasaba.hub;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.List;

/** The same guide opens from the tagged lobby book and the /rehber command. */
final class ServerGuide {
    private ServerGuide(){}
    static ItemStack create(org.bukkit.NamespacedKey key){
        ItemStack item=new ItemStack(Material.WRITTEN_BOOK);BookMeta meta=(BookMeta)item.getItemMeta();meta.title(Component.text("Kasaba Rehberi"));meta.author(Component.text("Kasaba"));meta.displayName(Component.text("Kasaba • Bilgi Kitabı",NamedTextColor.GOLD));meta.lore(List.of(Component.text("Oyunlar, kurallar ve komutlar",NamedTextColor.GRAY),Component.text("Açmak için sağ tıkla • /rehber",NamedTextColor.AQUA)));meta.getPersistentDataContainer().set(key,PersistentDataType.BYTE,(byte)2);
        meta.pages(List.of(
            page("Kasaba'ya Hoş Geldin","Arkadaşlarınla oyna,\narenada savaş veya\nkendi adanı inşa et.\n\n/oyunlar → oyun seç\n/grup → arkadaş grubu\n/lobi → ana lobi\n/rehber → bu kitap\n\nSayfaları çevir →"),
            page("Arena Girişi","/oyunlar git arena\nGüvenli arena lobisine\ngötürür. Burada savaş,\nblok kırma ve koyma\nkapalıdır.\n\n/arena → seçim menüsü\n/arena lobi → güvenli\narena girişine dön.\n/lobi → ana lobi."),
            page("1v1 ve 2v2","/arena 1v1\n/arena 2v2\nBoş odaya girersin.\n2 veya 4 kişi olunca\nmaç başlar.\n\nİki raunt kazanan alır;\nen fazla üç raunt.\nNormal kit silahları.\nVIP: netherit Korunma III.\nMaç sonunda arena\nlobisine dönersin."),
            page("Arkadaşla 1v1","/arena oda oluştur\nAltı haneli özel kod\nverir. Arkadaşın önce\narena lobisine gelsin.\n\n/arena katıl 123456\nÖrnek kod yerine\naldığınız kodu yazın.\n\n/arena oda liste\nOdaların durumunu gör."),
            page("X Arena","/arena xarena\nSınırsız PvP alanı.\nÖlünce eşya düşmez;\naynı arenanın farklı\nnoktasında doğarsın.\n\n/arena sınıf\nKarakterini seç.\nÖlümden sonra da\ndeğiştirebilirsin.\nSağda ilk 5 öldürme."),
            page("X Arena Becerileri","1–4. kutu: beceriler\n5. kutu: kılıç\nOkçu yayı: 6. kutu\n\nSağ tıkla kullan veya\n/arena yetenek 1\nNumara 1–4 olabilir.\nSıra şartı yok; her\nbecerinin ayrı süresi\nvardır. Hazır olan\nbeceriler parlar."),
            page("Karakterler","Savaşçı: darbe ve alan\nhasarı, savaş narası.\nSuikastçı: zehir,\natılım ve gizlenme.\nTank: kalkan ve şifa.\nOkçu: güçlü oklar,\nok yağmuru ve kaçış.\nBüyücü: alev, buz,\nışınlanma ve meteor.\nŞifacı: şifa, kalkan\nve ışık saldırıları."),
            page("Adanı Yap","İlk kez: /ada al\nNormal: 50 Akçe, 1 ada.\nVIP: 2 ücretsiz ada.\n\nSonra /oyunlar →\nAdanı Yap düğmesiyle\nkendi adana dön.\n/ada da adana götürür.\n\nAdada yaratıcı mod.\n15 gün giriş yapmazsan\nyapılar ve ada sıfırlanır.\nÇıkış: /lobi"),
            page("Adada Arkadaşlar","/ada ekle Ali\nArkadaşına adanda\nyapı yapma izni ver.\n\n/ada Redkit\nEklendiğin adaya git.\n/ada çıkar Ali\nİzni kaldır.\n\nAda envanterin ayrıdır;\nyaratıcı eşyalar diğer\noyunlara taşınmaz."),
            page("Arkadaşlar ve VIP","/arkadaş → arkadaşlar\n/mesaj Ali Merhaba\nArkadaş mesajları ve\ngruplar spawn/lobide.\nBoya: gizle / göster.\n\n/vip → ayrıcalıklar\n/market → VIP satın al\n3 gün 40 • 7 gün 75\n15 gün 145 • 30 gün 250\nAkçe. Süre gerçek zaman."),
            page("Spawn Balıkçılığı","/olta → ücretsiz olta\nYeni olta: 1 saat.\n/sat balık → kendi\nbalığını 0,05 Akçeye sat.\nGünlük 15 adet.\n\n15. satıştan sonra\n24 saat ara başlar.\nBu sürede gelen balıklar\notomatik kaldırılır.\nSatışta olta 0 Akçeye\ngeri alınır."),
            page("Aktif Oyun ve Akçe","Oyun süreleri birleşir.\n15 dk: +0,20 Akçe.\nGünde 5 ödül (1 Akçe).\nTürkiye gece yarısı\nhaklar yenilenir.\n\nLobi/AFK/seyirci\nsayılmaz; çıkışta\nsüren saklanır.\n\n/akçe → bakiyen\n/kazanç → kazanma rehberi"),
            page("At ve Tekne Yarışı","/oyunlar → At Yarışı\nveya Tekne Yarışı.\nİkisi aynı lobidedir.\nPusuladan oyun seç.\n\n/yaris → menü\n/yaris oluştur at\n/yaris oluştur tekne\n/yaris katıl 101\n/yaris başlat\nİki tur, sıralı kapılar.\nÖnce açık odaları gör.\nEngellerden dikkatle geç.\n/yaris ayrıl → yarış lobisi"),
            page("Şans Yolu","/sansyolu → menü\n/sansyolu oluştur\n/sansyolu katıl 101\n/sansyolu başlat\n/sansyolu zar\n\nZar otomatik atılmaz.\n3 kez atlamada lobi.\nSarıda kestirme çubuğu\nverilir; sağ tıkla kullan.\nRenkler her maç değişir.\nSonuçtan 10 sn sonra lobi."),
            page("Renk Kaçışı","/renk → menü\n/renk oda 1\n/renk başlat\n\nHedef renge yetiş.\nİki hata hakkın var.\nHer tur 8 itme kartopu;\n2 sn arayla sağ tıkla.\n3. turdan sonra alan\nve renk kareleri küçülür.\n\nTüm oyun menülerinde\nBurada kal ile lobide\nbekleyebilirsin."),
            page("Lanetli Taç","/tac → menü\n/tac oyna → sıraya gir\n/tac lobi → oyun lobisi\n\n6–16 kişi; 4 dakika.\nTacı taşı, puan topla.\nYakından tacı kap.\nKapılar 30 sn değişir.\n3 eşzamanlı oyun.\nLobide bekleyebilirsin."),
            page("Boya Savaşı","/boya → menü\n/boya oyna → sıraya gir\n/boya lobi → oyun lobisi\n\nKırmızı ve mavi takım.\nSağ tıkla alanı boya.\n3 dakika sonunda daha\nçok alan kazansın.\nRakip boya geri alınır.\nLobide bekleyebilirsin;\nOyna seçmeden sıra yok."),
            page("Eşya Saklambacı","/esya → menü\n/esya oyna → sıraya gir\n/esya kılık → eşya seç\n/esya lobi → oyun lobisi\n\n4–12 kişi, üç harita.\n20 sn saklan, 4 dk dayan.\nEbeler eşyaları arar.\nYanlış tahmin yavaşlatır.\nBulununca ebe olursun.\nKılık aralığı: 20 sn.\n\nLobide bekleyebilirsin;\nOyna seçmeden sıra yok."),
            page("Hapishane","Yetkili komutları:\n/hapis Oyuncu 60dk\n/hapis durum Oyuncu\n/hapis bırak Oyuncu\n\nCezalı oyuncu:\n/hapis durum\n\nÜstte kalan süre yazar.\nCeza gerçek zamanla\nişler; çıkışta ve sunucu\nyenilendiğinde korunur.\nSüre bitince ana lobi."),
            page("Maçı Yarım Bırakma","Başlayan maçı /lobi ile\nbırakırsan veya çıkarsan\no oyuna 15 dk giremezsin.\nDiğer oyunlar açık kalır.\n\nBekleme odası ve normal\nbitişte ceza yok.\nX Arena çıkışı serbest.\n\n/oyunceza → kalan süre\nPusula → Oyun lobileri\nLobiden lobiye geç.\nMenü eşyasına envanterde\ntıklayarak da açabilirsin."),
            page("Akçe ve Market","/akçe → bakiyen\n/akçe gönder Ali 100\nArkadaşına para gönder.\n/akçe sıralama\nZenginler listesi.\n\n/arena market\nX Arena ürünleri.\nSatın alınanlar yalnızca\nX Arena'da kullanılır;\nsüreli ürünler sona erer.")
        ));item.setItemMeta(meta);return item;
    }
    private static Component page(String title,String text){return Component.text(title+"\n\n",NamedTextColor.DARK_RED).append(Component.text(text,NamedTextColor.BLACK));}
    static void open(Player player,org.bukkit.NamespacedKey key){player.openBook(create(key));}
}

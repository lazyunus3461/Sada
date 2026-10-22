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
            page("1v1 ve 2v2","/arena 1v1\n/arena 2v2\nBoş odaya girersin.\n2 veya 4 kişi olunca\nmaç başlar.\n\nİki raunt kazanan alır;\nen fazla üç raunt.\nYalnızca normal kit.\nMaç sonunda arena\nlobisine dönersin."),
            page("Arkadaşla 1v1","/arena oda oluştur\nAltı haneli özel kod\nverir. Arkadaşın önce\narena lobisine gelsin.\n\n/arena katıl 123456\nÖrnek kod yerine\naldığınız kodu yazın.\n\n/arena oda liste\nOdaların durumunu gör."),
            page("X Arena","/arena xarena\nSınırsız PvP alanı.\nÖlünce eşya düşmez;\naynı arenanın farklı\nnoktasında doğarsın.\n\n/arena sınıf\nKarakterini seç.\nÖlümden sonra da\ndeğiştirebilirsin.\nSağda ilk 5 öldürme."),
            page("X Arena Becerileri","1–4. kutu: beceriler\n5. kutu: kılıç\nOkçu yayı: 6. kutu\n\nSağ tıkla kullan veya\n/arena yetenek 1\nNumara 1–4 olabilir.\nSıra şartı yok; her\nbecerinin ayrı süresi\nvardır. Hazır olan\nbeceriler parlar."),
            page("Karakterler","Savaşçı: darbe ve alan\nhasarı, savaş narası.\nSuikastçı: zehir,\natılım ve gizlenme.\nTank: kalkan ve şifa.\nOkçu: güçlü oklar,\nok yağmuru ve kaçış.\nBüyücü: alev, buz,\nışınlanma ve meteor.\nŞifacı: şifa, kalkan\nve ışık saldırıları."),
            page("Adanı Yap","İlk kez: /ada al\nHer oyuncu yalnızca\nbir ada alabilir.\n\nSonra /oyunlar →\nAdanı Yap düğmesiyle\nkendi adana dön.\n/ada da adana götürür.\n\nAdada yaratıcı mod.\nÇıkış: /lobi"),
            page("Adada Arkadaşlar","/ada ekle Ali\nArkadaşına adanda\nyapı yapma izni ver.\n\n/ada Redkit\nEklendiğin adaya git.\n/ada çıkar Ali\nİzni kaldır.\n\nAda envanterin ayrıdır;\nyaratıcı eşyalar diğer\noyunlara taşınmaz."),
            page("Akçe ve Market","/akçe → bakiyen\n/akçe gönder Ali 100\nArkadaşına para gönder.\n/akçe sıralama\nZenginler listesi.\n\n/arena market\nX Arena ürünleri.\nSatın alınanlar yalnızca\nX Arena'da kullanılır;\nsüreli ürünler sona erer.")
        ));item.setItemMeta(meta);return item;
    }
    private static Component page(String title,String text){return Component.text(title+"\n\n",NamedTextColor.DARK_RED).append(Component.text(text,NamedTextColor.BLACK));}
    static void open(Player player,org.bukkit.NamespacedKey key){player.openBook(create(key));}
}

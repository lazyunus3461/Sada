# KasabaBilgi 1.0.0

Paper **1.21.10**, Java **21**, KasabaHub 1.6.1, KasabaAuth 1.3.0, KasabaAkce 1.1.0 ve KasabaRoles **1.3.2** için canlı sağ bilgi tablosu. Kaynak paketi, oyuncu modu, LuckPerms veya TAB gerektirmez.

Gösterilen bilgiler: oyuncu adı, gerçek sunucu rütbesi, Akçe bakiyesi, varsa klan etiketi, toplam sunucuda geçirilen süre, `play.kasabatr.com`, `dc: kasabatr`, `ig: kasabatrminecraft`. Gecikme/MS alanı ve sağdaki kırmızı satır sayaçları yoktur.

## Kurulum

1. Sunucuyu tamamen durdur; `plugins` klasörünü yedekle.
2. Eski **KasabaRoles JAR'ını** kaldır ve **KasabaRoles-1.3.2.jar** koy. `plugins/KasabaRoles` klasörünü koru; mevcut rol isimleri, renkleri, yetkileri ve oyuncu atamaları aynı kalır.
3. **KasabaBilgi-1.0.0.jar** dosyasını `plugins` içine ekle. İki JAR da hazır ZIP'tedir. Aynı eklentiden farklı sürümleri birlikte bırakma.
4. Sunucuyu başlat. Kayıt/giriş bittikten sonra tablo otomatik görünür. Dünya aktarmak veya konum ayarlamak gerekmez. `/reload` kullanma.

`/tablo kapat` ve `/tablo aç` kişisel paneli mevcut oturumda kapatıp açar. `/kasababilgi` de kullanılabilir. Bazı oyunlar maç sırasında ek komutları kısıtladığından bu seçimi spawn/lobide yap. Bilgi kitabının mevcut `/bilgi` komutu korunur.

## Görünüm

Spawn ve oyun lobilerinde mat altın başlık, mavi oyuncu simgesi, mor rütbe simgesi, altın Akçe, yeşil klan ve turkuaz süre simgesi vardır. Rütbenin değeri mevcut rol rengini kullanır. Adres, Discord ve Instagram farklı renklerle ayrılır.

Aktif oyunda başlık küçülür, kalın yazı ve ayraçlar kaldırılır, boş satırlar azalır; yazılar koyu gri/mavi tonlara geçer. Akçe, klan, süre ve bağlantılar korunur. X Arena'nın ilk beş öldürme satırı ve Şans Yolu'nun kare/puan sıralaması aynı panelde gösterilir. Ortak maç tablosunu kullanan oyuncular da kendi bakiye ve profil bilgilerini görür.

**Saydamlık sınırı:** vanilla Minecraft'ın sağ tablo arka planı ve gerçek alfa/saydamlık değeri oyuncunun istemci ayarına bağlıdır; Paper eklentisi bunu lobi/oyun için ayrı oranlarda değiştiremez. Bu sürüm daha düşük kontrast ve küçük düzenle oyun içindeki dikkati azaltır. Onay için verilen büyük çerçeveli görsel bir taslaktır; gerçek görünüm Minecraft'ın yerel yazı tipi, Unicode simgeleri ve standart sağ tablo boyutundadır. Özel 3B simge veya çizilmiş çerçeve eklenmez.

## Veriler ve uyumluluk

- Rütbe `RoleAccess.rank(Player)` ile mevcut atamadan okunur. Kurucu/Rehber gibi VIP yetkisini miras alanlar yanlışlıkla VIP olarak gösterilmez. Geçici VIP süresi ve rol önceliği hesaba katılır. Yeni metot eski API'ye eklenen varsayılan bir metottur; diğer eklentilerin kullandığı mevcut metotlar değişmez.
- Akçe, mevcut `AkceApi` servisinden okunur; fiyat/ödül/bakiye yazılmaz. Virgüllü iki ondalık gösterilir.
- Klan, `ClanLabels` servisi varsa okunur. Eklenti veya üyelik yoksa `Yok` yazar. Bu paket yeni bir klan/claim sistemi kurmaz.
- Süre, Minecraft'ın kalıcı `PLAY_ONE_MINUTE` istatistiğinden okunur. Geçmiş süreyi içerir; çıkış ve yeniden başlatmada sıfırlanmaz. Spawn/lobi dahil toplam çevrimiçi süredir. Akçe aktiflik ödülünün 15 dakikalık sayacı ayrıdır ve değiştirilmez.
- Yenileme saniyede birdir; yalnız değişen satırlar gönderilir. Yeni bir bakiye/süre veritabanı oluşturulmaz.
- Kişisel panel oyuncuya gönderilen ayrı bir görünüm olarak çalışır; Bukkit scoreboard'u değiştirilmez veya kopyalanmaz. Böylece rol/kafa üstü takımları, BR isim gizleme ve maçların ortak tabloları korunur. Eklenti kapanırken gerçek oyun tablosu geri gösterilir.
- Yalnız Minecraft/Paper **1.21.10** paket yapısı test edilmiştir. Başka sürüme geçişte güncel uyumluluk kontrolü gerekir.

## Derleme ve test

Java 21/Maven ile önce repo kökündeki Auth, sonra Roles, Akçe ve Hub, son olarak `mvn -f kasaba-bilgi/pom.xml install` çalıştırılır. Sonuç `target/KasabaBilgi-1.0.0.jar` dosyasıdır. Netty/Paper ve Kasaba servisleri sağlanan bağımlılıklardır; JAR içine başka eklentiler gömülmez.

`validation/1.0.0` test sonuçlarını içerir. `scripts/integration/BilgiProbe.java` yalnız özel loopback test sunucusu içindir; üretim sunucusuna veya ZIP'e konmaz.

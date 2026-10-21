# KasabaHub 1.0.0

Paper **1.21.10 / Java 21** üzerinde arkadaşlarla oynanan Minecraft oyun merkezi için ana eklenti. Ortak lobi, Minecraft'ın **yerleşik sayfa arayüzü**, geçici arkadaş grupları ve sonraki oyun eklentileri için bağlantı sağlar. Bu sürümde mini oyun bulunmaz; oyun sayfası açıkça **Oyunlar yakında** gösterir.

## Kurulum

[KasabaHub kurulum ZIP'ini indir](https://github.com/lazyunus3461/Sada/raw/refs/heads/main/dist/KasabaHub-Kurulum.zip)

1. Sunucuyu durdur. Mevcut eklenti veri klasörlerini yedekle.
2. Eski KasabaAuth JAR'ını `plugins` dışına taşı. Yeni modelde kullanılmayacak KasabaKlan/KasabaWarp ve başka lobi yöneticilerinin JAR'larını da dışarı taşı. **Veri klasörlerini silme.**
3. ZIP'teki **KasabaAuth-1.3.0.jar** ve **KasabaHub-1.0.0.jar** dosyalarını `plugins` içine koy. Mevcut KasabaRoles kalabilir; roller/renkler korunur. Test Probe dosyaları kurulmaz.
4. Sunucuyu aç, hesabına giriş yap. Güvenli lobi platformunda Kurucu/OP olarak **`/merkez lobi ayarla`** yaz.
5. `/lobi` ile dönüşü, `/merkez` ile ana sayfayı, `/oyunlar` ile oyun sayfasını dene.

KasabaAuth hesabı/şifresi aynı kalır. 1.3.0, doğrulama sonrası bir olay ve salt okunur durum servisi ekler. Hub bu olayı dinleyip oyuncuyu doğrulama **bittikten sonra** lobiye taşır; kayıt sayfasını atlamaz. Auth'ın kayıt yeri giriş sırasında kullanılmaya devam eder; aynı yerde olmasını istersen lobi platformunda `/kayityeri ayarla` da kullanabilirsin. Hub yoksa Auth'ın önceki kayıt yeri davranışı korunur.

## Oyuncu komutları

| Komut | İşlev |
| --- | --- |
| `/merkez` | Ana sayfa: oyunlar, arkadaş grubu, lobiye dönüş |
| `/oyunlar` | Kayıtlı oyunları sayfalar; şu anda oyun yok, yakında bilgisi gösterilir |
| `/lobi` | Oyun bağlantısını bırakıp kayıtlı ana lobiye döner |
| `/grup` | Arkadaş grubu sayfası |
| `/grup oluştur` | Geçici grup kurar; kuran lider olur |
| `/grup davet Redkit` | Lider çevrimiçi/giriş yapmış arkadaşına davet yollar |
| `/grup katıl LiderAdı` | 60 saniyelik daveti kabul eder |
| `/grup çıkar Redkit` | Lider, üyeyi çıkarır |
| `/grup lider Redkit` | Liderliği gruptaki başka üyeye devreder |
| `/grup ayrıl` | Ayrılır; lider ayrılırsa sıradaki üye lider olur |
| `/grup dağıt onayla` | Lider bütün grubu dağıtır |
| `/grup sohbet Merhaba` | Yalnızca grubun çevrimiçi üyelerine gider |

Grup sayfasında oluşturma, oyuncu adını yazarak davet, ayrılma ve onaylı dağıtma düğmeleri vardır. Sandık/envanter menüsü kullanılmaz. Sayfalar ESC ile kapanabilir. Oyuncuya ve açık sayfaya bağlı düğmeler yeni sayfa açıldığında geçersizleşir; işlem anında yetki ve güncel üyelik tekrar kontrol edilir.

Varsayılan grup sınırı **8**. Bir oyuncu aynı anda tek gruptadır; davetler kabul anında kapasiteyi yeniden denetler. Gruplar, davetler ve düzenleme izni çıkış/restartta temizlenir. Kalıcı arkadaş listesi, para, puan, mağaza ve mini oyun kuralları bu ilk sürüme eklenmedi. Oyun bitmeden grup değişikliği yapılmaz; `/lobi` kişinin oyundan ayrılmasını sağlar. Diğer üyeler oynuyorsa grup yönetimi onların da oyundan çıkmasını bekler.

## Yönetim ve lobi koruması

```text
/merkez lobi ayarla
/merkez lobi bilgi
/merkez düzenleme aç
/merkez düzenleme kapat
/merkez yenile
/merkez yardım
```

Yönetim yetkisi `kasabahub.yonet` (varsayılan OP) veya `kasabaroles.admin` (Kurucu). Normal kullanım `kasabahub.kullan`, herkese açık. Kurucu bile lobide inşa için **düzenleme aç** kullanır; çıkışta kapanır. Türkçe harfsiz `duzenleme ac`, `olustur`, `katil`, `cikar`, `ayril`, `dagit` biçimleri kabul edilir.

Lobi noktası çevresinde varsayılan X/Z **100 blok uzaklığa kadar** kare koruma vardır; bu, eski warpın 100×100 claim yasağından farklı bir lobi ayarıdır. Burada normal oyuncu inşa/kova/blok etkileşimi yapamaz, eşya atamaz/toplayamaz; hasar, açlık, patlama/yangın ve sınırdan piston/sıvı müdahalesi engellenir. Düzenleme modu inşa ve temel etkileşimleri açar. Oyuncuların envanteri, oyun modu, rolü ve scoreboard'u değiştirilmez. Oyun haritalarını ayrı dünyalarda kurmak önerilir. Lobide boşluğa düşme ve oyun dışında yeniden doğma lobiye dönüşü kullanır.

`plugins/KasabaHub/lobi.yml` dünya UUID'si, tam koordinat ve yönü atomik saklar. Lobi dünya spawnını veya başka eklentinin dosyasını değiştirmez. Yüklü olmayan dünya, dünya sınırı dışı konum ve başka eklentinin teleport iptali aşılmaz. Bölge asenkron hazırlanır; ayrılan oyuncu veya eski konum isteği sonradan ışınlanmaz.

`config.yml`: `grup-uye-siniri: 8`, `davet-suresi-saniye: 60`, `otomatik-sayfa: true`, `lobi-koruma-yaricapi: 100`. Girişten sonra büyük karşılama başlığını göstermek için ana sayfa lobiye varıştan iki saniye sonra açılır; bu sırada oyuncu kendi sayfasını açarsa üstüne yazılmaz. `/merkez yenile` çalışan grupları korur. Hatalı yenileme mevcut ayarı değiştirmez. İlk açılışta dosya bozuksa koruma normal dünyalarda da kapalı kalmak yerine inşa/etkileşimleri engeller; dosyayı düzeltip konsoldan `merkez yenile` kullan. Veriyi sıfırlamaz.

## Sonraki oyunları bağlama

`HubApi` Bukkit ServicesManager üzerinden alınır. Oyun eklentisi `depend: [KasabaHub]` kullanır ve ana iş parçacığında `api.register(this, adapter)` çağırır. `HubGame` değişmez `GameInfo`, kullanılabilirlik, takım kabulü ve çıkış kancası sağlar. Bütün grup tek çağrıyla ve değişmez oyuncu listesiyle teslim edilir. Giriş aralığı/oyuncu yetkileri/aktif oyun kontrolü tekrar yapılır. `join` takımı atomik kabul etmeli; `false` dönerken hiç kimsenin konumunu/envanterini değiştirmemelidir. Bu sözleşmenin harita ve envanter geri alma kısmından oyun eklentisi sorumludur.

Kabulden sonra merkez oyun durumunu izler. Oyun bitiminde `api.returnToLobby(playerUuid)` kullanılır. Eklenti kapanır veya kendi oyununu kaldırırsa katılımcılar lobiye yönlendirilir. Başka eklentinin oyunu kaldırılamaz; aynı kimlik iki kez kaydedilemez. Oyun eklentisi kendi arena, sıra/maç, skor, envanter geri alma ve kapanış temizliğini uygular. Merkez, olmayan maçı veya sahte oyuncu sayısını göstermez.

## Geliştirme ve test

Bulutta `bash kasaba-hub/scripts/cloud-build.sh`; Auth'ı yerel Maven'e kurar ve Hub'ı derler. Normal JDK 21/Maven ortamında repo kökünde `mvn install`, ardından `mvn -f kasaba-hub/pom.xml verify`. 17 Auth ve 13 Hub birim testi bulunur. [Yerel test](scripts/integration/README.md) kayıt, lobi, sayfa, grup ve yalnızca testte kullanılan oyun adaptörünü sınar. Native görsel istemci, Bedrock/ViaVersion ve başka lobi eklentileriyle uyumluluk doğrulanmadı.

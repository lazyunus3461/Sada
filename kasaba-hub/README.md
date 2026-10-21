# KasabaHub 1.1.0

Paper **1.21.10 / Java 21** üzerinde arkadaşlarla oynanan Minecraft oyun merkezi için ana eklenti. Ortak lobi, Minecraft'ın **yerleşik sayfa arayüzü**, geçici arkadaş grupları ve sonraki oyun eklentileri için bağlantı sağlar. Bu sürümde mini oyun kuralları bulunmaz. Yönetici oyun giriş noktalarını `/oyunlar ekle` ile kaydedebilir; boş oyun listesi **Oyunlar yakında** gösterir.

## Kurulum

[KasabaHub kurulum ZIP'ini indir](https://github.com/lazyunus3461/Sada/raw/refs/heads/main/dist/KasabaHub-Kurulum.zip)

1. Sunucuyu durdur. Mevcut eklenti veri klasörlerini yedekle. Güncellemede eski KasabaHub JAR'ını `plugins` dışına çıkar; `plugins/KasabaHub` klasörünü koru.
2. Eski KasabaAuth JAR'ını `plugins` dışına taşı. Yeni modelde kullanılmayacak KasabaKlan/KasabaWarp ve başka lobi yöneticilerinin JAR'larını da dışarı taşı. **Veri klasörlerini silme.**
3. ZIP'teki **KasabaAuth-1.3.0.jar** ve **KasabaHub-1.1.0.jar** dosyalarını `plugins` içine koy. Mevcut KasabaRoles kalabilir; roller/renkler korunur. Test Probe dosyaları kurulmaz.
4. Sunucuyu aç, hesabına giriş yap. Güvenli lobi platformunda Kurucu/OP olarak **`/merkez lobi ayarla`** yaz.
5. `/lobi` ile dönüşü, `/merkez` ile ana sayfayı, `/oyunlar` ile oyun sayfasını dene.

KasabaAuth hesabı/şifresi aynı kalır. 1.3.0, doğrulama sonrası bir olay ve salt okunur durum servisi ekler. Hub bu olayı dinleyip oyuncuyu doğrulama **bittikten sonra** lobiye taşır; kayıt sayfasını atlamaz. Auth'ın kayıt yeri giriş sırasında kullanılmaya devam eder; aynı yerde olmasını istersen lobi platformunda `/kayityeri ayarla` da kullanabilirsin. Hub yoksa Auth'ın önceki kayıt yeri davranışı korunur.

## Oyuncu komutları

| Komut | İşlev |
| --- | --- |
| `/merkez` | Ana sayfa: oyunlar, arkadaş grubu, lobiye dönüş |
| `/oyunlar` | Kayıtlı oyun girişlerini ve bağlı oyun eklentilerini sayfalar |
| `/oyunlar git parkur` | Menü açmadan parkur girişine katılır |
| `/oyunlar yardım` | Oyun komutlarını gösterir |
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

## Oyun girişlerini komutla ekleme

Oyun haritasının güvenli girişinde dur ve Kurucu/OP olarak:

```text
/oyunlar ekle parkur Kasaba Parkur
/oyunlar konum parkur
/oyunlar sil parkur
```

`ekle` bulunduğun dünya, tam konum ve bakış yönünü kaydeder. Kimlik 1–32 küçük harf/rakam/alt çizgi/tire; görünen ad en çok 48 karakterdir ve boşluk içerebilir. Aynı kimlik üzerine yazılmaz. `konum` bulunduğun yeri yeni giriş yapar. `sil` yalnızca komutla eklenen girişi kaldırır; haritayı silmez. Kullanılan giriş değiştirilemez/silinemez; katılanlar önce `/lobi` ile ayrılır. Yetki, lobi yönetimiyle aynıdır. Normal oyuncu giriş ekleyemez.

Seçim tek oyuncuyu veya liderin bütün grubunu kayıtlı noktaya götürür. Bölge asenkron hazırlanır; çevrimiçi/giriş durumu ve grup rezervasyonu yeniden denetlenir. Başka eklenti bir üyenin ışınlanmasını iptal ederse önce taşınan üyeler eski konumlarına geri gönderilir ve giriş iptal edilir. Başka eklenti geri dönüşü de iptal ederse konsola oyuncu adıyla uyarı yazılır; eklentilerin teleport iptali aşılmaz. 10 saniyede hazırlanamazsa rezervasyon bırakılır. Beklerken `/lobi` veya çıkış yapmak eski isteğin sonradan grubu taşımasını engeller.

Bunlar **giriş noktalarıdır**; maç, skor, oyun kuralları ve ayrı arena örnekleri oluşturmaz. Oyun haritasını lobi korumasının dışında, tercihen ayrı bir dünyada kur. `plugins/KasabaHub/oyunlar.yml` atomik saklanır ve restartta korunur. Dünya yüklü değilse giriş **Bakımda** görünür. `/merkez yenile` dosyayı yeniden okur; bozuk veri çalışan listeyi sıfırlamaz. Gerçek bir oyun eklentisi aynı kimliği kullanacaksa önce komutla oluşturulan girişi sil, ardından oyun eklentisini başlat.

## Lobide menü pusulası

Girişini tamamlayan oyuncu ayarlı lobinin koruma alanındayken **Kasaba • Oyun Menüsü** pusulasını alır. Sağ tık, `/merkez` ile aynı yerleşik sayfayı açar. Öncelik hızlı çubuğun 9. yuvasıdır; doluysa başka boş envanter yuvası kullanılır. Envanter tamamen doluysa mevcut eşya değiştirilmez; oyuncu bir yuva açana kadar komut kullanabilir.

Pusula sadece ayarlı lobi alanında ve oyun dışında vardır. Oyun katılımında, alan/dünya değişiminde kaldırılır; `/lobi` ile dönünce geri verilir. Oyun eklentisine takım teslim edilmeden **önce** kaldırılır; oyun envanteri yedeğine karışmaz. Oyunda `/merkez`, `/oyunlar` ve `/grup` komutları çalışmaya devam eder; başka oyuna katılmak için önce `/lobi` gerekir.

Yalnızca eklentinin özel veri etiketi olan geçici pusula yönetilir; normal pusulalar ve diğer eşyalar korunur. Geçici pusula taşınamaz, atılamaz, başka ele veya sandığa aktarılamaz; ölümde düşmez ve çıkış/kapanışta temizlenir. Dışarı ışınlanma ve sınır geçişleri olaylarla, eski/kopya geçici eşyalar ayrıca saniyede bir kontrol edilir. Lobi henüz ayarlı değilse veya oyuncu giriş yapmadıysa verilmez.

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

Lobi noktası çevresinde varsayılan X/Z **100 blok uzaklığa kadar** kare koruma vardır; bu, eski warpın 100×100 claim yasağından farklı bir lobi ayarıdır. Burada normal oyuncu inşa/kova/blok etkileşimi yapamaz, eşya atamaz/toplayamaz; hasar, açlık, patlama/yangın ve sınırdan piston/sıvı müdahalesi engellenir. Düzenleme modu inşa ve temel etkileşimleri açar. Oyuncuların kendi eşyaları, oyun modu, rolü ve scoreboard'u korunur. Sadece Hub'ın etiketli geçici menü pusulası eklenir/kaldırılır. Oyun haritalarını ayrı dünyalarda kurmak önerilir. Lobide boşluğa düşme ve oyun dışında yeniden doğma lobiye dönüşü kullanır.

`plugins/KasabaHub/lobi.yml` dünya UUID'si, tam koordinat ve yönü atomik saklar. Lobi dünya spawnını veya başka eklentinin dosyasını değiştirmez. Yüklü olmayan dünya, dünya sınırı dışı konum ve başka eklentinin teleport iptali aşılmaz. Bölge asenkron hazırlanır; ayrılan oyuncu veya eski konum isteği sonradan ışınlanmaz.

`config.yml`: `grup-uye-siniri: 8`, `davet-suresi-saniye: 60`, `otomatik-sayfa: true`, `lobi-koruma-yaricapi: 100`. Girişten sonra büyük karşılama başlığını göstermek için ana sayfa lobiye varıştan iki saniye sonra açılır; bu sırada oyuncu kendi sayfasını açarsa üstüne yazılmaz. `/merkez yenile` çalışan grupları korur. Hatalı yenileme mevcut ayarı değiştirmez. İlk açılışta dosya bozuksa koruma normal dünyalarda da kapalı kalmak yerine inşa/etkileşimleri engeller; dosyayı düzeltip konsoldan `merkez yenile` kullan. Veriyi sıfırlamaz.

## Sonraki oyunları bağlama

`HubApi` Bukkit ServicesManager üzerinden alınır. Oyun eklentisi `depend: [KasabaHub]` kullanır ve ana iş parçacığında `api.register(this, adapter)` çağırır. `HubGame` değişmez `GameInfo`, kullanılabilirlik, takım kabulü ve çıkış kancası sağlar. Bütün grup tek çağrıyla ve değişmez oyuncu listesiyle teslim edilir. Giriş aralığı/oyuncu yetkileri/aktif oyun kontrolü tekrar yapılır. `join` takımı atomik kabul etmeli; `false` dönerken hiç kimsenin konumunu/envanterini değiştirmemelidir. Bu sözleşmenin harita ve envanter geri alma kısmından oyun eklentisi sorumludur.

Kabulden sonra merkez oyun durumunu izler. Oyun bitiminde `api.returnToLobby(playerUuid)` kullanılır. Eklenti kapanır veya kendi oyununu kaldırırsa katılımcılar lobiye yönlendirilir. Başka eklentinin oyunu kaldırılamaz; aynı kimlik iki kez kaydedilemez. Oyun eklentisi kendi arena, sıra/maç, skor, envanter geri alma ve kapanış temizliğini uygular. Merkez, olmayan maçı veya sahte oyuncu sayısını göstermez.

## Geliştirme ve test

Bulutta `bash kasaba-hub/scripts/cloud-build.sh`; Auth'ı yerel Maven'e kurar ve Hub'ı derler. Normal JDK 21/Maven ortamında repo kökünde `mvn install`, ardından `mvn -f kasaba-hub/pom.xml verify`. 17 Auth ve 17 Hub birim testi bulunur. [Yerel test](scripts/integration/README.md) kayıt, lobi, sayfa, grup ve yalnızca testte kullanılan oyun adaptörünü sınar. Native görsel istemci, Bedrock/ViaVersion ve başka lobi eklentileriyle uyumluluk doğrulanmadı.

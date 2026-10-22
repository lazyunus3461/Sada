# KasabaAdalar 1.3.2

Paper 1.21.10, Java 21. Kullanıcının gönderdiği **KasabaAdalar 1.2.0 JAR'ı ve ada.zip** incelenerek güncellendi. `KasabaAdalar` eklenti adı, ana sınıfı, izinler ve eski sahiplik dosyası biçimi korunur. Oyun merkezi bağlantısı için KasabaHub 1.2.0 önerilir; bu sürüm bilgi kitabını da ekler.

[Kurulum paketini indir](https://github.com/lazyunus3461/Sada/raw/refs/heads/main/dist/KasabaAdalar-Kurulum.zip) · [Ada JAR'ı](https://github.com/lazyunus3461/Sada/raw/refs/heads/main/dist/KasabaAdalar-1.3.2.jar) · [Dünya ZIP'i](https://github.com/lazyunus3461/Sada/raw/refs/heads/main/dist/KasabaAdalar-Dunya.zip)

## 1.3.0 / 1.3.1 kullananlar için küçük güncelleme

Sunucuyu kapat; `plugins` içindeki eski **KasabaAdalar JAR dosyasını** çıkarıp **KasabaAdalar-1.3.2.jar** koy ve sunucuyu yeniden aç. Mevcut `ada` dünyasını, `plugins/KasabaAdalar` veri klasörünü ve KasabaHub 1.2.0'ı koru. **Bu düzeltmede haritayı yeniden kurmak gerekmez.**

İlk `/ada`, `/ada git` veya `/ada al` kullanımında yaratıcı mod ve ada envanteri ışınlanmadan önce hazırlanır. Böylece oyuncunun ilk dünya giriş paketinde de yaratıcı mod bulunur; ikinci kez komut yazmak gerekmez. Başarısız/engellenen ışınlanmada normal envanter ve önceki mod geri yüklenir. Varışta yaratıcı mod ve uçuş izni yeniden doğrulanır.

Pusula → Oyunlar → Adanı Yap ile giriş, `/ada` ile aynı envanter/yaratıcı mod oturumunu açar. Dünya değişiminden sonra başka bir eklentinin uyguladığı survival/adventure modu ada oturumunu bozamaz. `/lobi` başlatılırken bu kontrol bırakılır; önceki mod ve normal envanter geri yüklenir. Giriş yapmamış hesaplara veya diğer oyunlara yaratıcı mod verilmez.

Ada dünyası sürekli **gündüz, öğlen (6000)** ve açık havadır. Gün/gece ve hava döngüleri kapalıdır; gece, yağmur ve fırtına değişimleri yalnızca ada dünyasında engellenir. Diğer dünyaların saat/hava kuralları değiştirilmez.

## Değişenler

- Oyuncu başına yalnızca **bir ada**. Tekrar `/ada al` aynı adayı açar; ikinci ada vermez.
- Gönderilen dünyadaki 300 ada yüzeyi korunarak merkez aralığı **200 → 100 blok** yapılır. Düzen 20 sütun, 15 satırdır. Ada sahiplik numaraları değişmez.
- Adaların altı ve okyanusun tabanı taban kayasına kadar doludur. Önceki dünya/üreticide bulunan 121 blokluk hava tabakası kaldırılır. Deniz tabanı Y=58, deniz yüzeyi Y=63, ada merkezlerinin çimen yüzeyi Y=65'tir. Yeni açılan okyanus parçaları da dolu tabanla üretilir.
- Adada yaratıcı mod açılır. Ada envanteri ve normal envanter ayrıdır; `/lobi` önceki oyun modunu ve normal eşyaları geri yükler.
- `/oyunlar` sayfasında **Adanı Yap** otomatik görünür. Adası olmayan oyuncu önce `/ada al` uyarısını görür; düğme otomatik ada satın almaz. Ada alındıktan sonra düğme adaya götürür.
- KasabaHub 1.2.0 ana lobide pusulanın yanına **bilgi kitabı** koyar. On sayfada arena, özel 1v1 kodları, X Arena, beceriler, karakterler, ada, arkadaşlar ve Akçe anlatılır. `/rehber` veya `/bilgi` kitabı her oyunda açar. Kitap geçici lobi eşyasıdır; oyuna girince kaldırılır.

## Kurulum / 1.2.0'dan güncelleme

Kullanıcı mevcut adalarda henüz yapı olmadığını doğruladı. Paket bu **boş başlangıç haritasının** yakınlaştırılmış halidir; sonradan yapılmış oyuncu yapılarının üzerinde harita değiştirmek için kullanılmamalıdır.

1. Sunucuyu tamamen kapat.
2. `plugins` içindeki eski KasabaAdalar ve KasabaHub **JAR dosyalarını** dışarı çıkar. Aynı eklentinin iki sürümü birlikte kalmamalı.
3. `plugins/KasabaAdalar` ve `plugins/KasabaHub` **veri klasörlerini koru**. Özellikle `claims.properties` ada sahiplerini, `player-data` envanterleri tutar.
4. Sunucunun ana dizinindeki mevcut `ada` dünya klasörünün adını `ada_eski` yapıp yedekle. Dünyaları üst üste birleştirme; eski uzak adaların bölge dosyaları kalmamalı.
5. Paketteki yeni **`ada` klasörünü** sunucunun ana dizinine koy; `plugins` içine koyma. `ada/level.dat` ve `ada/region` bulunmalı.
6. Paketin `plugins` klasöründeki **KasabaAdalar-1.3.2.jar** ve **KasabaHub-1.2.0.jar** dosyalarını sunucunun `plugins` klasörüne koy.
7. KasabaAuth, mevcut KasabaArena ve KasabaAkce eklentilerini koru. Sunucuyu aç; `/ada bilgi` aralığı **100 blok** göstermeli.

Dünyada `kasaba-layout.properties` yerleşim işareti vardır. İşaretsiz eski dünyalar yapıları yanlış konuma taşımamak için 200 blok düzeniyle tanınır. Sadece JAR değiştirmek eski bölge dosyalarını yeniden konumlandırmaz; yakınlaştırma ve eski boş tabanı düzeltme için paket dünyası da kurulmalıdır.

`Adanı Yap` eklenti tarafından kaydedilir; `/oyunlar ekle` ile eklemek gerekmez. Daha önce aynı `adaniyap` kimliğiyle elle kayıt oluşturduysan `/oyunlar sil adaniyap` ile o girişi kaldırıp yeniden başlat.

## Oyuncu komutları

| Komut | İşlem |
|---|---|
| `/ada al` | İlk adanı al; tekrar kullanım yeni ada vermez |
| `/ada` veya `/ada git` | Kendi adana git |
| `/oyunlar git adaniyap` | Oyun merkezi üzerinden adana git |
| `/ada ekle Redkit` | Arkadaşını kendi adana ekle |
| `/ada Redkit` | Sahibinin izin verdiği adaya git |
| `/ada çıkar Redkit` | Arkadaşının iznini kaldır |
| `/ada bilgi` | Ada numarası, tek ada sınırı ve mesafe |
| `/ada yardım` | Komut listesi |
| `/rehber` | Oyunları ve komutları anlatan kitap |
| `/lobi` | Ana lobiye dön; normal envanterini/modunu geri al |

İzinler eski sürümle aynıdır: `kasabatr.ada.use` herkese, `kasabatr.ada.admin` varsayılan OP. Ada izinleri yatay 96×96 alanı kapsar. Misafir, ancak sahibinin eklemesiyle girip yapı yapabilir. Oyuncunun başka adaya arkadaş olarak eklenmesi ikinci ada sahipliği vermez. Arkadaş çıkarılınca o adadaysa kendi adasına veya ana lobiye döner.

Başka bir oyun aktifken önce `/lobi`, sonra `/ada` kullanılır. Yaratıcı eşyaların Arena'ya taşınmaması için oyun merkezi bu geçişi denetler. Ada dünyasında ender sandığı ve oyuncunun eşya atması kapalıdır. Ada dışındaki envanter veya mod korunur; diğer dünyalarda yaratıcı moda zorlanmaz.

## Akçe ve sonradan market

Bu güncelleme ada edinirken para kesmez ve ada marketinde ürün/fiyat belirlemez. Akçe sunucu genelindeki ayrı servistir. Daha sonra Arena ve ada marketlerine ayrı ürün, süre, adet ve fiyat eklenebilir; bakiye altyapısı yeniden yazılmak zorunda değildir. X Arena marketindeki mevcut ürünlerin ayarları korunur.

## Kaynak ve doğrulama

Önce `mvn -f pom.xml install`, sonra `mvn -f kasaba-hub/pom.xml install` ile yerel API bağımlılıklarını kur. `mvn -f kasaba-adalar/pom.xml verify` sekiz sahiplik/yerleşim testini çalıştırır. Hub'ın 17 mevcut birim testi de korunur. `scripts/build-world.py --input <orijinal-ada> --output <yeni-boş-klasör>` NumPy ve nbtlib kullanır; yalnızca verilen boş başlangıç şablonu içindir, girdiyi değiştirmez. `scripts/validate-world.py <dünya>` tüm 11.970 parçanın alt tabanını ve 300 doğma yüzeyini bağımsız olarak doğrular.

Loopback Paper testleri: `scripts/integration/build-probe.sh`, `node scripts/integration/islands.cjs`; sunucuyu tamamen durdur/aç sonrası `node scripts/integration/restart.cjs`. `node scripts/integration/creative-mode.cjs` gerçek pusula/yerleşik sayfa tıklaması, yaratıcı modu iptal eden ve gecikmeli adventure uygulayan örnek dünya yöneticisi, sürekli gündüz/açık hava, önceki moda dönüş ve Arena izolasyonunu doğrular. Eski JAR ile `EXPECT_BROKEN=1` aynı yaratıcı mod hatasını yeniden üretir. `node scripts/integration/first-entry.cjs` gerçek Minecraft komut paketleriyle her giriş komutunu yalnızca bir kere gönderir; ilk dünya paketini, son istemci/sunucu modunu, yaratıcı envanteri ve uçuş iznini birlikte doğrular. İlk ada alımı, iptal edilen ışınlanmada envanter/mod geri alma ve hızlı çıkış da kapsanır. Eski 1.3.1 JAR ile `EXPECT_OLD_ENTRY=1` ilk dünya paketindeki survival değerini gösterir. Test yardımcıları üretim JAR'ı veya kurulum ZIP'ine alınmaz.

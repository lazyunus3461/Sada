# KasabaTR Şans Yolu 0.7.0

Paper **1.21.10 / Java 21**. Gönderilen 0.3.1 oyunundaki iki zar, 135 kare,
renk etkileri ve üç köprü korunarak KasabaTR merkezine uyarlanmıştır.
Gönderilen dosyalarda `.java` kaynakları yoktu; derlenmiş sınıflar incelenerek
bakımı yapılabilir kaynak kodu bu modülde yeniden oluşturuldu.

## Kurulum — sunucuya sen yükleyeceksin

1. Sunucuyu **durdur**, `plugins` ve dünya klasörlerini yedekle.
2. Tam sunucu ZIP'indeki `plugins` klasörünü mevcut sunucu `plugins` klasörüyle
   birleştir. Aynı eklentinin eski JAR'ını kaldır; veri klasörlerini silme.
   Eski `KasabaIspanya-0.3.1.jar` kalmamalı.
3. Paketteki güncel JAR’ları birlikte yükle. Bu sürüm
   **KasabaHub 1.5.0 ve KasabaAuth 1.3.0** ile birlikte test edildi.
4. Sunucuyu başlat. İlk kurulumda Şans Yolu dünyası JAR'dan otomatik açılır.
   Ayrı `.mca` veya `level.dat` taşıman ve `/mv import` yazman gerekmez.
5. Oyunda **`/oyunlar` → Şans Yolu** seç. Pusulaya sağ tıkla; **Oyna** veya
   **Oda oluştur** seç. Arkadaşın aynı menüden oda numarana basabilir.
   En az 2 kişi olunca 15 saniye geri sayar. Oda sahibi **Maçı başlat** ile
   beklemeden başlayabilir.

İlk girişte yalnız bir kişi olabilirsin. Her oda **2–4 kişi**; varsayılan
en fazla **3 eşzamanlı oda** vardır. Her maç kendi geçici dünya kopyasında
oynanır. Ana lobi/spawn, arena, adalar, BR ve Vampir Köylü dünyaları değiştirilmez.

Yeni kurulumdaki ortak dünya adı **`ispanya`**. Eski eklentide özel
`template-world` kayıtlıysa mevcut adı korunur; dolu mevcut dünyanın üstüne
yazılmaz. Yol ile dünya uyuşmazsa oyun açılmaz ve konsola nedeni yazılır.
Multiverse varsa ortak dünya kaydedilir; maç kopyaları Multiverse'e eklenmez.
`/mvtp ispanya` yönetici kontrolü içindir; oyuncular oyunlar pusulasını kullanır.

Önceden `/oyunlar ekle` ile aynı `ispanya` kimliğini elle eklediysen kayıt
çakışması konsolda bildirilir. O eski giriş noktasını **`/oyunlar sil ispanya`**
ile kaldırıp sunucuyu yeniden başlat; oyun kendi girişini kaydeder. Başka
oyunların kayıtlarını silme.

Menüde **Bu lobide kal** ile oda seçmeden bekleyebilirsin. Ayrıntılı oyun
bilgisi burada gösterilir; ana `/oyunlar` menüsünde uzun açıklama bulunmaz.

Yeni komut `/sansyolu`; aşağıdaki `/ispanya` komutlarının tamamı da çalışır.
Veri klasörü **KasabaIspanya**, dünya adı **ispanya** olarak korunur.

## Komutlar

Türkçe karakterli ve ASCII yazımlar kabul edilir: `oluştur/olustur`,
`katıl/katil`, `başlat/baslat`, `çık/cik`.

| Komut | Ne yapar? |
|---|---|
| `/ispanya` veya `/ispanya odalar` | Oyuna giriş ve pusulayla aynı oda sayfası. |
| `/ispanya oyna` | Uygun bekleyen odaya girer; yoksa yeni oda açar. |
| `/ispanya oluştur` | Yeni oda açar; odayı açan kişi sahibi olur. |
| `/ispanya katıl 101` | Örneğin 101 numaralı bekleyen odaya girer. |
| `/ispanya başlat` | Oda sahibi, en az 2 oyuncuyla maçı başlatır. |
| `/ispanya zar` | Sıra sendeyse iki zar atar; zar eşyasına sağ tık da aynıdır. |
| `/ispanya lobi` | Mevcut odanı bırakır, ortak Şans Yolu lobisine döner. |
| `/ispanya çık` veya `/lobi` | Şans Yolu oyunundan çıkar, normal eşya/modunu geri alır ve ana spawna döner. |
| `/ispanya yardım` | Renklerin ve maçın kısa açıklaması. |
| `/ispanya bilgi` | Yüklü dünya adı, kare ve oda sayısı. |
| `/ispanya bitir 101` | Yönetici 101 numaralı maçı ödülsüz durdurur. |
| `/ispanya lobi ayarla` | Yönetici ortak Şans Yolu dünyasında güvenli zeminde yeni lobi noktası belirler. |
| `/ispanya düzenleme aç` | Yönetici ortak Şans Yolu haritasında yapı düzenler. |
| `/ispanya düzenleme kapat` | Yapı korumasını geri açar. |

Yönetici yetkisi: `kasabatr.ispanya.admin`, varsayılan OP. Maç kopyasında
düzenleme açılamaz. Harita üzerinde değişiklik yapacaksan ortak dünyada yap;
sonraki maçlar bu dünyadan kopyalanır.

Grup için ana spawn/lobide `/grup oluştur`, `/grup davet Oyuncu`, davetli için
`/grup katıl Lider` kullan. Ardından **grup lideri `/oyunlar` üzerinden**
Şans Yolu'nı seçsin. Grup birlikte lobiye girer; odasını lider seçer.
Güvenli lobide arkadaş/görünürlük araçları çalışır; aktif maçta kaldırılır.

## Maç akışı

- Her oyuncu sırayla iki adet 1–6 zar atar. Toplam kadar otomatik ilerler;
  yol atlamak, hareket etmek, eşya bırakmak/toplamak ve hasar almak kapalıdır.
- Zar için varsayılan süre 25 saniye. Otomatik zar yoktur. Maç içinde üç kez
  zar atmazsan yalnız Şans Yolu lobisine çıkarılırsın.
- **Kırmızı:** rastgele sonraki tur 5 saniye zar atamadan bekleme veya 3 kare
  geri gitme. Geri hareketten önce iki saniyelik kötü şans uyarısı gösterilir.
- **Yeşil:** 3 kare ileri. **Mor:** rastgele 1–6 kare ileri/geri.
- **Sarı:** üç rastgele köprü kestirmesi. Sarıya gelince eline kestirme çubuğu
  verilir. Sağ tıkla veya `/sansyolu kestirme` kullan; çubuk geçişten sonra silinir.
- Renkli karelerin ve kestirmelerin konumları her maç yeniden karıştırılır.
- **Siyah:** etkisiz. Yalnız durulan son karenin etkisi uygulanır;
  ilerlerken üzerinden geçilen kareler ve etki sonrası varılan yeni renk zincir oluşturmaz.
- Bitişi aşan zar normalde bitişe ulaştırır. Tam sayı seçeneği config'den
  açılırsa fazla gelen zar yerinde bırakır.
- Biten oyuncu sıralamaya yazılır; diğerleri devam eder. Herkes bitince veya
  25 dakika dolunca sonuç açıklanır; 10 saniye sonra ortak Şans Yolu lobisine dönülür.
- Süre dolduğunda önce bitirenler, sonra ilerlediği kare ve puanı yüksek olanlar sıralanır.
- Oyuncu ayrılırsa sahibi gerekirse devredilir ve zar sırası düzeltilir.
  Diğer oyuncular devam eder. Son kişi çıkınca maç kopyası kaldırılır.
- Normal eşya, zırh, iksir, deneyim, yedek kalp, uçuş/mod ve konum için kalıcı
  kurtarma kaydı tutulur. Çıkışta geri verilir; yarım oturum, hesap girişi
  doğrulandıktan sonra kurtarılır. Okunamayan yedek silinmez.

`reward-commands` varsayılan boş: oyunun ayrıca Akçe ödülü tanımlı değildir.
Merkezin mevcut aktif oyun süresi ödülü uygulanır. İstenirse konsol ödülleri
`{player}`, `{rank}`, `{points}` ile ayrıca belirlenebilir. Yönetici durdurması,
oda hatası ve eklenti kapanışı ödül dağıtmaz.

Renk adımları gönderilen Minecraft uyarlamasının kurallarıdır; Sanalika'nın
doğrulanmış özgün adım sayıları olduğu iddia edilmez.

## Dosya düzeni ve doğrulama

`src/main/resources/map-template` içindeki dört region, `level.dat` ve
`arena.json` gönderilen dosyaların aynısıdır. `track.yml` ile 135 kare ve üç
kestirme birebir karşılaştırılır. Yeni parçaları boş üreten jeneratör hem
ortak dünyada hem maçlarda kullanılır. Yalnız eklentinin kendi işaretli
`isp_room_*` kopyaları temizlenebilir; diğer dünyalar silinmez.

Kaynak, birim testleri ve yalnız yerel test sunucusu için hazırlanan
entegrasyon yardımcıları bu modüldedir. Test yardımcı JAR'ları üretim paketine
konmaz. Gerçekleştirilen kontroller ve sınırları dağıtımdaki test kaydındadır.

## 0.6.0 ayrı lobi

İlk açılışta mevcut oyun yolundan ayrı, zar temalı bekleme meydanı boş alanda yapılır. Pusula ve lobi dönüşü buraya bağlanır; eski config önceden yedeklenir. Dünya sınırı lobiyi kapsar. Yeni meydan için dünya dosyası indirmen gerekmez. Menüler ortak renk düzenini kullanır.

## 0.7.0

Bekleme meydanının kenarları korkulukla kapalıdır; altına düşen oyuncu güvenli doğmaya geri alınır. Oda yeterli kişiye ulaşınca 15 saniyelik geri sayım yalnız o oda üyelerine üst çubukta görünür. Son kişi kaldığında kazanır ve üç saniye sonra Şans Yolu lobisine döner. Aktif maçı terk eden kişi yalnız bu oyuna 15 dakika giremez; ceza sunucu yeniden başlasa da devam eder. Bekleyen odadan ayrılmak ve normal bitiş cezasızdır. Güvenli pusulada **Oyun lobileri** bulunur; pusula envanterde tıklanınca da açılır.

## 0.8.0

Mevcut bilgi yazısı ortak görsel düzenle korunur ve yanına galibiyet ilk 5 tablosu gelir. Normal bitiş/son kalan oyuncu galibiyeti tek kez yazılır. Eski kalıcı yazılar yüklenince temizlenir; kopya yazılar birikmez.

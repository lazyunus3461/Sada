# Kasaba At ve Tekne Yarışı 1.2.0

Paper 1.21.10 / Java 21. KasabaHub 1.4.0 ve KasabaAuth gerektirir.
Gönderilen 1.0.0 yarış paketi sunucu altyapısına uyarlandı. Harita JAR içindedir;
ilk açılışta `KasabaTR-Yaris-Arenasi-v01` dünyası otomatik kurulur. Var olan
dünya ve ayarların üzerine yazılmaz.

Ana `/oyunlar` menüsünde **At Yarışı** ve **Tekne Yarışı** ayrı görünür. İkisi
aynı yarış lobisine götürür. Buradaki pusula veya `/yaris` ile iki oyun da
seçilir. **Bu lobide kal** menüyü kapatır, oyuncuyu sıraya/maça sokmaz.

- `/yaris` — seçim menüsü.
- `/yaris oluştur at` veya `/yaris oluştur tekne` — oda kur.
- `/yaris odalar` — bekleyen odaları gör.
- `/yaris katıl 101` — örnek numaralı odaya gir.
- `/yaris başlat` — oda sahibi, en az iki oyuncuyla başlatır.
- `/yaris ayrıl` — odadan ortak yarış lobisine dön.
- `/yaris çık` veya `/lobi` — ana spawna dön.

Oda başına 2–5 oyuncu, varsayılan iki tur ve sırayla geçilecek altı kontrol
noktası. İstenirse aynı anda sekiz oda açılabilir. Her başlayan maç kendi
harita kopyasında oynanır; at ve tekne maçları birbirini etkilemez. Atın ve
teknenin hızı oyuncular arasında eşittir. Erken bitiş ve kontrol noktası
atlama sayılmaz. Sonuçlar 12 saniye gösterildikten sonra yarış lobisine dönülür.

Hub grubu lideri oyun/oda seçtiğinde grup birlikte alınır. Güvenli lobi ile
oyun durumu ayrıdır; aktiflik kazancı yarış başladığında sayılır. Yarışta
hasar, blok düzenleme ve yarış sürerken binekten inmek engellenir. Normal
oyuncu eşyaları saklanır, ana lobiye çıkınca geri verilir. Bağlantısı kesilenin
yedeği kayıt/giriş tamamlandıktan sonra kurtarılır. Geçici maç dünyaları
Multiverse listesine eklenmez; ortak dünya Multiverse ile kaydedilir.

Kurulum: Sunucuyu durdur, eski KasabaYaris JAR'ını kaldır, yeni JAR'ı plugins'e
koy ve aç. Veri klasörlerini/dünyaları silme. Haritayı ayrıca çıkartman gerekmez.

## 1.2.0

Pusulada At/Tekne seçimi önce o türün açık odalarını gösterir. **Oda oluştur** ile bilerek oda açılır. At parkuruna çit, saman slalomu ve sığ su; tekne parkuruna geçilebilir adacıklar eklendi. Her maç ayrı kopyadır. Başlamış/geri sayımdaki yarışta son kişi kalırsa kazanır ve üç saniye sonra yarış lobisine döner. İlk girişteki otomatik menü, seçilen oda sayfasını yeniden açıp kapatmaz.

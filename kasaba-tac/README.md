# KasabaTR Lanetli Taç 1.1.0

Paper **1.21.10**, Java **21**, KasabaAuth **1.3.0**, KasabaHub **1.6.3**. Oyuncu modu ve kaynak paketi gerekmez. KasabaSosyal varsa arkadaş/grup araçları ve mevcut aktiflik Akçe sistemi çalışır. Multiverse-Core 5.3.2 isteğe bağlıdır.

## Yükleme

Sunucuyu tamamen durdur. ZIP'in `plugins` klasöründeki JAR'ları sunucunun `plugins` klasörüne koy; aynı eklentinin eski JAR'ını kaldır. **Eklenti veri klasörlerini, dünya klasörlerini, arena doğmalarını, rol/para kayıtlarını silme.** Yeniden başlat; `/reload` kullanma. Harita ve ayrı lobi JAR içinde gelir; eksik dünyalar otomatik kurulur. `/mv import`, `/oyunlar ekle` veya konum belirleme gerekmez. Mevcut harita üzerine yazılmaz.

Bu paket yeni oyunla birlikte istenen Eşya Saklambacı uçuş/hizalama ve dar bilgi tablosu güncellemelerini içerir. Eski KasabaHub, KasabaEsya, KasabaBilgi, KasabaTac JAR'larını yenileriyle değiştir. Roles 1.3.2 aynı sürümdür, önceki rol verileri korunur.

## Oyun

- `/oyunlar` → **Lanetli Taç**, ya da `/tac`: ayrı, kapalı ve güvenli oyun lobisi. **Oyna** seçmeden sıraya girilmez; **Bu lobide kal** sırayı iptal eder.
- Normal maç **6–16 kişi**, başlama sayımı **20 saniye**, maç **4 dakika**. Sayım yalnız sıradaki oyuncularda görünür.
- En fazla **3 eşzamanlı maç**; her oda şatonun ayrı, geçici kopyasındadır. Dördüncü maç yer açılmasını bekler.
- Taç rastgele başlar; altın miğfer ve parıltı sahibini gösterir. Tacı taşıyan saniyede **1 puan** kazanır. Taç sahibine **1,5 bloktan yakın** ve arada görüş varken yaklaşarak tacı alırsın. Devir sonrası **2,5 saniye** yeniden kapılamaz. Can hasarı yoktur.
- Kuzey ve doğu kapıları **30 saniyede** desen değiştirir. Oyuncunun üstüne kapanmaz; diğer yollar açık kalır. Şablon dünya değişmez.
- Süre bitince en çok puanı olan kazanır; puan eşitliğinde aynı en yüksek puanlı oyuncular ortak kazanır. Sıfır puanlı sonuç galibiyet üretmez. Herkes çıkarsa son kalan kazanır. Sonuçtan **5 saniye** sonra bu oyunun lobisine dönülür; otomatik yeniden sıra yoktur.
- Lobide oyunun açıklaması ve **ilk 5 galibiyet** tablosu vardır. Sonuç mevcut Hub liderlik kaydına bir kez yazılır. Yönetici denemesi galibiyet veya çıkış cezası üretmez.
- Başlamış normal maçı bırakmak bu oyuna **15 dakika** giriş cezası verir; diğer oyunları engellemez. Bekleme/sonuç/deneme/zorunlu çıkış ve sunucu kapanması bu cezayı üretmez.
- Grup birlikte girer, yalnız lider tüm grubu sıraya alır/çıkarır. Grup ayrı odalara bölünmez. Bu bireysel bir oyundur; arkadaşların da rakibindir.
- Pusula sağ tıkla veya envanterde tıklayarak açılır. Lobiden başka oyun lobisine doğrudan geçilebilir. Envanter ve oyuncu durumu yedeklenir; çıkışta geri verilir. Kopan bağlantıda kurtarma hesap girişinden sonra yapılır.
- Aktiflik Akçesi mevcut sunucu kuralına göre birikir; lobi ve AFK süresi sayılmaz. Ek maç başı para basılmaz.

## Komutlar

| Komut | İşlev |
|---|---|
| `/tac` | Taç lobisi / pusula menüsü |
| `/tac oyna` | Tek başına veya grubun lideri olarak sıraya gir |
| `/tac durum` | Sıra ve odaları gör |
| `/tac lobi` | Taç lobisine dön; normal maçtan çıkışta ceza uygulanır |
| `/tac çık`, `/lobi` | Ana spawna dön |
| `/tac yardım` | Kurallar |
| `/tac başlat` | Yönetici: en az 6 sıradaki oyuncuyla hemen başlat |
| `/tac deneme` | Yönetici: sırada tam 2 kişiyle deneme maçı |
| `/tac testharita` | Yönetici/konsol: oda yokken kopyada 16 doğmayı doğrula |
| `/tac düzenleme aç`, `/tac düzenleme kapat` | Yönetici: oyun dışındayken ortak dünyada düzenleme izni |
| `/oyunceza kaldır Oyuncu lanetlitac` | Yönetici: bu oyunun çıkış cezasını kaldır |

`/taç`, `/lanetlitac` ve Türkçe karaktersiz alt komut yazımları kabul edilir. Yönetici `kasaba.tac.admin` (OP), oynama `kasaba.tac.play` (herkes).

**İki kişiyle test:** İkiniz de `/tac`, sonra `/tac oyna` yazın. Sıradaki yönetici `/tac deneme` yazsın. Normal maçın altı kişi sınırı değişmez.

## Dünya ve ayarlar

- `KasabaTR-Tac-Lobi-v01`: şatodan ayrı bir boşluk dünyasında kalıcı bekleme salonu. İlk kurulumda inşa edilir; işaretli mevcut lobi yeniden inşa edilmez, düzenlemeler korunur.
- `KasabaTR-Lanetli-Tac-Satosu-v01`: yüklenen şato, maçların kalıcı şablonu.
- `tac_room_...`: yalnız eklentinin işaretlediği özel geçici maç dünyaları. Multiverse'e kaydedilmez. Oyuncu kalmadığında kapanır; sadece `.kasabatac-instance` işaretli kopyalar temizlenir.

`plugins/KasabaTac/config.yml`: `world`, `min-players: 6`, `max-players: 16`, `auto-start-seconds: 20`, `max-concurrent-rooms: 3`, `match-seconds: 240`, `gate-seconds: 30`, `tag-cooldown-millis: 2500`. Yüklenen 1.0.0 config anahtarları korunur. Gerçek doğmalar/kapılar `crown-map.json` üzerinden okunur; eski kullanılmayan `arena-spawns/lobby` alanları yeni ayrı lobiyi taşımaz.

Yüklenen haritadaki dört palet girdisinde eski `minecraft:chain` adı, Paper 1.21.10 için `minecraft:iron_chain` olarak düzeltildi. Diğer bloklar ve yapılar korunur. Orijinal belgeler `docs/` altında arşivdir; bu README güncel davranışı anlatır.

## Doğrulama

`validation/1.1.0` birim testi, oyuncu protokolü, normal maç/deneme, üç oda, taç/kapı/puan/bitiş, grup, koruma, envanter kurtarma ve yeniden başlatma kayıtlarını içerir. Süreli geçişler yalnız test adaptöründe hızlandırılır; dağıtılan ayarlar normal sürelerdir. Grafik istemcisinde görsel inceleme ve herkese açık yük testi yapılmamıştır. `scripts/integration` içindeki probe sadece loopback test içindir; ZIP'e konmaz.

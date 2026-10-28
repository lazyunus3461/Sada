# KasabaTR • Skyblock, Parkur ve Değişen Labirent güncellemesi

Paper **1.21.10**, **Java 21** ve mevcut KasabaTR ağı içindir. Canlı sunucuya kurulmadı. KasabaAuth ve KasabaAkce kurulu kalmalıdır.

1. Sunucuyu tamamen kapat; `plugins`, `kasabatr_skyblock`, `kasaba_parkur` ve mevcut diğer dünyaları yedekle.
2. Yalnız eski **KasabaHub**, **KasabaRoles**, **KasabaEvcil**, **KasabaSkyblock**, **KasabaParkur** JAR dosyalarını kaldır; **eklenti veri klasörlerini ve dünya klasörlerini silme**.
3. Paketteki altı JAR'ı `plugins` içine koy: **Hub 1.12.0**, **Roles 1.6.1**, **Evcil 1.2.0**, **Skyblock 1.1.0**, **Parkur 1.2.0**, **Labirent 1.0.0**. Aynı eklentinin iki sürümünü birlikte tutma.
4. Başlat; konsolda Paper `Done` satırını bekle. İlk harita kurulumu zaman alabilir. `/reload` kullanma.
5. `/oyunlar`, `/parkur kolay`, `/labirent` ve `/warp sb lobi` komutlarıyla kontrol et.

## Parkur

Kolay, orta ve zor yine **100'er bölüm**. Orman, maden ve volkan temaları korunur. Kıvrılan ve ayrılan yollar, kavisli sıçramalar, farklı çatı geçişleri, buz yüzeyleri, adacıklar, sarmal basamaklar, kule tırmanışları, bal ve slime bölümleri bulunur. Üç rotada sırasıyla **52, 50 ve 53 dönüş** var. Bölümler yalnız adlarıyla ayrılmaz; boyutları, yana açılmaları, yükselmeleri ve geçişleri değişir.

- Sağ panelde **bulunduğun zorluğun en ilerideki beş oyuncusu** canlı gösterilir. Her saniye yenilenir. Henüz beş kişi yoksa boş yerler `—` görünür.
- `/parkur ilk5` ve `/parkur ilerleme`: üç zorluğun canlı ilerleme listeleri. Menüde **Yenile** var.
- `/parkur süreler` ve `/parkur sıralama`: en hızlı **tamamlanmış** süreler; canlı ilerlemeden ayrıdır.
- Lobide üç canlı ilerleme panosu ve üç süre rekoru panosu bulunur.
- Deneme, yönetici düzenlemesi ve dışarıdan ışınlanarak yardım alınan koşular sıralamaya girmez.
- `/parkur dön`: başa dön ve kronometreyi sıfırla. Düşünce de rotanın başına dönersin. Ara kayıt veya her taşı sırayla basma zorunluluğu eklenmedi.
- Yeni harita **`kasaba_parkur_v12`** adıyla kurulur. Eski `kasaba_parkur` dünyası korunur. Eski yapılandırma `plugins/KasabaParkur/config-before-parkur-1.2.yml` olarak yedeklenir.
- Yeni rotaların süreleri **`sure-rekorlari-v12.json`** dosyasındadır. Eski süre dosyası silinmez; farklı haritaların süreleri birbiriyle karşılaştırılmaz.

## Değişen Labirent

`/labirent` veya Oyunlar menüsü ile gir. Pusula menüsünde Türkçe **Nasıl oynanır?** rehberi bulunur.

1. Ametist zeminler ve mor parçacıklarla işaretli **üç mührü** bul. Her oyuncu kendi mühürlerini toplar.
2. Hepsini alınca **yeşil çıkışa** ulaş. İlk bitiren kazanır.
3. Duvarlar varsayılan **45 saniyede bir** değişir; değişimden **5 saniye önce uyarı** gelir. Mühürler ve çıkış aynı yerde kalır. Toplanan mühürler kaybolmaz.
4. Her yeni yol düzeni bağlantılıdır. Değişimde oyuncunun gövdesinin kapladığı yer ve geri çekilebileceği yakın alan açık tutulur.
5. Varsayılan süre **5 dakika**. Süre dolarsa kazanan yazılmaz. Sonuçtan altı saniye sonra oyun lobisine dönülür.

Komutlar:

| Komut | İşlev |
| --- | --- |
| `/labirent oyna` | 2–8 kişilik ortak oda kuyruğu |
| `/labirent tek` | Hemen başlayan tek kişilik deneme; galibiyet ödülü yok |
| `/labirent odalar` | Oda listesi |
| `/labirent iptal` | Bekleme sırasından çık |
| `/labirent oda kur` | Mevcut VIP hakkıyla özel oda |
| `/labirent baslat` | Kurduğun özel odayı başlat |
| `/labirent yardim` | İlk adımlar ve kurallar |
| `/labirent lobi` | Maçtan çıkıp labirent lobisine dön |
| `/lobi` | Ağın ana lobisine dön; eşyaların geri yüklenir |

Ortak odalar mevcut Hub oda sistemiyle yeterli oyuncu geldiğinde geri sayar. Partiyle giriş birlikte alınır. Maç dünyaları birbirinden ayrıdır; bir odadan çıkmak başka oyuncunun maçını silmez. PvP, blok kırma/koyma, eşya atma/toplama ve açlık kapalıdır. Uçuş veya dış müdahale alan deneme galibiyet kazanamaz. Orijinal envanter, XP, konum ve oyuncu durumu yedeklenip çıkışta geri verilir; bağlantı kopmasında sonraki girişte kurtarılır.

Ayarlar `plugins/KasabaLabirent/config.yml`: `maze-size` 7–17 arasında tek sayı, `round-seconds` 60–1200, `change-seconds` 15–120, `maximum-rooms` 1–8. Varsayılan boyut 13×13 hücre ve üç eşzamanlı odadır. Ayar değişikliği için tam yeniden başlatma kullan.

## Skyblock ve diğer düzeltmeler

Ayrıntılar **SKYBLOCK-KURULUM.md** içindedir: ada içinde çalışan market, miktar seçip onaylanan alışveriş, otomatik toplama/emanet, %75 doğal ağaç devrilmesi, çağrılıp gizlenebilen Usta Poyraz, hazır su/lav jeneratörü, sade görev defteri, Altın/Mühür/Kredi paneli, dağlarla çevrili kasaba ve NPC warp noktaları, zararsız kâbus evcili, altın/fildişi Kurucu renkleri, OP'nin Skyblock düzenlemesi ve dünya genelinde 1–10 uçuş hızı ayarı.

## Doğrulamanın kapsamı

Paket gerçek yerel Paper sunucusu ve kimliği doğrulanmış sentetik Minecraft protokol istemcileriyle sınanmıştır. Harita dosyalarındaki destek, boşluk ve sıradan atlayış ölçüleri ayrıca denetlenir. Bütün 300 bölümün insanla oynanıp bitirildiği veya yüksek oyuncu sayısıyla yük testi yapıldığı iddia edilmez. Harita önizlemeleri gerçek bloklardan üretilen görsellerdir; Minecraft istemci ekran görüntüsü değildir.

Kullanım hakkının ne zaman biteceğine erişim veya sohbeti kendiliğinden yeniden başlatacak zamanlama aracı bulunmadığından otomatik `devam et` mesajı zamanlanmadı.

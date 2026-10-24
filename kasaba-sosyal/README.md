# KasabaTR sunucu güncellemesi

Paper **1.21.10 / Java 21** için hazırlanmıştır. Bu paket KasabaHub 1.3.1, KasabaRoles 1.3.1, KasabaAkçe 1.1.0, KasabaArena 1.3.1, KasabaAdalar 1.4.0, Battle Royale 1.1.1, Vampir Köylü 1.3.1 ve yeni KasabaSosyal 1.0.1 eklentilerini birlikte kullanır. KasabaAuth 1.3.0 da pakettedir.

## Kurulum

1. Sunucuyu tamamen **durdur**. `plugins` klasörünün ve dünyalarının yedeğini al.
2. `plugins` içindeki eski **KasabaAuth, KasabaHub, KasabaRoles, KasabaAkce, KasabaArena, KasabaAdalar, BattleRoyalePrototype/Battle Royale ve KasabaVampirKoylu** JAR'larını kaldır. Aynı eklentinin iki JAR'ını birlikte bırakma. **Eklentilerin veri klasörlerini, Akçe hesaplarını, sahiplik dosyalarını ve dünya klasörlerini silme.**
3. ZIP içindeki `plugins` klasörünü sunucunun ana dizinindeki mevcut `plugins` ile birleştir. Sonuç `plugins/KasabaSosyal-1.0.1.jar` gibi olmalı; `plugins/plugins` oluşturma.
4. Sunucuyu başlat. Eski arsa sahiplikleri, Akçe bakiyeleri, roller, arena noktaları, sandık adayları ve lobi ayarları korunur. Yeni davranışlar koddan eklenir; config dosyalarını elle değiştirmen gerekmez.

Bu paket mevcut sunucunun güncellemesidir. Mevcut `ada`, arena ve `KasabaBR` dünyaları kullanılmaya devam eder; yeni dünya indirmen gerekmez. Vampir Köylü hazır haritasını kendi JAR'ında taşır. Parkur, Warp ve diğer eklentilerini kaldırma.

## Spawn ve güvenli lobiler

- Yeşil/gri boya: bir sağ tıkla oyuncuları gizle, tekrar sağ tıkla göster. Başlangıçta 7. yuvayı kullanır; doluysa boş yuvaya yerleşir. Tercih yeniden girişte korunur.
- Oyuncu kafası: arkadaşlar ve grup menüsü. Başlangıçta 6. yuvayı kullanır.
- Defter ve oyun pusulası devam eder. Geçici menü eşyaları normal eşyaların üzerine yazılmaz; dolu envanterde komutları kullanabilirsin.
- Görünürlük, arkadaş ve grup araçları ana spawn, Arena güvenli lobisi/bekleyen oda, BR lobisi/kuyruğu ve Vampir Köylü bekleme lobilerinde çalışır. Parkur gibi elle eklenen giriş noktalarında girişin 16 blok çevresi lobi kabul edilir.
- Maçta veya yaratıcı adada bu araçlar kaldırılır. Gizlenen oyuncular oyuna girince tekrar görünür; savaş sırasında rakip gizleme yoktur.
- `/oyunlar` düğmelerinin sonundaki **Katıl** kaldırıldı. Arena pusulasındaki **Market** yerine **Lobi** vardır; ana spawna döndürür. X Arena eşya marketinin mevcut yönetim komutları korunur.

## Arkadaş ve grup komutları

| Komut | İşlev |
|---|---|
| `/arkadaş` | Çevrimiçi/çevrimdışı arkadaşlar, gelen istekler ve grup düğmeleri. |
| `/arkadaş ekle Ali` | Karşılıklı onay gerektiren arkadaşlık isteği. Çevrimdışı, daha önce giriş yapmış oyuncuya da istek bırakılabilir; istek 7 gün geçerlidir. |
| `/arkadaş kabul Ali` | Gelen isteği kabul eder. |
| `/arkadaş sil Ali` | Arkadaşlığı iki taraftan kaldırır. |
| `/mesaj Ali Merhaba` | Çevrimiçi arkadaşına özel mesaj. İkiniz de spawn/lobide olmalısınız. |
| `/grup` | Mevcut grup sayfası. |
| `/grup oluştur` | Arkadaş grubu kurar. |
| `/grup davet Ali` | Çevrimiçi oyuncuyu gruba davet eder. |
| `/grup katıl Lider` | Daveti kabul eder. |
| `/grup sohbet Merhaba` | Lobideki grup üyelerine mesaj gönderir. |
| `/grup ayrıl` | Gruptan çıkar. |
| `/grup dağıt onayla` | Lider grubu dağıtır. |
| `/görünürlük` | Boya ile aynı gizle/göster düğmesi. |

Arkadaşlıklar kalıcıdır; oyun grupları mevcut oturum sistemiyle çalışır. Grup lideri `/oyunlar` üzerinden seçim yapınca grup birlikte giriş yapar. Güvenli oyun lobisinde de grup kurup birlikte yeni giriş yapabilirsin; aktif maçtaki üyeler önce `/lobi` ile çıkmalıdır. Çevrimdışı oyuncuya sohbet teslim edilmez; durum listede gösterilir. Mesajlar sohbete genel olarak yayınlanmaz ve dosyaya kaydedilmez.

## VIP ve market

Herkes **`/vip`** yazarak ayrıcalık sayfasını görebilir. VIP oyuncuya kalan süre de gösterilir. Ana spawn pusulasındaki **Market** veya **`/market`**, süre ve satın alma onayı sayfasını açar.

| VIP süresi | Fiyat |
|---|---:|
| 3 gün | 40 Akçe |
| 7 gün | 75 Akçe |
| 15 gün | 145 Akçe |
| 30 gün | 250 Akçe |

Süre gerçek zamanla işler, çevrimdışıyken de azalır. Yenileme varsa mevcut bitişin sonuna eklenir. Onaylanmış ödeme ve VIP bitişi kalıcı kaydedilir; yeniden başlatma aynı satın almayı ikinci kez ücretlendirmez veya süreyi ikinci kez eklemez. VIP normal oyuncunun etiketini ve adını altın rengine çevirir. Kurucu, Admin, Moderatör ve Rehber adları da kendi rol rengindedir. VIP alan bir görevli kendi görevli etiketini korur; VIP ayrıcalıkları ayrıca uygulanır.

### VIP ayrıcalıkları

- **Adanı Yap:** en fazla 2 ücretsiz arsa. Normal oyuncu en fazla 1 arsayı 50 Akçeye alır. VIP bittikten sonra mevcut ikinci arsa silinmez; fazladan arsa satın alınamaz.
- **Arena:** X Arena, 1v1 ve 2v2'de netherit **Korunma III** zırh. Maç silahları normal kittendir; ayrı VIP kit seçme sekmesi yoktur. X Arena becerilerinin her birinin beklemesi 2 saniye azalır; en az 1 saniye kalır. Kısa ortak kullanım animasyonu ve diğer mevcut kurallar korunur. X Arena'nın özel hasar hesabında VIP zırhı ek azaltma uygular.
- **Vampir Köylü:** oyun lobisinde `/vk viprol` veya bekleyen odadaki **VIP rol tercihim** ile gizli tercihini seç. Rol sayısı değişmez. Aynı rolü isteyen VIP'ler arasında kura yapılır; kontenjan kazanamayan oyuncu kalan rollere dağıtılır. Oda kurucusunun kapattığı rol verilmez. Tercih yalnız sahibine gösterilir.
- **Fısıltı:** `/fısla Ali Merhaba` veya `/fısla 123 Merhaba`. Ad ya da çevrimiçi kimlik kullanılır; kimlik arkadaş listesindeki çevrimiçi kayıtta gösterilir ve yeniden girişte değişebilir. VIP fısıltısı oyun sırasında da kullanılabilir; alıcının VIP olması gerekmez.
- **BR:** 3 sarı yedek kalple başlar. Tam 36 yuvalı eşya envanteri ve alt el açıktır. Elitra verilmez; başlangıç göğüs zırhı korunur.

Yönetici tarafından `/rol ver Ali vip` ile atanmış kalıcı rol de VIP sayılır. Satın alınmış süreli VIP ayrı dosyada saklanır; satın alma görevli rolünü silmez. OP olmak tek başına VIP sayılmaz.

## Arsalar ve 15 günlük süre

- `/ada al` satın alma/onay ekranını açar. Fiyat ve **15 gün giriş yapılmazsa sıfırlama** uyarısı onaydan önce gösterilir.
- `/ada` veya `/ada git 1` ilk arsana; `/ada git 2` ikinci arsana götürür. `/oyunlar` → **Adanı Yap** ilk arsana gider.
- `/ada bilgi` arsalarını, sınırını ve sıfırlama uyarısını gösterir.
- İlgili kendi arsanda durup `/ada ekle Ali` veya `/ada çıkar Ali` kullanarak o arsanın izinlerini düzenlersin. `/ada Redkit 2`, izin verilen ikinci arsaya ziyaret içindir.
- Ada dünyasında yaratıcı mod ve sürekli gündüz devam eder. Yaratıcı ada envanteri diğer oyunlara taşınmaz.
- **Sahibi 15 gün sunucuya giriş yapmazsa bütün arsaları temizlenir ve tekrar satışa çıkar.** Sadece ada ziyareti gerekmez; sunucuda başarılı hesap girişi süreyi yeniler. Arkadaşının ziyareti sahibinin süresini yenilemez.
- Önceki sürümde son giriş kaydı olmayan mevcut sahiplere, güncellemenin ilk açılışından itibaren 15 günlük başlangıç süresi verilir. Eski sahiplik dosyası `claims.properties.before-vip-inactivity` olarak yedeklenir.
- Süresi dolmuş arsa temizlik sırasında kapalı ve satış dışıdır. Yapılar, sandıklar/içerikleri ve varlıklar kaldırılır; ada temiz başlangıç haline ve dolu alt zemine döner. Kalıcı kaydı tamamlanınca yeni alıcıya açılır. Yarım temizlik yeniden başlatmada devam eder. Temizliğe alınmış arsaya sonradan giriş yapılması başlamış sıfırlamayı iptal etmez.
- Ada dünyasında portal, TNT, redstone makineleri, piston/huni/gözlemci/dağıtıcı, üretici, spawn yumurtaları ve benzeri yoğun işlem oluşturabilecek araçlar kapalıdır. Varlık üretimi ve dekor varlıkları da sınırlandırılır.
- **Redstone Blokları / Çağırma Yumurtaları:** sekme başlıklarını Minecraft istemcisi gösterir; sunucu bunları gizleyemez. Redstone mekanizmaları ve bütün çağırma yumurtaları yaratıcı menüden alınamaz; arama, kayıtlı araç çubuğu, sandık, sürükleme veya yerden alma yoluyla da kullanılamaz. Önceden envantere alınan yasaklı eşyalar ada içinde kaldırılır. Normal yapı blokları, ahşap kapılar, normal sandıklar ve dekorasyon kullanılabilir.

## Akçe kazanma ve spawn balıkçılığı

Aktif oyun süresinin her **15 dakikası +0,20 Akçe** kazandırır. İlerleme çıkışta korunur. Spawn, güvenli lobiler, seyirci modu, çevrimdışı süre ve 2 dakikadan uzun etkileşimsiz bekleme sayılmaz. Hareket, yapı kurma/kırma ve gerçek savaş etkileşimleri aktiflik kaydını yeniler.

- **`/olta`:** spawn alanında ücretsiz olta verir. Aynı oyuncu en erken **1 saat** sonra tekrar alabilir. Olta yalnız sahibinin spawn balıkçılığında çalışır.
- **`/sat balık`:** kendi oltanla tuttuğun balıkları tanesi **0,05 Akçeye** satar. Başkasından gelen ya da yaratıcı envanterden alınan balık satılamaz. Kayıtlı balığın kopyası ikinci bir satış hakkı oluşturmaz.
- Günlük hak **15 adet**. Balıkları parça parça satabilirsin; ancak satışta ücretsiz olta **0 Akçeye geri alınır**, tekrar olta almak için kendi 1 saatlik sürenin dolması gerekir. İstersen 15 balığı biriktirip tek seferde sat.
- **15. balığın satıldığı anda 24 saat sayacı başlar.** Bu sürede yeni balık tutulamaz/satılamaz; envantere gelen balıklar otomatik kaldırılır. Sayaç gece yarısında sıfırlanmaz; 24 saat tamamlanmalıdır. Çıkış ve yeniden başlatma süreyi değiştirmez.
- 15 balığın toplam getirisi **0,75 Akçe**. Satış ve aktiflik ödemelerinde kalıcı işlem kimliği kullanılır; yeniden başlatma aynı ödülü tekrar kazandırmaz.

Mevcut `/akçe`, `/akçe gönder Ali 5`, `/akçe bak Ali`, `/akçe sıralama` ve yönetici para komutları korunur.

## BR envanteri, ganimet ve yedek kalp

- Normal oyuncunun eşya taşıma alanı **yalnız 9 hızlı erişim yuvasıdır**. Üstteki 27 eşya yuvası, 2×2 üretim alanı ve alt el kapalıdır; zırh yuvaları kullanılabilir. VIP tam envanteri kullanır.
- Shift, sürükleme, sayı tuşu, yerden eşya ve ok toplama da bu sınıra uyar. Sığmayan ganimet yerde kalır; oyuncuya gizli bir ek depo verilmez.
- Sandıklar mevcut merkez **250×250** ganimet alanına, iç/dış mekânlara dağıtılır. Aralarında yatay olarak en az **12 blok** bulunur. Birden çok kattaki üst üste sandıklar da aynı yatay mesafe kuralına uyar. Yeterli uygun konum bulunamazsa sandık adedi azalır.
- Koruma Lokması **2**, Güçlü Koruma Lokması **3** sarı kalp verir. Bir normal ve bir güçlü birlikte **en fazla 5 sarı kalbe** ulaşır; hangi sırada yediğin önemli değildir. Süreyle kaybolmaz, darbe aldıkça tükenir.
- Aynı lokma türünün kendi katkısı bitmeden tekrar yenemez. Katkısı biten tür tekrar kullanılabilir; ancak son oyuncu darbesinden sonra **3 saniye** geçmelidir. VIP'in başlangıçtaki 3 kalbi güçlü katkı sayılır; normal lokmayla 5'e çıkabilir.
- Sandık başına normal/güçlü lokma toplam olasılığı yaklaşık **%24**; her biri yaklaşık %12, bir sandıkta en fazla bir ek lokma. Önceki toplam %30'dan azaltıldı.
- Varsayılan alan beklemesi 180'den **120 saniyeye**, daralma 600'den **480 saniyeye** indi. Eski varsayılanlar otomatik güncellenir; yöneticinin farklı özel süreleri korunur.
- Mevcut üç maç, solo/ikili/dörtlü seçimleri, rastgele ganimet ve maç dışına eşya taşımama kuralları devam eder.

## Rehber ve yönetim

**`/fly`**, Rehber ve bu yetkiyi miras alan görevli rollerinde uçuş iznini açıp kapatır; oyun alanlarında da çalışır. Uçuş izni açıkken çift zıplayarak uçabilirsin. Rol izni kaldırılırsa bu sistemin verdiği uçuş izni de kaldırılır. Kurucu için de bu yetki eklenir.

Lobide yapı düzenlemek: `/merkez düzenleme aç`, bitince `/merkez düzenleme kapat`. Arena, BR ve Vampir Köylü'nün mevcut konum/bölge yönetim komutları korunur. Bu paket yeni arena veya lobi konumlarını kendi kendine değiştirmez.

## Son güncelleme

Ana oyun listesinde yalnız oyun düğmeleri vardır. Bilgiler oyunların kendi
menülerindedir. VIP satın alma düğmeleri ürünün VIP olduğunu açıkça yazar.
VIP etiketi altın, adı turkuaz, sohbet mesajı açık sarıdır. Kendi oltanla
spawn’da tuttuğun balık yere düşmeden doğrudan envantere eklenir.
Hapis cezası olanlar eski menülerden oyun/mesaj/market işlemi yapamaz.

## 1.1.0 biriken aktiflik

Oyunlar arasında ve yeniden girişte aktif süre saklanır: toplam 15 dakika 0,20 Akçe. Günlük en fazla beş ödül (1 Akçe). Türkiye saatine göre 00.00 yeni hak açar; yarım ilerleme korunur, sınır dolduktan sonra yeni süre birikmez. Yarış sürüşü, oyuncu girdisi ve oyun menüleri aktivite sayılır. Lobi/AFK/seyirci/hapis sayılmaz. Ödül işi ve günlük sayaç birlikte, atomik kaydedilir; kredi tekrarları tek ödeme verir.

## 1.2.0

`/kazanç` (`/kazanc`, `/akçebilgi`) Akçe kazanmayı anlatır. Spawn'da kendi tuttuğun balık başına 0,05 Akçe, 15 satışta 0,75 Akçe; 15. satıştan itibaren 24 saat bekleme. `/olta` saatte bir ücretsiz olta verir; `/sat balık` kendi balıklarını ve Kasaba oltasını satar. Oyunlar arasında biriken her 15 dakika aktif süre 0,20 Akçe, Türkiye takvim gününde en fazla 5 ödül / 1 Akçe. Spawn, lobiler, seyirci ve hareketsiz bekleme sayılmaz. Kazanç sayfası kalan süre ve günlük hakkı da gösterir. Envanterde görünürlük boyasına ve arkadaş simgesine tıklamak ilgili işlemi açar.

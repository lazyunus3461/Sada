# KasabaTR Boya Savaşı 1.1.0

Paper **1.21.10 / Java 21**, mevcut KasabaTR sunucusuna uyarlanmış kırmızı–mavi alan boyama oyunu. Oyuncular boya tabancasını sağ tıklayıp kartopu atar; beyaz beton zemini takım rengine çevirir. Rakip boyayı ele geçirince alan sayısı eski takımdan düşüp yeni takıma geçer. En çok alanı kaplayan takım kazanır.

## Kurulum

Hazır ZIP hem **Boya Savaşı** hem onaylanan **canlı bilgi tablosunu** içerir:

- KasabaBoya **1.1.0** — yeni oyun.
- KasabaHub **1.6.2** — on oyun kartı, Boya Savaşı rengi ve rehber sayfası.
- KasabaBilgi **1.0.0** — oyuncu/rütbe/Akçe/klan/süre ve üç adres.
- KasabaRoles **1.3.2** — bilgi tablosunun gerçek rütbe okuması.

1. Sunucuyu tamamen durdur ve `plugins`/dünya klasörlerini yedekle.
2. Aynı eklentilerin eski **JAR** dosyalarını kaldır. Örneğin eski Hub, Roles, varsa Boya ve Bilgi JAR'ları; veri klasörlerini koru.
3. ZIP'teki dört JAR'ı `plugins` içine koy. Mevcut KasabaAuth 1.3.0, KasabaAkce 1.1.0 ve diğer oyunlar kalır.
4. Sunucuyu başlat. `/reload` kullanma. `/oyunlar` → **Boya Savaşı** seç.

İki dünya JAR'ın içindedir, eksikse otomatik kurulur. Dünya dosyalarını ayrıca kopyalamak, `/mv import`, `/oyunlar ekle` veya spawn ayarlamak gerekmez. Önceden var olan aynı adlı dünya ve ayarların üzerine yazılmaz. Kaynak ZIP'teki eski KasabaHub 1.2.0 bu pakete alınmadı.

## Oyuncu komutları

| Komut | İşlem |
|---|---|
| `/boya` | Oyuna ait ayrı lobiye girer veya menüyü açar. |
| `/boya oyna` | Sıraya girer. Grup varsa lider yazar. |
| `/boya durum` | Sıra ve oda durumunu gösterir. |
| `/boya lobi` | Boya lobisine döner. Normal maçı yarım bırakmak ceza verir. |
| `/boya çık` veya `/lobi` | Ana spawna döner. |
| `/boya yardım` | Oyun açıklamasını açar. |
| `/tablo aç` / `/tablo kapat` | Kişisel bilgi panelini açar/kapatır. |

Pusula, elde sağ tıkla ve envanterden tıklayarak açılır. **Oyna**, **Sıradan çık**, **Bu lobide kal**, **Oyun lobileri** ve ana spawna dönüş seçenekleri vardır. Lobiye geldiğin anda veya maç bittiğinde kendiliğinden sıraya girmezsin. Oyun lobileri arasında doğrudan geçilebilir.

## Yönetici komutları

| Komut | İşlem |
|---|---|
| `/boya başlat` | Uygun sıradaki en az 4 kişiyle normal maçı hemen başlatır. |
| `/boya deneme` | Sırada tam 2 kişi varsa iki kişilik deneme başlatır. |
| `/boya testharita` | Aktif oda yokken lobi/arena ve takım doğmalarını geçici kopyada doğrular; konsoldan da olur. |
| `/boya düzenleme aç` / `kapat` | Oyundan çıktıktan sonra yöneticiye ortak şablonları düzenleme izni verir. |
| `/oyunceza kaldır Oyuncu boyasavasi` | Bu oyunun yarım bırakma cezasını kaldırır. |

İki kişi test: ikiniz `/boya`, ikiniz `/boya oyna`; yönetici sıradayken `/boya deneme` yazsın. Deneme galibiyet yazmaz ve çıkış cezası üretmez. Normal sınır dört kişi olarak kalır. Yönetici izni `kasaba.boya.admin` (OP); oyun izni `kasaba.boya.play` (varsayılan herkese açık). Türkçe veya `baslat`, `cik`, `duzenleme ac` yazımları kullanılabilir.

## Oyun ve veriler

- Varsayılan **4–12 kişi**, eşit iki takım, **15 saniye** başlangıç, **180 saniye** maç.
- Aynı anda **3 ayrı maç**. Her maç `boya_room_...` adlı ayrı dünya kopyasında oynanır. Öteki oda, bekleme lobisi ve orijinal arena boyanmaz.
- Kırmızı/mavi takımlar rastgele eşit dağıtılır. Arkadaş grubu aynı odada kalır; takım dağılımında karşı takımlara düşebilir. Uygun çift oyuncu sayısı elde edilirken grup oda sınırında kesilmez.
- Tekrarlanan atış aynı alanı yeniden puanlamaz. Yalnız haritadaki **4.376 izinli blok** boyanır. Varsayılan atış aralığı 6 tick (yaklaşık 0,3 saniye), sıçrama yarıçapı 2 bloktur.
- Rakibe isabet **1,5 saniye yavaşlık** verir; can hasarı yoktur, takım arkadaşına yavaşlık uygulanmaz.
- En çok boyalı bloğu olan takım kazanır. Beraberlikte galibiyet yazılmaz. Normal maçın kazanan oyuncuları lobideki **ilk 5 galibiyet tablosuna** işlenir; aynı sonuç iki kez sayılmaz.
- Sonuçtan **5 saniye** sonra aynı oyunun lobisine dönülür; envanterdeki silah/zırh/etkiler temizlenir. Eski kişisel eşyalar ana lobiye/başka oyuna geçince geri yüklenir.
- Normal aktif maçtan ayrılan **15 dakika yalnız bu oyuna** giremez. Bekleme, deneme, normal sonuç ve zorunlu sistem çıkışları cezayı üretmez. Bir takım tamamen ayrılırsa kalan takım kazanır.
- Envanter yedeği girişten önce alınır. Bağlantı koparsa hesap doğrulandıktan sonra kurtarılır. Bozuk yedeğin üzerine yazılmaz.
- Mevcut aktiflik/AFK kuralları geçerlidir; oyun süresi sunucunun ortak aktiflik havuzuna katılır. Lobi süresi sayılmaz. Bu oyun ayrıca para basmaz.
- Oyuncu modu veya kaynak paketi gerekmez. Lobi ve arena korumaları, kimlik doğrulama ve hapishane giriş kısıtları mevcut sistemle çalışır.

Ortak dünya adları `KasabaTR-Boya-Savasi-Lobi-v01` ve `KasabaTR-Boya-Savasi-Arena-v01` şeklindedir. Geçici kopyalar Multiverse'e kaydedilmez; ortak dünyalar varsa Multiverse'e otomatik tanıtılır. Yalnız bu eklentinin işaretlediği kullanılmayan maç kopyaları temizlenir. Eski başka dünya ve oyuncu kayıtları silinmez.

`plugins/KasabaBoya/config.yml` varsayılan süre, normal oyuncu sınırları, oda sayısı ve atış ayarlarını içerir. Ayar değiştirilirse tam yeniden başlatma gerekir. Sınırların dışındaki aşırı değerler güvenli aralığa alınır. Eski 1.0.0 ayarı korunur; eksik yeni alanlar varsayılanlardan okunur.

Bilgi tablosunda lobilerde mat renkler, oyunda daha küçük ve soluk yazılar kullanılır. Gerçek vanilla arka plan saydamlığı oyuncunun istemci ayarıdır; eklenti tarafından farklı alfa oranları ayarlanmaz. Ayrıntı [KasabaBilgi kılavuzunda](../kasaba-bilgi/README.md).

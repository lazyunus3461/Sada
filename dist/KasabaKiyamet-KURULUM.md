# Kasaba Kıyamet Parkuru 1.0.0

KasabaTR için bağımsız, çok zor bir süre parkuru. Tek rota; 48 farklı bölüm, 492 sıralı geçiş ve 33 yön değişimi. Mevcut KasabaParkur'un kolay, orta ve zor rotalarını değiştirmez. MrBeast videosunun kopyası değildir; bu paket için oluşturulmuştur.

## Oynanış

- Tek blok ve çapraz inişler, dar çitler, alçak tavanlar, merdiven çıkışları, slime sekmeleri, bal kayışı, ince kapaklar ve yükselen kıvrımlı sırtlar.
- Geçişler sırayla tamamlanır. Sonraki geçiş oyuncuya özel küçük bir ışıkla gösterilir; bölümün tekniği aksiyon çubuğunda anlatılır.
- Sahanlıklar dinlenmek içindir; kayıtlı yeniden doğma noktası değildir. Düşünce ilk bölüme dönersin ve süre yeniden başlar. Yeniden deneme için bekleme cezası yoktur.
- Sağ panelde canlı olarak en ilerideki 5 oyuncu, üstte süre/bölüm/geçiş bilgisi bulunur. Bitiriş rekorları ayrıca kalıcı kaydedilir.
- Uçuş, dışarıdan ışınlanma, düzenleme ve yönetici denemesi rekor dışıdır. Baştan başlamak normal bir denemeye izin verir.
- Oyuna girerken envanter, zırh, durum ve konum yedeklenir. Çıkışta geri yüklenir; bağlantı kesilirse sonraki hesap girişinde kurtarılır.
- Oyuncular birbirini itemez; açlık, hasar, blok kırma/yerleştirme ve eşya atma engellenir. OP düzenleme açabilir.

## Kurulum

1. Sunucuyu kapat. Mevcut sunucu yedeğini al.
2. **Java 21, Paper 1.21.10, KasabaAuth ve KasabaHub** gerekli. Bu sürüm gerçek KasabaAuth 1.4.2 ve KasabaHub 1.12.2 ile test edildi. İstemci Minecraft Java 1.21.10 olmalı; menüler yerel Dialog arayüzünü kullanır.
3. Kurulum ZIP'indeki `plugins/KasabaKiyamet-1.0.0.jar` dosyasını sunucunun `plugins` klasörüne koy. Diğer eklentileri veya dünyaları değiştirme.
4. Sunucuyu normal başlat; `/reload` kullanma. Harita JAR içinden otomatik olarak `kasaba_kiyamet_v1` klasörüne çıkarılır. Dünya veya hologramları elle kurman gerekmez.
5. `/kiyamet` ile menüyü aç veya `/kiyamet başla` kullan. Oyunlar menüsüne Kıyamet Parkuru otomatik eklenir. Ana lobi daha önce ayarlı değilse OP ana lobi noktasında `/merkez lobi ayarla` kullanabilir.

Mevcut `kasaba_kiyamet_v1` dünyasının üzerine yazılmaz. Başka/eksik bir dünya bu adı kullanıyorsa eklenti hata verip durur; veriyi silmez. Bu durumda klasörü kontrol et. Dünyanın tüm yüksekliği boş bir üreticiyle devam eder. Multiverse zorunlu değildir; varsa ortak dünya kaydı denenir. Farklı Minecraft sürümleri ve farklı Multiverse sürümleri bu pakette test edilmedi.

Mod, özel başlatıcı ve kaynak paketi gerekmez. Harita ayrıca arşivde verilmiştir; normal kurulumda onu elle çıkartma.

## Komutlar

| Komut | İşlev |
| --- | --- |
| `/kiyamet` | Menü / oyun lobisine giriş |
| `/kiyamet başla` | Süreli denemeye en baştan başla |
| `/kiyamet dön` | Rotayı ve süreyi başa al |
| `/kiyamet lobi` | Kıyamet'in kendi giriş meydanına dön |
| `/kiyamet ilk5` | Canlı ilerleme sıralaması |
| `/kiyamet süreler` | En hızlı bitirişler ve kişisel rekor |
| `/lobi` | Kasaba ana lobisine dön, eşyalarını geri al |
| `/kiyamet deneme asiri 27` | OP: 27. bölümden dene; rekor dışı |
| `/kiyamet yönetim aç` | OP: bu haritada yaratıcı düzenleme |
| `/kiyamet yönetim kapat` | OP: düzenlemeyi kapat, Kıyamet lobisine dön |
| `/kiyamet durum` | Dünya ve içerideki oyuncu sayısı |

Oyuncu başka bir oyunun içindeyse önce `/lobi` kullanmalı; KasabaHub'un oyun geçiş kuralları geçerlidir. Oyuna giriş izni `kasaba.kiyamet.play` varsayılan herkese açıktır; yönetim izni `kasaba.kiyamet.admin` varsayılan OP'dir.

## Kayıt ve bakım

`plugins/KasabaKiyamet/sure-rekorlari-v1.json` rekorları; `recoveries/` oyuncu eşya kurtarmalarını içerir. Eşya yedeği varken oyuncu tekrar oyuna alınmaz; önce kurtarma tamamlanır. Bozuk kayıt dosyaları sıfırlanmaz. Kaydetme hatasında yeni yarışlar durdurulur, önceki rekorlar korunur. Eklentiyi kaldırmadan önce oyuncuları `/lobi` ile çıkar ve kurtarma yedeklerini kontrol et.

`config.yml` içindeki düşüş mesafesi 6–20 blok, gecikmesi 6–40 tik sınırları içinde uygulanır. Varsayılan 8 blok ve 10 tik; slime/bal alt seviyeleri bölüm verisinde ayrıca hesaba katılır.

Haritayı düzenlemek otomatik geçiş noktalarını taşımaz. Rekor adaletini korumak için rota değişiklikleri `parkur-layout.json` ile birlikte yapılmalıdır. Yönetim modu yapı incelemesi/dekor için doğrudan kullanılabilir.

## Test kapsamı

21 birim testi; gerçek Anvil dosyalarında 49 sahanlık, 492 geçişin destek ve vücut boşluğu, 569 normal geçişin mesafe/yükseklik denetimi. Gerçek Paper üzerinde hesap girişi, yerel menü düğmesi, altı oyuncuyla ilk 5, sıralı bitiş, düşüş, uçuş/dış ışınlanma geçersizliği, OP düzenleme, mevcut Parkur ile birlikte çalışma ve eşya iadesi kontrol edildi. Yeniden başlatma testi rekor kalıcılığı ve bağlantı kesilmesi sonrası eşya kurtarmayı ayrıca denetler.

Sunucu testinde özel ve sadece yerel test sunucusunda çalışan bir araç oyuncuları geçişlere konumlandırır. Bu, 48 bölümün gerçek bir oyuncuyla baştan sona atlanıp bitirildiği anlamına gelmez. Slime, bal ve merdiven çıkışlarının tam oynanış doğrulaması insan testi gerektirir. Raporlar `test-raporlari` klasöründedir; görseller gerçek bloklardan çizilmiş teknik önizlemedir, Minecraft ekran görüntüsü değildir.

## Kaynak kodu derleme

`kaynak-kod.zip` içinde Maven projesi, Java testleri, hazır harita kaynakları ve Python dünya üreticisi bulunur. Python 3.12, nbtlib 2.0.4, numpy ve Pillow yalnız haritayı yeniden üretmek için gerekir; JAR derlemek için gerekmez.

`./build.sh /sunucu/plugins/KasabaAuth-1.4.2.jar /sunucu/plugins/KasabaHub-1.12.2.jar` ile sunucundaki API eklentileri yerel Maven deposuna alınır ve `mvn verify` çalışır. Windows'ta `build.ps1` aynı iki yolu kabul eder. Java 21 ve Maven 3.9+ gereklidir. Paper API ve Maven eklentileri ilk derlemede indirilir. Derleme çıktısı `target/KasabaKiyamet-1.0.0.jar` olur.

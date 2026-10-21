# KasabaRoles 1.0.0

Paper **1.21.10**, Java **21** için Kasaba'nın bağımsız rol eklentisi. LuckPerms veya TAB gerekmez. KasabaAuth ile birlikte çalışabilir. Oyuncuya verilen rol kaydedilir; yeniden bağlanınca ve sunucu yeniden başlayınca korunur.

TAB listesinde renkli, kalın rol etiketi ve beyaz oyuncu adı görünür: **[Kurucu] Redkit**. Rol sırası yüksek olan üstte görünür. Sohbet biçimi ve oyuncunun kafasının üstündeki isim değiştirilmez.

## İndir ve kur

[KasabaRoles-1.0.0.jar indir](https://github.com/lazyunus3461/Sada/raw/refs/heads/main/dist/KasabaRoles-1.0.0.jar)

1. Sunucuyu durdur.
2. JAR dosyasını sunucunun `plugins` klasörüne koy. KasabaAuth dosyasını kaldırma; bu ayrı bir eklentidir.
3. Sunucuyu yeniden aç. Konsolda `KasabaRoles v1.0.0` etkinleşmeli.
4. **Sunucu konsoluna**, başına `/` koymadan, `rol ver Redkit kurucu` yaz. `Redkit` yerine kendi Minecraft adını yaz.
5. Artık oyunda `/rol` komutlarını kullanabilirsin. İlk Kurucu ataması konsoldan veya zaten OP olan bir oyuncudan yapılır.

## Hazır roller

| Rol kimliği | TAB etiketi | Renk | Sıra | Başlangıç yetkisi |
| --- | --- | --- | --- | --- |
| `kurucu` | Kurucu | Açık kırmızı | 1000 | Rol yönetimi, kayıtlı Bukkit ve Minecraft komut yetkileri |
| `admin` | Admin | Koyu kırmızı | 800 | Moderatörün yetkileri, ban kaldırma/verme, zaman ve hava |
| `moderator` | Moderatör | Yeşil | 600 | Oyuncu atma ve ışınlanma |
| `rehber` | Rehber | Turkuaz | 400 | Rol bilgisi ve listesi |
| `vip` | VIP | Altın | 300 | Rol bilgisi ve listesi |
| `oyuncu` | Oyuncu | Gri | 1 | Rol bilgisi ve listesi; varsayılan rol |

Rehber ve VIP etiketleri hazırdır; sunucundaki diğer eklentilere özel yetkileri aşağıdaki komutla ekleyebilirsin. Her oyuncunun tek ana rolü vardır. Yeni bir rol verince önceki rol değiştirilir.

## Türkçe komutlar

| Komut | İşlev |
| --- | --- |
| `/rol bilgi [oyuncu]` | Rolünü veya belirtilen oyuncunun rolünü gösterir |
| `/rol liste` | Mevcut rolleri listeler |
| `/rol ver Redkit kurucu` | Rol atar; çevrimdışı oyuncuya da verilebilir |
| `/rol ver Redkit rehber` | Önceki rolü Rehber ile değiştirir |
| `/rol al Redkit` | Atamayı kaldırır, varsayılan role döndürür |
| `/rol oluştur destek turkuaz Destek Ekibi` | Yeni rol oluşturur |
| `/rol isim destek Yardım Ekibi` | TAB'da görünen rol adını değiştirir |
| `/rol renk destek yeşil` | Etiket rengini değiştirir; `#12ABEF` gibi HEX renk de kabul edilir |
| `/rol sıra destek 450` | TAB sırasını değiştirir; büyük sayı üsttedir |
| `/rol yetki destek eklenti.yetkisi ver` | Role yetki ekler |
| `/rol yetki destek eklenti.yetkisi yasakla` | Yetkiyi açıkça reddeder |
| `/rol yetki destek eklenti.yetkisi sil` | O yetki kuralını kaldırır |
| `/rol sil destek` | Boştaki rolü siler |
| `/rol yenile` | Ayarları ve oyuncu rollerini dosyalardan yeniden yükler |

`oluştur` yerine `olustur`, `sıra` yerine `sira`, `kaldır` yerine `kaldir` de yazabilirsin. `/roller` ve `/kasabarol`, `/rol` ile aynıdır. Komutlar TAB ile tamamlanır. Rol kimliklerinde `a-z`, rakam, `_` ve `-` kullan; görünen adlarda Türkçe harfler ve boşluk olabilir.

Rol yönetimi için `kasabaroles.admin` gerekir; varsayılan olarak OP oyunculara ve Kurucu rolüne verilir. Bilgi ve liste komutları için `kasabaroles.info` gerekir; varsayılan olarak herkese açıktır. Eklenti kimseye otomatik OP vermez. Zaten OP olan bir oyuncunun OP yetkileri rolü alınarak kaldırılmaz; bunun için sunucunun `deop` komutu kullanılır.

## Dosyalar ve yetkiler

İlk açılışta `plugins/KasabaRoles/config.yml` ve `plugins/KasabaRoles/oyuncular.yml` oluşur. Komutla yapılan değişiklikler hemen kaydedilir ve çevrimiçi oyunculara uygulanır. Oyuncu rolleri küçük harfli Minecraft adıyla tutulur; isim değiştiren hesaplara rol yeniden atanır. Oyuncu adının güvenli doğrulanması sunucunun veya KasabaAuth gibi kayıt eklentisinin sorumluluğundadır.

`config.yml` içindeki `inherits`, başka rollerin yetkilerini devralır. Örneğin Admin → Moderatör → Rehber → Oyuncu. Role yazılan aynı yetki düğümü, devralınan değeri ezer. Olmayan roller ve döngüsel devralma reddedilir. Varsayılan rol, oyuncuya atanmış rol ve başka rolün devraldığı rol doğrudan silinemez. Önce ilgili atamaları/bağlantıları değiştir.

`eklenti.*` ve `*`, Bukkit'e kayıtlı yetki düğümleri için genişletilir. Kayıtlı olmayan yetkileri tam adlarıyla ekle. Tam yetki kuralı jokerden, daha uzun joker kuralı daha kısa jokerden önceliklidir. `ver`, `yasakla` ve `sil`, yetki düğümünü sağlayan diğer eklentinin izin denetimleriyle çalışır; başka bir eklentinin kendi OP kontrolünü değiştirmez.

Rol etiketinin rengi `color`, oyuncu adının rengi `name-color`, sıralama değeri `priority` alanındadır. Sonradan başka bir TAB eklentisi kurarsan `manage-tab: false` yapıp `/rol yenile` kullanabilirsin; rol yetkileri devam eder. Bu sürümde birden fazla TAB yöneticisinin aynı anda etiketi değiştirmesi desteklenmez.

Dosyaları elle düzenlemeden önce yedekle. Hatalı `/rol yenile` mevcut çalışan rol verisini değiştirmez. İlk açılışta bozuk dosya varsa konsola hata yazılır ve eklenti devre dışı kalır. Eklenti değişiminde sunucuyu tamamen yeniden başlat; Bukkit `/reload` kullanma.

## Derleme ve test

JDK 21 / Maven 3.9+ ile bu klasörde `mvn -B clean verify`. Çıktı: `target/KasabaRoles-1.0.0.jar`.

Bulutta `bash /workspace/Sada/kasaba-roles/scripts/cloud-build.sh` bağımlılıkları hazırlayıp birim testlerini ve derlemeyi çalıştırır. Protokol testleri için [test yönergelerine](scripts/integration/README.md) bak. Test yardımcı eklentisini gerçek sunucuya kurma.

# KasabaRoles 1.2.0

1.3.2, sağ bilgi tablosu için `RoleAccess.rank(Player)` metodu ekler. Gerçek atanmış rol ve geçici VIP önceliği okunur; rol dosyaları ve mevcut API metotları değişmez. [Bilgi tablosu paketi](../dist/KasabaTR-Bilgi-Tablosu-1.0.0.zip) güncel Roles JAR'ını içerir.

Paper **1.21.10**, Java **21** için Kasaba'nın bağımsız rol eklentisi. LuckPerms veya TAB gerekmez. KasabaAuth ile birlikte çalışabilir. Oyuncuya verilen rol kaydedilir; yeniden bağlanınca ve sunucu yeniden başlayınca korunur.

TAB listesinde ve oyuncunun kafasının üstünde renkli, kalın rol etiketi görünür: **[Kurucu] Redkit**. Sohbet biçimi **[Kurucu] Redkit » mesaj** olur. Rol sırası yüksek olan TAB'da üstte görünür. Rol verildiğinde veya adı/rengi değiştiğinde üç görünüm de güncellenir.

## İndir ve kur

[KasabaRoles-1.2.0.jar indir](https://github.com/lazyunus3461/Sada/raw/refs/heads/main/dist/KasabaRoles-1.2.0.jar)

1. Sunucuyu durdur.
2. JAR dosyasını sunucunun `plugins` klasörüne koy. KasabaAuth dosyasını kaldırma; bu ayrı bir eklentidir.
3. Sunucuyu yeniden aç. Konsolda `KasabaRoles v1.2.0` etkinleşmeli.
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

Rol etiketinin rengi `color`, oyuncu adının rengi `name-color`, sıralama değeri `priority` alanındadır. `manage-tab`, `manage-chat` ve `manage-nametags` ayrı ayrı kapatılabilir; ardından `/rol yenile` kullan. Eski 1.0.0 ayarları ve rol atamaları korunur; yeni sohbet ve kafa üstü özellikleri varsayılan açık gelir.

Sohbet Paper'ın chat renderer API'siyle biçimlenir; mesaj içeriği, alıcılar, iptal/filtreleme akışı korunur. Mesaj imzaları veya Minecraft'ın sohbet güvenliği durumu değiştirilmez. Eşzamanlı sohbet işlemleri, rol verisinin değişmez anlık kopyasını kullanır.

Kafa üstü etiketleri mevcut scoreboard üzerinde yalnızca bu eklentinin oluşturduğu `kr...` takımlarını kullanır. Kişisel scoreboard değiştiğinde iki saniye içinde etiketler yeni tahtaya eklenir; sidebar ve objective'ler değiştirilmez. Bir oyuncu zaten başka eklentinin takımındaysa o takım devralınmaz; etiket atlanır ve konsola uyarı yazılır. Böyle bir durumda kafa üstü görünümünü tek eklentiden yönet. Minecraft'ın takım adı rengi 16 renkle sınırlıdır; özel HEX `name-color` için kafa üstündeki oyuncu adı en yakın renge çevrilir. Rol etiketi HEX rengini korur. Oyuncu kendi etiketini normal birinci şahıs görünümünde göremez; başka oyuncuyla kontrol et.

Dosyaları elle düzenlemeden önce yedekle. Hatalı `/rol yenile` mevcut çalışan rol verisini değiştirmez. İlk açılışta bozuk dosya varsa konsola hata yazılır ve eklenti devre dışı kalır. Eklenti değişiminde sunucuyu tamamen yeniden başlat; Bukkit `/reload` kullanma.

## Derleme ve test

JDK 21 / Maven 3.9+ ile bu klasörde `mvn -B clean verify`. Çıktı: `target/KasabaRoles-1.2.0.jar`.

Bulutta `bash /workspace/Sada/kasaba-roles/scripts/cloud-build.sh` bağımlılıkları hazırlayıp birim testlerini ve derlemeyi çalıştırır. Protokol testleri için [test yönergelerine](scripts/integration/README.md) bak. Test yardımcı eklentisini gerçek sunucuya kurma.

## KasabaKlan etiketi

1.2.0, KasabaKlan 1.0.0 ile rol ve klan etiketini birlikte gösterir: `[Kurucu] [KSB] Redkit`. Üyelik değişimleri TAB, sohbet ve mevcut scoreboard üzerindeki kafa üstü takımlarına anında yansır. Klan eklentisi yoksa eski davranış sürer; rol atamaları ve yetkiler korunur. `ClanLabels` servisi, async sohbet için değişmez bir etiket görüntüsü sunar. Birim testi sayısı 20 oldu.

## 1.3.1

VIP rolü için etiket `gold`, oyuncu adı `aqua`, sohbet metni `yellow` olarak
ayrılır. Mesaj rengi rolün `chat-color` ayarıdır; ayar bulunmayan rollerin
mesaj biçimi korunur. Mevcut VIP ayarları ilk açılışta güncellenir, eski config
yedeği saklanır. Kurucu/Admin/Moderatör için hapishane yönetim izni eklenir;
açıkça yasaklanmış izinler açılmaz. Rehber bu izni almaz.

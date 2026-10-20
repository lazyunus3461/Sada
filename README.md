# KasabaAuth

Paper **1.21.10** / Java **21** için bağımsız, Minecraft içi menülü kayıt ve giriş eklentisi. AuthMe gerektirmez; mevcut AuthMe şifrelerini içeri aktarmaz.

## Sunucuya kurulum

1. Sunucuyu durdur ve oyuncu verileriyle `plugins` klasörünü yedekle.
2. Başka bir kayıt eklentisi varsa birlikte çalıştırma. Mevcut kullanıcıları taşıma planını belirlemeden eski kayıt sistemini kaldırma.
3. `KasabaAuth-1.0.0.jar` dosyasını sunucunun `plugins` klasörüne koy.
4. Sunucuyu Java 21 ile aç. Konsolda `KasabaAuth` etkinleşmeli.
5. Oyuna gir; otomatik açılan menüde **Kayıt Ol** seçeneğini seç. Örs ekranının üstteki yazı alanını temizle, şifreni yaz ve sağdaki **Onayla** kâğıdına tıkla. İkinci ekranda şifreni tekrar yaz.
6. Oyundan çıkıp tekrar gir; bu kez **Giriş Yap** seçeneğini kullan.

Menüyü tekrar açma: `/kasaba`. `/giris`, `/kayit`, `/login`, `/register` aynı menüyü açan kısayollardır. **Komuta veya sohbete şifre yazma.**

Kayıt/giriş boyunca üst çubukta **Kasaba’ya Hoş Geldin** ve kalan süre görünür. Başarıdan sonra büyük karşılama yazısı çıkar. ESC ile kapanan ekran tekrar açılır. Giriş yapılmadan hareket, sohbet, diğer komutlar, envanter işlemleri, eşya alma/atma ve oyuncu etkileşimleri engellenir. Giriş beklerken hasar ve açlık değişimleri engellenir.

## Bilinen sınırlar

- Vanilla örs alanında şifreyi yıldızlarla gizlemek mümkün değildir: şifre oyuncunun **kendi ekranında** görünür. Diğer oyuncuların sohbetine gönderilmez. Mod veya kaynak paketi gerekmez.
- Bu eklenti ayrı bir giriş lobisi veya ayrı bir sunucu oluşturmaz. Oyuncuların doğduğu yeri güvenli yap; kimlik doğrulaması öncesinde dünyanın istemciye yüklenmesini engellemez.
- Java oyuncu adları desteklenir. Floodgate/Bedrock ad önekleri desteklenmez.
- Şifre sıfırlama, şifre değiştirme, e-posta doğrulaması, çoklu sunucu veritabanı ve AuthMe hesap aktarımı bu sürümde yoktur.
- Hatalı giriş sayacı aynı sunucu sürecinde yeniden bağlanmalarda korunur; sunucu yeniden başlatıldığında sıfırlanır.
- Bir kayıt eklentisi Minecraft lisans doğrulamasının yerine geçmez. Mevcut `online-mode` ayarını yalnızca bu eklenti için değiştirme. Proxy arkasındaki sunucularda backend portunu dış erişime kapat ve güvenli oyuncu yönlendirmesini sunucu/proxy belgelerine göre yapılandır.
- Kötü niyetli veya güvenilmeyen diğer eklentilerden koruma sağlamaz. Gerçek sunucuna geçmeden önce diğer eklentilerle birlikte test et. `/reload` yerine tam yeniden başlatma kullan.

## Ayarlar ve veri

İlk başlangıçta `plugins/KasabaAuth/config.yml` oluşur:

| Ayar | Varsayılan |
| --- | --- |
| Giriş süresi | 120 saniye |
| Hatalı şifre sınırı | 5 |
| Kilit süresi | 300 saniye |
| Şifre uzunluğu | 8–64 karakter |

Hesap dosyaları `plugins/KasabaAuth/accounts/` altında, küçük harfli oyuncu adıyla tutulur. PBKDF2-HMAC-SHA256, 600.000 iterasyon, rastgele 16 bayt tuz ve 256 bit özet kullanılır. Şifre hesaplama ana sunucu iş parçacığının dışında yapılır. Yedeklerine erişimi de sınırla. Ayar değişiklikleri tam yeniden başlatmada uygulanır; mevcut kullanıcıları dışarıda bırakmamak için minimum şifre uzunluğunu sonradan yükseltme.

## Geliştirme

JDK 21 ve Maven 3.9+ gerekir:

```sh
mvn -B verify
```

Çıktı: `target/KasabaAuth-1.0.0.jar`. Testler parola doğrulama, yanlış/bozuk özet reddi, rastgele tuz, yeniden başlatmada veri korunması, büyük/küçük harf ile hesap atlatmanın ve mevcut hesabın üzerine yazmanın engellenmesini kapsar.

Bulut geliştirmesinde mevcut `/workspace/Sada` checkout'unu kullan. Kullanıcı istemedikçe Git worktree oluşturma. Test sunucusunu gerçek sunucunun dosyalarından ayrı tut ve yalnızca loopback adresine bağla.

## Oyunda kabul kontrolü

Yeni test adıyla kayıt ol, iki farklı şifreyle onayın reddedildiğini gör, eşleşen şifreyle kaydı tamamla. Yeniden bağlanıp yanlış ve doğru şifreleri dene. Giriş öncesi hareket/komut/sohbet/shift-click/eşya atmayı, ESC ile menünün geri gelmesini, art arda yanlış şifreyle kilidi ve yeniden bağlantıda kilidin korunmasını kontrol et. Sunucuyu yeniden başlatıp aynı şifreyle giriş yap. Testleri kendi eklenti listenle tekrar et.

## Yerel protokol testi

`/workspace/kasaba-test-server` içinde hazırlanan, yalnızca `127.0.0.1:25575` adresini dinleyen Paper 1.21.10 build 130 test sunucusu kullanılabilir. Gerçek sunucunun dizinini kullanma. Test sunucusu offline modda yalnızca sentetik oyuncular içindir; dışarı açma. Sunucu çalışırken JAR değiştirme; önce durdur, yeni JAR'ı kopyala ve yeniden başlat.

```sh
# Buluttaki Java/Maven araçları ve güvenli proxy güven deposu:
bash /workspace/Sada/scripts/cloud-build.sh

# Ayrı terminalde; durdurmak için konsola stop yaz veya Ctrl+C kullan:
bash /workspace/Sada/scripts/start-test-server.sh

# Konsolda Done yazdıktan sonra:
cd /workspace/Sada/scripts/integration
npm ci --cache /workspace/.npm --no-audit --no-fund
node smoke.cjs
```

Protokol testi gerçek Paper sunucusuna bağlanır; sürüm yanıtı, karşılama menüsü/üst çubuk, ESC sonrası tekrar açılma, giriş öncesi komut/hareket engeli, şifre tekrarı, uyuşmayan şifre reddi, kayıt, yeniden bağlantıda yanlış/doğru şifre, deneme sınırı ve yeniden bağlantıda kilidin korunmasını doğrular. Her çalıştırmada yeni sentetik kullanıcı oluşturur; gerçek oyuncuları kullanmaz. Test şifresi yalnızca sentetik hesaplar içindir. Test sunucusu başlangıcında Mojang public-key ağ hatası görülebilir; bu, yerel offline test bağlantısını engellemez. Lisanslı hesapların online-mode doğrulaması bu testin kapsamı dışındadır.

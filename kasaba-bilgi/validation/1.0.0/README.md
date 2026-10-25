# Bilgi tablosu 1.0.0 doğrulaması

Paper 1.21.10 build 130 / Java 21 / Multiverse-Core 5.3.2 / KasabaTR'nin 15 üretim eklentisiyle özel loopback sunucusunda test edildi. KasabaRoles 1.3.2 (21) ve KasabaBilgi 1.0.0 (3), toplam 24 birim testi başarılıdır.

İki gerçek Minecraft protokol bağlantısıyla dokuz kontrol grubu:

1. Kimlik doğrulanmadan kişisel veri gönderilmemesi; lobide alanların ve üç adresin görünmesi; MS ve kırmızı satır numaralarının olmaması.
2. Gerçek Akçe servisi ve kalıcı geçmiş süre; aynı sunucu scoreboard'unu paylaşırken iki oyuncunun kendi bakiyesini görmesi.
3. Oyuncu/VIP/Kurucu/Rehber atamaları, geçici VIP ve yüksek rol önceliği; geçici test klan servisi değişimleri.
4. Başka sidebar'ın korunup birleştirilmesi; /tablo aç-kapat ile geri yükleme; Bukkit board kimliğinin korunması.
5. X Arena ilk beş satırı ve bilgi panelinin dim renklerle birlikte görünmesi; oyun bitince lobi görünümüne dönüş.
6. Şans Yolu'nda ortak scoreboard ve kişisel bilgi ayrımı; kare/puan metninin korunması.
7. BR'de ortak scoreboard ve NEVER isim görünürlüğünün korunması; ekipman/rol takımlarına müdahale edilmemesi.
8. Yerel sidebar kullanmayan Eşya Saklambacı'nda maçta soluk görünüm, sonuç sonrası ayrı lobide tekrar renkli görünüm.
9. Eklenti kapatılıp açıldığında istemci katmanının kaldırılması ve gerçek oyun sidebar'ının geri gösterilmesi.

Tam yeniden başlatma sonrasında bakiye, rol ve tarihi süre, ilk giriş ve Eşya Saklambacı lobisine geçiş de tekrar doğrulanır. Oyun akışlarını kısaltmak için var olan loopback adaptörleriyle yönetici/test maçları kullanılır. Testteki hot-enable Paper uyarısı normal kurulum yöntemi değildir; gerçek sunucuda tam yeniden başlatma gerekir.

Protokol paketlerindeki metin, RGB renk, boş sayı biçimi ve görünen objective kaydı doğrulanır. Gerçek grafik istemcisinde piksel incelemesi veya yük testi yapılmadı. Vanilla scoreboard arka plan alfa değeri oyuncunun ayarına bağlıdır; farklı bağlamlarda gerçek alfa değişimi yapıldığı iddia edilmez. Lobiden oyuna geçerken düşük kontrast, kalınlığın kaldırılması ve boş satırların azaltılması test edilir.

Tekrar: `python3 kasaba-bilgi/scripts/integration/build-probe.py`, özel test sunucusu açılınca `node kasaba-bilgi/scripts/integration/live.cjs`; tam kapatıp açınca `node kasaba-bilgi/scripts/integration/restart.cjs`. Sonrasında `python3 kasaba-bilgi/scripts/package-release.py --tested-server /workspace/scratch/kasaba-network-test-server`. Probe sınıfları yalnız loopback test ortamına yüklenir; paketleyici bunları reddeder ve her üretim JAR'ını test sunucusundaki SHA256 ile karşılaştırır.

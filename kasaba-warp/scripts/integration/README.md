# Yerel warp testi

Önce `bash kasaba-warp/scripts/cloud-build.sh` ve `bash kasaba-warp/scripts/integration/build-probe.sh` çalıştır. Test sunucusu kapalıyken `target/KasabaWarp-1.0.0.jar` ve `target/KasabaWarpProbe.jar` dosyalarını `/workspace/kasaba-test-server/plugins` içine kopyala. KasabaAuth 1.2.0, KasabaRoles 1.1.0 ve mevcut yerel FlightProbe bulunmalıdır. Bu sunucu önceki kurulumda hazırlanmıştır; temiz bir bilgisayarda otomatik oluşmaz.

Sunucuyu `bash scripts/start-test-server.sh` ile başlat; taze `Done` ve KasabaWarp etkinleşme kaydını bekle. Sunucu yalnızca `127.0.0.1:25575`, offline, allow-flight=false olarak çalışır. Sentetik hesaplar için kullanılan şifre gerçek kullanıcı şifresi değildir. Bu test ayarlarını veya test yardımcısını gerçek sunucuya kopyalama.

Repo kökünde `node kasaba-warp/scripts/integration/warp.cjs` çalıştır. Mevcut ortak Node bağımlılıkları yoksa `scripts/integration` klasöründe `npm ci --cache /workspace/.npm --no-audit --no-fund` gerekir. Test gerçek 3 saniye hazırlık ve 20 saniye kullanım arası sürelerini kullanır; daha kısa ayarlara geçmez.

Çıktıdaki `PERSIST_ADMIN` adını not et. Sunucuyu tamamen kapatıp aç; taze `Done` sonrasında `KASABA_WARP_PERSIST=1 KASABA_WARP_ADMIN=<not edilen ad> node kasaba-warp/scripts/integration/warp.cjs` çalıştır.

Test, `pazar`, `orman`, `vipalan` ve 46 sayfalama noktası oluşturur; yalnızca yerel test sunucusunda çalıştır. Başarısız denemeden sonra aynı testi tekrar çalıştıracaksan yalnızca yerel `plugins/KasabaWarp/warplar.yml` dosyasını sunucu kapalıyken ayrı bir test yedeğine taşı. Üretim verisini silme.

WarpProbe, yalnızca loopback sunucusunda yüklenir. Sınırlı konsol komutları, örnek konumlar, hasar, tek seferlik teleport iptali ve envanter denetimi sağlar. Üretim JAR'ında bu sınıf ve test kanalı bulunmaz. Menünün açılması ve tıklanması gerçek Minecraft protokol paketleriyle sınanır.

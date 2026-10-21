# Yerel warp testi

Önce `bash kasaba-warp/scripts/cloud-build.sh` ve `bash kasaba-warp/scripts/integration/build-probe.sh` çalıştır. Test sunucusu kapalıyken `target/KasabaWarp-1.1.0.jar` ve `target/KasabaWarpProbe.jar` dosyalarını `/workspace/kasaba-test-server/plugins` içine kopyala; eski Warp JAR'ını plugins dışına taşı. KasabaAuth 1.2.0, KasabaRoles 1.1.0 ve mevcut yerel FlightProbe bulunmalıdır. Bu sunucu önceki kurulumda hazırlanmıştır; temiz bir bilgisayarda otomatik oluşmaz.

Sunucuyu `bash scripts/start-test-server.sh` ile başlat; taze `Done` ve KasabaWarp etkinleşme kaydını bekle. Sunucu yalnızca `127.0.0.1:25575`, offline, allow-flight=false olarak çalışır. Sentetik hesaplar için kullanılan şifre gerçek kullanıcı şifresi değildir. Bu test ayarlarını veya test yardımcısını gerçek sunucuya kopyalama.

Repo kökünde `node kasaba-warp/scripts/integration/warp.cjs` çalıştır. Mevcut ortak Node bağımlılıkları yoksa `scripts/integration` klasöründe `npm ci --cache /workspace/.npm --no-audit --no-fund` gerekir. Test gerçek 3 saniye hazırlık ve 20 saniye kullanım arası sürelerini kullanır; daha kısa ayarlara geçmez.

Çıktıdaki `PERSIST_ADMIN` adını not et. Sunucuyu tamamen kapatıp aç; taze `Done` sonrasında `KASABA_WARP_PERSIST=1 KASABA_WARP_ADMIN=<not edilen ad> node kasaba-warp/scripts/integration/warp.cjs` çalıştır.

Test, `pazar`, `orman`, `vipalan` ve 46 sayfalama noktası oluşturur; yalnızca yerel test sunucusunda çalıştır. Başarısız denemeden sonra aynı testi tekrar çalıştıracaksan yalnızca yerel `plugins/KasabaWarp/warplar.yml` dosyasını sunucu kapalıyken ayrı bir test yedeğine taşı. Üretim verisini silme.

WarpProbe, yalnızca loopback sunucusunda yüklenir. Sınırlı konsol komutları, örnek konumlar, hasar, tek seferlik teleport iptali ve envanter denetimi sağlar. Üretim JAR'ında bu sınıf ve test kanalı bulunmaz. Menünün açılması ve tıklanması gerçek Minecraft protokol paketleriyle sınanır.

## 1.1.0 simge, kutu ve çoklu konum testi

`node kasaba-warp/scripts/integration/icons-points.cjs` benzersiz adlarla yeni test warpları oluşturur. İlk sayfanın 1-3, 1-5, 1-7 sabit kutuları boş olmalıdır; eski 1.0.0 sayfalama verileriyle birlikte çalışabilir. Elde yedi adet özel model verili meşe fidanı, menüde tek kopya, sabit yer değiştirme, dolu/alt sıra retleri, gizli özel kutu, nokta ekleme/silme, tek simge ve kayıtlı koordinat/yöne rastgele gidiş sınanır. Gerçek 3 saniye hazırlık ve 20 saniye kullanım arası ayarları korunur. Çeşitliliğin deterministik testi JUnit içindedir; canlı protokol testi rastgele tekrarları hata saymaz.

Durum `target/icons-points-state.json` içine yazılır. Sunucuyu tamamen kapatıp açtıktan ve taze `Done` gördükten sonra `KASABA_WARP_PERSIST=1 node kasaba-warp/scripts/integration/icons-points.cjs` ile simge/model, sabit kutu, üç konum ve özel erişimin diskten geri geldiğini doğrula. Aynı testi tekrar çalıştırmadan önce bu testin kendi önceki sabit-kutulu warplarını kaldır veya yalnızca yerel yedeğe dön; üretim verisine dokunma.

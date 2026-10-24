# KasabaTR 1.6.0 doğrulaması

Bu sürüm yalnız KasabaHub'ı 1.6.1'e günceller ve KasabaEsya 1.3.0'ı ekler. Tam paketteki diğer 12 üretim JAR'ı 1.5.0 dağıtımıyla byte byte aynıdır. Son paketleme betiği bunu otomatik karşılaştırır.

- `unit-results.json`: Hub 24 + Esya 3, toplam 27 test.
- `live-results.json`: 14 canlı kontrol grubu ve yeniden başlatma kontrolü; sekiz gerçek protokol bağlantısı, Paper 1.21.10 build 130, Multiverse 5.3.2, 14 üretim eklentisi.
- `input-and-map-manifest.json`: iki özgün yüklemenin ve gömülü 18 dünya dosyasının SHA256 değerleri/metadata.
- `artifacts.json`: ZIP/JAR sürümleri, boyutları ve SHA256 değerleri.

Java 21/Maven ile repo kökündeki Auth, ardından `kasaba-hub/pom.xml`, ardından `kasaba-esya/pom.xml` için `mvn install` çalıştırılır. Protokol testleri mevcut `scripts/integration/package.json` Node bağımlılıklarını kullanır. `kasaba-esya/scripts/integration/build-probe.py` yardımcı eklentiyi oluşturur. Bu yalnız özel loopback test sunucusuna yüklenir; gerçek sunucuya veya dağıtıma konmaz. Sunucu açıldıktan sonra repo kökünde `node kasaba-esya/scripts/integration/live.cjs`; tam durdurup yeniden açtıktan sonra `node kasaba-esya/scripts/integration/restart.cjs` çalıştırılır. Yardımcılar eski mevcut loopback Network/Arena/Followup test adaptörlerini de kullanır.

Test adaptörleri süre sınırlarını saat alanlarını ilerleterek tetikler. Kullanıcının üretim süreleri değiştirilmez. Asıl tıklama denemeleri gerçek `use_entity` sağ/sol tık paketleriyle, ekran yanıtları gerçek Minecraft native dialog paketleriyle yapılır. Bazı koruma olayları Paper ana iş parçacığında gerçek olay yolundan çağrılır. Otomatik test elle oynanan görsel incelemenin veya kamuya açık yük testinin yerini tutmaz.

Yayın paketi `python3 scripts/package-esya-release.py --tested-server /workspace/scratch/kasaba-network-test-server` ile üretilir. Paketleyici her JAR'ı test sunucusundaki son dosyayla, eski oyunları önceki immutable sürümle ve gömülü haritaları kaynak dosyalarla karşılaştırır; Probe sınıflarını reddeder, ZIP CRC'lerini denetler. Eski sürüm dosyalarının üzerine yazmaz.

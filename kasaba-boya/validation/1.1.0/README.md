# Boya Savaşı 1.1.0 doğrulaması

Paper 1.21.10 build 130 / Java 21 / Multiverse-Core 5.3.2 ve KasabaTR'nin 16 üretim eklentisiyle ayrı loopback test sunucusunda test edildi. Gönderilen eski Hub 1.2.0 JAR'ı çalıştırılmadı veya dağıtıma konmadı. Özgün kaynak ve iki harita incelendi; güncel Hub API ve kimlik doğrulama akışına uyarlandı.

- Hub 1.6.2: 24; Boya 1.1.0: 3; toplam 27 birim testi. Eklenen HUD/Role sürümlerinin 24 test ve 10 canlı kontrol kaydı kendi sürüm doğrulamasında bulunur; JAR'ları değiştirilmedi.
- Sekiz gerçek protokol bağlantısıyla 10 kontrol grubu: on oyun kartı, otomatik iki dünya, lobide kalma, harita doğrulama, normal maç/denk takımlar, güvenlik ve soluk HUD; gerçek use-item ile 13 hücrelik atış; aynı hücreye tekrar atışta puanın şişmemesi; gerçek rakip atışıyla alanın transferi; hasarsız/yavaşlatan isabet, takım arkadaşı istisnası; takım galibiyeti ve kayıt; sonuç/lobi/temizlik; üç eşzamanlı oda ve dördüncü bekleme; boya/mermi oda yalıtımı; denemelerin puansızlığı; grup ve lider kontrolü; diğer oyun lobisine doğrudan geçiş; 15 dk çıkış cezası; bağlantı kurtarma ve orijinal envanter.
- Yeniden başlatma: çevrimdışı yedek kurtarma, sıfır geçici dünya, pusula ve bright lobi HUD uyumu.

Süre geçişleri yalnız loopback adaptöründe saat alanları ilerletilerek sınandı; gerçek üretim ayarları değiştirilmedi. Gerçek atış testleri Minecraft `use_item` paketinden başlayıp Paper'ın fizik/ProjectileHit yoluyla boya alanına ulaştı. Bazı hasar/koruma ve çapraz oda isabet olayları Paper ana iş parçacığında test adaptörüyle tetiklendi. Görsel istemci incelemesi ve yük testi yapılmadı.

Kaynaklar: `input-and-map-manifest.json` iki özgün ZIP'in ve gömülü 11 dünya dosyasının SHA256 değerlerini içerir. Paketleyici JAR'ları test sunucusundaki son sürümlerle, HUD/Role dosyalarını önceki immutable yayınlarıyla ve gömülü haritaları kaynakla karşılaştırır. CRC ve sınıf paket sınırları kontrol edilir; Probe sınıfları üretim ZIP'ine alınmaz.

Tekrar: Maven ile önce Hub, sonra Boya `install`; `python3 kasaba-boya/scripts/integration/build-probe.py`. Özel test sunucusu açılınca `node kasaba-boya/scripts/integration/live.cjs`; tam durdurup yeniden açınca `node kasaba-boya/scripts/integration/restart.cjs`. Son olarak `python3 kasaba-boya/scripts/package-release.py --tested-server /workspace/scratch/kasaba-network-test-server`. Mevcut loopback Network/Bilgi/Esya adaptörleri bu test ortamında bulunmalıdır.

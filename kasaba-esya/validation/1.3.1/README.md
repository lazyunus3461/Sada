# KasabaEsya 1.3.1: ayrı lobi

Lobi, Oyuncakçı haritasının önünden alınarak `KasabaTR-Esya-Lobi-v01` adlı ayrı boşluk dünyasına taşındı. Lobi pencerelerinden oyun haritaları görülmez; bunlar başka dünyalardadır. Pusula, komut, maç sonucu ve güvenlik dönüşleri tek yeni lobi noktasını kullanır. Bilgi ve ilk beş yazıları yeni dünyada gösterilir. Oyun haritaları ve eski ayarlar korunur. Yeni lobi yalnız ilk açılışta inşa edilir; yönetici düzenlemeleri sonraki açılışlarda kalır.

Paper 1.21.10 build 130 / Java 21 / Multiverse 5.3.2 / KasabaTR 14 üretim eklentisiyle ayrı loopback sunucusunda doğrulandı:

- 3 KasabaEsya birim testi başarılı. Değişmeyen Hub'ın 24 test kaydı önceki doğrulamadan korundu.
- Sekiz oyuncuyla 14 canlı kontrol grubu: normal maç/sonuç, iki kişilik deneme, üç eşzamanlı oda, kılık ve gerçek sağ/sol tık, grup, çıkış cezası, başka lobiye geçiş, envanter ve bağlantı kurtarma.
- Yeni lobinin tüm zemin alanı ve çevre duvarları kontrol edildi. Oyun şablonları farklı dünyada; lobi dışındaki örnek chunk boş. Bilgi ve ilk beş için iki TextDisplay yeni lobide bulunuyor.
- Maç sonrası dönüşün yeni lobiye olduğu ayrıca kontrol edildi.
- Tam yeniden başlatmada lobiye giriş, oyuncu yedeği, skorlar ve Arena gidiş-dönüşü tekrar sınandı. Yeni lobi zeminine test için konan zümrüt blok açılışta korunarak yönetici düzenlemelerinin silinmediği doğrulandı.

Süre geçişleri yalnız test adaptöründe saat alanları ilerletilerek tetiklendi; üretim süreleri değiştirilmedi. Görsel istemci incelemesi ve yük testi yapılmadı. Test yardımcıları üretim paketine dahil değildir. Üç gömülü oyun haritasının dosyaları önceki KasabaEsya 1.3.0 JAR'ıyla paketlemede byte byte karşılaştırılır.

Tekrar çalıştırma: Java 21 ile `mvn -f kasaba-esya/pom.xml install`; loopback adaptörü için `python3 kasaba-esya/scripts/integration/build-probe.py`. Özel test sunucusu açılınca `node kasaba-esya/scripts/integration/live.cjs`; durdurup tekrar açınca `node kasaba-esya/scripts/integration/restart.cjs`. Sonra `python3 kasaba-esya/scripts/package-lobby-release.py --tested-server /workspace/scratch/kasaba-network-test-server` kullanılır. Bu komutlar mevcut Network/Arena/Followup loopback adaptörleri bulunan test ortamı içindir.

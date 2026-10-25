# Lanetli Taç + dar tablo + Eşya uçuşu doğrulaması

Paper 1.21.10 build 130 / Java 21, mevcut 17 üretim eklentisi ve Multiverse-Core 5.3.2 ile özel loopback sunucusu. Yeni oyun mod/kaynak paketi gerektirmez.

- **34 birim testi**, sıfır hata: Hub 24, Esya 3, Bilgi 4, Tac 3.
- **8 gerçek protokol istemcisiyle 10 canlı grup**, ardından son üretim JAR'larıyla temiz yeniden başlatmada 3 ek grup. Oyuncu protokolüyle doğrulanan native pusula, gizlilik/oyun HUD'u, uçuş yetenek paketi, taç/puan/sonuç, üç oda, grup, kaçış cezası ve envanter kurtarma.
- Esya'nın geniş regresyonu `../../../../kasaba-esya/validation/1.3.2` altında ayrıca 14 grup + yeniden başlatma: üç harita, iki türlü yakalama, duvar görüşü, kılık aralığı, galibiyet, grup ve durum geri yükleme.
- Saklanan oyuncu native uçuş paketini gönderir; yerleşim yanında otomatik hizalanır. Duvar, tavan ve izin verilen yükseklik dışında uçuş başlatma engellenir. Yakalanınca/lobide uçuş kapanır; eşyaları geri gelir.
- Dar tablo X Arena öldürme değerini isimden önce korur; uzun isim/süre paneli gereksiz genişletmez. Saydamlık/GUI ölçeği istemcinin kontrolündedir.
- Yüklenen haritadaki eski zincir blok kimliği düzeltildi; son yeniden başlatmada gerçek dünyada `IRON_CHAIN` doğrulandı. Kalıcı şatoya maç kapıları yazılmaz.

Süreleri aşmak için yalnız loopback test adaptörleri odanın süre alanlarını ilerletir. Üretim yapılandırmasındaki 20 sn / 240 sn / 30 sn değerleri değiştirilmez. Yer değiştirme, çarpışma ve event güvenlik kontrollerinin bir bölümü ana-thread fixture ile hazırlanır. Haritanın grafik Minecraft istemcisinde piksel/oynanış değerlendirmesi ve açık sunucu yük testi yapılmadı.

Test adaptörleri ve test oyuncusu verileri üretim JAR'larına/ZIP'e gömülmez. Paketleyici, test edilen JAR hash'lerini ve gömülü dünya kaynaklarını birebir karşılaştırır; yayımlanmış eski indirmeler değiştirilmez.

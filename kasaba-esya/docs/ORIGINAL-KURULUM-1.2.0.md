# KasabaTR Eşya Saklambacı — üç harita

## Kurulum

1. Sunucuyu kapat ve mevcut `KasabaEsya` JAR dosyanı yedekleyip `plugins` klasöründen kaldır. Aynı eklentinin iki sürümünü bir arada bırakma.
2. `KasabaTR-Esya-Saklambaci-TAM-PAKET-v1.2.0.zip` dosyasını **sunucu ana klasörüne** çıkar. `plugins/KasabaEsya-1.2.0.jar` ve üç dünya klasörü doğru yerlere gelir.
3. Sunucuyu aç. Paper **1.21.10** ile KasabaHub **1.2.0** ve onun gerektirdiği KasabaAuth kurulu olmalı.
4. Oyuncular `/oyunlar` menüsünden **Eşya Saklambacı** seçer; `/esya katil` de kullanılabilir.

Önceden kurulmuş oyuncakçı dünyası varsa ZIP'i açmadan önce onu yedekle. Oyuncakçı dünyası v02 olarak aynı kalır. Eski `plugins/KasabaEsya/config.yml` dosyasındaki tek `template-world` ayarı yeni üçlü listeyi engellemez; JAR içindeki `template-worlds` varsayılanı kullanılır. Özel bir harita listesi istiyorsan config'te üç dünya klasörünü `template-worlds` altında yaz.

## Haritalar ve oyun

- **Oyuncakçı:** yoğun raf bölümleri ve depo; 84 yem, 176 olası konum.
- **Müze:** merkez rotunda, dört sergi kanadı, geçitli galeriler; 72 yem, 180 olası konum.
- **Akvaryum:** dört su galerisi, dolanan kuru ziyaretçi yolları ve büfeler; 72 yem, 180 olası konum.
- Her oda açılırken üç haritadan biri rastgele seçilir. Rastgele karıştırılan üçlü sıra, her haritanın tekrar seçilmeden önce bir kez gelmesini sağlar. Aynı harita yeni üçlü sıranın başında yeniden gelebilir.
- Dört oyuncu gelince 15 saniyelik sayaç başlar. 4–7 oyuncuda 1, 8–12 oyuncuda 2 ebe olur. Ebeler 20 saniye bekler; saklananlar haritaya özgü eşyalara dönüşebilir.
- Yem eşyaların konumu her maç yeniden seçilir. Ebeler yanlış tahminde 3 saniye yavaşlar. Saklananların tamamı bulunursa ebeler; en az biri 4 dakika dayanırsa saklananlar kazanır.
- Her maç ayrı bir geçici dünya kopyasında oynanır. Maç bitince kopya kaldırılır; üç şablon dünya korunur.

## Yönetici kontrolü

Sunucu konsoluna `esya testharita` yaz. Komut üç dünyayı ayrı ayrı kopyalar, başlangıç yemlerini, rastgele yem yerlerini ve sayılarını doğrular. Başarı mesajı **her üç harita için** görünmelidir. Gerçek oyuncularla tam maç akışı yerel ortamda oynanmadı.

# KasabaSpawn 1.0.0

Paper 1.21.10 ve Java 21 için küçük, bağımsız bir dünya oluşturma eklentisi. Dümdüz zemindeki 64×64 alana kasaba meydanı kurar. KasabaHub, Auth veya WorldEdit kurulumu istemez.

## Görünüm

- Ortada geometrik taş desenli meydan ve aydınlatmalı su çeşmesi.
- Kasaba Konağı, Fırın, Demirci, Han ve renkli tenteli Pazar.
- Çeşitli malzemelerle kaplı çatılar, ahşap çerçeveli pencereler, giriş tenteleri ve içeride eşya/dolap ayrıntıları.
- Dört küçük açık pazar tezgâhı, banklar, çiçeklikler, kalıcı yapraklı ağaçlar, zincirli fenerler.
- Çevre duvarı, aydınlatması ve kemerli dört kapısı.

## Kurulum ve kullanım

1. [KasabaSpawn JAR'ını indir](https://github.com/lazyunus3461/Sada/raw/refs/heads/main/dist/KasabaSpawn-1.0.0.jar).
2. Sunucuyu durdur. JAR'ı `plugins` klasörüne koy ve sunucuyu başlat.
3. Düz ve boş kurulacak yerin **tam ortasında**, zemin seviyesinde dur. Bölgeyi 64×64 blok alan, çevresini boş bırak. OP olarak `/kasabaspawn yap` yaz.

Örnek komut:

```text
/kasabaspawn yap
```

Komut, merkezde durduğun bloktan X/Z yönünde 32 blok bir yana ve 31 blok diğer yana alanı kapsar. Altında sağlam, düz zemin; üstünde tamamen boş **16 blokluk** yükseklik olmalıdır. Başka oyuncular da alanın dışına çıkmalıdır. Eklenti bu koşulları kontrol eder; yer veya boşluk uygun değilse hiçbir bloğu değiştirmeden nedenini söyler. Dünya sınırına sığmazsa başlamaz.

Onaydan sonra çevredeki dünya parçalarını yükler ve yapıyı oyun takılmasın diye saniyede yaklaşık 7.000 blok hızla kademeli yerleştirir. Kurulum bitince dünya doğma noktası çeşmenin güneyindeki meydana alınır ve komutu kullanan oyuncu oraya geçer. İşlem sunucu kapanırsa yarım kalabilir; bitene kadar bekle.

KasabaHub varsa komuttan sonra durduğun yerde `/merkez lobi ayarla` yaz. KasabaAuth kullanıyorsan aynı yerde `/kayityeri ayarla` yaz ki kayıt/giriş tamamlanan oyuncular bu meydana gelsin. İkisini de kullanmıyorsan vanilla doğma noktası zaten ayarlanmıştır.

`/kasabaspawn bilgi` ölçüyü ve komutu gösterir. Eklenti yalnızca OP izniyle çalışır. Mevcut binaların üzerine kurulum yapmaz; boşluk kontrolü 64×64 alanın tamamında yapıldığı için mevcut bir yapının üstüne yanlışlıkla basamaz. Komutu tekrar çalıştırmak yeni durduğun yerin 64×64 alanını kontrol eder.

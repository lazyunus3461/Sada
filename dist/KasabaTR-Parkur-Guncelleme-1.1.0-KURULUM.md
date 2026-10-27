# Parkur 1.1.0 ve Bilgi Tablosu 1.0.5

## Kurulum

1. Sunucuyu durdur.
2. `plugins/` içindeki eski **KasabaParkur**, **KasabaBilgi** ve **KasabaHub** JAR dosyalarını
   kaldır; bu eklentilerin veri klasörlerini ve dünyaları silme.
3. Paketteki `KasabaParkur-1.1.0.jar`, `KasabaBilgi-1.0.5.jar` ve `KasabaHub-1.9.4.jar` dosyalarını
   `plugins/` içine koy ve sunucuyu başlat. `/reload` kullanma.

Paper 1.21.10 / Java 21 ve mevcut KasabaHub/KasabaAuth altyapısı gerekir.
Mevcut parkur dünyası aynen kullanılır. İlk kurulumda dünya JAR içinden otomatik
yüklenir. Yeniden harita yüklemek veya spawn ayarlamak gerekmez.

## Yeni parkur kuralları

- Kolay, orta ve zor rotalarına istediğin zaman girilir; maç veya kuyruk yoktur.
- **Ara kayıt ve kontrol noktası kaydı yoktur.** Ara bloklara sırayla tek tek
  basmak gerekmez; normal atlayışlarla serbestçe ilerlenir.
- Düşünce önce biraz aşağı düşersin, sonra **seçtiğin rotanın en başına** dönersin.
  Kronometre sıfırlanır ve yeni bir deneme başlar.
- Oyundan çıkıp tekrar girince de rotaya en baştan başlanır.
- Bitişe ulaşınca dakika, saniye ve milisaniye olarak süren gösterilir.
- Kolay, orta ve zor için **ayrı en hızlı ilk 5** vardır. Her oyuncunun her
  zorluktaki yalnız en iyi bitiriş süresi saklanır. Yavaş bir tekrar eski
  dereceni bozmaz; eşit sürelerde önce yapılan derece öndedir.
- Kendi bitiriş süren ve en iyi derecene göre sıralamadaki yerin ekranda görünür;
  parkur/lobideki oyunculara da duyurulur.
- Uçuş, dışarıdan ışınlanma ve yönetici denemesi süre sıralamasına yazılmaz.
- Eski `ilerleme.properties` dosyası okunmaz veya değiştirilmez; eski ara
  ilerlemelerden devam edilmez. Yeni `sure-rekorlari.json` yalnız bitiriş
  rekorlarını tutar; devam noktası tutmaz.

## Komutlar

- `/parkur`: Parkur lobisi ve zorluk menüsü.
- `/parkur kolay`, `/parkur orta`, `/parkur zor`: Rotaya sıfırdan başla.
- `/parkur dön`: Rotanın başına dön; kronometre yeniden başlar.
- `/parkur baştan kolay`: Kolay rotaya yeni deneme başlat; süre rekorun korunur.
- `/parkur lobi`: Parkur lobisine dön.
- `/parkur sıralama` veya `/parkur ilerleme`: En iyi sürelerin ve ilk 5 listeleri.
- `/lobi`: Ana lobi; parkurdan ayrılma cezası yoktur.
- Yönetici: `/parkur deneme zor 50` — yalnız deneme; süre kaydedilmez.

## Sağ panel

Kısaltılmış beş satırlı görünüm geri alındı. **Oyuncu, Rütbe, Akçe, Klan ve Süre**
etiketleri, ayrı renkler, eski başlık ve iletişim satırları geri geldi. Aktif
oyunun kendi skor tablosu varsa ayrı tutulur; özel rol bilgileri değiştirilmez.

Hub içindeki parkur rehberi de yeni kurallara göre güncellendi. Pakette bu üç
eklenti değiştirilir. IP onayları, Akçe, diğer oyunların
ayarları ve mevcut dünya yapıları için yeni ayar gerekmez.

Doğrulama kapsamı `TEST-SONUCLARI.json` içindedir. Protokol istemcisi testleri,
300 bölümün bir insan tarafından baştan sona oynanması anlamına gelmez.

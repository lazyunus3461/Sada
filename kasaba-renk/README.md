# Renk Kaçışı — KasabaTR için yeni oyun

Paper **1.21.10 / Java 21**, KasabaHub **1.3.0** ve KasabaAuth **1.3.0**.
Oyun dünyası ve üç platform JAR tarafından otomatik hazırlanır.

## Kurulum ve ilk test

1. Sunucuyu durdur. `KasabaRenk-1.0.0.jar` dosyasını `plugins` içine koy.
   Tam sunucu güncelleme ZIP'ini kullanıyorsan bu JAR zaten içindedir.
2. Başlat. **`/oyunlar` → Renk Kaçışı** seç.
3. Pusulaya sağ tıkla ve **Oda 1** seç. Arkadaşın da aynı odayı seçsin.
4. İki kişi gelince 10 saniye geri sayar. Odaya ilk giren kişi
   **`/renk başlat`** ile hemen başlatabilir.
5. Tek başına yönetici testi: odayı seç, **`/renk deneme`** yaz.

Başka dünya veya config taşıman gerekmez. Yeni dünya adı **`renkkacisi`**.
Multiverse varsa dünya otomatik kaydedilir. Mevcut aynı adlı, bu eklentiye
ait olmayan dünya varsa eklenti üzerine yazmaz.

## Nasıl oynanır?

- Ekranda örneğin **MAVİ** yazar. Eline de aynı renk blok gelir.
  Aynı renk zemine koş: süre bitince diğer renkler kaybolur.
- Her oyuncunun **2 hata hakkı** vardır. İlk düşüşte bir hakkın gider;
  izleme platformunda bekler, sonraki tur geri dönersin.
- İkinci hatada seyirci olursun. Son kalan oyuncu kazanır.
- Renkler her tur karışır. Sekiz rengin her biri eşit alan kaplar;
  her renk satırlara dağılır, tek köşede toplanmaz.
- İlk tur 8 saniye verir. Sonraki turlarda süre 0,4 saniye azalır;
  en az 2,5 saniye kalır. Zemin düştükten sonra 3 saniyelik kontrol vardır.
- 15 tur sonunda birden fazla kişi kaldıysa önce kalan hak, sonra başarılı
  tur sayısı karşılaştırılır. Eşitlik beraberedir.
- Hedef hem **renk adı** hem **eldeki blok** ile gösterilir. Üst çubukta süre,
  alt yazıda haklar ve başarılı tur sayın görünür. Sesler turu ve hatayı bildirir.
- PvP, blok koyma/kırma, eşya taşıma ve aktif oyunda uçuş kapalıdır.

**3 oda aynı anda**, her biri **2–8 kişi**. Oyunlar ayrı 32×32 platformlarda
oynanır. Elenen seyirciler kendi platformunun çevresinde tutulur. Bekleyen
lobide arkadaşlar, gruplar ve oyuncu gizleme araçları kullanılabilir.

## Komutlar

| Komut | İşlev |
|---|---|
| `/renk` | Pusulayla aynı oda sayfası. |
| `/renk oyna` | Uygun bekleyen odayı seçer veya boş oda açar. |
| `/renk oda 1` | 1, 2 veya 3 numaralı oda. |
| `/renk başlat` | Odanın ilk oyuncusu en az iki kişiyle başlatır. |
| `/renk deneme` | Yalnız yönetici, tek kişilik antrenman. |
| `/renk lobi` | Odayı bırakır, Renk Kaçışı lobisine döner. |
| `/renk çık` veya `/lobi` | Ana spawna çıkar; normal eşya/modunu geri verir. |

Türkçe karakterli/ASCII komutlar kabul edilir. Yönetici yetkisi:
`kasaba.renk.admin` (varsayılan OP).

Grup kurulduğunda lider `/oyunlar` üzerinden Renk Kaçışı'nı seçer;
grup birlikte girer ve odayı lider seçer. Envanter, zırh, iksir, deneyim,
yedek kalp ve uçuş/mod için kalıcı kurtarma kaydı tutulur. Bağlantısı kopan
oyuncunun kaydı sonraki başarılı hesap girişinde geri yüklenir.

Bu oyun için ayrıca Akçe maç ödülü belirlenmedi. Mevcut aktif oynama süresi
ödülü sunucunun ortak sistemi tarafından uygulanır; seyirci süresi sayılmaz.
Kaynaklar, birim testleri ve yalnız yerel sunucuya yönelik test araçları bu
modüldedir. Test yardımcı JAR'ları sunucu paketine dahil değildir.

## 1.1.0 değişiklikleri

Hem oyun alanı hem renkli kareler küçülür: 1–3. turlarda 32×32 / 4×4,
4–6. turlarda 24×24 / 3×3, 7. turdan sonra 16×16 / 2×2. Küçülen alan
ortalanır; oyuncular güvenli şekilde yeni alanın içine yerleştirilir.
**Bu lobide kal** menüyü kapatır, bekleyen oda üyeliğini bırakır ve oyun
lobisinde tutar; ana spawna ışınlamaz.

## 1.2.0 itme kartopları

Her tur 1. kutuya 8 kartopu gelir; sağ tıkla atılır, iki saniye aralık vardır. İsabet rakibi iter; can götürmez. Farklı oda/tur, lobi veya seyirci etkilenmez. 5. kutudaki renk göstergesi korunur. Normal envantere taşınmaz.

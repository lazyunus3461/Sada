# KasabaArena 1.2.1

Paper **1.21.10 / Java 21**. KasabaHub ve onun kullandığı KasabaAuth gerekir.

[JAR'ı indir](https://github.com/lazyunus3461/Sada/raw/refs/heads/main/dist/KasabaArena-1.2.1.jar) · [Kurulum ZIP'i](https://github.com/lazyunus3461/Sada/raw/refs/heads/main/dist/KasabaArena-Kurulum.zip)

## Güncelleme

Sunucuyu durdur, eski KasabaArena JAR'ını `plugins` dışına çıkar ve 1.2.1 JAR'ını koy. `plugins/KasabaArena` klasörünü koru. Tam yeniden başlatma yap. Eski lobi, X Arena noktaları, kit ve öldürmeler korunur. Eski kırmızı/mavi başlangıçlar **1v1 oda 1** olarak aktarılır. Eski VIP kiti artık kullanılmaz; VIP sekmesi ve izni kaldırıldı.

## 1.2.1: ıskalanabilen yetenekler

**Hedef oyuncu olmadan da dört yeteneğin tamamı kullanılabilir.** Boşa atılan beceri de kendi bekleme süresini ve kısa ortak kullanım arasını harcar. Eşya kutuları 1–4, kılıç 5 olarak kalır; istediğin beceriyi seçebilirsin.

- Ağır Darbe, Kalkan Darbesi ve İnfaz nişan alınan dar çizgide 0,3–0,4 saniye hazırlıktan sonra vurur. Rakip yana kaçarsa ıskalar. Yön, kullanım anında sabitlenir; otomatik hedef takibi yoktur.
- Büyücünün Alev Küresi, Şifacının Işık Oku ve Zehirli Bıçak baktığın yöne ilerler. Rakip kaçabilir; duvar saldırıyı durdurur. Zehir yalnızca bıçak isabet ederse işler. Okçunun okları da fiziksel ok olarak ıskalanabilir.
- Ok Yağmuru ve Meteor, baktığın **zemin noktasına** atılır. İstediğin yeri hedeflemek için zemine doğru nişan al. Uyarı halkası görülür; ok yağmuru 0,8 sn, meteor 1 sn sonra düşer. Hedef alan oyuncuyu takip etmez.
- Yakın alan saldırıları 0,5–0,8 sn önce halka/ses uyarısı verir ve kullanım yerinde kalır; rakip halkadan çıkabilir. Duvar arkasına hasar geçmez.
- Hareket ve kendine destek becerileri de oyuncu aramaz. Işın Adımı duvara/güvensiz yere atılırsa bekleme süresi harcanır; duvarın içinden geçirmez.
- Karakter değiştirme, ölme veya X Arena'dan çıkma; bekleyen saldırıları ve uçan becerileri iptal eder. 1v1/2v2'ye yetenek taşınmaz. Hasar dengesi, güçlü zırhlar, market ve Akçe sistemi korunur.

## Önce lobi ve X Arena

Oyuncuların karşılanacağı güvenli yerde dur:

```text
/arena lobi ayarla
```

X Arena'nın farklı güvenli noktalarında bu komutu tekrarla:

```text
/arena doğma ekle xarena
```

İki veya daha fazla nokta varsa oyuncu ölümden sonra bir önceki noktadan farklı bir noktada doğar. X Arena sınırsız PvP'dir; ölümde eşyalar düşmez, karakter değiştirme sayfası açılır. Kapatılırsa mevcut karakter kalır. Sağdaki ilk beş öldürme tablosu kalıcı istatistikleri gösterir.

## Beş ortak haritada 1v1 ve 2v2

Birinci 1v1 haritasında oyuncuların başlayacağı iki farklı yere giderek sırayla yaz:

```text
/arena doğma ayarla 1v1 1 kırmızı
/arena doğma ayarla 1v1 1 mavi
```

İkinci haritada `1v1 2`, üçüncüde `1v1 3`, dördüncüde `1v1 4`, beşincide `1v1 5` kullan. Her komutta karakterin bulunduğu yer ve bakış yönü kaydedilir.

2v2 için aynı işlemi `2v2` ile yap:

```text
/arena doğma ayarla 2v2 1 kırmızı
/arena doğma ayarla 2v2 1 mavi
```

2v2 oda numaraları da 1–5 arasındadır. Takım arkadaşları başlangıcın çevresine aralıklı yerleştirilir.

Oyuncu `/arena 1v1` veya `/arena 2v2` yazınca, boş yeri olan açık odaya doğrudan ışınlanır. Önce dolmayı bekleyen oda doldurulur, sonra boş oda açılır. 1v1 odası iki, 2v2 odası dört oyuncu olunca maç başlar. Bütün odalar doluysa oyuncu lobide kalır; genel kuyruk yoktur. Durumlar `/arena oda liste` ile görünür.

**Aynı numaralı 1v1 ve 2v2 odaları ortak fiziksel haritayı kullanır.** Varsayılan `ayarlar.ortak-oda-numaralari: true` eski ayar dosyalarında da geçerlidir. Örneğin 1v1 oda 1'e tek oyuncu beklemeye bile girse 2v2 oda 1 kilitlenir; 2v2 girişi boş 2, 3, 4 veya 5 numaraya gider. Özel kodlu odalar da alanı kilitler. Son oyuncu çıkınca veya maç sonu lobiye dönülünce kilit açılır. Beş alan da doluysa oyuncu güvenli lobide kalır. On odanın ayrı fiziksel haritaları varsa bu ayarı `false` yapabilirsin. Ayrıca kayıtlı koruma bölgeleri kesişen farklı numaralı odalar da eşzamanlı açılmaz.

Maçlar en fazla üç raunttur; iki rauntu kazanan takım seriyi alır. 2v2'de bir takımın iki oyuncusu da ölünce raunt biter. Ölen oyuncu sonraki raunta kadar izleyicidir. Maç sonunda herkes güvenli Arena lobisine döner.

## Blok koruması

Haritanın bir köşesinde zeminin altına inip ilk köşeyi, çapraz karşı köşesinde duvarların üstüne çıkıp ikinci köşeyi seç. Komutlar karakterinin konumunu alır; yükseklik de seçime dahildir.

```text
/arena bölge pos1
/arena bölge pos2
/arena bölge kaydet 1v1 1
```

Beş ortak harita için iki köşeyi her haritada yeniden seçerek kaydet:

```text
/arena bölge kaydet ortak 1
/arena bölge kaydet ortak 2
/arena bölge kaydet ortak 3
/arena bölge kaydet ortak 4
/arena bölge kaydet ortak 5
```

Her satırdan önce o haritanın `pos1` ve `pos2` köşelerini seç. Ortak korumayı hem 1v1 hem 2v2 kullanır; iki defa kaydetmek gerekmez. Önceki `1v1 1` / `2v2 1` biçimleri de çalışır. `pos1` ve `pos2` bitişik yazılır; yanlışlıkla `pos 1` ve `pos 2` yazılması da artık kabul edilir.

Bir arenada birbirinden ayrı iki veya daha fazla koruma parçası varsa her parçayı ayrı seçip **sonuna farklı isim ekle**:

```text
/arena bölge kaydet ortak 1 ana
/arena bölge kaydet ortak 1 tribun
/arena bölge kaydet 1v1 2 giris
/arena bölge liste
/arena bölge sil ortak 1 tribun
```

İsim verilmezse o odanın ana koruması güncellenir; aynı isim yeniden kullanılırsa yalnızca o parça değişir. İki bölge sınırı yoktur. `sil` yalnızca belirtilen korumayı kaldırır, haritayı veya doğma noktalarını silmez. Odalar doluyken koruma değiştirilmez.

Lobi ve X Arena için ayrı köşeler seç:

```text
/arena bölge kaydet lobi
/arena bölge kaydet xarena
```

Lobide hasar kapalıdır; X Arena'da PvP açıktır. Kaydedilen alanlarda herkes için blok kırma/koyma ve patlama blok hasarı engellenir. Ayrıca Arena'ya katılmış oyuncuların lobi, oda ve X Arena sırasında blok kırması/koyması engellenir; OP olmak korumayı atlatmaz. İnşaatları korumayı kaydetmeden önce tamamla. WorldEdit/FAWE ile yapım işlemleri ayrı yönetici işlemleridir.

## Maç kiti

1v1 ve 2v2'de **yalnızca normal kit** kullanılır. Her girişte ve yeni rauntta envanter, ikinci el, eski karakter becerileri, iksir etkileri ve geçici korumalar temizlenir. X Arena market eşyaları bu odalara verilmez.

Arena oyunu dışındayken istediğin eşyaları envanterine koy, zırhını giy, ikinci el eşyasını tak ve yaz:

```text
/arena kit normal kaydet
```

Bütün 1v1/2v2 odaları bu kiti kullanır. Kit hazırlanmamışsa demir kılıç, demir zırh ve yiyecek verilir. Arena'dan `/lobi` ile tamamen çıkınca giriş öncesindeki kendi envanterin ve etkilerin geri yüklenir.

## X Arena karakterleri ve hasar

Karakterler ve dört becerileri yalnızca X Arena'da çalışır. Beceri eşyaları **1, 2, 3, 4. yuvalarda**, yakın dövüş silahı **5. yuvada** bulunur. Okçu yayı 6. yuvadadır. Eşyaya sağ tıkla veya `/arena yetenek 1`, `2`, `3`, `4` yaz.

**Sıra zorunluluğu yoktur.** Hazır olan her beceriyi istediğin zaman kullan. Kullanılan beceri kendi süresi boyunca bekler; diğerleri açık kalır. Aynı anda paket yağdırılmasını önlemek için kullanımlar arasında yalnızca 0,3 saniyelik ortak geçiş vardır. Hazır beceriler parlar; ekranın altında dört becerinin ayrı kalan süreleri görünür. Ölümde beklemeler sıfırlanır.

| Karakter | Beceri 1 | Beceri 2 | Beceri 3 | Beceri 4 | Beklemeler (sn) |
|---|---|---|---|---|---|
| Savaşçı | Ağır Darbe: 1,8 kalp | Kasırga: yakına 1,2 kalp | Savaş Narası: 5 sn %25 artış | Deprem: 2,1 kalp + yavaşlatma | 10 / 12 / 18 / 20 |
| Suikastçı | Gölge Atılımı: atılma + 1,2 kalp | Zehirli Bıçak: toplam 1,5 kalp | Sis Perdesi: 3 sn gizlenme/hız | İnfaz: 1,8; düşük canlıya 2,4 kalp | 10 / 14 / 16 / 20 |
| Tank | Çelik Kalkan: 3 sn %50 ek azaltma | Kalkan Darbesi: 1,2 kalp | Demir İrade: 2 kalp iyileşme | Sarsıntı: 1,5 kalp + yavaşlatma | 14 / 10 / 18 / 22 |
| Okçu | Güçlü Ok: 1,8 kalp | Ok Yağmuru: toplam 1,8 kalp | Geri Sıçrayış: kaçış/hız | Avcı Oku: 2,4 kalp | 10 / 14 / 10 / 20 |
| Büyücü | Alev Küresi: 1,35 kalp | Buz Çemberi: 0,9 kalp + yavaşlatma | Işın Adımı: güvenli 5 blok | Meteor: gecikmeli 2,1 kalp alan hasarı | 10 / 12 / 16 / 22 |
| Şifacı | Işık Oku: 1,05 kalp | Şifa: 2 kalp iyileşme | Koruyucu Işık: 3 sn %50 azaltma | Yaşam Dalgası: 1,5 kalp şifa + yakına 1,2 kalp | 10 / 16 / 18 / 24 |

Tablo standart X Arena zırhı uygulanmış, Tank savunması ve Savaş Narası uygulanmamış can kaybını gösterir. Her karakter Koruma II elmas zırh, Tank Koruma II netherit zırh giyer. X Arena hasar motoru zırh için %40 sabit azaltma uygular; vanilla zırh ikinci kez uygulanmaz. Tank buna ek %15 azaltma alır. Zırhlar ve silahlar kırılmaz. Normal yakın vuruş yaklaşık 0,225–0,45 kalptir; 0,9 saniyede bir vuruş kabul edilir. Normal ok 0,75 kalptir. Savaşçı'nın tüm saldırı becerileri ve narası birlikte tam canlı standart rakibi anında öldürmez.

Normal yemek kaynaklı hızlı yenilenme kapalıdır. Son vuruş veya beceriden 8 saniye sonra, iki saniyede yarım kalp iyileşme başlar. Beceri ve market iyileştirmeleri ayrıca çalışır. Büyücü'nün sıçraması duvarın içinden geçmez; boşluk üzerindeki güvensiz noktaya ışınlamaz. Oklar ve görüş gerektiren beceriler duvar arkasına vurmaz. Doğma koruması ayarlardan değiştirilebilir; saldıran oyuncunun kendi koruması kalkar.

Karakteri lobide `/arena sınıf` ile veya ölüm sonrası sayfadan seç. Dövüş ortasında karakter değiştirerek can/bekleme süresi sıfırlanamaz.

## X Arena Market

Menüde **Market** düğmesi vardır; `/arena market` de açar. Ürünler yalnızca X Arena'da verilir ve oradan çıkışta envanterden kaldırılır. Sahiplik kaydı korunur; tekrar girince kalan ürünler yeniden verilir.

Eline istediğin kılıcı al, Arena oyunu dışındayken üç günlük ürünü kaydet:

```text
/arena market ekle kilic 500 3 1
```

Bu örnek 500 Akçe, satın alma anından itibaren gerçek zamanla üç gün, bir kılıçtır. Çıkış yapmak süreyi durdurmaz. Aynı süreli kılıcı tekrar almak kalan süresini uzatır.

Eline bir elma al, on adetlik paket oluştur:

```text
/arena market ekle elma 100 0 10
```

Bu örnek 100 Akçe, 10 elmadır. Gün `0` süre sınırı olmadığı anlamına gelir. Kullanılan elma adedi kalıcı olarak azalır; ölüm ve yeniden giriş adedi sıfırlamaz. Normal market elması 1 kalp iyileştirir; altın elmanın kendi etkileri kullanılır. Süresi biten ürün X Arena içinde de otomatik kaldırılır. Süre ve sahiplik `market-sahiplik.yml` dosyasında tutulur.

```text
/arena market sil kilic
```

Bu komut ürünü satış listesinden kaldırır; önceden satın alınan ürünler süreleri boyunca korunur. Desteklenen ürünler kılıç, balta, yay, arbalet, kalkan ve yenebilir eşyalardır.

**[KasabaAkce 1.0.0](../kasaba-akce/README.md) kuruluysa market otomatik olarak gerçek Akçe bakiyesini kullanır.** İki JAR da kurulum paketindedir. `/akçe` bakiyeyi, `/akçe gönder <oyuncu> <miktar>` transferi açar. Akçe eklentisi kurulmazsa Arena çalışmaya devam eder; market ödeme yapmaz. Satın alma önce onay sayfası gösterir; kalıcı işlem kimlikleri aynı alışverişin çökme sonrası tekrar ücretlendirilmesini önler. Testlerde kullanılan taklit banka yalnızca yerel test yardımcısındadır; üretim JAR'ına dahil edilmez.

## Oyuncu komutları

| Komut | İşlem |
|---|---|
| `/oyunlar git arena` | Arena güvenli lobisine gir |
| `/arena` | Arena menüsü |
| `/arena 1v1` / `/arena 2v2` | Boş yeri olan odaya gir |
| `/arena oda liste` | On odanın durumunu gör |
| `/arena oda oluştur` | Boş 1v1 odasında özel kod oluştur |
| `/arena katıl 123456` | Arkadaşının özel odasına gir |
| `/arena xarena` | Lobiden X Arena'ya geç |
| `/arena sınıf` | X Arena karakter sayfası |
| `/arena yetenek 1` | Hazır beceriyi kullan; numaralar 1–4 |
| `/arena market` | X Arena market sayfası |
| `/arena lobi` | Güvenli Arena lobisine dön |
| `/lobi` | Arena'dan tamamen çık, Hub ana lobisine dön |

Yönetim komutları `kasabaarena.yonet`, KasabaRoles yönetimi veya OP gerektirir. Yerel regresyon ve yeniden başlatma testi: `bash kasaba-arena/scripts/integration/build-probe.sh`, ardından yalnızca test sunucusunda `node kasaba-arena/scripts/integration/regression.cjs`. Sunucuyu tamamen yeniden başlatıp `node kasaba-arena/scripts/integration/restart.cjs` çalıştır. Yalnızca loopback test sunucusunu kullan; fixture JAR üretim paketine konmaz.

## 1.3.3

X Arena'da son alınan hasardan 10 saniye sonra her 2 saniyede yarım kalp yenilenir; saldırmak veya beceri kullanmak bu sayacı uzatmaz. Görünmezlik sırasında zırh ve iki eldeki eşyalar rakiplere gizlenir; kendi envanteri korunur. Süre bitince ve çıkışta görünüm geri gelir. Yarış lobisiyle çakışan Arena lobi konumu ayrı `kasaba_arena_lobi` dünyasına taşınır; oda doğmaları ve bölge kayıtları korunur, eski config yedeklenir. `/arena lobi ayarla` ile kendi yerini yine seçebilirsin. Lobi pusulası envanterden açılır ve diğer oyun lobilerine geçiş içerir. Başlamış 1v1/2v2 maçını terk etmek Arena'ya 15 dakika giriş cezası verir; sürekli X Arena'dan çıkış cezasızdır.

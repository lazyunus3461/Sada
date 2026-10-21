# KasabaArena 1.1.0

Paper **1.21.10 / Java 21**. KasabaHub ve onun kullandığı KasabaAuth gerekir.

[JAR'ı indir](https://github.com/lazyunus3461/Sada/raw/refs/heads/main/dist/KasabaArena-1.1.0.jar) · [Kurulum ZIP'i](https://github.com/lazyunus3461/Sada/raw/refs/heads/main/dist/KasabaArena-Kurulum.zip)

## Güncelleme

Sunucuyu durdur, eski KasabaArena JAR'ını `plugins` dışına çıkar ve 1.1.0 JAR'ını koy. `plugins/KasabaArena` klasörünü koru. Tam yeniden başlatma yap. Eski lobi, X Arena noktaları, kit ve öldürmeler korunur. Eski kırmızı/mavi başlangıçlar **1v1 oda 1** olarak aktarılır. Eski VIP kiti artık kullanılmaz; VIP sekmesi ve izni kaldırıldı.

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

## Beş 1v1 ve beş 2v2 odası

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

Maçlar en fazla üç raunttur; iki rauntu kazanan takım seriyi alır. 2v2'de bir takımın iki oyuncusu da ölünce raunt biter. Ölen oyuncu sonraki raunta kadar izleyicidir. Maç sonunda herkes güvenli Arena lobisine döner.

## Blok koruması

Haritanın bir köşesinde zeminin altına inip ilk köşeyi, çapraz karşı köşesinde duvarların üstüne çıkıp ikinci köşeyi seç. Komutlar karakterinin konumunu alır; yükseklik de seçime dahildir.

```text
/arena bölge pos1
/arena bölge pos2
/arena bölge kaydet 1v1 1
```

Her oda için yeniden iki köşe seçip son komuttaki türü ve numarayı değiştir: örneğin `2v2 4`. Lobi ve X Arena için:

```text
/arena bölge kaydet lobi
/arena bölge kaydet xarena
```

Kaydedilen alanlarda herkes için blok kırma/koyma ve patlama blok hasarı engellenir. Ayrıca Arena'ya katılmış oyuncuların lobi, oda ve X Arena sırasında blok kırması/koyması engellenir; OP olmak korumayı atlatmaz. WorldEdit/FAWE ile yapım işlemleri ayrı yönetici işlemleridir.

## Maç kiti

1v1 ve 2v2'de **yalnızca normal kit** kullanılır. Her girişte ve yeni rauntta envanter, ikinci el, eski karakter becerileri, iksir etkileri ve geçici korumalar temizlenir. X Arena market eşyaları bu odalara verilmez.

Arena oyunu dışındayken istediğin eşyaları envanterine koy, zırhını giy, ikinci el eşyasını tak ve yaz:

```text
/arena kit normal kaydet
```

Bütün 1v1/2v2 odaları bu kiti kullanır. Kit hazırlanmamışsa demir kılıç, demir zırh ve yiyecek verilir. Arena'dan `/lobi` ile tamamen çıkınca giriş öncesindeki kendi envanterin ve etkilerin geri yüklenir.

## X Arena karakterleri ve hasar

Karakterler ve dört becerileri yalnızca X Arena'da çalışır. Beceri eşyaları hotbar'ın **5–8 numaralı yuvalarındadır**. Eşyaya sağ tıkla veya `/arena yetenek 1`, `2`, `3`, `4` yaz.

Beceriler **1 → 2 → 3 → 4 → 1** sırasıyla açılır. Sıradaki hazır beceri parlar; ekranın altı sonraki beceriyi ve kalan süreyi gösterir. Her becerinin yeniden kullanım süresi 10 saniyedir. Kullanımlar arasında karaktere göre 1,25–3 saniye geçiş süresi vardır; sonraki beceri hazır olana kadar zincir bekler. Ölümde zincir başa döner.

| Karakter | Beceri 1 | Beceri 2 | Beceri 3 | Beceri 4 |
|---|---|---|---|---|
| Savaşçı | Ağır Darbe: 3 kalp | Kasırga: yakına 2 kalp | Savaş Narası: 5 sn %25 hasar artışı | Deprem: yakına 3,5 kalp + yavaşlatma |
| Suikastçı | Gölge Atılımı: ileri atılma + yakına 2 kalp | Zehirli Bıçak: toplam 2,5 kalp | Sis Perdesi: 3 sn görünmezlik/hız | İnfaz: 3 kalp; düşük canlıya 4 kalp |
| Tank | Çelik Kalkan: 3 sn %50 ek azaltma | Kalkan Darbesi: 2 kalp + geri itme | Demir İrade: 3 kalp iyileşme | Sarsıntı: yakına 2,5 kalp + yavaşlatma |
| Okçu | Güçlü Ok: isabette 3 kalp | Ok Yağmuru: üç dalgada toplam 3 kalp | Geri Sıçrayış: kaçış/hız | Avcı Oku: isabette 4 kalp |

Bunlar Tank savunması ve Savaş Narası uygulanmadan önceki değerlerdir. Normal zırh bu açıklanan beceri hasarını bir kez daha düşürmez. Tank'ın kalıcı hasar azaltması %15'tir; Çelik Kalkan ile toplam azaltma %57,5 olur. Okların rastgele boşa çıkma olasılığı yoktur; ıskalayan veya engellere çarpan ok hasar vermez.

Yakın vuruşlar karaktere göre yaklaşık 0,375–0,75 kalptir ve 0,9 saniyelik vuruş arası vardır. Market kılıcı en fazla 0,25 kalp ekleyebilir; yakın vuruş toplamı 1 kalbi aşmaz. Normal ok 1,25 kalptir. X Arena'da tokken kendiliğinden can yenilenmesi kapalıdır; iyileştiren beceriler ve tüketilen ürünler çalışır. Doğma koruması ayarlardan değiştirilebilir; saldıran oyuncunun kendi doğma koruması kalkar.

Karakteri lobide `/arena sınıf` ile veya ölüm sonrası sayfadan seç. Dövüş ortasında karakter değiştirerek can/bekleme süresi sıfırlanamaz.

## X Arena Market

Menüde **Market** düğmesi vardır; `/arena market` de açar. Ürünler yalnızca X Arena'da verilir ve oradan çıkışta envanterden kaldırılır. Sahiplik kaydı korunur; tekrar girince kalan ürünler yeniden verilir.

Eline istediğin kılıcı al, Arena oyunu dışındayken üç günlük ürünü kaydet:

```text
/arena market ekle kilic 500 3 1
```

Bu örnek 500 para, satın alma anından itibaren gerçek zamanla üç gün, bir kılıçtır. Çıkış yapmak süreyi durdurmaz. Aynı süreli kılıcı tekrar almak kalan süresini uzatır.

Eline bir elma al, on adetlik paket oluştur:

```text
/arena market ekle elma 100 0 10
```

Bu örnek 100 para, 10 elmadır. Gün `0` süre sınırı olmadığı anlamına gelir. Kullanılan elma adedi kalıcı olarak azalır; ölüm ve yeniden giriş adedi sıfırlamaz. Normal market elması 1 kalp iyileştirir; altın elmanın kendi etkileri kullanılır. Süresi biten ürün X Arena içinde de otomatik kaldırılır. Süre ve sahiplik `market-sahiplik.yml` dosyasında tutulur.

```text
/arena market sil kilic
```

Bu komut ürünü satış listesinden kaldırır; önceden satın alınan ürünler süreleri boyunca korunur. Desteklenen ürünler kılıç, balta, yay, arbalet, kalkan ve yenebilir eşyalardır.

**Para eklentisi bu sürüme dahil değildir.** Ürün ekleme ve market sayfası hazırdır. Sonraki para eklentisi [ArenaEconomy servisini](src/main/java/tr/kasaba/arena/api/ArenaEconomy.java) sağlayınca satın alma açılır. Servis yokken para veya ürün verilmez. Satın alma önce onay sayfası gösterir; kalıcı işlem kimlikleri aynı alışverişin çökme sonrası tekrar ücretlendirilmesini önler. Testlerde kullanılan taklit banka yalnızca yerel test yardımcısındadır; üretim JAR'ına dahil edilmez.

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
| `/arena yetenek 1` | Sıradaki beceriyi kullan; numaralar 1–4 |
| `/arena market` | X Arena market sayfası |
| `/arena lobi` | Güvenli Arena lobisine dön |
| `/lobi` | Arena'dan tamamen çık, Hub ana lobisine dön |

Yönetim komutları `kasabaarena.yonet`, KasabaRoles yönetimi veya OP gerektirir. Yerel regresyon testi: `bash kasaba-arena/scripts/integration/build-probe.sh`, ardından yalnızca test sunucusunda `node kasaba-arena/scripts/integration/regression.cjs`.

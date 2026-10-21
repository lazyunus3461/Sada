# KasabaArena 1.0.0

Paper 1.21.10 / Java 21 için Arena eklentisi. KasabaHub'ın oyun sayfasına **Arena** olarak eklenir. Menü Minecraft'ın yerleşik sayfasında açılır; oyun seçimi için envanter menüsü kullanılmaz.

## Kurulum

1. Sunucuyu tamamen kapat.
2. `KasabaAuth`, `KasabaHub` ve `KasabaArena` JAR dosyalarını sunucunun `plugins` klasörüne koy. Hub için kullandığın Auth ve Hub sürümlerini değiştirme; `KasabaArena-1.0.0.jar` dosyasını ekle.
3. Sunucuyu Java 21 ve Paper 1.21.10 ile başlat. Konsolda KasabaHub ve KasabaArena etkinleşmeli.
4. OP hesabıyla oyuna girip aşağıdaki Arena konumlarını belirle.

## İlk kurulum komutları

Oyuncuların `/oyunlar arena` ile geleceği güvenli Arena lobisinde dur:

```text
/arena lobi ayarla
```

1v1 haritasında kırmızı ve mavi başlangıç yerlerinde durup ayrı ayrı yaz:

```text
/arena doğma ayarla kırmızı
/arena doğma ayarla mavi
```

X Arena savaş alanında oyuncuların yeniden doğabileceği farklı noktalara gidip her birinde yaz. En az bir nokta gerekir; rastgelelik için birkaç nokta ekle:

```text
/arena doğma ekle xarena
```

Haritayı `//set` ile hazırladıktan sonra blok kırmayı/koymayı da kilitlemek için köşe noktalarını seçip bölgeyi kaydet. Lobi ve X Arena için bu işlemi ayrı yap:

```text
/arena bölge pos1
/arena bölge pos2
/arena bölge kaydet lobi
```

```text
/arena bölge pos1
/arena bölge pos2
/arena bölge kaydet xarena
```

`pos1` ve `pos2` yazarken karakterin bulunduğu blok köşe olarak alınır. Lobi oyuncuları oyun içindeyken her yerde hasar alamaz ve blok kıramaz/koyamaz. X Arena oyuncuları birbirine vurabilir; kaydedilen X Arena bölgesinde blok kıramaz/koyamaz.

## Oyun

- `/oyunlar arena`: güvenli Arena lobisine geçer. Bu noktada Arena pusulası görünür; sağ tık veya `/arena` menüyü açar.
- **X Arena:** sınırsız PvP. Öldüğünde X Arena'dan çıkmadıysan ayarlı noktalardan rastgele birinde yeniden doğarsın. Eşyalar yere düşmez. Beş kişilik öldürme tablosu sürekli güncellenir ve istatistikler dosyada kalır.
- **Özel 1v1:** menüden oda oluştur; gösterilen altı haneli kodu arkadaşına ver. Arkadaşın Arena lobisindeyken `/arena katıl <kod>` kullanır. İlk iki rauntu kazanan maçı alır; seri en fazla üç raunt sürer. Maç tamamlanınca ikiniz Arena güvenli lobisine dönersiniz.
- **2v2:** dört oyuncu sıraya girince otomatik eşleşir; maç üç raunt üzerinden oynanır.
- Maç alanında `/arena` ile sınıf/kit sayfasını açabilirsin. Savaşçı, Suikastçı, Tank ve Okçu yetenekleri eşya sağ tıkıyla veya `/arena yetenek` ile kullanılır. Ölümden sonra sınıf seçme sayfası gelir; kapatırsan mevcut sınıfın kalır.
- `/arena lobi`: maçtan/X Arena'dan güvenli Arena lobisine döner. `/lobi`: Arena'dan çıkarak KasabaHub ana lobisine döner.

## Kitler ve yetkiler

Normal veya VIP kitini düzenlemek için istediğin eşyaları ve zırhı envanterine koy, sonra:

```text
/arena kit normal kaydet
/arena kit vip kaydet
```

VIP kitine erişim için `kasabaarena.kit.vip` yetkisini oyuncunun rolüne ver. KasabaRoles kullanıyorsan:

```text
/rol yetki vip kasabaarena.kit.vip ver
```

Yönetim komutları `kasabaarena.yonet` izni veya OP gerektirir. KasabaRoles yöneticisi de Arena noktalarını yönetebilir. Komutların özeti `/arena yardım`.

Eşyaların Arena'ya girişte yedeklenir ve `/lobi` ile çıkışta geri verilir. Sunucu kapanıp açılsa bile yarım kalan Arena oturumu yedeği girişte geri yüklenir.

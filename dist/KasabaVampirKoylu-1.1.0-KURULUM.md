# Kasaba Vampir Köylü 1.1.0

Paper **1.21.10**, Java **21**, KasabaHub **1.2.0** ve KasabaAuth **1.3.0** için hazırlanmıştır. KasabaRoles ile çalışır. Kullanıcının gönderdiği 1.0.0 paketi ve birleşik harabe köy/lobi dünyası temel alınmıştır.

## Kurulum ve güncelleme

Sunucuyu durdur, eski Vampir Köylü JAR dosyasını kaldırıp **KasabaVampirKoylu-1.1.0.jar** dosyasını `plugins` içine koy ve yeniden başlat. KasabaHub 1.2.0 ve KasabaAuth 1.3.0 gereklidir. Hazır harita JAR'ın içindedir; ilk kurulumda dünya otomatik hazırlanır. `/oyunlar` menüsüne oyun kendisi eklenir.

Bu sürüm **vampirkoylu_sokaklar** adlı yeni bir dünya hazırlar. Küçük toplantı meydanı, 24 çift çıkışlı ev, 17 kapalı görev odası, avlular, fenerler ve S biçimli kısa sokaklar vardır. Önceki açık harita otomatik olarak bu yerleşime geçer. Hazır harita JAR'ın içindedir; ayrıca dünya indirip kurman gerekmez. Yeni chunk'lar boş üretilir.

**Eski dünya klasörleri silinmez ve üzerlerine yazılmaz.** Eski lobi oyun alanının dışındaysa lobi çevresindeki chunk'lar yeni dünyaya kopyalanır; bu bölgedeki düzenlemelerin ve lobi koordinatların korunur. Oyun alanı yeni köyle değiştirilir. Özel isimli başka bir harita/lobi kullanıyorsan o isimler korunur. Eski lobi doğrudan eski oyun alanının içindeyse lobi o eski dünyada kalır, maçlar yeni köyde açılır.

Geçiş öncesi ayarın yedeği `plugins/KasabaVampirKoylu/config-before-village-1.1.yml` dosyasındadır. Varsayılan ortak lobi **144.5 / 69 / 0.5**, köy merkezi **0 / 69 / 0** konumundadır. Ana lobi hiç ayarlanmamışsa orada `/merkez lobi ayarla` kullan. 1.0.4'teki 40 saniyelik gündüz avı, özel sürelerin ve üç oda sistemi korunur.

## Kolay ve orta görevler

Her oyuncuya ve vampire aynı dağılımla **2 kolay + 2 orta** kişisel görev verilir. İşler rolüne göre kolaylaşmaz; yalnız bitince gerçek/sahte ilerleme hesabı ayrılır.

| Seviye | İş | Nasıl yapılır? |
|---|---|---|
| Kolay | Düzeneği Ayarla | Sayfada yazan dört kontrolü sırayla uygula; en az 6 saniye hazırlık gerekir. |
| Kolay | Malzemeleri Sırala | Odun, demir, erzak ve kumaşı o maç için gösterilen sırayla ayır; en az 8 saniye. |
| Orta | İki Duraklı Teslimat | Kutuyu kontrol et, erzağı yerleştir, mühürle; en az 4 saniyelik hazırlıktan sonra paketi yakın iki ayrı istasyona sırayla teslim et. Harita hedefi her durakta yenilenir. |
| Orta | Hafıza Kontrolü | İlk turda 3, ikinci turda 4 rengi hatırla. Her diziyi en az 4 saniye incele, **Diziyi Gizle ve Başla** düğmesine bas, sonra sırayı uygula. |

İşlemler arasında kısa hazırlık aralığı vardır; hızlıca art arda basmak bu süreyi aşmaz. Yanlış sıralamada o aşama yeniden başlar. Hafızada **Diziyi Tekrar Göster** mevcut turu yeniden hazırlamana izin verir; tamamlanmış ilk tur korunur. Görev noktasından 5 bloktan fazla uzaklaşınca veya toplantı başlayınca yarım aşama yeniden hazırlanır. Taşınan paket kaybolmaz. Teslimat ve hafıza işi yalnız tamamen bitince görev sayılır.

İlk geceden sonra ortak bakım işleri de açılır. Bunlar da kısa bir sıralama/hazırlık işidir; başarıyla bitirdikten sonra tekrar almak için 30 saniye beklersin. Vampir aynı bakımı sahte ilerleme olarak yapabilir.

## Ekranlar, görev haritası ve gizlilik

- Her oyuncunun üstte sürekli görünen **ROLÜN** çubuğu vardır. Bu çubuk yalnız o oyuncuya gönderilir. Başkalarının rolü bu göstergeden görülemez.
- Toplantıda ortak üst çubuk **ŞU AN KONUŞUYOR: Oyuncu • 20 sn** yazar. Süre ve çubuk azalır; kişi değişince anında güncellenir.
- **Çanı çalan veya cesedi bildiren oyuncu ilk konuşur.** Otomatik gece toplantısı katılım sırasını kullanır; elenenler atlanır.
- 7. kutuda köyün gerçek görünümünü içeren **Görev Haritası** bulunur. Parlayan sarı 1–4 kendi görevlerin, yeşil bitenler, mavi taşıma işinin teslimat noktasıdır. Beyaz ok yalnız seni; kırmızı X seçtiğin hedefi gösterir. Diğer oyuncuların konumu haritada yer almaz.
- Haritaya/deftere sağ tıkla veya `/vk görevler` yaz. Bir görev seçince yön ve mesafe ekranın altında gösterilir. `/vk harita` haritayı eline alır; `/vk görev 1` birinci görevi takip eder.
- 17 istasyonda **GÖREV NOKTASI**, işin adı ve sağ tıklama yönergesi görünür. Yakındaki kendi aktif işlerin ayrıca yalnız sana görünen sarı ışık/parıltı parçacıklarıyla işaretlenir. Haritadaki bekleyen işler ve seçilen hedef beyaz/sarı çerçeveyle yanıp söner. Taşıma işinde paketi alınca hedef teslimat yerine döner.
- Vampirin kılıcı, avcının yayı ve diğer el eşyaları kendi ekranında görünür; diğer oyunculara el ekipmanı boş gönderilir. Bu maskeleme oyuncunun ilk görünme paketlerini de kapsar. Normal görünüm oyun çıkışında geri yüklenir.
- Kafa üstü oyuncu isimleri **10 blok içinde**, ceset isimleri **5 blok içinde**, ayrıca **açık görüş hattı varsa** görünür. Duvar ve kapalı kapı arkasındaki isim gösterilmez. Görünürlük her izleyen oyuncu için ayrı hesaplanır. Işınlanma veya varlık görünürlüğünün sıfırlanması sonrasında gerçek görünürlük yeniden denetlenir; geçersiz isim etiketleri yenilenir.
- `/vk rehber` oyunun amacı, roller, görevler, konuşma, oylama ve gece seçimlerini açıklar. Oylama sayfası toplantının nedenini, kimi neden seçtiğini, yanlış kararın etkisini ve Pas seçeneğini anlatır.
- Gece kahin/doktor/vampir seçim sayfaları otomatik açılır. Kahin `/vk gör`, doktor `/vk koru`, vampir `/vk seçim` ile yeniden açabilir. Kahin ve doktora ayrıca yalnız kendilerinin göreceği gece yeteneği eşyası verilir. Bu yetenekler gece rol seçimi aşamasında kullanılabilir.

Elde tutulan görev haritası yeni sokakları ve yeni görev odalarını gösterir. Sağ üstte sabit mini harita eklenmemiştir; oyuncuların mod kurması gerekmez.

## Üç oda

Oda sayfası **101, 102, 103** odalarını ve boş/bekliyor/oyunda durumlarını gösterir. `/vk oda kur` ilk boş odayı açar; sayfadan belirli boş oda da seçilebilir. En fazla üç maç aynı anda oynanır. Başlayan her oda köyün ayrı geçici kopyasında çalışır; oyuncular, görevler ve gece seçimleri birbirine karışmaz. Herkes aynı ortak oyun lobisinde buluşur. Maç bitince kopyası kaldırılır ve oda tekrar açılabilir.

## İki kişiyle ilk deneme

Yönetici hesabın OP veya `kasabatr.vampir.admin` yetkili olmalı.

1. Yönetici `/vk oda kur` yazar. Sohbette örneğin **101** numarası çıkar.
2. Diğer oyuncu `/vk oda katıl 101` yazar. 101 yerine ekranda çıkan numarayı kullanır.
3. Oda kurucusu olan yönetici **`/vk deneme`** yazar. Menüdeki **2 Kişilik Deneme** de aynı işi yapar.
4. Bir oyuncu rastgele vampir, diğeri köylü olur. Bu odada avcı, kahin ve doktor kapatılır. Bunları görmek için normal maçta en az 6 oyuncu gerekir.
5. Görev noktalarına sağ tıklayabilir, meydandaki çanla toplantı açabilir, konuşma/oylamayı deneyebilirsin. Vampirin köylüyü elemesi maçı bitirir; bu demo uzun ve dengeli maç için tasarlanmamıştır.
6. `/vk durdur` denemeyi bitirip ikinizi Vampir Köylü lobisine alır. `/lobi` ana lobiye döner. Normal maçın 6 kişilik sınırı değişmez.

## Oyuncu komutları

Türkçe karakterler ve bunların sade yazılışları desteklenir: `katıl/katil`, `başlat/baslat`, `çık/cik`, `yardım/yardim`.

| Komut | Ne yapar? |
|---|---|
| `/vk` | Oyuna giriş yapıp ilgili sayfayı açar: oda, oylama veya gece seçimi. Gündüz görevleri/rolünü hatırlatır. |
| `/vampirkoylu` | `/vk` kısayolunun uzun yazılışı. |
| `/oyunlar git vampirkoylu` | KasabaHub üzerinden Vampir Köylü lobisine gider. |
| `/vk oda kur` | Yeni oda açar; kuran oyuncu oda yöneticisi olur. |
| `/vk oda liste` | Katılınabilecek bekleyen odaları sayfada gösterir. |
| `/vk oda katıl <numara>` | Numarasını bildiğin bekleyen odaya katılır. |
| `/vk oda ayrıl` | Sadece odadan ayrılır; Vampir Köylü lobisinde kalır. |
| `/vk lobi` | `/vk oda ayrıl` ile aynı. |
| `/vk başlat` | Oda kurucusu normal maçı başlatır. Varsayılan 6–20 oyuncu. |
| `/vk bitir` | **Kendi konuşma sıranı erken bitirir. Maçı bitirmez.** |
| `/v <mesaj>` | Aynı odadaki yaşayan vampirlere gizli mesaj gönderir. |
| `/vk harita` | Görev haritasını 7. kutuda eline alır. |
| `/vk görevler` | Görev konumlarını ve durumlarını sayfada açar. |
| `/vk görev <1–4>` | Seçilen görev için yön/mesafe hedefi belirler. |
| `/vk rehber` | Ayrıntılı oyun açıklamasını açar. |
| `/vk gör` | Kahinin gece rol görme sayfasını açar. |
| `/vk koru` | Doktorun gece koruma sayfasını açar. |
| `/vk seçim` | Rolün uygunsa gece seçim sayfasını açar. |
| `/vk durum` | Açık odaların numarasını, aşamasını ve oyuncu sayısını gösterir; gizli rolleri göstermez. |
| `/vk çık` | Oyundan ayrılıp ana lobiye döner; envanterini geri alır. |
| `/lobi` | KasabaHub ana lobisine döner; maç sırasında da kullanılabilir. |
| `/vk yardım` | Komut özetini gösterir. |

Lobideki pusulaya sağ tıklamak da sayfayı açar. Ekranlar Minecraft'ın yerleşik sayfalarıdır. Envanter sandığı menüsü değildir. Oda kurucusu başlangıçtan önce sayfadan **1/2 vampir**, **avcı**, **kahin**, **doktor** ayarlarını değiştirir. En az bir sıradan köylü yeri kalmalıdır. Başlamış odaya yeni oyuncu alınmaz. Bekleme sırasında kurucu ayrılırsa oda sahipliği kalan ilk oyuncuya geçer.

## Yönetici komutları

| Komut | Ne yapar? |
|---|---|
| `/vk deneme` | Yönetici olan **oda kurucusunun**, tam iki kişilik bekleyen odasını deneme olarak başlatır. |
| `/vk durdur` | İçinde bulunduğun odayı bitirir. |
| `/vk durdur <numara>` | Belirttiğin odayı bitirir. Katılımcılar oyun lobisine döner. |
| `/vk doğma ayarla` | Durduğun yeri Vampir Köylü lobi noktası yapar. Tüm oyuncular önce bu oyundan çıkmış olmalı. |
| `/vk lobi ayarla` | Yukarıdaki lobi ayarlama komutunun alternatifidir. |
| `/vk setspawn` | Önceki sürümle uyumlu lobi ayarlama komutu. |
| `/vk düzenleme aç` | Oyun dışında yetkili olarak şablon haritada blok düzenlemeye izin verir. |
| `/vk düzenleme kapat` | Düzenleme iznini kapatır. |

Varsayılan haritayı kullanıyorsan doğma ayarlamak **gerekmez**. Düzenleme modu sadece şablon/oyun lobisinin bu eklentiye ait korumasını aşar; başka koruma eklentilerinin kurallarını kaldırmaz. Geçici maç dünyası düzenlenemez. Görev istasyonlarının koordinatları haritaya bağlıdır; istasyonları taşımak yalnız lobi ayarlama komutuyla yapılmaz.

Yetkiler: `kasabatr.vampir.kullan` herkes için açık; `kasabatr.vampir.admin` varsayılan OP. Yetkileri KasabaRoles üzerinden de verebilirsin.

## Maç nasıl işler?

- **Gündüz 180 saniye:** Köylüler görev yapar. Vampirin sahte görevleri de üstteki yüzdeyi gerçek görevlerle aynı miktarda artırır. Yüzdeyi izleyerek bir oyuncunun rolünü kesinleştiremezsin. Yaşayanların normal sohbetini yalnız aynı odada 10 blok yakındakiler duyar.
- **Vampir:** Gündüz 3.5 blok yakınındaki hedefe vurunca eleyebilir. Günlük av sayısı sınırı yoktur; başarılı her avdan sonra **40 saniye** bekler. Kalan süre yalnız kendi ROLÜN çubuğunda görünür. Iskalamak, uzaktaki hedefe vurmak veya bekleme sırasında denemek süreyi yeniden başlatmaz. Toplantı ve yeni gündüz de beklemeyi sıfırlamaz. Vampir diğer vampiri avlayamaz. Gece hedefini sayfadan seçer. 15 kişinin altındaki maçta vampir takımına toplam 1 gece avı; 15–20 kişide her vampire en fazla 1 hedef.
- **Avcı:** Varsayılan 1 ok. İsabet eden oyuncu elenir ve rolü açıklanır. Iskalama da ok hakkını tüketir; düşen ok geri alınamaz.
- **Kahin:** Gece başka bir yaşayan oyuncunun rolüne gizlice bakar.
- **Doktor:** Gece bir yaşayan oyuncuyu korur; kendisini seçebilir. Kahin/doktor seçim yapmazsa geçerli hedef rastgele seçilir. Vampir seçmezse otomatik av yapılmaz.
- **Görevler:** Defter yerlerini/ilerlemeyi gösterir. İstasyondaki bloğa sağ tıkla. Kolay işlerde sayfadaki sırayı ve hazırlık süresini uygula; orta işlerde iki teslimat durağını veya iki hafıza turunu tamamla. İlk geceden sonra herkes için ortak bakım görevleri de açılır; oyuncu başına 30 saniye aralıklıdır. Vampirin bakım işleri de görünür ilerleme verir. Köylü görev hedefi vampir olmayan başlangıç oyuncusu başına 3 iştir. Ölenin görevini tamamlamak zorunlu değildir; ortak bakımla ilerlenebilir. Görünür ilerleme gerçek+sahte işlerden hesaplanır, maç bitene kadar en fazla **%99** gösterilir. Köylü zaferi yalnız gerçek görev sayısından hesaplanır; sahte işler köylüleri kazandırmaz ve gerçek sayaç oyunculara açıklanmaz.
- **Toplantı:** Cesede 4 blok yakınken bildirim çanına sağ tıkla veya meydandaki sabit çana bas. Acil çan kişi başına maçta 1 kez, genel bekleme 45 saniye. Gündüz süresi dolunca da otomatik toplanılır.
- **Konuşma:** Yaşayanlara sırayla 20 saniye. Yalnız sırası gelen konuşur; `/vk bitir` erken geçirir. Oyuncular toplantıda yürüyemez.
- **Oylama:** 30 saniye içinde oyuncu seç veya pas geç. Eşitlikte/pasta kimse elenmez. Oylamada elenenin rolü açıklanır. Gece toplantısının ardından 40 saniyelik rol seçimi açılır; acil/ceset toplantısından sonra kalan gündüz devam eder.
- **Elendiğinde:** Seyirci olursun, oy/normal sohbet kullanamazsın. İstersen `/lobi` ile çıkabilirsin.
- **Bitiş:** Vampirler tamamen elenirse köylüler; yaşayan vampirler köylülerle eşit veya çoğunluk olursa vampirler kazanır. İlk gece sonrasında görev hedefi tamamlanırsa köylüler kazanır. Oda bittiğinde herkes bu oyunun lobisine döner; geçici dünya kaldırılır.

## Koruma ve envanter

Şablon harita, oyun lobisi ve geçici maç dünyaları kırma/koyma, yangın ve patlama hasarına karşı korunur. Oyun içinde eşya atma, alma, el değiştirme ve envanter taşıma engellenir. Köyde kapılar kullanılabilir. Normal Minecraft hasarı/açlık maçı bozmaz; elemeyi rol kuralları belirler. Maçta dünya/oyun değiştiren komutlar kapalıdır; `/lobi`, `/vk`, `/v` açıktır. Geçici dünyalarda oyuncu konumu gösteren alt çubuk kapalıdır.

Girişte envanter, imleçteki eşya, zırh/sol el, oyun modu, can, ek can, iksir etkileri, deneyim ve uçuş durumu `plugins/KasabaVampirKoylu/recoveries/` altında yedeklenir. Ana lobiye çıkarken geri yüklenir. Oyuncu bağlantısı kesilince de geri yüklenir; yarım kalan kayıt varsa sonraki **başarılı kimlik doğrulamasından sonra** kurtarılır. Yedek başarıyla yüklenmeden silinmez; bu klasörü oyuncu verisi olarak koru. Maçtan sadece oyun lobisine dönmek yedeği tüketmez.

## Ayarlar ve derleme

Süreler ve oyuncu sayıları `plugins/KasabaVampirKoylu/config.yml` içindedir. Değişiklikten sonra sunucuyu yeniden başlat; `/reload` kullanma. Normal minimum oyuncu sayısını 2'ye indirmene gerek yok; deneme modu oda bazında çalışır.

Depo kökünde önce `mvn install`, ardından `mvn -f kasaba-hub/pom.xml install`, son olarak `mvn -f kasaba-vampir/pom.xml verify` kullan. Java 21 gerekir. Kaynak ZIP tek başına derlenecekse önce KasabaAuth 1.3.0 ve KasabaHub 1.2.0 API JAR dosyalarını Maven yerel deposuna karşılık gelen `tr.kasaba:kasaba-auth:1.3.0` ve `tr.kasaba:kasaba-hub:1.2.0` koordinatlarıyla yükle. Paper API, KasabaHub ve KasabaAuth `provided` bağımlılıktır; JAR içine gömülmez. Netty taşıma sınıfları sunucudan sağlanır; ek bir protokol eklentisi kurmak gerekmez. Ekipman maskelemesi Paper 1.21.10 paketleriyle doğrulanır. Test adaptörü `scripts/integration` altındadır, yalnız 127.0.0.1 sunucuda açılır ve dağıtım paketine konmaz. Gerçek Paper/istemci test sonuçları dağıtımdaki `KasabaVampirKoylu-TEST-SONUCLARI.txt` dosyasındadır.

Yeni haritayı kaynaklardan yeniden üretmek için `pip install nbtlib numpy Pillow` ve `python scripts/build-village.py` kullanılır. Hazır dosyalar pakette olduğundan normal JAR derlemesinde Python gerekmez. Protokol testleri tam deponun `kasaba-arena/scripts/integration` ortak istemcisini kullanır.

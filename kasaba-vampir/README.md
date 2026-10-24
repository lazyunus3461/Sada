# Kasaba Vampir Köylü 1.2.1

Paper **1.21.10**, Java **21**, KasabaHub **1.2.0** ve KasabaAuth **1.3.0** için hazırlanmıştır. KasabaRoles ile çalışır. Kullanıcının gönderdiği 1.0.0 paketi ve birleşik harabe köy/lobi dünyası temel alınmıştır.

## Kurulum ve güncelleme

Sunucuyu durdur, eski Vampir Köylü JAR dosyasını kaldırıp **KasabaVampirKoylu-1.2.1.jar** dosyasını `plugins` içine koy ve yeniden başlat. KasabaHub 1.2.0 ve KasabaAuth 1.3.0 gereklidir. Hazır harita JAR'ın içindedir; ilk kurulumda dünya otomatik hazırlanır. `/oyunlar` menüsüne oyun kendisi eklenir.

Bu sürüm **vampirkoylu_uc_oda** adlı tek boş dünya içinde **üç fiziksel köy alanı ve bir ortak lobi** içerir. Oda 101 merkezi **0 / 69 / 0**, oda 102 **320 / 69 / 0**, oda 103 **640 / 69 / 0**; ortak lobi **144.5 / 69 / 0.5** konumundadır. Her köyde aynı 24 çift çıkışlı ev, 17 kapalı görev odası, avlular ve S biçimli dar sokaklar bulunur.

[Üç alanlı dünya ZIP’ini indir](../dist/KasabaVampirKoylu-1.2.0-3-ODA-DUNYA.zip). Sunucu kapalıyken ZIP’teki `vampirkoylu_uc_oda` klasörünü **server.properties dosyasının yanına**, JAR’ı `plugins` içine koy. ZIP dünya dosyalarıdır; oyun ve görevler için 1.2.1 JAR da gerekir. JAR ilk kurulumda dünya klasörü yoksa kendisi de hazırlar.

**Yapıların ve kendi zeminlerinin dışı tamamen boşluktur.** Doğal arazi, dağ, köy veya başka yapı yoktur; yeni alanlarda da oluşmaz. Eski dünya dosyaları yeni haritaya aktarılmaz. Önceki dünya klasörleri silinmez. Önceki hazır haritayı kullanan sunucu yeni dünyaya ve hazırlanmış tek ortak lobiye otomatik geçer; özel adla ayarlanmış başka dünyalar korunur. Ayarın geçiş öncesi kopyası `plugins/KasabaVampirKoylu/config-before-three-villages-1.2.yml` dosyasındadır.

Her oda maça başlarken bu dünyanın ayrı bir geçici kopyasını açar ve **kendi köyünün koordinatlarını** kullanır. Böylece 101, 102 ve 103 aynı anda oynanabilir; roller, görevler ve gece/gündüz süreleri ayrıdır. Maç sınırı yalnız ilgili köyü kapsar; o odanın oyuncusu diğer iki köye gidemez. Maç bitince yalnız geçici kopya kaldırılır; üç alanın bulunduğu ana dünya ve ortak lobi kalır. Yönetici düzenlemeyi ana dünyada `/vk düzenleme aç` ile yapar; sonraki maçlar bu düzenlenmiş şablondan açılır.

## Pusula, dünya adı ve Multiverse düzeltmesi (1.2.1)

Pusuladan girişte artık dünya dosyasının varlığıyla birlikte üç köyün ve güvenli lobi zemininin varlığı kontrol edilir. Boş veya yanlış lobiye oyuncu gönderilmez. Yüklü dünyalar arasında bu üç köyü içeren tek bir dünya varsa pusula ve maç kaynağı ona bağlanır; örneğin Multiverse ile `vampirkoylu` adıyla yüklediğin hazır harita. Bu bağlantı yeniden başlatmada korunur.

Mevcut hazır dünya boş veya eksikse, dolu başka bir kaynak bulunamadığında JAR içindeki harita ayrı bir `vampirkoylu_uc_oda_onarim` klasörüne hazırlanır. Dolu klasör varsa sonuna numara eklenir. Mevcut dünyaların üzerine harita yazılmaz; önceki dosyalar silinmez. Bu güncelleme için dünya ZIP'ini yeniden indirmen gerekmez; 1.2.0 harita verileri aynıdır.

Maç kopyası oluşturulmadan önce Paper’ın bekleyen dünya yazmaları diske tamamlanır; bölge dosyaları kayıt sürerken kopyalanmaz. Kaynak klasörü yüklü dünyanın gerçek klasöründen alınır.

Multiverse-Core 5 kuruluysa ortak dünya otomatik kaydedilir; `/mv info` dünyayı tanır, `/mvtp <bağlanan dünya>` aynı ortak lobiye gider. Multiverse isteğe bağlıdır. Aktif maçların geçici dünyaları Multiverse'e kaydedilmez. Gerçek Multiverse-Core **5.3.2** ile test edilmiştir.

Yönetici kontrolü:
- `/vk dünya bilgi` — bulunduğun dünya/kimlik/konum, pusula lobisi, üç köy kontrolü, gerçek kaynak klasörü ve Multiverse kaydı.
- `/vk dünya bağla` — bulunduğun üç köylü hazır dünyayı kaynak ve ortak lobi yapar.
- `/vk dünya bağla vampirkoylu` — yüklü `vampirkoylu` dünyasına bağlar. Dünya üç köyü ve hazırlanmış lobiyi içermiyorsa reddedilir; tek köylü eski haritaya zorla bağlamaz.

Bağlamadan önce aktif maçlar bitmelidir; bekleyen oyuncular yeni ortak lobiye taşınır. Önceki ayar `config-before-world-link-1.2.1.yml` olarak bir kez yedeklenir. Özel olarak ayarlanmış başka bir dünya otomatik değiştirilmez; yalnız doğrulanmış hazır kaynak bağlanır.

## Kolay ve orta görevler

Her oyuncuya ve vampire aynı dağılımla **2 kolay + 2 orta** kişisel görev verilir. İşler rolüne göre kolaylaşmaz; yalnız bitince gerçek/sahte ilerleme hesabı ayrılır.

| Seviye | İş | Nasıl yapılır? |
|---|---|---|
| Kolay | Düzeneği Ayarla | Sayfada yazan dört kontrolü sırayla uygula; en az 5 saniye hazırlık gerekir. |
| Kolay | Malzemeleri Sırala | Odun, demir, erzak ve kumaşı o maç için gösterilen sırayla ayır; en az 7 saniye. |
| Orta | İki Duraklı Teslimat | Kutuyu kontrol et, erzağı yerleştir, mühürle; en az 4 saniyelik hazırlıktan sonra paketi yakın iki ayrı istasyona sırayla teslim et. Harita hedefi her durakta yenilenir. |
| Orta | Hafıza Kontrolü | İki turda da 3 rengi hatırla. Her diziyi en az 4 saniye incele, **Diziyi Gizle ve Başla** düğmesine bas, sonra sırayı uygula. |

İşlemler arasında kısa hazırlık aralığı vardır; hızlıca art arda basmak bu süreyi aşmaz. Yanlış sıralamada o aşama yeniden başlar. Hafızada **Diziyi Tekrar Göster** mevcut turu yeniden hazırlamana izin verir; tamamlanmış ilk tur korunur. Görev noktasından 5 bloktan fazla uzaklaşınca veya toplantı başlayınca yarım aşama yeniden hazırlanır. Taşınan paket kaybolmaz. Teslimat ve hafıza işi yalnız tamamen bitince görev sayılır.

İlk geceden sonra ortak bakım işleri de açılır. Bunlar da kısa bir sıralama/hazırlık işidir; başarıyla bitirdikten sonra tekrar almak için 30 saniye beklersin. Vampir aynı bakımı sahte ilerleme olarak yapabilir.

## Renkli ve daha anlaşılır ekranlar

Görev başlığı altın sarısı, yapılacak işlemin başlığı açık mavi, adım/kolaylık bilgisi yeşil ve açıklama gri görünür. Hafızada her rengin adı kendi rengindedir; seçim düğmeleri de aynı renkleri kullanır. Üstte gündüz bilgisi mavi, süre altın sarısı, görev yüzdesi yeşildir. Kişisel rol adı rol rengiyle kalın yazılır; açıklaması gri kalır. Konuşanın adı açık mavi, oylama uyarısı kırmızı olur. Ekranlar yalnız renkle anlatmaz; başlık ve açıklama metinleri de korunur.

## Kişiye özel yerde görev okları

Maçta **7. kutuda Görev Yolu pusulası** bulunur. Sağ tıkla ve görevini seç; `/vk görev 1`–`/vk görev 4` de aynı işi yapar. `/vk yol` pusulayı tekrar eline alır. Görev seçmesen bile en yakın yürünebilir görev yolu otomatik seçilir; yolda yürürken hedef kendi kendine başka göreve geçmez.

Yerde yalnız sana gönderilen **mavi parçacık okları** görünür. Yol duvarı kestirmez; baş boşluğu ve sağlam zemini olan blokları, dört ana yönde takip eder. Önündeki en fazla 10 blok işaretlenir, her yarım saniyede bulunduğun bloğa göre yenilenir. Kapalı ahşap kapının önünde durur; kapıyı sağ tıklayıp açınca içeride devam eder. İstasyona bitişik zeminde işaretli görev bloğuna sağ tıklaman istenir. Mod veya kaynak paketi gerekmez; istemcide parçacıkların açık olması gerekir.

Paket hazırlanınca oklar ilk teslim yerine, ilk teslimattan sonra ikinci durağa döner. Görev bitince sıradaki bitmemiş işe geçer. Vampirin sahte işleri de aynı yol gösterimini kullanır. Toplantı, gece, elenme ve oyun çıkışında yeni ok gönderilmez; eski parçacıklar kısa sürede söner. Harita artık otomatik ele verilmez; isteyen `/vk harita` ile mevcut kişisel haritayı açabilir. 1.2.0 üç fiziksel alan içeren yeni dünyayı kullanır; önceki tek alanlı dünya ZIP’i yerine bu sürümün ZIP’ini kullan.

## Ekranlar, isteğe bağlı harita ve gizlilik

- Her oyuncunun üstte sürekli görünen **ROLÜN** çubuğu vardır. Bu çubuk yalnız o oyuncuya gönderilir. Başkalarının rolü bu göstergeden görülemez.
- Toplantıda ortak üst çubuk **ŞU AN KONUŞUYOR: Oyuncu • 20 sn** yazar. Süre ve çubuk azalır; kişi değişince anında güncellenir.
- **Çanı çalan veya cesedi bildiren oyuncu ilk konuşur.** Otomatik gece toplantısı katılım sırasını kullanır; elenenler atlanır.
- İsteğe bağlı `/vk harita` komutu 7. kutuya köyün gerçek görünümünü içeren **Görev Haritası** koyar. Parlayan sarı 1–4 kendi görevlerin, yeşil bitenler, mavi taşıma işinin teslimat noktasıdır. Beyaz ok yalnız seni; kırmızı X seçtiğin hedefi gösterir. Diğer oyuncuların konumu haritada yer almaz.
- Pusulaya/deftere sağ tıkla veya `/vk görevler` yaz. Bir görev seçince yerdeki oklar ve ekranın altındaki yönerge onu takip eder. `/vk görev 1` birinci görevi seçer; `/vk yol` görev pusulasını eline alır.
- 17 istasyonda **GÖREV NOKTASI**, işin adı ve sağ tıklama yönergesi görünür. Yakındaki kendi aktif işlerin ayrıca yalnız sana görünen sarı ışık/parıltı parçacıklarıyla işaretlenir. Haritadaki bekleyen işler ve seçilen hedef beyaz/sarı çerçeveyle yanıp söner. Taşıma işinde paketi alınca hedef teslimat yerine döner.
- Vampirin kılıcı, avcının yayı ve diğer el eşyaları kendi ekranında görünür; diğer oyunculara el ekipmanı boş gönderilir. Bu maskeleme oyuncunun ilk görünme paketlerini de kapsar. Normal görünüm oyun çıkışında geri yüklenir.
- Kafa üstü oyuncu isimleri **10 blok içinde**, ceset isimleri **5 blok içinde**, ayrıca **açık görüş hattı varsa** görünür. Duvar ve kapalı kapı arkasındaki isim gösterilmez. Görünürlük her izleyen oyuncu için ayrı hesaplanır. Işınlanma veya varlık görünürlüğünün sıfırlanması sonrasında gerçek görünürlük yeniden denetlenir; geçersiz isim etiketleri yenilenir.
- `/vk rehber` oyunun amacı, roller, görevler, konuşma, oylama ve gece seçimlerini açıklar. Oylama sayfası toplantının nedenini, kimi neden seçtiğini, yanlış kararın etkisini ve Pas seçeneğini anlatır.
- Gece kahin/doktor/vampir seçim sayfaları otomatik açılır. Kahin `/vk gör`, doktor `/vk koru`, vampir `/vk seçim` ile yeniden açabilir. Kahin ve doktora ayrıca yalnız kendilerinin göreceği gece yeteneği eşyası verilir. Bu yetenekler gece rol seçimi aşamasında kullanılabilir.

İsteğe bağlı görev haritası yeni sokakları ve yeni görev odalarını gösterir. Sağ üstte sabit mini harita eklenmemiştir; oyuncuların mod kurması gerekmez.

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
| `/vk yol` | Görev pusulasını eline alır; yerde yalnız sana görünen oklar seçili göreve götürür. |
| `/vk harita` | İsteğe bağlı eski kişisel haritayı 7. kutuda eline alır. |
| `/vk görevler` | Görev konumlarını ve durumlarını sayfada açar. |
| `/vk görev <1–4>` | Seçilen göreve sokak ve kapıları izleyen özel yer oklarını açar. |
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
| `/vk dünya bilgi` | Dünya kimliği, kaynak, lobi zemini ve Multiverse kaydını gösterir. |
| `/vk dünya bağla [dünya adı]` | Üç köylü hazır haritayı doğrulayarak pusula ve maç kaynağını bağlar. |
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

Haritayı kaynaklardan yeniden üretmek için `pip install nbtlib numpy Pillow`, `python scripts/build-village.py`, `python scripts/build-three-villages.py` ve `python scripts/validate-three-villages.py` kullanılır. Dünya ZIP’i `python scripts/package-world.py` ile hazırlanır. Hazır dosyalar pakette olduğundan normal JAR derlemesinde Python gerekmez. Protokol testleri tam deponun `kasaba-arena/scripts/integration` ortak istemcisini kullanır.

## 1.3.1

VIP rol tercihi kaydedilince onay gösterilir. Odada kapalı olan roller
işaretlenir; iki kişilik denemede yalnız Vampir/Köylü bulunur. Aynı rolü
isteyen VIP’ler arasında kura çekilir ve rol adetleri korunur. Oylama
menüsünde her hedefin toplamı canlı yenilenir; kimin kime oy verdiği
açıklanmaz. Başka oyuncunun oyu, açık menünün düğmelerini geçersiz kılmaz.

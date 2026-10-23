# Kasaba Vampir Köylü 1.0.1

Paper **1.21.10**, Java **21**, KasabaHub **1.2.0** ve KasabaAuth **1.3.0** için hazırlanmıştır. KasabaRoles ile çalışır. Kullanıcının gönderdiği 1.0.0 paketi ve birleşik harabe köy/lobi dünyası temel alınmıştır.

## İlk kurulum

1. Sunucuyu **durdur**.
2. Kurulum ZIP'ini aç. `plugins/KasabaVampirKoylu-1.0.1.jar` dosyasını sunucunun `plugins` klasörüne koy. Önceki Vampir Köylü JAR'ını kaldır; aynı eklentinin iki sürümü bulunmasın.
3. `KasabaTR-VampirKoylu-TamDunya` klasörünü sunucunun **ana klasörüne** koy: `server.properties` ile yan yana, `plugins` içine değil. Aynı isimli mevcut dünyan varsa önce yedekle; üzerine yazma.
4. KasabaHub 1.2.0 ve KasabaAuth 1.3.0 kurulu olsun. Sunucuyu aç. `/oyunlar` menüsüne Vampir Köylü kendisi eklenir; `/oyunlar ekle` gerekmez.
5. Ana lobi hiç ayarlanmadıysa ana lobide `/merkez lobi ayarla` kullan. Bu komut KasabaHub'a aittir. Vampir Köylü'nün kendi lobi noktası hazır gelir.

Lobi: `KasabaTR-VampirKoylu-TamDunya`, **144.5 / 69 / 0.5**. Lobi alanı x=112..175, z=-32..31. Köy merkezde x=0, z=0 konumundadır. Her maç bu dünyadan ayrı, geçici bir kopya oluşturur. Aynı anda açık odalar birbirine karışmaz; şablon dünya maç yüzünden değişmez.

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

- **Gündüz 120 saniye:** Köylüler görev yapar. Vampirin görevleri sahtedir, takım yüzdesine eklenmez. Yaşayanların normal sohbetini yalnız aynı odada 10 blok yakındakiler duyar.
- **Vampir:** Gündüz 3.5 blok yakınındaki hedefe vurunca eleyebilir. Varsayılan gündüz başına 1 av, avlar arasında 45 saniye. Vampir diğer vampiri avlayamaz. Gece hedefini sayfadan seçer. 15 kişinin altındaki maçta vampir takımına toplam 1 gece avı; 15–20 kişide her vampire en fazla 1 hedef.
- **Avcı:** Varsayılan 1 ok. İsabet eden oyuncu elenir ve rolü açıklanır. Iskalama da ok hakkını tüketir; düşen ok geri alınamaz.
- **Kahin:** Gece başka bir yaşayan oyuncunun rolüne gizlice bakar.
- **Doktor:** Gece bir yaşayan oyuncuyu korur; kendisini seçebilir. Kahin/doktor seçim yapmazsa geçerli hedef rastgele seçilir. Vampir seçmezse otomatik av yapılmaz.
- **Görevler:** Defter yerlerini/ilerlemeyi gösterir. İstasyondaki bloğa sağ tıkla. Sıralı işlerde sayfadaki sırayı uygula; taşıma işlerinde paketi alıp belirtilen başka istasyona götür. İlk geceden sonra ortak bakım görevleri de açılır; oyuncu başına 30 saniye aralıklıdır. Köylü görev hedefi vampir olmayan başlangıç oyuncusu başına 3 iştir. Ölenin görevini tamamlamak zorunlu değildir; ortak bakımla ilerlenebilir.
- **Toplantı:** Cesede 4 blok yakınken bildirim çanına sağ tıkla veya meydandaki sabit çana bas. Acil çan kişi başına maçta 1 kez, genel bekleme 45 saniye. Gündüz süresi dolunca da otomatik toplanılır.
- **Konuşma:** Yaşayanlara sırayla 20 saniye. Yalnız sırası gelen konuşur; `/vk bitir` erken geçirir. Oyuncular toplantıda yürüyemez.
- **Oylama:** 30 saniye içinde oyuncu seç veya pas geç. Eşitlikte/pasta kimse elenmez. Oylamada elenenin rolü açıklanır. Gece toplantısının ardından 20 saniyelik rol seçimi açılır; acil/ceset toplantısından sonra kalan gündüz devam eder.
- **Elendiğinde:** Seyirci olursun, oy/normal sohbet kullanamazsın. İstersen `/lobi` ile çıkabilirsin.
- **Bitiş:** Vampirler tamamen elenirse köylüler; yaşayan vampirler köylülerle eşit veya çoğunluk olursa vampirler kazanır. İlk gece sonrasında görev hedefi tamamlanırsa köylüler kazanır. Oda bittiğinde herkes bu oyunun lobisine döner; geçici dünya kaldırılır.

## Koruma ve envanter

Şablon harita, oyun lobisi ve geçici maç dünyaları kırma/koyma, yangın ve patlama hasarına karşı korunur. Oyun içinde eşya atma, alma, el değiştirme ve envanter taşıma engellenir. Köyde kapılar kullanılabilir. Normal Minecraft hasarı/açlık maçı bozmaz; elemeyi rol kuralları belirler. Maçta dünya/oyun değiştiren komutlar kapalıdır; `/lobi`, `/vk`, `/v` açıktır. Geçici dünyalarda oyuncu konumu gösteren alt çubuk kapalıdır.

Girişte envanter, imleçteki eşya, zırh/sol el, oyun modu, can, ek can, iksir etkileri, deneyim ve uçuş durumu `plugins/KasabaVampirKoylu/recoveries/` altında yedeklenir. Ana lobiye çıkarken geri yüklenir. Oyuncu bağlantısı kesilince de geri yüklenir; yarım kalan kayıt varsa sonraki **başarılı kimlik doğrulamasından sonra** kurtarılır. Yedek başarıyla yüklenmeden silinmez; bu klasörü oyuncu verisi olarak koru. Maçtan sadece oyun lobisine dönmek yedeği tüketmez.

## Ayarlar ve derleme

Süreler ve oyuncu sayıları `plugins/KasabaVampirKoylu/config.yml` içindedir. Değişiklikten sonra sunucuyu yeniden başlat; `/reload` kullanma. Normal minimum oyuncu sayısını 2'ye indirmene gerek yok; deneme modu oda bazında çalışır.

Depo kökünde önce `mvn install`, ardından `mvn -f kasaba-hub/pom.xml install`, son olarak `mvn -f kasaba-vampir/pom.xml verify` kullan. Java 21 gerekir. Kaynak ZIP tek başına derlenecekse önce KasabaAuth 1.3.0 ve KasabaHub 1.2.0 API JAR dosyalarını Maven yerel deposuna karşılık gelen `tr.kasaba:kasaba-auth:1.3.0` ve `tr.kasaba:kasaba-hub:1.2.0` koordinatlarıyla yükle. Paper API, KasabaHub ve KasabaAuth `provided` bağımlılıktır; JAR içine gömülmez. Test adaptörü `scripts/integration` altındadır, yalnız 127.0.0.1 sunucuda açılır ve dağıtım paketine konmaz. Gerçek Paper/istemci test sonuçları dağıtımdaki `KasabaVampirKoylu-TEST-SONUCLARI.txt` dosyasındadır.

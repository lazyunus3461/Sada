# Kasaba Battle Royale 1.0.3

Paper **1.21.10**, Java **21**. Gönderilen Batı Kasabası adasından hazırlanmış, **aynı dünyada üç ayrı savaş adası ve ortak BR lobisi**. Solo, İkili ve Dörtlü seçenekleri Minecraft'ın yerleşik sayfasında açılır. PUBG silahları kaldırıldı.

## 1.0.0 / 1.0.1 / 1.0.2 kuruluysa yalnızca güncelle

Sunucuyu durdur. `plugins` içindeki **eski KasabaBattleRoyale JAR** dosyasını dışarı çıkar, **KasabaBattleRoyale-1.0.3.jar** dosyasını koy ve sunucuyu yeniden başlat. `KasabaBR` dünyasını ve `plugins/BattleRoyalePrototype` klasörünü koru. Dünya indirmek, bölge/iniş/sandık konumu yeniden ayarlamak gerekmez.

1.0.3 değişiklikleri:

- **2–10 oyuncuda en az 240, 20 oyuncuda 320 sandık.** Önceki 36/45/90 sınırları büyük adada yetersizdi. Sandık adayı havuzu her adada **1.056 noktaya** çıktı: 352 ev içi, 16 çatı, 688 açık alan. Bunlardan 800 yeni noktanın tamamı merkezdeki **250×250** alandadır. Mevcut 256 adayın sırası, lobi, merkez, doğmalar ve dünyadaki yapılar korunur; 800 yeni aday bir kez eklenir.
- Sandıklar arena merkezindeki **250×250 blokluk alanın** 48×48 blokluk bölgelerine yayılarak seçilir. Sandıkların en az yarısı dış alandadır; bir önceki maçla yerleri çakışmaz. Böylece köy içindeki yoğun adaylar bütün seçimi tüketmez.
- **Her oyuncu tahta kılıç ve dört parça kahverengi deri zırhla başlar.** Minecraft Java'da tahta zırh yoktur; başlangıç için büyüsüz deri zırh kullanılır. Kılıç ilk kutudadır. Başlangıç eşyaları da BR'ye aittir; dış envantere taşınmaz.
- Silah bulunmayan sandıklarda %60 ek çekilişle yaygın seviyeden kılıç, balta veya yay eklenir. Bu çekiliş elmas/netherit eşyaları yaygınlaştırmaz; kendi eklediğin yaygın silahlar da havuza katılır. Zırh, yemek ve güçlü ganimetlerin her sandıktan çıkma garantisi yoktur. Yay/arbaletin yanında normal ok vardır.
- Eski config/ganimet dosyaları silinmez. Yeni anahtarlar dosyada yoksa varsayılanlar çalışır: `yogun-ganimet.asgari-sandik: 240`, `yogun-ganimet.oyuncu-basina-sandik: 16`, `sandik-dagitim-boyutu: 250`. Daha yüksek bir eski sandık ayarın varsa düşürülmez. Özel küçük aday havuzlarında, önceki maçın yerlerini tekrarlamamak için sayı havuzun yarısıyla sınırlanır.
- **Koruma Lokması** (kurutulmuş yosun) **2**, **Güçlü Koruma Lokması** (tatlı orman meyvesi) **3 sarı yedek kalp** verir. Bir sandıktan bu iki yiyecekten birinin çıkma ihtimali toplam **%30** (%15 + %15); en fazla bir adet. Süreyle kaybolmaz, hasarla tükenir, üst üste birikmez; daha fazla/eşit yedek can varken yiyecek harcanmaz. Ölümde ve BR'den çıkışta BR bonusu temizlenir. Şifa Lokması ayrıca 2 kırmızı kalp yeniler.
- Yönetici: `/br yedek ver 2 1`, `/br yedek ver 3 1`; aktif BR maçında yenir.

Korunan 1.0.1 düzeltmeleri:

- Maçta alttaki oyuncu konum göstergesi kapalıdır; rakip ve takım arkadaşlarının kafa üstü isimleri gizlenir. BR lobisine dönünce eski görünüm geri gelir. Rol etiketi ve TAB ayarları korunur.
- Boş elle veya elde eşya varken maç sandığı açılır. Oyun dışındaki oyuncu aynı sandığı kullanamaz. Son oyuncu çıkınca geçici sandıklar hemen temizlenir; sonraki maçta eski sandık birikmez. Sandık içeriği bütün maç oyuncuları için ortak ve tek kullanımlı paylaşılan ganimettir.
- Hazır haritada her maç sandıkların en az yarısı açık havadaki doğal zemindedir; kalan seçimde ev/çatı adayları da kullanılır. 10 oyuncuda en az 120, 20 oyuncuda en az 160 dış alan sandığı; iki kişilik yönetici testinde en az 120 dış alan sandığı. Önceki maçla konum çakışması engellenir. Kendi özel aday havuzunda uygun açık alan yetersizse kalan boş adaylar kullanılır.
- Güvenli alan **kare sınır duvarıyla** görünür: beklerken mavi, daralırken kırmızı. Üstte alan büyüklüğü ve daralma sayacı vardır. Hasar sınırı görünen duvarla aynıdır; her maçın kendi sınırı vardır. Lobi ve diğer maçların dünya sınırı değiştirilmez.

## Kurulum

1. Sunucuyu **durdur**. Mevcut dünya ve `plugins/BattleRoyalePrototype` klasörünü yedekle.
2. Eski `BattleRoyale-BatiKasabasi-TEST-v0.3.1.jar` dosyasını `plugins` dışına çıkar. Aynı eklentinin iki sürümünü bırakma.
3. Paketteki `KasabaBR` klasörünü sunucunun ana klasörüne, `server.properties` ile **yan yana** koy. `plugins` içine koyma. Bu yeni dünya; gönderdiğin orijinal dünya dosyasına dokunulmaz. Mevcut bir `KasabaBR` dünyasının üzerine yazma.
4. `plugins` içindeki `KasabaBattleRoyale-1.0.3.jar` ve `KasabaHub-1.2.0.jar` dosyalarını sunucunun `plugins` klasörüne koy. Eski Hub JAR'ını çıkar; `plugins/KasabaHub` veri klasörünü koru. Auth, Roles, Adalar ve diğer oyunları silme.
5. Sunucuyu aç. Yeni dünya otomatik yüklenir; lobi, üç arena, 60 iniş noktası ve 3.168 sandık adayı otomatik tanınır. Yeni dünya için Multiverse komutu gerekmez.
6. Oyunda `/br` veya `/oyunlar git battle` yaz. `/oyunlar` sayfasına **Battle Royale** otomatik eklenir. `battle` adıyla daha önce elle bir giriş eklediysen onu `/oyunlar sil battle` ile kaldırıp sunucuyu yeniden başlat.

Eklenti kimliği eski sürümle uyum için `BattleRoyalePrototype` kalır. Yeni arena verileri `plugins/BattleRoyalePrototype/arenalar.yml`, ganimetler `ganimetler.yml` dosyasındadır. Eski dosyalar silinmez; eski tek-arena koordinatları yeni üç arenaya otomatik aktarılmaz.

## Oyuncu komutları

| Komut | İşlev |
|---|---|
| `/br` | BR lobisine gelir, oyun seçme sayfasını açar. Lobide 9. kutudaki pusulaya sağ tık da sayfayı açar. |
| `/br solo` | Tek başına oynanan maça katılır. |
| `/br ikili` | İki kişilik takımlar. Hub arkadaş grubun bölünmez. |
| `/br dörtlü` | En fazla dört kişilik takımlar. Küçük gruplar aynı takımda tamamlanabilir. |
| `/br lobi` veya `/br ayrıl` | Kuyruktan/maçtan çıkar, **BR lobisine** döner. |
| `/br çık` veya Hub'ın `/lobi` komutu | Ana Kasaba lobisine döner; önceki eşya ve oyun modu geri gelir. |
| `/br durum` | Üç arenanın doluluğunu ve durumunu gösterir. |
| `/br yardım` | Komutları gösterir. |

BR lobisinde vurma, kırma, blok koyma ve konteyner kullanma kapalıdır. Maça geçince pusula kaldırılır; tahta kılıç ve kahverengi deri zırh verilir. Ana oyundaki envanter BR'ye taşınmaz; çıkarken geri yüklenir. Oyun envanteri dışarı çıkarılamaz. Takım arkadaşına ve başka maçtaki oyuncuya vurulamaz. Elenen oyuncu BR lobisine döner; son takım kazanır. Ayrılmak elenme sayılır.

## Maç ve ganimet dengesi

- **En az 10, en fazla 20 oyuncu.** 10 kişi olunca 20 saniyelik geri sayım başlar. Sayı 10'un altına inerse durur; 20 kişi dolarsa kalan süre en fazla 5 saniyeye iner. Yönetici iki rakip takım varken test için elle başlatabilir.
- Üç arena bağımsız çalışır. Bekleyen uygun mod doldurulur; oynanan maça sonradan oyuncu sokulmaz. Aynı dünyanın genel sınırı maç için daraltılmaz: daralan alan hasarı yalnızca ilgili maçtakilere uygulanır.
- 2–10 oyuncuda **240**, 20 oyuncuda **320 sandık**; varsayılanlarda sırasıyla **12** ve **16** nadir ganimet sandığı. Bütün bloklar normal **sandık**tır. Ender sandığının kişisel deposu kullanılmaz.
- Her adada 1.056 kayıtlı aday var: 352 iç mekân, 16 çatı, 688 dış mekân. Maç için yalnızca merkezdeki 250×250 alanda kalanlar kullanılır; bu alanın içinde en az 800 aday vardır. Seçimde açık alan kotası uygulanır; dışarıdaki sandıklar şansa bırakılmaz. Bir önceki maçın konumları tekrar seçilmez. Nadir sandıklar mor parçacık ve açıldığında mor başlıkla ayırt edilir.
- Yaygın: zincir/demir zırh, Koruma I–II, taş/demir kılıç, demir balta, Savurma I altın kılıç. Nadir: Keskinlik II demir kılıç, Alev I yay, Hızlı Şarj I arbalet, kalkan, şifa. Epik: Keskinlik III elmas kılıç, Koruma IV elmas zırh, Güç III yay, özel oklar. **Netherit ve Koruma V göğüslük çok ender.** Nadir sandık daha güçlü eşyalara ağırlık verir; bütün eşyalar aynı olasılıkla çıkmaz.
- Yay/arbalet çıkan sandıkta ayrıca **4–8 normal ok garanti**. Temel ok paketleri 1–4; havuzdaki normal ok 8–20, parlayan ok 3–6; zehir/yavaşlık/zayıflık okları 1–3 adet. Roket, mermi ve PUBG silahı yok. Zehir I 4 sn, yavaşlık/zayıflık I 6 sn; güçlü uzun süreli etkiler yok.
- Sıradan temel paketlerde 1–2 pişmiş et, ekmek veya fırın patates olabilir. Her sandıkta yemek zorunlu değildir. Altın havuç ve altın elma daha az. Çiğ et, çürük et ve gereksiz bütün yiyecek çeşitleri eklenmedi.
- **Şifa Lokması** kurabiye simgelidir: açlık doluyken de yenir, anında **2 kalp** doldurur, maksimum canı artırmaz, can sınırını aşmaz. İki kullanım arasında **10 saniye** vardır; dolu canda/erken kullanımda harcanmaz. Varsayılan olarak maçta doğal tokluk ve barışçıl mod yenilemesi kapalı; altın elmanın etkileri çalışır. Ana dünyayı etkilemez.
- Alan önce 180 sn bekler; sonra 600 sn'de 16×16 blokluk kare alana iner (merkezden kenara 8 blok). Dışarıda saniyede 2 can puanı hasar. Maç üst sınırı 20 dakika. Ayarlar `config.yml` içinden değişir; yeniden başlat.

## Kendi lobini yap

Hazır lobinin başlangıcı **KasabaBR: 768.5, 73, -440.5**. 49×49 korumalı başlangıç platformudur; kendi lobi yapını burada kurabilirsin.

Yönetici olarak `/br düzenleme aç` yaz; boş alanlarda korumayı aşarak düzenleyebilirsin. Aktif maçta düzenleme izni verilmez. İşin bitince `/br düzenleme kapat`. Lobi noktasını değiştireceksen önce tüm oyuncular BR'den `/br çık` ile çıksın. İstediğin güvenli zeminde `/br lobi ayarla` yaz. Eski bölgenin dışına taşırsan yeni yer çevresine 49×49 koruma oluşur; arena alanıyla çakışmasına izin verilmez.

Daha büyük/küçük lobi için iki köşede sırayla:

```
/br bölge pos1
/br bölge pos2
/br bölge kaydet lobi
```

Yüksekliğin önemi yok: yatay sınırın tüm yüksekliği korunur. Lobi noktası seçilen bölgenin içinde olmalı.

## Arenayı ve sandıkları değiştirme

Hazır haritada bunları **tekrar yapman gerekmez**. Kendi alanını kurarken yönetici yetkisi (`br.admin`, varsayılan OP) gerekir:

```
/br bitir 1
/br bölge pos1
/br bölge pos2
/br bölge kaydet 1
/br arena ayarla 1
/br doğma tara 1 20
/br sandık tara 1 800
```

Merkezi alanın ortasında ayarla. Aynı adımlar `2` ve `3` için geçerlidir. Lobi/arenalar aynı dünyada olmalı ve birbirine değmemeli. Mevcut kayıtları dışarıda bırakacak bölge daraltması reddedilir.

| Komut | İşlev |
|---|---|
| `/br doğma ekle 1` | Durduğun güvenli yeri Arena 1 iniş adayı olarak ekler. Her takım başka noktaya iner. |
| `/br doğma liste 1` | Kayıt sıralarını gösterir. |
| `/br doğma sil 1 3` | Yalnızca üçüncü iniş kaydını siler. `tümü` hepsini siler. |
| `/br sandık ekle 1` | **Zemine bakarken** yaz: baktığın bloğun üstündeki boş yer aday olur. Sabit sandık koyma. |
| `/br sandık liste 1` | Sandık adaylarını listeler. |
| `/br sandık sil 1 3` | Üçüncü sandık adayını siler. `tümü` hepsini siler. |
| `/br sandık tara 1 800` | Önceki listeyi değiştirip merkezdeki 250×250 alanda 800 güvenli yüzey noktası arar. Hazır haritanın iç mekân analizi bundan daha ayrıntılıdır. |
| `/br başlat 1` | Arena 1'de bekleyen en az iki rakip takımla test maçı başlatır. |
| `/br bitir 1` | Yalnızca Arena 1'i bitirir; sandıkları kaldırır, oyuncuları BR lobisine alır. |

## Sandıktan çıkan eşyaları değiştirme

Eşyayı istediğin büyülerle eline al. Örneğin:

```
/br ganimet ekle ozel-kilic 4 1 1 epik
```

Bu, eldeki eşyanın büyü/ad/açıklamalarını kaydeder. `4` ağırlık, `1 1` en az/en çok adet; seviyeler `yaygın`, `nadir`, `epik`, `efsane`. Ağırlık **yüzde değildir**; havuz ve sandık seviyesine göre göreli seçilme payıdır.

`/br ganimet liste` • `/br ganimet sil ozel-kilic` • `/br ganimet yenile` • `/br şifa ver 3` • `/br yedek ver 2 1` • `/br yedek ver 3 1`

Sonradan değiştirdiğin ganimetler **yeni maçların** sandıklarına yansır. Yiyecek garantisi yoktur; menzilli silaha 4–8 normal ok garantisi korunur. Sarı kalp yiyecekleri ayrı %30 ortak çekilişle eklenir. Bu yiyecekleri ayrıca ganimet olarak kaydetmek oranı artırmaz; havuzdan tekrar seçilmezler.

## Geliştirme ve doğrulama

`mvn -f kasaba-battle/pom.xml verify` Java 21 ile derler. Dünya üretimi `scripts/build-map.py`, bağımsız okuma doğrulaması `scripts/validate-map.py`; Python paketleri `requirements-world.txt` içindedir. Orijinal dünya salt okunur girdidir. `scripts/integration` gerçek Paper üzerinde loopback protokol istemcileriyle test içindir; probe eklentileri dağıtılmaz. Çıkış/geri yükleme testleri için test sunucusunun ana Hub lobisini `/merkez lobi ayarla` ile hazırlamak gerekir; yalnızca BR lobisini ayarlamak ana lobinin yerine geçmez. Sandıklar yazmadan önce günlüğe alınır ve yeniden açılışta temizlenir; dış envanter atomik dosyalarla korunur.

## 1.1.3

Sandık yoğunluğu önceki ayarın %80'ine indirilir; 250×250 ganimet alanı ve en az 12 blok aralık korunur. Varsayılan hedef en az 192 sandık veya oyuncu başına 12,8 adettir; güvenli konum ve mesafe nedeniyle gerçek sayı daha az olabilir. `loot-density-version: 1` tek seferlik geçişi kaydeder; aktif maçlar değişmez. Lobi pusulası envanterden açılır; diğer oyun lobilerine doğrudan geçiş bulunur. Canlı oyuncu aktif maçı terk ederse BR'ye 15 dakika giriş cezası alır; elenenlerin çıkışı serbesttir.

## 1.2.0

Mevcut BR lobi doğma noktası korunarak koruma alanının içine hazırlık kampı eklenir: erzak çadırları, ikmal paraşütü, ahşap kasalar, uyarı şeritleri ve aydınlatma. Oyun alanına/ganimete dokunulmaz. Renkli bilgi yazısı ve galibiyet ilk 5 tablosu bulunur; kazanan takımın bütün üyelerine birer galibiyet yazılır. Yönetici durdurması ve berabere bitiş puan vermez.

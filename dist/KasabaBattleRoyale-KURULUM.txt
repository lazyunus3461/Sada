# Kasaba Battle Royale 1.0.0

Paper **1.21.10**, Java **21**. Gönderilen Batı Kasabası adasından hazırlanmış, **aynı dünyada üç ayrı savaş adası ve ortak BR lobisi**. Solo, İkili ve Dörtlü seçenekleri Minecraft'ın yerleşik sayfasında açılır. PUBG silahları kaldırıldı.

## Kurulum

1. Sunucuyu **durdur**. Mevcut dünya ve `plugins/BattleRoyalePrototype` klasörünü yedekle.
2. Eski `BattleRoyale-BatiKasabasi-TEST-v0.3.1.jar` dosyasını `plugins` dışına çıkar. Aynı eklentinin iki sürümünü bırakma.
3. Paketteki `KasabaBR` klasörünü sunucunun ana klasörüne, `server.properties` ile **yan yana** koy. `plugins` içine koyma. Bu yeni dünya; gönderdiğin orijinal dünya dosyasına dokunulmaz. Mevcut bir `KasabaBR` dünyasının üzerine yazma.
4. `plugins` içindeki `KasabaBattleRoyale-1.0.0.jar` ve `KasabaHub-1.2.0.jar` dosyalarını sunucunun `plugins` klasörüne koy. Eski Hub JAR'ını çıkar; `plugins/KasabaHub` veri klasörünü koru. Auth, Roles, Adalar ve diğer oyunları silme.
5. Sunucuyu aç. Yeni dünya otomatik yüklenir; lobi, üç arena, 60 iniş noktası ve 480 sandık adayı otomatik tanınır. Yeni dünya için Multiverse komutu gerekmez.
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

BR lobisinde vurma, kırma, blok koyma ve konteyner kullanma kapalıdır. Maça geçince pusula kaldırılır. Ana oyundaki envanter BR'ye taşınmaz; çıkarken geri yüklenir. Oyun envanteri dışarı çıkarılamaz. Takım arkadaşına ve başka maçtaki oyuncuya vurulamaz. Elenen oyuncu BR lobisine döner; son takım kazanır. Ayrılmak elenme sayılır.

## Maç ve ganimet dengesi

- **En az 10, en fazla 20 oyuncu.** 10 kişi olunca 20 saniyelik geri sayım başlar. Sayı 10'un altına inerse durur; 20 kişi dolarsa kalan süre en fazla 5 saniyeye iner. Yönetici iki rakip takım varken test için elle başlatabilir.
- Üç arena bağımsız çalışır. Bekleyen uygun mod doldurulur; oynanan maça sonradan oyuncu sokulmaz. Aynı dünyanın genel sınırı maç için daraltılmaz: daralan alan hasarı yalnızca ilgili maçtakilere uygulanır.
- 10 oyuncuda **15**, 20 oyuncuda **30 sandık**; yaklaşık %15'i nadir ganimet. Bütün bloklar normal **sandık**tır. Ender sandığının kişisel deposu kullanılmaz.
- Her adada 160 güvenli aday var: 112 iç mekân, 16 çatı, 32 dış mekân. Bir önceki maçın konumları tekrar seçilmez. Nadir sandıklar mor parçacık ve açıldığında mor başlıkla ayırt edilir.
- Yaygın: zincir/demir zırh, Koruma I–II, taş/demir kılıç, demir balta, Savurma I altın kılıç. Nadir: Keskinlik II demir kılıç, Alev I yay, Hızlı Şarj I arbalet, kalkan, şifa. Epik: Keskinlik III elmas kılıç, Koruma IV elmas zırh, Güç III yay, özel oklar. **Netherit ve Koruma V göğüslük çok ender.** Nadir sandık daha güçlü eşyalara ağırlık verir; bütün eşyalar aynı olasılıkla çıkmaz.
- Yay/arbalet çıkan sandıkta ayrıca **8–16 normal ok garanti**. Normal ok 8–20, parlayan ok 3–6; zehir/yavaşlık/zayıflık okları 1–3 adet. Roket, mermi ve PUBG silahı yok. Zehir I 4 sn, yavaşlık/zayıflık I 6 sn; güçlü uzun süreli etkiler yok.
- Her sandıkta işe yarayan yiyecek var: pişmiş et, ekmek veya fırın patates. Altın havuç ve altın elma daha az. Çiğ et, çürük et ve gereksiz bütün yiyecek çeşitleri eklenmedi.
- **Şifa Lokması** kurabiye simgelidir: açlık doluyken de yenir, anında **2 kalp** doldurur, maksimum canı artırmaz, can sınırını aşmaz. İki kullanım arasında **10 saniye** vardır; dolu canda/erken kullanımda harcanmaz. Varsayılan olarak maçta doğal tokluk ve barışçıl mod yenilemesi kapalı; altın elmanın etkileri çalışır. Ana dünyayı etkilemez.
- Alan önce 180 sn bekler; sonra 600 sn'de 8 blok yarıçapa iner. Dışarıda saniyede 2 can puanı hasar. Maç üst sınırı 20 dakika. Ayarlar `config.yml` içinden değişir; yeniden başlat.

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
/br sandık tara 1 160
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
| `/br sandık tara 1 160` | Önceki listeyi değiştirip güvenli yüzeyleri otomatik arar. Hazır haritanın iç mekân analizi bundan daha ayrıntılıdır. |
| `/br başlat 1` | Arena 1'de bekleyen en az iki rakip takımla test maçı başlatır. |
| `/br bitir 1` | Yalnızca Arena 1'i bitirir; sandıkları kaldırır, oyuncuları BR lobisine alır. |

## Sandıktan çıkan eşyaları değiştirme

Eşyayı istediğin büyülerle eline al. Örneğin:

```
/br ganimet ekle ozel-kilic 4 1 1 epik
```

Bu, eldeki eşyanın büyü/ad/açıklamalarını kaydeder. `4` ağırlık, `1 1` en az/en çok adet; seviyeler `yaygın`, `nadir`, `epik`, `efsane`. Ağırlık **yüzde değildir**; havuz ve sandık seviyesine göre göreli seçilme payıdır.

`/br ganimet liste` • `/br ganimet sil ozel-kilic` • `/br ganimet yenile` • `/br şifa ver 3`

Sonradan değiştirdiğin ganimetler **yeni maçların** sandıklarına yansır. Her sandığa garanti yiyecek ve menzilli silaha garanti ok kuralı korunur.

## Geliştirme ve doğrulama

`mvn -f kasaba-battle/pom.xml verify` Java 21 ile derler. Dünya üretimi `scripts/build-map.py`, bağımsız okuma doğrulaması `scripts/validate-map.py`; Python paketleri `requirements-world.txt` içindedir. Orijinal dünya salt okunur girdidir. `scripts/integration` gerçek Paper üzerinde loopback protokol istemcileriyle test içindir; probe eklentileri dağıtılmaz. Sandıklar yazmadan önce günlüğe alınır ve yeniden açılışta temizlenir; dış envanter atomik dosyalarla korunur.

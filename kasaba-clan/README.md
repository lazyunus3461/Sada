# KasabaKlan 1.0.0

Paper **1.21.10 / Java 21** için Türkçe klan ve iki köşeli claim sistemi. Klan başına **10 üye**, toplam **5.000 yatay blok** arazi varsayılanıdır. Claim yalnızca yöneticinin ayarladığı orman dünyasında alınır. Kayıtlı her warp konumunun merkezindeki **100×100 blokluk alanla** kısmen bile çakışan claim reddedilir. Bir warpın ek/rastgele hedefleri ve özel warplar da hesaba katılır. PvP kuralları değiştirilmez.

## İndir ve kur

[Üç gerekli JAR'ı içeren kurulum ZIP'ini indir](https://github.com/lazyunus3461/Sada/raw/refs/heads/main/dist/KasabaKlan-Kurulum.zip)

1. Sunucuyu durdur. `plugins/KasabaRoles`, `plugins/KasabaWarp` ve varsa `plugins/KasabaKlan` klasörlerini yedekle.
2. Eski KasabaRoles/KasabaWarp JAR'larını `plugins` dışına çıkar. Veri klasörlerini silme.
3. ZIP içindeki **KasabaKlan-1.0.0.jar**, **KasabaRoles-1.2.0.jar**, **KasabaWarp-1.2.0.jar** dosyalarını `plugins` içine koy. KasabaAuth olduğu gibi kalır.
4. Sunucuyu tamamen aç. Orman dünyasında Kurucu/OP olarak `/klan orman ayarla` yaz. Dünya adı değil UUID'si saklanır. `/klan orman bilgi` ile denetle.

Klan etiketi TAB, sohbet ve kafa üstünde rolün yanında görünür: **`[Kurucu] [KSB] Redkit`**. Üyelik/rol değişimi anında yansır; rol renkleri, yetkileri, sıralaması ve özel scoreboard sidebar'ları korunur. Roles ayarlarındaki `manage-tab`, `manage-chat`, `manage-nametags` seçenekleri ilgili gösterimi yönetmeye devam eder. Eski Roles/Warp sürümleriyle kullanma; bu sürümler ortak etiket ve warp koruması API'sini içerir.

## Klan kur ve üyeleri yönet

```text
/klan oluştur KSB Kasaba
/klan davet Redkit
/klan katıl KSB
/klan üyeler
/klan bilgi
/klan liste
/klan sohbet Merhaba arkadaşlar
```

Davet olmadan katılım yoktur. Davet 120 saniye geçerlidir; daveti veren kişi yardımcı/lider yetkisini kaybederse davet kullanılamaz. Davetler ve seçimler sunucu yeniden başlatıldığında temizlenir; üyelikler ve araziler kalıcıdır. Oyuncu aynı anda yalnızca bir klana üye olabilir. Kayıt/giriş tamamlanmadan KasabaAuth bu komutları engeller.

| Komut | Yetki / davranış |
| --- | --- |
| `/klan oluştur <etiket> [ad]` | Klanı olmayan oyuncu; etiketi 2–6 harf/rakam, adı 3–32 karakter |
| `/klan davet <oyuncu>` | Lider veya yardımcı; hedef çevrimiçi olmalı |
| `/klan katıl <etiket>` | Geçerli daveti olan, klansız oyuncu |
| `/klan bilgi [etiket]`, `/klan üyeler [etiket]`, `/klan liste` | Klan bilgisi ve üye/alan sınırları |
| `/klan sohbet <mesaj>` | Yalnızca aynı klanın çevrimiçi üyelerine gider |
| `/klan çıkar <oyuncu>` | Lider veya yardımcı; yardımcı başka yardımcıyı/lideri çıkaramaz |
| `/klan ayrıl` | Üye/yardımcı ayrılır; lider önce devretmeli veya dağıtmalı |
| `/klan yetki <oyuncu> yardımcı` | Lider, üyeyi yardımcı yapar |
| `/klan yetki <oyuncu> üye` | Lider, yardımcı yetkisini kaldırır |
| `/klan lider <oyuncu>` | Liderliği başka üyeye devreder; önceki lider yardımcı olur |
| `/klan dağıt onayla` | Lider; klanı dağıtır ve bütün arazilerini bırakır |
| `/klan yardım` | Türkçe komut özeti |

## İki köşeyle claim al

1. Ormanda `/klan alan seç` yaz.
2. İlk köşe olacak bloğa **sol tıkla**.
3. Diğer köşe olacak bloğa **sağ tıkla**.
4. Gösterilen boyutu denetleyip **`/klan alan al`** yaz.

Seçim açıkken normal blok etkileşimi iptal edilir; elindeki eşya silinmez veya değiştirilmez. Başarılı claim seçim modunu kapatır. Vazgeçmek için `/klan alan iptal` kullan. Seçim 10 dakika sonra veya çıkışta silinir.

Tıklamak yerine ilk köşede durup `/klan alan birinci`, diğer köşede `/klan alan ikinci` de kullanılabilir. İki köşe aynı ayarlı orman dünyasında olmalıdır. Sınır blokları hesaba dahildir: X=200…209 ve Z=200…209 **10×10=100 blok** eder. Yükseklik bütçeye katılmaz; alan dünyanın bütün yüksekliğinde korunur. Araziler ayrı yerlerde olabilir; birbirleriyle çakışamaz.

- `/klan alan liste`: klanının arazi numaraları, sınırları ve büyüklükleri.
- `/klan alan bilgi`: bulunduğun yerin sahibi/numarası.
- `/klan alan bırak 1`: lider/yardımcı, yalnızca kendi klanının 1 numaralı arazisini bırakır.

Tüm üyeler klanın arazilerinde inşa edebilir ve sandıkları kullanabilir; alan alma/bırakma yalnızca lider ve yardımcılara aittir. Üyelikten çıkan/çıkarılan kişinin bu erişimi ve klan etiketi hemen kalkar. Araziler kişinin değil klanın malıdır; üye çıkınca silinmez.

## Warp çevresi ve koruma

Warpın blok koordinatı aşağı yuvarlanır. Örneğin merkez X=0,5 / Z=0,5 ise koruma X=-50…49, Z=-50…49'dur: tam **100×100**. Koruma tam yüksekliğe uygulanır. Bu, warp çevresinde **claim alma yasağıdır**; bu bölgeyi kendiliğinden blok kırmaya kapatmaz.

Warp bilgisi KasabaWarp'ın çalışan durumundan alınır. `/warp noktaekle`, `/warp taşı`, `/warp oluştur` ve `/warp yenile` güncel claimlerle denetlenir. Yeni warp koruması mevcut araziyle çakışıyorsa işlem reddedilir; arazi sessizce silinmez. Dosyaları dışarıdan değiştirip yeniden başlatma ile çakışma oluşturulmuşsa mevcut araziler korunur, konsola hata yazılır ve çakışma çözülene kadar yeni claim alınamaz.

Yabancılara blok kırma/koyma, sandık/kapı/düğme kullanma, kova, hayvan/çerçeve/zırh standı etkileşimleri kapatılır. Önceden açılmış sandıkta üyelik kaybolursa eşya hareketi tekrar denetlenir. Ziyaret ve yürüyüş serbesttir. Oyuncular arası hasar ve klan içi PvP mevcut sunucu kurallarını izler.

Sınırdan sıvı akışı, pistonla taşıma ve hopper aktarımı denetlenir. Aynı klanın alanları içinde otomasyon çalışabilir; başka sahipliğe geçiş engellenir. Claim içindeki bloklar patlama/yayılma/yanma zararından korunur. Başka eklentilerin doğrudan blok değiştirmesi (örneğin WorldEdit), eski istemci katmanları, Bedrock ve diğer claim eklentileri için uyumluluk garantisi verilmez. Aynı alanı yöneten başka claim eklentisiyle birlikte kurma.

## Yönetim ve kalıcı veri

Yönetim yetkisi `kasabaklan.yonet` (varsayılan OP) veya `kasabaroles.admin` (Kurucu). Yönetici olmak kendiliğinden başka klanın korumasını aşmaz.

```text
/klan orman ayarla
/klan orman bilgi
/klan yönetim aç
/klan yönetim kapat
/klan yönet alansil KSB 1
/klan yönet sil KSB onayla
/klan yenile
```

`yönetim aç` geçici koruma geçişidir; çıkışta/kapanışta kalkar ve konsola kaydedilir. Mevcut araziler varken orman dünyası başka dünyaya çevrilemez. Normal kullanım yetkisi `kasabaklan.kullan`, varsayılan herkese açıktır. Türkçe harfsiz `olustur`, `katil`, `uyeler`, `cikar`, `ayril`, `dagit`, `sec`, `birak`, `yonetim`, `ac` biçimleri de kabul edilir.

`plugins/KasabaKlan/klanlar.yml` dünya UUID'si, klanlar, üyeler, yardımcılar ve tam arazi sınırlarını atomik olarak saklar. `config.yml` varsayılanları: `uye-siniri: 10`, `toplam-alan-siniri: 5000`, `arazi-sayisi-siniri: 20`, `kenar-siniri: 1024`, `davet-suresi-saniye: 120`, `etiket-rengi: aqua`. Değişiklikten sonra `/klan yenile` kullan. Sınır azaltılırsa mevcut kayıtlar silinmez; yeni eklemelerde yeni sınır uygulanır.

Bozuk dosyayla `/klan yenile` mevcut çalışan veriyi korur. İlk açılışta dosya okunamazsa eklenti etkin kalıp normal oyuncuların inşa/etkileşimlerini güvenlik için engeller; bütün dünyaları etkileyen bu kilit dosya düzeltildikten ve `/klan yenile` başarıyla çalıştıktan sonra açılır. Veriyi sıfırlamaz. OP/Kurucu gerektiğinde açıkça `/klan yönetim aç` kullanabilir. `/reload` yerine tam yeniden başlatma yap.

## Geliştirme

Bulutta `bash kasaba-clan/scripts/cloud-build.sh`; Roles/Warp bağımlılıklarını kurar ve üç modülün testlerini çalıştırır. Normal JDK 21 / Maven ortamında sırasıyla `mvn -f kasaba-roles/pom.xml install`, `mvn -f kasaba-warp/pom.xml install`, `mvn -f kasaba-clan/pom.xml verify`. Çıktı `kasaba-clan/target/KasabaKlan-1.0.0.jar`.

16 klan birim testi, 20 rol testi ve 21 warp testi bulunur. [Yerel entegrasyon](scripts/integration/README.md) gerçek Paper protokolünü ve sınırlı olay yardımcılarını kullanır. Test Probe JAR'ları üretim paketine konulmaz. Native Minecraft görsel istemcisiyle inceleme yapılmamıştır.

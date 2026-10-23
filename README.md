# Kasaba oyun merkezi

**Yeni: [KasabaBattleRoyale](kasaba-battle/README.md)** — aynı dünyada üç savaş adası, ortak lobi, Solo/İkili/Dörtlü, 10–20 oyuncu, evlerde ve dışarıda yer değiştiren normal/nadir sandıklar ve büyülü ganimet. BR 1.0.1 maçta konum göstergesini ve kafa üstü isimlerini gizler; daralan alanı sınır duvarıyla gösterir. [Dünya + BR kurulum paketi](https://github.com/lazyunus3461/Sada/raw/refs/heads/main/dist/KasabaBattleRoyale-Kurulum.zip). X Arena 1.2.1 yetenekleri hedef olmadan da kullanılır; ıskalanabilir ve boşa atılınca bekleme süresi harcanır.

Güzel bir kasaba meydanı kurmak için [64×64 hazır KasabaKasabasi dünyasını indir](https://github.com/lazyunus3461/Sada/raw/refs/heads/main/dist/KasabaKasabasi-1.21.10.zip) (Java 1.21.10). ZIP içindeki `KURULUM.txt` adımları anlatır. İstersen [KasabaSpawn eklentisini](https://github.com/lazyunus3461/Sada/raw/refs/heads/main/dist/KasabaSpawn-1.0.0.jar) kurup `/kasabaspawn yap` komutuyla kendi düz alanına da inşa edebilirsin. [Tasarım ve kurulum adımları](kasaba-spawn/README.md).

Yeni ana eklenti: **[KasabaHub](kasaba-hub/README.md)** — Minecraft içinde arkadaşlarla buluşulan lobi, yerleşik oyun sayfaları ve arkadaş grupları. Lobide menü pusulası bulunur; `/oyunlar ekle parkur Kasaba Parkur` bulunduğun konumu oyun girişine ekler. Oyun eklentileri bu merkeze bağlanır.

**[KasabaHub kurulum paketini indir](https://github.com/lazyunus3461/Sada/raw/refs/heads/main/dist/KasabaHub-Kurulum.zip)**. KasabaHub'ın üzerine kurulan yeni **[KasabaArena](kasaba-arena/README.md)** eklentisi X Arena, özel kodlu 1v1 ve 2v2 maçlarını ekler. [Arena JAR'ını indir](https://github.com/lazyunus3461/Sada/raw/refs/heads/main/dist/KasabaArena-1.2.1.jar) veya [kurulum paketini indir](https://github.com/lazyunus3461/Sada/raw/refs/heads/main/dist/KasabaArena-Kurulum.zip). Yeni [KasabaAkce](kasaba-akce/README.md) sunucu parası ve market ödemelerini sağlar; [Arena + Akçe paketini indir](https://github.com/lazyunus3461/Sada/raw/refs/heads/main/dist/KasabaArena-Akce-Kurulum.zip). Mevcut [KasabaRoles](kasaba-roles/README.md) kullanılabilir. Yeni oyun merkezi modelinde önceki [warp](kasaba-warp/README.md) ve [klan/claim](kasaba-clan/README.md) çalışmaları arşiv olarak tutulur; kurulum paketine eklenmez.

## KasabaAuth 1.3.0

Hub kullanıldığında doğrulama bittikten sonra Hub'ın ana lobisine geçilir. Hub olmadan aşağıdaki kayıt yeri davranışı korunur. 1.3.0 salt okunur AuthState servisi ve PlayerAuthenticatedEvent ekler; hesap/şifre dosyası biçimi değişmez.

Paper **1.21.10** / Java **21** için bağımsız kayıt ve giriş eklentisi. Oyuncu bağlanınca Minecraft'ın **yerleşik diyalog sayfası** otomatik açılır. Envanter, sandık ve örs ekranı kullanılmaz. İstemci modu, kaynak paketi ve tarayıcı gerekmez.

Bağlanınca önce 2 saniye boyunca büyük ekran başlığıyla **Kasaba’ya Hoş Geldin** görünür; ardından form otomatik açılır. Başlık altın, turkuaz ve yeşil renklerle kalın yazılır. Formun üst başlığında ve üst çubukta aynı renkli yazı kullanılır. Yerleşik form başlığının yazı boyutu Minecraft tarafından sabittir; mod veya kaynak paketi olmadan büyütülemez. Yeni oyuncuda aynı sayfada **Şifre**, **Şifreyi tekrar yaz** alanları ve **Kayıt Ol** düğmesi bulunur. Kayıtlı oyuncuda doğrudan **Şifre** alanı ile **Giriş Yap** düğmesi açılır. Hatalar sayfanın içinde kırmızı yazıyla gösterilir. Doğrulama sırasında form açık kalır; başarıda doğrudan kapanır ve oyuncu oyuna geçer. Arada bekleme veya “Geri dön” sayfası yoktur. **Sunucudan Ayrıl** düğmesi bağlantıyı kapatır. ESC ile form kapatılamaz.

## Sunucuya kurulum / güncelleme

1. Sunucuyu durdur ve oyuncu verileriyle `plugins/KasabaAuth` klasörünü yedekle.
2. Önceki KasabaAuth JAR dosyasını `plugins` dışına çıkar. İki sürümü birlikte bırakma. AuthMe gibi başka bir kayıt eklentisiyle aynı anda kullanma.
3. `KasabaAuth-1.3.0.jar` dosyasını sunucunun `plugins` klasörüne koy.
4. Sunucuyu Java 21 ile aç. Konsolda `KasabaAuth v1.3.0` etkinleşmeli.
5. Minecraft Java **1.21.10** ile bağlan. Kayıt / giriş sayfası otomatik açılır.

Önceki KasabaAuth sürümlerindeki hesap dosyaları ve şifreleri korunur; aynı `plugins/KasabaAuth` veri klasörü kullanılır. AuthMe hesap aktarımı yoktur. Daha önce başka kayıt sistemi kullandıysan hesap geçişini planlamadan o sistemi kaldırma.

`/kasaba` kayıt / giriş sayfasını tekrar açar. `/giris`, `/kayit`, `/login`, `/register` aynı işlemin kısayollarıdır. **Komuta veya sohbete şifre yazma.**

## Kayıt / giriş yerini ayarla

Oyunda istediğin noktada dur, oyuncuların bakmasını istediğin yöne dön ve **`/kayityeri ayarla`** yaz. OP veya KasabaRoles Kurucu yetkisi (`kasabaroles.admin`) ya da `kasabaauth.admin` gerekir. Önce kendi hesabına giriş yapmalısın.

Bu noktayı ayarladıktan sonra **her bağlanışta** yeni ve kayıtlı oyuncular kayıt/giriş noktasına ışınlanır. Doğrulama bitince de burada kalırlar. Son çıkış konumuna dönmezler. Konum, dünya ve bakış yönü `plugins/KasabaAuth/kayit-yeri.yml` dosyasına kaydedilir; tam yeniden başlatmada korunur.

- `/kayityeri bilgi`: mevcut noktayı gösterir.
- `/kayityeri sil`: özel noktayı kaldırır; oyuncular tekrar son bulundukları yerde giriş yapar. Konsoldan da `kayityeri sil` kullanılabilir.

İlk kurulumda kayıt yeri ayarlı değildir; komutla belirleyene kadar eski davranış sürer. Dünya spawnı ve doğma yatağı değiştirilmez; bu ayar yalnızca sunucuya bağlanmayı düzenler, ölüm sonrası doğma noktasını değiştirmez. Güvenli, yerde duran bir nokta seç. Belirlenen dünya yüklenmemişse giriş reddedilir ve yöneticiye hata kaydı yazılır. Hatalı kayıt yeri dosyası da girişleri durdurur; dosyayı yedekleyip konsoldan `kayityeri sil` ile sıfırlayabilirsin. Hesap ve şifre dosyaları korunur.

## Davranış ve sınırlar

- Minecraft'ın yerleşik metin alanında şifre **oyuncunun kendi ekranında görünür**; yıldızla maskeleme yoktur. Şifre diğer oyuncuların sohbetine veya komut günlüğüne gönderilmez.
- Doğrulama tamamlanana kadar hareket, ışınlanma, sohbet, diğer komutlar, blok kırma/koyma, envanter işlemleri, eşya alma/atma ve etkileşimler engellenir. Beklerken hasar ve açlık değişimleri engellenir.
- Bu bir HTML web sayfası veya ayrı giriş lobisi değildir; Minecraft'ın kendi form sayfasıdır. Dünya istemciye yüklenir. Doğma yerini güvenli tut.
- Yalnızca Minecraft Java 1.21.10 ile doğrulandı. Eski istemciler, ViaVersion ve Floodgate/Bedrock ad önekleri desteklenmez.
- Kayıt/girişte oyuncu havada sabit kalsa da vanilla uçma kontrolünün atmasını önlemek için yalnızca bekleme sırasında geçici uçuş izni verilir. Hareket ve uçuşa geçiş engellenir. Girişte, çıkışta ve eklenti kapanırken önceki uçuş durumu geri yüklenir. Buna ek olarak kayıt/giriş oturumu açıkken yalnızca `FLYING_PLAYER` / `FLYING_VEHICLE` atılmaları iptal edilir. Başarılı girişten sonra bu koruma en fazla 5 saniye daha sürer; sonra normal uçma kontrolü devam eder. Ban, operatör atması, giriş süresi ve yanlış şifre kilidi iptal edilmez. Sunucunun genel `allow-flight` ayarı değiştirilmez.
- “Sohbet iletileri doğrulanamıyor” bildirimi Minecraft istemcisinin doğrulanmamış sohbet uyarısıdır. Crack hesaplara açık sunucularda görülebilir. Bu sürüm uyarıyı gizlemez veya sohbet güvenliğini doğrulanmış gibi göstermez; standart Paper API'sinde bu istemci bildiriminin kapatma seçeneği yoktur.
- Şifre değiştirme/sıfırlama, e-posta, çoklu sunucu veritabanı ve AuthMe hesap aktarımı bu sürümde yoktur.
- Hatalı giriş sayacı aynı sunucu sürecinde yeniden bağlanmalarda korunur; sunucu yeniden başlatılınca sıfırlanır.
- Mevcut `online-mode` ayarını yalnızca bu eklenti için değiştirme. Proxy arkasında backend portunu dışarı kapat ve güvenli oyuncu yönlendirmesini sunucu/proxy belgelerine göre yapılandır.
- Güvenilmeyen diğer eklentilerden koruma sağlamaz. Gerçek sunucunda kendi eklenti listenle kontrol et. `/reload` yerine tam yeniden başlatma kullan.

## Ayarlar ve veri

İlk başlangıçta `plugins/KasabaAuth/config.yml` oluşur. Eski sürümün ayarları ilk güncellemede bir kez 6 karaktere geçirilir; hesaplar ve şifreler korunur. Sonraki başlangıçlarda kendi ayarların korunur:

| Ayar | Varsayılan |
| --- | --- |
| Giriş süresi | 120 saniye |
| Hatalı şifre sınırı | 5 |
| Kilit süresi | 300 saniye |
| Şifre uzunluğu | 6–64 karakter |

Hesap dosyaları `plugins/KasabaAuth/accounts/` altında, küçük harfli oyuncu adıyla tutulur. PBKDF2-HMAC-SHA256, 600.000 iterasyon, rastgele 16 bayt tuz ve 256 bit özet kullanılır. Parola hesaplama ana sunucu iş parçacığının dışında yapılır. Form yanıtları oyuncuya ve mevcut oturuma bağlıdır; eski veya tekrar kullanılan form düğmeleriyle giriş yapılamaz. Şifreler form hata mesajlarına veya sunucu günlüğüne yazılmaz. Yedeklerine erişimi de sınırla.

Ayar değişiklikleri tam yeniden başlatmada uygulanır. Mevcut kullanıcıları dışarıda bırakmamak için minimum şifre uzunluğunu sonradan yükseltme.

## Geliştirme ve doğrulama

JDK 21 ve Maven 3.9+ ile:

```sh
mvn -B clean verify
```

Çıktı: `target/KasabaAuth-1.3.0.jar`. Birim testleri: 17. Bulutta mevcut `/workspace/Sada` checkout'unu kullan; kullanıcı istemedikçe Git worktree oluşturma.

Bulut komutları:

```sh
bash /workspace/Sada/scripts/cloud-build.sh
bash /workspace/Sada/scripts/integration/build-probe.sh
# Test sunucusu kapalıyken yeni JAR'ı plugins içine kopyala:
cp /workspace/Sada/target/KasabaAuth-1.3.0.jar /workspace/kasaba-test-server/plugins/
cp /workspace/Sada/target/KasabaAuthFlightProbe.jar /workspace/kasaba-test-server/plugins/
# Ayrı terminalde, durdurmak için stop veya Ctrl+C:
bash /workspace/Sada/scripts/start-test-server.sh
# Sunucu konsolunda Done görüldükten sonra:
cd /workspace/Sada/scripts/integration
npm ci --cache /workspace/.npm --no-audit --no-fund
node smoke.cjs
node flight-regression.cjs
```

Yerel test sunucusu Paper 1.21.10 build 130 ile yalnızca `127.0.0.1:25575` adresinde offline modda çalışır. Dışarı açma ve ayarlarını gerçek sunucuya kopyalama. İndirmelerde TLS ve SHA doğrulaması korunur. İsteğe bağlı Mojang public-key erişim hatası yerel offline test bağlantısını engellemez; lisanslı hesapların online-mode kimlik doğrulaması bu testin kapsamında değildir.

Gerçek sunucuya bağlanan protokol testi 18 kontrol yapar: `allow-flight=false` altında 7 saniyelik havada bekleme sırasında atılmama, doğrulanmadan uçuşa geçişin engellenmesi, giriş sonrası uçuş izninin geri alınması, renkli/kalın başlık ve formdan önce büyük karşılama, sürüm yanıtı, envanter açılmadan doğrudan iki alanlı kayıt formu, üst karşılama çubuğu, hareket/komut engeli, 5 karakterli şifre reddi ve 6 karakterle kayıt, eşleşmeyen şifre reddi, eski form yanıtının reddi, başarılı kayıt ve formun kapanması, doğrudan giriş formu, yanlış/doğru şifre, deneme kilidi, yeniden bağlantıda kilit ve çıkış düğmesi.

Test istemcisinde, `minecraft-data` paketindeki eski diyalog yanıt şemasına yalnızca test kapsamında düzeltme uygulanır: 1.21.10'un gerçek `ServerboundCustomClickActionPacket` / `ByteBufCodecs.lengthPrefixed` şemasına göre VarInt uzunluklu NBT gönderilir. Sunucu veya uygulama doğrulaması gevşetilmez.

Gerçek Minecraft istemcisinde görsel inceleme ve başka eklentilerle uyumluluk kontrolü ayrıca yapılmalıdır.

`flight-regression.cjs` ek olarak 7 uçma regresyon kontrolü yapar. Yalnızca loopback test sunucusunda çalışan `KasabaAuthFlightProbe` yardımcı eklentisi, Paper 1.21.10’un gerçek uçma sayacını taşmış duruma getirir ve gerçek `PlayerKickEvent` nedenlerini tetikler. Eski 1.1.3 sürümü bu senaryoda atılır; yeni sürüm kayıt/giriş ve 5 saniyelik geçişte korur. Süre dolunca uçma atılmasının ve kayıt sırasında normal PLUGIN atmasının çalıştığı da sınanır. Yardımcı eklenti üretim JAR’ına eklenmez; gerçek sunucuya yalnızca KasabaAuth JAR’ını koy.

Kayıt noktası testi: repo kökünde `node scripts/integration/spawn-regression.cjs`. KasabaRoles ve yerel FlightProbe test yardımcısı bulunmalıdır. Altı grup, yetki denetimini, nokta ayarlama/bilgiyi, son çıkış yerinden farklı noktada yeniden bağlanmayı, giriş sırasında başka ışınlanmaların engellenmesini, giriş sonrası aynı yerde kalmayı ve yeni kayıt olmayı sınar. Çıktıdaki `PERSIST_USER` adını al, test sunucusunu tamamen kapatıp aç ve `KASABA_SPAWN_PERSIST=1 KASABA_SPAWN_USER=<ad> node scripts/integration/spawn-regression.cjs` çalıştır. Bu aşama kalıcılığı ve `sil` sonrası eski davranışın geri gelmesini sınar. Ardından normal kayıt ve uçma testlerini çalıştır. Yardımcının kayıt yeri test kanalı da yalnızca loopback sunucusunda bulunur; üretim JAR'ına dahil değildir.

Auth entegrasyonu: `tr.kasaba.auth.api.AuthState` yalnızca giriş durumunu sorgular; kimlik doğrulaması yapmaz. `PlayerAuthenticatedEvent`, hesap doğrulanıp form kapandıktan sonra ana iş parçacığında yayınlanır. Hub bu olayı dinler.

## Yaratıcı adalar ve bilgi kitabı

[KasabaAdalar 1.3.2](kasaba-adalar/README.md) oyuncu başına tek yaratıcı ada, 100 blok aralıklı dolu tabanlı 300 ada ve otomatik Adanı Yap oyun girişini sağlar. İlk `/ada` kullanımında ve pusuladan ada girişinde yaratıcı mod hazırlanır; adada sürekli gündüz ve açık hava vardır. [Ada + Hub + dünya kurulum paketi](https://github.com/lazyunus3461/Sada/raw/refs/heads/main/dist/KasabaAdalar-Kurulum.zip) yeni KasabaHub 1.2.0 bilgi kitabını da içerir.

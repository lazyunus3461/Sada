# KasabaWarp 1.1.0

Paper **1.21.10 / Java 21** için bağımsız warp eklentisi. `/warp` bir envanter menüsü açar; `/warp pazar` aynı noktaya komutla götürür. Hazırlık **3 saniye**, başarılı ışınlanmaların arası **20 saniye**dir. Hareket, hasar ve `/warp iptal` hazırlığı iptal eder. İptal edilen veya başka eklentinin engellediği ışınlanmada kullanım arası süre başlamaz.

## İndir ve kur

[KasabaWarp-1.1.0.jar indir](https://github.com/lazyunus3461/Sada/raw/refs/heads/main/dist/KasabaWarp-1.1.0.jar)

1. Sunucuyu durdur. Önceki KasabaWarp JAR'ını `plugins` dışına çıkar ve yeni JAR'ı koy. `plugins/KasabaWarp` veri klasörünü koru; eski noktalar yüklenir. KasabaAuth ve KasabaRoles dosyalarını kaldırma.
2. Sunucuyu yeniden aç. OP veya KasabaRoles Kurucu olarak giriş yap.
3. Noktanın olmasını istediğin **güvenli zeminde** dur, istediğin yöne bak, simge olacak eşyayı ana eline al ve `/warp oluştur pazar 1-3` yaz.
4. `/warp` ile menüden seç veya `/warp pazar` kullan.

Başlangıçta örnek warp oluşturulmaz. Her nokta senin belirlediğin yerde oluşur. Eklenti dünya spawnını, KasabaAuth kayıt yerini veya ev/yatak konumunu değiştirmez.

## Türkçe komutlar

| Komut | İşlev |
| --- | --- |
| `/warp` veya `/warplar` | İzin verilen warp noktalarının envanter menüsünü açar |
| `/warp pazar` | Pazar'a ışınlanma başlatır |
| `/warp bilgi pazar` | İsim ve açıklamayı gösterir; yöneticiye konum/yetki de gösterilir |
| `/warp iptal` | Hazırlığı durdurur |
| `/warp yardım` | Komutları gösterir |
| `/warp oluştur pazar` | Bulunduğun yere nokta oluşturur |
| `/warp oluştur orman 1-3` | Ana eldeki eşyayı simge yapar; 1. sıra 3. kutuya koyar |
| `/warp simge orman 1-3` | Mevcut warpın simgesini ve menü yerini değiştirir; konumlarını korur |
| `/warp simge orman` | Yalnızca eldeki eşyayı simge yapar; menü yerini korur |
| `/warp noktaekle orman` | Aynı dünyadaki mevcut konumunu ormanın rastgele hedeflerine ekler |
| `/warp noktalar orman` | Yöneticiye numaralı konum/yön listesini gösterir |
| `/warp noktasil orman 2` | Yalnızca listedeki 2. konumu siler; son konum silinemez |
| `/warp taşı pazar` | 1. konumu ve bakış yönünü günceller; ek konumları korur |
| `/warp sil pazar` | Noktayı siler |
| `/warp isim pazar Pazar Yeri` | Menüde ve mesajlarda görünen adını değiştirir |
| `/warp açıklama pazar Alışveriş alanı` | Menü açıklamasını değiştirir |
| `/warp yetki pazar özel` | Noktayı yetkiye bağlar |
| `/warp yetki pazar herkes` | Noktayı herkese açar |
| `/warp gönder Redkit pazar` | Çevrimiçi oyuncu için ışınlanma başlatır; oyuncunun erişim ve süre kuralları uygulanır |
| `/warp yenile` | Dosyaları yeniden yükler; mevcut kullanım arası süreleri sıfırlamaz |

Yönetim için `kasabawarp.yonet` (varsayılan OP) veya `kasabaroles.admin` gerekir. Normal kullanım yetkisi `kasabawarp.kullan`, varsayılan herkese açıktır. Kurucu dahil bütün oyunculara aynı hazırlık ve kullanım arası süreleri uygulanır.

`oluştur/olustur`, `taşı/tasi`, `açıklama/aciklama`, `gönder/gonder`, `yardım/yardim`, `özel/ozel` biçimleri kabul edilir. Warp kimliği en fazla 32 harf/rakam/`_`/`-` içerebilir. Türkçe harfler kimlikte standartlaştırılır: `Çarşı` → `carsi`; `/warp çarşı` yine çalışır. Görünen isimde Türkçe harfler ve boşluk kullanılabilir. Komut adları warp kimliği olarak kullanılamaz.

## Elindeki eşya ve menü kutusu

Örneğin eline bir meşe fidanı alıp `/warp oluştur orman 1-3` yaz. Menüde orman, **1. sıra 3. kutuda meşe fidanıyla** görünür. Eşya elinden alınmaz; menüdeki kopyanın adedi birdir. Eşyanın modeli, kafa dokusu ve diğer görsel verileri saklanır; menü adı/açıklaması warpın adına ve açıklamasına göre yazılır. Boş elle oluşturursan varsayılan simge ender incisidir; `/warp simge` kullanırken elinde eşya olmalıdır.

Yer belirtmezsen otomatik boş kutu kullanılır. `1-1` ile `5-9` arası ilk sayfadaki sabit kutulardır; 6. sıra sayfa/kapat/bilgi düğmelerine ayrılır. Dolu bir sabit kutuya başka warp konulamaz. Yetkisiz oyuncuya gizlenen özel warpın sabit kutusu boş kalır. Otomatik yerleşen diğer warplar boş alanlara ve gerekirse sonraki sayfalara geçer.

## Tek orman simgesi, birden fazla kayıtlı konum

1. Orman dünyasında ilk güvenli noktada dur, eline simgeyi al ve `/warp oluştur orman 1-3` yaz. Orman zaten varsa bu adımı atla; simge için `/warp simge orman 1-3` kullan.
2. **Aynı dünyada** ikinci güvenli noktaya git ve `/warp noktaekle orman` yaz.
3. Başka noktalar için 2. adımı tekrarla. Bakış yönün de her noktayla birlikte kaydedilir.
4. Oyuncu `/warp orman` yazınca veya tek orman simgesine tıklayınca kayıtlı konumlardan biri **rastgele, eşit olasılıkla** seçilir. Aynı konum peş peşe seçilebilir. Kaydetmediğin bir koordinat üretilmez.

Listeyi `/warp noktalar orman` ile gör; örneğin ikinci kaydı `/warp noktasil orman 2` ile kaldır. İlk kaydı silersen sıradaki ilk konum olur; numaralar güncellenir. Son konumu kaldırmak için tüm warpı `/warp sil orman` ile sil. `/warp taşı orman` yalnızca birinci konumu taşır. Çoklu konum varken başka dünyaya taşıma veya farklı dünyadan ekleme reddedilir. Bütün konumlar aynı özel erişimi, tek menü simgesini ve aynı 20 saniyelik kullanım aralığını paylaşır.

## VIP / role özel nokta

Kurucu olarak noktada dur ve şu komutları kullan:

```text
/warp oluştur vipalan
/warp isim vipalan VIP Alanı
/warp yetki vipalan özel
/rol yetki vip kasabawarp.git.vipalan ver
```

VIP rolü olan oyuncular noktayı görür ve kullanır. Yetkisi olmayanın menüsünde ve TAB tamamlamasında görünmez; doğrudan komutla da gidemez. Hazırlık başladıktan sonra yetki geri alınırsa ışınlanmadan önce tekrar denetlenir. Yöneticiler özel noktalara erişebilir. KasabaRoles zorunlu bağımlılık değildir; başka bir yetki sistemi de aynı `kasabawarp.git.<kimlik>` düğümünü verebilir.

KasabaRoles joker yetkileri kayıtlı düğümlere genişletir. Sonradan yeni warp eklendiğinde daha önce verilmiş joker kurallarını güncellemek için `/rol yenile` kullan. Tam düğüm adlarıyla verilen yetkiler bunu gerektirmez.

## Davranış ve dosyalar

- Menü 45 noktayı bir sayfada gösterir; daha fazlası için önceki/sonraki sayfa düğmeleri vardır. Gösterim eşyaları alınamaz. Özel erişim ve nokta değişiklikleri açık menülere de yansır.
- Dünya, tüm konumlar ve bakış yönleri, isim, açıklama, erişim, simge ve menü kutusu `plugins/KasabaWarp/warplar.yml` içinde atomik olarak saklanır. Tam yeniden başlatmada korunur. 1.0.0 dosyaları tek konumlu warp olarak yüklenir.
- `config.yml`: `hazirlik-saniye: 3`, `kullanim-arasi-saniye: 20`. Hazırlık 0–60, kullanım arası 0–3600 saniye olabilir. Değişiklikten sonra `/warp yenile` kullan.
- Kullanım arası süre gerçek geçen zamandır; yeniden bağlanma veya `/warp yenile` bunu sıfırlamaz. Sunucu yeniden başlatıldığında süreler sıfırlanır. İsimlerde büyük/küçük harf değişimiyle süre atlatılamaz.
- Başını çevirmek hazırlığı iptal etmez. Yaklaşık 0,1 bloktan fazla yer değiştirme, gerçek hasar, ölüm, ayrılma, hedefin düzenlenmesi/silinmesi veya başka bir ışınlanma iptal eder. Araçtayken veya yolcun varken önce inmen gerekir.
- Hedef chunk asenkron hazırlanır. Geciken/iptal edilen yükleme sonradan oyuncuyu ışınlamaz. Hazırlık ve erişim, ışınlanmadan hemen önce tekrar kontrol edilir. Başka eklentinin `PlayerTeleportEvent` iptali korunur; KasabaAuth giriş kilidi aşılmaz.
- Yüklü olmayan dünyaya veya dünya sınırı dışına gidilmez. Dünya ismi yerine UUID kullanılır. Güvenli bir platformu yönetici belirler; eklenti blok yerleştirmez, tehlikeli bir koordinatın yerine başka bir konum seçmez.
- Bozuk dosyayla ilk açılışta eklenti devre dışı kalır ve veriye dokunulmaz. Hatalı `/warp yenile` mevcut çalışan veriyi korur. Dosyaları elle düzenlemeden önce yedekle; Bukkit `/reload` yerine tam yeniden başlatma kullan.

## Geliştirme

Bu klasörde JDK 21 ve Maven ile `mvn -B clean verify`. Bulutta `bash /workspace/Sada/kasaba-warp/scripts/cloud-build.sh`. Çıktı: `target/KasabaWarp-1.1.0.jar`. 21 birim testi ve [yerel protokol testleri](scripts/integration/README.md) bulunur. Test yardımcı JAR'ı üretim JAR'ına eklenmez.

Gerçek Minecraft istemcisinde görsel inceleme, Bedrock/ViaVersion, başka warp yöneticileri ve çoklu sunucu ağları doğrulanmadı. Para/ekonomi, kişisel ev, kaydedilmemiş rastgele koordinata ışınlanma ve sunucular arası taşıma bu sürümün kapsamı dışındadır.

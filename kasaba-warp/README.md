# KasabaWarp 1.0.0

Paper **1.21.10 / Java 21** için bağımsız warp eklentisi. `/warp` bir envanter menüsü açar; `/warp pazar` aynı noktaya komutla götürür. Hazırlık **3 saniye**, başarılı ışınlanmaların arası **20 saniye**dir. Hareket, hasar ve `/warp iptal` hazırlığı iptal eder. İptal edilen veya başka eklentinin engellediği ışınlanmada kullanım arası süre başlamaz.

## İndir ve kur

[KasabaWarp-1.0.0.jar indir](https://github.com/lazyunus3461/Sada/raw/refs/heads/main/dist/KasabaWarp-1.0.0.jar)

1. Sunucuyu durdur ve JAR dosyasını `plugins` klasörüne koy. KasabaAuth ve KasabaRoles dosyalarını kaldırma.
2. Sunucuyu yeniden aç. OP veya KasabaRoles Kurucu olarak giriş yap.
3. Noktanın olmasını istediğin **güvenli zeminde** dur, istediğin yöne bak ve `/warp oluştur pazar` yaz.
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
| `/warp taşı pazar` | Mevcut noktanın konumunu ve bakış yönünü günceller |
| `/warp sil pazar` | Noktayı siler |
| `/warp isim pazar Pazar Yeri` | Menüde ve mesajlarda görünen adını değiştirir |
| `/warp açıklama pazar Alışveriş alanı` | Menü açıklamasını değiştirir |
| `/warp yetki pazar özel` | Noktayı yetkiye bağlar |
| `/warp yetki pazar herkes` | Noktayı herkese açar |
| `/warp gönder Redkit pazar` | Çevrimiçi oyuncu için ışınlanma başlatır; oyuncunun erişim ve süre kuralları uygulanır |
| `/warp yenile` | Dosyaları yeniden yükler; mevcut kullanım arası süreleri sıfırlamaz |

Yönetim için `kasabawarp.yonet` (varsayılan OP) veya `kasabaroles.admin` gerekir. Normal kullanım yetkisi `kasabawarp.kullan`, varsayılan herkese açıktır. Kurucu dahil bütün oyunculara aynı hazırlık ve kullanım arası süreleri uygulanır.

`oluştur/olustur`, `taşı/tasi`, `açıklama/aciklama`, `gönder/gonder`, `yardım/yardim`, `özel/ozel` biçimleri kabul edilir. Warp kimliği en fazla 32 harf/rakam/`_`/`-` içerebilir. Türkçe harfler kimlikte standartlaştırılır: `Çarşı` → `carsi`; `/warp çarşı` yine çalışır. Görünen isimde Türkçe harfler ve boşluk kullanılabilir. Komut adları warp kimliği olarak kullanılamaz.

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
- Dünya, tam konum, bakış yönü, isim, açıklama ve erişim `plugins/KasabaWarp/warplar.yml` içinde atomik olarak saklanır. Tam yeniden başlatmada korunur.
- `config.yml`: `hazirlik-saniye: 3`, `kullanim-arasi-saniye: 20`. Hazırlık 0–60, kullanım arası 0–3600 saniye olabilir. Değişiklikten sonra `/warp yenile` kullan.
- Kullanım arası süre gerçek geçen zamandır; yeniden bağlanma veya `/warp yenile` bunu sıfırlamaz. Sunucu yeniden başlatıldığında süreler sıfırlanır. İsimlerde büyük/küçük harf değişimiyle süre atlatılamaz.
- Başını çevirmek hazırlığı iptal etmez. Yaklaşık 0,1 bloktan fazla yer değiştirme, gerçek hasar, ölüm, ayrılma, hedefin düzenlenmesi/silinmesi veya başka bir ışınlanma iptal eder. Araçtayken veya yolcun varken önce inmen gerekir.
- Hedef chunk asenkron hazırlanır. Geciken/iptal edilen yükleme sonradan oyuncuyu ışınlamaz. Hazırlık ve erişim, ışınlanmadan hemen önce tekrar kontrol edilir. Başka eklentinin `PlayerTeleportEvent` iptali korunur; KasabaAuth giriş kilidi aşılmaz.
- Yüklü olmayan dünyaya veya dünya sınırı dışına gidilmez. Dünya ismi yerine UUID kullanılır. Güvenli bir platformu yönetici belirler; eklenti blok yerleştirmez, tehlikeli bir koordinatın yerine başka bir konum seçmez.
- Bozuk dosyayla ilk açılışta eklenti devre dışı kalır ve veriye dokunulmaz. Hatalı `/warp yenile` mevcut çalışan veriyi korur. Dosyaları elle düzenlemeden önce yedekle; Bukkit `/reload` yerine tam yeniden başlatma kullan.

## Geliştirme

Bu klasörde JDK 21 ve Maven ile `mvn -B clean verify`. Bulutta `bash /workspace/Sada/kasaba-warp/scripts/cloud-build.sh`. Çıktı: `target/KasabaWarp-1.0.0.jar`. Dokuz birim testi ve [yerel protokol testleri](scripts/integration/README.md) bulunur. Test yardımcı JAR'ı üretim JAR'ına eklenmez.

Gerçek Minecraft istemcisinde görsel inceleme, Bedrock/ViaVersion, başka warp yöneticileri ve çoklu sunucu ağları doğrulanmadı. Para/ekonomi, kişisel ev, rastgele ışınlanma ve sunucular arası taşıma bu sürümün kapsamı dışındadır.

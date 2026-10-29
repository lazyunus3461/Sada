# KasabaTR • Skyblock güncellemesi 1.5.0

Bu paket Skyblock 1.4.0 ve Roles 1.7.0 içerir. Hub 1.12.2, Evcil 1.2.1, Parkur 1.2.1 ve Labirent 1.0.0 önceki 1.4.0 dağıtımıyla byte olarak aynıdır. Kaynak kod, oyuncu rehberi, VIP tablosu, araştırma notları, test kanıtları ve donanım önerisi ZIP içindedir. Canlı sunucuya uzaktan kurulmadı.

## Kurulum

1. Sunucuyu `stop` ile tamamen kapat. Dünya dosyalarını ve `plugins/KasabaSkyblock`, `plugins/KasabaRoles`, `plugins/KasabaEvcil` klasörlerini birlikte yedekle. SQLite verisini çalışan sunucudan ayrı WAL dosyalarıyla gelişigüzel kopyalama.
2. Önceki 1.4.0 dağıtımı kuruluysa yalnız **KasabaSkyblock-1.4.0.jar** ve **KasabaRoles-1.7.0.jar** dosyalarını değiştir. Aynı eklentinin eski JAR'larını plugins içinde bırakma. Diğer dört JAR aynıdır. Daha eski paketten geliyorsan altı JAR'ın sürümlerini listedekiyle eşleştir.
3. Dünyaları, ayar/veri klasörlerini, `skyblock.db` dosyasını veya oyuncu kayıtlarını silme. `/reload` kullanma. Paper **1.21.10**, **Java 21** ve mevcut **KasabaAuth 1.4.2 / KasabaAkce 1.1.1** kurulumu kalır; Auth/Akçe bu altılı pakete dahil değildir.
4. Sunucuyu aç. Günlükte Skyblock 1.4.0 ve Roles 1.7.0'ın etkinleştiğini kontrol et. Var olan rol atamaları/VIP süreleri korunur; dört VIP kademesi ve chest hakları ilk geçişte kaydedilir. Eski Roles ayarına geçiş yedeği oluşturulur.

Yeni ortak alanların kasabada kullandığı yerler: buğday x45..59/z-35..-25; mera x61..79/z9..35; kasalar x-17..17/z-59..-47; Yonca x-61..-45/z9..21. Bu alanları oluşturan ilk geçişte bölgedeki bloklar düzenlenir; önce dünya yedeğini al. Daha sonraki açılışlar bütün kasabayı baştan üretmez. Eski kişisel tarla kayıtları silinmez ama erişime açılmaz/yeni kişisel tarla oluşturulmaz.

## Oyuncuya görünecek akış

- `/market`: Skyblock malzeme alışverişi ve satış. Dokuz kaynak kategorisi, okunaklı fiyat/adet/toplam, son onay, sabit alt sekmeler. **VIP satışı burada yoktur.**
- `/sb`: ada, anlaşılır görev ve İlk Yardım rehberi, geliştirme, takım/ziyaretçi/kooperatif, ayrı **VIP ve Kredi** bölümü. Dört kademe: VIP / VIP+ / MegaVIP / UltraVIP.
- `/warp sb tarla`: herkesin birlikte kullandığı tek buğday tarlası. 154 ürün ve tek 15 dakika saati. Normal oyuncu hasat yapabilir, çevreyi kıramaz.
- `/warp yoncatarla`: tek ortak Yonca tarlası. Önce gelen mevcut 24 bitkiyi toplar. Varsayılan her bitki 1 Yonca, 15 dakikada yenilenme ve tıklanabilir “Yonca yenilendi!” duyurusu. Eski **Mühür bakiyesi aynı kayıtla Yonca oldu**, kayıp veya ikinci bakiye yok.
- `/warp sb hayvanlar`: aynı merada inek/domuz/tavuk; 12 hayvan, varsayılan kesimden 30 saniye sonra yeniden doğuş. Ürünler çantaya gelir.
- `/warp sb kasalar`: beş özgün kasa. Sol tık ödüller/oranlar, sağ tık açılış onayı. Kişisel anahtar ve açılış sayısı/geçmişi.
- `/sb jenerator`: küçük yüzey lav–taş–su düzeneği, meşe yan/üst kapaklar; çekirdek tek oyun tikinde yenilenir. Değiştirilmemiş eski şablon güvenli dönüşür. Oyuncu yapısına rastlanırsa dönüşüm durur, eski düzenek çalışır.
- `/sb minyon`: açık meslek, enerji, sandık ve kesin geliştirme bedeli. Ürünler kendi sandığına gider. Elle besleme varsayılan minyon başına günlük toplam 60 dakika; bütün üyeler/kooperatif ortak kullanır. Bakıcı kendi sandığındaki gerçek ekmeği dağıtır.

Depolu 12 spawner, fiziksel ağaç dikip kesen Oduncu, otomatik hasat pistonu, topluluk pazarı, OP testi, yetkili envanter/sandık inceleme, ada havası, eşya/XP koruması ve önceki oyunlar korunur. Spawner/minyon çevrimdışı üretimi en fazla 6 saat ve yakıt/depo sınırlarına bağlıdır; ada sürekli yüklenmez. Piston gerçek yüklenmiş bitkileri toplar, çevrimdışı hayali ürün üretmez.

## Yönetici kısayolları

- Kendi testin: `/sb test ac`; bitince `/sb test kapat`. Üretim kapasitesi ve sahiplik sürer; testte verilen gerçek eşyalar kalır.
- Altın: `/sb yonet altin Oyuncu miktar`. Kredi: `/sb yonet kredi Oyuncu miktar`. Yonca: `/sb yonet yonca Oyuncu miktar`; eski `muhur` kısayolu korunur.
- Ödeme sonrası yükleme: konsoldan `sb yonet krediyukle Oyuncu TL benzersiz-siparis-kodu`. 1 TL temel 1 Kredi, açık %100 bonusla 2 Kredi. Aynı sipariş tekrar çalışsa da iki kez bakiye verilmez. 5–50.000 TL aralığı.
- Gerçek PayTR/diğer sağlayıcı hesabı, ödeme sitesi ve imzalı webhook bağlantısı bu pakette yoktur. `credits.shop-url` HTTPS mağaza adresi içindir; bilgi ekranı kendi başına para tahsil etmez.
- Rolleri deneme: `/rol ver Oyuncu vip|vipplus|megavip|ultravip`. Tüm VIP özellikleri ve süreler oyuncu rehberindeki tabloda yazılıdır.
- Düzenleme: `/yonet duzenleme ac|kapat`. 24 saat kaldırma: `/sb Oyuncu 24saatkaldir`. Diğer ada: `/sb ada Oyuncu`. Salt okunur bakış: `/envanter Oyuncu`, `/chest Oyuncu [1-4]`.

Yeni ayarlar mevcut config dosyasına otomatik yazılmasa da kodda varsayılanları çalışır. Değiştirmek için config'e şu bölümleri ekleyebilirsin:

```yaml
minions:
  maximum-offline-hours: 6
  fuel-minutes-per-coal: 30
  manual-minutes-per-day: 60
public-areas:
  animal-respawn-seconds: 30
clover-field:
  renewal-minutes: 15
  reward-per-plant: 1
credits:
  per-try: 1
  campaign-enabled: true
  bonus-percent: 100
  shop-url: ''
chests:
  pages-by-tier:
    0: 1
    1: 2
    2: 3
    3: 4
    4: 4
```

Aynı YAML bölümünü iki kez yazma; mevcut bölüme eksik satırları ekle. Sesli diğer oyunlar için mevcut Simple Voice Chat kurulumu/istemci modu gerekebilir; Skyblock için kaynak paketi veya yeni ekonomi eklentisi gerekmez. Donanım ve testlerin sınırı için ZIP içindeki `SUNUCU-GEREKSINIMLERI.md` ve `TEST-KAPSAMI.md` dosyalarına bak.

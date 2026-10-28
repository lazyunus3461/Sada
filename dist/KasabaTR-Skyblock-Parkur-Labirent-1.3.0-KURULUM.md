# KasabaTR 1.3.0 kurulumu

Java 21 / Paper 1.21.10. Bu paket altı eklenti içerir. Dördü güncellenir: **KasabaSkyblock 1.2.0, KasabaHub 1.12.2, KasabaRoles 1.6.2, KasabaEvcil 1.2.1**. Parkur 1.2.0 ve Labirent 1.0.0 önceki paketle aynıdır.

1. Sunucuyu tamamen durdur; dünya ve eklenti veri klasörlerinin birlikte yedeğini al.
2. `plugins` içindeki eski Skyblock, Hub, Roles ve Evcil JAR dosyalarını kaldır. **Veri klasörlerini ve dünyaları silme.** Aynı eklentinin iki sürümünü bırakma.
3. ZIP içindeki `plugins/` klasöründen yeni dört JAR'ı koy. Parkur/Labirent eski kurulu güncel sürümse değiştirmek zorunda değilsin. Mevcut Auth ve Akçe kurulu kalır.
4. Başlat ve `Done` satırını bekle; `/reload` kullanma.
5. OP hesabınla `/sb gir`, `/sb ev` ve **`/sb test ac`** yaz. Test bitince **`/sb test kapat`** kullan. Test eşyaları ve ada değişiklikleri kalır.

## Hızlı deneme

- `/market`: kategori → ürün → adet → onay. Ürün sat ayrı düğmededir.
- `/sb jenerator`: ada içinde 3×3 lavsız jeneratör. Boş yer yoksa mevcut yapıyı silmez. Eski değiştirilmemiş jeneratör dönüştürülür.
- `/sb rehber`, `/gorevler`: kısa hedefler,32/128 blok ödülleri,500 ada Altınıyla ilk minyon.
- `/sb minyon`: üretici + Bakıcı; ekmek gerçek enerji kaynağıdır.
- `/warp sb tarla`: buğday biç,20 saniye yenilenme, Çiftçi Efe'ye sat.
- `/chest 1`,2,3,4:54'er yuva; Oyuncu/ VIP/ VIP+/ VIP Ultra sırasıyla 1/2/3/4 sayfa. `chests.pages-by-tier` ile değişir; OP hepsini açar.
- `/sb set kar`, `/sb set yagmur`, `/sb set gunes`: kendi adanın havası; iklim birkaç saniyede güncellenir. Diğer adalar etkilenmez.
- `/evcil kapat`, `/evcil ac`: aynı evcili ücretsiz gizle/göster, oyunculara çarpmaz.
- `/rol ver Oyuncu oncu` veya `/beta ver Oyuncu`: bakımdaki çalışır oyunlara test girişi; yönetim yetkisi vermez.
- Skyblock'ta ölümde eşya ve XP korunur; bu sürümden önce lavda yok olmuş kayıt dışı eşyalar otomatik geri getirilemez.
- `/lobi` → Oyunlar/Skyblock: son güvenli kayıtlı konum ve yön geri gelir.

Ana oyun menüsü iki sütundur; yalnız Skyblock üstte geniş tıklanabilir giriş şerididir. İlk Yardım Skyblock içindedir. Bu şerit vanilla gri düğmeyle aynı göründüğü iddia edilen özel bir kaynak paketi değildir.

Bütün ayrıntılar SKYBLOCK-REHBER.md içindedir. Test raporları gerçek yerel Paper ve protokol istemcileriyle sınırlıdır; canlı sunucuna erişilip kurulmuş sayılmaz. Dünya/veri klasörü silmeden güncelle. KAYNAK.zip kendi eklenti kaynaklarını içerir; test adaptörü içermez.

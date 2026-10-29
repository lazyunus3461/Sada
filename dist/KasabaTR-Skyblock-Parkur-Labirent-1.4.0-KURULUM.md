# KasabaTR 1.4.0 güncellemesi

**Paper 1.21.10 / Java 21.** Paket altı eklenti içerir: Skyblock **1.3.0**, Hub 1.12.2, Roles 1.6.2, Evcil 1.2.1, Parkur 1.2.1, Labirent 1.0.0. Önceki 1.3.0 dağıtımına göre Skyblock ve Parkur değişir; kalan dört JAR birebir korunur. Dağıtım ve Skyblock sürüm numaraları bu yüzden farklıdır.

1. Sunucuyu tamamen durdur. Dünyaları ve eklenti veri klasörlerini birlikte yedekle.
2. Eski `KasabaSkyblock-*.jar` ve `KasabaParkur-*.jar` dosyalarını kaldır; ZIP içindeki **KasabaSkyblock-1.3.0.jar** ve **KasabaParkur-1.2.1.jar** dosyalarını `plugins` içine koy. Aynı eklentinin iki sürümünü bırakma. **Veri klasörlerini ve dünyaları silme.**
3. Önceki 1.3.0 paketi kuruluysa diğer dört JAR'ı değiştirmen gerekmez. Daha eski sürümden geliyorsan paketteki altı JAR'ı birlikte güncelle. Mevcut **KasabaAuth 1.4.2 ve KasabaAkce 1.1.1** kurulu kalmalıdır; bu iki bağımlılık bu ZIP'te bulunmaz.
4. Sunucuyu başlat, `Done` satırını bekle. `/reload` kullanma. Canlı sunucuya otomatik kurulum yapılmaz.
5. OP hesabınla `/sb gir`, `/sb ev`, ardından **`/sb test ac`**. Test bitince **`/sb test kapat`**. Bu modda alınan eşyalar ve yapılan geliştirmeler kalır.

## Yeni kullanım

- `/market`: **SATIN AL** veya **ÜRÜN SAT** → ürün → biriken adet → toplam → açık onay. Adet seçmek ödeme yapmaz.
- `/sb jenerator`: adanın içinde görünür su/lav ve demir tuzak kapaklı küçük jeneratör; çekirdek bir sonraki tikte yenilenir. Başlangıçta demir, bakır ve lapis de çıkar. Mevcut oyuncu yapısı zorla silinmez; dönüşüm alanı doluysa açıklamadaki 6×3 alanı boşalt.
- `/warp sb tarla`: yalnız sana ait **80 ürün**, ilk başarılı girişten başlayarak **15 dakikada yenilenir**. Her oyuncunun kendi süresi vardır; çıkıp girmek yenilemez. Çiftçi Efe satış menüsünü açar.
- `/sb minyon kur tasci`: hazır jeneratör yanında kur; Taşçı çıkan gerçek taşı/cevheri işler. Ürünler yanındaki **kendi sandığına otomatik gider**. Sandığa sağ tık veya `/sb minyon sandik 1`. Eski depolar korunarak taşınır.
- `/sb minyon kur bakici`: Bakıcı sandığına **normal ekmek** koy; ihtiyaç olan üreticiye gerçek ekmek tüketerek enerji verir. Dolu depoda gereksiz yakıt tüketilmez.
- Oduncu önünde düz3×3alan ve5blok yükseklik bırak: fidan diker, kendi ağacını büyütüp keser; oyuncu yapısını kesmez.
- `/sb spawner`: canlı doğurmak yerine45saniyede kendi deposunda ürün biriktirir. Adada/çevrimiçi bulunman gerekmez; varsayılan6saat çevrimdışı hesap ve3456eşya sınırı. Sağ tıkla al/sat.
- `/sb piston`: özel Hasat Pistonu al; tarla kenarına baktığın yöne yerleştir. Önündeki3×5alanda gerçek olgun ürünleri toplar, ekinleri yeniden eker, kamış/kaktüsün dibini bırakır. Kendi3456eşyalık deposu vardır; ürün güvenli kayıttan sonra en fazla10saniyede görünür. Yüklü alan gerekir, çevrimdışı bitki büyümesi üretilmez.
- `/sb pazar` veya markette **Topluluk pazarı**: elindeki ürünü adet/toplam fiyat seçerek ilan et; İlanlarım'dan geri al.5ilan/24saat, %5vergi; kazanç kişisel Altın cüzdanına gelir.
- `/envanter Oyuncu`, `/chest Oyuncu [1-4]`: yetkili için salt okunur envanter/sandık görünümü. Normal oyuncular başkasına bakamaz. `/sb ada Oyuncu`: hedefin adasına yetkili ziyareti, üyeliği değiştirmez; `/sb ev` geri döner.
- Ağaç devrilmesi normal oyuncuda en alttaki kütükte %75 ihtimalle çalışır. OP yaratıcı modda deneyecekse `/sb test ac` kullanır. Eğilerek kırmak tek kütük alır.
- Parkur 1.2.1 rengi tanımlanmamış scoreboard takımının paneli bozmasını düzeltir; diğer takım seçenekleri korunur.
- `/sb rehber`, `/gorevler`: ilerleme adımları, ada kasası ödülleri ve minyon kurulumu. Ana menüde kısa, amacını anlatan başlıklar vardır.
- `/chest 1`…`/chest 4`: kişisel 54 yuvalı sayfalar. Normal/VIP/VIP+/VIP Ultra 1/2/3/4 sayfa; ayarı `chests.pages-by-tier` yönetir. OP hepsini açar.
- `/sb set kar|yagmur|gunes`: yalnız kendi adanın havasını ayarlar.
- `/evcil kapat`, `/evcil ac`: evcili gizle/göster; çarpışma koruması sürer.
- Normal Skyblock/VIP uçuş hız seçicisi kaldırıldı. Yalnız OP/yönetici için önceden istenen genel `/ucushizi 1–10` aracı korunur.
- Skyblock'ta ölümde eşya/XP korunur. Önceki sürümde kayıtsız yok olmuş eşya otomatik geri getirilemez.
- `/lobi` → Oyunlar/Skyblock: son güvenli kayıtlı konum ve yön geri gelir.
- `/rol ver Oyuncu oncu` veya `/beta ver Oyuncu`: bakımdaki çalışır oyunlara test girişi; yönetim yetkisi vermez.

Dünyalar, mevcut para ve görevler silinmez. `ada` / KasabaAdalar ayrı oyundur. Towny/Vault/Sikke/Chunky bu paketin gereksinimi değildir. Ana oyun menüsünde yalnız Skyblock üstte geniş giriş şerididir; diğer oyunlar iki sütunda kalır.

Donanım ve yazılım önerisi **SUNUCU-GEREKSINIMLERI.md**, ayrıntılı komutlar **SKYBLOCK-REHBER.md**, gerçek test kapsamı **TEST-KAPSAMI.md** ve **TEST-SONUCLARI.json** içindedir. Yerel işlev testleri canlı kapasite/yük ölçümü veya grafik istemci onayı yerine geçmez. Kendi kaynak kodlarımız `kaynak/KasabaTR-KAYNAK.zip` içindedir; özel test adaptörü ve test oyuncu verileri dağıtılmaz.

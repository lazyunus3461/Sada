# KasabaTR 1.2.1 • OP ada beklemesi ve oyun seçimi düzeltmesi

Mevcut 1.2.0 paketini kullanan sunucuda yalnız **iki eklenti** değişir. Java21/Paper1.21.10.

1. Sunucuyu tamamen durdur.
2. `plugins` içinden eski `KasabaHub-1.12.0.jar` ve `KasabaSkyblock-1.1.0.jar` dosyalarını kaldır. **Veri klasörlerine, skyblock.db dosyasına ve dünyalara dokunma.**
3. Yerlerine `KasabaHub-1.12.1.jar` ve `KasabaSkyblock-1.1.1.jar` koy. Aynı eklentinin iki sürümü birlikte kalmasın.
4. Başlat; Paper `Done` satırını bekle. `/reload` kullanma.
5. OP hesabınla `/sb gir`, ardından `/sb olustur` yaz. Önceden başlamış 24 saatlik bekleme OP'yi engellemez.

## İstenen bekleme kaldırma komutu

Oyunda: **`/sb OyuncuAdı 24saatkaldir`**

Konsolda: **`sb OyuncuAdı 24saatkaldir`**

Yalnız OP/yönetici veya konsol kullanır. Skyblock’a daha önce girmiş çevrimiçi ya da çevrimdışı oyuncunun yalnız ada kurma beklemesi silinir. Kendi adını da yazabilirsin. OP bunu Skyblock dışında da kullanabilir. Oyuncunun adası, üyeliği, eşyaları ve parası değişmez.

## OP olarak test

- OP veya mevcut `kasabaroles.admin` yetkili yönetici yeni ada kurarken 24 saat beklemez.
- Bu düzeltme **önceden kaydedilmiş beklemeyi de** otomatik aşar; veritabanından kayıt silmek gerekmez.
- Zaten bir adanın üyesiysen `/sb ev` ile gidersin. Yeni bir ada denemek için yalnız olduğun kendi adanda `/sb adabirak` onayını kullanıp ardından `/sb olustur` yazabilirsin. Yapılar arşivlenir; var olan adaya sessizce ikinci ada eklenmez.
- Takım üyeliği/sahiplik kontrolleri ve normal oyuncuların 24 saat beklemesi korunur. VIP tek başına yönetici sayılmaz.

## Oyunlar ekranı

- Adanı Yap, Arena, Parkur, Labirent ve diğer oyunların düğmeleri yeniden **iki sütundadır**; sayfa başına16 oyun gösterilir.
- **Skyblock en üstte ayrı, geniş ve tıklanabilir bir giriş şerididir.** İlk Yardım ayrı oyun seçeneği değildir; Skyblock içindeki rehberde kalır.
- Vanilla dialog ekranı bütün düğme ızgarası için tek sütun sayısı kullanır. Bu yüzden yalnız Skyblock üstte ayrı bir metin şeridi olarak gösterilir; alttaki düğmelerin genişliği değiştirilmez. Minecraft'ın gri düğmesiyle birebir aynı görünümde olduğu iddia edilmez.
- Geniş Skyblock girişi de diğer düğmeler gibi kimlik, parti, bakım ve geçerli sayfa kontrollerinden geçer. Eski sayfa tıklamaları ve tekrar gönderilen girişler etkisizdir.

## Tam paket

`KasabaTR-Skyblock-Parkur-Labirent-1.2.1.zip` altı eklentinin güncel halini içerir. Roles1.6.1, Evcil1.2.0, Parkur1.2.0 ve Labirent1.0.0 önceki paketle aynıdır. Skyblock1.1.1 ve Hub1.12.1 bu düzeltmeyi içerir. Önceki haritalar, canlı parkur ilk beşi, değişen labirent ve Skyblock özellikleri korunur.

Bu iki eklentinin birim testleri ve gerçek özel Paper/protokol kontrolleri `TEST-SONUCLARI.json` içindedir. Önceki sürümün raporları tarihsel doğrulama olarak ayrıca korunur; yeni sürümde bütün eski testlerin tekrarlandığı iddia edilmez. Canlı sunucuya erişilip kurulum yapılmadı.

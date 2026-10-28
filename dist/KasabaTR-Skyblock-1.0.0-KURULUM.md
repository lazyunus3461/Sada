# KasabaTR Skyblock 1.0.0 — Hazır kurulum

Mevcut KasabaTR ağı için **Paper 1.21.10 / Java 21** paketi. Mevcut KasabaAuth ve diğer oyun eklentilerin yerinde kalır.

1. Sunucuyu durdur ve dosyalarını yedekle.
2. `plugins` içindeki eski **KasabaHub** ve **KasabaRoles JAR** dosyalarını kaldır. ZIP'in `plugins` klasöründeki **KasabaHub-1.11.0.jar**, **KasabaRoles-1.6.0.jar** ve **KasabaSkyblock-1.0.0.jar** dosyalarını koy. Aynı eklentinin iki JAR'ını bırakma. **Veri klasörlerini silme.**
3. Sunucuyu başlat. İlk açılışta özgün gökyüzü kasabası ve boşluk dünyası otomatik hazırlanır. Konsolda “Skyblock hazır” ve sunucunun “Done” satırlarını bekle.
4. `/sb gir` ile merkeze gel; **Ada Yolcusu** veya `/sb olustur` ile ilk adanı oluştur. **Kasaba Rehberi** ve `/sb rehber` ilerlemeyi adım adım anlatır. Pusulada Skyblock ve İlk Yardım ilk sıradadır.

## Neler hazır?

- Tek `kasabatr_skyblock` dünyası; her özel ada **tam 100×100**, koruma bütün dünya yüksekliğinde (**Y=-64…319**). Eklenti ek dikey sınır koymaz; Minecraft'ın fiziksel yüksekliği sonsuz değildir.
- Kendi tasarımımız olan pazar meydanı, görev konağı, Mühür Hanı, atölyeler, saat kulesi, bahçeler, yel değirmeni ve balon iskelesi; **8 işlevli NPC**.
- Rehber kitabı, ada yardımcısı, 9 aşamalı ilerleme rehberi, **40 ilerleme görevi + 9 günlük/haftalık/aylık teslimat**.
- **586 ürün / 9 kategori** normal market, 5 seviye jeneratör, ortak kasa, 4 minyon türü, 12 spawner türü, oyuncu pazarı ve ada sıralaması.
- Altın, görevden kazanılan **Mühür**, ayrı destek bakiyesi **Kredi**. Özgün **Gezgin / Usta / Mimar** paketleri, Mühür Tüccarı, işlem geçmişi ve Kredi hediyesi.
- Kalıcı takım, 30 dakikalık ziyaret/kooperatif, gruplara ayrı **17 izin**, ayrı çanta/ender sandığı/XP kayıtları.

İlk ada ücretsiz, aynı anda bir kalıcı ada üyeliği, varsayılan sahip dahil 4 kişi. Kişiye özel ziyaret/koop izni kalıcı üyeliği değiştirmez. Diğer adalara yetkisiz giriş engellenir. Oyuncular varsayılan arkadaşları dahil gizlidir; kalıcı üyelerin birbirini görmesi için `plugins/KasabaSkyblock/config.yml` içinde `show-island-members: true` yapıp yeniden başlat.

## Kredi ve mağaza yönetimi

Oyuncunun önce Skyblock'a bir kez giriş yapması gerekir. Konsol veya yönetici:

```
skyblock yonet kredi Oyuncu 100.00 benzersiz-siparis-kodu
skyblock yonet muhur Oyuncu 50 benzersiz-odul-kodu
```

Aynı işlem kodu ve aynı içerik tekrar gönderilirse bakiye ikinci kez eklenmez; kod farklı içerikle tekrar kullanılamaz. Bir ödeme mağazan varsa başarılı siparişin konsol komutunu ilk satıra bağlayabilirsin. **Bu ZIP gerçek para tahsil eden bir web sitesi veya ödeme sağlayıcısı kurmaz**; oyun içi Kredi bakiyesi, mağaza ve sipariş işleme komutu hazırdır.

30 günlük paketler: **Gezgin 120 Kredi**, **Usta 220 Kredi**, **Mimar 360 Kredi**. Sırasıyla süreli VIP / VIP+ / VIP Ultra verir. Eski VIP süre dosyası korunur; üst kademe süreleri ayrı kaydedilir. Mevcut Akçe sistemi devam eder; bu bakiyeler birbirine çevrilmez. Ayrıntılı haklar ve Mühür ödülleri `OYUN-REHBERI.md` içindedir.

## Veriler

`ada` dünyası / **Adanı Yap** ayrı kalır. Towny, Sikke, Vault veya Chunky bu pakette yoktur. Mevcut world, oyuncu ve Hub/Role veri klasörlerini silme. Sunucu kapalıyken `kasabatr_skyblock`, `plugins/KasabaSkyblock` ve `plugins/KasabaRoles` klasörlerini birlikte yedekle. `/reload` yerine durdur/başlat kullan.

Ada sahibi yalnızsa `/sb adabirak` onayıyla adasını arşivleyip arkadaşına katılabilir; yeni ada kurma beklemesi 24 saattir. Yapılar arşivde kalır; ada kimliği başkasına verilmez. Alan büyütme ve otomatik yapı sıfırlama açık değildir.

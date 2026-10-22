# KasabaAkce 1.0.0

Paper 1.21.10, Java 21. Sunucu genelinde kalıcı **Akçe** bakiyesi. KasabaArena zorunlu değildir; kuruluysa X Arena marketi otomatik bağlanır.

[JAR indir](https://github.com/lazyunus3461/Sada/raw/refs/heads/main/dist/KasabaAkce-1.0.0.jar) · [Arena + Akçe paketi](https://github.com/lazyunus3461/Sada/raw/refs/heads/main/dist/KasabaArena-Akce-Kurulum.zip)

## Kurulum

Sunucuyu kapat. JAR'ı `plugins` içine koyup tamamen yeniden başlat. Başlangıç bakiyesi varsayılan olarak 0 Akçe'dir. `plugins/KasabaAkce/config.yml` içindeki `baslangic-akcesi` yalnızca ilk kez açılan hesaplara uygulanır. Dövüşte öldürme ödülü otomatik verilmez; yönetici veya başka bir oyun eklentisi ödül dağıtabilir.

## Komutlar

Türkçe `/akçe` ve ASCII `/akce` aynı komuttur. `/para` ve `/bakiye` de bakiyeni açar. Mesajlarda altın renkli `[Akçe]` etiketi, bakiye mesajında tıklanabilir gönderme/sıralama düğmeleri vardır.

| Komut | İşlem |
|---|---|
| `/akçe` | Kendi bakiyen |
| `/akçe gönder Redkit 100` | Redkit'e 100 Akçe gönder |
| `/akçe gönder Redkit 25,50` | 25,50 Akçe gönder; nokta da kabul edilir |
| `/akçe bak Redkit` | Oyuncunun bakiyesi |
| `/akçe sıralama` | En zengin 10 oyuncu |
| `/akçe yardım` | Komutlar |
| `/akçe ver Redkit 1000` | Yönetici: bakiyeye ekle |
| `/akçe al Redkit 100` | Yönetici: bakiyeden düş |
| `/akçe ayarla Redkit 500` | Yönetici: bakiyeyi tam 500 yap |

Hedef oyuncu sunucuya önceden girmişse çevrimdışıyken de para alabilir. Bilinmeyen isme hesap yaratılmaz. Kendine gönderme, negatif/sıfır transfer, ikiden fazla ondalık basamak, yetersiz bakiye ve üst limit aşımı reddedilir. Üst limit hesap başına 1.000.000.000.000 Akçe'dir. Transfer vergisi yoktur.

İzinler: `kasabaakce.kullan` ve `kasabaakce.gonder` varsayılan herkese; `kasabaakce.yonet` varsayılan OP. Komutlar konsoldan da yönetilebilir. Örneğin KasabaRoles ile `/rol yetki kurucu kasabaakce.yonet ver`.

## Market ve kalıcı kayıt

KasabaArena 1.2.0 ile satın alma onayında gerçek Akçe hesabından ürün fiyatı kesilir. Eşyalar yalnızca X Arena'da verilir; 1v1, 2v2 veya ana lobide kullanılmaz. Ürünleri Arena yönetim komutlarıyla ekle; örneğin eline kılıç alıp `/arena market ekle kilic 500 3 1`.

Hesaplar ve tamamlanmış market ödeme kimlikleri aynı atomik dosya işleminde `plugins/KasabaAkce/hesaplar.properties` içine kaydedilir. Transferin iki tarafı birlikte kaydedilir; yazma başarısızsa iki bakiye de değişmez. Sunucu yeniden açıldığında tamamlanmış ödeme tekrar kesilmez. Bozuk hesap dosyası boş hesaplarla değiştirilmez; eklenti açılmayı reddeder. Sunucu kapalıyken bu veri klasörünü yedekle. Dosyayı silmek para ve ödeme geçmişini siler.

Diğer eklentiler Bukkit ServicesManager üzerinden `AkceApi` servisini kullanabilir. `withdraw` aynı işlem kimliği, oyuncu ve miktar için kaydedilmiş sonucu verir; farklı bir ödeme için aynı kimlik kullanılamaz. API ile para çekmek oyuncu komutu yetkisine bağlı değildir; çağıran eklenti kendi izin denetimini yapmalıdır.

## Doğrulama

`mvn -f kasaba-akce/pom.xml verify` yedi kalıcı kayıt testini çalıştırır: çift taraflı transfer/yeniden açılma, kalıcı idempotent kesinti, yetersiz bakiye sonucu, geçersiz miktarlar, disk yazma hatasında geri alma, bozuk dosyayı koruma ve eşzamanlı çekimlerde eksi bakiyeyi önleme. Arena modülü 1.2.0 Maven deposuna önce kurulmalıdır. Gerçek Paper testi ve yeniden başlatma yönergeleri Arena test klasöründedir.

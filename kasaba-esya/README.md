# KasabaTR Eşya Saklambacı 1.3.0

Paper **1.21.10**, Java **21**, KasabaAuth **1.3.0** ve KasabaHub **1.6.1** için hazırlandı. KasabaSosyal varsa lobi arkadaş/grup/görünürlük araçları ve aktif oyun süresi mevcut sistemden çalışır. Multiverse-Core 5.3.2 ile ortak haritalar otomatik kaydedilir; Multiverse zorunlu değildir.

## Kurulum

1. Sunucuyu tamamen durdur. Mevcut `plugins` ve dünya klasörlerinin yedeğini al.
2. Hazır paketin `plugins` klasöründeki **KasabaHub-1.6.1.jar** ve **KasabaEsya-1.3.0.jar** dosyalarını sunucunun `plugins` klasörüne koy. Eski KasabaHub ve KasabaEsya JAR'larını kaldır; aynı eklentiden iki sürüm bırakma.
3. Sunucuyu başlat. Dünya yüklemek, `/mv import` veya `/oyunlar ekle` yazmak gerekmez. JAR, eksik olan üç dünyayı ilk açılışta kurar. Mevcut dünya ve ayar klasörlerinin üzerine yazmaz.
4. `/oyunlar` → **Eşya Saklambacı**. İlk gelişte yalnız lobi açılır. **Oyna** seçince sıraya girilir; **Bu lobide kal** sıradan çıkarır.

Mevcut Kasaba oyunlarını, para/rol kayıtlarını veya lobi/arena doğmalarını silme. `/reload` kullanma. Paket bir sunucu yaması ve yeni oyundur; kullanıcı yüklemeyi kendisi yapar.

## Oyun

- Normal maç **4–12 kişi**, başlangıç sayımı **15 saniye**. Sıraya girenlerin üstünde sayım görünür.
- Saklanma **20 saniye**; ardından ebeler **4 dakika** arar.
- 4–7 oyuncuda bir, 8–12 oyuncuda iki ilk ebe. Bulunan oyuncu ebeye dönüşüp kalanları arar.
- Oyuncakçı, Müze, Akvaryum sırayla karıştırılmış havuzdan gelir. Aynı harita havuz bitmeden tekrar seçilmez. Varsayılan en fazla **3 eşzamanlı maç** vardır; her maç ayrı dünya kopyasında çalışır.
- Haritaya uygun altı eşya kılığı seçilebilir. Kılık değiştirmenin bekleme süresi **20 saniye**. Gerçek oyuncu modeli diğer oyunculara gizlenir, hareket eden eşya görünür.
- Ebe şüpheli eşyaya sağ veya sol tıklar. Gerçek oyuncu yakalanır; yanlış tahminde **3 saniye yavaşlık**, tahminler arasında **1,2 saniye** bekleme vardır. Duvar arkasından veya erişim dışından yakalama kabul edilmez.
- 45 saniyede bir ebeye yaklaşık 8 blokluk bölge ipucu verilir.
- Herkes bulunursa maçta kalan **ilk ebeler**; süre biterse **hâlâ saklanan oyuncular** galibiyet alır. Sonradan yakalanıp ebeye dönüşenler bu sonuçtan ücretsiz galibiyet kazanmaz. Lobide ilk **5** galibiyet tablosu vardır.
- Sonuçtan **5 saniye** sonra oyuncular Eşya Saklambacı lobisine döner; kendiliğinden yeni sıraya girmez.
- Normal aktif maçı yarıda bırakan kişi bu oyuna **15 dakika** giremez. Bekleme lobisi, tamamlanan maç, yönetici denemesi ve zorunlu hapishane çıkışı bu cezayı üretmez.
- Grup lideri birlikte giren grubun tamamını sıraya alır/çıkarır; oda grubu ikiye bölmez.
- Envanter ve oyuncu durumu girişte yedeklenir; ana lobiye/başka oyuna geçince geri yüklenir. Bağlantı kopmasında yedek hesap doğrulandıktan sonra kurtarılır.

## Komutlar

| Komut | Ne yapar? |
|---|---|
| `/esya` | Eşya lobisine gelir veya pusula menüsünü açar. |
| `/esya oyna` | Grubun lideriyse grubunla, yalnızsan tek başına sıraya girer. |
| `/esya kılık` | Saklanırken eşya seçme sayfasını açar. |
| `/esya durum` | Sıra ve oda durumunu gösterir. |
| `/esya lobi` | Aynı oyunun lobisine döner; aktif normal maçta çıkış cezası uygulanır. |
| `/esya çık` veya `/lobi` | Ana spawna döner. |
| `/esya yardım` | Oyun kurallarını açar. |
| `/esya başlat` | Yönetici: sırada en az 4 kişiyle normal maçı hemen başlatır. |
| `/esya deneme` | Yönetici: sırada tam 2 kişiyle deneme maçı açar. Galibiyet yazmaz ve çıkış cezası üretmez. |
| `/esya testharita` | Yönetici/konsol: aktif oda yokken üç haritanın doğmalarını ve yem konumlarını gerçek kopyalarda kontrol eder. |
| `/esya düzenleme aç` | Yönetici: önce `/lobi` ile oyundan çıkıp `/mvtp <dünya>` ile gittiği ortak haritayı düzenleyebilir. |
| `/esya düzenleme kapat` | Düzenleme iznini kapatır. |
| `/oyunceza kaldır Oyuncu esyasaklambaci` | Yönetici: bu oyunun çıkış cezasını kaldırır. |

Türkçe karakterler veya `kilik`, `cik`, `baslat`, `duzenleme ac` yazımları kabul edilir. `/eşya` ve `/esyasaklambaci` kök komut takma adlarıdır. Yönetici izni `kasaba.esya.admin` (varsayılan OP); oynama izni `kasaba.esya.play` (varsayılan herkese açık).

### İki kişiyle deneme

İkiniz de `/esya` yazın, sonra ikiniz de `/esya oyna` yazın. Yönetici sıradayken `/esya deneme` yazsın. Bir ebe ve bir saklananla başlar. Bu, normal maçın dört kişilik sınırını değiştirmez.

## Dünya ve veri

Otomatik kurulan dünyalar:

- `KasabaTR-Esya-Saklambaci-Oyuncakci-v02`
- `KasabaTR-Esya-Saklambaci-Muze-v01`
- `KasabaTR-Esya-Saklambaci-Akvaryum-v01`

Oyuncakçı ortak lobidir. Müze ve Akvaryum da maç şablonlarıdır; buralara ayrı ayrı lobi kurmak gerekmez. Maç kopyaları `esya_room_...` adını alır ve Multiverse'e kaydedilmez. Yalnız bu eklentinin işaretlediği kullanılmayan geçici kopyalar silinir. Ortak dünyalar, kullanıcı yapıları ve başka eklentilerin dosyaları silinmez. Harita düzenlemeleri sonraki maç kopyalarına alınır; `prop-map.json` içindeki doğma ve yem konumlarını kapatmamak gerekir.

`plugins/KasabaEsya/config.yml` üç şablonun adını, `countdown-seconds: 15` ve `max-concurrent-rooms: 3` ayarlarını içerir. Eski 1.2.0 ayarı ve envanter kurtarma kayıtları korunur. Bozuk kayıt varsa üzeri yazılmaz ve yöneticiye günlükte hata gösterilir.

Kaynak yüklemelerinin belgeleri ve önizlemeleri `docs/` içindedir; eski kurulum metni 1.2.0 davranışını anlatır. Bu README 1.3.0'ın güncel kılavuzudur.

# Kasaba Hapishane 1.1.0

Paper 1.21.10 / Java 21, KasabaHub 1.3.1 ve KasabaAuth.
Gönderilen hapishane haritası JAR içindedir. İlk açılışta
`KasabaTR-Hapishane-Dunyasi-v1` dünyası otomatik kurulur; mevcut dünya korunur.

- `/hapis Redkit 60dk` — Redkit'i 60 dakika hapse at.
- `/hapis durum Redkit` — yetkili, kalan süreyi ve hücreyi görür.
- `/hapis bırak Redkit` — erken serbest bırak.
- `/hapis durum` — cezalı oyuncu kendi süresini görür.
- `/hapis setlobi` — ana lobide durup isteğe bağlı yedek dönüş noktası kaydet.

Normal dönüş KasabaHub'ın mevcut `/lobi` noktasıdır. Arena veya başka oyundan
hapse alınırken önce oyun üyeliği ve geçici eşyalar temizlenir, asıl oyuncu
eşyaları geri yüklenir. Cezalı oyuncu Adventure modunda hücrede kalır;
ışınlanma, oyun pusulası, eski menü düğmeleri, sohbet, mesajlaşma, komutlar,
blok düzenleme, eşya kullanımı ve envanter hareketi engellenir. Kayıt/giriş
komutları ile kendi cezasını sorgulama serbesttir.

Ceza gerçek zamanla işler; oyuncu çıkınca veya sunucu kapanınca sıfırlanmaz.
Girişte önce kimlik doğrulaması tamamlanır, sonra hücre/serbest kalma uygulanır.
Süre bitince oyuncu ana lobiye döner. Kayıt `plugins/KasabaHapis/sentences.yml`
içindedir; silme. 16 hücre en az dolu olandan dağıtılır. Varsayılan süre üst
sınırı 43.200 dakikadır.

Yetki `kasabatr.hapis.admin`. OP veya KasabaRoles Kurucu/Admin/Moderatör
kullanabilir. Rehber ve normal oyuncu kullanamaz. Önceden açıkça kapatılmış
bir rol izni güncellemede açılmaz. Yetkiyi elle vermek için:
`/rol yetki moderator kasabatr.hapis.admin ver`

Kurulum: Sunucuyu durdur, eski KasabaHapis JAR'ını kaldır, yenisini plugins'e
koy ve aç. KasabaHub 1.3.1 ve KasabaRoles 1.3.1 de paketle birlikte güncellenmeli.
Mevcut plugins veri klasörlerini veya dünyaları silme.

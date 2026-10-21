# Yalnızca yerel test

Önce `bash kasaba-clan/scripts/cloud-build.sh`, sonra `bash kasaba-clan/scripts/integration/build-probe.sh`. Mevcut loopback test sunucusu kapalıyken Roles 1.2.0, Warp 1.2.0, Klan 1.0.0 ve ClanProbe JAR'larını yerleştir; eski üretim JAR sürümlerini plugins dışına taşı. KasabaAuth 1.2.0 ve önceki loopback FlightProbe kalır. Test sunucusu yalnızca `127.0.0.1:25575`, offline ve allow-flight=false çalışmalıdır. Gerçek sunucuya yardımcı JAR kurma.

Bu fixture kurulu bir yerel Paper 1.21.10 build 130 sunucusu gerektirir; temiz bilgisayarda sunucuyu otomatik oluşturmaz. Klan/rol/warp veri klasörlerini önce yedekle. `bash scripts/start-test-server.sh` sonrasında **taze** Done ve üç yeni sürümün etkinleşme satırlarını bekle.

Repo kökünde `node kasaba-clan/scripts/integration/clan.cjs`. Gerçek kayıt/giriş, davet, sol/sağ tıklama, yabancı/member blok kırma, TAB, sohbet, kafa üstü takım paketleri kullanılır. ClanProbe ayrıca gerçek Bukkit olaylarını ana iş parçacığında göndererek blok/kova/sandık/mob, yüksek Y, mevcut PvP, sıvı/piston/hopper/patlama/yangın sınırlarını doğrular. Bu olay denetimleri fiziksel bütün otomasyon düzeneklerinin elle incelenmesi anlamına gelmez.

Test 200…209 X/Z alanını, 600/1000 çevresinde bir özel çoklu warpı ve menünün 2-3 kutusunu kullanır. Mevcut test verisinde bu alan ve sabit kutu boş olmalı. Test klanlarını/warpını tekrar kullanmadan önce yalnızca kendi yerel fixture kayıtlarını temizle; üretim verisini silme. İsimler rastgele son ek içerir. Normal komut paketleri vanilla spam sınırına uygun aralıkla gönderilir; toplu fixture komutları sınırlı yerel kanalla yürütülür.

Test bir aşamada sadece yerel klan dosyasını geçici bozup `/klan yenile` reddini ve çalışan korumanın sürmesini sınar; ardından özgün baytları geri yükler. Bu aşamada test yarıda kesilirse ClanProbe içindeki yedek sunucu kapanana kadar bellektedir (`RESTORE` işlemi). Başlamadan alınmış disk yedeğini koru.

Başarıda `target/integration-state.json` oluşur. Sunucuyu tamamen durdur/aç, taze Done'ı bekle; `KASABA_CLAN_PERSIST=1 node kasaba-clan/scripts/integration/clan.cjs` ile dünya, üyeler, alanlar, etiketler, yabancı koruması ve warp yerleştirme engelini diskten doğrula. Son olarak yalnızca sahip olduğun sunucuyu stop veya SIGTERM ile kapat.

Önceki role/chat/nametag testleri klansız yeni hesaplarla tekrar çalıştırılabilir. Eski Warp `icons-points.cjs` kalıcılık modu korunmuş durum dosyasıyla çalışır. Test kullanıcıları sentetiktir; gerçek hesap şifresi kullanılmaz. Üretim JAR'ında ClanProbe, test kanalı ve fixture sınıfları yoktur.

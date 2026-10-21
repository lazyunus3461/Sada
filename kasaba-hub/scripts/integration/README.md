# Loopback testi

`bash kasaba-hub/scripts/cloud-build.sh` ve `bash kasaba-hub/scripts/integration/build-probe.sh` çalıştır. Önceden hazırlanmış Paper 1.21.10 build 130 sunucusu yalnızca `127.0.0.1:25575`, offline ve allow-flight=false kullanmalıdır. Sunucu kapalıyken Auth 1.3.0, Hub 1.0.0, Roles 1.2.0, mevcut FlightProbe ve yeni HubProbe yerleştirilir. Önceki claim/warp/lobi yöneticileri ve onlara bağımlı test yardımcıları test yedeğine taşınır. Veri klasörleri korunur; üretime test ayarı/Probe kurulmaz.

Taze Done ve Auth/Hub etkinleşme kayıtlarını bekle. `node kasaba-hub/scripts/integration/hub.cjs` gerçek kayıt/giriş, native sayfa ve düğme paketleri, metin girişli davet, eski sayfa reddi, grup liderliği ve envanter korunmasını doğrular. HubProbe gerçek Bukkit olaylarıyla lobi/dış alan/hasar/açlık/düzenleme denetimi sağlar. Geçici test oyun adaptörü, ret/kabul ve kaldırma işlemlerini sınar; sonunda kaldırılır. Test 35.5,-60,35.5 lobi ve 500.5,-60,500.5 dış alanını kullanır. Teste başlamadan lobi verisini yedekle; bu bölgeler test içindir.

Durum `target/integration-state.json` içine yazılır. Sunucuyu tamamen kapat/aç ve taze Done sonrasında `KASABA_HUB_PERSIST=1 node kasaba-hub/scripts/integration/hub.cjs` çalıştır. Lobi/yön, eski test hesabı, geçici grubun temizlenmesi ve test adaptörünün üretim oyun listesinde kalmaması doğrulanır. Native istemcide görsel inceleme veya bütün fiziksel redstone düzenekleri denenmiş sayılmaz.

Fixture bir oyun uygulaması değildir. Üretim paketine yalnızca Auth ve Hub JAR'ları alınır. Sahip olduğun yerel sunucuyu stop veya SIGTERM ile kapat. Node bağımlılıkları repo `scripts/integration` klasöründeki kilitli npm bağımlılıklarıdır. Test şifreleri sentetiktir.

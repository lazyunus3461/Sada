# Loopback testi

`bash kasaba-hub/scripts/cloud-build.sh` ve `bash kasaba-hub/scripts/integration/build-probe.sh` çalıştır. Önceden hazırlanmış Paper 1.21.10 build 130 sunucusu yalnızca `127.0.0.1:25575`, offline ve allow-flight=false kullanmalıdır. Sunucu kapalıyken Auth 1.3.0, Hub 1.1.0, Roles 1.2.0, mevcut FlightProbe ve yeni HubProbe yerleştirilir. Önceki claim/warp/lobi yöneticileri ve onlara bağımlı test yardımcıları test yedeğine taşınır. Veri klasörleri korunur; üretime test ayarı/Probe kurulmaz.

Taze Done ve Auth/Hub etkinleşme kayıtlarını bekle. `node kasaba-hub/scripts/integration/hub.cjs` gerçek kayıt/giriş, native sayfa ve düğme paketleri, metin girişli davet, eski sayfa reddi, grup liderliği ve envanter korunmasını doğrular. HubProbe gerçek Bukkit olaylarıyla lobi/dış alan/hasar/açlık/düzenleme denetimi sağlar. Geçici test oyun adaptörü, ret/kabul ve kaldırma işlemlerini sınar; sonunda kaldırılır. Test 35.5,-60,35.5 lobi ve 500.5,-60,500.5 dış alanını kullanır. Teste başlamadan lobi verisini yedekle; bu bölgeler test içindir.

Durum `target/integration-state.json` içine yazılır. Sunucuyu tamamen kapat/aç ve taze Done sonrasında `KASABA_HUB_PERSIST=1 node kasaba-hub/scripts/integration/hub.cjs` çalıştır. Lobi/yön, eski test hesabı, geçici grubun temizlenmesi ve test adaptörünün üretim oyun listesinde kalmaması doğrulanır. Native istemcide görsel inceleme veya bütün fiziksel redstone düzenekleri denenmiş sayılmaz.

Fixture bir oyun uygulaması değildir. Üretim paketine yalnızca Auth ve Hub JAR'ları alınır. Sahip olduğun yerel sunucuyu stop veya SIGTERM ile kapat. Node bağımlılıkları repo `scripts/integration` klasöründeki kilitli npm bağımlılıklarıdır. Test şifreleri sentetiktir.

## 1.1.0 oyun girişleri ve lobi pusulası

Ana testteki lobi ayarlandıktan sonra `node kasaba-hub/scripts/integration/additions.cjs` çalıştırılır. Gerçek `use_item` paketiyle sağ tık ve yerleşik ana sayfa; lobi/dış alan pusulası; normal pusula ve elmasların korunması; 36 dolu yuvada üzerine yazmama ve boşalan yuvaya tek pusula verme; Bukkit olaylarıyla shift/numara/atma/el değiştirme engelleri denenir. Yönetim komutlarıyla giriş ekleme, aynı kimliği reddetme, üye/lider yetkileri, bütün grubun girişe taşınması, diğer eklentinin ikinci üyeyi engellemesinde ilk üyeyi geri alma, aktif girişi düzenleyememe, güncelleme/silme ve bozuk yenilemede çalışan listenin korunması sınanır. Beklenen bozuk YAML testinde bir hata kaydı üretilir; dosya `finally` ile eski haline alınır.

Tam durdur/aç sonrasında `KASABA_HUB_ADDITIONS_PERSIST=1 node kasaba-hub/scripts/integration/additions.cjs` girişin tam konum/adıyla kaldığını, komuttan gidince pusulanın kaldırıldığını, lobide geri geldiğini ve silmeyi doğrular. Test girişi bu aşamanın sonunda silinir. Ana `hub.cjs` testi yeni fixture ile oyun adaptörünün hiçbir geçici menü eşyası teslim almadığını da denetler. Üretim paketine fixture alınmaz.

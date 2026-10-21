# Yerel protokol testi

Bu test yalnızca `/workspace/kasaba-test-server` altındaki Paper 1.21.10 build 130 test sunucusuna bağlanır. `server-ip=127.0.0.1`, `server-port=25575`, `online-mode=false`, `allow-flight=false` gerekir. Gerçek sunucunun ayarlarını değiştirme. Bu test sunucusu önceki KasabaAuth çalışmasında hazırlanmıştır; temiz bir makinede kendiliğinden oluşmaz.

1. `bash /workspace/Sada/scripts/cloud-build.sh`
2. `bash /workspace/Sada/kasaba-roles/scripts/cloud-build.sh`
3. `bash /workspace/Sada/kasaba-roles/scripts/integration/build-probe.sh`
4. Sunucu kapalıyken KasabaAuth JAR'ını, `kasaba-roles/target/KasabaRoles-1.0.0.jar` ve `kasaba-roles/target/KasabaRolesProbe.jar` dosyalarını test sunucusunun `plugins` klasörüne kopyala.
5. `/workspace/Sada/scripts/integration` klasöründe `npm ci --cache /workspace/.npm --no-audit --no-fund` çalıştır.
6. Ayrı terminalde `bash /workspace/Sada/scripts/start-test-server.sh` çalıştır. Konsolda `Done` görülmesini bekle.
7. Repo kökünde `node kasaba-roles/scripts/integration/roles.cjs` çalıştır.
8. Çıktıdaki `PERSIST_USER` adını not et. Sunucuyu tamamen durdurup yeniden aç.
9. `KASABA_ROLES_PERSIST=1 KASABA_ROLES_USER=<not edilen ad> node kasaba-roles/scripts/integration/roles.cjs` çalıştır.

Protokol testi KasabaAuth formundan sentetik `Kasaba` şifresiyle giriş yapar. Gerçek hesap şifresi kullanmaz. Tekrar test ederken `deneme` rolünü önceki başarısız denemeden kaldıysa yalnızca test sunucusunda kaldır.

`KasabaRolesProbe`, yalnızca loopback adresine bağlı sunucuda yüklenir. Test istemcisinden rol komutlarını konsol olarak çalıştırır ve gerçek `Player.hasPermission` sonuçlarını kontrol eder. Dağıtılan KasabaRoles JAR'ında bu kod veya test kanalı bulunmaz. Yardımcı JAR gerçek sunucuya kurulmaz.

TAB'ın gerçek protokol paketinde etiket, renk ve sıra; yetkisiz rol verme, rol alma sonrası yetki temizliği, joker/tam ret önceliği, kayıtlı olmayan yetkiler, çevrimdışı atama, hatalı rol silme/verme, yenileme, yeniden bağlanma ve tam sunucu yeniden başlatma sınanır. Gerçek Minecraft istemcisinde görsel inceleme ve farklı eklentilerle uyumluluk ayrıca kontrol edilmelidir.

Ortak test istemcisindeki `minecraft-data` TAB paket tanımında ters yazılmış `UPDATE_LIST_ORDER` / `UPDATE_HAT` bitleri, gerçek 1.21.10 sunucusunun `ClientboundPlayerInfoUpdatePacket.Action` sırasına göre düzeltilir. Bu yalnızca test istemcisinin paket okumasını düzeltir; üretim eklentisi standart Paper API'sini kullanır.

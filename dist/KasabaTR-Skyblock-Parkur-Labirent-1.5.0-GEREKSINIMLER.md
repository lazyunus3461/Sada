# KasabaTR sunucusu için sistem önerisi

Bu öneri mevcut Skyblock, Parkur, Labirent ve Kasaba ağ eklentilerine göredir. Yerel işlev testleri kapasite/yük testi değildir; aşağıdaki oyuncu aralıkları garanti değil, kiralama için başlangıç tahminidir.

## Doğrulanmış yazılım gereksinimleri

- **Minecraft Java Edition istemcisi ve Paper 1.21.10; 64 bit Java 21.** Özel istemci veya kaynak paketi Skyblock için gerekmez. Paper'ın diyalog ve display API'leri kullanılır; Folia/Spigot kurulumu bu paketin test kapsamı değildir.
- Skyblock için **KasabaAuth 1.4.2, KasabaHub 1.12.2 ve KasabaRoles 1.7.0** kurulu olmalıdır. Yeni Skyblock JAR'ı 1.4.0'dır.
- Evcil mağazası ayrıca **KasabaAkce 1.1.1** ister. Mevcut Akçe ve Auth kurulumları korunur; altı JAR'lık güncelleme paketi bunların yerine geçmez.
- Dağıtımdaki diğer sürümler: **KasabaEvcil 1.2.1, KasabaParkur 1.2.1, KasabaLabirent 1.0.0**. Parkur ve Labirent, Auth ve Hub'a bağlıdır.
- **Multiverse-Core isteğe bağlıdır.** LuckPerms/TAB rol eklentisinin isteğe bağlı entegrasyonlarıdır; sırf bu paket için kurulmaları gerekmez. Towny, Vault, Sikke veya Chunky eklenmez.
- SQLite kullanılır; ayrıca MySQL/PostgreSQL sunucusu gerekmez. Dünya ve eklenti veri dosyaları aynı kalıcı diskte tutulur.
- Sesli oyunları da açacaksan gerçek **Simple Voice Chat** sunucu eklentisi ve oyuncularda uyumlu istemci modu gerekir. Bu Skyblock'un zorunlu bağımlılığı değildir. Minecraft için seçtiğin TCP portuna ek olarak ses eklentisinin ayarlı UDP portu açılır; yaygın varsayılanı 24454'tür.

## Donanım

En dengeli ilk seçim: **16 GB RAM, yüksek tek çekirdek performanslı 6 modern vCPU/çekirdek, 100 GB NVMe SSD ve Linux**. Çok sayıda zayıf/paylaşımlı vCPU yerine güçlü işlemci önemlidir; Ryzen 7600/7700/7900 sınıfı bir ana makine iyi bir örnektir. Hosting firmasının yalnız “6 çekirdek” yazması eşdeğer performans anlamına gelmez.

| Hedef kullanım | Sunucu RAM'i | Java üst sınırı | İşlemci / disk başlangıcı |
| --- | --- | --- | --- |
| 1–10 kişilik deneme/küçük açılış | 8–10 GB | 6 GB | 4 güçlü modern vCPU, en az 50 GB NVMe |
| Yaklaşık 10–30 aktif oyunculu ağ | 16 GB | 10–12 GB | 6 güçlü modern vCPU, 100 GB NVMe |
| Yaklaşık 30–60 aktif oyuncu, çok sayıda ada/minyon ve farklı oyun | 24–32 GB | 16–20 GB | 8 güçlü modern vCPU, 150–200 GB NVMe |

İşletim sistemi ve dosya önbelleği için RAM bırak; örneğin 16 GB makinada Java'ya 16 GB verme. Java başlangıç örneği:

```sh
java -Xms4G -Xmx12G -jar paper-1.21.10.jar nogui
```

Aynı makinede web sitesi, veritabanı veya başka oyun sunucuları çalışıyorsa onlar için ayrıca kaynak ayır. Paylaşımsız veya düşük paylaşımlı işlemci tercih et. Minecraft bağlantısı için kararlı düşük gecikmeli ağ ve en az 100 Mbit/s bağlantı önerilir; hostinglerde 1 Gbit/s hat olağandır. Bunlar ölçülmüş sabit oyuncu kapasitesi değildir.

## Bu pakette yükü etkileyen noktalar

Skyblock dünyası şu anda koddan **görüş mesafesi 12, simülasyon mesafesi 5 chunk** kullanır. Uzak adalar oyuncu başına farklı chunk kümeleri yükleyebilir. Buğday ve Yonca tarlaları tek ortak kasabadadır; oyuncu başına yeni tarla chunkları oluşturulmaz. Ortak mera 12 gerçek hayvanı yalnız yakın oyuncu varken tutar. Minyonlar, evciller, biyom değişimleri, ilk harita/ada oluşturma ve aynı anda açılan diğer oyunlar da etkiler. Çevrimdışı minyon/spawner üretimi geçmiş zamandan hesaplanır; bunlar için ada sürekli yüklü tutulmaz. Hasat pistonu yalnız yüklü gerçek bitkileri tarar; en fazla32makine/tur tarama ve bekleyen hasat sınırı vardır. Aktif hasat varken en fazla10saniyede bir Skyblock dünyasına disk kaydını bekleyen `save(true)` bariyeri uygulanır; bu, disk gecikmesi ve yüklü dünya büyüklüğüne bağlı tik sıçraması yaratabilir. Gerçek açılışta özellikle bu kayıt sürelerini ölç; NVMe ve boş disk alanı önemlidir.

Gerçek açılışta oyuncu sayısını kademeli artırıp Paper/spark ile **20 TPS ve oyun tiki başına 50 ms altı** hedefini izle. Bellek arttırmak tek çekirdek darboğazını düzeltmez. Büyük kapasite taahhüdünü gerçek oyuncu, ada ve minyon yüküyle ölçüm yaptıktan sonra ver.

Dünya ve SQLite kayıtları büyüdüğü için boş disk alanı bırak. Sunucu durdurulmuşken dünya + Skyblock/Roles/Evcil veri klasörlerini birlikte yedekle; yedekleri mümkünse ikinci bir diskte/depoda tut. Bu güncelleme mevcut dünyaları ve veri klasörlerini silerek kurulmaz.

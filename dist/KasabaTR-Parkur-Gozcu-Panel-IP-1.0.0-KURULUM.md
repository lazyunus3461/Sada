# KasabaTR güncellemesi — 27 Ekim 2026

Paper **1.21.10**, Java **21**. Canlı sunucuya yüklemeyi sen yapacaksın.

## Yükleme

1. Sunucuyu durdur ve mevcut `plugins/` klasörünü yedekle.
2. Yalnız eski `KasabaAuth`, `KasabaHub`, `KasabaBilgi` JAR sürümlerini kaldır.
   Bu eklentilerin veri klasörlerini, hesapları, spawn ve arena kayıtlarını silme.
3. ZIP içindeki `plugins/` klasörünün beş JAR dosyasını sunucunun `plugins/`
   klasörüne koy: Auth 1.4.1, Hub 1.9.3, Bilgi 1.0.4, Parkur 1.0.0, Gözcü 1.0.0.
4. Sunucuyu yeniden başlat; `/reload` kullanma. Pusulada Parkur görünür ve yeni
   dünya ilk kez otomatik kurulur. Eski dünyalara harita yazılmaz.

Sadece sağ paneli küçültmek için **KasabaBilgi-1.0.4.jar** yeterlidir.
Sadece IP hesabı yönetimini güncellemek için **KasabaAuth-1.4.1.jar** yeterlidir.
Parkur ve Gözcü için paketteki Hub sürümü de gerekir. Diğer oyun JAR'ları bu
pakette değiştirilmez. Arşivde deneme eklentileri veya test oyuncusu verileri yoktur.

## Küçük sağ panel

Beş kısa satır: oyuncu, rütbe, akçe, klan ve süre. IG/DC/adres satırları, uzun
etiketler ve boş satırlar kaldırıldı. Uzun değerler kısaltılır; akçenin kuruşu
korunur. Satır genişliği en fazla 72 yazı pikselidir; istemci arayüz ölçeği ayrıca
görünümü etkiler. Lobide renkler belirgin, oyunda daha soluktur. Oyunlara ait
ayrı skor tabloları ve gizli rol takımları korunur.

## IP onay komutları

Komutlar OP/Kurucu içindir. Kayıt sırasında kullanılan IP grubunu gösterir;
oyuncunun son bağlandığı farklı bir IP'yi esas almaz. Ham IP adresi gösterilmez.

- `/iponay Redkit`: Aynı kayıt IP'sindeki hesap isimleri, kayıtlı sayı, toplam
  izin ve boş hak. **Yalnız görüntüler; kendiliğinden hesap hakkı vermez.**
- `/iponay Redkit 3`: Bu IP'de **toplam üç** hesaba izin ver.
- `/iponay Redkit 4` veya `/iponay Redkit 5`: Toplam sınırı değiştir.
- `/iponay 3`: Son 10 dakika içinde senin seçtiğin oyuncunun IP sınırını üç yap.
  Önce oyuncu seçilmemişse yardım gösterir; rastgele bir IP'ye izin vermez.
- `/iponay3`, `/iponay4`, `/iponay5`: Seçili oyuncu için aynı kısa yollar.
  `/iponay3 Redkit` biçimi de kullanılabilir.
- `/iponay kaldır Redkit`: Toplam sınırı tekrar bire indir. Mevcut hesapları silmez.
- Sayıyla başlayan tamamen sayısal isim için `/iponay bilgi 123` kullan.

Sayı ek hak değildir: `3` = ana hesap dahil en çok 3 hesap. Tekrar yazmak hakkı
katlamaz. Sınır yalnız **yeni hesap kaydını** etkiler; mevcut hesapların girişini
ve aynı anda giriş yapmasını engellemez. Eski iki hesap onayları taşınır. Sayısal
sınırlar yeni `.ip-limits.properties` dosyasına atomik kaydedilir; parola
kayıtlarına dokunulmaz. Eski IP bilgisi olmayan hesap önce şifresiyle giriş yapar.

## Parkur ve yönetim

- `/parkur`: Parkur lobisi; kolay/orta/zor, her biri 100 bölüm.
- `/parkur kolay|orta|zor`: Kayıttan devam et. Maç veya kuyruk yoktur.
- `/parkur dön`: Son kontrol noktasına dön. `/parkur lobi`: Parkur lobisine dön.
- `/parkur ilerleme` veya `/parkur sıralama`: Üç rotanın toplamı ve ilk 5.
- `/lobi`: Ana lobi; parkurdan ayrılma cezası yoktur.
- `/parkur deneme zor 50`: Yönetici denemesi; rekor kazandırmaz.
- `/yonet` → **Bildirimler** → oyuncu → **Görünmez ol ve izle**.
- `/yonet izlemeyibit`: İzlemeyi bitir; eski eşya ve moduna dön.

Gözcü kesin hile kararı vermez; tekrar eden şüpheleri yalnız OP/Kurucuya bildirir.
Otomatik kick/ban uygulamaz. Özel mini oyun hareketlerinde hareket kontrolü
muafiyetleri vardır; saldırı denemeleri ayrıca ölçülür.

Test kapsamı ve sınırları `TEST-SONUCLARI.json` içindedir. Harita kontrolleri ve
protokol istemcileri 300 bölümün insan tarafından baştan sona bitirilmesinin
yerini tutmaz; parkur özel mekaniklerini oyun içinde de dene.

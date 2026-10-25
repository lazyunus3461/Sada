# KasabaTR Lanetli Taç — kurulum ve oyun akışı

## Dosyalar

- `KasabaTR-Lanetli-Tac-TAM-PAKET-v1.0.0.zip`: yüklemeye hazır şato dünyası, JAR ve bu belge.
- `KasabaTac-1.0.0.jar`: Paper eklentisi.
- `KasabaTR-Lanetli-Tac-Satosu-v01-DUNYA.zip`: ayrı boş dünya içinde şato ve oyun lobisi.
- `KasabaTR-Lanetli-Tac-Satosu-v01-Onizleme.png`: çatılı üst görünüm.
- `KasabaTR-Lanetli-Tac-Satosu-v01-Ic-Mekanlar.png`: mekânları göstermek için çatıları kaldırılmış plan görünümü; gerçek dünyada çatılar vardır.
- `KasabaTR-Lanetli-Tac-KAYNAK-v1.0.0.zip`: Java eklentisi ve dünya üretim kaynakları.

## Gereksinimler

- Paper **1.21.10**, Java **21**.
- Mevcut **KasabaAuth** ve **KasabaHub 1.2.0**. KasabaTac, KasabaHub'a kayıt olup `/oyunlar` menüsünde görünür. Var olan eklentilerin JAR'larını bu pakete kopyalamadım.
- Oyuncular için istemci modu veya kaynak paketi gerekmez.

## Yükleme

1. Sunucuyu tamamen durdur.
2. `KasabaTR-Lanetli-Tac-TAM-PAKET-v1.0.0.zip` içindeki `plugins/KasabaTac-1.0.0.jar` dosyasını sunucunun `plugins/` klasörüne koy.
3. ZIP içindeki `KasabaTR-Lanetli-Tac-Satosu-v01/` klasörünü **sunucu ana klasörüne** koy. `level.dat` dosyası bu klasörün hemen içinde olmalı; dünya klasörünü `plugins/` içine koyma.
4. Sunucuyu başlat. Konsolda `[KasabaTac] Lanetli Taç hazır; /oyunlar içinde lanetlitac açıldı.` satırını gör.
5. `/oyunlar` menüsünden **Lanetli Taç** oyununa gir. Oyun lobisi şato arenasından ayrı bir boş dünya platformundadır.

`/reload` yerine sunucuyu durdurup yeniden başlat. Dünya adı değiştirilirse `plugins/KasabaTac/config.yml` içindeki `world` değerini de değiştir.

## Oyun nasıl işler?

- **6–16 oyuncu** oyun lobisine girer. Altıncı oyuncu geldiğinde **20 saniyelik** başlama sayacı açılır.
- Her oyuncu farklı başlangıç noktasına yerleşir. Maç **240 saniye** sürer.
- Taç rastgele bir oyuncuda başlar. Tacı taşıyan oyuncu her saniye **1 puan** kazanır ve parlayan efekt ile altın miğferi sayesinde görülebilir.
- Başka bir oyuncu taç sahibine yaklaşarak tacı alır. Devirden sonra **2,5 saniye** koruma süresi vardır; bu, tacın iki oyuncu arasında anında gidip gelmesini engeller.
- Şatonun kuzey ve doğu kapıları **30 saniyede bir** düzen değiştirir. Yan geçitler daima açık kalır. Kapı, üstünde oyuncu varsa kapanmaz.
- Süre dolunca en yüksek puanlı oyuncu kazanır. İlk üç kişi gösterilir, oyuncuların eşyaları geri yüklenir ve KasabaHub lobisine dönerler. Kazananın galibiyeti ve en iyi puanı `plugins/KasabaTac/stats.yml` dosyasına kaydedilir.
- Oyun sırasında blok kırma/koyma, eşya atma ve diğer ışınlanma komutları engellenir. Oyuncu `/tac cik` ile kendi isteğiyle ayrılabilir. Sunucu kapanması/oyundan çıkış için envanter yedeği tutulur.

## Komutlar

| Komut | İşlev |
|---|---|
| `/oyunlar` | KasabaHub menüsünden Lanetli Taç'a katıl. |
| `/tac durum` | Bekleyen oyuncu ve maç durumunu gösterir. |
| `/tac cik` | Oyundan ayrılıp lobiye döner. |
| `/tac baslat` | En az 6 oyuncu varsa maçı erken başlatır; `kasabatac.admin` yetkisi gerekir. |

`plugins/KasabaTac/config.yml` ile oyuncu sayısı, maç süresi ve kapı süresi düzenlenebilir. Ödül olarak Akçe dağıtımı eklenmedi; mevcut Akçe ekonomisinin dengesi bilinmeden otomatik ödeme yapılmıyor.

## Yapılan doğrulama

- Oluşturulan dünya gerçek `level.dat` ve Anvil `.mca` bölge dosyaları içerir; void jeneratörü kullanır. Paper'ın normal çevre arazisi, şatonun etrafında oluşmaz.
- 16 başlangıç konumu, dört salon ve köşe yolları; kapıların açık, yalnız kuzey kapalı, yalnız doğu kapalı ve ikisi kapalı durumlarında yol bulma kontrolünden geçti.
- Eklenti Paper **1.21.10**, Java **21**, KasabaAuth **1.3.0** ve KasabaHub **1.2.0** bulunan yerel test sunucusunda etkinleşti; dünya açıldı ve `/tac durum` çalıştı.
- **Sınır:** Bu testte gerçek 6 oyuncuyla tam maç, taç devri ve bitiş akışı oynanmadı. Canlıya geçmeden önce 6 oyunculuk bir deneme maçı yapın.

package tr.kasabatr.vampir;

import java.util.List;

final class GameText {
    private GameText() {}
    static String purpose(GameRoom.Role role) {
        return switch(role){
            case VAMPIRE -> "Kimliğini gizle. Gündüz av süren doldukça yeniden avlan, gece listeden hedef seç. Sahte görevlerin üstteki ilerlemeyi artırır; köylüleri kazandırmaz.";
            case VILLAGER -> "Yerdeki mavi okları izleyip görevlerini tamamla. Gördüklerini toplantıda anlat ve vampiri bul.";
            case HUNTER -> "Köylü takımındasın. Yalnız bir okun var; ıskalama da hakkını tüketir. Hedefini dikkatli seç.";
            case SEER -> "Köylü takımındasın. Geceleri seçtiğin kişinin rolünü yalnız sen görürsün. Bilgini dikkatli kullan.";
            case DOCTOR -> "Köylü takımındasın. Her gece birini vampir saldırısından koru; kendini de seçebilirsin.";
        };
    }
    static List<String> rules() {
        return List.of(
            "Kasabanın içinde gizli vampirler var. Herkes rolünü yalnız kendi ekranındaki ROLÜN çubuğunda görür. Rolünü açıklayıp açıklamamak sana kalır.",
            "Köylü takımı: bütün vampirleri oylama/avcı ile ele veya ilk geceden sonra takım görev hedefini doldur. Vampirler: sayınızı kalan köylülerle eşitleyerek kasabayı ele geçirin.",
            "Gündüz: 7. kutudaki görev pusulasına veya deftere sağ tıkla, bir görev seç. Yerdeki mavi oklar sokak ve kapıları izleyerek seni götürür; okları yalnız sen görürsün. Kapalı kapıyı sağ tıklayıp aç. Paket aldıysan oklar teslim yerine döner. /vk görev 1–4 ile görev değiştir; /vk yol ile pusulayı al.",
            "Her oyuncuda 2 kolay, 2 orta görev var. Kolay işler: sıralı düzenek (en az 5 sn) ve malzeme ayırma (en az 7 sn). Orta işler: her turda 3 renkli, iki turlu hafıza oyunu ve hazırladıktan sonra iki durağa götürülen paket. Evdeki GÖREV NOKTASI bloğuna sağ tıkla; yönergeleri takip et. Uzaklaşırsan veya toplantı başlarsa o aşama yeniden hazırlanır. Üstteki ilerleme sahte işleri de içerir; birinin rolünü yalnız yüzdeye bakarak anlayamazsın. Köylüler gerçek görev hedefiyle kazanır; o zamana kadar gösterge en fazla %99 olur. İlk geceden sonra herkes ortak bakım da yapabilir.",
            "Ceset bulursan 4 blok yakınına gel ve bildirim çanına sağ tıkla. Meydandaki sabit çan acil toplantı açar. Gündüz bitince de herkes otomatik toplanır.",
            "Toplantı: üstte ŞU AN KONUŞUYOR yazan kişi ve kalan süre görünür. Sıran gelince gördüklerini anlat. /vk bitir kendi sıranı erken bitirir. Normal gündüz sohbeti 10 blok yakınındakilere gider. İsimler de yalnız yakında ve görüş hattında görünür.",
            "Neden oy veriyoruz? Şüpheli vampiri kasabadan elemek için. En çok oyu tek başına alan oyuncu elenir ve rolü açıklanır. Bir köylüyü elemek kendi takımını zayıflatır. Emin değilsen Pas seç; pas kazanırsa veya oylar eşitse kimse elenmez.",
            "Gece: vampir hedef, doktor korunacak kişi, kahin rolüne bakılacak kişi seçer. Seçimler gizlidir. Elenenler seyircidir; oy veremez ve yaşayanların sohbetine katılamaz. /lobi ile ana lobiye dönebilirsin.");
    }
    static List<String> voting(String reason) {
        return List.of("Toplantı nedeni: "+reason,
            "Amaç: vampir olduğundan şüphelendiğin kişiyi kasabadan elemek. Konuşmalara ve gördüklerine göre karar ver.",
            "En çok oyu tek başına alan oyuncu elenir; rolü herkese açıklanır. Yanlışlıkla bir köylüyü elemek takımını zayıflatır.",
            "Emin değilsen Pas. Pas kazanırsa veya eşitlik olursa kimse elenmez. Oy vermemek Pas oyu sayılmaz.");
    }
}

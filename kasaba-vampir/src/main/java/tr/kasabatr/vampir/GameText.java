package tr.kasabatr.vampir;

import java.util.List;

final class GameText {
    private GameText() {}
    static String purpose(GameRoom.Role role) {
        return switch(role){
            case VAMPIRE -> "Kimliğini gizle. Gündüz yakınındaki birini avla, gece listeden hedef seç. Görevlerin sahtedir.";
            case VILLAGER -> "Haritada sarı işaretli görevleri tamamla. Gördüklerini toplantıda anlat ve vampiri bul.";
            case HUNTER -> "Köylü takımındasın. Yalnız bir okun var; ıskalama da hakkını tüketir. Hedefini dikkatli seç.";
            case SEER -> "Köylü takımındasın. Geceleri seçtiğin kişinin rolünü yalnız sen görürsün. Bilgini dikkatli kullan.";
            case DOCTOR -> "Köylü takımındasın. Her gece birini vampir saldırısından koru; kendini de seçebilirsin.";
        };
    }
    static List<String> rules() {
        return List.of(
            "Kasabanın içinde gizli vampirler var. Herkes rolünü yalnız kendi ekranındaki ROLÜN çubuğunda görür. Rolünü açıklayıp açıklamamak sana kalır.",
            "Köylü takımı: bütün vampirleri oylama/avcı ile ele veya ilk geceden sonra takım görev hedefini doldur. Vampirler: sayınızı kalan köylülerle eşitleyerek kasabayı ele geçirin.",
            "Gündüz: 7. kutudaki haritayı eline al. Sarı 1–4 görevlerin, yeşil bitenler, mavi teslimat noktasıdır. Beyaz ok yalnız seni gösterir. Deftere veya haritaya sağ tıklayıp bir görev seç; alttaki yön ve mesafeyi izle.",
            "Görev yerindeki GÖREV NOKTASI yazısının altındaki bloğa sağ tıkla. Sayfada sırayı tamamla veya paketi belirtilen yere taşı. Vampirin sahte işleri gerçek takım yüzdesini artırmaz.",
            "Ceset bulursan 4 blok yakınına gel ve bildirim çanına sağ tıkla. Meydandaki sabit çan acil toplantı açar. Gündüz bitince de herkes otomatik toplanır.",
            "Toplantı: üstte ŞU AN KONUŞUYOR yazan kişi ve kalan süre görünür. Sıran gelince gördüklerini anlat. /vk bitir kendi sıranı erken bitirir. Normal gündüz sohbeti 10 blok yakınındakilere gider.",
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

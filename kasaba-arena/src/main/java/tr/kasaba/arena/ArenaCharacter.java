package tr.kasaba.arena;

import java.util.List;
import java.util.Locale;

/** Damage values are health points: two health points equal one heart. */
public enum ArenaCharacter {
    WARRIOR("Savaşçı", 1.5, 0.0, List.of(
        new Skill("Ağır Darbe", "Öndeki rakibe 1,8 kalp hasar", 10, 6),
        new Skill("Kasırga", "4 blok çevredeki rakiplere 1,2 kalp hasar", 12, 6),
        new Skill("Savaş Narası", "5 saniye boyunca %25 fazla hasar", 18, 6),
        new Skill("Deprem", "5 blok çevreye 2,1 kalp hasar ve yavaşlatma", 20, 6))),
    ASSASSIN("Suikastçı", 1.25, 0.0, List.of(
        new Skill("Gölge Atılımı", "İleri atıl; yakındaki rakibe 1,2 kalp hasar", 10, 6),
        new Skill("Zehirli Bıçak", "Üç saniyede toplam 1,5 kalp hasar", 14, 6),
        new Skill("Sis Perdesi", "3 saniye görünmezlik ve hız", 16, 6),
        new Skill("İnfaz", "1,8 kalp; canı az rakibe 2,4 kalp hasar", 20, 6))),
    TANK("Tank", 1.0, 0.15, List.of(
        new Skill("Çelik Kalkan", "3 saniye boyunca gelen hasarı yarıya indirir", 14, 6),
        new Skill("Kalkan Darbesi", "1,2 kalp hasar, geri itme ve yavaşlatma", 10, 6),
        new Skill("Demir İrade", "2 kalp iyileşme", 18, 6),
        new Skill("Sarsıntı", "5 blok çevreye 1,5 kalp hasar ve yavaşlatma", 22, 6))),
    ARCHER("Okçu", 0.75, 0.0, List.of(
        new Skill("Güçlü Ok", "İsabet eden ok 1,8 kalp hasar verir", 10, 6),
        new Skill("Ok Yağmuru", "Hedef alana üç dalgada toplam 1,8 kalp hasar", 14, 6),
        new Skill("Geri Sıçrayış", "Geriye sıçrama ve 2 saniye hız", 10, 6),
        new Skill("Avcı Oku", "İsabet eden ok 2,4 kalp hasar verir", 20, 6))),
    MAGE("Büyücü", 0.75, 0.0, List.of(
        new Skill("Alev Küresi", "Görüşündeki rakibe 1,35 kalp hasar", 10, 6),
        new Skill("Buz Çemberi", "5 blok çevreye 0,9 kalp hasar ve yavaşlatma", 12, 6),
        new Skill("Işın Adımı", "Önündeki güvenli noktaya en fazla 5 blok ışınlan", 16, 6),
        new Skill("Meteor", "Hedef alana kısa gecikmeyle 2,1 kalp hasar", 22, 6))),
    HEALER("Şifacı", 0.75, 0.0, List.of(
        new Skill("Işık Oku", "Görüşündeki rakibe 1,05 kalp hasar", 10, 6),
        new Skill("Şifa", "Kendini 2 kalp iyileştir", 16, 6),
        new Skill("Koruyucu Işık", "3 saniye boyunca gelen hasarı yarıya indirir", 18, 6),
        new Skill("Yaşam Dalgası", "1,5 kalp iyileşme; çevredeki rakiplere 1,2 kalp hasar", 24, 6)));

    public record Skill(String name, String description, int cooldownSeconds, int recoveryTicks) {}
    public final String label;
    public final double meleeDamage, passiveReduction;
    public final List<Skill> skills;
    ArenaCharacter(String label, double meleeDamage, double passiveReduction, List<Skill> skills) {
        this.label=label;this.meleeDamage=meleeDamage;this.passiveReduction=passiveReduction;this.skills=skills;
    }
    public static ArenaCharacter parse(String value) {
        String id=value.toLowerCase(Locale.ROOT).replace('ı','i').replace('ş','s').replace('ç','c').replace('ğ','g').replace('ü','u').replace('ö','o');
        return switch(id){case "savasci", "warrior"->WARRIOR;case "suikastci", "assassin"->ASSASSIN;case "tank"->TANK;case "okcu", "archer"->ARCHER;case "buyucu", "mage"->MAGE;case "sifaci", "healer"->HEALER;default->throw new IllegalArgumentException("Karakterler: savaşçı, suikastçı, tank, okçu, büyücü, şifacı.");};
    }
    public static double finalDamage(double base, ArenaCharacter victim, boolean warCry, boolean shield) {
        double value=base*0.60*(warCry?1.25:1.0)*(1-victim.passiveReduction)*(shield?0.5:1.0);
        return Math.max(0.0,Math.min(10.0,value));
    }
}

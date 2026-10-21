package tr.kasaba.arena;

import java.util.List;
import java.util.Locale;

/** Damage values are health points: two health points equal one heart. */
public enum ArenaCharacter {
    WARRIOR("Savaşçı", 1.5, 0.0, List.of(
        new Skill("Ağır Darbe", "Öndeki rakibe 3 kalp hasar", 10, 25),
        new Skill("Kasırga", "4 blok çevredeki rakiplere 2 kalp hasar", 10, 25),
        new Skill("Savaş Narası", "5 saniye boyunca %25 fazla hasar", 10, 60),
        new Skill("Deprem", "5 blok çevreye 3,5 kalp hasar ve yavaşlatma", 10, 30))),
    ASSASSIN("Suikastçı", 1.25, 0.0, List.of(
        new Skill("Gölge Atılımı", "İleri atıl; yakındaki rakibe 2 kalp hasar", 10, 30),
        new Skill("Zehirli Bıçak", "1 kalp ilk hasar ve 3 saniyede 1,5 kalp zehir", 10, 60),
        new Skill("Sis Perdesi", "3 saniye görünmezlik ve hız", 10, 60),
        new Skill("İnfaz", "3 kalp; canı 4 kalpten az rakibe 4 kalp hasar", 10, 30))),
    TANK("Tank", 1.0, 0.15, List.of(
        new Skill("Çelik Kalkan", "3 saniye boyunca gelen hasarı yarıya indirir", 10, 60),
        new Skill("Kalkan Darbesi", "2 kalp hasar, geri itme ve yavaşlatma", 10, 30),
        new Skill("Demir İrade", "3 kalp iyileşme", 10, 25),
        new Skill("Sarsıntı", "5 blok çevreye 2,5 kalp hasar ve yavaşlatma", 10, 50))),
    ARCHER("Okçu", 0.75, 0.0, List.of(
        new Skill("Güçlü Ok", "İsabet eden ok 3 kalp hasar verir", 10, 30),
        new Skill("Ok Yağmuru", "Hedef alana üç dalgada toplam 3 kalp hasar", 10, 30),
        new Skill("Geri Sıçrayış", "Geriye sıçrama ve 2 saniye hız", 10, 30),
        new Skill("Avcı Oku", "İsabet eden ok 4 kalp hasar verir", 10, 30)));

    public record Skill(String name, String description, int cooldownSeconds, int recoveryTicks) {}
    public final String label;
    public final double meleeDamage, passiveReduction;
    public final List<Skill> skills;
    ArenaCharacter(String label, double meleeDamage, double passiveReduction, List<Skill> skills) {
        this.label=label;this.meleeDamage=meleeDamage;this.passiveReduction=passiveReduction;this.skills=skills;
    }
    public static ArenaCharacter parse(String value) {
        String id=value.toLowerCase(Locale.ROOT).replace('ı','i').replace('ş','s').replace('ç','c').replace('ğ','g').replace('ü','u').replace('ö','o');
        return switch(id){case "savasci", "warrior"->WARRIOR;case "suikastci", "assassin"->ASSASSIN;case "tank"->TANK;case "okcu", "archer"->ARCHER;default->throw new IllegalArgumentException("Karakterler: savaşçı, suikastçı, tank, okçu.");};
    }
    public static double finalDamage(double base, ArenaCharacter victim, boolean warCry, boolean shield) {
        double value=base*(warCry?1.25:1.0)*(1-victim.passiveReduction)*(shield?0.5:1.0);
        return Math.max(0.0,Math.min(10.0,value));
    }
}

package tr.kasabatr.ispanya;

/** Rules reconstructed from the supplied 0.3.1 plugin; no Bukkit dependency. */
final class RaceRules {
    private RaceRules() {}
    static int destination(int current,int first,int second,int last,boolean exact){
        if(first<1||first>6||second<1||second>6)throw new IllegalArgumentException("Her zar 1–6 olmalı.");
        if(current<0||current>last)throw new IllegalArgumentException("Konum yol dışında.");
        int target=current+first+second;
        return exact&&target>last?current:Math.min(target,last);
    }
    static int effectDestination(int current,int change,int last){return Math.clamp((long)current+change,0,last);}
}

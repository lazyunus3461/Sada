package tr.kasaba.battle;

import java.util.*;

final class BattleRules {
    enum Mode {
        SOLO(1,"Solo"), IKILI(2,"İkili"), DORTLU(4,"Dörtlü");
        final int size; final String label;
        Mode(int size,String label){this.size=size;this.label=label;}
        static Mode parse(String text){return switch(normalize(text)){case "solo"->SOLO;case "ikili","duo"->IKILI;case "dortlu","squad"->DORTLU;default->throw new IllegalArgumentException("solo, ikili veya dörtlü seç.");};}
        String key(){return name().toLowerCase(Locale.ROOT);}
    }
    record Area(UUID world,int minX,int maxX,int minZ,int maxZ){
        Area{Objects.requireNonNull(world);if(minX>maxX||minZ>maxZ)throw new IllegalArgumentException("Bölge sınırları geçersiz.");}
        boolean contains(UUID w,double x,double z){return world.equals(w)&&x>=minX&&x<maxX+1.0&&z>=minZ&&z<maxZ+1.0;}
        boolean overlaps(Area a){return world.equals(a.world)&&minX<=a.maxX&&maxX>=a.minX&&minZ<=a.maxZ&&maxZ>=a.minZ;}
        double radius(double x,double z){return Math.max(0,Math.min(Math.min(x-minX,maxX+1.0-x),Math.min(z-minZ,maxZ+1.0-z))-2);}
    }
    /** Whole parties are packed together; a party is never silently split. */
    static List<List<UUID>> teams(Collection<List<UUID>> parties,int size){
        List<List<UUID>> result=new ArrayList<>();Set<UUID> seen=new HashSet<>();
        for(List<UUID> party:parties){if(party.isEmpty()||party.size()>size)throw new IllegalArgumentException("Grubun bu mod için fazla kalabalık.");for(UUID id:party)if(!seen.add(id))throw new IllegalArgumentException("Oyuncu iki takımda olamaz.");List<UUID> target=result.stream().filter(t->t.size()+party.size()<=size).findFirst().orElse(null);if(target==null){target=new ArrayList<>();result.add(target);}target.addAll(party);}
        return result.stream().map(List::copyOf).toList();
    }
    static double zone(double initial,double last,int elapsed,int delay,int duration){double end=Math.min(initial,Math.max(1,last));if(elapsed<=delay)return initial;return Math.max(end,initial-(initial-end)*Math.min(1,(elapsed-delay)/(double)Math.max(1,duration)));}
    static double healed(double current,double maximum,double hearts){return Math.min(maximum,current+Math.max(0,hearts)*2);}
    static int chestCount(int players,int minimum,double perPlayer,int multiplier){return Math.max(Math.max(1,minimum),(int)Math.ceil(players*Math.max(0,perPlayer)))*Math.clamp(multiplier,1,10);}
    static boolean outsideZone(double x,double z,double centerX,double centerZ,double radius){return Math.max(Math.abs(x-centerX),Math.abs(z-centerZ))>radius;}
    static List<Integer> chestSelection(int candidates,int wanted,Set<Integer> previous,Random random){return chestSelection(candidates,wanted,previous,Set.of(),random);}
    static List<Integer> chestSelection(int candidates,int wanted,Set<Integer> previous,Set<Integer> outdoors,Random random){
        if(candidates<4)throw new IllegalArgumentException("En az 4 güvenli sandık adayı kaydet.");
        int count=Math.max(1,Math.min(wanted,candidates/2));List<Integer> outside=new ArrayList<>(),inside=new ArrayList<>(),old=new ArrayList<>();
        for(int i=0;i<candidates;i++)if(previous.contains(i))old.add(i);else (outdoors.contains(i)?outside:inside).add(i);
        Collections.shuffle(outside,random);Collections.shuffle(inside,random);Collections.shuffle(old,random);
        int exterior=Math.min(outside.size(),(count+1)/2);List<Integer> chosen=new ArrayList<>(outside.subList(0,exterior));
        List<Integer> remaining=new ArrayList<>(inside);remaining.addAll(outside.subList(exterior,outside.size()));remaining.addAll(old);
        chosen.addAll(remaining.subList(0,Math.min(count-chosen.size(),remaining.size())));Collections.shuffle(chosen,random);return List.copyOf(chosen);
    }
    static String normalize(String s){return s.toLowerCase(Locale.ROOT).replace("\u0307", "").replace('ı','i').replace('ş','s').replace('ç','c').replace('ğ','g').replace('ü','u').replace('ö','o');}
}

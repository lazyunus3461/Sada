package tr.kasabatr.vampir;

import java.util.*;

/** Server-timed work shared by villagers and vampires. No role-specific shortcuts. */
final class TaskWork {
    static final int READY = 10, HINT = 11;
    enum Result { WAIT, WRONG, STEP, HIDDEN, ROUND, DONE, IGNORED }
    final int kind;
    final Random random;
    int[] sequence;
    int step, round;
    long startedAt = -1, nextInputAt;
    boolean preview = true, done;

    TaskWork(int kind, Random random) {
        if(kind<0||kind>3)throw new IllegalArgumentException("Görev türü geçersiz");
        this.kind=kind;this.random=random;sequence=pattern();
    }
    private int[] pattern() {
        if(kind==1){List<Integer> order=new ArrayList<>(List.of(0,1,2,3));Collections.shuffle(order,random);return order.stream().mapToInt(Integer::intValue).toArray();}
        if(kind==2)return new int[]{0,1,2};
        int count=kind==3?3:4,choices=kind==3?4:3;
        int[] values=new int[count];for(int i=0;i<count;i++)values[i]=random.nextInt(choices);return values;
    }
    void begin(long now){if(startedAt<0){startedAt=now;nextInputAt=now+700;}}
    void interrupt(){if(done)return;startedAt=-1;nextInputAt=0;step=0;preview=true;}
    long remaining(int action,long now){
        if(kind==3&&preview)return Math.max(0,startedAt+4000-now);
        long input=Math.max(0,nextInputAt-now);
        if(step==sequence.length-1)input=Math.max(input,startedAt+minimumMillis()-now);
        return Math.max(0,input);
    }
    long minimumMillis(){return switch(kind){case 0->5000;case 1->7000;case 2->4000;default->0;};}
    Result select(int action,long now){
        if(done||startedAt<0)return Result.IGNORED;
        if(kind==3&&action==HINT){step=0;preview=true;startedAt=now;nextInputAt=now+700;return Result.ROUND;}
        if(kind==3&&preview){
            if(action!=READY)return Result.IGNORED;
            if(remaining(action,now)>0)return Result.WAIT;
            preview=false;nextInputAt=now+700;return Result.HIDDEN;
        }
        if(action<0||action>=(kind==0?3:kind==2?3:4))return Result.IGNORED;
        if(now<nextInputAt)return Result.WAIT;
        if(action!=sequence[step]){step=0;preview=kind==3;startedAt=now;nextInputAt=now+700;return Result.WRONG;}
        if(step==sequence.length-1&&remaining(action,now)>0)return Result.WAIT;
        step++;nextInputAt=now+700;
        if(step<sequence.length)return Result.STEP;
        if(kind==3&&round==0){round=1;step=0;preview=true;startedAt=now;sequence=pattern();return Result.ROUND;}
        done=true;return Result.DONE;
    }
    String[] labels(){return switch(kind){case 0->new String[]{"Sol","Orta","Sağ"};case 1->new String[]{"Odun","Demir","Erzak","Kumaş"};case 2->new String[]{"Kutuyu Kontrol Et","Erzağı Yerleştir","Mührü Kapat"};default->new String[]{"Kırmızı","Mavi","Yeşil","Sarı"};};}
    String sequenceText(){String[] labels=labels();return Arrays.stream(sequence).mapToObj(i->labels[i]).reduce((a,b)->a+" → "+b).orElse("");}
    String title(){return switch(kind){case 0->"Düzeneği Ayarla";case 1->"Malzemeleri Sırala";case 2->"İki Duraklı Teslimat";default->"Hafıza Kontrolü";};}
    String difficulty(){return kind<2?"Kolay":"Orta";}
}

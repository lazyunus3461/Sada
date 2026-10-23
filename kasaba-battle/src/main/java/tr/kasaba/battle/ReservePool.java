package tr.kasaba.battle;

/** Two independent contributions: 2 normal + 3 strong hearts, never more than 5. */
final class ReservePool {
 double normal,strong;
 void reconcile(double actual){double loss=Math.max(0,normal+strong-Math.max(0,actual));double taken=Math.min(normal,loss);normal-=taken;strong=Math.max(0,strong-(loss-taken));}
 boolean canEat(int hearts,double actual,long lastDamage,long now){reconcile(actual);return now-lastDamage>=3000&&(hearts==2?normal<=.001:strong<=.001)&&actual<10;}
 double add(int hearts){if(hearts==2)normal=4;else if(hearts==3)strong=6;else throw new IllegalArgumentException("Kalp 2 veya 3 olmalı.");return Math.min(10,normal+strong);}
}

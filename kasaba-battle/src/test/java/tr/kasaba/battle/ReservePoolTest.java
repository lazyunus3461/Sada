package tr.kasaba.battle;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ReservePoolTest {
 @Test void normalAndStrongWorkInEitherOrderWithoutExceedingFiveHearts(){for(boolean normalFirst:new boolean[]{true,false}){ReservePool pool=new ReservePool();int first=normalFirst?2:3,second=normalFirst?3:2;double actual=pool.add(first);assertTrue(pool.canEat(second,actual,0,4000));actual=pool.add(second);assertEquals(10,actual);assertFalse(pool.canEat(first,actual,0,5000));}}
 @Test void aHitLocksBothFoodsForThreeSecondsAndExhaustedContributionCanBeReplaced(){ReservePool pool=new ReservePool();assertEquals(6,pool.add(3));assertFalse(pool.canEat(2,6,1000,3999));assertTrue(pool.canEat(2,6,1000,4000));assertEquals(10,pool.add(2));assertTrue(pool.canEat(2,6,1000,5000));assertFalse(pool.canEat(3,6,1000,5000));assertEquals(10,pool.add(2));assertTrue(pool.canEat(3,0,1000,5000));}
 @Test void chestSpacingUsesHorizontalDistanceEvenAcrossLevels(){var points=new java.util.ArrayList<BattleRules.Point>();for(int x=0;x<250;x+=2)for(int z=0;z<250;z+=2)points.add(new BattleRules.Point(x,z));var selected=BattleRules.spacedChests(points,240,java.util.Set.of(),java.util.Set.of(),12,new java.util.Random(19));assertEquals(240,selected.size());for(int a:selected)for(int b:selected)if(a!=b){var p=points.get(a);var q=points.get(b);assertTrue(Math.pow(p.x()-q.x(),2)+Math.pow(p.z()-q.z(),2)>=144);}}
}

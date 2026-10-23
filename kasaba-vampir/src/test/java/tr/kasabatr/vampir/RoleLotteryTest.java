package tr.kasabatr.vampir;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RoleLotteryTest {
 @Test void conflictingVipChoicesPreserveQuotasAndEveryVipHasAChance(){List<UUID> players=new ArrayList<>();for(int i=0;i<6;i++)players.add(UUID.randomUUID());Map<GameRoom.Role,Integer> quotas=Map.of(GameRoom.Role.VAMPIRE,1,GameRoom.Role.HUNTER,1,GameRoom.Role.SEER,1,GameRoom.Role.DOCTOR,1,GameRoom.Role.VILLAGER,2);Map<UUID,GameRoom.Role> requests=Map.of(players.get(0),GameRoom.Role.VAMPIRE,players.get(1),GameRoom.Role.VAMPIRE,players.get(2),GameRoom.Role.SEER);Set<UUID> winners=new HashSet<>();for(int seed=0;seed<100;seed++){var result=RoleLottery.assign(players,quotas,requests,new Random(seed*7919L));assertEquals(6,result.size());quotas.forEach((role,count)->assertEquals(count.intValue(),result.values().stream().filter(r->r==role).count()));assertEquals(GameRoom.Role.SEER,result.get(players.get(2)));UUID winner=result.entrySet().stream().filter(e->e.getValue()==GameRoom.Role.VAMPIRE).findFirst().orElseThrow().getKey();assertTrue(winner.equals(players.get(0))||winner.equals(players.get(1)));winners.add(winner);}assertEquals(2,winners.size());}
 @Test void disabledRoleIsNeverCreatedByAVipRequest(){List<UUID> players=List.of(UUID.randomUUID(),UUID.randomUUID());var result=RoleLottery.assign(players,Map.of(GameRoom.Role.VAMPIRE,1,GameRoom.Role.VILLAGER,1),Map.of(players.get(0),GameRoom.Role.DOCTOR),new Random(1));assertFalse(result.containsValue(GameRoom.Role.DOCTOR));}
}

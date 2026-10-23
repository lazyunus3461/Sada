package tr.kasabatr.vampir;

import java.util.*;

/** Preserves every role quota. Competing VIP requests are shuffled before allocation. */
final class RoleLottery {
 static Map<UUID,GameRoom.Role> assign(List<UUID> players,Map<GameRoom.Role,Integer> quotas,Map<UUID,GameRoom.Role> preferences,Random random){
  Map<UUID,GameRoom.Role> result=new HashMap<>();Map<GameRoom.Role,Integer> left=new EnumMap<>(GameRoom.Role.class);left.putAll(quotas);
  if(quotas.values().stream().mapToInt(Integer::intValue).sum()!=players.size())throw new IllegalArgumentException("Rol adedi oyuncu sayısıyla eşleşmeli.");
  for(GameRoom.Role role:GameRoom.Role.values()){List<UUID> wanting=new ArrayList<>(players.stream().filter(id->preferences.get(id)==role).toList());Collections.shuffle(wanting,random);int count=Math.min(left.getOrDefault(role,0),wanting.size());for(int i=0;i<count;i++)result.put(wanting.get(i),role);left.put(role,left.getOrDefault(role,0)-count);}
  List<UUID> remaining=new ArrayList<>(players.stream().filter(id->!result.containsKey(id)).toList());Collections.shuffle(remaining,random);int cursor=0;for(GameRoom.Role role:GameRoom.Role.values())for(int i=0;i<left.getOrDefault(role,0);i++)result.put(remaining.get(cursor++),role);return result;
 }
}

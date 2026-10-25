package tr.kasabatr.tac;
import java.util.*;
final class CrownRules {
    /** Whole groups only, oldest fitting group first. Odd player counts are valid in this free-for-all game. */
    static List<UUID> batch(List<List<UUID>> groups,int maximum){List<UUID> result=new ArrayList<>();Set<UUID> seen=new HashSet<>();for(var group:groups){if(group.isEmpty()||group.size()>maximum||new HashSet<>(group).size()!=group.size()||group.stream().anyMatch(seen::contains))throw new IllegalArgumentException("Geçersiz grup.");seen.addAll(group);if(result.size()+group.size()<=maximum)result.addAll(group);}return List.copyOf(result);}
    static Set<UUID> winners(Map<UUID,Integer> scores){int best=scores.values().stream().mapToInt(Integer::intValue).max().orElse(0);if(best<=0)return Set.of();Set<UUID> out=new LinkedHashSet<>();scores.forEach((id,score)->{if(score==best)out.add(id);});return Set.copyOf(out);}
}

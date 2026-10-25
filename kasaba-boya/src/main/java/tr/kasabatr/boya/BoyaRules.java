package tr.kasabatr.boya;
import java.util.*;
/** Select whole groups into an even-sized room, without blocking a later playable group. */
final class BoyaRules {
 static List<UUID> batch(List<List<UUID>> groups,int max){
  List<List<UUID>> choices=new ArrayList<>(Collections.nCopies(max+1,null));choices.set(0,List.of());Set<UUID> seen=new HashSet<>();
  for(List<UUID> group:groups){if(group.isEmpty()||group.size()>max)throw new IllegalArgumentException("Grup oda sınırını aşıyor.");for(UUID id:group)if(!seen.add(id))throw new IllegalArgumentException("Yinelenen oyuncu.");for(int n=max-group.size();n>=0;n--)if(choices.get(n)!=null&&choices.get(n+group.size())==null){List<UUID> value=new ArrayList<>(choices.get(n));value.addAll(group);choices.set(n+group.size(),List.copyOf(value));}}
  for(int n=max-max%2;n>=2;n-=2)if(choices.get(n)!=null)return choices.get(n);return List.of();
 }
}

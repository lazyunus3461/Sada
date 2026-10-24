package tr.kasabatr.esya;

import java.util.*;

/** Rules shared by admission, role assignment and durable result handling. */
final class EsyaRules {
    static int seekerCount(int players){if(players<2||players>12)throw new IllegalArgumentException("Oyuncu sayısı 2–12 olmalı.");return players>=8?2:1;}
    static List<UUID> batch(List<List<UUID>> groups,int maximum){
        List<UUID> selected=new ArrayList<>();Set<UUID> seen=new HashSet<>();
        for(List<UUID> group:groups){if(group.isEmpty()||group.size()>maximum)throw new IllegalArgumentException("Grup sınırı geçersiz.");if(selected.size()+group.size()>maximum)break;for(UUID id:group)if(!seen.add(id))throw new IllegalArgumentException("Oyuncu iki grupta olamaz.");selected.addAll(group);}
        return List.copyOf(selected);
    }
    static List<UUID> winners(boolean seekersWin,Set<UUID> firstSeekers,Set<UUID> hiders,Set<UUID> participants){return (seekersWin?firstSeekers:hiders).stream().filter(participants::contains).toList();}
}

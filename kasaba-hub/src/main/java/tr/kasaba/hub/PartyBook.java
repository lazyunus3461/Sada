package tr.kasaba.hub;

import java.util.*;
import java.util.function.LongSupplier;
import java.util.concurrent.TimeUnit;

/** Main-thread, transient online groups. Invitations are single-use and tied to their issuer. */
final class PartyBook {
    record Party(UUID id,UUID leader,List<UUID> members){Party{members=List.copyOf(members);}}
    private record Invite(UUID party,UUID issuer,long expires){}
    private final Map<UUID,Party> parties=new HashMap<>();
    private final Map<UUID,UUID> membership=new HashMap<>();
    private final Map<UUID,Map<UUID,Invite>> invitations=new HashMap<>();
    private final LongSupplier clock;
    PartyBook(){this(System::nanoTime);}
    PartyBook(LongSupplier clock){this.clock=clock;}
    Party party(UUID member){return parties.get(membership.get(member));}
    Party create(UUID player){if(party(player)!=null)throw new IllegalArgumentException("Zaten bir gruptasın.");Party p=new Party(UUID.randomUUID(),player,List.of(player));put(p);return p;}
    Party leader(UUID player){Party p=party(player);if(p==null)throw new IllegalArgumentException("Önce /grup oluştur kullan.");if(!p.leader().equals(player))throw new IllegalArgumentException("Bu işlem grup liderine ait.");return p;}
    Party invite(UUID issuer,UUID target,int capacity,int seconds){
        Party p=leader(issuer);if(party(target)!=null)throw new IllegalArgumentException("Oyuncu zaten bir grupta.");if(p.members().size()>=capacity)throw new IllegalArgumentException("Grup dolu: "+capacity);
        Map<UUID,Invite> incoming=invitations.computeIfAbsent(target,k->new HashMap<>());
        if(incoming.size()>=20&&!incoming.containsKey(p.id()))throw new IllegalArgumentException("Oyuncunun bekleyen davetleri dolu.");
        incoming.put(p.id(),new Invite(p.id(),issuer,clock.getAsLong()+TimeUnit.SECONDS.toNanos(seconds)));return p;
    }
    Party accept(UUID player,UUID leader,int capacity){
        if(party(player)!=null)throw new IllegalArgumentException("Zaten bir gruptasın.");Party p=leader(leader);Invite invite=invitations.getOrDefault(player,Map.of()).get(p.id());
        if(invite==null||clock.getAsLong()-invite.expires()>=0||!invite.issuer().equals(p.leader()))throw new IllegalArgumentException("Bu gruptan geçerli davetin yok.");
        if(p.members().size()>=capacity)throw new IllegalArgumentException("Grup dolu: "+capacity);
        List<UUID> next=new ArrayList<>(p.members());next.add(player);Party result=new Party(p.id(),p.leader(),next);put(result);invitations.remove(player);return result;
    }
    Party transfer(UUID leader,UUID target){Party p=leader(leader);if(!p.members().contains(target)||target.equals(leader))throw new IllegalArgumentException("Gruptaki başka bir üyeyi seç.");Party result=new Party(p.id(),target,p.members());put(result);return result;}
    Party kick(UUID leader,UUID target){Party p=leader(leader);if(leader.equals(target))throw new IllegalArgumentException("Kendini çıkarmak için /grup ayrıl kullan.");if(!p.members().contains(target))throw new IllegalArgumentException("Oyuncu grubunda değil.");return leave(target);}
    Party leave(UUID player){
        Party p=party(player);invitations.remove(player);if(p==null)return null;membership.remove(player);
        List<UUID> next=p.members().stream().filter(id->!id.equals(player)).toList();
        if(next.isEmpty()){parties.remove(p.id());return null;}
        Party result=new Party(p.id(),p.leader().equals(player)?next.getFirst():p.leader(),next);put(result);return result;
    }
    Party disband(UUID leader){Party p=leader(leader);parties.remove(p.id());p.members().forEach(membership::remove);return p;}
    private void put(Party p){parties.put(p.id(),p);p.members().forEach(id->membership.put(id,p.id()));}
    void prune(){long now=clock.getAsLong();invitations.values().forEach(map->map.values().removeIf(i->now-i.expires()>=0||!parties.containsKey(i.party())));invitations.values().removeIf(Map::isEmpty);}
}

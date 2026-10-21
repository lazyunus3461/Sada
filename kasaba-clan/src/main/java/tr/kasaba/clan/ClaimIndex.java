package tr.kasaba.clan;

import java.util.*;

final class ClaimIndex {
    record Cell(UUID world,int x,int z) {}
    record Owned(Clan clan,Claim claim) {}
    private final Map<Cell,List<Owned>> cells;
    ClaimIndex(ClanState state){
        Map<Cell,List<Owned>> next=new HashMap<>();
        for(Clan clan:state.clans().values())for(Claim claim:clan.claims()){
            Rect r=claim.bounds();Owned owned=new Owned(clan,claim);
            for(int x=r.minX()>>4;x<=r.maxX()>>4;x++)for(int z=r.minZ()>>4;z<=r.maxZ()>>4;z++)next.computeIfAbsent(new Cell(claim.world(),x,z),k->new ArrayList<>()).add(owned);
        }
        next.replaceAll((cell,owners)->List.copyOf(owners));cells=Map.copyOf(next);
    }
    Owned at(UUID world,int x,int z){return cells.getOrDefault(new Cell(world,x>>4,z>>4),List.of()).stream().filter(c->c.claim().bounds().contains(x,z)).findFirst().orElse(null);}
    Owned overlap(UUID world,Rect r){
        for(int x=r.minX()>>4;x<=r.maxX()>>4;x++)for(int z=r.minZ()>>4;z<=r.maxZ()>>4;z++)
            for(Owned owner:cells.getOrDefault(new Cell(world,x,z),List.of()))if(owner.claim().bounds().overlaps(r))return owner;
        return null;
    }
}

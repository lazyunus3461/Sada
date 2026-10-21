package tr.kasaba.clan;

import java.util.*;

record ClanState(UUID forest,String forestName,Map<UUID,Clan> clans) {
    ClanState {
        clans=Map.copyOf(clans);
        if(forest!=null&&(forestName==null||forestName.isBlank()))throw new IllegalArgumentException("Orman dünya adı eksik.");
        Set<String> tags=new HashSet<>(),members=new HashSet<>();
        List<Claim> claims=new ArrayList<>();
        for(var entry:clans.entrySet()){
            Clan clan=entry.getValue();if(!entry.getKey().equals(clan.id()))throw new IllegalArgumentException("Klan kimliği tutarsız.");
            if(!tags.add(Names.tagKey(clan.tag())))throw new IllegalArgumentException("Klan etiketi zaten kullanılıyor.");
            for(String member:clan.members().keySet())if(!members.add(member))throw new IllegalArgumentException("Bir oyuncu iki klana üye olamaz: "+member);
            for(Claim claim:clan.claims()){
                if(!claim.world().equals(forest))throw new IllegalArgumentException("Arazi orman dünyasında olmalı.");
                for(Claim existing:claims)if(existing.bounds().overlaps(claim.bounds()))throw new IllegalArgumentException("Araziler birbiriyle çakışıyor.");
                claims.add(claim);
            }
        }
    }
    static ClanState empty(){return new ClanState(null,null,Map.of());}
    Clan member(String player){String key=Names.key(player);return clans.values().stream().filter(c->c.members().containsKey(key)).findFirst().orElse(null);}
    Clan tag(String value){String key=Names.tagKey(value);return clans.values().stream().filter(c->Names.tagKey(c.tag()).equals(key)).findFirst().orElseThrow(()->new IllegalArgumentException("Klan bulunamadı: "+value));}
    ClanState put(Clan clan){Map<UUID,Clan> next=new HashMap<>(clans);next.put(clan.id(),clan);return new ClanState(forest,forestName,next);}
    ClanState remove(Clan clan){Map<UUID,Clan> next=new HashMap<>(clans);next.remove(clan.id());return new ClanState(forest,forestName,next);}
    ClanState forest(UUID world,String name){
        if(!Objects.equals(forest,world)&&clans.values().stream().anyMatch(c->!c.claims().isEmpty()))throw new IllegalArgumentException("Mevcut araziler varken orman dünyası değiştirilemez.");
        return new ClanState(world,name,clans);
    }
}

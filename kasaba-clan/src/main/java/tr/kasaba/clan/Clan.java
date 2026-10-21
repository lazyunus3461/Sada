package tr.kasaba.clan;

import java.util.*;

record Clan(UUID id,String name,String tag,String leader,Map<String,String> members,Set<String> assistants,List<Claim> claims) {
    Clan {
        Objects.requireNonNull(id);name=Names.label(name);tag=Names.tag(tag);leader=Names.key(leader);
        members=Map.copyOf(members);assistants=Set.copyOf(assistants);claims=List.copyOf(claims);
        for(var entry:members.entrySet())if(!Names.key(entry.getValue()).equals(entry.getKey()))throw new IllegalArgumentException("Üye adı/anahtarı tutarsız.");
        if(!members.containsKey(leader)||assistants.contains(leader)||!members.keySet().containsAll(assistants))throw new IllegalArgumentException("Klan lideri veya yardımcı üyeler geçersiz.");
        Set<Integer> numbers=new HashSet<>();for(Claim claim:claims)if(!numbers.add(claim.number()))throw new IllegalArgumentException("Tekrarlanan arazi numarası.");
    }
    static Clan create(String tag,String name,String player){return new Clan(UUID.randomUUID(),name,tag,Names.key(player),Map.of(Names.key(player),player),Set.of(),List.of());}
    boolean contains(String player){return members.containsKey(Names.key(player));}
    boolean manages(String player){String key=Names.key(player);return leader.equals(key)||assistants.contains(key);}
    long area(){return claims.stream().mapToLong(c->c.bounds().area()).sum();}
    Clan joined(String player,int limit){
        if(members.size()>=limit)throw new IllegalArgumentException("Klanın üye sınırı dolu: "+limit);
        Map<String,String> next=new HashMap<>(members);next.put(Names.key(player),player);return new Clan(id,name,tag,leader,next,assistants,claims);
    }
    Clan removed(String player){
        String key=Names.key(player);if(leader.equals(key))throw new IllegalArgumentException("Lider önce /klan lider <oyuncu> ile liderliği devretmeli veya /klan dağıt onayla kullanmalı.");
        Map<String,String> next=new HashMap<>(members);next.remove(key);Set<String> ranks=new HashSet<>(assistants);ranks.remove(key);return new Clan(id,name,tag,leader,next,ranks,claims);
    }
    Clan promoted(String player,boolean assistant){
        String key=Names.key(player);if(!members.containsKey(key)||leader.equals(key))throw new IllegalArgumentException("Kendi klanındaki lider dışı bir üyeyi seç.");
        Set<String> ranks=new HashSet<>(assistants);if(assistant)ranks.add(key);else ranks.remove(key);return new Clan(id,name,tag,leader,members,ranks,claims);
    }
    Clan transferred(String player){
        String key=Names.key(player);if(!members.containsKey(key)||leader.equals(key))throw new IllegalArgumentException("Kendi klanındaki başka bir üyeyi seç.");
        Set<String> ranks=new HashSet<>(assistants);ranks.remove(key);ranks.add(leader);return new Clan(id,name,tag,key,members,ranks,claims);
    }
    Clan claimed(Claim claim){List<Claim> next=new ArrayList<>(claims);next.add(claim);return new Clan(id,name,tag,leader,members,assistants,next);}
    Clan unclaimed(int number){
        if(claims.stream().noneMatch(c->c.number()==number))throw new IllegalArgumentException("Arazi bulunamadı: "+number);
        return new Clan(id,name,tag,leader,members,assistants,claims.stream().filter(c->c.number()!=number).toList());
    }
}

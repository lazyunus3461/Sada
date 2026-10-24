package tr.kasaba.hub;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Durable aggregate scores. A result is counted once even when a finish callback is repeated. */
final class LeaderboardStore {
    record Row(UUID id,String name,long score){}
    private final Path file;
    private final Map<String,Map<UUID,Row>> scores=new HashMap<>();
    private final Map<String,LinkedHashSet<UUID>> results=new HashMap<>();
    LeaderboardStore(Path file){this.file=file;}
    private void game(String game){if(!game.matches("[a-z0-9_-]{1,40}"))throw new IllegalArgumentException("Geçersiz oyun kimliği");}
    void load()throws IOException{
        if(!Files.exists(file))return;
        Properties p=new Properties();try(var in=Files.newInputStream(file)){p.load(in);}catch(IllegalArgumentException e){throw new IOException("Liderlik dosyası bozuk; korundu.",e);}
        Map<String,Map<UUID,Row>> next=new HashMap<>();Map<String,LinkedHashSet<UUID>> seen=new HashMap<>();
        try{for(String key:p.stringPropertyNames()){
            String[] parts=key.split("/");game(parts[0]);
            if(parts.length==2&&parts[1].equals("seen")){LinkedHashSet<UUID> ids=new LinkedHashSet<>();if(!p.getProperty(key).isBlank())for(String id:p.getProperty(key).split(","))ids.add(UUID.fromString(id));seen.put(parts[0],ids);}
            else if(parts.length==2){UUID id=UUID.fromString(parts[1]);String[] value=p.getProperty(key).split(":",2);long score=Long.parseLong(value[0]);if(score<0||value.length!=2||!value[1].matches("[A-Za-z0-9_]{1,16}"))throw new IllegalArgumentException();next.computeIfAbsent(parts[0],x->new HashMap<>()).put(id,new Row(id,value[1],score));}
            else throw new IllegalArgumentException();
        }}catch(RuntimeException e){throw new IOException("Liderlik dosyası bozuk; korundu.",e);}
        scores.clear();scores.putAll(next);results.clear();results.putAll(seen);
    }
    void total(String game,UUID id,String name,long value){game(game);if(value<0||!name.matches("[A-Za-z0-9_]{1,16}"))throw new IllegalArgumentException("Geçersiz skor");Map<UUID,Row> rows=scores.computeIfAbsent(game,x->new HashMap<>());Row old=rows.get(id);rows.put(id,new Row(id,name,Math.max(value,old==null?0:old.score)));}
    boolean result(String game,UUID match,Map<UUID,String> winners)throws IOException{
        game(game);LinkedHashSet<UUID> seen=results.computeIfAbsent(game,x->new LinkedHashSet<>());if(seen.contains(match)||winners.isEmpty())return false;
        for(var winner:winners.entrySet())if(!winner.getValue().matches("[A-Za-z0-9_]{1,16}"))throw new IllegalArgumentException("Geçersiz oyuncu adı");
        for(var winner:winners.entrySet()){Row old=scores.getOrDefault(game,Map.of()).get(winner.getKey());total(game,winner.getKey(),winner.getValue(),Math.addExact(old==null?0:old.score,1));}
        seen.add(match);while(seen.size()>512)seen.remove(seen.iterator().next());save();return true;
    }
    List<Row> top(String game){return scores.getOrDefault(game,Map.of()).values().stream().filter(r->r.score>0).sorted(Comparator.comparingLong(Row::score).reversed().thenComparing(Row::name,String.CASE_INSENSITIVE_ORDER).thenComparing(Row::id)).limit(5).toList();}
    void save()throws IOException{
        Properties p=new Properties();scores.forEach((game,rows)->rows.forEach((id,row)->p.setProperty(game+"/"+id,row.score+":"+row.name)));results.forEach((game,seen)->p.setProperty(game+"/seen",String.join(",",seen.stream().map(UUID::toString).toList())));
        Files.createDirectories(file.toAbsolutePath().getParent());Path tmp=Files.createTempFile(file.getParent(),"leaderboard-",".tmp");
        try{try(var out=Files.newOutputStream(tmp)){p.store(out,"Kasaba per-game leaderboards");}try{Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(tmp);}
    }
}

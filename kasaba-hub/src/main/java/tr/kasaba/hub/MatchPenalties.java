package tr.kasaba.hub;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Per-player/per-game re-entry bans; wall clock, atomic writes, no account balances. */
final class MatchPenalties {
 static final long DURATION=15*60_000L;
 private final Path file;private final Map<String,Long> until=new HashMap<>();
 MatchPenalties(Path file){this.file=file;}
 private String key(UUID player,String game){if(!game.matches("[a-z0-9_-]{1,40}"))throw new IllegalArgumentException("Geçersiz oyun kimliği");return player+"/"+game;}
 void load()throws IOException{if(!Files.exists(file))return;Properties props=new Properties();try(var in=Files.newInputStream(file)){props.load(in);}Map<String,Long> next=new HashMap<>();try{for(String key:props.stringPropertyNames()){String[] p=key.split("/",-1);if(p.length!=2)throw new IllegalArgumentException();key(UUID.fromString(p[0]),p[1]);long expiry=Long.parseLong(props.getProperty(key));if(expiry<0)throw new IllegalArgumentException();next.put(key,expiry);}}catch(RuntimeException e){throw new IOException("Maç cezası kaydı bozuk; korunuyor.",e);}until.clear();until.putAll(next);}
 long remaining(UUID id,String game,long now){return Math.max(0,until.getOrDefault(key(id,game),0L)-now);}
 boolean ban(UUID id,String game,long now)throws IOException{String key=key(id,game);if(remaining(id,game,now)>0)return false;until.put(key,Math.addExact(now,DURATION));save();return true;}
 Map<String,Long> active(UUID id,long now){Map<String,Long> result=new TreeMap<>();String prefix=id+"/";until.forEach((key,value)->{if(key.startsWith(prefix)&&value>now)result.put(key.substring(prefix.length()),value-now);});return result;}
 int clear(UUID id,String game)throws IOException{String prefix=id+"/";int before=until.size();if(game==null)until.keySet().removeIf(k->k.startsWith(prefix));else until.remove(key(id,game));save();return before-until.size();}
 void save()throws IOException{Properties p=new Properties();until.forEach((key,value)->p.setProperty(key,Long.toString(value)));Files.createDirectories(file.toAbsolutePath().getParent());Path temp=Files.createTempFile(file.getParent(),"penalty-",".tmp");try{try(var out=Files.newOutputStream(temp)){p.store(out,"Kasaba game re-entry penalties");}try{Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temp);}}
}

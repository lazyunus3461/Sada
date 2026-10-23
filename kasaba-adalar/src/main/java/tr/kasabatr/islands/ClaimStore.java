package tr.kasabatr.islands;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

/** Legacy island/name/member keys with durable pending payments and resets. */
final class ClaimStore {
 static final long INACTIVE=15L*86_400_000;
 private final Path file;private Properties data=new Properties();
 ClaimStore(JavaPlugin plugin){this(plugin.getDataFolder().toPath().resolve("claims.properties"),plugin.getLogger());}
 ClaimStore(Path file,Logger logger){this.file=file;}
 void load()throws IOException{if(!Files.exists(file))return;Properties next=new Properties();try(InputStream in=Files.newInputStream(file)){next.load(in);}validate(next);data=next;long now=System.currentTimeMillis();boolean changed=false;for(String key:new ArrayList<>(next.stringPropertyNames()))if(key.startsWith("island.")){String owner=next.getProperty(key);if(!next.containsKey("seen."+owner)){next.setProperty("seen."+owner,Long.toString(now));changed=true;}}if(changed){Path backup=file.resolveSibling(file.getFileName()+".before-vip-inactivity");if(!Files.exists(backup))Files.copy(file,backup);update(y->{});}}
 private void validate(Properties values)throws IOException{try{Map<UUID,Integer> counts=new HashMap<>();Set<Integer> pending=new HashSet<>();for(String key:values.stringPropertyNames())if(key.startsWith("island.")){int island=Integer.parseInt(key.substring(7));UUID owner=UUID.fromString(values.getProperty(key));if(island<1||island>IslandLayout.COUNT||counts.merge(owner,1,Integer::sum)>2)throw new IllegalArgumentException("En fazla iki ada kaydı");}else if(key.startsWith("member.")){String[] fields=key.split("\\.");if(fields.length!=3||!values.containsKey("island."+fields[1])||UUID.fromString(fields[2]).toString().equals(values.getProperty("island."+fields[1])))throw new IllegalArgumentException("Geçersiz arkadaş kaydı");}else if(key.startsWith("name.")){UUID.fromString(key.substring(5));if(!values.getProperty(key).matches("[A-Za-z0-9_]{1,16}"))throw new IllegalArgumentException("Oyuncu adı");}else if(key.startsWith("pending.")){UUID.fromString(key.substring(8));int island=Integer.parseInt(values.getProperty(key));if(island<1||island>IslandLayout.COUNT||values.containsKey("island."+island)||!pending.add(island))throw new IllegalArgumentException("Geçersiz bekleyen ada");}else if(key.startsWith("seen.")){UUID.fromString(key.substring(5));if(Long.parseLong(values.getProperty(key))<0)throw new IllegalArgumentException("Geçersiz giriş zamanı");}}catch(RuntimeException e){throw new IOException("Ada sahiplik dosyası bozuk; dosya korundu.",e);}}
 private void update(Consumer<Properties> action)throws IOException{Properties next=new Properties();next.putAll(data);action.accept(next);validate(next);Files.createDirectories(file.toAbsolutePath().getParent());Path temp=Files.createTempFile(file.toAbsolutePath().getParent(),"claims-",".tmp");try{try(OutputStream out=Files.newOutputStream(temp)){next.store(out,"Kasaba Adalar - sahiplik, bekleyen odeme ve sifirlama kayitlari");}Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);data=next;}finally{Files.deleteIfExists(temp);}}
 void remember(UUID id,String name)throws IOException{if(!name.matches("[A-Za-z0-9_]{1,16}")||name.equals(nameOf(id)))return;update(y->y.setProperty("name."+id,name));}
 UUID playerByName(String name){for(String key:data.stringPropertyNames())if(key.startsWith("name.")&&name.equalsIgnoreCase(data.getProperty(key)))return UUID.fromString(key.substring(5));return null;}
 String nameOf(UUID id){return data.getProperty("name."+id);}
 UUID ownerOf(int id){String value=data.getProperty("island."+id);return value==null?null:UUID.fromString(value);}
 List<Integer> owned(UUID id){List<Integer> result=new ArrayList<>();for(int island=1;island<=IslandLayout.COUNT;island++)if(id.equals(ownerOf(island)))result.add(island);return List.copyOf(result);}
 Integer islandOf(UUID id){List<Integer> all=owned(id);return all.isEmpty()?null:all.getFirst();}
 boolean isClaimed(int id){return ownerOf(id)!=null||resetting(id)||data.stringPropertyNames().stream().anyMatch(key->key.startsWith("pending.")&&data.getProperty(key).equals(Integer.toString(id)));}
 boolean canAccess(int island,UUID player){return !resetting(island)&&(player.equals(ownerOf(island))||Boolean.parseBoolean(data.getProperty("member."+island+"."+player)));}
 int size(){return (int)data.stringPropertyNames().stream().filter(k->k.startsWith("island.")).count();}
 int claim(UUID id)throws IOException{Integer old=islandOf(id);if(old!=null)return old;int reserved=prepare(id);return reserved==0?0:complete(id);}
 int prepare(UUID id)throws IOException{return prepare(id,"0.00");}
 int prepare(UUID id,String price)throws IOException{if(data.containsKey("pending."+id))return Integer.parseInt(data.getProperty("pending."+id));if(owned(id).size()>=2)return 0;int free=0;for(int island=1;island<=IslandLayout.COUNT;island++)if(!isClaimed(island)){free=island;break;}if(free==0)return 0;int chosen=free;String tx=UUID.randomUUID().toString();update(y->{y.setProperty("pending."+id,Integer.toString(chosen));y.setProperty("pending-tx."+id,tx);y.setProperty("pending-price."+id,price);});return chosen;}
 String price(UUID id){return data.getProperty("pending-price."+id,"0.00");}
 Map<UUID,Integer> pending(){Map<UUID,Integer> result=new HashMap<>();for(String key:data.stringPropertyNames())if(key.startsWith("pending."))result.put(UUID.fromString(key.substring(8)),Integer.parseInt(data.getProperty(key)));return result;}
 String transaction(UUID id){return data.getProperty("pending-tx."+id);}
 int complete(UUID id)throws IOException{int island=Integer.parseInt(data.getProperty("pending."+id));update(y->{y.setProperty("island."+island,id.toString());y.setProperty("seen."+id,Long.toString(System.currentTimeMillis()));y.remove("pending."+id);y.remove("pending-tx."+id);y.remove("pending-price."+id);});return island;}
 void cancel(UUID id)throws IOException{update(y->{y.remove("pending."+id);y.remove("pending-tx."+id);y.remove("pending-price."+id);});}
 boolean addMember(int island,UUID id)throws IOException{if(ownerOf(island)==null||id.equals(ownerOf(island))||data.containsKey("member."+island+"."+id))return false;update(y->y.setProperty("member."+island+"."+id,"true"));return true;}
 boolean removeMember(int island,UUID id)throws IOException{String key="member."+island+"."+id;if(!data.containsKey(key))return false;update(y->y.remove(key));return true;}
 void seen(UUID id,long now)throws IOException{if(islandOf(id)!=null)update(y->y.setProperty("seen."+id,Long.toString(now)));}
 long lastSeen(UUID id){return Long.parseLong(data.getProperty("seen."+id,"0"));}
 Set<UUID> owners(){Set<UUID> result=new HashSet<>();for(int i=1;i<=IslandLayout.COUNT;i++)if(ownerOf(i)!=null)result.add(ownerOf(i));return result;}
 boolean resetting(int island){return Boolean.parseBoolean(data.getProperty("reset."+island));}
 Set<Integer> resets(){Set<Integer> result=new TreeSet<>();for(int i=1;i<=IslandLayout.COUNT;i++)if(resetting(i))result.add(i);return result;}
 void beginReset(int island)throws IOException{update(y->y.setProperty("reset."+island,"true"));}
 void finishReset(int island)throws IOException{update(y->{y.remove("island."+island);y.remove("reset."+island);for(String key:new ArrayList<>(y.stringPropertyNames()))if(key.startsWith("member."+island+"."))y.remove(key);});}
}

package tr.kasabatr.islands;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

/** Compatible with 1.2.0 claims.properties. Exactly one owner record per player. */
final class ClaimStore {
    private final Path file;
    private final Map<UUID,Integer> byPlayer=new HashMap<>();
    private final Map<Integer,UUID> byIsland=new HashMap<>();
    private final Map<Integer,Set<UUID>> members=new HashMap<>();
    private final Map<UUID,String> names=new HashMap<>();
    ClaimStore(JavaPlugin plugin){this(plugin.getDataFolder().toPath().resolve("claims.properties"),plugin.getLogger());}
    ClaimStore(Path file,Logger logger){this.file=file;}
    void load()throws IOException{
        if(!Files.exists(file))return;Properties data=new Properties();try(InputStream stream=Files.newInputStream(file)){data.load(stream);}
        Map<Integer,UUID> islands=new TreeMap<>();Map<UUID,Integer> owners=new HashMap<>();Map<UUID,String> labels=new HashMap<>();Map<Integer,Set<UUID>> friends=new HashMap<>();
        try{
            for(String key:data.stringPropertyNames())if(key.startsWith("island.")){int id=Integer.parseInt(key.substring(7));UUID owner=UUID.fromString(data.getProperty(key));if(id<1||id>IslandLayout.COUNT||owners.putIfAbsent(owner,id)!=null)throw new IllegalArgumentException("Tek oyuncuda birden fazla ada veya geçersiz numara");islands.put(id,owner);}
            for(String key:data.stringPropertyNames())if(key.startsWith("name.")){UUID id=UUID.fromString(key.substring(5));String name=data.getProperty(key);if(!name.matches("[A-Za-z0-9_]{1,16}"))throw new IllegalArgumentException("Geçersiz oyuncu adı");labels.put(id,name);}
            for(String key:data.stringPropertyNames())if(key.startsWith("member.")){String[] fields=key.split("\\.");if(fields.length!=3)throw new IllegalArgumentException("Geçersiz arkadaş kaydı");int island=Integer.parseInt(fields[1]);UUID id=UUID.fromString(fields[2]);if(!islands.containsKey(island)||id.equals(islands.get(island)))throw new IllegalArgumentException("Arkadaşın adası bulunamadı");friends.computeIfAbsent(island,k->new HashSet<>()).add(id);}
        }catch(RuntimeException ex){throw new IOException("Ada sahiplik dosyası bozuk; mevcut dosya korundu.",ex);}
        byIsland.clear();byIsland.putAll(islands);byPlayer.clear();byPlayer.putAll(owners);names.clear();names.putAll(labels);members.clear();members.putAll(friends);
    }
    void remember(UUID id,String name)throws IOException{if(!name.matches("[A-Za-z0-9_]{1,16}")||name.equals(names.get(id)))return;String old=names.put(id,name);try{save();}catch(IOException ex){if(old==null)names.remove(id);else names.put(id,old);throw ex;}}
    UUID playerByName(String name){return names.entrySet().stream().filter(e->e.getValue().equalsIgnoreCase(name)).map(Map.Entry::getKey).findFirst().orElse(null);}
    String nameOf(UUID id){return names.get(id);}
    UUID ownerOf(int id){return byIsland.get(id);}
    Integer islandOf(UUID id){return byPlayer.get(id);}
    boolean isClaimed(int id){return byIsland.containsKey(id);}
    boolean canAccess(int island,UUID player){return player.equals(byIsland.get(island))||members.getOrDefault(island,Set.of()).contains(player);}
    int size(){return byIsland.size();}
    int claim(UUID player)throws IOException{Integer existing=byPlayer.get(player);if(existing!=null)return existing;int free=0;for(int id=1;id<=IslandLayout.COUNT;id++)if(!isClaimed(id)){free=id;break;}if(free==0)return 0;byPlayer.put(player,free);byIsland.put(free,player);try{save();}catch(IOException ex){byPlayer.remove(player);byIsland.remove(free);throw ex;}return free;}
    boolean addMember(int island,UUID player)throws IOException{if(!isClaimed(island)||player.equals(ownerOf(island)))return false;Set<UUID> set=members.computeIfAbsent(island,k->new HashSet<>());if(!set.add(player))return false;try{save();}catch(IOException ex){set.remove(player);throw ex;}return true;}
    boolean removeMember(int island,UUID player)throws IOException{Set<UUID> set=members.get(island);if(set==null||!set.remove(player))return false;try{save();}catch(IOException ex){set.add(player);throw ex;}return true;}
    private void save()throws IOException{Properties data=new Properties();byIsland.forEach((id,owner)->data.setProperty("island."+id,owner.toString()));names.forEach((id,name)->data.setProperty("name."+id,name));members.forEach((island,friends)->friends.forEach(id->data.setProperty("member."+island+"."+id,"true")));Files.createDirectories(file.getParent());Path temp=Files.createTempFile(file.getParent(),"claims-",".tmp");try{try(OutputStream stream=Files.newOutputStream(temp)){data.store(stream,"Kasaba island claims - one island per UUID");}Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(temp);}}
}

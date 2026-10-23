package tr.kasabatr.vampir;

import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;
import org.bukkit.persistence.PersistentDataType;

/** Private ground navigation, optional atlas and role bars. No other players' positions or roles. */
final class RoomGuide {
    private final GameRoom room;
    final GroundTrail trail;
    final MapView map;
    final List<TextDisplay> markers=new ArrayList<>();
    private final Map<Integer,List<TextDisplay>> signs=new HashMap<>();
    final Map<UUID,BossBar> roleBars=new HashMap<>();
    final Map<UUID,Integer> tracked=new HashMap<>();
    final Map<UUID,TextDisplay> playerLabels=new HashMap<>(),corpseLabels=new HashMap<>();
    private final Map<UUID,String> rendered=new HashMap<>();
    private final byte[] terrain=new byte[128*128];
    private final AtlasRenderer renderer=new AtlasRenderer();
    private boolean closed;
    private static final byte GOLD=MapPalette.matchColor(255,208,55),GREEN=MapPalette.matchColor(70,220,80),CYAN=MapPalette.matchColor(50,220,245),INK=MapPalette.matchColor(28,25,35),PAPER=MapPalette.matchColor(242,226,178),SHINE=MapPalette.matchColor(255,255,255);

    RoomGuide(GameRoom room){
        this.room=room;trail=new GroundTrail(room.world);map=Bukkit.createMap(room.world);map.getRenderers().forEach(map::removeRenderer);map.setCenterX(MapLayout.originX(room.world));map.setCenterZ(0);map.setScale(MapView.Scale.CLOSEST);map.setTrackingPosition(false);map.setUnlimitedTracking(false);map.setLocked(true);
        Map<Integer,Byte> colors=new HashMap<>();
        for(int z=0;z<128;z++)for(int x=0;x<128;x++){
            int wx=TaskNavigation.world(x),wz=TaskNavigation.world(z);byte color=INK;
            if(wx>=-68&&wx<=68&&wz>=-68&&wz<=68){var block=room.world.getHighestBlockAt(MapLayout.originX(room.world)+wx,wz,HeightMap.MOTION_BLOCKING_NO_LEAVES);var c=block.getBlockData().getMapColor();color=colors.computeIfAbsent(c.asRGB(),key->MapPalette.matchColor(c.getRed(),c.getGreen(),c.getBlue()));}
            terrain[z*128+x]=color;
        }
        map.addRenderer(renderer);
        try{for(var station:MapLayout.STATIONS)markers.add(stationMarker(station));refreshSigns();}
        catch(RuntimeException e){close();throw e;}
    }
    private TextDisplay stationMarker(MapLayout.Station station){return room.world.spawn(station.location(room.world).add(0,2.35,0),TextDisplay.class,d->{d.text(Component.text("✦ GÖREV NOKTASI\n",NamedTextColor.YELLOW).append(Component.text(station.name()+"\n",NamedTextColor.GOLD)).append(Component.text("Alttaki bloğa sağ tıkla",NamedTextColor.GRAY)));d.setBillboard(Display.Billboard.CENTER);d.setBrightness(new Display.Brightness(15,15));d.setShadowed(true);d.setSeeThrough(false);d.setViewRange(16f/64f);d.setPersistent(false);});}
    private void refreshSigns(){for(int i=0;i<MapLayout.STATIONS.size();i++){var station=MapLayout.STATIONS.get(i);if(!room.world.isChunkLoaded((MapLayout.originX(room.world)+station.x())>>4,station.z()>>4))continue;if(!markers.get(i).isValid())markers.set(i,stationMarker(station));List<TextDisplay> previous=signs.get(i);if(previous!=null&&previous.stream().allMatch(Entity::isValid))continue;if(previous!=null)previous.forEach(Entity::remove);List<TextDisplay> entries=new ArrayList<>();for(int side:new int[]{-1,1})entries.add(room.world.spawn(new Location(room.world,MapLayout.originX(room.world)+station.x()-2,71.3,station.z()+.5+side*6.6),TextDisplay.class,d->{d.text(Component.text(station.zone(),NamedTextColor.GOLD));d.setBillboard(Display.Billboard.CENTER);d.setBrightness(new Display.Brightness(15,15));d.setShadowed(true);d.setSeeThrough(false);d.setViewRange(14f/64f);d.setPersistent(false);}));signs.put(i,entries);}}
    void attach(Player p){
        var role=room.roles.get(p.getUniqueId());BarColor color=switch(role){case VAMPIRE->BarColor.RED;case DOCTOR->BarColor.BLUE;case SEER->BarColor.PURPLE;case HUNTER->BarColor.YELLOW;default->BarColor.GREEN;};
        BossBar bar=Bukkit.createBossBar(roleTitle(p),color,BarStyle.SOLID);bar.addPlayer(p);roleBars.put(p.getUniqueId(),bar);
    }
    private String roleTitle(Player p){var role=room.roles.get(p.getUniqueId());String objective=!room.alive.contains(p.getUniqueId())?"Elendin • Seyirci":role==GameRoom.Role.VAMPIRE?"Kimliğini gizle, kasabayı ele geçir":"Görevleri tamamla, vampiri bul";if(role==GameRoom.Role.VAMPIRE&&room.alive.contains(p.getUniqueId())&&room.phase==GameRoom.Phase.DAY){long remaining=(room.dayKillRemainingMillis(p.getUniqueId())+999)/1000;objective=(remaining==0?"§aAv hazır":"§eAv: "+remaining+" sn")+" §7• Kimliğini gizle";}String roleColor=switch(role){case VAMPIRE->"§c";case DOCTOR->"§b";case SEER->"§d";case HUNTER->"§6";default->"§a";};return "§7ROLÜN: "+roleColor+"§l"+role.label.toUpperCase(Locale.forLanguageTag("tr-TR"))+" §r§8• §7"+objective;}
    ItemStack item(){ItemStack item=new ItemStack(Material.FILLED_MAP);MapMeta meta=(MapMeta)item.getItemMeta();meta.setMapView(map);meta.setDisplayName("§eGörev Haritası §7• Sağ Tıkla");meta.setLore(List.of("§eParlayan sarı: görev §aYeşil: bitti §bMavi: teslimat","§fBeyaz ok sensin; görev seçmek için sağ tıkla."));meta.getPersistentDataContainer().set(new NamespacedKey(room.plugin,"menu_item"),PersistentDataType.STRING,"map");item.setItemMeta(meta);return item;}
    ItemStack navigationItem(){ItemStack item=new ItemStack(Material.COMPASS);var meta=item.getItemMeta();meta.setDisplayName("§bGörev Yolu §7• Sağ Tıkla");meta.setLore(List.of("§fGörevini seç; yerdeki mavi okları izle.","§7Okları yalnız sen görürsün; sokak ve kapılardan gider."));meta.getPersistentDataContainer().set(new NamespacedKey(room.plugin,"menu_item"),PersistentDataType.STRING,"tasks");item.setItemMeta(meta);return item;}
    void takeMap(Player p){if(closed||!room.members.contains(p.getUniqueId())||p.getWorld()!=room.world)throw new IllegalArgumentException("Harita maç başladıktan sonra kullanılabilir.");p.getInventory().setItem(6,item());p.getInventory().setHeldItemSlot(6);p.sendMap(map);}
    List<GameRoom.Task> tasks(Player p){return room.tasks.getOrDefault(p.getUniqueId(),List.of());}
    int target(Player p){
        var tasks=tasks(p);Integer chosen=tracked.get(p.getUniqueId());if(chosen!=null&&chosen>=0&&chosen<tasks.size()&&!tasks.get(chosen).completed)return TaskNavigation.station(tasks.get(chosen));
        int nearest=-1;double best=Double.MAX_VALUE;for(int i=0;i<tasks.size();i++){var task=tasks.get(i);if(task.completed)continue;int s=TaskNavigation.station(task),walking=trail.distance(p,s);double distance=walking>=0?walking:10000+Math.sqrt(distanceSquared(p,s));if(distance<best){best=distance;nearest=s;tracked.put(p.getUniqueId(),i);}}
        if(nearest<0&&room.firstNightDone&&room.taskDone<room.taskGoal)for(int i=0;i<MapLayout.STATIONS.size();i++){double distance=distanceSquared(p,i);if(distance<best){best=distance;nearest=i;}}
        return nearest;
    }
    void select(Player p,int number){if(number<1||number>tasks(p).size())throw new IllegalArgumentException("Görev numarası 1–4 olmalı.");var task=tasks(p).get(number-1);if(task.completed)throw new IllegalArgumentException("Bu görevi zaten tamamladın.");tracked.put(p.getUniqueId(),number-1);showTrail(p);}
    void showTrail(Player p){if(closed||!room.members.contains(p.getUniqueId())||p.getWorld()!=room.world||!room.alive.contains(p.getUniqueId()))throw new IllegalArgumentException("Görev yolu maçta yaşayan oyuncular içindir.");p.getInventory().setItem(6,navigationItem());p.getInventory().setHeldItemSlot(6);int target=target(p);room.plugin.tell(p,target<0?"Görevlerin tamam.":"Hedef: "+MapLayout.STATIONS.get(target).name()+" • Yerdeki mavi okları izle. Okları yalnız sen görürsün.");if(room.phase==GameRoom.Phase.DAY&&target>=0)trail.render(p,target);}
    void trails(){if(closed||room.phase!=GameRoom.Phase.DAY)return;for(UUID id:List.copyOf(room.alive)){Player p=Bukkit.getPlayer(id);if(p==null||p.getWorld()!=room.world||!room.members.contains(id))continue;int target=target(p);if(target>=0)trail.render(p,target);}}
    int distance(Player p,int station){return (int)Math.ceil(Math.sqrt(distanceSquared(p,station)));}
    private double distanceSquared(Player p,int station){var s=MapLayout.STATIONS.get(station);return Math.pow(p.getX()-MapLayout.originX(room.world)-s.x()-.5,2)+Math.pow(p.getZ()-s.z()-.5,2);}
    void tick(){
        if(closed)return;for(UUID id:List.copyOf(room.members)){Player p=Bukkit.getPlayer(id);if(p==null||p.getWorld()!=room.world)continue;
            BossBar bar=roleBars.get(id);if(bar!=null&&!bar.getTitle().equals(roleTitle(p)))bar.setTitle(roleTitle(p));
            Component taskHud=null;String line;if(!room.alive.contains(id))line="Seyircisin • /vk rehber • Ana lobi: /lobi";
            else if(room.phase==GameRoom.Phase.DAY){int target=target(p);if(target<0)line="Görevlerin tamam • Gördüklerini toplantıda anlat";else{var s=MapLayout.STATIONS.get(target);line=s.name()+" • "+trail.instruction(p,target);taskHud=GameUi.target(s.name(),trail.instruction(p,target));}
                Set<Integer> active=new HashSet<>();for(var task:tasks(p))if(!task.completed)active.add(TaskNavigation.station(task));if(active.isEmpty()&&target>=0)active.add(target);for(int station:active){if(distanceSquared(p,station)>32*32)continue;var point=MapLayout.STATIONS.get(station).location(room.world).add(0,1.6,0);p.spawnParticle(Particle.END_ROD,point,4,.3,.4,.3,.01);p.spawnParticle(Particle.DUST,point,8,.4,.35,.4,0,new Particle.DustOptions(station==target?Color.fromRGB(255,225,70):Color.fromRGB(255,185,35),1.4f));}
            }else line=switch(room.phase){case SPEECH->room.isSpeaker(id)?"Söz sende • Gördüklerini anlat • Erken bitir: /vk bitir":"Konuşanı dinle; ardından şüpheli vampir için oy vereceksin";case VOTE->"Vampir olduğundan şüphelendiğin kişiyi seç; emin değilsen Pas • /vk";case ROLE->switch(room.roles.get(id)){case VAMPIRE->"Gizlice gece hedefini seç • /vk";case SEER->"Rolünü öğrenmek istediğin kişiyi seç • /vk";case DOCTOR->"Koruyacağın kişiyi seç • /vk";default->"Gece rollerinin seçim yapmasını bekle";};default->"";};
            p.sendActionBar(taskHud!=null?taskHud:GameUi.line(line));
        }
    }
    private TextDisplay label(Location position,Component name){return room.world.spawn(position,TextDisplay.class,d->{d.setVisibleByDefault(false);d.text(name);d.setBillboard(Display.Billboard.CENTER);d.setShadowed(true);d.setSeeThrough(false);d.setBrightness(new Display.Brightness(15,15));d.setPersistent(false);});}
    private void removeLabel(TextDisplay label){if(label!=null)label.remove();}
    private void visible(Player viewer,TextDisplay label,boolean show){
        // Paper can clear visibility when entities unload. Check its current state, not a stale local cache.
        if(show==viewer.canSee(label))return;
        if(show)viewer.showEntity(room.plugin,label);else viewer.hideEntity(room.plugin,label);
    }
    void proximity(){
        if(closed||room.phase==GameRoom.Phase.FINISHED)return;
        refreshSigns();
        for(UUID id:List.copyOf(playerLabels.keySet()))if(!room.alive.contains(id)||!room.members.contains(id)||!playerLabels.get(id).isValid())removeLabel(playerLabels.remove(id));
        for(UUID id:room.alive){Player actor=Bukkit.getPlayer(id);if(actor==null||actor.getWorld()!=room.world)continue;TextDisplay label=playerLabels.computeIfAbsent(id,key->label(actor.getLocation().add(0,2.25,0),Component.text(actor.getName(),NamedTextColor.WHITE)));label.teleport(actor.getLocation().add(0,2.25,0));}
        for(UUID id:List.copyOf(corpseLabels.keySet()))if(!room.corpses.containsKey(id)||!corpseLabels.get(id).isValid())removeLabel(corpseLabels.remove(id));
        for(var entry:room.corpses.entrySet())corpseLabels.computeIfAbsent(entry.getKey(),id->label(entry.getValue().stand().getLocation().add(0,1.25,0),Component.text(ChatColor.stripColor(entry.getValue().stand().getCustomName()),NamedTextColor.RED)));
        for(UUID id:room.members){Player viewer=Bukkit.getPlayer(id);if(viewer==null||viewer.getWorld()!=room.world)continue;for(var entry:playerLabels.entrySet()){Player actor=Bukkit.getPlayer(entry.getKey());visible(viewer,entry.getValue(),actor!=null&&!id.equals(entry.getKey())&&viewer.getLocation().distanceSquared(actor.getLocation())<=100&&viewer.hasLineOfSight(actor));}for(var entry:corpseLabels.entrySet()){var corpse=room.corpses.get(entry.getKey());visible(viewer,entry.getValue(),corpse!=null&&viewer.getLocation().distanceSquared(corpse.stand().getLocation())<=25&&viewer.hasLineOfSight(corpse.stand()));}}
    }
    void forget(UUID id){removeLabel(playerLabels.remove(id));Player actor=Bukkit.getPlayer(id);if(actor!=null)room.plugin.privacy.release(actor);BossBar role=roleBars.remove(id);if(role!=null)role.removeAll();tracked.remove(id);rendered.remove(id);Player p=Bukkit.getPlayer(id);if(p!=null)p.sendActionBar(Component.empty());}
    void close(){if(closed)return;closed=true;for(UUID id:List.copyOf(room.members))forget(id);markers.forEach(Entity::remove);markers.clear();signs.values().forEach(list->list.forEach(Entity::remove));signs.clear();playerLabels.values().forEach(Entity::remove);corpseLabels.values().forEach(Entity::remove);playerLabels.clear();corpseLabels.clear();map.removeRenderer(renderer);rendered.clear();tracked.clear();}
    final class AtlasRenderer extends MapRenderer {
        AtlasRenderer(){super(true);}
        @Override public void render(MapView view,MapCanvas canvas,Player p){
            if(closed||p.getWorld()!=room.world||!room.members.contains(p.getUniqueId()))return;
            boolean shine=(System.currentTimeMillis()/700)%2==0;
            var tasks=tasks(p);String key=TaskNavigation.pixel(p.getX()-MapLayout.originX(room.world))+":"+TaskNavigation.pixel(p.getZ())+":"+Math.floorMod(Math.round(p.getYaw()/22.5f),16)+":"+target(p)+":"+shine+":"+tasks.stream().map(t->t.station+"/"+t.completed+"/"+t.carrying).toList();
            if(key.equals(rendered.put(p.getUniqueId(),key)))return;
            for(int z=0;z<128;z++)for(int x=0;x<128;x++)canvas.setPixel(x,z,terrain[z*128+x]);
            for(int i=0;i<tasks.size();i++){var task=tasks.get(i);var s=MapLayout.STATIONS.get(TaskNavigation.station(task));int x=TaskNavigation.pixel(s.x()+.5),z=TaskNavigation.pixel(s.z()+.5);byte color=task.completed?GREEN:task.kind==2&&task.carrying?CYAN:GOLD;
                for(int dz=-4;dz<=4;dz++)for(int dx=-4;dx<=4;dx++)if(x+dx>=0&&x+dx<128&&z+dz>=0&&z+dz<128)canvas.setPixel(x+dx,z+dz,Math.max(Math.abs(dx),Math.abs(dz))==4?(!task.completed&&shine?SHINE:INK):color);
                canvas.drawText(Math.clamp(x-1,0,121),Math.clamp(z-3,0,119),MinecraftFont.Font,String.valueOf(i+1));
            }
            int selected=target(p);if(selected>=0){var s=MapLayout.STATIONS.get(selected);int x=TaskNavigation.pixel(s.x()+.5),z=TaskNavigation.pixel(s.z()+.5);for(int dz=-6;dz<=6;dz++)for(int dx=-6;dx<=6;dx++)if(Math.max(Math.abs(dx),Math.abs(dz))==6&&x+dx>=0&&x+dx<128&&z+dz>=0&&z+dz<128)canvas.setPixel(x+dx,z+dz,shine?SHINE:GOLD);}
            for(int z=0;z<9;z++)for(int x=0;x<128;x++)canvas.setPixel(x,z,PAPER);canvas.drawText(3,1,MinecraftFont.Font,"N ^  GOREV HARITASI");
            var cursors=new MapCursorCollection();byte direction=(byte)Math.floorMod(Math.round(p.getYaw()/22.5f),16);cursors.addCursor(new MapCursor(TaskNavigation.cursor(p.getX()-MapLayout.originX(room.world)),TaskNavigation.cursor(p.getZ()),direction,MapCursor.Type.PLAYER,true,Component.text("Sen")));
            if(selected>=0){var s=MapLayout.STATIONS.get(selected);cursors.addCursor(new MapCursor(TaskNavigation.cursor(s.x()+.5),TaskNavigation.cursor(s.z()+.5),(byte)0,MapCursor.Type.RED_X,true,Component.text("Hedef: "+s.name())));}canvas.setCursors(cursors);
        }
    }
}

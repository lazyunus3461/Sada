package tr.kasaba.social;

import java.math.BigDecimal;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;

final class Fishing implements Listener {
 private final KasabaSocial plugin;private final NamespacedKey fish,rod;
 private static final Set<Material> FISH=Set.of(Material.COD,Material.SALMON,Material.TROPICAL_FISH,Material.PUFFERFISH,Material.COOKED_COD,Material.COOKED_SALMON);
 Fishing(KasabaSocial plugin){this.plugin=plugin;fish=new NamespacedKey(plugin,"tutulan-balik");rod=new NamespacedKey(plugin,"ucretsiz-olta");}
 private String path(Player p){return "players."+p.getUniqueId()+".fishing";}
 private String token(ItemStack item){return item!=null&&item.hasItemMeta()?item.getItemMeta().getPersistentDataContainer().get(fish,PersistentDataType.STRING):null;}
 private boolean own(Player p,String id){return id!=null&&p.getUniqueId().toString().equals(plugin.data.get().getString("fish."+id+".owner"))&&!plugin.data.get().getBoolean("fish."+id+".sold");}
 private boolean locked(Player p){return plugin.data.get().getLong(path(p)+".until")>System.currentTimeMillis();}
 private boolean issuedRod(ItemStack item){return item!=null&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(rod);}
 private void spawn(Player p){plugin.ready(p,false);if(!plugin.hub.mainLobby(p.getUniqueId()))throw new IllegalArgumentException("Balıkçılık yalnız ana spawn alanında kullanılır.");}
 void rod(Player p){spawn(p);long now=System.currentTimeMillis(),next=plugin.data.get().getLong(path(p)+".rodAt")+NetworkRules.HOUR;if(locked(p))throw new IllegalArgumentException("15 balık sattın. Yeni hak için "+((plugin.data.get().getLong(path(p)+".until")-now+59_999)/60_000)+" dakika bekle.");if(next>now)throw new IllegalArgumentException("Yeni ücretsiz olta için "+((next-now+59_999)/60_000)+" dakika bekle.");if(p.getInventory().firstEmpty()<0)throw new IllegalArgumentException("Oltaya yer aç.");ItemStack item=new ItemStack(Material.FISHING_ROD);item.editMeta(m->{m.setDisplayName("§bKasaba • Ücretsiz Spawn Oltası");m.setUnbreakable(true);m.getPersistentDataContainer().set(rod,PersistentDataType.STRING,p.getUniqueId().toString());m.setLore(List.of("§7Yalnız spawn balıkçılığında kullanılır.","§7/sat balık: kendi balığın 0,05 Akçe; günlük 15 adet.","§7Olta satışta 0 Akçeye geri alınır. Yeni olta: 1 saat."));});plugin.data.update(y->y.set(path(p)+".rodAt",now));p.getInventory().addItem(item);plugin.tell(p,"Ücretsiz oltan verildi. /sat balık • 15. satıştan sonra 24 saat ara; yeni olta 1 saat sonra.");}
 @EventHandler(priority=EventPriority.HIGHEST) public void use(PlayerInteractEvent e){if(!issuedRod(e.getItem()))return;Player p=e.getPlayer();boolean allowed=plugin.authenticated(p)&&plugin.hub.mainLobby(p.getUniqueId())&&!locked(p)&&p.getUniqueId().toString().equals(e.getItem().getItemMeta().getPersistentDataContainer().get(rod,PersistentDataType.STRING));e.setUseInteractedBlock(Event.Result.DENY);e.setUseItemInHand(allowed?Event.Result.ALLOW:Event.Result.DENY);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void fish(PlayerFishEvent e){Player p=e.getPlayer();if(!plugin.hub.mainLobby(p.getUniqueId())){if(issuedRod(p.getInventory().getItemInMainHand())||issuedRod(p.getInventory().getItemInOffHand()))e.setCancelled(true);return;}if(!plugin.authenticated(p)||locked(p)){e.setCancelled(true);return;}if(e.getState()!=PlayerFishEvent.State.CAUGHT_FISH||!(e.getCaught() instanceof Item caught))return;
  ItemStack rodItem=p.getInventory().getItemInMainHand();if(rodItem.getType()!=Material.FISHING_ROD)rodItem=p.getInventory().getItemInOffHand();if(!issuedRod(rodItem)||!p.getUniqueId().toString().equals(rodItem.getItemMeta().getPersistentDataContainer().get(rod,PersistentDataType.STRING))){e.setCancelled(true);return;}
  String id=UUID.randomUUID().toString();plugin.data.update(y->{y.set("fish."+id+".owner",p.getUniqueId().toString());y.set("fish."+id+".sold",false);});ItemStack item=new ItemStack(Material.COD);item.editMeta(m->{m.setDisplayName("§bKasaba Balığı §7• "+p.getName());m.setLore(List.of("§7Yalnız tutan oyuncu satabilir.","§7/sat balık • 0,05 Akçe • günlük 15 adet"));m.getPersistentDataContainer().set(fish,PersistentDataType.STRING,id);});caught.setItemStack(item);caught.setOwner(p.getUniqueId());e.setExpToDrop(0);
 }
 void sell(Player p){spawn(p);long now=System.currentTimeMillis();if(locked(p)){clean(p);throw new IllegalArgumentException("Günlük 15 balık hakkın doldu. 24 saat tamamlanınca yeniden satabilirsin.");}int already=plugin.data.get().getLong(path(p)+".until")>0?0:plugin.data.get().getInt(path(p)+".sold");Map<Integer,String> selected=new LinkedHashMap<>();Set<String> tokens=new HashSet<>();for(int slot=0;slot<36&&selected.size()<15-already;slot++){String id=token(p.getInventory().getItem(slot));if(own(p,id)&&tokens.add(id))selected.put(slot,id);}if(selected.isEmpty())throw new IllegalArgumentException("Satabileceğin, kendi tuttuğun balık yok. Başkasının balığı satılamaz.");int total=already+selected.size();String transaction="fish-"+UUID.randomUUID();BigDecimal amount=BigDecimal.valueOf(selected.size()*5L,2);
  plugin.data.update(y->{selected.values().forEach(id->y.set("fish."+id+".sold",true));y.set(path(p)+".sold",total);y.set(path(p)+".until",total==15?now+NetworkRules.DAY:0L);plugin.creditJob(y,transaction,p.getUniqueId(),amount);});selected.keySet().forEach(slot->p.getInventory().setItem(slot,null));for(int slot=0;slot<p.getInventory().getSize();slot++)if(issuedRod(p.getInventory().getItem(slot)))p.getInventory().setItem(slot,null);clean(p);plugin.settle();plugin.tell(p,selected.size()+" balık satıldı: +"+amount.toPlainString()+" Akçe. Olta ücretsiz geri alındı. "+(total==15?"24 saat sayacı başladı.":"Günlük kullanım: "+total+"/15."));
 }
 void clean(Player p){boolean lock=locked(p);for(int slot=0;slot<p.getInventory().getSize();slot++){ItemStack item=p.getInventory().getItem(slot);if(item==null)continue;String id=token(item);if(lock&&FISH.contains(item.getType())||id!=null&&!own(p,id))p.getInventory().setItem(slot,null);}ItemStack cursor=p.getItemOnCursor();if(cursor!=null&&(lock&&FISH.contains(cursor.getType())||token(cursor)!=null&&!own(p,token(cursor))))p.setItemOnCursor(null);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void pickup(EntityPickupItemEvent e){if(!(e.getEntity() instanceof Player p)||!plugin.authenticated(p))return;ItemStack item=e.getItem().getItemStack();String id=token(item);if(locked(p)&&FISH.contains(item.getType())||id!=null&&!own(p,id)){e.setCancelled(true);if(locked(p))e.getItem().remove();}}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void click(InventoryClickEvent e){if(e.getWhoClicked() instanceof Player p)Bukkit.getScheduler().runTask(plugin,()->{if(p.isOnline()&&plugin.authenticated(p))clean(p);});}
}

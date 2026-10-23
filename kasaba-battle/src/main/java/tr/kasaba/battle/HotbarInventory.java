package tr.kasaba.battle;

import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.Material;

final class HotbarInventory {
 private HotbarInventory(){}
 /** Return leftover amount; never writes slots 9..35 or offhand. */
 static int add(Player p,ItemStack source){int left=source.getAmount();for(int i=0;i<9&&left>0;i++){ItemStack item=p.getInventory().getItem(i);if(item!=null&&item.isSimilar(source)){int amount=Math.min(left,Math.max(0,item.getMaxStackSize()-item.getAmount()));item.setAmount(item.getAmount()+amount);p.getInventory().setItem(i,item);left-=amount;}}for(int i=0;i<9&&left>0;i++){ItemStack item=p.getInventory().getItem(i);if(item==null||item.getType().isAir()){ItemStack copy=source.clone();copy.setAmount(Math.min(left,copy.getMaxStackSize()));p.getInventory().setItem(i,copy);left-=copy.getAmount();}}return left;}
 static boolean equip(Player p,ItemStack item){String type=item.getType().name();EquipmentSlot slot=type.endsWith("_HELMET")?EquipmentSlot.HEAD:type.endsWith("_CHESTPLATE")?EquipmentSlot.CHEST:type.endsWith("_LEGGINGS")?EquipmentSlot.LEGS:type.endsWith("_BOOTS")?EquipmentSlot.FEET:null;if(slot==null)return false;ItemStack existing=p.getInventory().getItem(slot);if(existing!=null&&!existing.getType().isAir())return false;ItemStack one=item.clone();one.setAmount(1);p.getInventory().setItem(slot,one);item.setAmount(item.getAmount()-1);return true;}
 static boolean forbidden(int playerSlot){return playerSlot>=9&&playerSlot<=35||playerSlot==40;}
}

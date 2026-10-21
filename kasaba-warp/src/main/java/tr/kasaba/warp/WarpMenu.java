package tr.kasaba.warp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.*;
import java.util.*;

final class WarpMenu implements InventoryHolder {
    static final int PAGE_SIZE=45;
    final UUID owner;
    final int page;
    final List<Warp> visible;
    final Map<Integer,String> destinations=new HashMap<>();
    private final Inventory inventory;

    WarpMenu(UUID owner,int requestedPage,List<Warp> visible,int warmup,int cooldown) {
        this.owner=owner;this.visible=List.copyOf(visible);
        int last=Math.max(0,(visible.size()-1)/PAGE_SIZE);
        page=Math.clamp(requestedPage,0,last);
        inventory=Bukkit.createInventory(this,54,Component.text("Kasaba • Warp Noktaları "+(page+1)+"/"+(last+1),NamedTextColor.GOLD));
        for(int slot=0,index=page*PAGE_SIZE;slot<PAGE_SIZE&&index<visible.size();slot++,index++) {
            Warp warp=visible.get(index);List<Component> lore=new ArrayList<>();
            if(!warp.description().isBlank())for(String line:wrap(warp.description()))lore.add(line(line,NamedTextColor.GRAY));
            lore.add(line("/warp "+warp.id(),NamedTextColor.DARK_GRAY));
            lore.add(line("Gitmek için tıkla",NamedTextColor.GREEN));
            inventory.setItem(slot,item(Material.ENDER_PEARL,warp.name(),NamedTextColor.AQUA,lore));destinations.put(slot,warp.id());
        }
        if(visible.isEmpty())inventory.setItem(22,item(Material.MAP,"Gidilebilecek warp yok",NamedTextColor.YELLOW,List.of()));
        for(int slot=45;slot<54;slot++)inventory.setItem(slot,item(Material.GRAY_STAINED_GLASS_PANE," ",NamedTextColor.GRAY,List.of()));
        if(page>0)inventory.setItem(45,item(Material.ARROW,"Önceki sayfa",NamedTextColor.YELLOW,List.of()));
        inventory.setItem(47,item(Material.CLOCK,"Işınlanma bilgisi",NamedTextColor.GOLD,List.of(
                line("Hazırlık: "+warmup+" saniye",NamedTextColor.GRAY),line("Kullanım arası: "+cooldown+" saniye",NamedTextColor.GRAY),
                line("Hareket ve hasar hazırlığı iptal eder.",NamedTextColor.GRAY))));
        inventory.setItem(49,item(Material.BARRIER,"Kapat",NamedTextColor.RED,List.of()));
        inventory.setItem(51,item(Material.COMPASS,visible.size()+" warp noktası",NamedTextColor.AQUA,List.of(line("Komut: /warp <ad>",NamedTextColor.GRAY))));
        if(page<last)inventory.setItem(53,item(Material.ARROW,"Sonraki sayfa",NamedTextColor.YELLOW,List.of()));
    }
    @Override public Inventory getInventory(){return inventory;}
    private static Component line(String value,NamedTextColor color){return Component.text(value,color).decoration(TextDecoration.ITALIC,false);}
    private static ItemStack item(Material material,String label,NamedTextColor color,List<Component> lore) {
        ItemStack item=new ItemStack(material);var meta=item.getItemMeta();
        meta.displayName(line(label,color));meta.lore(lore);item.setItemMeta(meta);return item;
    }
    private static List<String> wrap(String text){
        List<String> lines=new ArrayList<>();StringBuilder line=new StringBuilder();
        for(String word:text.split("\\s+")) {
            if(line.length()>0&&line.length()+word.length()+1>40){lines.add(line.toString());line.setLength(0);}
            if(line.length()>0)line.append(' ');line.append(word);
        }
        if(line.length()>0)lines.add(line.toString());return lines;
    }
}

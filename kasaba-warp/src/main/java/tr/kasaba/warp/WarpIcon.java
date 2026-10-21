package tr.kasaba.warp;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import java.util.Base64;

final class WarpIcon {
    private static final int MAX_BYTES=65_536;
    static void validate(String encoded) {
        if(encoded==null)throw new IllegalArgumentException("Simge verisi boş olamaz.");
        if(encoded.isEmpty())return;
        if(encoded.length()>87_384)throw new IllegalArgumentException("Simge eşyasının verisi çok büyük.");
        try {
            if(Base64.getDecoder().decode(encoded).length>MAX_BYTES)throw new IllegalArgumentException("Simge eşyasının verisi çok büyük.");
        }catch(IllegalArgumentException e){throw new IllegalArgumentException("Simge verisi geçersiz veya çok büyük.",e);}
    }
    static String capture(ItemStack held) {
        if(held==null||held.getType().isAir())return "";
        ItemStack copy=held.clone();copy.setAmount(1);
        String encoded=Base64.getEncoder().encodeToString(copy.serializeAsBytes());validate(encoded);return encoded;
    }
    static ItemStack decode(String encoded) {
        if(encoded.isEmpty())return new ItemStack(Material.ENDER_PEARL);
        validate(encoded);ItemStack item=ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded));
        if(item.getType().isAir())throw new IllegalArgumentException("Simge boş bir eşya olamaz.");
        item.setAmount(1);return item;
    }
}

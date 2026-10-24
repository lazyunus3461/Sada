package tr.kasaba.roles;

import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.LinkedHashMap;

record Role(String id, String label, String color, String nameColor, String chatColor, int priority,
            List<String> inherits, Map<String, Boolean> permissions) {
    Role(String id,String label,String color,String nameColor,int priority,List<String> inherits,Map<String,Boolean> permissions){this(id,label,color,nameColor,null,priority,inherits,permissions);}
    Role {
        id = normalizeId(id);
        if (label == null || label.isBlank() || label.length() > 48) throw new IllegalArgumentException("Rol etiketi 1–48 karakter olmalı");
        if (priority < 1 || priority > 1_000_000) throw new IllegalArgumentException("Sıra 1–1000000 arasında olmalı");
        inherits = inherits.stream().map(Role::normalizeId).toList();
        Map<String,Boolean> normalized=new LinkedHashMap<>();
        permissions.forEach((node,value)->normalized.put(permissionNode(node),value));
        permissions = Map.copyOf(normalized);
    }
    static String normalizeId(String value) {
        String id = value.toLowerCase(Locale.ROOT);
        if (!id.matches("[a-z0-9_-]{1,32}")) throw new IllegalArgumentException("Rol kimliği yalnızca a-z, 0-9, _ ve - içerebilir");
        return id;
    }
    static String playerKey(String value) {
        if (!value.matches("[A-Za-z0-9_]{1,16}")) throw new IllegalArgumentException("Geçerli Minecraft oyuncu adını yaz");
        return value.toLowerCase(Locale.ROOT);
    }
    static String permissionNode(String node) {
        if (!node.matches("[A-Za-z0-9_.*:-]{1,128}")) throw new IllegalArgumentException("Geçersiz yetki düğümü");
        return node.toLowerCase(Locale.ROOT);
    }
}

package tr.kasaba.warp;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

record Warp(String id, String name, String description, boolean restricted, UUID world, String worldName,
            double x, double y, double z, float yaw, float pitch) {
    static final Set<String> COMMANDS=Set.of("bilgi","iptal","yardim","olustur","tasi","sil","isim","aciklama","gonder","yenile","yetki");
    Warp {
        id=id(id);
        if(COMMANDS.contains(id))throw new IllegalArgumentException("Bu ad bir komut için ayrılmış: "+id);
        if(name==null||name.isBlank()||name.length()>64)throw new IllegalArgumentException("Görünen ad 1–64 karakter olmalı.");
        if(description==null||description.length()>240)throw new IllegalArgumentException("Açıklama en fazla 240 karakter olmalı.");
        if(world==null||worldName==null||worldName.isBlank()
                ||!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)
                ||!Float.isFinite(yaw)||!Float.isFinite(pitch)
                ||Math.abs(x)>30_000_000||Math.abs(z)>30_000_000||Math.abs(pitch)>90)
            throw new IllegalArgumentException("Geçersiz warp konumu.");
    }
    static String id(String input) {
        if(input==null)throw new IllegalArgumentException("Warp adını yaz.");
        String value=Normalizer.normalize(input,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replace("\u0307","").replace('ç','c').replace('ğ','g').replace('ı','i')
                .replace('ö','o').replace('ş','s').replace('ü','u');
        if(!value.matches("[a-z0-9_-]{1,32}"))
            throw new IllegalArgumentException("Warp adı 1–32 harf, rakam, _ veya - içermeli; boşluk kullanma.");
        return value;
    }
    String permission(){return "kasabawarp.git."+id;}
    Warp renamed(String label){return new Warp(id,label,description,restricted,world,worldName,x,y,z,yaw,pitch);}
    Warp described(String text){return new Warp(id,name,text,restricted,world,worldName,x,y,z,yaw,pitch);}
    Warp restricted(boolean value){return new Warp(id,name,description,value,world,worldName,x,y,z,yaw,pitch);}
}

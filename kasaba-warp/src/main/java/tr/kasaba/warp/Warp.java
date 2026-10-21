package tr.kasaba.warp;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.random.RandomGenerator;

record Warp(String id, String name, String description, boolean restricted, UUID world, String worldName,
            double x, double y, double z, float yaw, float pitch, String icon, int menuSlot,List<WarpPoint> extraPoints) {
    static final Set<String> COMMANDS=Set.of("bilgi","iptal","yardim","olustur","tasi","sil","isim","aciklama","gonder","yenile","yetki","simge","noktaekle","noktalar","noktasil");
    Warp {
        id=id(id);
        if(COMMANDS.contains(id))throw new IllegalArgumentException("Bu ad bir komut için ayrılmış: "+id);
        if(name==null||name.isBlank()||name.length()>64)throw new IllegalArgumentException("Görünen ad 1–64 karakter olmalı.");
        if(description==null||description.length()>240)throw new IllegalArgumentException("Açıklama en fazla 240 karakter olmalı.");
        if(world==null||worldName==null||worldName.isBlank())
            throw new IllegalArgumentException("Geçersiz warp konumu.");
        WarpPoint first=new WarpPoint(x,y,z,yaw,pitch);
        extraPoints=List.copyOf(extraPoints);
        List<WarpPoint> seen=new ArrayList<>();seen.add(first);
        for(WarpPoint point:extraPoints){if(seen.stream().anyMatch(point::samePosition))throw new IllegalArgumentException("Bu konum zaten warp noktaları arasında.");seen.add(point);}
        WarpIcon.validate(icon);
        if(menuSlot< -1||menuSlot>=45)throw new IllegalArgumentException("Menü yeri ilk 5 sıra ve 9 kutu içinde olmalı.");
    }
    Warp(String id,String name,String description,boolean restricted,UUID world,String worldName,double x,double y,double z,float yaw,float pitch) {
        this(id,name,description,restricted,world,worldName,x,y,z,yaw,pitch,"",-1);
    }
    Warp(String id,String name,String description,boolean restricted,UUID world,String worldName,double x,double y,double z,float yaw,float pitch,String icon,int menuSlot) {
        this(id,name,description,restricted,world,worldName,x,y,z,yaw,pitch,icon,menuSlot,List.of());
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
    Warp renamed(String label){return new Warp(id,label,description,restricted,world,worldName,x,y,z,yaw,pitch,icon,menuSlot,extraPoints);}
    Warp described(String text){return new Warp(id,name,text,restricted,world,worldName,x,y,z,yaw,pitch,icon,menuSlot,extraPoints);}
    Warp restricted(boolean value){return new Warp(id,name,description,value,world,worldName,x,y,z,yaw,pitch,icon,menuSlot,extraPoints);}
    Warp displayed(String data,int slot){return new Warp(id,name,description,restricted,world,worldName,x,y,z,yaw,pitch,data,slot,extraPoints);}
    List<WarpPoint> points(){List<WarpPoint> result=new ArrayList<>();result.add(new WarpPoint(x,y,z,yaw,pitch));result.addAll(extraPoints);return List.copyOf(result);}
    WarpPoint choosePoint(RandomGenerator random){List<WarpPoint> points=points();return points.get(random.nextInt(points.size()));}
    private Warp withPoints(List<WarpPoint> points,UUID world,String worldName){WarpPoint p=points.getFirst();return new Warp(id,name,description,restricted,world,worldName,p.x(),p.y(),p.z(),p.yaw(),p.pitch(),icon,menuSlot,points.subList(1,points.size()));}
    Warp addedPoint(UUID world,WarpPoint point){
        if(!this.world.equals(world))throw new IllegalArgumentException("Ek noktayı warpın dünyasında kaydet: "+worldName);
        List<WarpPoint> points=new ArrayList<>(points());points.add(point);return withPoints(points,this.world,worldName);
    }
    Warp removedPoint(int number){
        List<WarpPoint> points=new ArrayList<>(points());
        if(number<1||number>points.size())throw new IllegalArgumentException("Nokta numarası 1–"+points.size()+" arasında olmalı.");
        if(points.size()==1)throw new IllegalArgumentException("Son nokta silinemez. Warpı kaldırmak için /warp sil "+id);
        points.remove(number-1);return withPoints(points,world,worldName);
    }
    Warp moved(UUID world,String worldName,WarpPoint point){
        if(!extraPoints.isEmpty()&&!this.world.equals(world))throw new IllegalArgumentException("Birden fazla noktası olan warp başka dünyaya taşınamaz; önce ek noktaları sil.");
        List<WarpPoint> points=new ArrayList<>(points());points.set(0,point);return withPoints(points,world,worldName);
    }
}

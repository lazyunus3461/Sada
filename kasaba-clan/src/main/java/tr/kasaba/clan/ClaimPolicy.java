package tr.kasaba.clan;

import tr.kasaba.warp.api.WarpDestination;
import java.util.*;

final class ClaimPolicy {
    static void check(ClanState state,Clan clan,UUID world,Rect rect,ClaimIndex index,List<WarpDestination> warps,int total,int count,int side){
        if(state.forest()==null)throw new IllegalArgumentException("Orman dünyası ayarlanmamış. Yönetici /klan orman ayarla kullanmalı.");
        if(!state.forest().equals(world))throw new IllegalArgumentException("Yalnızca orman dünyasında claim alabilirsin: "+state.forestName());
        if(rect.width()>side||rect.depth()>side)throw new IllegalArgumentException("Bir kenar en fazla "+side+" blok olabilir.");
        if(clan.claims().size()>=count)throw new IllegalArgumentException("Klanın arazi sayısı sınırı dolu: "+count);
        if(rect.area()+clan.area()>total)throw new IllegalArgumentException("Toplam arazi sınırı "+total+" blok. Kullanılan: "+clan.area()+", seçilen: "+rect.area());
        var overlap=index.overlap(world,rect);if(overlap!=null)throw new IllegalArgumentException("Seçtiğin alan mevcut bir araziyle çakışıyor: "+overlap.clan().tag()+" #"+overlap.claim().number());
        for(WarpDestination warp:warps)if(warp.world().equals(world)&&Rect.warp(warp.x(),warp.z()).overlaps(rect))throw new IllegalArgumentException("Bir warp çevresindeki 100×100 alanda claim alınamaz.");
    }
    static Optional<String> warpRejection(ClaimIndex index,List<WarpDestination> warps){
        for(WarpDestination warp:warps){var overlap=index.overlap(warp.world(),Rect.warp(warp.x(),warp.z()));
            if(overlap!=null)return Optional.of("Warpın 100×100 koruması "+overlap.clan().tag()+" klanının #"+overlap.claim().number()+" arazisiyle çakışıyor. Önce başka nokta seç veya araziyi yetkili olarak kaldır.");
        }
        return Optional.empty();
    }
}

package tr.kasaba.warp;

import java.util.*;

final class MenuLayout {
    static int parse(String position) {
        if(position==null||!position.matches("[1-5]-[1-9]"))
            throw new IllegalArgumentException("Menü yeri sıra-kutu biçiminde olmalı: 1-3. Sıra 1–5, kutu 1–9; alt sıra düğmeler içindir.");
        return (position.charAt(0)-'1')*9+(position.charAt(2)-'1');
    }
    static String label(int slot){return (slot/9+1)+"-"+(slot%9+1);}
    static Set<Integer> reserved(Collection<Warp> warps) {
        Set<Integer> reserved=new HashSet<>();
        for(Warp warp:warps)if(warp.menuSlot()>=0)reserved.add(warp.menuSlot());
        return Set.copyOf(reserved);
    }
    static void validate(Collection<Warp> warps) {
        Map<Integer,String> occupied=new HashMap<>();
        for(Warp warp:warps)if(warp.menuSlot()>=0) {
            String existing=occupied.putIfAbsent(warp.menuSlot(),warp.id());
            if(existing!=null)throw new IllegalArgumentException(label(warp.menuSlot())+" kutusunda zaten "+existing+" var. Başka kutu seç.");
        }
    }
    static Map<Integer,Warp> plan(List<Warp> visible,Set<Integer> reserved) {
        Map<Integer,Warp> slots=new TreeMap<>();
        for(Warp warp:visible)if(warp.menuSlot()>=0)slots.put(warp.menuSlot(),warp);
        int next=0;
        for(Warp warp:visible)if(warp.menuSlot()<0) {
            while(reserved.contains(next)||slots.containsKey(next))next++;
            slots.put(next++,warp);
        }
        return Collections.unmodifiableMap(slots);
    }
}

package tr.kasaba.battle;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;

/** A compact staging camp contained entirely in the saved, protected BR lobby. */
final class BattleLobby {
    private final ArenaStore store;private final Location origin;
    private BattleLobby(ArenaStore store){this.store=store;origin=store.lobby;}
    private void put(int dx,int y,int dz,Material material){Location at=origin.clone().add(dx,y,dz);if(ArenaStore.in(store.lobbyArea,at))at.getBlock().setType(material,false);}
    static void decorate(JavaPlugin plugin,ArenaStore store){
        if(!store.lobbyReady())return;Location p=store.lobby;String key=p.getWorld().getUID()+"/"+p.getBlockX()+"/"+p.getBlockY()+"/"+p.getBlockZ();
        if(key.equals(plugin.getConfig().getString("lobby-camp-v1")))return;
        BattleLobby camp=new BattleLobby(store);
        for(int x=-19;x<=19;x++)for(int z=-17;z<=17;z++){
            camp.put(x,-1,z,Math.abs(x)<=3||Math.abs(z)<=2?Material.STONE_BRICKS:Math.floorMod(x*17+z*7,5)==0?Material.MOSSY_COBBLESTONE:Material.COARSE_DIRT);
            if(Math.abs(x)==19||Math.abs(z)==17){camp.put(x,0,z,Material.POLISHED_DEEPSLATE);camp.put(x,1,z,Material.IRON_BARS);}
        }
        // Two supply tents, clear center lane, wooden supply stacks and watch posts.
        for(int side:new int[]{-1,1}){
            int cx=side*12;
            for(int x=-5;x<=5;x++)for(int z=-5;z<=3;z++){
                for(int y=0;y<=3;y++)camp.put(cx+x,y,z,Material.AIR);
                camp.put(cx+x,-1,z,Material.SPRUCE_PLANKS);
                int roof=5-Math.abs(x)/2;camp.put(cx+x,roof,z,((x+z)&3)==0?Material.MOSS_BLOCK:Material.GREEN_CONCRETE);
                if(Math.abs(x)==5||z==-5)for(int y=0;y<roof;y++)camp.put(cx+x,y,z,Material.GREEN_TERRACOTTA);
            }
            for(int z=-4;z<=1;z++)camp.put(cx+side*3,0,z,Material.RED_CARPET);
            for(int i=0;i<3;i++)for(int y=0;y<2+(i&1);y++)camp.put(cx-3+i*2,y,10,Material.STRIPPED_SPRUCE_LOG);
            for(int x:new int[]{-17,17})for(int z:new int[]{-14,14}){
                for(int y=0;y<=5;y++)camp.put(x,y,z,Material.POLISHED_ANDESITE);
                camp.put(x,6,z,Material.SEA_LANTERN);
            }
        }
        for(int x=-5;x<=5;x++){camp.put(x,-1,7,(x&1)==0?Material.YELLOW_CONCRETE:Material.BLACK_CONCRETE);camp.put(x,-1,-8,(x&1)==0?Material.YELLOW_CONCRETE:Material.BLACK_CONCRETE);}
        // A suspended parachute canopy and supply pallet at the back of the camp.
        for(int x=-4;x<=4;x++)for(int z=-3;z<=3;z++)camp.put(x,8-Math.max(Math.abs(x),Math.abs(z))/2,-12+z,Math.abs(x)%3==0?Material.ORANGE_WOOL:Material.WHITE_WOOL);
        for(int x:new int[]{-3,3})for(int z:new int[]{-14,-10})for(int y=3;y<=6;y++)camp.put(x,y,z,Material.IRON_CHAIN);
        for(int x=-2;x<=2;x++)for(int z=-13;z<=-11;z++)camp.put(x,0,z,Material.STRIPPED_OAK_LOG);
        p.getBlock().setType(Material.AIR,false);p.clone().add(0,1,0).getBlock().setType(Material.AIR,false);p.getWorld().save();
        plugin.getConfig().set("lobby-camp-v1",key);plugin.saveConfig();
    }
}

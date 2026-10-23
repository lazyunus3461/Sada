package tr.kasabatr.vampir;

import java.util.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;

/** Station routes shared within one immutable match map; particles go only to their owner. */
final class GroundTrail {
    static final int FLOOR_Y=69, VISIBLE_STEPS=10;
    private static final Particle.DustOptions DUST=new Particle.DustOptions(Color.fromRGB(65,235,255),.65f);
    private static final double[][] ARROW={{-.28,0},{-.12,0},{.04,0},{.20,0},{.28,0},{.10,-.16},{.10,.16}};
    private final World world;
    private final List<GroundRoute.Field> fields;

    GroundTrail(World world) {
        this.world=world;
        GroundRoute grid=new GroundRoute(-68,-68,137,137,(x,z)->{
            Block feet=world.getBlockAt(x,FLOOR_Y,z),head=world.getBlockAt(x,FLOOR_Y+1,z);
            return world.getBlockAt(x,FLOOR_Y-1,z).getType().isSolid()
                &&openable(feet)&&openable(head)&&!feet.isLiquid()&&!head.isLiquid();
        });
        fields=MapLayout.STATIONS.stream().map(s->grid.toward(s.x(),s.z())).toList();
    }

    private static boolean openable(Block block) {
        return block.isPassable()||block.getBlockData() instanceof Door&&Tag.WOODEN_DOORS.isTagged(block.getType());
    }

    private static boolean clearNow(Block block) {
        // Bukkit isPassable() is false even for an open door: its thin leaf still has collision.
        if(block.getBlockData() instanceof Door door&&Tag.WOODEN_DOORS.isTagged(block.getType()))return door.isOpen();
        return block.isPassable();
    }

    int distance(Player player,int station) {
        return player.getWorld()!=world?-1:fields.get(station).distance(player.getLocation().getBlockX(),player.getLocation().getBlockZ());
    }

    List<GroundRoute.Cell> steps(Player player,int station) {
        if(player.getWorld()!=world||Math.abs(player.getY()-FLOOR_Y)>1.25)return List.of();
        return fields.get(station).steps(player.getLocation().getBlockX(),player.getLocation().getBlockZ(),VISIBLE_STEPS+1);
    }

    String instruction(Player player,int station) {
        List<GroundRoute.Cell> route=steps(player,station);
        if(route.isEmpty())return "Yere in; yürünebilir yolda oklar görünecek";
        if(route.size()==1)return "Görev burada • İşaretli bloğa sağ tıkla";
        var next=route.get(1);
        if(!clearNow(world.getBlockAt(next.x(),FLOOR_Y,next.z())))return "Önündeki kapıyı sağ tıklayıp aç • Yerdeki okları izle";
        return "Yerdeki mavi okları izle • "+distance(player,station)+" blok yol";
    }

    int render(Player player,int station) {
        List<GroundRoute.Cell> route=steps(player,station);int arrows=0;
        for(int i=0;i<route.size();i++) {
            var cell=route.get(i);Block feet=world.getBlockAt(cell.x(),FLOOR_Y,cell.z());
            // A closed door is a valid route, but show the trail up to it until the player opens it.
            if(!clearNow(feet)||!clearNow(world.getBlockAt(cell.x(),FLOOR_Y+1,cell.z())))break;
            GroundRoute.Cell next=i+1<route.size()?route.get(i+1):null;
            if(next==null&&distance(player,station)==i){var s=MapLayout.STATIONS.get(station);next=new GroundRoute.Cell(s.x(),s.z());}
            if(next==null)break;
            int dx=next.x()-cell.x(),dz=next.z()-cell.z();
            for(double[] dot:ARROW)player.spawnParticle(Particle.DUST,
                cell.x()+.5+dx*dot[0]-dz*dot[1],FLOOR_Y+.08,cell.z()+.5+dz*dot[0]+dx*dot[1],
                1,0,0,0,0,DUST);
            if(++arrows==VISIBLE_STEPS)break;
        }
        return arrows;
    }
}

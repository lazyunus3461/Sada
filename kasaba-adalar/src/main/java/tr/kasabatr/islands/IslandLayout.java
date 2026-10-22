package tr.kasabatr.islands;

import org.bukkit.Location;
import org.bukkit.World;

/** IDs stay stable when the supplied world is compacted: 20 columns by 15 rows. */
final class IslandLayout {
    static final int COLUMNS=20,ROWS=15,COUNT=300,CLAIM_RADIUS=48,SPAWN_Y=66;
    private final int spacing;
    IslandLayout(int spacing){if(spacing!=100&&spacing!=200)throw new IllegalArgumentException("Ada aralığı 100 veya 200 olmalı.");this.spacing=spacing;}
    int spacing(){return spacing;}
    int centerX(int id){validate(id);return (id-1)%COLUMNS*spacing;}
    int centerZ(int id){validate(id);return (id-1)/COLUMNS*spacing;}
    Location spawn(World world,int id){return new Location(world,centerX(id)+0.5,SPAWN_Y,centerZ(id)+0.5,0,0);}
    boolean contains(int id,int x,int z){return id>=1&&id<=COUNT&&x>=centerX(id)-CLAIM_RADIUS&&x<centerX(id)+CLAIM_RADIUS&&z>=centerZ(id)-CLAIM_RADIUS&&z<centerZ(id)+CLAIM_RADIUS;}
    int at(int x,int z){int column=Math.floorDiv(x+CLAIM_RADIUS,spacing),row=Math.floorDiv(z+CLAIM_RADIUS,spacing);if(column<0||column>=COLUMNS||row<0||row>=ROWS)return 0;int id=row*COLUMNS+column+1;return contains(id,x,z)?id:0;}
    private static void validate(int id){if(id<1||id>COUNT)throw new IllegalArgumentException("Ada numarası 1–300 arasında olmalı.");}
}

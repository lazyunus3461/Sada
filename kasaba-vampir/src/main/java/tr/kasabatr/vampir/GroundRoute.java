package tr.kasabatr.vampir;

import java.util.*;
import java.util.function.BiPredicate;

/** A cardinal, shortest walking route. No diagonal corner cutting or wall shortcuts. */
final class GroundRoute {
    record Cell(int x, int z) {}
    private static final int[][] DIRECTIONS = {{0,-1},{1,0},{0,1},{-1,0}};
    private final int minX, minZ, width, depth;
    private final boolean[] walkable;

    GroundRoute(int minX, int minZ, int width, int depth, BiPredicate<Integer,Integer> walkable) {
        if (width <= 0 || depth <= 0) throw new IllegalArgumentException("Invalid grid");
        this.minX=minX; this.minZ=minZ; this.width=width; this.depth=depth;
        this.walkable=new boolean[Math.multiplyExact(width,depth)];
        for(int z=minZ;z<minZ+depth;z++)for(int x=minX;x<minX+width;x++)
            this.walkable[index(x,z)]=walkable.test(x,z);
    }

    private int index(int x,int z) {
        return x<minX||z<minZ||x>=minX+width||z>=minZ+depth ? -1 : (z-minZ)*width+x-minX;
    }

    Field toward(int targetX,int targetZ) {
        int[] distance=new int[walkable.length]; Arrays.fill(distance,-1);
        int[] queue=new int[walkable.length]; int head=0,tail=0;
        // The workstation itself is a solid block. Stop on a directly adjacent floor tile.
        for(int[] direction:DIRECTIONS) {
            int i=index(targetX+direction[0],targetZ+direction[1]);
            if(i>=0&&walkable[i]){distance[i]=0;queue[tail++]=i;}
        }
        int target=index(targetX,targetZ);
        while(head<tail) {
            int current=queue[head++],x=minX+current%width,z=minZ+current/width;
            for(int[] direction:DIRECTIONS) {
                int i=index(x+direction[0],z+direction[1]);
                if(i>=0&&i!=target&&walkable[i]&&distance[i]<0) {
                    distance[i]=distance[current]+1;queue[tail++]=i;
                }
            }
        }
        return new Field(distance);
    }

    final class Field {
        private final int[] distance;
        private Field(int[] distance){this.distance=distance;}
        int distance(int x,int z){int i=index(x,z);return i<0?-1:distance[i];}
        List<Cell> steps(int x,int z,int limit) {
            if(limit<=0||distance(x,z)<0)return List.of();
            List<Cell> result=new ArrayList<>(); int previous=-1;
            while(result.size()<limit) {
                result.add(new Cell(x,z));int remaining=distance(x,z);if(remaining==0)break;
                int selected=-1;
                // Prefer continuing straight when several shortest paths have the same cost.
                for(int n=0;n<4;n++) {
                    int d=previous>=0?Math.floorMod(previous+n,4):n;
                    if(distance(x+DIRECTIONS[d][0],z+DIRECTIONS[d][1])==remaining-1){selected=d;break;}
                }
                if(selected<0)return List.of();
                x+=DIRECTIONS[selected][0];z+=DIRECTIONS[selected][1];previous=selected;
            }
            return List.copyOf(result);
        }
    }
}

package tr.kasabatr.renk;

import java.util.*;

/** Each row contains every color; nearby rows shift the pattern to keep escape routes reachable. */
final class RoundRules {
    static final int COLORS=8,SIZE=32,CELL=4;
    static int[] layout(Random random){var colors=new ArrayList<Integer>();for(int i=0;i<COLORS;i++)colors.add(i);Collections.shuffle(colors,random);int[] cells=new int[64];for(int z=0;z<8;z++)for(int x=0;x<8;x++)cells[z*8+x]=colors.get((x+2*z)%8);return cells;}
    static int cellSize(int round){return round<=3?4:round<=6?3:2;}
    static int boardSize(int round){return cellSize(round)*8;}
    static int offset(int round){return (SIZE-boardSize(round))/2;}
    static int colorAt(int[] cells,int x,int z){return colorAt(cells,x,z,1);}
    static int colorAt(int[] cells,int x,int z,int round){int offset=offset(round),size=boardSize(round),cell=cellSize(round);x-=offset;z-=offset;if(x<0||z<0||x>=size||z>=size)return -1;return cells[(z/cell)*8+x/cell];}
    static long warningMillis(int round){return Math.max(2500,8000L-400L*Math.max(0,round-1));}
}

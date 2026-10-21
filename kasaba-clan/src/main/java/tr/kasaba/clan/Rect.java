package tr.kasaba.clan;

record Rect(int minX,int minZ,int maxX,int maxZ) {
    Rect {
        if(minX>maxX||minZ>maxZ||Math.abs((long)minX)>30_000_100||Math.abs((long)minZ)>30_000_100||Math.abs((long)maxX)>30_000_100||Math.abs((long)maxZ)>30_000_100)
            throw new IllegalArgumentException("Geçersiz arazi sınırı.");
    }
    static Rect between(int x1,int z1,int x2,int z2){return new Rect(Math.min(x1,x2),Math.min(z1,z2),Math.max(x1,x2),Math.max(z1,z2));}
    static Rect warp(double x,double z){int bx=(int)Math.floor(x),bz=(int)Math.floor(z);return new Rect(bx-50,bz-50,bx+49,bz+49);}
    long width(){return (long)maxX-minX+1;}
    long depth(){return (long)maxZ-minZ+1;}
    long area(){return width()*depth();}
    boolean contains(int x,int z){return x>=minX&&x<=maxX&&z>=minZ&&z<=maxZ;}
    boolean overlaps(Rect other){return minX<=other.maxX&&maxX>=other.minX&&minZ<=other.maxZ&&maxZ>=other.minZ;}
}

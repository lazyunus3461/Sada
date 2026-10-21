package tr.kasaba.warp;

record WarpPoint(double x,double y,double z,float yaw,float pitch) {
    WarpPoint {
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)
                ||!Float.isFinite(yaw)||!Float.isFinite(pitch)
                ||Math.abs(x)>30_000_000||Math.abs(z)>30_000_000||Math.abs(pitch)>90)
            throw new IllegalArgumentException("Geçersiz warp konumu.");
    }
    boolean samePosition(WarpPoint other){return Math.abs(x-other.x)<0.000001&&Math.abs(y-other.y)<0.000001&&Math.abs(z-other.z)<0.000001;}
}

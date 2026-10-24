package tr.kasabatr.yaris;

/** The uploaded oval consists of two 90-block straights and two elliptical bends. */
final class CourseGeometry {
    record Position(double lateral,double speedScale){}
    static Position at(double centerX,double x,double z){
        double local=x-centerX;
        if(Math.abs(local)<=45)return new Position(Math.abs(z)-55,1);
        double dx=local-Math.copySign(45,local),dz=z;
        double radius=Math.sqrt(dx*dx/(57*57)+dz*dz/(55*55));
        return new Position((radius-1)*55,Math.clamp(radius,0.75,1.25));
    }
}

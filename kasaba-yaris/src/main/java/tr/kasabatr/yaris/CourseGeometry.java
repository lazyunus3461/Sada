package tr.kasabatr.yaris;

/** The uploaded oval consists of two 90-block straights and two elliptical bends. */
final class CourseGeometry {
    record Position(double lateral,double speedScale){}
    static Position at(double centerX,double x,double z){
        return at(centerX,45,57,55,x,z);
    }
    static Position at(RaceMap.Course course,double x,double z){double right=course.checkpoints().get(0).x(),left=course.checkpoints().get(3).x(),middle=(right+left)/2;return at(middle,(right-left)/2,course.checkpoints().get(1).x()-right,Math.abs(course.checkpoints().get(0).z()),x,z);}
    static Position at(double centerX,double straight,double rx,double rz,double x,double z){
        double local=x-centerX;
        if(Math.abs(local)<=straight)return new Position(Math.abs(z)-rz,1);
        double dx=local-Math.copySign(straight,local),dz=z;
        double radius=Math.sqrt(dx*dx/(rx*rx)+dz*dz/(rz*rz));
        return new Position((radius-1)*rz,Math.clamp(radius,0.75,1.25));
    }
}

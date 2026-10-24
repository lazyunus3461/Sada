package tr.kasabatr.yaris;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CourseGeometryTest {
 @Test void bothStraightsAndBothBendsShareTheSameCenterline(){
  for(double center:new double[]{190,-190}){
   for(double x:new double[]{center-40,center,center+40})for(double z:new double[]{-55,55})assertEquals(0,CourseGeometry.at(center,x,z).lateral(),0.0001);
   for(double x:new double[]{center-102,center+102})assertEquals(0,CourseGeometry.at(center,x,0).lateral(),0.0001);
  }
 }
 @Test void bendSpeedCompensatesTheShorterInnerPath(){
  double inner=CourseGeometry.at(190,235+57*.9,0).speedScale();
  double outer=CourseGeometry.at(190,235+57*1.1,0).speedScale();
  assertEquals(inner/(57*.9),outer/(57*1.1),0.0001);
  assertTrue(Math.abs(CourseGeometry.at(190,270,0).lateral())>12.5);
  assertTrue(Math.abs(CourseGeometry.at(190,190,64).lateral())>7);
 }
 @Test void widenedCoursesHaveContinuousBendsAndASeparatedInterior(){
  for(double center:new double[]{-370,370}){
   for(double theta=0;theta<Math.PI*2;theta+=.05){double bend=center+(Math.cos(theta)>=0?90:-90);double x=bend+Math.cos(theta)*80,z=Math.sin(theta)*80;assertEquals(0,CourseGeometry.at(center,90,80,80,x,z).lateral(),.00001);}
   assertTrue(Math.abs(CourseGeometry.at(center,90,80,80,center,0).lateral())>8);
  }
 }
}

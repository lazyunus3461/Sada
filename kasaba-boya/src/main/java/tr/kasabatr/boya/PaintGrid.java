package tr.kasabatr.boya;
import java.util.*;
/** Counts distinct owned cells; repainting transfers area rather than accumulating hit points. */
final class PaintGrid {
 final Set<PaintMap.Tile> allowed;final Map<PaintMap.Tile,PaintTeam> owners=new HashMap<>();final EnumMap<PaintTeam,Integer> counts=new EnumMap<>(PaintTeam.class);
 PaintGrid(Set<PaintMap.Tile> allowed){this.allowed=Set.copyOf(allowed);for(PaintTeam team:PaintTeam.values())counts.put(team,0);}
 List<PaintMap.Tile> splash(PaintMap.Tile centre,PaintTeam team,int radius){List<PaintMap.Tile> changed=new ArrayList<>();for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){if(Math.abs(x)+Math.abs(z)>radius)continue;PaintMap.Tile tile=new PaintMap.Tile(centre.x()+x,centre.y(),centre.z()+z);if(!allowed.contains(tile)||owners.get(tile)==team)continue;PaintTeam old=owners.put(tile,team);if(old!=null)counts.put(old,counts.get(old)-1);counts.put(team,counts.get(team)+1);changed.add(tile);}return List.copyOf(changed);}
 int count(PaintTeam team){return counts.get(team);}
 PaintTeam winner(){int red=count(PaintTeam.RED),blue=count(PaintTeam.BLUE);return red==blue?null:red>blue?PaintTeam.RED:PaintTeam.BLUE;}
}

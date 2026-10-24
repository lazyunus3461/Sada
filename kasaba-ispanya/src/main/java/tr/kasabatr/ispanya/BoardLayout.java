package tr.kasabatr.ispanya;

import java.util.*;

/** Per-match layout; links join adjacent rows, so every shortcut has a real buildable bridge. */
final class BoardLayout {
    record Node(double x,double z,String kind){}
    record Link(int from,int to){}
    final List<String> kinds;final Map<Integer,Integer> shortcuts;
    private BoardLayout(List<String> kinds,Map<Integer,Integer> shortcuts){this.kinds=List.copyOf(kinds);this.shortcuts=Map.copyOf(shortcuts);}
    static BoardLayout create(List<Node> nodes,int bridgeCount,Random random){
        var candidates=new ArrayList<Link>();
        for(int from=1;from<nodes.size()-2;from++)for(int to=from+12;to<nodes.size()-1;to++){
            Node a=nodes.get(from),b=nodes.get(to);if(Math.abs(a.x-b.x)<.01&&Math.abs(a.x)<=32&&Math.abs(Math.abs(a.z-b.z)-10)<.01)candidates.add(new Link(from,to));
        }
        var links=new LinkedHashMap<Integer,Integer>();var used=new HashSet<Integer>();
        Collections.shuffle(candidates,random);
        for(Link link:candidates){if(links.size()==bridgeCount)break;if(used.contains(link.from)||used.contains(link.to))continue;Node a=nodes.get(link.from),b=nodes.get(link.to);boolean overlap=links.entrySet().stream().anyMatch(e->{Node c=nodes.get(e.getKey()),d=nodes.get(e.getValue());return Math.abs(a.x-c.x)<5&&Math.max(Math.min(a.z,b.z),Math.min(c.z,d.z))<Math.min(Math.max(a.z,b.z),Math.max(c.z,d.z));});if(overlap)continue;links.put(link.from,link.to);used.add(link.from);used.add(link.to);}
        if(links.size()!=bridgeCount)throw new IllegalArgumentException("Rastgele kestirmeler için yeterli komşu sıra yok.");
        var kinds=new ArrayList<>(Collections.nCopies(nodes.size(),"plain"));kinds.set(0,"start");kinds.set(nodes.size()-1,"finish");links.keySet().forEach(i->kinds.set(i,"yellow"));
        var eligible=new ArrayList<Integer>();for(int i=1;i<nodes.size()-1;i++)if(!used.contains(i))eligible.add(i);Collections.shuffle(eligible,random);
        int cursor=0;for(String kind:List.of("red","green","purple","black")){long count=nodes.stream().filter(n->n.kind.equals(kind)).count();for(int i=0;i<count;i++)kinds.set(eligible.get(cursor++),kind);}
        return new BoardLayout(kinds,links);
    }
}

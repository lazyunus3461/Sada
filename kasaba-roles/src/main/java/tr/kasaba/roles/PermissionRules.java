package tr.kasaba.roles;

import java.util.*;

final class PermissionRules {
    /** Wildcards are expanded for Bukkit-registered nodes; exact nodes also work unregistered. */
    static Map<String,Boolean> expand(Map<String,Boolean> rules,Collection<String> registered) {
        Map<String,Boolean> result=new HashMap<>();
        for(String name:registered) {
            String node=name.toLowerCase(Locale.ROOT);
            if(node.contains("*"))continue;
            int best=-1;Boolean value=null;
            for(var rule:rules.entrySet()) {
                String pattern=rule.getKey();int score=-1;
                if(pattern.equals(node))score=10_000+pattern.length();
                else if(pattern.equals("*"))score=0;
                else if(pattern.endsWith(".*")&&node.startsWith(pattern.substring(0,pattern.length()-1)))score=pattern.length();
                if(score>best){best=score;value=rule.getValue();}
            }
            if(value!=null)result.put(node,value);
        }
        rules.forEach((node,value)->{if(!node.contains("*"))result.put(node,value);});
        Map<String,Boolean> ordered=new LinkedHashMap<>();
        result.keySet().stream().sorted(Comparator.comparingInt(String::length).thenComparing(String::compareTo))
                .forEach(k->ordered.put(k,result.get(k)));
        return ordered;
    }
}

package tr.kasaba.roles;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

/** Changes only our own teams on existing scoreboards; never replaces a player's board. */
final class NameTags {
    private static final class BoardState {
        final Map<UUID,Team> teams=new HashMap<>();
        final Set<UUID> conflicts=new HashSet<>();
    }
    private final Map<Scoreboard,BoardState> boards=new IdentityHashMap<>();
    private final Logger logger;
    NameTags(Logger logger){this.logger=logger;}

    void refresh(Collection<? extends Player> players,Function<String,Role> roles) {
        refresh(players,roles,name->Component.empty());
    }
    void refresh(Collection<? extends Player> players,Function<String,Role> roles,Function<String,Component> clans) {
        Set<Scoreboard> active=Collections.newSetFromMap(new IdentityHashMap<>());
        active.add(Bukkit.getScoreboardManager().getMainScoreboard());
        for(Player viewer:players)active.add(viewer.getScoreboard());
        for(var iterator=boards.entrySet().iterator();iterator.hasNext();) {
            var entry=iterator.next();
            if(!active.contains(entry.getKey())){clear(entry.getValue());iterator.remove();}
        }
        Set<UUID> online=new HashSet<>();for(Player player:players)online.add(player.getUniqueId());
        for(Scoreboard board:active) {
            BoardState state=boards.computeIfAbsent(board,key->new BoardState());
            for(UUID id:new HashSet<>(state.teams.keySet()))if(!online.contains(id))remove(state,id);
            state.conflicts.retainAll(online);
            for(Player player:players)apply(board,state,player,roles.apply(player.getName()),clans.apply(player.getName()));
        }
    }
    private void apply(Scoreboard board,BoardState state,Player player,Role role,Component clan) {
        UUID id=player.getUniqueId();Team team=state.teams.get(id);
        // Another plugin may unregister a team; drop stale handles before rebuilding.
        if(team!=null&&!team.equals(board.getTeam(team.getName()))){state.teams.remove(id);team=null;}
        Team current=board.getEntryTeam(player.getName());
        if(current!=null&&!current.equals(team)){conflict(state,id,player.getName());return;}
        if(team==null) {
            String name="kr"+id.toString().replace("-","").substring(0,14);
            if(board.getTeam(name)!=null){conflict(state,id,player.getName());return;}
            team=board.registerNewTeam(name);state.teams.put(id,team);
        }
        Component prefix=Component.empty().append(Component.text("["+role.label()+"] ",KasabaRoles.color(role.color())).decorate(TextDecoration.BOLD)).append(clan);
        if(!team.prefix().equals(prefix))team.prefix(prefix);
        NamedTextColor nameColor=NamedTextColor.nearestTo(KasabaRoles.color(role.nameColor()));
        if(!team.hasColor()||!nameColor.equals(team.color()))team.color(nameColor);
        if(team.getOption(Team.Option.NAME_TAG_VISIBILITY)!=Team.OptionStatus.ALWAYS)
            team.setOption(Team.Option.NAME_TAG_VISIBILITY,Team.OptionStatus.ALWAYS);
        if(!team.hasEntry(player.getName()))team.addEntry(player.getName());
        state.conflicts.remove(id);
    }
    private void conflict(BoardState state,UUID id,String name) {
        if(state.conflicts.add(id))logger.warning("Kafa üstü etiketi atlandı; başka bir eklentinin takımı var: "+name);
    }
    void remove(UUID id){for(BoardState state:boards.values()){remove(state,id);state.conflicts.remove(id);}}
    private void remove(BoardState state,UUID id){
        Team team=state.teams.remove(id);if(team!=null)try{team.unregister();}catch(IllegalStateException ignored){}
    }
    private void clear(BoardState state){for(UUID id:new HashSet<>(state.teams.keySet()))remove(state,id);state.conflicts.clear();}
    void clear(){for(BoardState state:boards.values())clear(state);boards.clear();}
}

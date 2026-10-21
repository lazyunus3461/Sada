package tr.kasaba.test;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import java.nio.charset.StandardCharsets;

/** Local integration fixture. Never included in the distributable plugin. */
public final class RolesProbe extends JavaPlugin {
    @Override public void onEnable() {
        if(!getServer().getIp().equals("127.0.0.1"))throw new IllegalStateException("Probe requires loopback binding");
        for(String node:new String[]{"kasaba.test.use","kasaba.test.denied"})
            getServer().getPluginManager().addPermission(new Permission(node,PermissionDefault.FALSE));
        getServer().getMessenger().registerIncomingPluginChannel(this,"kasaba_test:roles",(channel,player,data)->{
            if(data.length>512)throw new IllegalArgumentException("Oversized test request");
            String request=new String(data,StandardCharsets.UTF_8);
            if(request.startsWith("COMMAND|rol ")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),request.substring(8));
                player.sendMessage(Component.text("roles-probe:command-done"));
            } else if(request.equals("BOARD|CUSTOM")) {
                var board=Bukkit.getScoreboardManager().getNewScoreboard();
                var objective=board.registerNewObjective("kasaba_probe",Criteria.DUMMY,Component.text("Preserve sidebar"));
                objective.setDisplaySlot(DisplaySlot.SIDEBAR);objective.getScore("keep").setScore(1);
                player.setScoreboard(board);player.sendMessage(Component.text("roles-probe:custom-board"));
            } else if(request.equals("BOARD|CHECK")) {
                boolean intact=player.getScoreboard().getObjective("kasaba_probe")!=null
                        &&player.getScoreboard().getObjective(DisplaySlot.SIDEBAR)!=null;
                player.sendMessage(Component.text("roles-probe:sidebar="+intact));
            } else if(request.startsWith("CHECK|")) {
                String node=request.substring(6);
                player.sendMessage(Component.text("roles-probe:"+node+"="+player.hasPermission(node)));
            } else throw new IllegalArgumentException("Unknown test request");
        });
    }
}

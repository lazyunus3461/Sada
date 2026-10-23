package tr.kasabatr.vampir;

import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.configuration.file.FileConfiguration;

/** Migrate only the distributed world names; preserve custom worlds and positions. */
final class WorldNames {
    static final String CURRENT = "vampirkoylu";
    static final String CLEAN = "vampirkoylu_bos";
    static final String VILLAGE = "vampirkoylu_sokaklar";
    static boolean managed(String name){return CURRENT.equals(name)||LEGACY.equals(name)||CLEAN.equals(name)||VILLAGE.equals(name);}
    static boolean separateLobby(FileConfiguration config){double x=config.getDouble("lobby.x",144.5),z=config.getDouble("lobby.z",.5),radius=Math.clamp(config.getInt("lobby.radius",31),1,128)+16;return Double.isFinite(x)&&Double.isFinite(z)&&(x-radius>80||x+radius< -80||z-radius>80||z+radius< -80);}
    static boolean toVillage(FileConfiguration config){boolean changed=false;if(managed(config.getString("template-world"))&&!VILLAGE.equals(config.getString("template-world"))){config.set("template-world",VILLAGE);changed=true;}if(managed(config.getString("lobby.world"))&&!VILLAGE.equals(config.getString("lobby.world"))&&separateLobby(config)){config.set("lobby.world",VILLAGE);changed=true;}return changed;}
    static boolean toClean(FileConfiguration config){boolean changed=false;if(managed(config.getString("template-world"))&&!CLEAN.equals(config.getString("template-world"))){config.set("template-world",CLEAN);changed=true;}if(managed(config.getString("lobby.world"))&&!CLEAN.equals(config.getString("lobby.world"))){config.set("lobby.world",CLEAN);changed=true;}return changed;}
    static final String LEGACY = "KasabaTR-VampirKoylu-TamDunya";

    private WorldNames() {}

    static boolean migrate(FileConfiguration config, Path worlds) {
        String template = config.getString("template-world", "");
        String selected = template;
        if (LEGACY.equals(template) && present(worlds, CURRENT)) {
            selected = CURRENT;
        } else if (CURRENT.equals(template) && !present(worlds, CURRENT) && present(worlds, LEGACY)) {
            selected = LEGACY;
        }
        boolean changed = !selected.equals(template);
        if (changed) config.set("template-world", selected);
        String lobby = config.getString("lobby.world", "");
        if ((CURRENT.equals(selected) || LEGACY.equals(selected)) && present(worlds, selected)
                && (CURRENT.equals(lobby) || LEGACY.equals(lobby)) && !selected.equals(lobby)) {
            config.set("lobby.world", selected);
            changed = true;
        }
        return changed;
    }

    private static boolean present(Path worlds, String name) {
        return Files.isRegularFile(worlds.resolve(name).resolve("level.dat"));
    }
}

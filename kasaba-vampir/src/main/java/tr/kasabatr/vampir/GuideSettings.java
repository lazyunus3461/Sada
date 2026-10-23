package tr.kasabatr.vampir;
import org.bukkit.configuration.file.FileConfiguration;
final class GuideSettings {
    private GuideSettings(){}
    static boolean migrate(FileConfiguration config){if(config.contains("guide-settings-version",true))return false;config.set("game.day-seconds",Math.max(180,config.getInt("game.day-seconds",120)));config.set("game.night-choice-seconds",Math.max(40,config.getInt("game.night-choice-seconds",20)));config.set("guide-settings-version",1);return true;}
}

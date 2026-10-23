package tr.kasabatr.vampir;
import org.bukkit.configuration.file.FileConfiguration;
final class GuideSettings {
    private GuideSettings(){}
    static boolean migrate(FileConfiguration config){
        int version=config.contains("guide-settings-version",true)?config.getInt("guide-settings-version"):0;if(version>=2)return false;
        if(version<1){config.set("game.day-seconds",Math.max(180,config.getInt("game.day-seconds",120)));config.set("game.night-choice-seconds",Math.max(40,config.getInt("game.night-choice-seconds",20)));}
        config.set("game.day-vampire-cooldown-seconds",40);
        config.set("game.day-vampire-kills-per-player",null);
        config.set("guide-settings-version",2);return true;
    }
}

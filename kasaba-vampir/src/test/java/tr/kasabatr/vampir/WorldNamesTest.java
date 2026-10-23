package tr.kasabatr.vampir;

import java.nio.file.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WorldNamesTest {
    @TempDir Path worlds;
    private YamlConfiguration config(String template,String lobby){var c=new YamlConfiguration();c.set("template-world",template);c.set("lobby.world",lobby);c.set("lobby.x",222.5);c.set("game.day-seconds",137);return c;}
    private void world(String name)throws Exception{Files.createDirectories(worlds.resolve(name));Files.createFile(worlds.resolve(name).resolve("level.dat"));}
    @Test void oldSettingsFindRenamedWorldAndPreserveOtherSettings()throws Exception{world(WorldNames.CURRENT);var c=config(WorldNames.LEGACY,WorldNames.LEGACY);assertTrue(WorldNames.migrate(c,worlds));assertEquals(WorldNames.CURRENT,c.getString("template-world"));assertEquals(WorldNames.CURRENT,c.getString("lobby.world"));assertEquals(222.5,c.getDouble("lobby.x"));assertEquals(137,c.getInt("game.day-seconds"));}
    @Test void originalInstallKeepsWorking()throws Exception{world(WorldNames.LEGACY);var c=config(WorldNames.LEGACY,WorldNames.LEGACY);assertFalse(WorldNames.migrate(c,worlds));assertEquals(WorldNames.LEGACY,c.getString("template-world"));}
    @Test void renamedWorldWinsWhenBothFoldersExist()throws Exception{world(WorldNames.LEGACY);world(WorldNames.CURRENT);var c=config(WorldNames.LEGACY,WorldNames.LEGACY);assertTrue(WorldNames.migrate(c,worlds));assertEquals(WorldNames.CURRENT,c.getString("template-world"));}
    @Test void freshJarSupportsOriginalMapFolder()throws Exception{world(WorldNames.LEGACY);var c=config(WorldNames.CURRENT,WorldNames.CURRENT);assertTrue(WorldNames.migrate(c,worlds));assertEquals(WorldNames.LEGACY,c.getString("template-world"));assertEquals(WorldNames.LEGACY,c.getString("lobby.world"));}
    @Test void customWorldsArePreserved()throws Exception{world(WorldNames.CURRENT);var c=config("ozel-harita","ozel-lobi");assertFalse(WorldNames.migrate(c,worlds));assertEquals("ozel-harita",c.getString("template-world"));assertEquals("ozel-lobi",c.getString("lobby.world"));}
    @Test void separateLobbyIsPreserved()throws Exception{world(WorldNames.CURRENT);var c=config(WorldNames.LEGACY,"ozel-lobi");assertTrue(WorldNames.migrate(c,worlds));assertEquals("ozel-lobi",c.getString("lobby.world"));}
    @Test void oldLobbyNameCanBeRepairedIndependently()throws Exception{world(WorldNames.CURRENT);var c=config(WorldNames.CURRENT,WorldNames.LEGACY);assertTrue(WorldNames.migrate(c,worlds));assertEquals(WorldNames.CURRENT,c.getString("lobby.world"));assertFalse(WorldNames.migrate(c,worlds));}
    @Test void emptyOrMissingMapIsNotAccepted()throws Exception{Files.createDirectory(worlds.resolve(WorldNames.CURRENT));var c=config(WorldNames.LEGACY,WorldNames.LEGACY);assertFalse(WorldNames.migrate(c,worlds));assertEquals(WorldNames.LEGACY,c.getString("template-world"));}
    @Test void threeVillagesMigratesManagedWorldToPreparedSharedLobby(){var c=config(WorldNames.CLEAN,WorldNames.CLEAN);assertTrue(WorldNames.toVillage(c));assertEquals(WorldNames.VILLAGE,c.getString("template-world"));assertEquals(WorldNames.VILLAGE,c.getString("lobby.world"));assertEquals(144.5,c.getDouble("lobby.x"));assertEquals(137,c.getInt("game.day-seconds"));assertFalse(WorldNames.toVillage(c));}
    @Test void enclosedVillageDoesNotMoveACustomLobbyOrCustomMap(){var c=config(WorldNames.CLEAN,"ozel-lobi");WorldNames.toVillage(c);assertEquals("ozel-lobi",c.getString("lobby.world"));var custom=config("ozel-harita","ozel-lobi");assertFalse(WorldNames.toVillage(custom));}
    @Test void aLobbyInsideTheOldGameAreaMovesToTheNewSharedLobby(){var c=config(WorldNames.CLEAN,WorldNames.CLEAN);c.set("lobby.x",0);assertTrue(WorldNames.toVillage(c));assertEquals(WorldNames.VILLAGE,c.getString("template-world"));assertEquals(WorldNames.VILLAGE,c.getString("lobby.world"));assertEquals(144.5,c.getDouble("lobby.x"));}
    @Test void anAdminLobbyAlreadyInTheNewWorldIsNotReset(){var c=config(WorldNames.VILLAGE,WorldNames.VILLAGE);assertFalse(WorldNames.toVillage(c));assertEquals(222.5,c.getDouble("lobby.x"));}
    @Test void thePreviousSingleVillageMigratesToThreeAreas(){var c=config(WorldNames.OLD_VILLAGE,WorldNames.OLD_VILLAGE);assertTrue(WorldNames.toVillage(c));assertEquals(WorldNames.VILLAGE,c.getString("template-world"));assertEquals(WorldNames.VILLAGE,c.getString("lobby.world"));}
    @Test void aConfirmedMultiverseBindingSurvivesTheLegacyNameMigration(){var c=config(WorldNames.CURRENT,WorldNames.CURRENT);c.set("world-link.world",WorldNames.CURRENT);assertFalse(WorldNames.toVillage(c));assertEquals(WorldNames.CURRENT,c.getString("template-world"));assertEquals(WorldNames.CURRENT,c.getString("lobby.world"));}
}

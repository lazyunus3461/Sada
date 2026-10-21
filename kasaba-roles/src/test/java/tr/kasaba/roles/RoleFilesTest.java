package tr.kasaba.roles;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RoleFilesTest {
    @TempDir Path directory;
    YamlConfiguration defaults() throws Exception {
        var yaml=new YamlConfiguration();
        try(var in=new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/config.yml")),StandardCharsets.UTF_8)) {
            yaml.load(in);
        }
        return yaml;
    }
    @Test void defaultRolesIncludeRehberAndInheritance() throws Exception {
        var roles=RoleFiles.configuration(defaults()).catalog();
        assertNotNull(roles.get("rehber"));
        assertEquals(true,roles.permissions("moderator").get("kasabaroles.info"));
        assertEquals(true,roles.permissions("admin").get("minecraft.command.kick"));
    }
    @Test void assignmentSaveSurvivesReloadAndRoleRemovalReplacesOldData() throws Exception {
        Path file=directory.resolve("oyuncular.yml");
        var roles=RoleFiles.configuration(defaults()).catalog();
        RoleFiles.writeAssignments(file,Map.of("redkit","rehber","testplayer","vip"));
        assertEquals(Map.of("redkit","rehber","testplayer","vip"),RoleFiles.assignments(RoleFiles.read(file),roles));
        RoleFiles.writeAssignments(file,Map.of("redkit","kurucu"));
        assertEquals(Map.of("redkit","kurucu"),RoleFiles.assignments(RoleFiles.read(file),roles));
        try(var files=Files.list(directory)){assertEquals(List.of(file),files.toList());}
    }
    @Test void brokenYamlIsRejectedWithoutReplacingSource() throws Exception {
        Path file=directory.resolve("config.yml");String broken="roles: [\n";
        Files.writeString(file,broken);
        assertThrows(IOException.class,()->RoleFiles.read(file));
        assertEquals(broken,Files.readString(file));
    }
    @Test void unknownOrNonStringAssignedRolesAreRejected() throws Exception {
        var roles=RoleFiles.configuration(defaults()).catalog();
        var yaml=new YamlConfiguration();yaml.set("players.Redkit","missing");
        assertThrows(IllegalArgumentException.class,()->RoleFiles.assignments(yaml,roles));
        yaml.set("players.Redkit",List.of("vip"));
        assertThrows(IllegalArgumentException.class,()->RoleFiles.assignments(yaml,roles));
    }
    @Test void malformedPermissionOrColorIsRejected() throws Exception {
        var yaml=defaults();yaml.set("roles.vip.permissions.example.use","yes");
        assertThrows(IllegalArgumentException.class,()->RoleFiles.configuration(yaml));
        var invalidColor=defaults();invalidColor.set("roles.vip.color","invalid");
        assertThrows(IllegalArgumentException.class,()->RoleFiles.configuration(invalidColor));
    }
    @Test void malformedContainersCannotSilentlyRemovePermissionsOrRoles() throws Exception {
        var yaml=defaults();yaml.set("roles.vip.permissions",List.of("example.use"));
        assertThrows(IllegalArgumentException.class,()->RoleFiles.configuration(yaml));
        var players=new YamlConfiguration();players.set("players",List.of("Redkit"));
        var roles=RoleFiles.configuration(defaults()).catalog();
        assertThrows(IllegalArgumentException.class,()->RoleFiles.assignments(players,roles));
    }
    @Test void samePlayerWithDifferentCaseCannotHaveTwoAssignments() throws Exception {
        var yaml=new YamlConfiguration();yaml.set("players.Redkit","vip");yaml.set("players.REDKIT","kurucu");
        var roles=RoleFiles.configuration(defaults()).catalog();
        assertThrows(IllegalArgumentException.class,()->RoleFiles.assignments(yaml,roles));
    }
}

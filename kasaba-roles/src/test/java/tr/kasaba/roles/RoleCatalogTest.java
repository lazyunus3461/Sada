package tr.kasaba.roles;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RoleCatalogTest {
    Role role(String id, List<String> parents, Map<String, Boolean> permissions) {
        return new Role(id,id,"gray","white",1,parents,permissions);
    }
    @Test void inheritedPermissionsCanBeExplicitlyDenied() {
        var base=role("oyuncu",List.of(),Map.of("example.use",true));
        var vip=role("vip",List.of("oyuncu"),Map.of("example.use",false,"example.vip",true));
        var catalog=new RoleCatalog(Map.of("oyuncu",base,"vip",vip),"oyuncu");
        assertEquals(false,catalog.permissions("vip").get("example.use"));
        assertEquals(true,catalog.permissions("vip").get("example.vip"));
        assertEquals(true,catalog.permissions("oyuncu").get("example.use"));
    }
    @Test void rejectsInheritanceCycles() {
        var a=role("a",List.of("b"),Map.of());var b=role("b",List.of("a"),Map.of());
        assertThrows(IllegalArgumentException.class,()->new RoleCatalog(Map.of("a",a,"b",b),"a"));
    }
    @Test void rejectsMissingParent() {
        var a=role("a",List.of("missing"),Map.of());
        assertThrows(IllegalArgumentException.class,()->new RoleCatalog(Map.of("a",a),"a"));
    }
    @Test void requiresExistingDefaultRole() {
        assertThrows(IllegalArgumentException.class,()->new RoleCatalog(Map.of(),"oyuncu"));
    }
    @Test void returnedPermissionsCannotModifyCatalog() {
        var a=role("a",List.of(),Map.of("example.use",true));var catalog=new RoleCatalog(Map.of("a",a),"a");
        assertThrows(UnsupportedOperationException.class,()->catalog.permissions("a").put("example.admin",true));
    }
    @Test void accountKeysMatchRegardlessOfCase() { assertEquals(Role.playerKey("Redkit"),Role.playerKey("REDKIT")); }
    @Test void rejectsInvalidIdsAndPlayers() {
        for(String id:List.of("../a","a.b","", "a b"))assertThrows(IllegalArgumentException.class,()->Role.normalizeId(id));
        for(String id:List.of("../a","a.b","abcdefghijklmnopq"))assertThrows(IllegalArgumentException.class,()->Role.playerKey(id));
    }
}

package tr.kasaba.roles;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PermissionRulesTest {
    @Test void exactDenialOverridesBroadGrantAndMoreSpecificWildcardWins() {
        var result=PermissionRules.expand(Map.of("*",false,"minecraft.command.*",true,
                "minecraft.command.ban",false),List.of("minecraft.command.kick","minecraft.command.ban","other.use"));
        assertEquals(Map.of("minecraft.command.kick",true,"minecraft.command.ban",false,"other.use",false),result);
    }
    @Test void prefixDoesNotGrantSimilarSiblingAndExplicitUnregisteredNodeWorks() {
        var result=PermissionRules.expand(Map.of("example.*",true,"future.use",true),
                List.of("example.use","examples.use","example.*"));
        assertEquals(Map.of("example.use",true,"future.use",true),result);
    }
    @Test void explicitChildrenAreAppliedAfterParentNodes() {
        var result=PermissionRules.expand(Map.of("example",true,"example.use",false),List.of());
        assertEquals(List.of("example","example.use"),List.copyOf(result.keySet()));
    }
}

package tr.kasaba.roles;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RoleDisplayTest {
    Role role() { return new Role("kurucu","Kurucu","red","white",1000,List.of(),Map.of()); }
    @Test void chatContainsRoleNamePlayerAndOriginalMessage() {
        var message=Component.text("Merhaba",NamedTextColor.AQUA).append(Component.text(" Kasaba!"));
        var rendered=RoleDisplay.chat(role(),"Redkit",message);
        assertEquals("[Kurucu] Redkit » Merhaba Kasaba!",PlainTextComponentSerializer.plainText().serialize(rendered));
        assertEquals(message,rendered.children().getLast());
    }
    @Test void clanAppearsBesideRoleWithoutChangingMessageOrPlayerColor() {
        Component clan=Component.text("[KSB] ",NamedTextColor.AQUA);
        Component message=Component.text("Merhaba");
        Component rendered=RoleDisplay.chat(role(),"Redkit",message,clan);
        assertEquals("[Kurucu] [KSB] Redkit » Merhaba",PlainTextComponentSerializer.plainText().serialize(rendered));
        assertEquals(message,rendered.children().getLast());
    }
    @Test void boldRoleDoesNotMakePlayerNameOrMessageBold() {
        var rendered=RoleDisplay.chat(role(),"Redkit",Component.text("Merhaba"));
        var tag=rendered.children().getFirst();
        assertEquals(TextDecoration.State.TRUE,tag.children().getFirst().decoration(TextDecoration.BOLD));
        assertEquals(TextDecoration.State.NOT_SET,rendered.decoration(TextDecoration.BOLD));
        assertEquals(TextDecoration.State.NOT_SET,tag.children().getLast().decoration(TextDecoration.BOLD));
        assertEquals(TextDecoration.State.NOT_SET,rendered.children().getLast().decoration(TextDecoration.BOLD));
    }
}

package tr.kasaba.roles;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

final class RoleDisplay {
    static Component tag(Role role, String playerName) {
        return tag(role,playerName,Component.empty());
    }
    static Component tag(Role role,String playerName,Component clan) {
        return Component.empty()
                .append(Component.text("[" + role.label() + "] ", KasabaRoles.color(role.color())).decorate(TextDecoration.BOLD))
                .append(clan)
                .append(Component.text(playerName, KasabaRoles.color(role.nameColor())));
    }

    static Component chat(Role role, String playerName, Component message) {
        return Component.empty().append(tag(role, playerName))
                .append(Component.text(" » ", NamedTextColor.GRAY)).append((role.chatColor()==null?message:message.color(KasabaRoles.color(role.chatColor()))));
    }
    static Component chat(Role role,String playerName,Component message,Component clan) {
        return Component.empty().append(tag(role,playerName,clan))
                .append(Component.text(" » ",NamedTextColor.GRAY)).append((role.chatColor()==null?message:message.color(KasabaRoles.color(role.chatColor()))));
    }
}

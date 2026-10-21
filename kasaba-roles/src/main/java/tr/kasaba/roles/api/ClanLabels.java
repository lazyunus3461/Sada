package tr.kasaba.roles.api;

import net.kyori.adventure.text.Component;

/** Thread-safe, immutable-snapshot backed provider. Prefix includes its trailing space. */
public interface ClanLabels {
    Component prefix(String playerName);
}

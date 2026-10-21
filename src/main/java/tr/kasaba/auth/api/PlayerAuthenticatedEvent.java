package tr.kasaba.auth.api;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/** Emitted on the main thread only after successful credential verification and session cleanup. */
public final class PlayerAuthenticatedEvent extends PlayerEvent {
    private static final HandlerList HANDLERS=new HandlerList();
    private final boolean registration;
    public PlayerAuthenticatedEvent(Player player,boolean registration){super(player);this.registration=registration;}
    public boolean registration(){return registration;}
    @Override public HandlerList getHandlers(){return HANDLERS;}
    public static HandlerList getHandlerList(){return HANDLERS;}
}

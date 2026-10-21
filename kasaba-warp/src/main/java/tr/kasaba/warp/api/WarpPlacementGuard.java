package tr.kasaba.warp.api;

import java.util.List;
import java.util.Optional;

/** Main-thread validation before publishing or persisting a replacement warp snapshot. */
public interface WarpPlacementGuard {
    Optional<String> rejection(List<WarpDestination> next);
}

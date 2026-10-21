package tr.kasaba.warp.api;

import java.util.List;

/** Contains every saved point, including extra targets and private destinations. */
public interface WarpRegions {
    List<WarpDestination> destinations();
}

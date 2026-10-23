package tr.kasabatr.vampir;

import java.util.*;

final class MeetingOrder {
    private MeetingOrder() {}
    static List<UUID> create(Collection<UUID> members, Set<UUID> alive, UUID reporter) {
        List<UUID> order = new ArrayList<>();
        if (reporter != null && members.contains(reporter) && alive.contains(reporter)) order.add(reporter);
        for (UUID id : members) if (alive.contains(id) && !id.equals(reporter)) order.add(id);
        return List.copyOf(order);
    }
}

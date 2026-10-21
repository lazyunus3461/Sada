package tr.kasaba.roles;

import java.util.*;

final class RoleCatalog {
    private final Map<String, Role> roles;
    private final String defaultId;
    private final Map<String, Map<String, Boolean>> resolved = new HashMap<>();
    RoleCatalog(Map<String, Role> roles, String defaultId) {
        this.roles = Collections.unmodifiableMap(new LinkedHashMap<>(roles));
        this.defaultId = Role.normalizeId(defaultId);
        if (!this.roles.containsKey(this.defaultId)) throw new IllegalArgumentException("Varsayılan rol bulunamadı: " + defaultId);
        for (Role role : this.roles.values()) resolve(role.id(), new HashSet<>());
    }
    Role get(String id) { return roles.get(Role.normalizeId(id)); }
    Role defaultRole() { return roles.get(defaultId); }
    Collection<Role> all() { return roles.values(); }
    Map<String, Boolean> permissions(String id) {
        Map<String, Boolean> result = resolved.get(Role.normalizeId(id));
        if (result == null) throw new IllegalArgumentException("Rol bulunamadı: " + id);
        return result;
    }
    private Map<String, Boolean> resolve(String id, Set<String> visiting) {
        if (resolved.containsKey(id)) return resolved.get(id);
        Role role = roles.get(id);
        if (role == null) throw new IllegalArgumentException("Miras alınan rol bulunamadı: " + id);
        if (!visiting.add(id)) throw new IllegalArgumentException("Rollerde miras döngüsü var: " + id);
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (String parent : role.inherits()) result.putAll(resolve(Role.normalizeId(parent), visiting));
        result.putAll(role.permissions());
        visiting.remove(id);
        Map<String, Boolean> immutable = Collections.unmodifiableMap(result);
        resolved.put(id, immutable);
        return immutable;
    }
}

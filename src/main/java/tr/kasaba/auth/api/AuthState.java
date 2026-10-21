package tr.kasaba.auth.api;

import java.util.UUID;

/** Read-only authentication state. False while registering/logging in and after quit. */
public interface AuthState { boolean authenticated(UUID player); }

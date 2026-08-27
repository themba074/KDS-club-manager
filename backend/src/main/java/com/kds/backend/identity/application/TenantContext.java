package com.kds.backend.identity.application;

import org.springframework.security.access.AccessDeniedException;
import java.util.UUID;

/** Request-local context; populated only after JWT and membership validation. */
public final class TenantContext {
    private static final ThreadLocal<UUID> ACTIVE_CLUB = new ThreadLocal<>();
    private TenantContext() {}
    public static UUID requireClubId() {
        UUID clubId = ACTIVE_CLUB.get();
        if (clubId == null) throw new AccessDeniedException("Select a club before accessing club data.");
        return clubId;
    }
    public static void set(UUID clubId) { ACTIVE_CLUB.set(clubId); }
    public static void clear() { ACTIVE_CLUB.remove(); }
}

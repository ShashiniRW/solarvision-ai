package com.solarvision.backend.access;

import java.util.Set;

/**
 * Which projects a user may see: either all of them (administrators)
 * or only the listed ids.
 */
public record ProjectScope(boolean unrestricted, Set<Long> projectIds) {

    public static ProjectScope everything() {
        return new ProjectScope(true, Set.of());
    }

    public static ProjectScope only(Set<Long> projectIds) {
        return new ProjectScope(false, Set.copyOf(projectIds));
    }

    public boolean allows(Long projectId) {
        return unrestricted || projectIds.contains(projectId);
    }
}
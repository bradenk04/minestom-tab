package com.bradenkennedy.tab.api;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Defines visibility rules between an observer and a target player.
 */
public interface VisibilityGroup {
    /**
     * Checks if the observer should see the target player according to the observer's rules.
     *
     * @param observer The player who is observing.
     * @param target   The player who is being observed.
     * @return True if the target should be visible to the observer.
     */
    boolean canSee(@NotNull Player observer, @NotNull Player target);

    /**
     * Checks if the target player should be visible to the observer according to the target's rules.
     *
     * @param target   The player who is being observed.
     * @param observer The player who is observing.
     * @return True if the target allows themselves to be seen by the observer.
     */
    default boolean canBeSeenBy(@NotNull Player target, @NotNull Player observer) {
        return true;
    }

    /**
     * A simple visibility group that only allows seeing players in the same instance.
     */
    VisibilityGroup PER_INSTANCE = (observer, target) -> {
        var instance = observer.getInstance();
        return instance != null && instance == target.getInstance();
    };

    /**
     * A visibility group where everyone can see everyone.
     */
    VisibilityGroup GLOBAL = (observer, target) -> true;
}

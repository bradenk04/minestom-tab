package com.bradenkennedy.tab.api;

import net.minestom.server.entity.Player;
import com.bradenkennedy.tab.TabVisibilityManager;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for common visibility groups.
 */
public final class VisibilityGroups {
    private VisibilityGroups() {}

    /**
     * Creates a spectator visibility group.
     * Spectators can see everyone.
     * Non-spectators cannot see spectators.
     * Non-spectators can only see other non-spectators in the same instance.
     *
     * @param manager The visibility manager.
     * @return The spectator visibility group.
     */
    public static VisibilityGroup spectator(@NotNull TabVisibilityManager manager) {
        return new SpectatorGroup(manager);
    }

    private record SpectatorGroup(TabVisibilityManager manager) implements VisibilityGroup {

        @Override
            public boolean canSee(@NotNull Player observer, @NotNull Player target) {
                return observer.getInstance() == target.getInstance();
            }

            @Override
            public boolean canBeSeenBy(@NotNull Player target, @NotNull Player observer) {
                return manager.getGroup(observer) instanceof SpectatorGroup;
            }
        }
}

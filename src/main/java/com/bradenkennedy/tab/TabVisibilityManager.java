package com.bradenkennedy.tab;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerPacketOutEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.PlayerInfoRemovePacket;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.server.entity.EntityType;
import net.minestom.server.coordinate.Vec;
import com.bradenkennedy.tab.api.VisibilityGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player visibility in both the world and the tablist.
 */
public class TabVisibilityManager {
    private final Map<Player, VisibilityGroup> playerGroups = new ConcurrentHashMap<>();
    private final VisibilityGroup defaultGroup;
    private final ThreadLocal<Boolean> filtering = ThreadLocal.withInitial(() -> false);

    public TabVisibilityManager(@NotNull EventNode<Event> eventNode) {
        this(eventNode, VisibilityGroup.PER_INSTANCE);
    }

    public TabVisibilityManager(@NotNull EventNode<Event> eventNode, @NotNull VisibilityGroup defaultGroup) {
        this.defaultGroup = defaultGroup;

        //noinspection UnstableApiUsage
        eventNode.addListener(PlayerPacketOutEvent.class, this::handlePacketOut);
        eventNode.addListener(AddEntityToInstanceEvent.class, this::handleInstanceAdd);
        eventNode.addListener(PlayerSpawnEvent.class, this::handleSpawn);
        eventNode.addListener(PlayerDisconnectEvent.class, this::handleDisconnect);
    }

    public void setGroup(@NotNull Player player, @NotNull VisibilityGroup group) {
        playerGroups.put(player, group);
        refresh(player);
    }

    public @NotNull VisibilityGroup getGroup(@NotNull Player player) {
        return playerGroups.getOrDefault(player, defaultGroup);
    }

    public boolean canSee(@NotNull Player observer, @NotNull Player target) {
        if (observer == target) return true;
        return getGroup(observer).canSee(observer, target) && getGroup(target).canBeSeenBy(target, observer);
    }

    public void refresh(@NotNull Player player) {
        filtering.set(true);
        try {
            refreshForEveryone(player);

            player.updateViewableRule(observer -> canSee(observer, player));
            player.updateViewerRule(entity -> {
                if (entity instanceof Player target) {
                    return canSee(player, target);
                }
                return true;
            });

            for (Player online : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                if (online == player) {
                    for (Player other : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                        if (other == player) continue;
                        if (canSee(player, other)) {
                            player.sendPacket(createAddPacket(other));
                            player.sendPacket(new SpawnEntityPacket(
                                    other.getEntityId(),
                                    other.getUuid(),
                                    EntityType.PLAYER,
                                    other.getPosition(),
                                    other.getPosition().yaw(),
                                    0,
                                    Vec.ZERO
                            ));
                        }
                    }
                    continue;
                }

                if (canSee(online, player)) {
                    online.sendPacket(new SpawnEntityPacket(
                            player.getEntityId(),
                            player.getUuid(),
                            EntityType.PLAYER,
                            player.getPosition(),
                            player.getPosition().yaw(),
                            0,
                            Vec.ZERO
                    ));
                }
            }
        } finally {
            filtering.set(false);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private void handlePacketOut(PlayerPacketOutEvent event) {
        if (filtering.get()) return;

        ServerPacket packet = event.getPacket();
        if (packet instanceof PlayerInfoUpdatePacket updatePacket) {
            handleUpdatePacket(event, updatePacket);
        } else if (packet instanceof SpawnEntityPacket spawnPacket) {
            handleSpawnPacket(event, spawnPacket);
        }
    }

    private void handleSpawnPacket(PlayerPacketOutEvent event, SpawnEntityPacket packet) {
        if (packet.type() != EntityType.PLAYER) return;

        Player observer = event.getPlayer();
        Player target = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(packet.uuid());

        if (target != null && !canSee(observer, target)) {
            event.setCancelled(true);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private void handleUpdatePacket(PlayerPacketOutEvent event, PlayerInfoUpdatePacket packet) {
        Player observer = event.getPlayer();
        List<PlayerInfoUpdatePacket.Entry> entries = packet.entries();
        List<PlayerInfoUpdatePacket.Entry> filteredEntries = new ArrayList<>(entries.size());
        boolean changed = false;

        for (PlayerInfoUpdatePacket.Entry entry : entries) {
            Player target = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(entry.uuid());
            if (target == null || canSee(observer, target)) {
                filteredEntries.add(entry);
            } else {
                changed = true;
            }
        }

        if (changed) {
            event.setCancelled(true);
            if (!filteredEntries.isEmpty()) {
                resendPacket(observer, new PlayerInfoUpdatePacket(packet.actions(), filteredEntries));
            }
        }
    }

    private void resendPacket(Player player, ServerPacket packet) {
        filtering.set(true);
        try {
            player.sendPacket(packet);
        } finally {
            filtering.set(false);
        }
    }

    private void handleInstanceAdd(AddEntityToInstanceEvent event) {
        if (event.getEntity() instanceof Player player) {
            MinecraftServer.getSchedulerManager().buildTask(() -> refresh(player))
                    .delay(net.minestom.server.timer.TaskSchedule.nextTick())
                    .schedule();
        }
    }

    private void handleSpawn(PlayerSpawnEvent event) {
        MinecraftServer.getSchedulerManager().buildTask(() -> refresh(event.getPlayer()))
                .delay(net.minestom.server.timer.TaskSchedule.nextTick())
                .schedule();
    }

    private void handleDisconnect(PlayerDisconnectEvent event) {
        playerGroups.remove(event.getPlayer());
    }

    private void refreshForEveryone(Player subject) {
        for (Player online : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (online == subject) {
                refreshViewOfOthers(subject);
                continue;
            }

            if (canSee(online, subject)) {
                online.sendPacket(createAddPacket(subject));
            } else {
                online.sendPacket(new PlayerInfoRemovePacket(subject.getUuid()));
            }
        }
    }

    private void refreshViewOfOthers(Player observer) {
        List<PlayerInfoUpdatePacket.Entry> entries = new ArrayList<>();
        List<UUID> toRemove = new ArrayList<>();

        for (Player online : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (online == observer) continue;

            if (canSee(observer, online)) {
                entries.add(createEntry(online));
            } else {
                toRemove.add(online.getUuid());
            }
        }

        if (!toRemove.isEmpty()) {
            observer.sendPacket(new PlayerInfoRemovePacket(toRemove));
        }
        if (!entries.isEmpty()) {
            observer.sendPacket(new PlayerInfoUpdatePacket(EnumSet.allOf(PlayerInfoUpdatePacket.Action.class), entries));
        }
    }

    private PlayerInfoUpdatePacket createAddPacket(Player player) {
        return new PlayerInfoUpdatePacket(EnumSet.allOf(PlayerInfoUpdatePacket.Action.class), List.of(createEntry(player)));
    }

    private PlayerInfoUpdatePacket.Entry createEntry(Player player) {
        List<PlayerInfoUpdatePacket.Property> properties = new ArrayList<>();
        PlayerSkin skin = player.getSkin();
        if (skin != null) {
            properties.add(new PlayerInfoUpdatePacket.Property("textures", skin.textures(), skin.signature()));
        }

        return new PlayerInfoUpdatePacket.Entry(
                player.getUuid(),
                player.getUsername(),
                properties,
                true,
                player.getLatency(),
                player.getGameMode(),
                player.getDisplayName(),
                null,
                0,
                false
        );
    }
}
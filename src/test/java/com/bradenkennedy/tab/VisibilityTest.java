package com.bradenkennedy.tab;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import com.bradenkennedy.tab.api.VisibilityGroup;
import com.bradenkennedy.tab.api.VisibilityGroups;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@EnvTest
public class VisibilityTest {

    @Test
    public void testPerInstanceVisibility(Env env) {
        EventNode<Event> node = EventNode.all("test");
        TabVisibilityManager manager = new TabVisibilityManager(node);
        env.process().eventHandler().addChild(node);

        Instance instanceA = env.createEmptyInstance();
        Instance instanceB = env.createEmptyInstance();

        Player p1 = env.createConnection().connect(instanceA, new Pos(0, 40, 0));
        Player p2 = env.createConnection().connect(instanceA, new Pos(0, 40, 0));
        Player p3 = env.createConnection().connect(instanceB, new Pos(0, 40, 0));

        for (int i = 0; i < 10; i++) env.tick();

        assertTrue(manager.canSee(p1, p2), "P1 should see P2 (same instance)");
        assertFalse(manager.canSee(p1, p3), "P1 should NOT see P3 (different instance)");

        p3.setInstance(instanceA).join();
        env.tick();

        assertTrue(manager.canSee(p1, p3), "P1 should see P3 after move");
    }

    @Test
    public void testSpectatorVisibility(Env env) {
        EventNode<Event> node = EventNode.all("test-spectator");
        TabVisibilityManager manager = new TabVisibilityManager(node);
        env.process().eventHandler().addChild(node);

        Instance instanceA = env.createEmptyInstance();
        Instance instanceB = env.createEmptyInstance();

        Player p1 = env.createConnection().connect(instanceA, new Pos(0, 40, 0));
        Player p2 = env.createConnection().connect(instanceA, new Pos(0, 40, 0));
        Player p3 = env.createConnection().connect(instanceB, new Pos(0, 40, 0));

        for (int i = 0; i < 10; i++) env.tick();

        VisibilityGroup specGroup = VisibilityGroups.spectator(manager);
        manager.setGroup(p1, specGroup);

        env.tick();

        assertTrue(manager.canSee(p1, p2), "Spectator P1 should see P2");
        assertFalse(manager.canSee(p1, p3), "Spectator P1 should NOT see P3");
        assertFalse(manager.canSee(p2, p1), "Non-spectator P2 should NOT see spectator P1");
    }
}

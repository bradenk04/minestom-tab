package com.bradenkennedy.tab;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@EnvTest
public class InstanceChangeTest {

    @Test
    public void testTabListUpdateOnInstanceChange(Env env) {
        EventNode<Event> node = EventNode.all("test");
        TabVisibilityManager manager = new TabVisibilityManager(node);
        env.process().eventHandler().addChild(node);

        Instance instanceA = env.createEmptyInstance();
        Instance instanceB = env.createEmptyInstance();

        Player p1 = env.createConnection().connect(instanceA, new Pos(0, 40, 0));
        Player p2 = env.createConnection().connect(instanceB, new Pos(0, 40, 0));

        for (int i = 0; i < 10; i++) env.tick();

        // Initially p1 and p2 should not see each other in tab (assuming PER_INSTANCE is default)
        assertFalse(manager.canSee(p1, p2));
        assertFalse(manager.canSee(p2, p1));

        // Move p2 to instanceA
        p2.setInstance(instanceA).join();
        for (int i = 0; i < 10; i++) env.tick();

        assertTrue(manager.canSee(p1, p2), "P1 should see P2 after P2 moved to same instance");
        assertTrue(manager.canSee(p2, p1), "P2 should see P1 after P2 moved to same instance");
    }
}

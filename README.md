# Minestom Tab

A simple library for managing per-instance tablists and player visibility groups in Minestom.

[![Maven Central](https://img.shields.io/maven-central/v/com.bradenkennedy/minestom-tab)](https://central.sonatype.com/artifact/com.bradenkennedy/minestom-tab)
[![License](https://img.shields.io/github/license/bradenkennedy/minestom-tab)](LICENSE)

## Features

- **Per-Instance TabList:** By default, players only see others within their own `Instance`.
- **Visibility Groups:** Define custom rules for who can see whom using a clean, asymmetric API.
- **Spectator Support:** Built-in support for spectators who can see everyone, but remain hidden from non-spectators.
- **Packet-Level Filtering:** Efficiently filters `PlayerInfoUpdatePacket` and `PlayerInfoRemovePacket` without requiring custom player classes.
- **Synchronized View Rules:** Automatically keeps world entity visibility in sync with tablist visibility.


> [!WARNING]
> This library is very basic and may contain issues, it's a rushed job currently and documentation might be lacking.
> I will be continuing to work on this project and make it actually solid :)

## Installation

### Gradle (Kotlin)
```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.bradenkennedy:minestom-tab:1.0.0")
}
```

### Maven
```xml
<dependency>
    <groupId>com.bradenkennedy</groupId>
    <artifactId>minestom-tab</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

### 1. Initialize the Manager
Register the `TabVisibilityManager` with your global `EventNode`.

```java
EventNode<Event> globalNode = MinecraftServer.getGlobalEventHandler();
TabVisibilityManager visibilityManager = new TabVisibilityManager(globalNode);
```

### 2. Define Visibility Groups
You can assign players to specific visibility groups to control their experience.

```java
import com.bradenkennedy.tab.api.VisibilityGroup;
import com.bradenkennedy.tab.api.VisibilityGroups;

// Create a spectator group
VisibilityGroup specGroup = VisibilityGroups.spectator(visibilityManager);

// Assign it to a player
visibilityManager.setGroup(player, specGroup);
```

### 3. Custom Visibility Logic
You can create your own visibility rules by implementing the `VisibilityGroup` interface.

```java
public class TeamVisibility implements VisibilityGroup {
    @Override
    public boolean canSee(@NotNull Player observer, @NotNull Player target) {
        // Only see players on the same team
        return getTeam(observer) == getTeam(target);
    }

    @Override
    public boolean canBeSeenBy(@NotNull Player target, @NotNull Player observer) {
        // Admins can always see this target
        return observer.hasPermission("admin") || VisibilityGroup.super.canBeSeenBy(target, observer);
    }
}
```

## API Overview

### `TabVisibilityManager`
The core coordinator for visibility.
- `setGroup(Player, VisibilityGroup)`: Updates a player's visibility rules.
- `getGroup(Player)`: Retrieves a player's current group.
- `refresh(Player)`: Manually triggers a visibility update for a player.
- `canSee(Player, Player)`: Checks if an observer can currently see a target.

### `VisibilityGroup`
The interface used to define visibility logic.
- `canSee(observer, target)`: Does the observer's group allow seeing the target?
- `canBeSeenBy(target, observer)`: Does the target's group allow being seen by the observer?

## Requirements
- **Java 25+**
- **Minestom** (Latest Version)

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

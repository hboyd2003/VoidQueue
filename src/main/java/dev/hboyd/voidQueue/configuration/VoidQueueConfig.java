/*
 * VoidQueue, a high-performance velocity queueing solution
 *
 * Copyright (c) 2025 Harrison Boyd
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package dev.hboyd.voidQueue.configuration;

import org.spongepowered.configurate.loader.HeaderMode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import dev.hboyd.voidQueue.queues.NotifyMethod;

import java.io.File;
import java.time.Duration;
import java.util.List;

@ConfigSerializable
public class VoidQueueConfig {



    @Comment("Total number of players allowed in the queue. -1 to disable")
    @Constraints.Min(-1)
    public int queuePlayerLimit = -1;

    @Comment("Total number of players allowed on the server (excludes queued). -1 to use velocity 'show-max-players' amount")
    @Constraints.Min(-1)
    public int connectedPlayerLimit = -1;

    @Comment("Duration between each queue movement")
    @Constraints.Positive
    public Duration movementDelay = Duration.ofMillis(500);

    @Comment("Duration a disconnected queued player remains in the queue")
    @Constraints.Positive
    public Duration queuedDisconnectTimeout = Duration.ofMinutes(3);

    @Comment("Duration a disconnected in-game player remains eligible for priority queue upon rejoining.")
    @Constraints.Positive
    public Duration inGameDisconnectTimeout = Duration.ofMinutes(3);

    @Comment("Duration between pruning of disconnected players.")
    @Constraints.Positive
    public Duration pruneDelay = Duration.ofSeconds(5);

    @Comment("The number of slots to reserve for priority.")
    @Constraints.Positive
    public int priorityQueueReserved = 10;

    @Comment("The number of slots to reserve for staff.")
    @Constraints.Positive
    public int staffQueueReserved = 15;

    @Comment("Permission used to place players in the priority queue")
    public String priorityPermission = "voidqueue.priority";

    @Comment("Permission used to place players in the staff queue")
    public String staffPermission = "voidqueue.staff";

    @Comment("List of kick reasons that should be considered fatal. If a player gets kicked from a queued server with one of these reasons, they will be removed from the queue without retrying. Accepts partial reasons")
    public List<String> fatalErrors = List.of();

    @Comment("Methods to notify the player that they are in the queue. Currently supports: BOSSBAR, ACTIONBAR, TEXT, TITLE")
    public List<NotifyMethod> notifyMethods = List.of(NotifyMethod.TITLE);

    protected static YamlConfigurationLoader getLoader(TypeSerializerCollection typeSerializerCollection, File configFile) {
        return YamlConfigurationLoader.builder()
                .defaultOptions(configurationOptions ->
                        configurationOptions
                                .serializers(typeSerializerCollection)
                                .shouldCopyDefaults(true)
                                .header("Configuration for proxy queues\nTest"))
                .indent(4)
                .nodeStyle(NodeStyle.BLOCK)
                .headerMode(HeaderMode.PRESERVE)
                .file(configFile)
                .commentsEnabled(true)
                .build();
    }
}

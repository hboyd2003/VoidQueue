/*
 * VoidQueue, a high-performance velocity queueing solution
 *
 * Copyright (c) 2025 Harrison Boyd
 *
 * Some portions of this file were taken from https://github.com/JLyne/ProxyQueues
 * These portions are Copyright (c) 2025 James Lyne
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

package dev.hboyd.voidQueue.queues;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import dev.hboyd.voidQueue.utils.TranslationUtil;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import net.elytrium.limboapi.api.player.GameMode;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;
import dev.hboyd.voidQueue.api.queues.QueueType;
import dev.hboyd.voidQueue.configuration.VoidQueueConfig;

import java.time.Duration;
import java.util.*;

// TODO: Should we have methods pass services to what needs them or should we provide methods?
public class VoidQueue {
    private final Logger logger;
    private final ProxyServer proxyServer;
    private final QueueStore queueStore;
    private final QueueRouterService queueRouterService;
    private final PlayerTracker playerTracker;
    private final Object plugin;
    private final QueueNotifierService queueNotifierService;
    private VoidQueueConfig voidQueueConfig;

    private final Map<PluginContainer, String> pauses = new HashMap<>(); // TODO: Should pauses be owned by QueueStore?

    private ScheduledTask queueTickTask;
    private ScheduledTask queuePruneTask;
    private ScheduledTask queueNotifyTask;

    private int connectedPlayerLimit; // TODO: This should probably be handled in another way

    Collection<RegisteredServer> registeredServers;
    private final Limbo limboServer;

    public VoidQueue(Logger logger,
                     Object plugin, // TODO: Should we register our own event handlers?
                     ProxyServer proxyServer,
                     Collection<RegisteredServer> servers,
                     VoidQueueConfig voidQueueConfig) {
        this.logger = logger;

        this.voidQueueConfig = voidQueueConfig;
        this.playerTracker = new PlayerTracker(logger,
                voidQueueConfig.priorityPermission,
                voidQueueConfig.staffPermission);
        this.proxyServer = proxyServer;

        this.connectedPlayerLimit = voidQueueConfig.connectedPlayerLimit;
        if (this.connectedPlayerLimit == -1)
            this.connectedPlayerLimit = proxyServer.getConfiguration().getShowMaxPlayers();

        LimboFactory limboFactory = (LimboFactory) this.proxyServer.getPluginManager()
                .getPlugin("limboapi")
                .flatMap(PluginContainer::getInstance)
                .orElseThrow();


        VirtualWorld queueWorld = limboFactory.createVirtualWorld(
                Dimension.OVERWORLD,
                0,
                100,
                0,
                (float) 90,
                (float) 0.0);

        this.limboServer = limboFactory.createLimbo(queueWorld)
                .setName("Queue")
                .setWorldTime(18000)
                .setGameMode(GameMode.SPECTATOR)
                .setViewDistance(2)
                .setSimulationDistance(2);

        this.queueStore = new QueueStore(logger);
        this.queueRouterService = new QueueRouterService(
                logger,
                servers,
                limboServer,
                this::createLimboWorldHandler);

        Optional<PluginContainer> container = proxyServer.getPluginManager().fromInstance(plugin);
        if(container.isEmpty()) throw new IllegalArgumentException("plugin is not registered");

        this.plugin = plugin;

        this.queueNotifierService = new QueueNotifierService(this, voidQueueConfig.notifyMethods);

        EventManager eventManager = proxyServer.getEventManager();
        eventManager.register(plugin, this);
        eventManager.register(plugin, playerTracker);

        Scheduler scheduler = proxyServer.getScheduler();
        queueTickTask = scheduler.buildTask(plugin, this::moveQueue).repeat(voidQueueConfig.movementDelay).schedule();
        queuePruneTask = scheduler.buildTask(plugin, this::pruneTrackedPlayers).repeat(voidQueueConfig.pruneDelay).schedule();
        queueNotifyTask = scheduler.buildTask(plugin, () -> queueNotifierService.notifyPositions()).repeat(Duration.ofMillis(500)).schedule();
    }


    private LimboWorldHandler createLimboWorldHandler(Player player) {
        return new LimboWorldHandler(this::onLimboSpawn, (limboPlayer) -> {});
    }

    private void onLimboSpawn(LimboPlayer limboPlayer) {
        playerTracker.trackPlayer(limboPlayer, TrackedPlayer.ConnectionState.LIMBO_JOIN).thenAccept(trackedPlayer -> {
            if (queueStore.isQueued(trackedPlayer)) return;

            Optional<TrackedPlayer.Disconnect> disconnect = trackedPlayer.getLastDisconnect();

            // Check if normal player should be priority queued
            if (disconnect.isPresent()
                    && trackedPlayer.getQueuePermissionType() == QueueType.NORMAL
                    && !disconnect.get().hasElapsed(voidQueueConfig.inGameDisconnectTimeout)
                    && disconnect.get().server() != null) {
                logger.info("Queueing recent in-game disconnect {} as priority queue", trackedPlayer.getPlayer().getUsername()); // TODO: Log with UUID
                queueStore.addPlayer(trackedPlayer, QueueType.PRIORITY); // TODO: Notify player
                return;
            }

            logger.info("Queueing {}", trackedPlayer.getPlayer().getUsername());
            queueStore.addPlayer(trackedPlayer, trackedPlayer.getQueuePermissionType());
            if (!pauses.isEmpty()) queueNotifierService.notifyPause(trackedPlayer);
        });
    }

    @Subscribe
    private void onLimbo(LoginLimboRegisterEvent event) {
        event.setOnKickCallback(this::onKickedFromServer);
    }

    private boolean onKickedFromServer(KickedFromServerEvent event) {
        Optional<TrackedPlayer> trackedPlayer = playerTracker.getQueuePlayer(event.getPlayer().getUniqueId());
        if (trackedPlayer.isEmpty()) return false;

        Component kickReason = event.getServerKickReason().orElse(Component.text("Unable to connect to server"));
        if (event.kickedDuringServerConnect()) { // Failed to connect needs to rejoin
            event.setResult(KickedFromServerEvent.DisconnectPlayer.create(kickReason));
            return false;
        }

        String kickReasonString = TranslationUtil.toString(kickReason);
        for (String fatalError : this.voidQueueConfig.fatalErrors) {
            if (kickReasonString.toLowerCase().contains(fatalError.toLowerCase())) return false;
        }

        this.queueRouterService.sendToLimbo(event.getPlayer()); // Requeue player
        return true;
    }

    @Subscribe
    private void onServerConnect(ServerConnectedEvent event) {
        Optional<TrackedPlayer> trackedPlayer = playerTracker.getQueuePlayer(event.getPlayer().getUniqueId());
        if (trackedPlayer.isEmpty()) return;

        if (!queueStore.isQueued(trackedPlayer.get())) return;

        trackedPlayer.get().getPlayer().clearTitle(); // TODO: Should this be the responsibility of proxy queue or should the notifier have an event?
        queueStore.removePlayer(trackedPlayer.get());
    }


    public void moveQueue() {
        if (isServerFull(QueueType.NORMAL)
                && isServerFull(QueueType.PRIORITY)
                && isServerFull(QueueType.STAFF)) return;
        TrackedPlayer trackedPlayer;

        if (!isServerFull(QueueType.STAFF) && queueStore.hasNextIdleActive(QueueType.STAFF)) {
            trackedPlayer = queueStore.nextIdleActive(QueueType.STAFF).orElseThrow();
        } else if (!isServerFull(QueueType.PRIORITY) && queueStore.hasNextIdleActive(QueueType.PRIORITY) && !isPaused()) {
            trackedPlayer = queueStore.nextIdleActive(QueueType.PRIORITY).orElseThrow();
        } else if (!isServerFull(QueueType.NORMAL) && queueStore.hasNextIdleActive(QueueType.NORMAL) && !isPaused()) {
            trackedPlayer = queueStore.nextIdleActive(QueueType.NORMAL).orElseThrow();
        } else {
            return;
        }

        queueRouterService.routeToServer(trackedPlayer);
    }

    public void pruneTrackedPlayers() {
        for (TrackedPlayer trackedPlayer : playerTracker.getTrackedPlayers()) {
            if (trackedPlayer.getPlayer().isActive()) continue;

            Optional<TrackedPlayer.Disconnect> disconnect = trackedPlayer.getLastDisconnect();
            if (disconnect.isEmpty()) {
                logger.error("Tracked player {} ({}) has been disconnected but does not have a disconnect time!",
                        trackedPlayer.getPlayer().getUsername(),
                        trackedPlayer.getPlayer().getUniqueId());
                continue;
            }
            // TODO: On disconnect a tracked players state give better indication as to where/what they where stage they where at (connecting to server, in server, connecting to queue etc)
            if (queueStore.isQueued(trackedPlayer)) {
                if (!disconnect.get().hasElapsed(voidQueueConfig.queuedDisconnectTimeout)) return;
            } else if (!disconnect.get().hasElapsed(voidQueueConfig.inGameDisconnectTimeout)) return;

            queueStore.removePlayer(trackedPlayer);
            playerTracker.unTrackPlayer(trackedPlayer);
        }
    }


    public void addPause(Object plugin, String reason) {
        Optional<PluginContainer> container = proxyServer.getPluginManager().fromInstance(plugin);

        if(container.isEmpty()) throw new IllegalArgumentException("plugin is not registered");

        pauses.put(container.get(), reason);

        if(pauses.size() == 1) {
            queueNotifierService.notifyPause();
        }
    }

    public void removePause(Object plugin) {
       Optional<PluginContainer> container = proxyServer.getPluginManager().fromInstance(plugin);

        if(container.isEmpty()) throw new IllegalArgumentException("plugin is not registered");

        if(pauses.remove(container.get()) != null && pauses.isEmpty()) {
            queueNotifierService.notifyResume();
        }
    }

    public boolean hasPause(Object plugin) {
        Optional<PluginContainer> container = proxyServer.getPluginManager().fromInstance(plugin);

        if(container.isEmpty()) throw new IllegalArgumentException("plugin is not registered");

        return pauses.containsKey(container.get());
    }

    public boolean isPaused() {
        return !pauses.isEmpty();
    }

    public boolean isPaused(Object plugin) {
        Optional<PluginContainer> container = proxyServer.getPluginManager().fromInstance(plugin);
        if(container.isEmpty()) throw new IllegalArgumentException("plugin is not registered");

        for (PluginContainer pauserPlugin : pauses.keySet()) {
            if (pauserPlugin.equals(container.get())) return true;
        }
        return false;
    }

    public QueueStore getQueueStore() {
        return queueStore;
    }

    public Map<PluginContainer, String> getPauses() {
        return pauses; // TODO: Should we return a immutable version?
    }

    public QueueRouterService getQueueRouterService() {
        return queueRouterService;
    }

    public record SlotsUsed(int  normalSlotsUsed, int prioritySlotsUsed, int staffSlotsUsed) {}

    private record SlotUsage(int normalSlotsUsed, int prioritySlotsUsed, int priorityOverflow, int staffSlotsUsed, int staffOverflow) {}

    // TODO: Should getSlotsUsed, isServerFull, calculateSlotUsage be apart of VoidQueue class?
    // TODO: Better way to calculate slot usage?
    public int getSlotsUsed(QueueType queueType) {
        SlotsUsed slotsUsed = getSlotsUsed();
        return switch (queueType) {
            case NORMAL -> slotsUsed.normalSlotsUsed;
            case PRIORITY -> slotsUsed.prioritySlotsUsed;
            case STAFF -> slotsUsed.staffSlotsUsed;
        };
    }

    public SlotsUsed getSlotsUsed() {
        SlotUsage slotUsage = calculateSlotUsage();

        return new SlotsUsed(slotUsage.normalSlotsUsed, slotUsage.prioritySlotsUsed, slotUsage.staffSlotsUsed);
    }

    public boolean isServerFull(QueueType queueType) {
        SlotUsage slotUsage = calculateSlotUsage();

        int maxNormalQueue = getReservedSlots(QueueType.NORMAL);

        return switch (queueType)
        {
            case NORMAL -> maxNormalQueue - slotUsage.normalSlotsUsed <= 0;
            case PRIORITY -> slotUsage.priorityOverflow > 0;
            case STAFF -> slotUsage.staffOverflow > 0;
        };
    }

    private SlotUsage calculateSlotUsage() {
        int priorityReservedSlots = getReservedSlots(QueueType.PRIORITY);
        int staffReservedSlots = getReservedSlots(QueueType.STAFF);
        int maxNormalQueue = getReservedSlots(QueueType.NORMAL);

        int staffSlotsUsed = playerTracker.getInGameCount(QueueType.STAFF);
        int prioritySlotsUsed = playerTracker.getInGameCount(QueueType.PRIORITY);
        int normalSlotsUsed = playerTracker.getInGameCount(QueueType.NORMAL);

        int staffOverflow = Math.max(staffSlotsUsed - staffReservedSlots, 0);
        int priorityOverflow = Math.max(prioritySlotsUsed - priorityReservedSlots, 0);

        int availableNormal = Math.max(0, maxNormalQueue - normalSlotsUsed);
        int usedByStaffOverflow = Math.min(staffOverflow, availableNormal);
        normalSlotsUsed += usedByStaffOverflow;
        staffOverflow -= usedByStaffOverflow;

        int availablePriority = Math.max(0, priorityReservedSlots - prioritySlotsUsed);
        usedByStaffOverflow = Math.min(staffOverflow, availablePriority);
        staffOverflow -= usedByStaffOverflow;

        availableNormal = Math.max(0, maxNormalQueue - normalSlotsUsed);
        int usedByPriorityOverflow = Math.min(priorityOverflow, availableNormal);
        normalSlotsUsed += usedByPriorityOverflow;
        priorityOverflow -= usedByPriorityOverflow;

        normalSlotsUsed += priorityOverflow + staffOverflow;

        return new SlotUsage(normalSlotsUsed, prioritySlotsUsed, priorityOverflow, staffSlotsUsed, staffOverflow);
    }

    public PlayerTracker getPlayerTracker() {
        return playerTracker;
    }

    public Component getStatusMessage(QueueType queueType) {
        String message = """
                <aqua>Queued:
                    <aqua>Online: <yellow><queued_online>
                    <aqua>Offline: <yellow><queued_offline>
                <aqua>Slots used: <yellow><slots_used> / <reserved>
                <aqua>In Game: <yellow><connected>
                <aqua>Front 3: <yellow><first>, <second>, <third>""".stripIndent().indent(4);

        Iterator<TrackedPlayer> trackedPlayerIterator = queueStore.getQueueIterator(queueType);
        String first = trackedPlayerIterator.hasNext() ? trackedPlayerIterator.next().getPlayer().getUsername() : "N/A";
        String second = trackedPlayerIterator.hasNext() ? trackedPlayerIterator.next().getPlayer().getUsername() : "N/A";
        String third = trackedPlayerIterator.hasNext() ? trackedPlayerIterator.next().getPlayer().getUsername() : "N/A";

        return MiniMessage.miniMessage().deserialize(message,
                Placeholder.unparsed("queued_online", String.valueOf(queueStore.getQueuedActiveIdleCount(queueType))),
                Placeholder.unparsed("queued_offline", String.valueOf(queueStore.getQueuedInActiveCount(queueType))),
                Placeholder.unparsed("slots_used", String.valueOf(getSlotsUsed(queueType))),
                Placeholder.unparsed("reserved", String.valueOf(getReservedSlots(queueType))),
                Placeholder.unparsed("connected", String.valueOf(playerTracker.getInGameCount(queueType))),
                Placeholder.unparsed("first", first),
                Placeholder.unparsed("second", second),
                Placeholder.unparsed("third", third));
    }

    private int getReservedSlots(QueueType queueType) {
        int priorityReservedSlots = voidQueueConfig.priorityQueueReserved;
        int staffReservedSlots = voidQueueConfig.staffQueueReserved;

        return switch (queueType) {
          case NORMAL -> connectedPlayerLimit - priorityReservedSlots - staffReservedSlots;
          case PRIORITY -> priorityReservedSlots;
          case STAFF -> staffReservedSlots;
        };
    }

    // TODO: This should be handled by the plugin itself (for multiple queues support)
    @Subscribe
    private void onLoginLimboRegister(LoginLimboRegisterEvent event) {
        event.addOnJoinCallback(() -> {
            Player player = event.getPlayer();
            // Get the queue
            queueRouterService.sendToLimbo(player);
        });
    }

    public int getConnectedPlayerLimit() {
        return connectedPlayerLimit;
    }

}

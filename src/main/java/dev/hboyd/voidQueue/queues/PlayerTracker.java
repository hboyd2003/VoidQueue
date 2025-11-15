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

package dev.hboyd.voidQueue.queues;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.*;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.elytrium.limboapi.api.event.LimboDisconnectEvent;
import net.elytrium.limboapi.api.event.LimboSpawnEvent;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import dev.hboyd.voidQueue.api.queues.QueueType;
import dev.hboyd.voidQueue.utils.LuckPermsPermissionUtil;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerTracker {
    private final Logger logger;

    private final ConcurrentHashMap<UUID, TrackedPlayer> trackedPlayers;

    private final LuckPerms luckPerms;

    private final @NotNull String priorityPermission;
    private final @NotNull String staffPermission;

    public PlayerTracker(Logger logger,
                         @NotNull String priorityPermission,
                         @NotNull String staffPermission) {
        this.logger = logger;

        trackedPlayers = new ConcurrentHashMap<>();

        this.luckPerms = LuckPermsProvider.get();

        this.priorityPermission = priorityPermission;
        this.staffPermission = staffPermission;


        luckPerms.getEventBus().subscribe(UserDataRecalculateEvent.class, this::onUserDataRecalculate);
    }

    public int getInGameCount(QueueType queueType) {
        return (int) trackedPlayers.values().stream()
                .filter(TrackedPlayer::isInGame)
                .filter(trackedPlayer -> trackedPlayer.getQueuePermissionType() == queueType)
                .count();
    }

    /**
     * Tracks a given player and updates its data based on events.
     *
     * @param limboPlayer The player to track
     * @return
     */
    public CompletableFuture<TrackedPlayer> trackPlayer(LimboPlayer limboPlayer, TrackedPlayer.ConnectionState connectionState) {
        return trackPlayer(limboPlayer.getProxyPlayer(), connectionState).thenApply(trackedPlayer -> {
            trackedPlayer.setLimboPlayer(limboPlayer);
            return trackedPlayer;
        });
    }

    /**
     * Tracks a given player and updates its data based on events.
     *
     * @param player The player to track
     * @return
     */
    public CompletableFuture<TrackedPlayer> trackPlayer(Player player, TrackedPlayer.ConnectionState connectionState) {
        return LuckPermsPermissionUtil.getUserAsync(player.getUniqueId()).thenApply(user -> {
            QueueType queueType = mapPermissionsToQueueType(user.getCachedData().getPermissionData());

            if (trackedPlayers.containsKey(player.getUniqueId()))  {
                TrackedPlayer oldQueuePlayer = trackedPlayers.get(player.getUniqueId());
                oldQueuePlayer.setQueuePermissionType(queueType);
                oldQueuePlayer.clearLimboPlayer();
                oldQueuePlayer.setPlayer(player);
                oldQueuePlayer.setConnectionState(connectionState);

                return oldQueuePlayer;
            }

            TrackedPlayer trackedPlayer = new TrackedPlayer(player, queueType, connectionState);
            trackedPlayers.put(player.getUniqueId(), trackedPlayer);
            return trackedPlayer;
        });
    }

    public void unTrackPlayer(UUID uuid) {
        trackedPlayers.remove(uuid);
    }

    public void unTrackPlayer(TrackedPlayer trackedPlayer) {
        trackedPlayers.remove(trackedPlayer.getPlayer().getUniqueId());
    }

    public boolean isPlayerTracked(UUID uuid) {
        return trackedPlayers.containsKey(uuid);
    }

    public Collection<TrackedPlayer> getTrackedPlayers() {
        return trackedPlayers.values();
    }

    public Optional<TrackedPlayer> getQueuePlayer(UUID uuid) {
        return Optional.ofNullable(trackedPlayers.get(uuid));
    }

    private QueueType mapPermissionsToQueueType(CachedPermissionData permissionData) {
        if (permissionData.checkPermission(staffPermission).asBoolean()) return QueueType.STAFF;
        if (permissionData.checkPermission(priorityPermission).asBoolean()) return QueueType.PRIORITY;
        return QueueType.NORMAL;
    }


    @Subscribe
    private void onPreLogin(PreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        if (uuid == null) return;
        TrackedPlayer trackedPlayer = trackedPlayers.get(event.getUniqueId());
        if (trackedPlayer == null) return;

        trackedPlayer.setConnectionState(TrackedPlayer.ConnectionState.PRE_LOGIN);
    }

    @Subscribe
    private void onLimboSpawn(LimboSpawnEvent event) {
        TrackedPlayer trackedPlayer = trackedPlayers.get(event.limboPlayer().getProxyPlayer().getUniqueId());
        if (trackedPlayer == null) return;

        trackedPlayer.setLimboPlayer(event.limboPlayer());
        trackedPlayer.setConnectionState(TrackedPlayer.ConnectionState.LIMBO_JOIN);
    }

    @Subscribe
    private void onLimboDisconnect(LimboDisconnectEvent event) {
        TrackedPlayer trackedPlayer = trackedPlayers.get(event.limboPlayer().getProxyPlayer().getUniqueId());
        if (trackedPlayer == null) return;

        trackedPlayer.setLimboPlayer(event.limboPlayer());
        trackedPlayer.setConnectionState(TrackedPlayer.ConnectionState.LIMBO_LEAVE);
    }

    @Subscribe
    private void onLogin(LoginEvent event) {
        TrackedPlayer trackedPlayer = trackedPlayers.get(event.getPlayer().getUniqueId());
        if (trackedPlayer == null) return;

        trackedPlayer.setConnectionState(TrackedPlayer.ConnectionState.LOGIN);
    }

    @Subscribe
    private void onPostLogin(PostLoginEvent event) {
        TrackedPlayer trackedPlayer = trackedPlayers.get(event.getPlayer().getUniqueId());
        if (trackedPlayer == null) return;

        trackedPlayer.setConnectionState(TrackedPlayer.ConnectionState.POST_LOGIN);
    }

    @Subscribe
    private void onServerPreConnect(ServerPreConnectEvent event) {
        TrackedPlayer trackedPlayer = trackedPlayers.get(event.getPlayer().getUniqueId());
        if (trackedPlayer == null) return;

        trackedPlayer.setConnectionState(TrackedPlayer.ConnectionState.SERVER_PRE_CONNECT);
    }

    @Subscribe
    private void onServerConnect(ServerConnectedEvent event) {
        TrackedPlayer trackedPlayer = trackedPlayers.get(event.getPlayer().getUniqueId());
        if (trackedPlayer == null) return;

        trackedPlayer.setConnectionState(TrackedPlayer.ConnectionState.SERVER_CONNECT);
    }

    @Subscribe
    private void onServerPostConnect(ServerPostConnectEvent event) {
        TrackedPlayer trackedPlayer = trackedPlayers.get(event.getPlayer().getUniqueId());
        if (trackedPlayer == null) return;

        trackedPlayer.setConnectionState(TrackedPlayer.ConnectionState.POST_LOGIN);
    }

    @Subscribe
    private void onPreTransfer(PreTransferEvent event) {
        TrackedPlayer trackedPlayer = trackedPlayers.get(event.player().getUniqueId());
        if (trackedPlayer == null) return;

        trackedPlayer.setConnectionState(TrackedPlayer.ConnectionState.PRE_TRANSFER);
    }

    @Subscribe
    private void onKicked(KickedFromServerEvent event) {
        TrackedPlayer trackedPlayer = trackedPlayers.get(event.getPlayer().getUniqueId());
        if (trackedPlayer == null) return;

        trackedPlayer.setConnectionState(TrackedPlayer.ConnectionState.SERVER_KICKED);
    }

    @Subscribe
    private void onDisconnect(DisconnectEvent event) {
        TrackedPlayer trackedPlayer = trackedPlayers.get(event.getPlayer().getUniqueId());
        if (trackedPlayer == null) return;

        trackedPlayer.clearLimboPlayer();
        RegisteredServer lastServer = trackedPlayer.getPlayer().getCurrentServer()
                .map(ServerConnection::getServer)
                .orElse(null);

        trackedPlayer.setLastDisconnect(new TrackedPlayer.Disconnect(Instant.now(), lastServer));
        trackedPlayer.clearConnectionState();
    }

    private void onUserDataRecalculate(UserDataRecalculateEvent event) {
        TrackedPlayer trackedPlayer = trackedPlayers.get(event.getUser().getUniqueId());
        if (trackedPlayer == null) return;

        LuckPermsPermissionUtil.getUserAsync(trackedPlayer.getPlayer().getUniqueId()).thenAccept(user -> {
            QueueType queueType = mapPermissionsToQueueType(user.getCachedData().getPermissionData());
            trackedPlayer.setQueuePermissionType(queueType);
        });
    }


}

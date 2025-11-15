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
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.elytrium.limboapi.api.player.LimboPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import dev.hboyd.voidQueue.api.queues.QueueType;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public final class TrackedPlayer {
    public enum ConnectionState {
        PRE_LOGIN,
        LOGIN,
        POST_LOGIN,
        LIMBO_JOIN,
        LIMBO_LEAVE,
        SERVER_PRE_CONNECT,
        SERVER_CONNECT, // TODO: Which order does server pre/post connect fire?
        SERVER_POST_CONNECT,
        PRE_TRANSFER,
        SERVER_KICKED // Kicked from server and "looking" for new server to join (or will disconnect fully)
    }

    // TODO: Should Disconnect record be here or somewhere else?
    public record Disconnect(Instant time, @Nullable RegisteredServer server) {
        public boolean hasElapsed(Duration duration) {
            return Instant.now().isAfter(this.time.plus(duration));
        }
    }

    private @NotNull Player player;
    private @Nullable LimboPlayer limboPlayer;

    private QueueType queuePermissionType;
    private @Nullable Disconnect lastDisconnect;

    private @Nullable ConnectionState connectionState;

    public TrackedPlayer(@NotNull LimboPlayer limboPlayer, @NotNull QueueType queuePermissionType, @Nullable ConnectionState connectionState) {
        this.player = limboPlayer.getProxyPlayer();
        this.limboPlayer = limboPlayer;
        this.queuePermissionType = queuePermissionType;
        this.connectionState = connectionState;

        this.lastDisconnect = null;
    }

    public TrackedPlayer(@NotNull Player player, @NotNull QueueType queuePermissionType, @Nullable ConnectionState connectionState) {
        this.player = player;
        this.queuePermissionType = queuePermissionType;
        this.connectionState = connectionState;

        this.limboPlayer = null;
        this.lastDisconnect = null;
    }

    public boolean isInGame() {
        return player.isActive() && player.getCurrentServer().isPresent();
    }

    public Optional<Disconnect> getLastDisconnect() {
        return Optional.ofNullable(lastDisconnect);
    }

    public void clearDisconnect() {
        this.lastDisconnect = null;
    }

    // TODO: Values updated by PlayerTracker should probably be protected and in a separate package? or maybe 2 different trackedPlayer class one with the setters and one without?
    public void setLastDisconnect(@Nullable Disconnect lastDisconnect) {
        this.lastDisconnect = lastDisconnect;
    }

    public QueueType getQueuePermissionType() {
        return queuePermissionType;
    }

    public void setQueuePermissionType(QueueType queuePermissionType) {
        this.queuePermissionType = queuePermissionType;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public void setPlayer(@NotNull Player player) {
        this.player = player;
    }

    public Optional<LimboPlayer> getLimboPlayer() {
        return Optional.ofNullable(this.limboPlayer);
    }

    public void clearLimboPlayer() {
        this.limboPlayer = null;
    }

    public void setLimboPlayer(@Nullable LimboPlayer limboPlayer) {
        this.limboPlayer = limboPlayer;
    }

    public Optional<ConnectionState> getConnectionState() {
        return Optional.ofNullable(connectionState);
    }

    public void setConnectionState(@Nullable ConnectionState connectionState) {
        this.connectionState = connectionState;
    }

    public void clearConnectionState() {
        this.connectionState = null;
    }

}
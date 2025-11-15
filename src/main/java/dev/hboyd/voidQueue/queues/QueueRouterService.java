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
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Function;

public class QueueRouterService {
    private final Logger logger;

    private Collection<RegisteredServer> servers;
    private final Limbo limboServer;
    private final Function<Player, LimboSessionHandler> limboSessionHandlerCreator;

    public QueueRouterService(Logger logger,
                              Collection<RegisteredServer> servers,
                              Limbo limboServer,
                              Function<Player, LimboSessionHandler> limboSessionHandlerCreator) {
        this.logger = logger;

        this.servers = servers;
        this.limboServer = limboServer;
        this.limboSessionHandlerCreator = limboSessionHandlerCreator;
    }

    public void sendToLimbo(Player player) {
        if (!player.isActive()) throw new IllegalArgumentException("Player is not online");

        logger.info("Attempting to send {} to limbo", player.getUsername()); // TODO: Add name of limbo server we are sending to
        limboServer.spawnPlayer(player, limboSessionHandlerCreator.apply(player));
    }

    public void routeToServer(TrackedPlayer trackedPlayer) {
        RegisteredServer server = getLeastPopServer();

        logger.info("Attempting to connect player {} to {}", trackedPlayer.toString(), server.getServerInfo().getName());
        if (trackedPlayer.getLimboPlayer().isEmpty())
            throw new IllegalArgumentException("Cannot route a player who is not connected to limbo");

        trackedPlayer.getLimboPlayer().get().disconnect(server);

        //trackedPlayer.setConnecting(true);
    }

    public void kick(TrackedPlayer trackedPlayer, Component reason) {
        if (!trackedPlayer.getPlayer().isActive())
            throw new IllegalArgumentException("Player is not online");

        trackedPlayer.getPlayer().disconnect(reason);
    }

    private RegisteredServer getLeastPopServer() {
        Iterator<RegisteredServer> iterator = servers.iterator();
        RegisteredServer leastPopServer = iterator.next();
        int leastPopServerPlayerCount =  leastPopServer.getPlayersConnected().size();
        while(iterator.hasNext()) {
            RegisteredServer server = iterator.next();
            int currentServerPlayerCount = server.getPlayersConnected().size();
            if (currentServerPlayerCount < leastPopServerPlayerCount) {
                leastPopServer = server;
                leastPopServerPlayerCount = currentServerPlayerCount;
            }
        }
        return leastPopServer;
    }
}

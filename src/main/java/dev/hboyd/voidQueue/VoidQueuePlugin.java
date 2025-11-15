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

package dev.hboyd.voidQueue;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIVelocityConfig;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.player.GameMode;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import dev.hboyd.voidQueue.command.VoidQueueCommand;
import dev.hboyd.voidQueue.configuration.ConfigService;
import org.slf4j.Logger;
import dev.hboyd.voidQueue.queues.VoidQueue;
import uk.co.notnull.vanishbridge.helper.VanishBridgeHelper;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;

import com.google.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.ResourceBundle;

// TODO: Support for multiple queues. Queue should provide a router which the plugin uses to send players to each queue.
public final class VoidQueuePlugin {

    private static VoidQueuePlugin INSTANCE;
    private VoidQueue voidQueue;
    private final ConfigService configService;
    //private final VoidQueueTranslationService proxyQueueTranslationService;
    private Limbo queueServer;

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirPath;

    @Inject
    public VoidQueuePlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirPath) throws IOException {
        this.proxyServer = proxy;
        this.logger = logger;
        INSTANCE = this;

        File dataDir = dataDirPath.toFile();
        if (!dataDir.exists() && !dataDir.mkdirs()) {
            throw new IOException("Failed to create data directory");
        }
        if (!dataDir.isDirectory()) {
            throw new IOException("Data directory is not a directory");
        }
        this.dataDirPath = dataDirPath;


        try {
            this.configService = new ConfigService(this.logger, dataDirPath);
        } catch (IOException e) {
            throw new IOException("Failed to load config", e);
        }

        MiniMessageTranslationStore translationStore = MiniMessageTranslationStore.create(Key.key("voidqueue:lang"));
        ResourceBundle bundle = ResourceBundle.getBundle("dev.hboyd.voidQueue.Lang", Locale.US, UTF8ResourceBundleControl.utf8ResourceBundleControl());
        translationStore.registerAll(Locale.US, bundle, true);
        GlobalTranslator.translator().addSource(translationStore);

        CommandAPI.onLoad(new CommandAPIVelocityConfig(this.proxyServer, this));

//        try {
//            this.proxyQueueTranslationService = new VoidQueueTranslationService(dataDirPath);
//        } catch (IOException e) {
//            throw new IOException("Failed to load translations", e);
//        }
    }

    @Subscribe
    private void onProxyReload(ProxyReloadEvent proxyReloadEvent) {
        reload();
    }

    // TODO: Implement reload
    public void reload() {
        logger.error("VoidQueue reload is not implemented yet");
        configService.reload();
        // Block all new player connections
        // Disconnect currently queued players
        // Go through all tracked players and attempt to end their connection if they not in-game but have an active connection
        // Re-initialize
        // Re-track all players
        // Unblock incoming players
    }

    // TODO: Implement disable/shutdown
    public void disable() {
        // TODO: Is this a safe way to "disable" the plugin?
        proxyServer.getPluginManager().fromInstance(this).get().getExecutorService().shutdown();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        initLimbo();
        buildQueue();

        VoidQueueCommand.register(this);
        CommandAPI.onEnable();

        new VanishBridgeHelper(proxyServer);
    }


    private void initLimbo() {
        LimboFactory limboFactory = (LimboFactory) this.proxyServer.getPluginManager()
                .getPlugin("limboapi")
                .flatMap(PluginContainer::getInstance)
                .orElseThrow();

        VirtualWorld queueWorld = limboFactory.createVirtualWorld(Dimension.OVERWORLD, 0, 100, 0, (float) 90, (float) 0.0);
        this.queueServer = limboFactory.createLimbo(queueWorld)
                .setName("Queue")
                .setWorldTime(18000)
                .setGameMode(GameMode.SPECTATOR)
                .setViewDistance(2)
                .setSimulationDistance(2);
    }

    public VoidQueue getVoidQueue() {
        return this.voidQueue;
    }

    private void buildQueue() {
        getLogger().info("Creating the queue");


        voidQueue = new VoidQueue(logger,
                this,
                proxyServer,
                proxyServer.getAllServers(),
                configService.getVoidQueueConfig());
    }

    public Logger getLogger() {
        return logger;
    }

    public static VoidQueuePlugin getInstance() {
        return INSTANCE;
    }

}

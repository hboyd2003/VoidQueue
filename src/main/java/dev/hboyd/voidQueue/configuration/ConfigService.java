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

import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import dev.hboyd.voidQueue.configuration.serializer.DurationSerializer;
import dev.hboyd.voidQueue.configuration.serializer.InformMethodSerializer;
import dev.hboyd.voidQueue.queues.NotifyMethod;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public class ConfigService {
    private static final String configFilename = "config.yaml";

    private final Logger logger;
    private final Path dataDir;
    private final ObjectMapper.Factory configFactory;
    private final YamlConfigurationLoader configLoader;

    private CommentedConfigurationNode voidQueueConfigNode;
    private VoidQueueConfig voidQueueConfig;

    public ConfigService(Logger logger, Path dataDir) throws IOException {
        this.logger = logger;
        this.dataDir = dataDir;
        Files.createDirectories(this.dataDir);
        File configFile = dataDir.resolve(configFilename).toFile();

        this.configFactory = ObjectMapper.factoryBuilder()
                .addConstraint(Constraints.Positive.class, Number.class, new Constraints.Positive.Factory())
                .addConstraint(Constraints.Min.class, Number.class, new Constraints.Min.Factory())
                .build();

        TypeSerializerCollection typeSerializers = TypeSerializerCollection.defaults().childBuilder()
                .registerAnnotatedObjects(configFactory)
                .register(NotifyMethod.class, InformMethodSerializer.INSTANCE)
                .register(Duration.class, DurationSerializer.INSTANCE)
                .build();

        this.configLoader = VoidQueueConfig.getLoader(typeSerializers, configFile);
        this.voidQueueConfigNode = configLoader.load();
        this.voidQueueConfig = this.voidQueueConfigNode.get(VoidQueueConfig.class);

        if (this.voidQueueConfig == null) {
            throw new RuntimeException("Failed to load Proxy queue config. Null was returned by the loader");
        }
        if (!configFile.exists()) {
            logger.info("No config detected, saving defaults");
            save();
        }

        logger.info("Loaded config");
    }

    public VoidQueueConfig getVoidQueueConfig() {
        return voidQueueConfig;
    }

    public boolean reload() {
        logger.info("Reloading config");
        try {
            this.voidQueueConfigNode = this.configLoader.load();
            this.voidQueueConfig = this.voidQueueConfigNode.get(VoidQueueConfig.class);
        } catch (ConfigurateException e) {
            logger.error("Failed to reload config", e);
            return false;
        }
        return true;
    }

    private boolean save() {
        logger.info("Saving config");
        try {
            this.voidQueueConfigNode.set(VoidQueueConfig.class, voidQueueConfig);
            this.configLoader.save(this.voidQueueConfigNode);
        } catch (ConfigurateException e) {
            logger.error("Failed to save config", e);
            return false;
        }
        return true;
    }
}
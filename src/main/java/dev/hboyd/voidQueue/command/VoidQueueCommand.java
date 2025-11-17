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

package dev.hboyd.voidQueue.command;

import com.mojang.brigadier.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import dev.hboyd.voidQueue.VoidQueuePlugin;
import dev.hboyd.voidQueue.queues.VoidQueue;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.translation.Argument;
import dev.hboyd.voidQueue.api.queues.QueueType;
import dev.hboyd.voidQueue.command.argument.TrackedPlayerArgument;
import dev.hboyd.voidQueue.queues.QueueRouterService;
import dev.hboyd.voidQueue.queues.QueueStore;
import dev.hboyd.voidQueue.queues.TrackedPlayer;
import dev.hboyd.voidQueue.utils.Constants;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

// TODO: Add support for multiple queues
public class VoidQueueCommand {
    // TODO: Clean this up....
    private static final Supplier<Collection<TrackedPlayer>> queuedPlayerProvider = () ->
            VoidQueuePlugin.getInstance().getVoidQueue()
                    .getPlayerTracker()
                    .getTrackedPlayers()
                    .stream()
                    .filter(trackedPlayer -> VoidQueuePlugin.getInstance().getVoidQueue().getQueueStore().isQueued(trackedPlayer)).toList();

    private static final Supplier<Collection<TrackedPlayer>> onlineQueuedPlayerProvider = () ->
            VoidQueuePlugin.getInstance().getVoidQueue()
                    .getPlayerTracker()
                    .getTrackedPlayers()
                    .stream()
                    .filter(trackedPlayer -> VoidQueuePlugin.getInstance().getVoidQueue().getQueueStore().isQueued(trackedPlayer)
                            && trackedPlayer.getPlayer().isActive()
                            && trackedPlayer.getConnectionState().isPresent()
                            && trackedPlayer.getConnectionState().get() == TrackedPlayer.ConnectionState.LIMBO_JOIN)
                    .toList();

    public static void register(Object plugin) {
        CommandAPICommand pauseCommand = new CommandAPICommand("pause")
                .withPermission(Constants.BASE_PERM + "pause")
                .withOptionalArguments(new GreedyStringArgument("reason"))
                .executes(VoidQueueCommand::pause);

        CommandAPICommand unpauseCommand = new CommandAPICommand("unpause")
                .withPermission(Constants.BASE_PERM + "pause")
                .withOptionalArguments(new GreedyStringArgument("reason"))
                .withUsage("unpause <reason>")
                .executes(VoidQueueCommand::unpause);

        CommandAPICommand kickCommand = new CommandAPICommand("kick")
                .withPermission(Constants.BASE_PERM + "kick")
                .withOptionalArguments(new TrackedPlayerArgument("player", queuedPlayerProvider))
                .executes(VoidQueueCommand::kickFromQueue);

        CommandAPICommand clearCommand = new CommandAPICommand("clear")
                .withPermission(Constants.BASE_PERM + "clear")
                .executes(VoidQueueCommand::clearQueue);

        CommandAPICommand pullCommand = new CommandAPICommand("pull")
                .withPermission(Constants.BASE_PERM + "pull")
                .withOptionalArguments(new TrackedPlayerArgument("player", onlineQueuedPlayerProvider))
                .executes(VoidQueueCommand::pullPastQueue);

        CommandAPICommand flushCommand = new CommandAPICommand("flush")
                .withPermission(Constants.BASE_PERM + "flush")
                .withOptionalArguments(new BooleanArgument("force"))
                .executes(VoidQueueCommand::flushQueue);

        CommandAPICommand statusCommand = new CommandAPICommand("status")
                .withPermission(Constants.BASE_PERM + "status")
                .executes(VoidQueueCommand::status);

        CommandAPICommand queueCommand = new CommandAPICommand("q")
                .withSubcommand(pauseCommand)
                .withSubcommand(unpauseCommand)
                .withSubcommand(kickCommand)
                .withSubcommand(clearCommand)
                .withSubcommand(pullCommand)
                .withSubcommand(flushCommand)
                .withSubcommand(statusCommand);

        queueCommand.register(plugin);
    }

    private static int pause(CommandSource source, CommandArguments args) throws WrapperCommandSyntaxException {
        Optional<String> reason = args.getOptionalByClass("reason", String.class);
        if (reason.isEmpty()) {
            throw CommandAPI.failWithMessage(VelocityBrigadierMessage.tooltip(Component.translatable("queue.commands.usage.pause")));
        }

        VoidQueuePlugin voidQueuePlugin = VoidQueuePlugin.getInstance();
        VoidQueue voidQueue = voidQueuePlugin.getVoidQueue();
        if (voidQueue.hasPause(voidQueuePlugin)) {
            source.sendMessage(Component.translatable("queue.commands.error.pause.already-paused"));
            return 0;
        }

        voidQueue.addPause(voidQueuePlugin, reason.orElse(null));

        source.sendMessage(Component.translatable("queue.commands.info.pause.success"));
        return Command.SINGLE_SUCCESS;
    }

    private static int unpause(CommandSource source, CommandArguments args) throws WrapperCommandSyntaxException {
        VoidQueuePlugin voidQueuePlugin = VoidQueuePlugin.getInstance();
        if (!voidQueuePlugin.getVoidQueue().isPaused(voidQueuePlugin))
            throw CommandAPI.failWithMessage(VelocityBrigadierMessage.tooltip(Component.translatable("queue.commands.error.no-pause")));
        voidQueuePlugin.getVoidQueue().removePause(voidQueuePlugin);

        source.sendMessage(Component.translatable("queue.commands.info.unpause.success"));
        return Command.SINGLE_SUCCESS;
    }

    private static int clearQueue(CommandSource source, CommandArguments args) {
        VoidQueue voidQueue = VoidQueuePlugin.getInstance().getVoidQueue();

        voidQueue.getPlayerTracker().getTrackedPlayers().stream()
                .filter(trackedPlayer -> voidQueue.getQueueStore().isQueued(trackedPlayer))
                .forEach(trackedPlayer -> {
                    voidQueue.getQueueRouterService().kick(trackedPlayer,
                            Component.translatable("queue.errors.queue-cleared"));
                    voidQueue.getQueueStore().removePlayer(trackedPlayer);
                });

        source.sendMessage(Component.translatable("queue.commands.info.clear.success"));
        return Command.SINGLE_SUCCESS;
    }

    private static int kickFromQueue(CommandSource source, CommandArguments args) throws WrapperCommandSyntaxException {
        Optional<TrackedPlayer> trackedPlayer = args.getOptionalByClass("player", TrackedPlayer.class);
        if (trackedPlayer.isEmpty()) {
            throw CommandAPI.failWithMessage(VelocityBrigadierMessage.tooltip(Component.translatable("queue.commands.usage.kick")));
        }

        VoidQueue voidQueue = VoidQueuePlugin.getInstance().getVoidQueue();
        voidQueue.getQueueRouterService().kick(trackedPlayer.get(), Component.translatable("queue.errors.queue-removed"));
        voidQueue.getQueueStore().removePlayer(trackedPlayer.get());
        voidQueue.getPlayerTracker().unTrackPlayer(trackedPlayer.get());

        source.sendMessage(Component.translatable("queue.commands.info.kick.success",
                Argument.string("player", trackedPlayer.get().getPlayer().getUsername())));
        return Command.SINGLE_SUCCESS;
    }

    private static int pullPastQueue(CommandSource source, CommandArguments args) throws WrapperCommandSyntaxException {
        Optional<TrackedPlayer> trackedPlayer = args.getOptionalByClass("player", TrackedPlayer.class);
        if (trackedPlayer.isEmpty()) {
            throw CommandAPI.failWithMessage(VelocityBrigadierMessage.tooltip(Component.translatable("queue.commands.usage.pull")));
        }

        VoidQueue voidQueue = VoidQueuePlugin.getInstance().getVoidQueue();

        voidQueue.getQueueRouterService().routeToServer(trackedPlayer.get());
        voidQueue.getQueueStore().removePlayer(trackedPlayer.get());

        source.sendMessage(Component.translatable("queue.commands.info.pull.success",
                Argument.component("player", Component.text(trackedPlayer.get().getPlayer().getUsername()))));
        return Command.SINGLE_SUCCESS;
    }

    private static int flushQueue(CommandSource source, CommandArguments args) {
        boolean force = args.getByClassOrDefault("force", Boolean.class, false);

        VoidQueue voidQueue = VoidQueuePlugin.getInstance().getVoidQueue();
        QueueRouterService queueRouterService = voidQueue.getQueueRouterService();
        QueueStore queueStore = voidQueue.getQueueStore();


        for (QueueType queueType : QueueType.values()) {
            while (queueStore.hasNextIdleActive(queueType) && (!voidQueue.isServerFull(queueType) || force)) {

                TrackedPlayer trackedPlayer = queueStore.nextIdleActive(queueType).orElseThrow();
                queueRouterService.routeToServer(trackedPlayer);
                queueStore.removePlayer(trackedPlayer);
            }
        }

        source.sendMessage(Component.translatable("queue.commands.info.flush.success"));
        return Command.SINGLE_SUCCESS;
    }

    private static int status(CommandSource source, CommandArguments args) {
        VoidQueue voidQueue = VoidQueuePlugin.getInstance().getVoidQueue();

        String baseStatusMessage = """
                <aqua>Queue Status:
                    <aqua>Server: <yellow><in_game> / <max_connected>
                    <aqua>Queued: <yellow><queued> / <max_queued>
                        <aqua>Queued offline: <yellow><queued_offline>
                    <aqua>Pause count: <pause_count>""".stripIndent();


        Component status = MiniMessage.miniMessage().deserialize(baseStatusMessage,
                Placeholder.unparsed("in_game", String.valueOf(VoidQueuePlugin.getProxyServer().getAllPlayers().size())),
                Placeholder.unparsed("max_connected", String.valueOf(voidQueue.getConnectedPlayerLimit())),
                Placeholder.unparsed("pause_count", String.valueOf(voidQueue.getPauses().size())),
                Placeholder.unparsed("queued", String.valueOf(voidQueue.getQueueStore().getQueuedCount())),
                Placeholder.unparsed("max_queued", String.valueOf(voidQueue.getQueuedPlayerLimit().orElse(-1))),
                Placeholder.unparsed("queued_offline", String.valueOf(voidQueue.getQueueStore().getQueuedInActiveCount())));

        for (QueueType queueType : QueueType.values()) {
            status = status
                    .appendNewline()
                    .append(Component.text(queueType.getNameTitleCase() + ":"))
                    .appendNewline()
                    .append(voidQueue.getStatusMessage(queueType));
        }

        source.sendMessage(status);
        return Command.SINGLE_SUCCESS;
    }
}

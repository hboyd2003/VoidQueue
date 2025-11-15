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

package dev.hboyd.voidQueue.command.argument;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import dev.jorel.commandapi.CommandAPIHandler;
import dev.jorel.commandapi.arguments.CommandAPIArgumentType;
import dev.jorel.commandapi.arguments.GreedyArgument;
import dev.jorel.commandapi.arguments.SafeOverrideableArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import dev.hboyd.voidQueue.queues.TrackedPlayer;

import java.util.Collection;
import java.util.function.Supplier;

public class QueuePlayerArgument extends SafeOverrideableArgument<TrackedPlayer, String> implements GreedyArgument {
    private final Supplier<Collection<TrackedPlayer>> trackedPlayerSupplier;
    public static final DynamicCommandExceptionType ERROR_UNKNOWN_PLAYER = new DynamicCommandExceptionType(username ->
            VelocityBrigadierMessage.tooltip(Component.translatable("queue.commands.player-argument.error.target-unknown",
                    Argument.string("player", (String) username))));


    public QueuePlayerArgument(String nodeName, Supplier<Collection<TrackedPlayer>> trackedPlayerSupplier) {
        super(nodeName, StringArgumentType.greedyString(), s -> s);
        this.trackedPlayerSupplier = trackedPlayerSupplier;
        applySuggestions();
    }

    @Override
    public Class<TrackedPlayer> getPrimitiveType() {
        return TrackedPlayer.class;
    }

    @Override
    public CommandAPIArgumentType getArgumentType() {
        return CommandAPIArgumentType.PRIMITIVE_GREEDY_STRING;
    }

    @Override
    public <Source> TrackedPlayer parseArgument(CommandContext<Source> cmdCtx, String key, CommandArguments previousArgs) throws CommandSyntaxException {
        String input = CommandAPIHandler.getRawArgumentInput(cmdCtx, key);
        for (TrackedPlayer trackedPlayer : trackedPlayerSupplier.get()) {
            if (trackedPlayer.getPlayer().getUsername().equals(input)) return trackedPlayer;
        }

        throw ERROR_UNKNOWN_PLAYER.create(input);
    }

    private void applySuggestions() {
        super.replaceSuggestions((info, builder) -> {
            Collection<TrackedPlayer> trackedPlayers = trackedPlayerSupplier.get();
            for (TrackedPlayer trackedPlayer : trackedPlayers) {
                builder.suggest(trackedPlayer.getPlayer().getUsername());
            }
            return builder.buildFuture();
        });
    }
}

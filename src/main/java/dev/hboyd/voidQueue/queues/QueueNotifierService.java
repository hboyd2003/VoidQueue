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

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.jetbrains.annotations.NotNull;
import dev.hboyd.voidQueue.api.queues.QueueType;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

// TODO: Play sound on pause/unpause
public class QueueNotifierService {
    private static final Title.Times TITLE_TIMES =
            Title.Times.times(Ticks.duration(0),
                    Ticks.duration(100),
                    Ticks.duration(0));

    private final VoidQueue queue;
	private List<NotifyMethod> notifyMethods;

    public QueueNotifierService(VoidQueue queue, List<NotifyMethod> notifyMethods) {
		this.queue = queue;
		this.notifyMethods = notifyMethods;
	}

    /**
     * Notify the player that they are in the queue with a specified position
     *
     * @param player the player to check
     */
    public void notifyPosition(Player player, String baseTranslationKey, int position, int size) {
        for(NotifyMethod notifyMethod : notifyMethods) {
            StringBuilder key = new StringBuilder(baseTranslationKey);
            switch (notifyMethod) {
                case BOSSBAR -> {
                    break; // TODO: Re-implement bossbar notify method
                }
                case ACTIONBAR -> {
                    key.append(".actionbar").append(queue.isPaused() ? ".paused" : ".active");
                    player.sendActionBar(Component.translatable(key.toString(),
                            Argument.string("size", String.valueOf(position)),
                            Argument.string("pos", String.valueOf(size))));
                }
                case CHAT -> {
                    key.append(".chat").append(queue.isPaused() ? ".paused" : ".active");
                    player.sendActionBar(Component.translatable(key.toString(),
                            Argument.string("size", String.valueOf(position)),
                            Argument.string("pos", String.valueOf(size))));
                }
                case TITLE -> {
                    key.append(".title").append(queue.isPaused() ? ".paused" : ".active");
                    Title title = Title.title(Component.translatable(key + ".title"),
                            Component.translatable(key + ".subtitle",
                                    Argument.string("size", String.valueOf(position)),
                                    Argument.string("pos", String.valueOf(size))), TITLE_TIMES);
                    player.showTitle(title);
                }
            }
        }
    }

//    private void updateBossBar(QueuePlayer player) {
//        int position = player.getPosition();
//        String key = switch (player.getQueueType()) {
//            case STAFF -> queue.isPaused() ? "notify.staff.bossbar.paused" : "notify.staff.bossbar.active";
//            case PRIORITY -> queue.isPaused() ? "notify.priority.bossbar.paused" : "notify.priority.bossbar.active";
//            default -> queue.isPaused() ? "notify.normal.bossbar.paused" : "notify.normal.bossbar.active";
//        };
//
//        player.showBossBar();
//        player.getBossBar().name(
//                Messages.getComponent(key, Map.of(
//                                              "pos", String.valueOf(position),
//                                              "size", String.valueOf(queue.getQueuedCount(player.getQueueType()))),
//                                      Collections.emptyMap())).color(player.getBossBarColor());
//    }

	public List<NotifyMethod> getNotifyMethods() {
    	return notifyMethods;
	}

    public void notifyPause() {
        notifyAll(getPauseMessage());
    }

    public void notifyPause(@NotNull TrackedPlayer trackedPlayer) {
        trackedPlayer.getPlayer().sendMessage(getPauseMessage());

    }

    private Component getPauseMessage() {
        Map<PluginContainer, String> pauses = queue.getPauses();
        Component reasons = Component.translatable("queue.notify.paused");
        for (String reason : pauses.values()) {
            reasons = reasons.appendNewline().append(Component.text(" - " + reason));
        }

        return reasons;
    }

    public void notifyResume() {
        notifyAll(Component.translatable("queue.notify.unpaused"));
    }

    private void notifyAll(Component message) {
        QueueStore queueStore = queue.getQueueStore();
        PlayerTracker playerTracker = queue.getPlayerTracker();

        playerTracker.getTrackedPlayers().stream()
                .filter(queueStore::isQueued)
                .map(TrackedPlayer::getPlayer)
                .filter(InboundConnection::isActive)
                .forEach(p -> p.sendMessage(message));
    }

    public void notifyPositions() {
        notifyPositions(QueueType.NORMAL);
        notifyPositions(QueueType.PRIORITY);
        notifyPositions(QueueType.STAFF);
    }

    public void notifyPositions(QueueType queueType) {
        QueueStore queueStore = queue.getQueueStore();

        int queuedCount = queueStore.getQueuedCount(queueType);

        Iterator<TrackedPlayer> queueIterator = queueStore.getQueueIterator(queueType);
        for (int i = 1; queueIterator.hasNext(); i++) {
            TrackedPlayer trackedPlayer = queueIterator.next();
            notifyPosition(trackedPlayer.getPlayer(),  "queue.notify." + queueType.name().toLowerCase(), i, queuedCount);
        }

    }

    public void setNotifyMethods(List<NotifyMethod> notifyMethods) {
        this.notifyMethods = notifyMethods;
    }
}

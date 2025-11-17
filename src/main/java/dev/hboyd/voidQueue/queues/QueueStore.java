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

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import dev.hboyd.voidQueue.api.queues.QueueType;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class QueueStore {
    private final Logger logger;

    private final ConcurrentLinkedQueue<TrackedPlayer> normalQueue;
    private final ConcurrentLinkedQueue<TrackedPlayer> priorityQueue;
    private final ConcurrentLinkedQueue<TrackedPlayer> staffQueue;

    public QueueStore(Logger logger) {
        this.logger = logger;

        this.normalQueue = new ConcurrentLinkedQueue<>();
        this.priorityQueue = new ConcurrentLinkedQueue<>();
        this.staffQueue = new ConcurrentLinkedQueue<>();
    }

    public void addPlayer(@NotNull TrackedPlayer trackedPlayer,
                          @NotNull QueueType queueType) {
        if (priorityQueue.contains(trackedPlayer)
                || staffQueue.contains(trackedPlayer)
                || normalQueue.contains(trackedPlayer))
            throw new IllegalArgumentException("Cannot add a existing queued player to the queue");

        switch (queueType) {
            case NORMAL:
                normalQueue.add(trackedPlayer);
                break;
            case PRIORITY:
                priorityQueue.add(trackedPlayer);
                break;
            case STAFF:
                staffQueue.add(trackedPlayer);
        }
    }

    public void removePlayer(@NotNull TrackedPlayer trackedPlayer) {
        normalQueue.remove(trackedPlayer);
        priorityQueue.remove(trackedPlayer);
        staffQueue.remove(trackedPlayer);
    }

    public Optional<QueueType> getQueueType(TrackedPlayer trackedPlayer) {
        if (normalQueue.contains(trackedPlayer)) return Optional.of(QueueType.NORMAL);
        if (priorityQueue.contains(trackedPlayer)) return Optional.of(QueueType.PRIORITY);
        if (staffQueue.contains(trackedPlayer)) return Optional.of(QueueType.STAFF);

        return Optional.empty();
    }

    // TODO: hasNextIdleActive should probably should just be removed since it does the same thing as nextIdleActive
    public boolean hasNextIdleActive(QueueType queueType) {
        ConcurrentLinkedQueue<TrackedPlayer> queue;
        switch (queueType) {
            case NORMAL -> queue = normalQueue;
            case PRIORITY -> queue = priorityQueue;
            case STAFF -> queue = staffQueue;
            default -> throw new IllegalArgumentException("Unknown queue type");
        }
        for (TrackedPlayer trackedPlayer : queue) {
            Optional<TrackedPlayer.ConnectionState> connectionState = trackedPlayer.getConnectionState();
            if (trackedPlayer.getPlayer().isActive()
                    && connectionState.isPresent()
                    && connectionState.get() == TrackedPlayer.ConnectionState.LIMBO_JOIN)
                return true;

        }
        return false;
    }

    public TrackedPlayer peek(QueueType queueType) {
        return switch (queueType) {
            case NORMAL ->  normalQueue.peek();
            case PRIORITY -> priorityQueue.peek();
            case STAFF ->  staffQueue.peek();
        };
    }


    public Optional<TrackedPlayer> nextIdleActive(QueueType queueType) {
        ConcurrentLinkedQueue<TrackedPlayer> queue;
        switch (queueType) {
            case NORMAL -> queue = normalQueue;
            case PRIORITY -> queue = priorityQueue;
            case STAFF -> queue = staffQueue;
            default -> throw new IllegalArgumentException("Unknown queue type");
        }
        for (TrackedPlayer trackedPlayer : queue) {
            Optional<TrackedPlayer.ConnectionState> connectionState = trackedPlayer.getConnectionState();
            if (trackedPlayer.getPlayer().isActive()
                    && connectionState.isPresent()
                    && connectionState.get() == TrackedPlayer.ConnectionState.LIMBO_JOIN)
                return Optional.of(trackedPlayer);

        }
        return Optional.empty();
    }

    public boolean isQueued(TrackedPlayer trackedPlayer) {
        return normalQueue.contains(trackedPlayer)
                || priorityQueue.contains(trackedPlayer)
                || staffQueue.contains(trackedPlayer);
    }

    public int getQueuedCount(QueueType queueType) {
        return getQueue(queueType).size();
    }

    public int getQueuedActiveIdleCount(QueueType queueType) {
        ConcurrentLinkedQueue<TrackedPlayer> queue = getQueue(queueType);

        return (int) queue.stream().filter(trackedPlayer ->
                trackedPlayer.getPlayer().isActive()
                        && trackedPlayer.getConnectionState().isPresent()
                        && trackedPlayer.getConnectionState().get() == TrackedPlayer.ConnectionState.LIMBO_JOIN)
                .count();
    }

    public int getQueuedInActiveCount(QueueType queueType) {
        ConcurrentLinkedQueue<TrackedPlayer> queue = getQueue(queueType);

        return (int) queue.stream()
                .filter(trackedPlayer -> !trackedPlayer.getPlayer().isActive())
                .count();
    }

    public int getQueuedInActiveCount() {
        return getQueuedInActiveCount(QueueType.NORMAL)
                + getQueuedInActiveCount(QueueType.PRIORITY)
                + getQueuedInActiveCount(QueueType.STAFF);
    }

    private ConcurrentLinkedQueue<TrackedPlayer> getQueue(QueueType queueType) {
        return switch (queueType) {
            case NORMAL -> normalQueue;
            case PRIORITY -> priorityQueue;
            case STAFF -> staffQueue;
        };
    }

    public int getQueuedCount() {
        return normalQueue.size() + priorityQueue.size() + staffQueue.size();
    }

    public Iterator<TrackedPlayer> getQueueIterator(QueueType queueType) {
        return switch (queueType) {
            case NORMAL -> normalQueue.iterator();
            case PRIORITY -> priorityQueue.iterator();
            case STAFF -> staffQueue.iterator();
        };
    }
}

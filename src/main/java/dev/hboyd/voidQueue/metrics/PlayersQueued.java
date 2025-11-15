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

package dev.hboyd.voidQueue.metrics;

import de.sldk.mc.metrics.AbstractMetric;
import dev.hboyd.voidQueue.VoidQueuePlugin;
import io.prometheus.metrics.core.metrics.GaugeWithCallback;

import dev.hboyd.voidQueue.api.VoidQueue;
import dev.hboyd.voidQueue.api.queues.QueueType;
import dev.hboyd.voidQueue.queues.QueueStore;

public class PlayersQueued extends AbstractMetric {
    private static final GaugeWithCallback playersQueued = GaugeWithCallback.builder()
            .name(prefix("players_queued"))
            .help("Number of players queued by server and queue type")
            .labelNames("queue_type", "server")
            .callback(callback -> {
                VoidQueuePlugin plugin = VoidQueuePlugin.getInstance();
                QueueStore queueStore = plugin.getVoidQueue().getQueueStore();

                callback.call(queueStore.getQueuedCount(QueueType.NORMAL), "normal");
                callback.call(queueStore.getQueuedCount(QueueType.PRIORITY), "priority");
                callback.call(queueStore.getQueuedCount(QueueType.STAFF), "staff");
            })
            .build();

    public PlayersQueued(VoidQueue plugin) {
        super(plugin, playersQueued);
    }

    protected void initialValue() {
        playersQueued.collect();
    }
}

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

package dev.hboyd.voidQueue.api.events;

import com.velocitypowered.api.proxy.Player;
import dev.hboyd.voidQueue.api.queues.VoidQueue;

@SuppressWarnings("unused")
// TODO: Update to follow new implementation
public class PlayerQueueEvent {
	private final Player player;
    private final VoidQueue voidQueue;
	private String reason = null;
	private boolean cancelled = false;


	public PlayerQueueEvent(Player player, VoidQueue voidQueue) {
		this.player = player;
        this.voidQueue = voidQueue;
    }

	public Player getPlayer() {
		return player;
	}

	public String getReason() {
		return reason;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public void setReason(String reason) {
		this.reason = reason;
	}

    public VoidQueue getVoidQueue() {
        return voidQueue;
    }
}

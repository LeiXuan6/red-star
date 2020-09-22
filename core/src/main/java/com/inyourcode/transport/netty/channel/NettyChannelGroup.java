/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.inyourcode.transport.netty.channel;

import com.inyourcode.common.util.JConstants;
import com.inyourcode.common.util.JUnsafe;
import com.inyourcode.common.util.Lists;
import com.inyourcode.common.util.SystemClock;
import com.inyourcode.common.util.SystemPropertyUtil;
import com.inyourcode.common.util.atomic.AtomicUpdater;
import com.inyourcode.transport.api.UnresolvedAddress;
import com.inyourcode.transport.api.channel.JChannel;
import com.inyourcode.transport.api.channel.JChannelGroup;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * jupiter
 * org.jupiter.transport.netty.channel
 *
 * @author jiachun.fjc
 */
public class NettyChannelGroup implements JChannelGroup {

    private static long LOSS_INTERVAL = SystemPropertyUtil
            .getLong("jupiter.io.channel.group.loss.interval.millis", TimeUnit.MINUTES.toMillis(5));

    private static final AtomicReferenceFieldUpdater<CopyOnWriteArrayList, Object[]> channelsUpdater =
            AtomicUpdater.newAtomicReferenceFieldUpdater(CopyOnWriteArrayList.class, Object[].class, "array");
    private static final AtomicIntegerFieldUpdater<NettyChannelGroup> signalNeededUpdater =
            AtomicUpdater.newAtomicIntegerFieldUpdater(NettyChannelGroup.class, "signalNeeded");
    private static final AtomicIntegerFieldUpdater<NettyChannelGroup> indexUpdater =
            AtomicUpdater.newAtomicIntegerFieldUpdater(NettyChannelGroup.class, "index");

    private static final ThreadLocal<SimpleDateFormat> dateFormatThreadLocal = new ThreadLocal<SimpleDateFormat>() {

        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
        }
    };

    private final CopyOnWriteArrayList<NettyChannel> channels = new CopyOnWriteArrayList<>();

    // 连接断开时自动被移除
    private final ChannelFutureListener remover = new ChannelFutureListener() {

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            remove(NettyChannel.attachChannel(future.channel()));
        }
    };

    private final UnresolvedAddress address;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notifyCondition = lock.newCondition();
    // attempts to elide conditional wake-ups when the lock is uncontended.
    @SuppressWarnings("unused")
    private volatile int signalNeeded = 0; // 0: false, 1: true

    @SuppressWarnings("unused")
    private volatile int index = 0;
    private volatile int capacity = Integer.MAX_VALUE;
    private volatile int weight = JConstants.DEFAULT_WEIGHT; // the weight for this group
    private volatile int warmUp = JConstants.DEFAULT_WARM_UP; // warm-up time
    private volatile long timestamp = SystemClock.millisClock().now();
    private volatile long deadlineMillis = -1;

    public NettyChannelGroup(UnresolvedAddress address) {
        this.address = address;
    }

    @Override
    public UnresolvedAddress remoteAddress() {
        return address;
    }

    @Override
    public JChannel next() {
        for (;;) {
            // snapshot of channels array
            Object[] elements = channelsUpdater.get(channels);
            int length = elements.length;
            if (length == 0) {
                if (waitForAvailable(1000)) { // wait a moment
                    continue;
                }
                throw new IllegalStateException("no channel");
            }
            if (length == 1) {
                return (JChannel) elements[0];
            }

            int index = indexUpdater.getAndIncrement(this) & Integer.MAX_VALUE;

            return (JChannel) elements[index % length];
        }
    }

    @Override
    public List<? extends JChannel> channels() {
        return Lists.newArrayList(channels);
    }

    @Override
    public boolean isEmpty() {
        return channels.isEmpty();
    }

    @Override
    public boolean add(JChannel channel) {
        boolean added = channel instanceof NettyChannel && channels.add((NettyChannel) channel);
        if (added) {
            timestamp = SystemClock.millisClock().now(); // reset timestamp

            ((NettyChannel) channel).channel().closeFuture().addListener(remover);
            deadlineMillis = -1;

            if (signalNeededUpdater.getAndSet(this, 0) != 0) { // signal needed: true
                final ReentrantLock _look = lock;
                _look.lock();
                try {
                    notifyCondition.signalAll(); // must signal all
                } finally {
                    _look.unlock();
                }
            }
        }
        return added;
    }

    @Override
    public boolean remove(JChannel channel) {
        boolean removed = channel instanceof NettyChannel && channels.remove(channel);
        if (removed) {
            timestamp = SystemClock.millisClock().now(); // reset timestamp

            if (channels.isEmpty()) {
                deadlineMillis = SystemClock.millisClock().now() + LOSS_INTERVAL;
            }
        }
        return removed;
    }

    @Override
    public int size() {
        return channels.size();
    }

    @Override
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public boolean isAvailable() {
        return !channels.isEmpty();
    }

    @Override
    public boolean waitForAvailable(long timeoutMillis) {
        if (isAvailable()) {
            return true;
        }

        boolean available = false;
        long start = System.nanoTime();
        final ReentrantLock _look = lock;
        _look.lock();
        try {
            while (!isAvailable()) {
                signalNeededUpdater.set(this, 1); // set signal needed to true
                notifyCondition.await(timeoutMillis, TimeUnit.MILLISECONDS);

                available = isAvailable();

                if (available || (System.nanoTime() - start) > TimeUnit.MILLISECONDS.toNanos(timeoutMillis)) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            JUnsafe.throwException(e);
        } finally {
            _look.unlock();
        }

        return available;
    }

    @Override
    public int getWeight() {
        return weight > 0 ? weight : 0;
    }

    @Override
    public void setWeight(int weight) {
        this.weight = weight > JConstants.MAX_WEIGHT ? JConstants.MAX_WEIGHT : weight;
    }

    @Override
    public int getWarmUp() {
        return warmUp > 0 ? warmUp : 0;
    }

    @Override
    public void setWarmUp(int warmUp) {
        this.warmUp = warmUp;
    }

    @Override
    public boolean isWarmUpComplete() {
        return SystemClock.millisClock().now() - timestamp - warmUp > 0;
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public long deadlineMillis() {
        return deadlineMillis;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NettyChannelGroup that = (NettyChannelGroup) o;

        return address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public String toString() {
        SimpleDateFormat dateFormat = dateFormatThreadLocal.get();

        return "NettyChannelGroup{" +
                "channels=" + channels +
                ", weight=" + weight +
                ", warmUp=" + warmUp +
                ", time=" + dateFormat.format(new Date(timestamp)) +
                ", address=" + address +
                '}';
    }
}

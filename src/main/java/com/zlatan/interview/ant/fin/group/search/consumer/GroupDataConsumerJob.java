package com.zlatan.interview.ant.fin.group.search.consumer;

import com.zlatan.interview.ant.fin.group.search.domain.GroupData;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据消费作业
 *
 * @see GroupDataSortConsumer
 *
 * Created by Zlatan on 19/3/16.
 */
public class GroupDataConsumerJob<T extends GroupData> implements Runnable {

    private static long DEFAULT_READ_TIMEOUT_MS = 20L;

    /**
     * 阻塞队列
     */
    private final BlockingQueue<T> dataQueue;

    /**
     * 数据消费者
     */
    private final GroupDataSortConsumer<T> consumer;

    /**
     * 当前作业是否仍在工作状态
     */
    private final AtomicBoolean running;

    /**
     * 每次从队列拉取数据的超时时间
     */
    private final long readTimeoutMs;

    /**
     * 活跃的消费者作业计数器
     */
    private final AtomicInteger activeJobCounter;

    public GroupDataConsumerJob(BlockingQueue<T> dataQueue,
                                GroupDataSortConsumer<T> consumer,
                                int sorterCount) {
        this(dataQueue, consumer, DEFAULT_READ_TIMEOUT_MS, sorterCount);
    }

    public GroupDataConsumerJob(BlockingQueue<T> dataQueue,
                                GroupDataSortConsumer<T> consumer,
                                long readTimeoutMs,
                                int sorterCount) {
        this.dataQueue = dataQueue;
        this.consumer = consumer;
        this.running = new AtomicBoolean(true);
        this.readTimeoutMs = readTimeoutMs;
        this.activeJobCounter = new AtomicInteger(sorterCount);
    }

    @Override
    public void run() {
        while (!dataQueue.isEmpty() || running.get()) {
            T data = null;
            try {
                data = dataQueue.poll(readTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // 超时了, 继续尝试拉(如果还在reading状态)
            }
            if (data != null) {
                consumer.consume(data);
            }
        }
        if (activeJobCounter.decrementAndGet() <= 0) {
            // 没有活跃的作业了, 可以告知消费者任务已完结
            consumer.finish();
        }
    }

    /**
     * 释放生产者不再生产数据的信号,
     * 作业线程处理完工作可以完结生命周期了
     */
    public void signalNoInput() {
        running.compareAndSet(true, false);
    }
}

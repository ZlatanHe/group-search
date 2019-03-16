package com.zlatan.interview.ant.fin.group.search.sort;

import com.zlatan.interview.ant.fin.group.search.domain.Data;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Zlatan on 19/3/16.
 */
public class MinDataSortJob implements Runnable {

    private static long DEFAULT_READ_TIMEOUT_MS = 20L;

    private final BlockingQueue<Data> blockingQueue;

    private final MinDataSorter minDataSorter;

    private final AtomicBoolean reading;

    private final long readTimeoutMs;

    private final AtomicInteger workingSorterCount;

    public MinDataSortJob(BlockingQueue<Data> blockingQueue,
                          MinDataSorter minDataSorter,
                          int sorterCount) {
        this(blockingQueue, minDataSorter, DEFAULT_READ_TIMEOUT_MS, sorterCount);
    }

    public MinDataSortJob(BlockingQueue<Data> blockingQueue,
                          MinDataSorter minDataSorter,
                          long readTimeoutMs,
                          int sorterCount) {
        this.blockingQueue = blockingQueue;
        this.minDataSorter = minDataSorter;
        this.reading = new AtomicBoolean(true);
        this.readTimeoutMs = readTimeoutMs;
        this.workingSorterCount = new AtomicInteger(sorterCount);
    }

    @Override
    public void run() {
        while (!blockingQueue.isEmpty() || reading.get()) {
            Data data = null;
            try {
                data = blockingQueue.poll(readTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // 超时了, 继续尝试拉(如果还在reading状态)
            }
            if (data != null) {
                minDataSorter.add(data);
            }
        }
        if (workingSorterCount.decrementAndGet() <= 0) {
            minDataSorter.finish();
        }
    }

    /**
     * 释放生产者不再生产数据的信号,
     * 排序线程处理完工作可以完结生命周期了
     */
    public void signalNoInput() {
        reading.compareAndSet(true, false);
    }
}

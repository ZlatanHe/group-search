package com.zlatan.interview.ant.fin.group.search.consumer;

import com.zlatan.interview.ant.fin.group.search.domain.QuotaData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 支持并发的排序器
 * Created by Zlatan on 19/3/16.
 */
public class ConcurrentQuotaDataSorter
        implements GroupDataSortConsumer<QuotaData> {

    private static final int DEFAULT_GROUP_QUEUE_CAPACITY = 20;

    /**
     * 工作中
     */
    private static final int RUNNING = 0;
    /**
     * 接收到完结工作的信号, 正在处理还为处理完的数据（不再接收新数据）
     */
    private static final int TERMINATING = 1;
    /**
     * 工作已完成
     */
    private static final int FINISHED = 2;

    /*******************************************************************
     * 工作区
     *******************************************************************/

    /**
     * 工作状态
     * @see #RUNNING
     * @see #TERMINATING
     * @see #FINISHED
     */
    private final AtomicInteger runState;

    /**
     * 当前有几个数据正在处理
     */
    private final AtomicInteger processingDataCount;

    /**
     * 组号常量池, 组号常量的监视器可在排序时做互斥锁
     */
    private final Map<String, String> groupIdConstantPool;

    /**
     * 组号优先队列(最小堆)
     */
    private final PriorityQueue<String> groupIdQueue;

    /**
     * 最小值字典
     * key为组号
     */
    private final ConcurrentHashMap<String, QuotaData> minDataMap;

    /*******************************************************************
     * 结果区
     *******************************************************************/

    /**
     * 根据组号升序排序的各组最小值序列
     */
    private List<QuotaData> minDataListByGroup;

    public ConcurrentQuotaDataSorter() {
        this(DEFAULT_GROUP_QUEUE_CAPACITY);
    }

    public ConcurrentQuotaDataSorter(int groupIdQueueInitCapacity) {
        this.runState = new AtomicInteger(RUNNING);
        this.processingDataCount = new AtomicInteger(0);
        this.groupIdConstantPool = new HashMap<>();
        this.groupIdQueue = new PriorityQueue<>(groupIdQueueInitCapacity);
        this.minDataMap = new ConcurrentHashMap<>();
    }

    @Override
    public void add(QuotaData data) {
        if (runState.get() != RUNNING) {
            return;
        }
        processingDataCount.incrementAndGet();

        // 常量化组号
        String groupId = groupIdConstantPool.get(data.getGroupId());
        if (groupId == null) {
            synchronized (this) {
                // hashMap去重, 优先队列做最小堆调整
                if (!groupIdConstantPool.containsKey(data.getGroupId())) {
                    groupIdConstantPool.put(data.getGroupId(), data.getGroupId());
                    groupIdQueue.add(data.getGroupId());
                }
            }
            groupId = groupIdConstantPool.get(data.getGroupId());
        }

        QuotaData minData = minDataMap.get(groupId);
        if (minData == null || Float.compare(data.getQuota(), minData.getQuota()) < 0) {
            // 要更换最小值前先获取锁（以组号常量的监视器为锁）
            synchronized (groupId) {
                minData = minDataMap.get(groupId);
                if (minData != null && Float.compare(data.getQuota(), minData.getQuota()) >= 0) {
                    // double check
                    return;
                }
                minDataMap.put(groupId, data);
            }
        }

        if (processingDataCount.decrementAndGet() == 0
                && runState.get() == TERMINATING) {
            runState.compareAndSet(TERMINATING, FINISHED);
        }
    }

    @Override
    public QuotaData findMin(String groupId) {
        return minDataMap.get(groupId);
    }

    /**
     * 根据组号升序排序的各组最小值序列
     *
     * 当排序还未完成时, 将工作区数据拷贝出一份副本, 处理后返回
     * 当排序已经完成, 直接返回结果(第一次的时候要处理结果)
     *
     * @return 根据组号升序排序的各组最小值序列
     */
    @Override
    public List<QuotaData> listMinByGroup() {
        if (runState.get() != FINISHED) {
            final PriorityQueue<String> groupIdQueueCopy = new PriorityQueue<>(groupIdQueue);
            final Map<String, QuotaData> minDataMapCopy = new HashMap<>(minDataMap);
            List<QuotaData> result = new ArrayList<>();
            while (!groupIdQueueCopy.isEmpty()) {
                QuotaData quotaData;
                if ((quotaData = minDataMapCopy.get(groupIdQueueCopy.poll())) != null) {
                    result.add(quotaData);
                }
            }
            return result;
        }

        if (minDataListByGroup == null) {
            synchronized (this) {
                if (minDataListByGroup != null) {
                    return minDataListByGroup;
                }
                List<QuotaData> result = new ArrayList<>();
                while (!groupIdQueue.isEmpty()) {
                    result.add(minDataMap.get(groupIdQueue.poll()));
                }
                minDataListByGroup = Collections.unmodifiableList(result);
            }
        }
        return minDataListByGroup;
    }

    @Override
    public void finish() {
        runState.compareAndSet(RUNNING, TERMINATING);
        if (processingDataCount.get() <= 0) {
            runState.compareAndSet(TERMINATING, FINISHED);
        }
    }


    @Override
    public boolean isFinished() {
        return runState.get() == FINISHED;
    }
}

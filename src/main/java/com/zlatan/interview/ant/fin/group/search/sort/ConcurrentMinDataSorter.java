package com.zlatan.interview.ant.fin.group.search.sort;

import com.zlatan.interview.ant.fin.group.search.domain.Data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Zlatan on 19/3/16.
 */
public class ConcurrentMinDataSorter implements MinDataSorter {

    private static final int DEFAULT_GROUP_QUEUE_CAPACITY = 20;

    /*******************************************************************
     * 工作域
     *******************************************************************/
    private AtomicBoolean finish;

    /**
     * 组号常量池, 组号常量的监视器可在排序时做互斥锁
     */
    private final Map<String, String> groupIdConstantPool;

    private final PriorityQueue<String> groupIdQueue;

    private final ConcurrentHashMap<String, Data> minDataMap;

    /*******************************************************************
     * 结果域
     *******************************************************************/
    private List<Data> minDataListByGroup;

    public ConcurrentMinDataSorter() {
        this(DEFAULT_GROUP_QUEUE_CAPACITY);
    }

    public ConcurrentMinDataSorter(int groupIdQueueInitCapacity) {
        this.finish = new AtomicBoolean(false);
        this.groupIdConstantPool = new HashMap();
        this.groupIdQueue = new PriorityQueue<>(groupIdQueueInitCapacity);
        this.minDataMap = new ConcurrentHashMap<>();
    }

    @Override
    public void add(Data data) {
        if (finish.get()) {
            throw new IllegalStateException("已经完成排序");
        }

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

        Data minData = minDataMap.get(groupId);
        if (minData == null || Float.compare(data.getQuota(), minData.getQuota()) < 0) {
            // 要更换最小值前先获取锁
            synchronized (groupId) {
                minData = minDataMap.get(groupId);
                if (minData != null && Float.compare(data.getQuota(), minData.getQuota()) >= 0) {
                    // double check
                    return;
                }
                minDataMap.put(groupId, data);
            }
        }
    }

    @Override
    public Data findMin(String groupId) {
        return minDataMap.get(groupId);
    }

    @Override
    public List<Data> listMinByGroup() {
        if (!finish.get()) {
            throw new IllegalStateException("排序中");
        }
        if (minDataListByGroup == null) {
            synchronized (this) {
                if (minDataListByGroup != null) {
                    return minDataListByGroup;
                }
                List<Data> result = new ArrayList<>();
                while (!groupIdQueue.isEmpty()) {
                    result.add(minDataMap.get(groupIdQueue.poll()));
                }
                minDataListByGroup = result;
            }
        }
        return minDataListByGroup;
    }

    @Override
    public void finish() {
        finish.compareAndSet(false, true);
    }


    @Override
    public boolean isFinished() {
        return finish.get();
    }
}

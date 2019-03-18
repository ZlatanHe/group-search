package com.zlatan.interview.ant.fin.group.search.consumer;

import com.zlatan.interview.ant.fin.group.search.domain.GroupData;

import java.util.List;

/**
 * 分组数据排序器
 *
 * @see GroupData
 *
 * Created by Zlatan on 19/3/16.
 */
public interface GroupDataSorter<T extends GroupData> {

    /**
     * 向排序器中添加数据
     *
     * @param data 数据
     * @throws NullPointerException 如果数据为空
     */
    void add(T data);

    /**
     * 找出分组的最小值
     *
     * @param groupId 组号
     */
    T findMin(String groupId);

    /**
     * 升序列出每个组的最小值
     *
     */
    List<T> listMinByGroup();

    /**
     * 完成排序, 不再接受新数据
     */
    void finish();

    /**
     * 是否已经完成排序
     */
    boolean isFinished();
}

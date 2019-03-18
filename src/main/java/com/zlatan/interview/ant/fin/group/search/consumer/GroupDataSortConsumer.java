package com.zlatan.interview.ant.fin.group.search.consumer;

import com.zlatan.interview.ant.fin.group.search.domain.GroupData;

import java.util.List;

/**
 * Created by Zlatan on 19/3/18.
 */
public interface GroupDataSortConsumer<T extends GroupData>
        extends DataConsumer<T>, GroupDataSorter<T> {

    @Override
    default void consume(T data) {
        add(data);
    }
}

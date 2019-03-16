package com.zlatan.interview.ant.fin.group.search.sort;

import com.zlatan.interview.ant.fin.group.search.domain.Data;

import java.util.List;

/**
 * Created by Zlatan on 19/3/16.
 */
public interface MinDataSorter {

    void add(Data data);

    Data findMin(String groupId);

    List<Data> listMinByGroup();

    void finish();

    boolean isFinished();
}

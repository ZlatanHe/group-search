package com.zlatan.interview.ant.fin.group.search.provider;

import java.util.concurrent.BlockingQueue;

/**
 * 数据生产者
 * Created by Zlatan on 19/3/18.
 */
public interface DataProvider<T> {

    void provide(BlockingQueue<T> dataQueue);
}

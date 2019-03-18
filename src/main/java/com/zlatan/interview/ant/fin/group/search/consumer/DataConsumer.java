package com.zlatan.interview.ant.fin.group.search.consumer;

/**
 * Created by Zlatan on 19/3/18.
 */
public interface DataConsumer<T> {

    void consume(T data);
}

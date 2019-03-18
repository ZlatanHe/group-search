package com.zlatan.interview.ant.fin.group.search.consumer;

import com.zlatan.interview.ant.fin.group.search.domain.QuotaData;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Zlatan on 19/3/18.
 */
public class GroupDataConsumerJobTest {

    @Test
    public void test() throws Exception {
        BlockingQueue<QuotaData> dataQueue = new LinkedBlockingQueue<>();
        GroupDataSortConsumer<QuotaData> consumer = new ConcurrentQuotaDataSorter();
        int jobCount = 4;
        GroupDataConsumerJob<QuotaData> job = new GroupDataConsumerJob<QuotaData>(
                dataQueue,
                consumer,
                jobCount
        );
        ExecutorService threadPool = Executors.newFixedThreadPool(jobCount);
        for (int i = 0; i < jobCount; i++) {
            threadPool.execute(job);
        }
        Assert.assertEquals(
                ((AtomicInteger)ReflectionTestUtils.getField(job, "activeJobCounter")).get(),
                4
        );
        dataQueue.add(new QuotaData("1", "1", 10.1f));
        dataQueue.add(new QuotaData("2", "2", 90.9f));
        dataQueue.add(new QuotaData("3", "3", 78.1f));
        dataQueue.add(new QuotaData("4", "3", 77.4f));
        dataQueue.add(new QuotaData("5", "1", 45.3f));
        dataQueue.add(new QuotaData("6", "2", 39.4f));
        Assert.assertEquals(
                ((AtomicInteger)ReflectionTestUtils.getField(job, "activeJobCounter")).get(),
                4
        );
        job.signalNoInput();

        Thread.sleep(100);
        Assert.assertEquals(
                ((AtomicInteger)ReflectionTestUtils.getField(job, "activeJobCounter")).get(),
                0
        );
        Assert.assertTrue(consumer.isFinished());
    }
}

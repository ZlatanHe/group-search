package com.zlatan.interview.ant.fin.group.search.consumer;

import com.zlatan.interview.ant.fin.group.search.domain.QuotaData;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Zlatan on 19/3/18.
 */
@SuppressWarnings("unchecked")
public class ConcurrentQuotaDataSorterTest {

    private ExecutorService executorPool;

    @Before
    public void init() {
        executorPool = Executors.newFixedThreadPool(4);
    }

    @Test
    public void testNormal() throws Exception {
        ConcurrentQuotaDataSorter sorter = new ConcurrentQuotaDataSorter();
        Assert.assertEquals(
                ((AtomicInteger)ReflectionTestUtils.getField(sorter, "runState")).get(),
                0
        );
        executorPool.execute(() -> sorter.consume(new QuotaData("1", "1", 99.8f)));
        executorPool.execute(() -> sorter.consume(new QuotaData("2", "1", 29.8f)));
        executorPool.execute(() -> sorter.consume(new QuotaData("3", "3", 93.8f)));

        Thread.sleep(10);
        List<QuotaData> quotaDatas = sorter.listMinByGroup();
        Assert.assertEquals(quotaDatas.size(), 2);
        Assert.assertEquals(quotaDatas.get(0).getId(), "2");
        Assert.assertEquals(quotaDatas.get(1).getId(), "3");
        Assert.assertFalse(sorter.isFinished());

        executorPool.execute(() -> sorter.consume(new QuotaData("4", "1", 19.8f)));
        executorPool.execute(() -> sorter.consume(new QuotaData("5", "2", 59.8f)));
        executorPool.execute(() -> {
            sorter.consume(new QuotaData("6", "3", 34.8f));
            sorter.finish();
        });
        Assert.assertNotEquals(
                ((AtomicInteger)ReflectionTestUtils.getField(sorter, "runState")).get(),
                0
        );
        Thread.sleep(10);
        Map<String, QuotaData> minData =
                (Map<String, QuotaData>) ReflectionTestUtils.getField(sorter, "minDataMap");
        Assert.assertEquals(minData.size(), 3);
        quotaDatas = sorter.listMinByGroup();
        Assert.assertEquals(quotaDatas.size(), 3);
        Assert.assertEquals(quotaDatas.get(0).getId(), "4");
        Assert.assertEquals(quotaDatas.get(1).getId(), "5");
        Assert.assertEquals(quotaDatas.get(2).getId(), "6");
        Assert.assertTrue(sorter.isFinished());
    }
}

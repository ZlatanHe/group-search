package com.zlatan.interview.ant.fin.group.search.provider;

import com.zlatan.interview.ant.fin.group.search.domain.QuotaData;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Zlatan on 19/3/18.
 */
public class QuotaDataReaderTest {

    @Test
    public void testDefault() throws Exception {
        AtomicInteger atomicInteger = new AtomicInteger(1);
        BlockingQueue<QuotaData> dataQueue = new LinkedBlockingQueue<>();
        QuotaDataReader quotaDataReader = new QuotaDataReader(
                new File("src/test/resources/ut-file-1.txt"),
                dataQueue,
                atomicInteger
        );
        Runnable runnable = quotaDataReader::read;
        new Thread(runnable).start();
        Thread.sleep(100);

        Assert.assertTrue(quotaDataReader.isOwned());
        new Thread(runnable).start();
        Assert.assertTrue(atomicInteger.get() == 0);
        Assert.assertTrue(dataQueue.size() == 4);
    }

    @Test
    public void testIgnoreTheRemainData() {
        AtomicInteger atomicInteger = new AtomicInteger(1);
        BlockingQueue<QuotaData> dataQueue = new LinkedBlockingQueue<>();
        QuotaDataReader quotaDataReader = new QuotaDataReader(
                new File("src/test/resources/ut-file-1.txt"),
                QuotaDataReader.IllegalFormatPolicy.DISCARD_CURRENT_FILE,
                dataQueue,
                atomicInteger
        );
        quotaDataReader.read();
        Assert.assertTrue(quotaDataReader.isOwned());
        Assert.assertTrue(atomicInteger.get() == 0);
        Assert.assertTrue(dataQueue.size() == 1);
    }

    @Test
    public void testFileNotFound() {
        AtomicInteger atomicInteger = new AtomicInteger(1);
        BlockingQueue<QuotaData> dataQueue = new LinkedBlockingQueue<>();
        QuotaDataReader quotaDataReader = new QuotaDataReader(
                new File("invalidFilePath"),
                QuotaDataReader.IllegalFormatPolicy.DISCARD_CURRENT_FILE,
                dataQueue,
                atomicInteger
        );
        quotaDataReader.read();

        Assert.assertTrue(quotaDataReader.isOwned());
        Assert.assertTrue(atomicInteger.get() == 0);
        Assert.assertTrue(dataQueue.size() == 0);
    }
}

package com.zlatan.interview.ant.fin.group.search.read;

import com.zlatan.interview.ant.fin.group.search.domain.Data;
import lombok.AllArgsConstructor;

import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Zlatan on 19/3/16.
 */
public class DataReadJob implements Runnable {

    /**
     * 这个作业是否已经被分派
     */
    private volatile boolean owned = false;

    private final File file;

    private final IllegalFormatPolicy illegalFormatPolicy;

    private final BlockingQueue<Data> dataBlockingQueue;

    private final AtomicInteger remainFileCounter;

    public DataReadJob(File file,
                       IllegalFormatPolicy illegalFormatPolicy,
                       BlockingQueue<Data> dataBlockingQueue,
                       AtomicInteger remainFileCounter) {
        if (file == null) {
            throw new IllegalArgumentException("filePath is empty");
        }
        if (illegalFormatPolicy == null) {
            throw new NullPointerException("illegalFormatPolicy is null");
        }
        if (dataBlockingQueue == null) {
            throw new NullPointerException("dataBlockingQueue is null");
        }
        if (remainFileCounter == null) {
            throw new NullPointerException("remainFileCounter is null");
        }
        this.file = file;
        this.illegalFormatPolicy = illegalFormatPolicy;
        this.dataBlockingQueue = dataBlockingQueue;
        this.remainFileCounter = remainFileCounter;
    }

    @Override
    public void run() {
        if (owned) {
            return;
        }
        owned = true;

        try (FileReader fileReader = new FileReader(file);
             BufferedReader bufferedReader = new BufferedReader(fileReader)
        ) {
            String dataLine;
            while ((dataLine = bufferedReader.readLine()) != null) {
                try {
                    Data data = new Data(dataLine);
                    dataBlockingQueue.add(data);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    switch (illegalFormatPolicy) {
                        case DISCARD_CURRENT_FILE:
                            return;
                        case DISCARD_CURRENT_LINE:
                            break;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException ie) {
            ie.printStackTrace();
        }

        remainFileCounter.decrementAndGet();
    }

    /**
     * 遇到不合规的数据格式如何处理
     */
    @AllArgsConstructor
    public enum IllegalFormatPolicy {

        DISCARD_CURRENT_FILE("忽略这个文件"),
        DISCARD_CURRENT_LINE("忽略这一行")
        ;

        String desc;
    }
}

package com.zlatan.interview.ant.fin.group.search.provider;

import com.zlatan.interview.ant.fin.group.search.domain.QuotaData;
import lombok.AllArgsConstructor;

import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 指标数据阅读者（数据消息生产者, 继承 {@link DataProvider}）
 *
 * @see QuotaData
 * @see DataProvider
 * @see BlockingQueue
 * Created by Zlatan on 19/3/18.
 */
public class QuotaDataReader implements DataProvider<QuotaData> {

    /**
     * 这个文件的阅读任务是否已经被分派
     */
    private boolean owned = false;

    /**
     * 文件
     */
    private final File file;

    /**
     * 遇到不合规数据的处理方式
     * 目前两种：
     *  － 忽略整个文件
     *  － 忽略当前行（默认）
     */
    private final IllegalFormatPolicy illegalFormatPolicy;

    /**
     * 阻塞队列
     */
    private final BlockingQueue<QuotaData> dataQueue;

    /**
     * 剩余待完成读取的文件计数器
     */
    private final AtomicInteger remainFileCounter;

    public QuotaDataReader(File file,
                           BlockingQueue<QuotaData> dataQueue,
                           AtomicInteger remainFileCounter) {
        this(file, IllegalFormatPolicy.DISCARD_CURRENT_LINE, dataQueue, remainFileCounter);
    }

    public QuotaDataReader(File file,
                           IllegalFormatPolicy illegalFormatPolicy,
                           BlockingQueue<QuotaData> dataQueue) {
        this(file, illegalFormatPolicy, dataQueue, null);
    }

    public QuotaDataReader(File file,
                           IllegalFormatPolicy illegalFormatPolicy,
                           BlockingQueue<QuotaData> dataQueue,
                           AtomicInteger remainFileCounter) {
        if (file == null) {
            throw new IllegalArgumentException("filePath is empty");
        }
        if (dataQueue == null) {
            throw new NullPointerException("dataQueue is null");
        }
        this.remainFileCounter = remainFileCounter;
        this.file = file;
        this.illegalFormatPolicy =
                illegalFormatPolicy == null ? IllegalFormatPolicy.DISCARD_CURRENT_LINE : illegalFormatPolicy;
        this.dataQueue = dataQueue;
    }

    @Override
    public void provide(BlockingQueue<QuotaData> dataQueue) {
        // 确保
        if (owned) {
            return;
        }
        synchronized (this) {
            if (owned) {
                return;
            }
            owned = true;
        }
        try (FileReader fileReader = new FileReader(file);
             BufferedReader bufferedReader = new BufferedReader(fileReader)
        ) {
            String dataLine;
            while ((dataLine = bufferedReader.readLine()) != null) {
                try {
                    QuotaData data = QuotaData.build(dataLine);
                    dataQueue.add(data);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    switch (illegalFormatPolicy) {
                        // 错误数据处理
                        case DISCARD_CURRENT_FILE:
                            System.err.println("数据格式错误, 忽略当前文件: " + file.getName());
                            return;
                        case DISCARD_CURRENT_LINE:
                            break;
                        case TERMINATE_THE_TASK:
                            System.err.println("数据格式错误, 任务中止");
                            System.exit(2);
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

    public void read() {
        provide(dataQueue);
    }

    /**
     * 遇到不合规的数据格式的处理策略
     */
    @AllArgsConstructor
    public enum IllegalFormatPolicy {

        DISCARD_CURRENT_FILE("忽略这个文件"),
        DISCARD_CURRENT_LINE("忽略这一行"), // 默认
        TERMINATE_THE_TASK("关闭整个任务(关闭进程)")
        ;

        String desc;
    }
}

package com.zlatan.interview.ant.fin.group.search.task;

import com.zlatan.interview.ant.fin.group.search.consumer.ConcurrentQuotaDataSorter;
import com.zlatan.interview.ant.fin.group.search.consumer.GroupDataConsumerJob;
import com.zlatan.interview.ant.fin.group.search.consumer.GroupDataSortConsumer;
import com.zlatan.interview.ant.fin.group.search.domain.QuotaData;
import com.zlatan.interview.ant.fin.group.search.output.FilePrinter;
import com.zlatan.interview.ant.fin.group.search.provider.QuotaDataReadJob;
import com.zlatan.interview.ant.fin.group.search.provider.QuotaDataReader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Zlatan on 19/3/16.
 */
public final class GroupSearchTask {

    /**
     * 默认读取文件的线程数
     */
    private static final int DEFAULT_READER_THREAD_COUNT = 10;

    /**
     * 默认做数据消费的线程数
     */
    private static final int DEFAULT_SORTER_THREAD_COUNT = 1;

    /**
     * 默认目标文件前缀
     */
    private static final String DEFAULT_TARGET_FILE_PATH_PREFIX = "result";

    /***************************************************************
     * 配置项
     **************************************************************/

    private String dirPath;

    private String targetFilePath;

    private int readerThreadCount = DEFAULT_READER_THREAD_COUNT;

    private int sorterThreadCount = DEFAULT_SORTER_THREAD_COUNT;

    private QuotaDataReader.IllegalFormatPolicy illegalFormatPolicy;

    public GroupSearchTask setDirPath(String dirPath) {
        this.dirPath = dirPath;
        return this;
    }

    public GroupSearchTask setTargetFilePath(String targetFilePath) {
        this.targetFilePath = targetFilePath;
        return this;
    }

    public GroupSearchTask setReaderThreadCount(int readerThreadCount) {
        this.readerThreadCount = readerThreadCount;
        return this;
    }

    public GroupSearchTask setSorterThreadCount(int sorterThreadCount) {
        this.sorterThreadCount = sorterThreadCount;
        return this;
    }

    public GroupSearchTask setIllegalFormatPolicy(QuotaDataReader.IllegalFormatPolicy illegalFormatPolicy) {
        this.illegalFormatPolicy = illegalFormatPolicy;
        return this;
    }

    public void start() {
        // 配置检查
        if (dirPath == null || dirPath.length() == 0) {
            throw new NullPointerException("文件夹路径为空");
        }
        if (targetFilePath == null || targetFilePath.length() == 0) {
            targetFilePath = DEFAULT_TARGET_FILE_PATH_PREFIX + System.currentTimeMillis();
        }
        // 创建队列
        BlockingQueue<QuotaData> dataQueue = new LinkedBlockingQueue<>();
        // 排序器
        GroupDataSortConsumer<QuotaData> minDataSorter = new ConcurrentQuotaDataSorter();
        // 生产者
        List<File> validFiles = extractValidFile(new File(dirPath));
        if (validFiles.isEmpty()) {
            // 不存在有效文件, 结果就是个空列表
            new FilePrinter(targetFilePath).print(Collections.emptyList());
            return;
        }
        AtomicInteger remainFileCounter = new AtomicInteger(validFiles.size());
        ExecutorService readerPool = startCreateData(validFiles, dataQueue, remainFileCounter);
        // 消费者
        GroupDataConsumerJob<QuotaData> groupDataSortJob = new GroupDataConsumerJob<>(
                dataQueue,
                minDataSorter,
                sorterThreadCount
        );
        ExecutorService sorterPool = startConsumeData(groupDataSortJob);
        // 生产者停止生产数据后通知消费者, 并回收线程池
        while (remainFileCounter.get() > 0) {
            // spinning
        }
        groupDataSortJob.signalNoInput();
        readerPool.shutdown();
        // 消费者作业全部完成后获取结果并输出, 并回收线程池
        while (!minDataSorter.isFinished()) {
            // spinning
        }
        new FilePrinter(targetFilePath).print(minDataSorter.listMinByGroup());
        sorterPool.shutdown();
    }

    /**
     * 创建一个大小为 {@link #readerThreadCount} 的生产者线程池,
     * 遍历有效文件对象列表, 逐个创建数据读取作业, 并提交给线程池
     *
     * @param validFiles 有效文件对象列表
     * @param quotaDataQueue 数据阻塞队列
     * @param remainFileCounter 剩余待完成读取文件计数器（用于判断何时告知消费者, 线程生产者已经不再产生数据）
     * @return 生产者线程池
     */
    private ExecutorService startCreateData(List<File> validFiles,
                                            BlockingQueue<QuotaData> quotaDataQueue,
                                            AtomicInteger remainFileCounter) {
        ExecutorService readerPool = Executors.newFixedThreadPool(readerThreadCount);
        for (File validFile : validFiles) {
            readerPool.execute(new QuotaDataReadJob(
                    new QuotaDataReader(validFile, illegalFormatPolicy, quotaDataQueue, remainFileCounter)
            ));
        }
        return readerPool;
    }

    /**
     * 创建一个大小为 {@link #sorterThreadCount} 的消费者线程池, 执行相同的排序作业
     *
     * @param groupDataSortJob 分组对象排序作业
     * @return 消费者线程池
     */
    private ExecutorService startConsumeData(GroupDataConsumerJob<QuotaData> groupDataSortJob) {
        ExecutorService sorterPool = Executors.newFixedThreadPool(sorterThreadCount);
        for (int i = 0; i < sorterThreadCount; i++) {
            sorterPool.execute(groupDataSortJob);
        }
        return sorterPool;
    }

    /**
     * 获取有效的文件对象列表
     *
     * @param dir 目录对象
     * @return 后缀为.txt的文件对象列表
     */
    private List<File> extractValidFile(File dir) {
        if (!dir.exists()) {
            throw new IllegalArgumentException("文件夹不存在");
        }
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("请输入正确的文件夹路径");
        }

        List<File> validFiles = new ArrayList<>();
        for (File file : dir.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".txt")) {
                // 有效文件
                validFiles.add(file);
            }
        }
        return validFiles;
    }
}

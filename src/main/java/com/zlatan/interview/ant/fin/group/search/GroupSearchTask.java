package com.zlatan.interview.ant.fin.group.search;

import com.zlatan.interview.ant.fin.group.search.domain.Data;
import com.zlatan.interview.ant.fin.group.search.domain.Printable;
import com.zlatan.interview.ant.fin.group.search.read.DataReadJob;
import com.zlatan.interview.ant.fin.group.search.sort.ConcurrentMinDataSorter;
import com.zlatan.interview.ant.fin.group.search.sort.MinDataSortJob;
import com.zlatan.interview.ant.fin.group.search.sort.MinDataSorter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
public class GroupSearchTask {

    private static final int DEFAULT_READER_THREAD_COUNT = 10;

    private static final int DEFAULT_SORTER_THREAD_COUNT = 1;

    private static final DataReadJob.IllegalFormatPolicy DEFAULT_ILLEGAL_FORMAT_POLICY =
            DataReadJob.IllegalFormatPolicy.DISCARD_CURRENT_LINE;

    private static final String DEFAULT_TAGGET_FILE_PATH_PREFIX = "result";

    /***************************************************************
     * 配置项
     **************************************************************/

    private String dirPath;

    private String targetFilePath;

    private int readerThreadCount = DEFAULT_READER_THREAD_COUNT;

    private int sorterThreadCount = DEFAULT_SORTER_THREAD_COUNT;

    private DataReadJob.IllegalFormatPolicy illegalFormatPolicy = DEFAULT_ILLEGAL_FORMAT_POLICY;

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

    public GroupSearchTask setIllegalFormatPolicy(DataReadJob.IllegalFormatPolicy illegalFormatPolicy) {
        this.illegalFormatPolicy = illegalFormatPolicy;
        return this;
    }

    public void start() {
        // 配置检查
        if (dirPath == null || dirPath.length() == 0) {
            throw new NullPointerException("文件夹路径为空");
        }
        if (targetFilePath == null || targetFilePath.length() == 0) {
            targetFilePath = DEFAULT_TAGGET_FILE_PATH_PREFIX + System.currentTimeMillis() + ".txt";
        }

        // 创建队列
        BlockingQueue<Data> blockingQueue = new LinkedBlockingQueue<>();

        // 生产者
        File dir = new File(dirPath);
        validateDir(dir); // 校验是否是有效的目录
        List<File> validFiles = new ArrayList<>();
        for (File file : dir.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".txt")) {
                // 有效文件
                validFiles.add(file);
            }
        }
        if (validFiles.isEmpty()) {
            // 不存在有效文件, 结果就是个空列表
            generateTargetFile(Collections.emptyList());
            return;
        }
        ExecutorService readerPool = Executors.newFixedThreadPool(readerThreadCount);
        AtomicInteger remainFileCounter = new AtomicInteger(validFiles.size());
        for (File validFile : validFiles) {
            readerPool.execute(new DataReadJob(validFile, illegalFormatPolicy, blockingQueue, remainFileCounter));
        }

        // 消费者
        MinDataSorter minDataSorter = new ConcurrentMinDataSorter();
        MinDataSortJob minDataSortJob = new MinDataSortJob(
                blockingQueue,
                minDataSorter,
                sorterThreadCount
        );
        ExecutorService sorterPool = Executors.newFixedThreadPool(sorterThreadCount);
        for (int i = 0; i < sorterThreadCount; i++) {
            sorterPool.execute(minDataSortJob);
        }

        // 生产者停止生产数据后通知消费者
        while (remainFileCounter.get() > 0) {
        }
        minDataSortJob.signalNoInput();

        // 消费者作业全部完成后获取结果并输出
        while (!minDataSorter.isFinished()) {
        }
        generateTargetFile(minDataSorter.listMinByGroup());

        readerPool.shutdown();
        sorterPool.shutdown();
    }

    private void validateDir(File dir) {
        if (!dir.exists()) {
            throw new IllegalArgumentException("文件夹不存在");
        }
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("请输入正确的文件夹路径");
        }
    }

    private void generateTargetFile(List<? extends Printable> dataList) {
        final List<? extends Printable> list = dataList == null ? Collections.emptyList() : dataList;
        try (PrintWriter printWriter = new PrintWriter(new FileWriter(targetFilePath))) {
            for (Printable data : list) {
                printWriter.println(data.format());
            }
        } catch (IOException e) {
            // 结果写文件失败, 将结果和IO一场堆栈打到标准错误
            System.err.println("结果写文件失败, 文件名" + targetFilePath);
            e.printStackTrace();
            System.err.println("下面是结果");
            System.err.println(list);
        }
    }
}

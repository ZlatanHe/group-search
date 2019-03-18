package com.zlatan.interview.ant.fin.group.search.output;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;

/**
 * Created by Zlatan on 19/3/18.
 */
public class FilePrinter implements Printer {

    private String targetFilePath;

    public FilePrinter(String filePath) {
        if (filePath == null || filePath.length() == 0) {
            throw new IllegalArgumentException("targetFilePath is empty");
        }
        this.targetFilePath = filePath;
    }

    @Override
    public void print(Printable printable) {
        try (PrintWriter printWriter = new PrintWriter(new FileWriter(targetFilePath))) {
            printWriter.println(printable.format());
        } catch (IOException e) {
            // 结果写文件失败, 将结果和IO一场堆栈打到标准错误
            System.err.println("结果写文件失败, 文件名" + targetFilePath);
            e.printStackTrace();
            System.err.println("下面是结果");
            System.err.println(printable);
        }
    }

    @Override
    public void print(Collection<? extends Printable> printableCollection) {
        final Collection<? extends Printable> list =
                printableCollection == null ? Collections.emptyList() : printableCollection;
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

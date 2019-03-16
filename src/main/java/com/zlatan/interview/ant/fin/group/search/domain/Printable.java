package com.zlatan.interview.ant.fin.group.search.domain;

import java.io.PrintStream;

/**
 * Created by Zlatan on 19/3/16.
 */
public interface Printable {

    /**
     * 打印
     * @param printStream 打印流 {@link PrintStream}
     */
    default void print(PrintStream printStream) {
        printStream.println(format());
    }

    /**
     * 格式化成要打印的字符串
     */
    String format();
}

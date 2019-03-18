package com.zlatan.interview.ant.fin.group.search;

import com.zlatan.interview.ant.fin.group.search.task.GroupSearchTask;

public class Application {

	public static void main(String[] args) {
		new GroupSearchTask()
                .setDirPath("testData")
                .setTargetFilePath("testData/result/result" + System.currentTimeMillis())
                .setReaderThreadCount(10)
                .setSorterThreadCount(1)
                .start();
	}

}

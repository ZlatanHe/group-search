package com.zlatan.interview.ant.fin.group.search;

public class Application {

	public static void main(String[] args) {
		new GroupSearchTask()
                .setDirPath("/Users/zlatan/workspace/interview/test")
                .setTargetFilePath("/Users/zlatan/workspace/interview/test/result" + System.currentTimeMillis())
                .setSorterThreadCount(4)
                .start();
	}

}

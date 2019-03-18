# group-search

## 异常处理
由于本项目要求只能用jdk原生库编写，考虑到日志持久化并不是本项目的核心路径，
故大部分的错误信息打入进程标准错误中。

## 使用
使用类`com.zlatan.interview.ant.fin.group.search.task.GroupSearchTask`进行任务执行, 示例:

```
new GroupSearchTask()
                .setDirPath(String) // 文件夹路径
                .setTargetFilePath(String) // (可选项)结果文件路径,不填的时候默认打到工作目录,名字result加时间戳
                .setReaderThreadCount(int) // (可选项)读取文件线程数,默认10
                .setSorterThreadCount(int) // (可选项)排序线程数,默认1
                .setIllegalFormatPolicy(QuotaDataReader.IllegalFormatPolicy) // (可选项)默认忽略错误行并继续任务
                .start();
```
或参考`com.zlatan.interview.ant.fin.group.search.Application`

## 设计
### 生产者
![avatar](设计/生产者.JPEG)
### 消费者
![avatar](设计/消费者.JPEG)
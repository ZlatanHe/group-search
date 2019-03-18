package com.zlatan.interview.ant.fin.group.search.domain;

import com.zlatan.interview.ant.fin.group.search.output.Printable;
import lombok.Getter;

/**
 * 指标数据
 * Created by Zlatan on 19/3/16.
 */
@Getter
public class QuotaData extends GroupData implements Printable {

    private static final int COLUMNS = 3;

    private final String id;

    private final float quota;

    /**
     * 根据输入数据行构建实例
     * 如: 2000102,100,98.3
     * 数据行要求必须满足至少三列数据,以半角逗号','隔开,且最后一列为浮点数
     * 如果有第四列及更多数据，忽略
     *
     * @param dataLine 数据行
     * @throws NullPointerException 数据行为空时抛出
     * @throws IllegalArgumentException 数据行不满足上述要求时抛出
     */
    public static QuotaData build(String dataLine) {
        if (dataLine == null) {
            throw new NullPointerException("数据行为空");
        }
        String[] cols = dataLine.split(",");
        if (cols.length < COLUMNS) {
            throw new IllegalArgumentException("数据行不足三列数据: " + dataLine);
        }

        float quota;
        try {
            quota = Float.parseFloat(cols[2]);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("quota数据列格式错误: " + cols[3]);
        }
        return new QuotaData(cols[0], cols[1], quota);
    }

    private QuotaData(String id, String groupId, float quota) {
        super(groupId);
        this.id = id;
        this.quota = quota;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        QuotaData quota1 = (QuotaData) o;
        if (Float.compare(quota1.quota, quota) != 0) {
            return false;
        }
        if (id != null ? !id.equals(quota1.id) : quota1.id != null) {
            return false;
        }

        return getGroupId() != null ? getGroupId().equals(quota1.getGroupId()) : quota1.getGroupId() == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (getGroupId() != null ? getGroupId().hashCode() : 0);
        result = 31 * result + (quota != +0.0f ? Float.floatToIntBits(quota) : 0);
        return result;
    }

    @Override
    public String format() {
        return getGroupId() + "," + id + "," + quota;
    }

    @Override
    public String toString() {
        return "QuotaData{" +
                "id='" + id + '\'' +
                ", groupId='" + getGroupId() + '\'' +
                ", quota=" + quota +
                '}';
    }
}

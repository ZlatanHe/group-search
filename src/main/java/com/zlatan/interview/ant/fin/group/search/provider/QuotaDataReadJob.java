package com.zlatan.interview.ant.fin.group.search.provider;

/**
 * Created by Zlatan on 19/3/16.
 */
public class QuotaDataReadJob implements Runnable {

    private QuotaDataReader quotaDataReader;

    public QuotaDataReadJob(QuotaDataReader quotaDataReader) {
        if (quotaDataReader == null) {
            throw new NullPointerException("quotaDataReader is null");
        }
        this.quotaDataReader = quotaDataReader;
    }

    @Override
    public void run() {
        quotaDataReader.read();
    }
}

package com.zlatan.interview.ant.fin.group.search.domain;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Zlatan on 19/3/18.
 */
public class QuotaDataTest {

    @Test
    public void testNormal() {
        QuotaData quotaData = QuotaData.build("20180102, 100, 98.334");
        Assert.assertNotNull(quotaData);
        Assert.assertEquals(quotaData.getId(), "20180102");
        Assert.assertEquals(quotaData.getGroupId(), "100");
        Assert.assertTrue(Float.compare(98.334f, quotaData.getQuota()) == 0);
    }

    @Test(expected = NullPointerException.class)
    public void testNullDataLine() {
        QuotaData.build(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEnoughDataCols() {
        QuotaData.build("201293894834");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNumberFormatException() {
        QuotaData.build("201293894834, 100, abcd");
    }
}

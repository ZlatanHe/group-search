package com.zlatan.interview.ant.fin.group.search.domain;

import lombok.Getter;

/**
 * Created by Zlatan on 19/3/18.
 */
@Getter
public abstract class GroupData {

    private final String groupId;

    public GroupData(String groupId) {
        this.groupId = groupId;
    }
}

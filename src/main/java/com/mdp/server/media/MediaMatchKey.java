package com.mdp.server.media;

import java.util.Objects;

public class MediaMatchKey {

    private final String content;
    private final String tableNum;
    private final String userId;
    private final String lightNum;

    public MediaMatchKey(String content, String tableNum, String userId, String lightNum) {
        this.content = content;
        this.tableNum = tableNum;
        this.userId = userId;
        this.lightNum = lightNum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MediaMatchKey that)) return false;
        return Objects.equals(content, that.content)
                && Objects.equals(tableNum, that.tableNum)
                && Objects.equals(userId, that.userId)
                && Objects.equals(lightNum, that.lightNum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, tableNum, userId, lightNum);
    }

    @Override
    public String toString() {
        return "MediaMatchKey{" +
                "content='" + content + '\'' +
                ", tableNum='" + tableNum + '\'' +
                ", userId='" + userId + '\'' +
                ", lightNum='" + lightNum + '\'' +
                '}';
    }
}
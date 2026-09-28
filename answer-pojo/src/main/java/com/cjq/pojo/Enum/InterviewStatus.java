package com.cjq.pojo.Enum;

public enum InterviewStatus {
    IN_PROGRESS("进行中"),
    COMPLETED("已完成"),
    TERMINATED("已终止"),
    ABANDONED("已放弃");

    private final String description;

    InterviewStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

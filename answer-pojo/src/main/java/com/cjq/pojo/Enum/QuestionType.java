package com.cjq.pojo.Enum;

/**
 * 题目类型
 */
public enum QuestionType {
    TECHNICAL("技术题"),       // 八股文、原理、场景设计
    BEHAVIORAL("行为题"),      // 项目经验、团队协作、冲突处理
    CODING("编程题"),          // 在线写代码
    FOLLOW_UP("追问");         // 根据回答的追问

    private final String description;

    QuestionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

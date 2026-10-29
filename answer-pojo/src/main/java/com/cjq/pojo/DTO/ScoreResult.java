package com.cjq.pojo.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreResult {

    /** 评分 0-10 */
    private int score;

    /** 一句话评价 */
    private String comment;

    /** 是否需要追问 */
    private boolean shouldFollowUp;

    /** 追问原因（shouldFollowUp=true 时有值） */
    private String followUpReason;

    /**
     * 兜底工厂方法 — AI 评分失败时使用
     */
    public static ScoreResult fallback() {
        return ScoreResult.builder()
                .score(5)
                .comment("AI 评分服务暂不可用，默认给分")
                .shouldFollowUp(false)
                .followUpReason("")
                .build();
    }

    /**
     * 钳制分数到 0-10 范围（防止 AI 乱给分）
     */
    public void clampScore() {
        if (score < 0) score = 0;
        if (score > 10) score = 10;
    }
}

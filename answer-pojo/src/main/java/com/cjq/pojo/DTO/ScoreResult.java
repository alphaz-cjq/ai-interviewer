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

    /*
    * 1.因为你在类上加了 @Data 注解。Lombok 的 @Data 相当于一口气包含了 @Getter、@Setter、@ToString 等注解。
*2.编译前（你看到的 .java 文件）：只有 private boolean shouldFollowUp; 这一行字段定义。
*3.编译后（JVM 运行的 .class 字节码）：Lombok 的注解处理器会在编译期介入，
* 自动生成 isShouldFollowUp() 和 setShouldFollowUp(boolean) 方法，并塞进 .class 文件里。
*4.所以，你的 IDE（如 IDEA）之所以能识别 scoreResult.isShouldFollowUp() 不报红，是因为 IDE
* 自带的 Lombok 插件解析了注解，在编辑器层面“模拟”出了这个方法的存在，
* 但实际上，.java 文本里确实没有这行代码。
    * */

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

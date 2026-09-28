package com.cjq.util;

/**
 * JSON 工具类 — 清洗 AI 返回的 JSON 字符串
 */
public class JsonUtils {

    /**
     * 清洗 AI 返回的 JSON 字符串（去除 Markdown 代码块，提取花括号内容）
     */
    public static String cleanJsonResponse(String rawResponse) {
        if (rawResponse == null) {
            return "{}";
        }
        String clean = rawResponse.trim();
        // 去除 Markdown 代码块标记（```json ... ```）
        if (clean.startsWith("```")) {
            clean = clean.replaceAll("```[a-zA-Z]*\\s*", "")
                    .replaceAll("```$", "")
                    .trim();
        }
        // 提取第一个 { 到最后一个 }
        int firstBrace = clean.indexOf("{");
        int lastBrace = clean.lastIndexOf("}");
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            clean = clean.substring(firstBrace, lastBrace + 1);
        }
        return clean;
    }
}

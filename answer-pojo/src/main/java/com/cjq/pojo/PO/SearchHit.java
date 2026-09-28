package com.cjq.pojo.PO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
* 相似度
* */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchHit {
    private String content;//文本内容
    private double score;//距离分数（越低越相似，0，表示完全相同）
}

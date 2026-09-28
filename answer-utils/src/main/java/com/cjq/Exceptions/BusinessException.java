package com.cjq.Exceptions;


import lombok.Getter;

/*
* 自定义业务异常
*           ：1.不能直接转为Json格式返回给前端
*               2.用来描述出了什么错
*               3.不懂HTTP、JSON、Response
* */

@Getter
/* Getter:
    1.自动生成所有private字段的get方法
    2.只方便拿数据，不参与异常处理
    */


public class BusinessException extends RuntimeException{
   private Integer code;//状态码

    public BusinessException(String message){
        //一个只需要传错误信息的构造方法

        super(message);//继承父类的构造方法
        this.code=500;//自定义异常信息
    }
    public BusinessException(Integer code,String message){
        super(message);//继承父类的构造方法
        this.code=code;//自定义异常信息
    }
}

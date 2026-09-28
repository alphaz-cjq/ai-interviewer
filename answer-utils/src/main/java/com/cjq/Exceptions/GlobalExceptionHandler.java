package com.cjq.Exceptions;


import com.cjq.pojo.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/*
* 全局异常处理 （兜底）
*              转成Result返回给前端
* */
@Slf4j
@RestControllerAdvice//ResponseBody 返回值自动转为JSON格式
public class GlobalExceptionHandler {
    // 处理业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e){
        log.error("业务异常：{}", e.getMessage(), e);
        return Result.error(e.getCode(), e.getMessage());
    }
    // 处理参数校验异常（@Valid @RequestBody 校验失败时抛出）
    // 注意：必须返回真正的 HTTP 400，否则前端用原生 fetch 的 SSE 接口会把它当 200 处理，
    //       然后拿 JSON 去解析 SSE，解析不出任何事件，表现为"卡死"且没有报错。
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(MethodArgumentNotValidException e){
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        log.error("参数校验失败：{}", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(400, msg));
    }
    // 处理其他异常
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e){
        log.error("系统异常", e);
        return Result.error("服务器内部错误");
    }
}

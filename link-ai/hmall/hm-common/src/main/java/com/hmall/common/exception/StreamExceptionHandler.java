// 文件路径: E:/private_project/LinkAI/hmall/hm-common/src/main/java/com/hmall/common/exception/StreamExceptionHandler.java

package com.hmall.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Flux;

import java.io.IOException;

/**
 * 流式输出（SSE/Server-Sent Events）专用异常处理器
 * <p>
 * 处理客户端断开连接时产生的异常，避免这些正常的中断行为被当作错误记录
 */
@Slf4j
@RestControllerAdvice
public class StreamExceptionHandler {

    /**
     * 处理客户端主动断开连接的异常
     * <p>
     * 当前端点击停止或关闭页面时，后端正在写入的数据无法发送，
     * 此时会抛出 ClientAbortException，这是正常现象，不应记录为错误
     */
    @ExceptionHandler(ClientAbortException.class)
    @ResponseStatus(HttpStatus.OK)
    public Flux<String> handleClientAbort(ClientAbortException e) {
        log.debug("Client disconnected (normal behavior): {}", e.getMessage());
        return Flux.empty();
    }

    /**
     * 处理 I/O 异常，通常也是客户端断开连接导致
     */
    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.OK)
    public Flux<String> handleIOException(IOException e) {
        log.debug("I/O error (likely client disconnect): {}", e.getMessage());
        return Flux.empty();
    }

    /**
     * 处理异步请求不可用异常
     */
    @ExceptionHandler(org.springframework.web.context.request.async.AsyncRequestNotUsableException.class)
    @ResponseStatus(HttpStatus.OK)
    public Flux<String> handleAsyncRequestNotUsable(
            org.springframework.web.context.request.async.AsyncRequestNotUsableException e) {
        log.debug("Async request not usable (likely client disconnect): {}", e.getMessage());
        return Flux.empty();
    }
}
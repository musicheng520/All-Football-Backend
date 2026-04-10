package com.msc.handler;

import com.msc.constant.MessageConstant;
import com.msc.exception.BaseException;
import com.msc.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Business exception (custom)
     */
    @ExceptionHandler(BaseException.class)
    public Result<?> handleBaseException(BaseException ex) {
        log.error("Business exception: {}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * Runtime exception (e.g. wrong password)
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception: {}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * SQL constraint exception
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public Result<?> handleSqlIntegrityViolation(SQLIntegrityConstraintViolationException ex) {
        String message = ex.getMessage();
        log.error("SQL constraint violation: {}", message);

        if (message != null && message.contains("Duplicate entry")) {
            return Result.error(MessageConstant.ALREADY_EXISTS);
        }
        return Result.error(MessageConstant.UNKNOWN_ERROR);
    }

    /**
     * Global fallback exception
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return Result.error(MessageConstant.UNKNOWN_ERROR);
    }
}
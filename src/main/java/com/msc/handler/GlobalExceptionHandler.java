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

    @ExceptionHandler(BaseException.class)
    public Result<?> handleBaseException(BaseException ex) {
        log.error("Business exception: {}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public Result<?> handleSqlIntegrityViolation(SQLIntegrityConstraintViolationException ex) {
        String message = ex.getMessage();
        log.error("SQL constraint violation: {}", message);

        if (message != null && message.contains("Duplicate entry")) {
            return Result.error(MessageConstant.ALREADY_EXISTS);
        }
        return Result.error(MessageConstant.UNKNOWN_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return Result.error(MessageConstant.UNKNOWN_ERROR);
    }
}
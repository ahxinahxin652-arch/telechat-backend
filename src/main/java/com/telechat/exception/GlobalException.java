/**
 * 功能: 全局异常处理类
 * 作者: 那不勒斯的老大
 * 日期: 2025/8/18 下午2:04
 */
package com.telechat.exception;


import com.telechat.constant.ExceptionConstant;
import com.telechat.exception.exceptions.*;
import com.telechat.pojo.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;


@RestControllerAdvice
@Slf4j
public class GlobalException {
    /**
     * 参数校验
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMsg = ex.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("; "));

        return Result.error(ExceptionConstant.Judge_Query_Exception_Code,
                ExceptionConstant.Judge_Query_Exception_MSG + errorMsg);
    }

    /**
     * 处理自定义的邮箱异常
     */
    @ExceptionHandler(value = {EmailException.class})
    public Result<String> emailExceptionHandler(HttpServletRequest request, EmailException e) {
        log.info("邮箱异常,请求地址:{},错误信息:{}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(value = {RegisterException.class})
    public Result<String> registerExceptionHandler(HttpServletRequest request, RegisterException e) {
        log.info("注册异常,请求地址:{},错误信息:{}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(value = {VerifyCodeException.class})
    public Result<String> verifyCodeExceptionHandler(HttpServletRequest request, VerifyCodeException e) {
        log.info("验证码异常,请求地址:{},错误信息:{}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
    @ExceptionHandler(value = {LoginException.class})
    public Result<String> loginExceptionHandler(HttpServletRequest request, LoginException e) {
        log.info("登录异常,请求地址:{},错误信息:{}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(value = {FileException.class})
    public Result<String> fileExceptionHandler(HttpServletRequest request, FileException e) {
        log.info("文件异常,请求地址:{},错误信息:{}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(value = {ContactException.class})
    public Result<String> contactExceptionHandler(HttpServletRequest request, ContactException e) {
        log.info("联系人异常,请求地址:{},错误信息:{}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

}
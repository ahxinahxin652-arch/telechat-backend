package com.telechat.annotation.aspect;

import com.telechat.annotation.FrequencyLock;
import com.telechat.constant.ExceptionConstant;
import com.telechat.exception.exceptions.ContactException;
import com.telechat.exception.exceptions.FrequencyException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@Order(1)
public class FrequencyLockAspect {

    @Autowired
    private RedissonClient redissonClient;

    private final ExpressionParser parser = new SpelExpressionParser();

    // 修复：使用标准反射发现器
    private final ParameterNameDiscoverer discoverer = new StandardReflectionParameterNameDiscoverer();

    @Around("@annotation(frequencyLock)")
    public Object around(ProceedingJoinPoint joinPoint, FrequencyLock frequencyLock) throws Throwable {
        String lockKey = parseSpelKey(joinPoint, frequencyLock.key());
        RLock lock = redissonClient.getLock(lockKey);

        boolean isLocked = false;
        try {
            if (frequencyLock.waitTime() > 0) {
                isLocked = lock.tryLock(frequencyLock.waitTime(), frequencyLock.leaseTime(), frequencyLock.unit());
            } else {
                isLocked = lock.tryLock(0, frequencyLock.leaseTime(), frequencyLock.unit());
            }

            if (!isLocked) {
                log.warn("频率限制: key={}", lockKey);
                throw new FrequencyException(ExceptionConstant.TOO_BUSY_CODE, frequencyLock.msg());
            }

            return joinPoint.proceed();
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String parseSpelKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        if (!keyExpression.contains("#")) {
            return keyExpression;
        }

        // 使用新的 discoverer 获取参数名
        String[] paramNames = discoverer.getParameterNames(method);

        // 健壮性检查：如果没开启 -parameters 编译参数，这里可能返回 null
        if (paramNames == null || paramNames.length == 0) {
            log.warn("无法解析方法参数名，请确保编译开启了 -parameters 选项。Method: {}", method.getName());
            return keyExpression;
        }

        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        Expression expression = parser.parseExpression(keyExpression);
        Object value = expression.getValue(context);
        return value != null ? value.toString() : "";
    }
}
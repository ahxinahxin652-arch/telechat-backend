package com.telechat.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class AfterCommitUtil {
    /**
     * 数据库事务提交后执行：
     */
    public static void executeAfterCommit(Runnable runnable) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runnable.run();
                }
            });
        } else {
            // 如果当前没有事务，直接执行
            runnable.run();
        }
    }
}

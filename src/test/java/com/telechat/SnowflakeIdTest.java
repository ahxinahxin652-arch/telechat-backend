package com.telechat;

import com.telechat.util.SnowflakeIdGenerator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SnowflakeIdTest {

    public static void main(String[] args) throws InterruptedException {
        // 1. 初始化生成器 (数据中心ID=1, 机器ID=1)
        SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(1, 1);

        // 2. 准备并发参数
        int taskCount = 500; // 模拟500次生成请求
        int threadPoolSize = 50; // 线程池大小
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        
        // 使用线程安全的 Set 存储生成的 ID，用于去重检查
        Set<Long> idSet = ConcurrentHashMap.newKeySet();
        
        // 倒计数锁，用于等待所有线程执行完毕
        CountDownLatch latch = new CountDownLatch(taskCount);

        System.out.println("开始并发生成 " + taskCount + " 个 ID...");
        long startTime = System.currentTimeMillis();

        // 3. 提交任务
        for (int i = 0; i < taskCount; i++) {
            executorService.submit(() -> {
                try {
                    // 调用 nextId 生成 ID
                    long id = idGenerator.nextId();
                    // 将 ID 放入 Set
                    idSet.add(id);
                    // 打印当前线程生成的ID（可选，看控制台会比较乱）
                    System.out.println("Thread-" + Thread.currentThread().getId() + " generated: " + id);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // 任务完成，计数器减1
                    latch.countDown();
                }
            });
        }

        // 4. 等待所有任务完成
        latch.await();
        long endTime = System.currentTimeMillis();
        
        // 5. 关闭线程池
        executorService.shutdown();

        // 6. 验证结果
        System.out.println("所有任务执行完毕，耗时: " + (endTime - startTime) + "ms");
        System.out.println("预期生成数量: " + taskCount);
        System.out.println("实际去重后数量: " + idSet.size());

        if (idSet.size() == taskCount) {
            System.out.println("✅ 测试通过：所有生成的 ID 均为唯一！");
        } else {
            System.out.println("❌ 测试失败：发现重复 ID，重复数量: " + (taskCount - idSet.size()));
        }
    }
}
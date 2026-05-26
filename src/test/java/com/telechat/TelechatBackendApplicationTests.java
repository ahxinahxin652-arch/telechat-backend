package com.telechat;

import com.telechat.util.AliOssUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TelechatBackendApplicationTests {
    @Autowired
    private AliOssUtil aliOssUtil;

    @Test
    void test() {
        aliOssUtil.deleteByUrl("https://nabelese-telechat.oss-cn-hangzhou.aliyuncs.com/d0654fce-03d4-4bb9-b552-77eaa6c9f59b.png");
        System.out.println(System.currentTimeMillis());
    }

    @Test
    void contextLoads() {
    }

}

/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/6/19 下午4:43
 */
package com.telechat.controller.common;

import com.telechat.pojo.result.Result;
import com.telechat.util.AliOssUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/common")
@Slf4j
@Tag(name = "通用接口")
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;

    /*
    * 上传文件
    * */
    @PostMapping("/upload")
    @Operation(summary = "文件上传")
    public Result<String> upload(MultipartFile file) {
        log.info("文件上传：{}",file);

        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return Result.error(400,"文件为空");
            }
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

            String objectName = UUID.randomUUID() + extension;


            String filePath =  aliOssUtil.upload(file.getBytes(),objectName);
            return Result.success(filePath);
        } catch (IOException e) {
            log.error("文件上传失败：{}",e.getMessage(),e);
        }

        return Result.error(500,"上传失败");
    }

    /*
    * 文件下载
    * */
    @PostMapping("/download")
    @Operation(summary = "文件下载")
    public void download(String name){
        log.info("文件下载：{}",name);
    }

    /*
    * 文件删除
    * */
    @PostMapping("/delete")
    @Operation(summary = "文件删除")
    public void delete(String name){
        log.info("文件删除：{}",name);
        aliOssUtil.delete(name);
    }
}

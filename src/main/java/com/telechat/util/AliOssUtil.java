package com.telechat.util;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.telechat.constant.ExceptionConstant;
import com.telechat.exception.exceptions.FileException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

@Data
@AllArgsConstructor
@Slf4j
public class AliOssUtil {
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    // 最大头像大小 10MB
    private Long maxAvatarSize;

    public String uploadFile(MultipartFile file){
        log.info("文件上传：{}",file);
        // 校验文件大小
        if (file.getSize() > maxAvatarSize) {
            throw new FileException(ExceptionConstant.FileExceededMaxSizeCode, ExceptionConstant.FileExceededMaxSizeMsg);
        }
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null){
                throw new FileNotFoundException(ExceptionConstant.FileNotFoundExceptionMsg);
            }
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String objectName = UUID.randomUUID() + extension;
            return this.upload(file.getBytes(),objectName);
        } catch (IOException e) {
            log.error("文件上传失败：{}", e.getMessage(),e);
            throw new FileException(ExceptionConstant.FileUploadExceptionCode,"文件上传失败");
        }
    }

    /**
     * 文件上传
     *
     * @param bytes 文件字节数组
     * @param objectName 文件名
     * @return 文件访问路径
     */
    public String upload(byte[] bytes, String objectName) {

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // 创建PutObject请求。
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes));
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        //文件访问路径规则 https://BucketName.Endpoint/ObjectName
        StringBuilder stringBuilder = new StringBuilder("https://");
        stringBuilder
                .append(bucketName)
                .append(".")
                .append(endpoint)
                .append("/")
                .append(objectName);

        log.info("文件上传到:{}", stringBuilder);

        return stringBuilder.toString();
    }

    /**
     * 删除文件
     *
     * @param objectName 文件名
     */
    public void delete(String objectName) {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            // 删除文件。
            ossClient.deleteObject(bucketName, objectName);
            log.info("删除文件成功:{}",objectName);
        }
        catch (OSSException oe) {
            log.error("删除文件失败:{}",oe.getMessage(),oe);
            throw new FileException(ExceptionConstant.FileDeleteExceptionCode, ExceptionConstant.FileDeleteExceptionMsg);
        }
        finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 通过URL删除文件
     *
     * @param fileUrl 文件完整URL
     */
    public void deleteByUrl(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("http")) {
            throw new FileException(ExceptionConstant.FileDeleteExceptionCode, "无效的文件URL");
        }

        try {
            // 使用现代API处理URL
            java.net.URI uri = new java.net.URI(fileUrl);
            String path = uri.getPath();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            delete(path);
        } catch (Exception e) {
            log.error("解析URL失败: {}", fileUrl, e);
            throw new FileException(ExceptionConstant.FileDeleteExceptionCode, "文件URL格式错误");
        }
    }
}
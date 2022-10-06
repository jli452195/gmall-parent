package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import io.minio.*;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * author:atGuiGu-mqx
 * date:2022/10/4 14:42
 * 描述：
 **/
@RestController
@RequestMapping("/admin/product")
@RefreshScope
public class FileUploadController {

    //  这种方式叫软编码 ，如果直接 写在java 文件中，叫硬编码！
    @Value("${minio.endpointUrl}")
    private String endpointUrl;

    @Value("${minio.accessKey}")
    public String accessKey;

    @Value("${minio.secreKey}")
    public String secreKey;

    @Value("${minio.bucketName}")
    public String bucketName;

    //  Java io springmvc 牛, oss
    /*
    <form action="url" method="post" type="multipart/form-data">
        <input type = "file" name="file">
    </form>
     */
    //  /admin/product/fileUpload
    @PostMapping("fileUpload")
    public Result fileUpload(MultipartFile file) {
        //  声明一个变量来接收url 地址。
        String url = "";
        try {
            // 使用MinIO服务的URL，端口，Access key和Secret key创建一个MinioClient对象
            //  MinioClient minioClient = new MinioClient(endpointUrl, accessKey, secreKey);
            MinioClient minioClient =
                    MinioClient.builder()
                            .endpoint(endpointUrl)
                            .credentials(accessKey, secreKey)
                            .build();
            // 检查存储桶是否已经存在
            boolean found =
                    minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (found) {
                System.out.println(bucketName + "my-bucketname exists");
            } else {
                System.out.println(bucketName + " does not exist");
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                System.out.println(bucketName + " create...");

            }

            // 使用putObject上传一个文件到存储桶中。
            //  minioClient.putObject("asiatrip","asiaphotos.zip", "/home/user/Photos/asiaphotos.zip");
            String fileName = System.currentTimeMillis() + UUID.randomUUID().toString();
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucketName).object(fileName).stream(
                                    file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            //  //img11.360buyimg.com/n7/jfs/t1/218402/40/21999/37948/63369d37E7afcaf63/a27722c9df60c3cc.jpg
            //  http://192.168.200.129:9000/bucketName
            //  http://192.168.200.129:9000/gmall/16421452145622b459c16-a9b9-43f9-99bc-e3ded33d7752
            url = endpointUrl + "/" + bucketName + "/" + fileName;
            System.out.println("url:\t" + url);
        } catch (ErrorResponseException e) {
            e.printStackTrace();
        } catch (InsufficientDataException e) {
            e.printStackTrace();
        } catch (InternalException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidResponseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (XmlParserException e) {
            e.printStackTrace();
        }
        //  默认返回
        return Result.ok(url);
    }
}
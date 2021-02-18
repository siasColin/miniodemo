package cn.net.colin.utils;

import cn.net.colin.config.properties.MinioProperties;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * @Package: cn.net.colin.utils
 * @Author: sxf
 * @Date: 2021-1-31
 * @Description:
 */
@Component
public class MinioClientUtils {
    @Resource
    private MinioClient minioClient;
    @Resource
    private MinioProperties minioProperties;

    /**
     * 通过完整路径上传对象
     * @param targetFileName
     * @param sourceFilePath
     * @return
     */
    public String uploadObject(String targetFileName,String sourceFilePath){
        String fileurl = null;
        try {
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(targetFileName)
                            .filename(sourceFilePath)
                            .build());
            fileurl = minioProperties.getEndpoint()+"/"+minioProperties.getBucketName()+"/"+targetFileName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileurl;
    }

    /**
     * 通过InputStream上传对象
     * @param targetFileName
     * @param inputStream
     * @param contentType
     * @return
     */
    public String putObject(String targetFileName, InputStream inputStream,String contentType){
        try{
            if(contentType == null ||(contentType != null && contentType.trim().equals(""))){
                contentType = "application/octet-stream";
            }
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(targetFileName)
                    .stream(inputStream,inputStream.available(),-1)
                    .contentType(contentType)
                    .build());
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }finally {
            if(inputStream != null){
                try {
                    inputStream.close();
                    inputStream = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return minioProperties.getEndpoint()+"/"+minioProperties.getBucketName()+"/"+targetFileName;
    }

    /**
     * 获取一个对象的 InputStream 流
     * @param targetFileName
     * @return
     */
    public InputStream getObject(String targetFileName){
        InputStream stream = null;
        try {
            stream = minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket(minioProperties.getBucketName())
                                    .object(targetFileName).build());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stream;
    }

    /**
     * 判断对象是否存在
     *      statObject 获取对象的元数据，
     * @param targetFileName
     * @return
     */
    public boolean objectExists(String targetFileName){
        try{
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(targetFileName).build());
            return true;
        }catch (Exception e){
            return false;
        }
    }

    /**
     * 获取存储桶中的对象集合
     * @param prefix 对象名称的前缀
     * @param startAfter 从某个对象开始检索
     * @return
     */
    public Iterable<Result<Item>> listObjects(String prefix,String startAfter){
        ListObjectsArgs.Builder builder = ListObjectsArgs.builder();
        try{
            builder.bucket(minioProperties.getBucketName());
            if(prefix != null && !prefix.trim().equals("")){
                builder.prefix(prefix);
            }
            if(startAfter != null && !startAfter.trim().equals("")){
                builder.startAfter(startAfter);
            }
            return minioClient.listObjects(builder.build());
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }


}

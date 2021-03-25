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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

    private String prefix = "miniodemo/";//文件前缀，建议设置为项目名
    private int dateDir = 1;//前缀是否自动追加日期；1追加，0不追加

    /**
     * 通过完整路径上传对象
     * @param targetFileName
     * @param sourceFilePath
     * @return
     */
    public Map<String,Object> uploadObject(String targetFileName, String sourceFilePath){
        Map<String,Object> resultMap = new HashMap<String,Object>();
        try {
            if(dateDir == 1){
                SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
                targetFileName = prefix+format.format(new Date())+"/"+targetFileName;
            }else{
                targetFileName = prefix+targetFileName;
            }
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(targetFileName)
                            .filename(sourceFilePath)
                            .build());
            String fileurl = minioProperties.getEndpoint()+"/"+minioProperties.getBucketName()+"/"+targetFileName;
            resultMap.put("fileurl",fileurl);
            resultMap.put("filename",targetFileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultMap;
    }

    /**
     * 通过InputStream上传对象
     * @param targetFileName
     * @param inputStream
     * @param contentType
     * @return
     */
    public Map<String,Object> putObject(String targetFileName, InputStream inputStream,String contentType){
        Map<String,Object> resultMap = new HashMap<String,Object>();
        try{
            if(dateDir == 1){
                SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
                targetFileName = prefix+format.format(new Date())+"/"+targetFileName;
            }else{
                targetFileName = prefix+targetFileName;
            }
            if(contentType == null ||(contentType != null && contentType.trim().equals(""))){
                contentType = "application/octet-stream";
            }
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(targetFileName)
                    .stream(inputStream,inputStream.available(),-1)
                    .contentType(contentType)
                    .build());
            resultMap.put("fileurl",minioProperties.getEndpoint()+"/"+minioProperties.getBucketName()+"/"+targetFileName);
            resultMap.put("filename",targetFileName);
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
        return resultMap;
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
     * 删除一个对象
     * @param targetFileName
     * @return
     */
    public boolean removeObject(String targetFileName){
        try{
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
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
            return minioClient.listObjects(builder.recursive(true).build());
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }


}

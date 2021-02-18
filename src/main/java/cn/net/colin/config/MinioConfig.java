package cn.net.colin.config;

import cn.net.colin.config.properties.MinioProperties;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @Package: cn.net.colin.config
 * @Author: sxf
 * @Date: 2021-1-31
 * @Description:
 */
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {
    Logger logger = LoggerFactory.getLogger(MinioConfig.class);
    @Resource
    private MinioProperties minioProperties;

    @Bean
    private MinioClient minioClient(){
        MinioClient minioClient = null;
        try {
            minioClient =
                    MinioClient.builder()
                            .endpoint(minioProperties.getEndpoint())
                            .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                            .build();
        } catch (Exception e) {
            logger.error("MinIoClient init fail ...");
            e.printStackTrace();
        }
        return minioClient;
    }

}

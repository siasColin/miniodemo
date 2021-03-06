package cn.net.colin.config;

import com.github.xiaoymin.swaggerbootstrapui.annotations.EnableSwaggerBootstrapUI;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import springfox.documentation.RequestHandler;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Created by colin on 2020-2-27.
 */
@Configuration
@EnableSwagger2
@EnableSwaggerBootstrapUI
public class MyMvcConfig implements WebMvcConfigurer {
    /**
     * 解决跨域问题
     * @param registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")//添加映射路径，“/**”表示对所有的路径实行全局跨域访问权限的设置。
                .allowedOrigins("*")//开放哪些ip、端口、域名的访问权限
                .allowedMethods("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS")//开放哪些http方法，允许跨域访问
                .allowCredentials(true)//是否允许发送cookie信息
                .maxAge(3600)// 预检请求的缓存时间（秒），即在这个时间段里，对于相同的跨域请求不会再预检了
                //在使用CORS方式跨域时，浏览器只会返回以下默认头部header;使用exposedHeaders可以返回自定义header
                .exposedHeaders("Authorization","Refresh_token")
                .allowedHeaders("*");// #允许访问的头信息,*表示全

    }



    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:static/","file:static/");

        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:META-INF/resources/");

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:META-INF/resources/webjars/");

        registry.addResourceHandler("doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
    }


    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("MINIODEMO-API")
                .description("项目接口列表")
                // 创建人信息
                .contact(new Contact("Colin",  "",  "1540247870@qq.com"))
                .version("1.0")
                .build();
    }

    @Bean(value = "publicApi")
    public Docket publicApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                //为有@Api注解的Controller生成API文档
                .apis(RequestHandlerSelectors.withClassAnnotation(Api.class))
                //为有@ApiOperation注解的方法生成API文档
                .apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class))
                .apis(MyMvcConfig.basePackage("cn.net.colin.controller"))
                .paths(PathSelectors.any())
                .build().groupName("public");
    }


    /**
     * Predicate that matches RequestHandler with given base package name for the class of the handler method.
     * This predicate includes all request handlers matching the provided basePackage
     *
     * @param basePackage - base package of the classes
     * @return this
     */
    public static Predicate<RequestHandler> basePackage(final String basePackage) {
        return new Predicate<RequestHandler>() {
            @Override
            public boolean apply(RequestHandler input) {
                return declaringClass(input).transform(handlerPackage(basePackage)).or(true);
            }
        };
    }

    /**
     * 处理包路径配置规则,支持多路径扫描匹配以逗号隔开
     *
     * @param basePackage 扫描包路径
     * @return Function
     */
    private static Function<Class<?>, Boolean> handlerPackage(final String basePackage) {
        return new Function<Class<?>, Boolean>() {

            @Override
            public Boolean apply(Class<?> input) {
                for (String strPackage : basePackage.split(",")) {
                    boolean isMatch = input.getPackage().getName().startsWith(strPackage);
                    if (isMatch) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    /**
     * @param input RequestHandler
     * @return Optional
     */
    private static Optional<? extends Class<?>> declaringClass(RequestHandler input) {
        return Optional.fromNullable(input.declaringClass());
    }
}

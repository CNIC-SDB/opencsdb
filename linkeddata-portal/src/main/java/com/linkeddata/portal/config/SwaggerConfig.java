package com.linkeddata.portal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * swagger3 配置类
 *
 * @author wangzzhiliang
 * @date 20220831
 */
@Configuration
public class SwaggerConfig {
    /**
     * 与前端接口分组
     * 扫描com.linkeddata.portal包，但排除com.linkeddata.portal.script包
     *
     * @return
     */
    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.OAS_30).groupName("与前端接口")
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.linkeddata.portal"))
                .apis(RequestHandlerSelectors.basePackage("com.linkeddata.portal.script").negate())
                .apis(RequestHandlerSelectors.basePackage("com.linkeddata.portal.co_analysis").negate())
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("关联数据门户接口文档")
                .description("更多请咨询网络中心开发人员")
                .contact(new Contact("cnic", "https://www.baidu.com", "yml@xxxx.com"))
                .version("1.0")
                .build();
    }


    /**
     * 脚本分组接口
     *
     * @return
     */
   /* @Bean
    public Docket scriptApi() {
        return new Docket(DocumentationType.OAS_30).groupName("脚本接口")
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.linkeddata.portal.script"))
                .paths(PathSelectors.any())
                .build();
    }*/
    @Bean
    public Docket coAnalysisApi() {
        return new Docket(DocumentationType.OAS_30).groupName("协同分析接口")
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.linkeddata.portal.co_analysis"))
                .paths(PathSelectors.any())
                .build();
    }

}

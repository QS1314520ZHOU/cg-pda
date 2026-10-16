package com.digixmed.icu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * Swagger3 配置：UI 地址 http://<host>:<port>/swagger-ui/index.html
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public Docket docket() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.digixmed.icu.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("ICU PDA 医嘱执行记录同步服务 API")
                .description("测试接口：按小时窗口/全量查询金仓视图 vi_icu_hsz_yzzxjl，同步医嘱执行记录到 DataCenter")
                .version("1.0")
                .build();
    }
}

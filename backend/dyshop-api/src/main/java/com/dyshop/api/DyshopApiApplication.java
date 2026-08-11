package com.dyshop.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * C 端用户接口服务入口（端口 8081）。
 * <p>扫描 {@code com.dyshop} 全部包，使 dyshop-common 中的公共配置/组件一并生效。</p>
 * <p>@EnableScheduling：启用 {@code OrderTimeoutScheduler} 等定时任务（待支付订单超时自动取消）。</p>
 */
@SpringBootApplication(scanBasePackages = "com.dyshop")
@MapperScan("com.dyshop.api.mapper")
@EnableScheduling
public class DyshopApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DyshopApiApplication.class, args);
    }
}

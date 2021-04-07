package com.athena.jdbc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * @author yusheng
 */
@SpringBootApplication
@MapperScan(basePackages = {"com.athena.jdbc"})
@Import(ShardingJdbcBeanDefinitionPostProcessor.class)
public class BootStrap {
    public static void main(String[] args) {
        SpringApplication.run(BootStrap.class, args);
    }

    @Bean
    public ShardingJdbcBeanDefinitionPostProcessor shardingJdbcBeanDefinitionPostProcessor(){
        return new ShardingJdbcBeanDefinitionPostProcessor();
    }
}

package com.arts.q1.webflux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

/**
 * @author yusheng
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * 初始化路由信息
     *
     * @return {@link RouterFunction}
     */
    @Bean
    public RouterFunction routerFunction() {
        return RouterFunctions.route()
                .GET("/router", RequestPredicates.accept(MediaType.APPLICATION_JSON), this::helloRouter)
                .GET("/routerX", RequestPredicates.accept(MediaType.APPLICATION_JSON), this::helloRouter)
                .build();
    }

    /**
     * http请求具体的执行方法
     *
     * @param serverRequest 请求参数
     * @return 请求返回参数
     */
    private Mono<ServerResponse> helloRouter(ServerRequest serverRequest) {
        return ServerResponse.ok().body(Mono.just("Hello Router"), String.class);
    }
}

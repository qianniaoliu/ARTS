package com.arts.q1.webflux.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author yusheng
 */
@RestController
@RequestMapping("/xxx")
public class IndexController {

    @GetMapping("/test/requestParam")
    public String requestParam(@RequestParam String hello) {
        return hello;
    }

    @GetMapping("/test/requestParamX")
    public String requestParamX(@RequestParam String hello) {
        return hello;
    }

    public String test(String hello){
        return hello;
    }
}

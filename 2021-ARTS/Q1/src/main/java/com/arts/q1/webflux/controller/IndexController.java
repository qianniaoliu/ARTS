package com.arts.q1.webflux.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yusheng
 */
@RestController
public class IndexController {

    @GetMapping("/test/requestParam")
    public String requestParam(@RequestParam String hello){
        return hello;
    }

    @GetMapping("/test/requestParamX")
    public String requestParamX(@RequestParam String hello){
        return hello;
    }
}

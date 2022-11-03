/*
 * Ant Group
 * Copyright (c) 2004-2021 All Rights Reserved.
 */
package com.tm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

/**
 * @author shenlong
 */
@RestController
public class ReceiveController {

    private final static Logger logger = LoggerFactory.getLogger(ReceiveController.class);


    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/receive/message")
    public String receiveMessage(@RequestParam(required = false) Map<String, Object> message){
        logger.info("receive:{}", message);
        return "receive : "+ message;
    }

    @GetMapping("/test")
    public String test(){
        MultiValueMap<String, String> headers = new HttpHeaders();
        String authorization = "tomcat" + ":" + "tomcat";
        byte[] rel = Base64.getEncoder().encode(authorization.getBytes());
        headers.add("Authorization", "Basic " + new String(rel));
        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange("http://localhost:9091/out/message", HttpMethod.GET, httpEntity,String.class);
        return responseEntity.getBody();
    }




















    @GetMapping("/index")
    public String index(@RequestParam String message){
        return "receive : "+ message;
    }























    @GetMapping("/hello/world")
    public String helloWorld(){
        return "Hello,World";
    }
}
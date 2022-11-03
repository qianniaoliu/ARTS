/*
 * Ant Group
 * Copyright (c) 2004-2021 All Rights Reserved.
 */
package com.spring.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.framework.ProxyFactory;

import java.lang.reflect.Method;

/**
 * @author shenlong
 * @version ProxyFactoryDemo.java, v 0.1 2021年07月02日 3:34 下午 shenlong
 */
public class ProxyFactoryDemo {
    public static void main(String[] args) {
        EchoService echoService = new EchoService();
        ProxyFactory proxyFactory = new ProxyFactory(echoService);
        proxyFactory.addAdvice(new EchoServiceAdvice());
        proxyFactory.addAdvice(new EchoServiceInterceptor());
        EchoService proxy = (EchoService) proxyFactory.getProxy();
        System.out.printf(proxy.hello("Hello,World"));
    }

    public static class EchoServiceAdvice implements MethodBeforeAdvice {

        @Override
        public void before(Method method, Object[] args, Object target) throws Throwable {
            System.out.println("print log before method process");
        }
    }

    public static class EchoServiceInterceptor implements MethodInterceptor {

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            return "Hello Proxy Result";
        }
    }
}
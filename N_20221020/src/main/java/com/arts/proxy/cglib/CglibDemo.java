/*
 * Ant Group
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.arts.proxy.cglib;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.InvocationHandler;

import java.lang.reflect.Method;

/**
 * @author shenlong
 * @version CglibDemo.java, v 0.1 2022年10月21日 10:15 AM shenlong
 */
public class CglibDemo {

    public static void main(String[] args) {
        Enhancer enhancer = new Enhancer();
        enhancer.setCallback((InvocationHandler) (o, method, objects) -> "athena");
        enhancer.setInterfaces(new Class[]{InterfaceUser.class});
        InterfaceUser proxyUser = (InterfaceUser) enhancer.create();
        System.out.println(proxyUser.getName());
    }
}
package com.arts.jvm;

import java.lang.reflect.Method;

/**
 * @author yusheng
 */
public class ReflectionDemo {

    public static void target(int i) {
        new Exception("#" + i).printStackTrace();
    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 20; i++) {
            Method method = ReflectionDemo.class.getMethod("target", int.class);
            method.invoke(null, i);
        }
    }
}

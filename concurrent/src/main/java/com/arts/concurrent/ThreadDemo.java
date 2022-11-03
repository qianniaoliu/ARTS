/*
 * Ant Group
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.arts.concurrent;

/**
 * @author shenlong
 * @version ThreadDemo.java, v 0.1 2022年10月17日 11:57 AM shenlong
 */
public class ThreadDemo {

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 20; i++) {
            Thread t1 = new Thread(() -> {
                System.out.println("t1 start running");
            });
            Thread t2 = new Thread(() -> {
                System.out.println("t2 start running");
            });
            t1.start();
            t1.join();
            t2.start();
        }
    }
}
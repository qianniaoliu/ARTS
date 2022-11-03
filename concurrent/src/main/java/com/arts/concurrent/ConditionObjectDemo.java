/*
 * Ant Group
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.arts.concurrent;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author shenlong
 * @version ConditionObjectDemo.java, v 0.1 2022年10月17日 4:44 PM shenlong
 */
public class ConditionObjectDemo {

    private static Lock lock = new ReentrantLock();

    private static          Condition condition    = lock.newCondition();
    private static volatile int       currentValue = 0;

    public static void main(String[] args) throws Exception {
        Thread t1 = new Thread(() -> {
            while (true) {
                try {
                    lock.lock();
                    if (currentValue % 2 == 0) {
                        System.out.println("t1 start running, current value : " + currentValue);
                        currentValue++;
                        condition.signal();
                    } else {
                        condition.await();
                    }
                    if (currentValue > 100) {
                        break;
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    lock.unlock();
                }
            }
        });

        Thread t2 = new Thread(() -> {
            while (true) {
                try {
                    lock.lock();
                    if (currentValue % 2 == 1) {
                        System.out.println("t2 start running, current value : " + currentValue);
                        currentValue++;
                        condition.signal();
                    } else {
                        condition.await();
                    }
                    if (currentValue > 100) {
                        break;
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    lock.unlock();
                }
            }
        });
        t2.start();
        t1.start();

    }
}
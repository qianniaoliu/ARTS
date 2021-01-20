package com.arts.queue;

import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;

/**
 * @author yusheng
 */
public class MpscArrayQueueDemo {

    public static void main(String[] args) {
        MpscArrayQueue<Integer> mpscArrayQueue = new MpscArrayQueue(2);
        mpscArrayQueue.offer(111);
        mpscArrayQueue.offer(222);
        mpscArrayQueue.offer(333);
    }
}

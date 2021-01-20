package com.arts.queue;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author yusheng
 */
public class ConcurrentLinkedQueueDemo {

    public static void main(String[] args) {
        Queue<Integer> queue = new ConcurrentLinkedQueue();
        queue.offer(1);
        queue.offer(2);
        queue.offer(3);
    }
}

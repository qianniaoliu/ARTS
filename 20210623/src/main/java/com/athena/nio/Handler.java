/*
 * Ant Group
 * Copyright (c) 2004-2021 All Rights Reserved.
 */
package com.athena.nio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author shenlong
 * @version Handler.java, v 0.1 2021年06月23日 10:06 上午 shenlong
 */
public class Handler implements Runnable {

    private final Selector selector;
    private final SocketChannel socketChannel;
    private final SelectionKey selectionKey;

    ByteBuffer input = ByteBuffer.allocate(1024);
    ByteBuffer output = ByteBuffer.allocate(1024);
    private static final int READING = 0;
    private static final int SENDING = 1;
    private int state = READING;

    public Handler(Selector selector, SocketChannel socketChannel) throws IOException {
        this.selector = selector;
        this.socketChannel = socketChannel;
        this.socketChannel.configureBlocking(false);
        this.selectionKey = this.socketChannel.register(this.selector, 0);
        this.selectionKey.attach(this);
        this.selectionKey.interestOps(SelectionKey.OP_READ);
        selector.wakeup();
    }

    @Override
    public void run() {
        try {
            if(state == READING){
                read();
            }else if(state == SENDING){
                send();
            }
        }catch (Exception ex){

        }
    }

    private void send() throws IOException {
        socketChannel.write(output);
        if(outputIsComplete()){
            selectionKey.cancel();
        }
    }

    private boolean outputIsComplete() {
        return true;
    }

    private void read() throws IOException {
        socketChannel.read(input);
        if(inputIsComplete()){
            process();
            state = SENDING;
            selectionKey.interestOps(SelectionKey.OP_WRITE);
        }
        
    }

    private void process() {
        String msg = null;
        try {
            msg = new String(input.array(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        System.out.printf("接收到客户端消息:" + msg);
    }

    private boolean inputIsComplete() {
        return true;
    }

}
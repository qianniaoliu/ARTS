/*
 * Ant Group
 * Copyright (c) 2004-2021 All Rights Reserved.
 */
package com.athena.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author shenlong
 * @version Client.java, v 0.1 2021年06月23日 10:09 上午 shenlong
 */
public class Client {

    public static void main(String[] args) throws IOException {
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 8999));
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        byteBuffer.put("Hello,World".getBytes("UTF-8"));
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
        System.in.read();
    }
}
package com.arts.nio;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * @author yusheng
 */
public class ChatClient {

    private Selector selector;

    public void start() throws Exception {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);

        selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
        socketChannel.connect(new InetSocketAddress(8081));


    }

    public static void main(String[] args) {


        System.out.println(10240 & (16 * 1024 - 1));
    }
}

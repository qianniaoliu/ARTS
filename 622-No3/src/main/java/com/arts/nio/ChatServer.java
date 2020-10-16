package com.arts.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

/**
 * @author yusheng
 */
public class ChatServer {

    private Selector selector;

    public void start() throws Exception {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(6379));

        selector = Selector.open();
        SelectionKey selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        handleKeys();

    }

    private void handleKeys() throws Exception {
        while (true){
            int num = selector.select();
            if(num == 0){
                continue;
            }
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectionKeys.iterator();
            while (keyIterator.hasNext()){
                SelectionKey selectionKey = keyIterator.next();
                keyIterator.remove();
                if(!selectionKey.isValid()){
                    continue;
                }
                handleKey(selectionKey);
            }

        }
    }

    private void handleKey(SelectionKey selectionKey) throws IOException {
        if(selectionKey.isAcceptable()){
            handleAcceptableKey(selectionKey);
        }
        if(selectionKey.isReadable()){
            handleReadableKey(selectionKey);
        }
        if(selectionKey.isWritable()){
            handleWritableKey(selectionKey);
        }
    }

    private void handleWritableKey(SelectionKey selectionKey) throws IOException {

    }

    private void handleReadableKey(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        int count = socketChannel.read(byteBuffer);
        if(count == -1){
            return;
        }
        System.out.println("有数据可读");
    }

    private void handleAcceptableKey(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = ((ServerSocketChannel)selectionKey.channel()).accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ, new ArrayList<String>());
        System.out.println("有连接接入");
    }
}

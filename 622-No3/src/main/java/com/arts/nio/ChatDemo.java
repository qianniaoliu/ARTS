package com.arts.nio;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yusheng
 */
public class ChatDemo {

    public static void main(String[] args) throws Exception {
        /*ChatServer server = new ChatServer();
        server.start();*/

        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        List<String> strList = list.stream().map(String::valueOf).collect(Collectors.toList());
        System.out.println(strList.contains(2));
    }
}

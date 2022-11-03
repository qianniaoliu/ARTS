/*
 * Ant Group
 * Copyright (c) 2004-2021 All Rights Reserved.
 */
package com.athena.nio;

/**
 * @author shenlong
 * @version Server.java, v 0.1 2021年06月23日 10:16 上午 shenlong
 */
public class Server {

    public static void main(String[] args) throws Exception {
        Reactor reactor = new Reactor(8999);
        reactor.run();
    }
}
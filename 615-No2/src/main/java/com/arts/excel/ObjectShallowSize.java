package com.arts.excel;

import java.lang.instrument.Instrumentation;

/**
 * @author yusheng
 */
public class ObjectShallowSize {
    private static Instrumentation instrumentation;

    public static void premain(String agentArgs, Instrumentation param) {
        instrumentation = param;
    }

    public static long sizeOf(Object obj) {
        return instrumentation.getObjectSize(obj);
    }
}

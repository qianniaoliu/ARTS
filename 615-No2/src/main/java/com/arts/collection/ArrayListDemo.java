package com.arts.collection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author yusheng
 */
public class ArrayListDemo {

    private final static int STATIC_INT = 1;

    public static void main(String[] args) {
        List<String> data = new LinkedList<>();
        data.add("A");
        data.add("B");
        data.add("C");
        data.add("D");
        data.add("E");
        data.set(11, "F");
        Iterator<String> iterator1 = data.iterator();
        Iterator<String> iterator2 = data.iterator();
        iterator1.next();
        iterator1.remove();
        iterator2.next();
    }
}

/*
 * Ant Group
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.arts.altor;

/**
 * @author shenlong
 * @version NumberNode.java, v 0.1 2022年11月02日 8:42 PM shenlong
 */
public class NumberNode {
    private NumberNode next;

    private int value;

    public NumberNode(int value) {
        this.value = value;
    }

    public static NumberNode init1(){
        NumberNode head = new NumberNode(1);
        NumberNode node2 = new NumberNode(2);
        head.next = node2;
        NumberNode node3 = new NumberNode(5);
        node2.next = node3;
        NumberNode node4 = new NumberNode(6);
        node3.next = node4;
        NumberNode node5 = new NumberNode(8);
        node4.next = node5;
        return head;
    }

    /**
     * Getter method for property <tt>next</tt>.
     *
     * @return property value of next
     */
    public NumberNode getNext() {
        return next;
    }

    /**
     * Setter method for property <tt>next</tt>.
     *
     * @param next value to be assigned to property next
     */
    public void setNext(NumberNode next) {
        this.next = next;
    }

    /**
     * Getter method for property <tt>value</tt>.
     *
     * @return property value of value
     */
    public int getValue() {
        return value;
    }

    /**
     * Setter method for property <tt>value</tt>.
     *
     * @param value value to be assigned to property value
     */
    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public String toString() {
        NumberNode head = this;
        StringBuilder sb = new StringBuilder();
        sb.append(value);
        while (head.next != null){
            sb.append(",").append(head.next.value);
            head = head.next;
        }
        return sb.toString();
    }
}
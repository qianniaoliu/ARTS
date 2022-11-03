/*
 * Ant Group
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.arts.altor;

/**
 * @author shenlong
 * @version Node.java, v 0.1 2022年10月27日 8:40 PM shenlong
 */
public class Node {

    private Node prev;

    private Node next;

    private String value;

    public Node(String value) {
        this.value = value;
    }

    public static Node init(){
        Node head = new Node("A");
        Node node1 = new Node("B");
        Node node2 = new Node("C");
        Node tail = new Node("D");
        head.next = node1;
        node1.prev = head;
        node1.next = node2;
        node2.prev = node1;
        node2.next = tail;
        //tail.prev = node2;
        //tail.next = node2;
        return head;

    }

    public static Node init1(){
        Node head = new Node("1");
        Node node2 = new Node("3");
        head.next = node2;
        return head;

    }

    public static Node init2(){
        Node node1 = new Node("2");
        Node tail = new Node("4");
        node1.next = tail;
        return node1;
    }

    /**
     * Getter method for property <tt>prev</tt>.
     *
     * @return property value of prev
     */
    public Node getPrev() {
        return prev;
    }

    /**
     * Setter method for property <tt>prev</tt>.
     *
     * @param prev value to be assigned to property prev
     */
    public void setPrev(Node prev) {
        this.prev = prev;
    }

    /**
     * Getter method for property <tt>next</tt>.
     *
     * @return property value of next
     */
    public Node getNext() {
        return next;
    }

    /**
     * Setter method for property <tt>next</tt>.
     *
     * @param next value to be assigned to property next
     */
    public void setNext(Node next) {
        this.next = next;
    }

    /**
     * Getter method for property <tt>value</tt>.
     *
     * @return property value of value
     */
    public String getValue() {
        return value;
    }

    /**
     * Setter method for property <tt>value</tt>.
     *
     * @param value value to be assigned to property value
     */
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(value);
        Node tmp = next;
        while (tmp != null){
            sb.append(",")
                    .append(tmp.value);
            tmp = tmp.next;
        }
        return sb.toString();
    }
}
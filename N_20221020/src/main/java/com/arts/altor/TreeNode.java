/*
 * Ant Group
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.arts.altor;

/**
 * @author shenlong
 * @version TreeNode.java, v 0.1 2022年10月31日 10:47 AM shenlong
 */
public class TreeNode {
    private int val;

    private TreeNode left;

    private TreeNode right;

    public TreeNode(int val) {
        this.val = val;
    }

    public static TreeNode init(){
        TreeNode root = new TreeNode(1);
        TreeNode node2 = new TreeNode(2);
        TreeNode node3 = new TreeNode(3);
        root.left = node2;
        root.right = node3;
        TreeNode node4 = new TreeNode(4);
        TreeNode node5 = new TreeNode(5);
        node2.left=node4;
        node2.right = node5;
        TreeNode node6 = new TreeNode(6);
        TreeNode node7 = new TreeNode(7);
        node3.left = node6;
        node3.right = node7;
        TreeNode node8 = new TreeNode(8);
        TreeNode node9 = new TreeNode(9);
        node4.left = node8;
        node4.right = node9;
        return root;
    }

    /**
     * Getter method for property <tt>val</tt>.
     *
     * @return property value of val
     */
    public int getVal() {
        return val;
    }

    /**
     * Setter method for property <tt>val</tt>.
     *
     * @param val value to be assigned to property val
     */
    public void setVal(int val) {
        this.val = val;
    }

    /**
     * Getter method for property <tt>left</tt>.
     *
     * @return property value of left
     */
    public TreeNode getLeft() {
        return left;
    }

    /**
     * Setter method for property <tt>left</tt>.
     *
     * @param left value to be assigned to property left
     */
    public void setLeft(TreeNode left) {
        this.left = left;
    }

    /**
     * Getter method for property <tt>right</tt>.
     *
     * @return property value of right
     */
    public TreeNode getRight() {
        return right;
    }

    /**
     * Setter method for property <tt>right</tt>.
     *
     * @param right value to be assigned to property right
     */
    public void setRight(TreeNode right) {
        this.right = right;
    }
}
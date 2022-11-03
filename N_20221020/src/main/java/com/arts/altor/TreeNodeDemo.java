/*
 * Ant Group
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.arts.altor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author shenlong
 * @version TreeNodeDemo.java, v 0.1 2022年10月31日 10:49 AM shenlong
 */
public class TreeNodeDemo {

    public static void main(String[] args) {
        TreeNode root = TreeNode.init();
        List<Integer> data = new ArrayList<>();
        preOrder(root, data);
        System.out.println("preOrder:" + data);
        data.clear();
        //
        //inOrder(root, data);
        //System.out.println("inOrder:" + data);
        //data.clear();
        //
        //postOrder(root, data);
        //System.out.println("postOrder:" + data);
        //data.clear();

        //iterInOrder(root, data);
        //
        //System.out.println("iterInOrder:" + data);

        iterPreOrder(root, data);

        System.out.println("iterPreOrder:" + data);

         // 二叉树每一层节点遍历
        //levelOrder(root);
    }

    public static void levelOrder(TreeNode root){
        List<List<Integer>> data = new ArrayList<>();
        Queue<TreeNode> queue = new LinkedBlockingQueue<>();
        queue.add(root);

        while (!queue.isEmpty()){
            int size = queue.size();
            List<Integer> itemData = new ArrayList<>();
            while (size-- > 0){
                TreeNode currentNode = queue.poll();
                itemData.add(currentNode.getVal());
                if(currentNode.getLeft() !=null){
                    queue.add(currentNode.getLeft());
                }
                if(currentNode.getRight() != null){
                    queue.add(currentNode.getRight());
                }
            }
            data.add(itemData);
        }
        System.out.println("levelOrder:" + data);
    }

    /**
     * 前序遍历
     *
     * @param root
     * @param data
     */
    public static void preOrder(TreeNode root, List<Integer> data) {
        if (root == null) {
            return;
        }
        data.add(root.getVal());
        preOrder(root.getLeft(), data);
        preOrder(root.getRight(), data);
    }

    public static void iterPreOrder(TreeNode root, List<Integer> data){
        Deque<TreeNode> deque = new ArrayDeque<>();
        while (root != null || !deque.isEmpty()){
            while (root != null){
                data.add(root.getVal());
                deque.push(root);
                root = root.getLeft();
            }
            root = deque.pop();

            root = root.getRight();
        }
    }

    public static void iterInOrder(TreeNode root, List<Integer> data){
        Deque<TreeNode> deque = new ArrayDeque<>();
        while (root != null || !deque.isEmpty()){
            while (root != null){
                deque.push(root);
                root = root.getLeft();
            }
            root = deque.pop();
            data.add(root.getVal());
            root = root.getRight();
        }
    }

    /**
     * 中序遍历
     *
     * @param root
     * @param data
     */
    public static void inOrder(TreeNode root, List<Integer> data) {
        if (root == null) {
            return;
        }
        inOrder(root.getLeft(), data);
        data.add(root.getVal());
        inOrder(root.getRight(), data);
    }

    /**
     * 后续遍历
     *
     * @param root
     * @param data
     */
    public static void postOrder(TreeNode root, List<Integer> data) {
        if (root == null) {
            return;
        }
        postOrder(root.getLeft(), data);
        postOrder(root.getRight(), data);
        data.add(root.getVal());
    }
}
/*
 * Ant Group
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.arts.altor;

import java.util.Arrays;

/**
 * @author shenlong
 * @version ReverseNodeList.java, v 0.1 2022年10月27日 8:39 PM shenlong
 */
public class ReverseNodeList {

    public static void main(String[] args) {
        Node head = Node.init();
        // 链表反转
        //reverseNode(head);
        // 判断链表是否有环
        //loopNode(head);
        // 合并两个有序链表
        //mergeTwoList(Node.init1(), Node.init2());
        // 判断是否是回文数字
        //isPalindrome(543451);

        // 移出重复数字
        //removeDuplicates();

        // 快排
        //quickSort();

        //removeNthFromEnd(4);

        // 链表元素两两交换
        //swapPairs();

        // 两个链表相加
        addTwoNumbers(NumberNode.init1(), NumberNode.init1());
    }

    public static void addTwoNumbers(NumberNode m, NumberNode n) {
        NumberNode head = null, tail = null;
        int carry = 0;
        while (m != null || n != null) {
            int mval = m == null ? 0 : m.getValue();
            int nval = n == null ? 0 : n.getValue();
            if (head == null) {
                head = tail = new NumberNode((mval + nval) % 10);
            } else {
                tail.setNext(new NumberNode((mval + nval + carry) % 10));
                tail = tail.getNext();
            }
            carry = (mval + nval + carry) / 10;
            if (m != null) {
                m = m.getNext();
            }
            if (n != null) {
                n = n.getNext();
            }
        }
        if (carry > 0) {
            tail.setNext(new NumberNode(carry));
        }
        System.out.println(head);
    }

    private static void swapPairs() {
        Node result = doSwapPairs(Node.init());
        System.out.println(result);
    }

    private static Node doSwapPairs(Node head) {
        if (head == null || head.getNext() == null) {
            return head;
        }
        Node next = head.getNext();
        head.setNext(doSwapPairs(next.getNext()));
        next.setNext(head);
        return next;
    }

    private static void removeNthFromEnd(int n) {
        Node head = Node.init();
        removeNode(head, n);
        System.out.println(head);
    }

    private static int removeNode(Node node, int n) {
        if (node.getNext() == null) {
            return 1;
        }
        int m = removeNode(node.getNext(), n);
        if (m == n) {
            node.setNext(node.getNext().getNext());
        }
        if (m == n - 1) {
            Node tmp = node.getNext();
            node.setValue(tmp.getValue());
            node.setNext(node.getNext().getNext());
        }
        return m + 1;
    }

    private static void quickSort() {
        int[] nums = new int[] {6, 11, 3, 9, 8};
        quickSort0(nums, 0, nums.length - 1);
        Arrays.stream(nums).forEach(System.out::println);
    }

    private static void quickSort0(int[] nums, int start, int end) {
        if (start >= end) {
            return;
        }
        int partition = partition(nums, start, end);
        quickSort0(nums, start, partition - 1);
        quickSort0(nums, partition + 1, end);
    }

    private static int partition(int[] nums, int start, int end) {
        int partitionValue = nums[end];
        int i = start;
        for (int j = start; j < end; j++) {
            if (nums[j] < partitionValue) {
                int tmp = nums[i];
                nums[i] = nums[j];
                nums[j] = tmp;
                i++;
            }
        }
        int tmp = nums[i];
        nums[i] = partitionValue;
        nums[end] = tmp;
        return i;
    }

    private static void removeDuplicates() {
        int[] nums = new int[] {1, 1, 2, 2, 4, 4, 4, 5, 6, 7, 8, 8, 9};
        int i = 0;
        int j = 1;
        while (j < nums.length) {
            if (nums[i] == nums[j]) {
                j++;
            } else {
                nums[++i] = nums[j];
                j++;
            }
        }
        while (i < nums.length - 1) {
            nums[++i] = 0;
        }
        System.out.println(nums.toString());
    }

    private static void isPalindrome(int x) {
        boolean flag;
        if (x == 0) {
            flag = true;
        } else if (x < 0 || x % 10 == 0) {
            flag = false;
        } else {
            int reverse = 0;
            while (x > reverse) {
                reverse = reverse * 10 + x % 10;
                x = x / 10;
            }
            flag = x == reverse || reverse / 10 == x;
        }
        System.out.println("palindrome : " + flag);
    }

    private static void mergeTwoList(Node init1, Node init2) {
        Node result = new Node("0");
        Node current = result;
        Node n1 = init1;
        Node n2 = init2;
        while (n1 != null && n2 != null) {
            if (of(n1.getValue()) < of(n2.getValue())) {
                current.setNext(n1);
                current = n1;
                n1 = n1.getNext();
            } else {
                current.setNext(n2);
                current = n2;
                n2 = n2.getNext();
            }
        }
        if (n1 != null) {
            current.setNext(n1);
        }
        if (n2 != null) {
            current.setNext(n2);
        }
        System.out.println(result);
    }

    private static Integer of(String value) {
        return Integer.valueOf(value);
    }

    /**
     * 链表反转
     *
     * @param head
     */
    public static void reverseNode(Node head) {
        System.out.println("反转前:" + head);
        Node current = head;
        Node prev = null;
        while (current != null) {
            Node tmp = current.getNext();
            current.setNext(prev);
            prev = current;
            current = tmp;
        }
        System.out.println("反转后:" + prev);
    }

    public static void loopNode(Node head) {
        Node fast = head;
        Node slow = head;
        while (fast != null && fast.getNext() != null) {
            fast = fast.getNext().getNext();
            slow = slow.getNext();
            if (fast == slow) {
                System.out.println("current node have loop,value:" + fast.getValue());
                break;
            }
        }
    }
}
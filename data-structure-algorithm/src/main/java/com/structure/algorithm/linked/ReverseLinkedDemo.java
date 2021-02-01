package com.structure.algorithm.linked;

/**
 * @author yusheng
 */
public class ReverseLinkedDemo {

    public static void main(String[] args) {
        Node<String> head = new Node<>("A");
        Node<String> secondNode = new Node<>("B");
        Node<String> threeNode = new Node<>("C");
        Node<String> fourNode = new Node<>("D");
        threeNode.next = fourNode;
        secondNode.next = threeNode;
        head.next = secondNode;

        reverseNode(head);
        System.out.printf("11");
    }


    private static void reverseNode(Node<String> head) {
        Node<String> fast = head;
        Node<String> slow = head;
        while (fast.next != null && fast.next.next != null){
            slow = slow.next;
            fast = fast.next.next;
        }
        // 获取中间节点
        Node<String> mid = slow.next;
        // 将链表从中间节点这里断开
        slow.next = null;

        // 后半截链表反转
        // 记录前一个节点
        Node<String> prev = null;
        while (mid != null){
            // 首先记录下一个节点
            Node<String> next = mid.next;
            // 节点反转，将当前节点的下一个节点置为上一个节点
            mid.next = prev;
            // 将上一个节点置为当前节点，供下一次循环使用
            prev = mid;
            // 遍历节点置为下一个节点
            mid = next;
        }

        Node<String> tmp1= null;
        Node<String> tmp2= null;

        while (head != null){
            // 先保存两个临时节点
            tmp1 = head.next;
            tmp2 = prev.next;
            // 第一个链表的节点的下一个节点是第二个链表的节点
            head.next = prev;
            // 第二个链表的节点的下一个节点是第一个链表的节点
            prev.next = tmp1;

            // 循环下一个节点
            head = tmp1;
            prev = tmp2;
        }

    }





    private static class Node<D> {
        private D d;
        private Node<D> next;

        public Node(D d) {
            this.d = d;
        }
    }
}

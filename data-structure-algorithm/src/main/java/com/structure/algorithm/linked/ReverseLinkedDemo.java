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

    }

    private static void reverseNode(Node<String> head) {
        if(head == null || head.next == null){
            return;
        }
        Node<String> fast = head;
        Node<String> slow = head;
        while (fast.next != null && fast.next.next != null){
            fast = fast.next.next;
            slow = slow.next;
        }
        // 获取中间节点
        Node<String> mid = slow.next;
        // 将中间节点的上一节点的next节点置为空，也就是截断
        slow.next = null;

        Node<String> tail = mid;
        Node<String> prev = null;
        while (tail != null){
            Node<String> next = tail.next;
            tail.next = prev;
            prev = tail;
            tail = next;
        }
        Node<String> tmp1 = null;
        Node<String> tmp2= null;
        while (head != null && prev != null){
            tmp1 = head.next;
            tmp2 = prev.next;

            head.next = prev;
            prev.next = tmp1;

            head = tmp1;
            prev = tmp2;
        }
        System.out.printf("111");
    }

    private static class Node<D> {
        private D d;
        private Node<D> next;

        public Node(D d) {
            this.d = d;
        }
    }
}

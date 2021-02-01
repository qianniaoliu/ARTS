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
        Node<String> fast = head;
        Node<String> slow = head;
        while (fast.next != null && fast.next.next != null){
            slow = slow.next;
            fast = fast.next.next;
        }
        Node<String> mid = slow.next;
        slow.next = null;
    }



    private static class Node<D> {
        private D d;
        private Node<D> next;

        public Node(D d) {
            this.d = d;
        }
    }
}

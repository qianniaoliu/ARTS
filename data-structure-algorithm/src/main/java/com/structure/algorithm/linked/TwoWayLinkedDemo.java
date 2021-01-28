package com.structure.algorithm.linked;

/**
 * @author yusheng
 */
public class TwoWayLinkedDemo {

    public static void main(String[] args) {
        TwoWayLinked<String> twoWayLinked = new TwoWayLinked<>();
        twoWayLinked.add("A");
        twoWayLinked.add("B");
        twoWayLinked.add("C");
        twoWayLinked.add("D");
        System.out.printf("111");
        twoWayLinked.remove("B");
        System.out.printf("222");

    }


    private static class TwoWayLinked<D> {

        private Node<D> head;

        private Node<D> tail;

        public TwoWayLinked() {
            head = tail = new Node<>(null);
            head.next = tail;
            tail.prev = head;
        }

        public void add(D d) {
            Node<D> currentNode = new Node<>(d);
            if (head == tail) {
                head.next = currentNode;
                currentNode.prev = head;
                tail = currentNode;
            } else {
                tail.next = currentNode;
                currentNode.prev = tail;
                tail = currentNode;
            }

        }

        public void remove(D d){
            if(head == tail){
                throw new IllegalArgumentException("no node");
            }
            Node<D> currentNode = head;
            do{
                Node<D> nextNode = currentNode.next;
                if(nextNode.d == d){
                    currentNode.next = nextNode.next;
                    nextNode.next.prev = currentNode;
                    break;
                }
                currentNode = nextNode;
            }while (currentNode.next != null);
        }

    }

    private static class Node<D> {
        private D d;
        private Node<D> prev;
        private Node<D> next;

        public Node(D d) {
            this.d = d;
        }
    }
}

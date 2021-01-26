package com.structure.algorithm.linked;

/**
 * @author yusheng
 */
public class OneWayLinkedDemo {

    public static void main(String[] args) {
        OneWayLinked<Integer> oneWayLinked = new OneWayLinked<>();
        oneWayLinked.add(1);
        oneWayLinked.add(2);
        oneWayLinked.add(3);
        oneWayLinked.remove(2);
        oneWayLinked.add(4);
    }

    private static class OneWayLinked<D> {

        private Node<D> head;

        private Node<D> tail;

        public OneWayLinked() {
            tail = new Node<>(null, null);
            head = new Node<>(null, tail);
        }

        public void add(D d) {
            Node<D> data = new Node<>(d, null);
            if (head.next == tail) {
                head.next = tail = data;
                return;
            }
            tail.next = data;
            tail = data;
        }

        public D remove(D d) {
            if (tail.d == null) {
                throw new IllegalArgumentException("oneWayLinked is empty");
            }
            Node<D> currentNode = head;
            do {
                Node<D> prev = currentNode;
                currentNode = currentNode.next;
                if (currentNode.d == d) {
                    prev.next = currentNode.next;
                    if(currentNode == tail){
                        tail = prev;
                    }
                    return d;
                }
            } while (currentNode.next != null);

            throw new IllegalArgumentException("d is not exist");
        }
    }

    private static class Node<D> {
        private D d;
        private Node<D> next;

        public Node(D d, Node<D> next) {
            this.d = d;
            this.next = next;
        }
    }
}

package com.structure.algorithm.linked;

/**
 * @author yusheng
 */
public class TwoWayLinkedDemo {


    private static class TwoWayLinked<D>{

        private Node<D> head;

        private Node<D> tail;

        public TwoWayLinked(){
            tail = new Node<>(null, null, null);
            head = new Node<>(null, null, tail);
            tail.prev = head;
        }

        public void add(D d){
            
        }

    }

    private static class Node<D> {
        private D d;
        private Node<D> prev;
        private Node<D> next;

        public Node(D d, Node<D> prev, Node<D> next) {
            this.d = d;
            this.prev = prev;
            this.next = next;
        }
    }
}

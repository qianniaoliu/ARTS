package com.structure.algorithm.sort;

/**
 * @author yusheng
 */
public class InsertionSortDemo {
    public static void main(String[] args) {
        int[] arr = new int[]{22,12,32,43,12,234,122};
        insertionSort(arr, arr.length);
        System.out.printf("11");
    }

    public static void insertionSort(int[] a, int n){
        if(n <= 1){
            return;
        }
        for(int i = 1; i < n; i++){
            int value = a[i];
            int j = i - 1;
            for(; j >=0; j--){
                if(value < a[j]){
                    a[j + 1] = a[j];
                }else {
                    break;
                }
            }
            a[j + 1] = value;
        }
    }
}

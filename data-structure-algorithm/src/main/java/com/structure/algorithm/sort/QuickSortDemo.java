package com.structure.algorithm.sort;

/**
 * 递推公式：quick_sort(p...r) = quick_sort(p...q-1) + quick_sort(q+1...r)
 *
 * @author yusheng
 */
public class QuickSortDemo {

    public static void main(String[] args) {
        int[] arr = new int[]{22, 12, 32, 43, 12, 234, 122};
        doQuickSort(arr, 0, arr.length - 1);
        System.out.println(111);
    }

    private static void doQuickSort(int[] arr, int p, int r) {
        if (p > r) {
            return;
        }
        int q = partition(arr, p, r);
        doQuickSort(arr, p, q - 1);
        doQuickSort(arr, q + 1, r);
    }

    private static int partition(int[] arr, int p, int r) {
        int k = (p + r) /2;
        int pivot = arr[k];
        arr[k] = arr[r];
        arr[r] = pivot;
        int i = p;
        for (int j = p; j < r; j++) {
            if (arr[j] <= pivot) {
                int tmp = arr[i];
                arr[i] = arr[j];
                arr[j] = tmp;
                i++;
            }
        }
        int tmp = arr[i];
        arr[i] = pivot;
        arr[r] = tmp;
        return i;
    }
}

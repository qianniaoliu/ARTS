/*
 * Ant Group
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.arts.altor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author shenlong
 * @version Solution.java, v 0.1 2022年10月31日 3:03 PM shenlong
 */
public class Solution {

    public static void main(String[] args) {
        //sortArrayByParity();
        //longestPalindrome("fcabacg");
        //mergeArray();
        //removeDupArray();
        //allPathsSourceTarget();
        lengthOfLongestSubstring("abbacdsccsd");
    }

    /**
     * 计算不重复子串的最大长度
     *
     * @param s
     */
    public static void lengthOfLongestSubstring(String s) {
        Set<Character> data = new HashSet<>();
        int l = s.length();
        int m = 0, n = -1;
        for (int i = 0; i < l; i++) {
            if (i > 0) {
                data.remove(s.charAt(i - 1));
            }
            while ((n + 1) < l && !data.contains(s.charAt(n + 1))) {
                data.add(s.charAt(n + 1));
                n++;
            }
            m = Math.max(m, n - i + 1);
        }
        System.out.println(m);
    }

    static List<List<Integer>> ans   = new ArrayList<>();
    static Deque<Integer>      stack = new ArrayDeque<>();

    public static List<List<Integer>> allPathsSourceTarget() {
        // graph = [[1,2],[3],[3],[]]
        // [[0,1,3],[0,2,3]]
        int[][] graph = new int[5][];
        graph[0] = new int[] {4, 3, 1};
        graph[1] = new int[] {3, 2, 4};
        graph[2] = new int[] {3};
        graph[3] = new int[] {4};
        graph[4] = new int[] {};
        stack.offerLast(0);
        dfs(graph, 0, graph.length - 1);
        return ans;
    }

    public static void dfs(int[][] graph, int x, int n) {
        if (x == n) {
            ans.add(new ArrayList<>(stack));
            return;
        }
        for (int y : graph[x]) {
            stack.offerLast(y);
            dfs(graph, y, n);
            stack.pollLast();
        }
    }

    public static void mergeArray() {
        int[][] arr = new int[4][2];
        arr[0] = new int[] {1, 3};
        arr[1] = new int[] {2, 6};
        arr[2] = new int[] {15, 18};
        arr[3] = new int[] {8, 10};
        Arrays.sort(arr, Comparator.comparingInt(o -> o[0]));
        List<int[]> mergeList = new ArrayList<>();
        for (int i = 0; i < arr.length; i++) {
            if (mergeList.size() == 0) {
                mergeList.add(arr[i]);
            } else {
                int[] item = mergeList.get(mergeList.size() - 1);
                // 判断当前数组的左边界是否大于merge中最后一个元素的左边界
                if (item[1] < arr[i][0]) {
                    // 如果大于，表示没有重合的，直接添加到merge数组中
                    mergeList.add(arr[i]);
                } else {
                    // 如果小于或等于，表示有重合的，需要对比右边界的值
                    if (item[1] < arr[i][1]) {
                        item[1] = arr[i][1];
                    }
                }
            }
        }
        System.out.println(mergeList);
    }

    public static void removeDupArray() {
        int[][] arr = new int[4][2];
        arr[0] = new int[] {1, 2};
        arr[1] = new int[] {2, 3};
        arr[2] = new int[] {3, 4};
        arr[3] = new int[] {1, 3};
        Arrays.sort(arr, Comparator.comparingInt(o -> o[0]));
        List<int[]> mergeList = new ArrayList<>();
        int m = 0;
        for (int i = 0; i < arr.length; i++) {
            if (mergeList.size() == 0) {
                mergeList.add(arr[i]);
            } else {
                int[] item = mergeList.get(mergeList.size() - 1);
                if (item[1] <= arr[i][0]) {
                    mergeList.add(arr[i]);
                } else {
                    m++;
                }
            }
        }
        System.out.println(m);
    }

    public static void longestPalindrome(String s) {
        if (s.length() <= 1) {
            return;
        }
        int start = 0;
        int end = 0;
        for (int i = 0; i < s.length(); i++) {
            // 回文字符串长度为奇数
            int a = expandAroundCenter(s, i, i);
            // 回文字符串长度为偶数
            int b = expandAroundCenter(s, i, i + 1);
            int max = Math.max(a, b);
            if (max > end - start) {
                start = i - (max - 1) / 2;
                end = i + max / 2;
            }
        }
        System.out.println(s.substring(start, end + 1));
    }

    public static int expandAroundCenter(String s, int left, int right) {
        while (left >= 0 && right < s.length() && s.charAt(left) == s.charAt(right)) {
            left--;
            right++;
        }
        return right - left - 1;
    }

    public static void sortArrayByParity() {
        int[] arr = {1, 2, 3, 4, 5, 6, 7, 8};
        int left = 0;
        int right = arr.length - 1;
        while (left < right) {
            if (arr[left] % 2 == 1 && arr[right] % 2 == 0) {
                int tmp = arr[left];
                arr[left] = arr[right];
                arr[right] = tmp;
            }
            while (arr[left] % 2 == 0) {
                left++;
            }
            while (arr[right] % 2 == 1) {
                right--;
            }
        }
        Arrays.stream(arr).forEach(System.out::println);
    }
}
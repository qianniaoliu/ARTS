package com.arts.jvm;

/**
 * @author yusheng
 */
public class OverLoadDemo {

    private int tryBlock;
    private int catchBlock;
    private int finallyBlock;
    private int methodExit;

    public String getData(String param) {
        return "demo1";
    }

    public static void main(String[] args) {
        String app = "";
        try {
            for(int i = 0; i< 10000000; i++) {
                new OverLoadDemo().getData("123");
            }
            System.gc();
            for(int i = 0; i< 10000000; i++) {
                new OverLoadDemo().getData("123");
            }
            System.gc();
            for(int i = 0; i< 10000000; i++) {
                new OverLoadDemo().getData("123");
            }
        } catch (IllegalArgumentException ex) {
        } catch (Exception ex) {
        } finally {
            app = "appName";
        }
    }

    public void checkException() {
        for (int i = 0; i < 100; i++) {
            try {
                tryBlock = 0;
                if (i < 50) {
                    continue;
                } else if (i < 80) {
                    break;
                } else {
                    return;
                }
            } catch (Exception e) {
                catchBlock = 1;
            } finally {
                finallyBlock = 2;
            }
        }
        methodExit = 3;
    }

}

package com.arts.jvm;

/**
 * @author yusheng
 */
public class OverWriteDemo {

    interface Customer{
        boolean isVip();
    }

    class Merchant{
        public Number actionPrice(double price, Customer customer){
            return 1;
        }
    }

    class NaviteMerchant extends Merchant{
        @Override
        public Number actionPrice(double price, Customer customer) {
            return 2;
        }
    }
}

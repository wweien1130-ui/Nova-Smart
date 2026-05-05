package com.liang.agent.constants;

public interface Constant {


    String STOP = "STOP";
    String ID = "id";


    interface Tools{




        // 商品搜索
        String SEARCH_ITEMS = "searchItems";
        String QUERY_ITEM_BY_ID = "queryItemById";
        // 购物车
        String ADD_TO_CART = "addToCart";
        String VIEW_CART = "viewCart";
        String REMOVE_FROM_CART = "removeFromCart";
        // 地址
        String LIST_ADDRESSES = "listAddresses";
        String ADD_ADDRESS = "addAddress";
        String UPDATE_ADDRESS = "updateAddress";
        String DELETE_ADDRESS = "deleteAddress";
        // 订单
        String CREATE_ORDER = "createOrder";
        String QUERY_ORDER = "queryOrder";
        String QUERY_ORDERS = "queryOrders";
        String CANCEL_ORDER = "cancelOrder";
        String CONFIRM_RECEIPT = "confirmReceipt";
    }

    interface ToolParamns{
        // 商品
        String KEYWORD = "搜索关键词";
        String ITEM_ID = "商品ID";
        String ITEM_NAME = "商品名称";
        String ITEM_PRICE = "商品价格（单位：分）";
        String ITEM_IMAGE = "商品图片URL";
        String ITEM_SPEC = "商品规格";
        String ITEM_NUM = "购买数量";
        // 地址
        String ADDRESS_ID = "地址ID";
        String CONTACT = "联系人";
        String MOBILE = "手机号";
        String PROVINCE = "省份";
        String CITY = "城市";
        String TOWN = "区县";
        String STREET = "详细地址";
        String IS_DEFAULT = "是否默认地址";
        // 订单
        String ORDER_ID = "订单ID";
        String ADDRESS_ID_FOR_ORDER = "收货地址ID";
        String PAYMENT_TYPE = "支付类型（1-支付宝 2-微信 3-余额）";
        String ORDER_DETAILS = "订单商品详情列表";
    }

    String REQUEST_ID = "requestId";
    String USER_ID = "userId";
}

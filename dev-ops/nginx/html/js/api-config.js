/**
 * 前端所有接口路径集中在此文件，修改网关前缀/版本号时只改这里即可。
 * 依赖：先引入 common.js（提供 AppConfig 里的各服务根地址）。
 *
 * 用法：
 *   AppApi.market(AppApiPaths.mallIndex.queryGoodsPage)
 *   AppApi.order(AppApiPaths.order.queryUserOrderList)
 *   AppApi.login(AppApiPaths.login.checkLogin)
 */
(function (global) {
    function normBase(b) {
        if (b == null || b === "") return "";
        return String(b).replace(/\/+$/, "");
    }

    function join(base, path) {
        const p = path.startsWith("/") ? path : "/" + path;
        const b = normBase(base);
        return b + p;
    }

    function cfg() {
        return global.AppConfig || {};
    }

    /** 仅路径片段（不含域名端口），与 AppConfig 中的 base 拼接 */
    // 注意以下接口开头的gw会被网关层去掉
    global.AppApiPaths = {
        mallIndex: {
            queryCategoryTypeList: "/gw/api/v1/mall/index/query_category_type_list",
            queryActivityGoods: "/gw/api/v1/mall/index/query_activity_goods",
            queryGoodsPage: "/gw/api/v1/mall/index/query_goods_page",
            querySkuDetail: "/gw/api/v1/mall/index/query_sku_detail",
        },
        /** 普通商品：mall 防刷 + 锁库后异步落单（需 RocketMQ normal-order-create 消费） */
        mallTrade: {
            createNormalOrder: "/gw/api/v1/mall/trade/create_normal_order",
        },
        gbm: {
            // 拼团市场配置查询已迁移至 group-buy-service
            queryGroupBuyMarketConfig: "/gw/api/v1/group-buy/market/query_group_buy_market_config",
            queryGoodsList: "/gw/api/v1/group-buy/market/query_goods_list",
            createPayOrder: "/gw/api/v1/group-buy/trade/create_pay_order",
            refund: "/gw/api/v1/group-buy/trade/refund",
        },
        seckill: {
            queryGoodsList: "/gw/api/v1/seckill/market/query_goods_list",
            createPayOrder: "/gw/api/v1/seckill/trade/create_pay_order",
            refund: "/gw/api/v1/seckill/trade/refund",
            admin: {
                queryActivities: "/gw/api/v1/seckill/admin/query_activities",
                preheat: "/gw/api/v1/seckill/admin/preheat",
            },
        },
        order: {
            createOrder: "/gw/api/v1/order/create_order",
            getPayUrl: "/gw/api/v1/order/get_pay_url",
            refund: "/gw/api/v1/order/refund",
            queryUserOrderList: "/gw/api/v1/order/query_user_order_list",
            querySeckillOrder: "/gw/api/v1/order/query_seckill_order",
        },
        login: {
            checkLogin: "/gw/api/v1/pay/login/check_login",
            register: "/gw/api/v1/pay/login/register",
            weixinQrcodeTicket: "/gw/api/v1/pay/login/weixin_qrcode_ticket",
        },
    };

    global.AppApi = {
        market: function (path) {
            return join(cfg().groupBuyMarketUrl, path);
        },
        order: function (path) {
            return join(cfg().orderServiceUrl || cfg().groupBuyMarketUrl, path);
        },
        groupBuy: function (path) {
            return join(cfg().groupBuyServiceUrl || cfg().groupBuyMarketUrl, path);
        },
        seckill: function (path) {
            return join(cfg().seckillServiceUrl || cfg().groupBuyMarketUrl, path);
        },
        pay: function (path) {
            const b = cfg().payApiBase || cfg().groupBuyMarketUrl;
            return join(b, path);
        },
        login: function (path) {
            const b = cfg().loginApiUrl || cfg().payApiBase || cfg().groupBuyMarketUrl;
            return join(b, path);
        },
    };
})(window);

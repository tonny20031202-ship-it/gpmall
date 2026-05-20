package com.gpmall.order.biz.context;

import com.gpmall.order.biz.convert.TransConvert;
import com.gpmall.order.dal.persistence.StockMapper;
import lombok.Data;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 腾讯课堂搜索【咕泡学院】
 * 官网：www.gupaoedu.com
 * 风骚的Mic 老师
 * create-date: 2019/8/2-下午9:55
 * 交易相关的抽象
 */
@Data
public abstract class AbsTransHandlerContext implements TransHandlerContext {

    private String orderId;

    private TransConvert convert = null;

    private TransactionTemplate transactionTemplate;

    private StockMapper stockMapper;
}

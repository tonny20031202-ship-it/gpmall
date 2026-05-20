package com.gpmall.order.biz.handler;

import com.gpmall.commons.tool.exception.BaseBusinessException;
import com.gpmall.order.biz.context.CreateOrderContext;
import com.gpmall.order.biz.context.TransHandlerContext;
import com.gpmall.order.dal.entitys.OrderItem;
import com.gpmall.order.dal.entitys.Stock;
import com.gpmall.order.dal.persistence.OrderItemMapper;
import com.gpmall.order.dal.persistence.StockMapper;
import com.gpmall.order.dto.CartProductDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @Description: 扣减库存处理器
 * @Author： wz
 * @Date: 2019-09-16 00:03
 **/
@Component
@Slf4j
public class SubStockHandler extends AbstractTransHandler {
	@Autowired
	OrderItemMapper orderItemMapper;

	@Override
	public boolean isAsync() {
		return false;
	}

	@Override
	public boolean handle(TransHandlerContext context) {
		CreateOrderContext createOrderContext = (CreateOrderContext) context;
		StockMapper stockMapper = createOrderContext.getStockMapper();
		List<CartProductDto> cartProductDtoList = createOrderContext.getCartProductDtoList();
		List<Long> itemIds = createOrderContext.getBuyProductIds();
		itemIds.sort(Long::compareTo);
		List<Stock> list = stockMapper.findStocksForUpdate(itemIds);
		if(list==null||list.isEmpty()){
			throw new BaseBusinessException("库存未初始化");
		}
		if(list.size()!=itemIds.size()){
			throw new BaseBusinessException("有商品未初始化库存,请在如下商品id中检查库存状态："+itemIds.toString());

		}
		list.forEach(stock -> {
			cartProductDtoList.forEach(one -> {
				if (Objects.equals(one.getProductId(), stock.getItemId())) {
					if (stock.getStockCount() < one.getProductNum()) {
						throw new BaseBusinessException(stock.getItemId()+"库存不足");
					}
					stock.setLockCount(one.getProductNum().intValue());
					stock.setStockCount(-one.getProductNum());
					stockMapper.updateStock(stock);
					return;
				}
			});
		});
		return true;
	}
}
package com.gpmall.search.services;


import com.google.common.collect.Lists;
import com.gpmall.search.ProductSearchService;
import com.gpmall.search.constant.PageInfo;
import com.gpmall.search.constant.SearchConstants;
import com.gpmall.search.converter.ProductConverter;
import com.gpmall.search.dto.ProductDto;
import com.gpmall.search.dto.SearchRequest;
import com.gpmall.search.dto.SearchResponse;
import com.gpmall.search.entity.ItemDocument;
import com.gpmall.search.repository.ProductRepository;
import com.gpmall.search.utils.ExceptionProcessorUtils;
import jdk.nashorn.internal.runtime.GlobalConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Service;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 商品搜索服务类 对商品搜索接口API进行实现
 * 并对外提供商品搜索服务
 *
 * @author jin
 * @version v1.0.0
 * @Date 2019年8月10日
 */
@Slf4j
@Service
public class ProductSearchServiceImpl implements ProductSearchService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    ProductConverter productConverter;

	@Autowired
	private RedissonClient redissonClient;

    @Override
    public SearchResponse search(SearchRequest request) {

        SearchResponse response = new SearchResponse();
		try {
            request.requestCheck();
            staticsSearchHotWord(request);

            BoolQueryBuilder boolQueryBuilder = buildSearchQuery(request);

            Sort sort = buildSort(request);
            Pageable pageable = buildPageable(request, sort);

            Iterable<ItemDocument> elasticRes =
                    productRepository.search(boolQueryBuilder, pageable);
            ArrayList<ItemDocument> itemDocuments = Lists.newArrayList(elasticRes);

            List<ProductDto> productDtos = productConverter.items2Dto(itemDocuments);
            response.ok(productDtos);
        }catch (Exception e){
            e.printStackTrace();
            log.error("ProductSearchServiceImpl.search Occur Exception :" + e);
            ExceptionProcessorUtils.wrapperHandlerException(response,e);
        }
        return response;
    }

    private BoolQueryBuilder buildSearchQuery(SearchRequest request) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        if (StringUtils.isNotBlank(request.getKeyword())) {
            boolQuery.must(QueryBuilders.matchQuery("title", request.getKeyword()));
        }

        if (request.getCid() != null) {
            boolQuery.filter(QueryBuilders.termQuery("cid", request.getCid()));
        }

        if (request.getPriceGt() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("price").gt(request.getPriceGt()));
        }
        if (request.getPriceLte() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("price").lte(request.getPriceLte()));
        }

        return boolQuery;
    }

    private Sort buildSort(SearchRequest request) {
        if ("1".equals(request.getSort())) {
            return Sort.by(Sort.Direction.ASC, "price");
        } else if ("-1".equals(request.getSort())) {
            return Sort.by(Sort.Direction.DESC, "price");
        }
        return null;
    }

    private Pageable buildPageable(SearchRequest request, Sort sort) {
        int currentPage = request.getCurrentPage() != null ? request.getCurrentPage() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;
        if (sort != null) {
            return PageRequest.of(currentPage - 1, pageSize, sort);
        }
        return PageRequest.of(currentPage - 1, pageSize);
    }


	@Override
    public SearchResponse fuzzySearch(SearchRequest request) {
        SearchResponse response = new SearchResponse();
        try {
            request.requestCheck();
			//统计搜索热词
			staticsSearchHotWord(request);
            // 分页
			PageInfo pageInfo=new PageInfo();
			pageInfo.setPageNumber(request.getCurrentPage());
			pageInfo.setPageSize(request.getPageSize());
			pageInfo.setSort(new Sort(Sort.Direction.DESC,request.getSort()));
            Page<ItemDocument> elasticRes =
                    productRepository.search(QueryBuilders.matchQuery("title",request.getKeyword()),pageInfo);
            ArrayList<ItemDocument> itemDocuments = Lists.newArrayList(elasticRes);
            List<ProductDto> productDtos = productConverter.items2Dto(itemDocuments);
            response.setTotal(elasticRes.getTotalElements());
            response.ok(productDtos);
        }catch (Exception e){
            e.printStackTrace();
            log.error("ProductSearchServiceImpl.fuzzySearch Occur Exception :" + e);
            ExceptionProcessorUtils.wrapperHandlerException(response,e);
        }
        return response;
    }

	/**
	 * 统计搜索热词
	 * @param request request
	 */
	private void staticsSearchHotWord(SearchRequest request) {
		//搜索词
		String keyword = request.getKeyword();
		if(StringUtils.isNotEmpty(keyword)){
			RScoredSortedSet<Object> scoredSortedSet = redissonClient.getScoredSortedSet(getSearchHotWordRedisKey());
			Double score = scoredSortedSet.getScore(keyword);
			if(score!=null){
				scoredSortedSet.addAndGetRank(score+1.0,keyword);
			}else{
				scoredSortedSet.addScore(keyword,1);

			}

		}
	}

    @Override
	@SuppressWarnings("unchecked")
    public SearchResponse hotProductKeyword() {
    	//商品热门搜索关键字
		//获取到分数第一的 搜索词
		SearchResponse response = new SearchResponse();
		try {
			RScoredSortedSet<Object> scoredSortedSet = redissonClient.getScoredSortedSet(getSearchHotWordRedisKey());
			Object first = scoredSortedSet.first();
			if(!Objects.isNull(first)){
				response.ok(Collections.singletonList(first));
			}
		}catch (Exception e){
			e.printStackTrace();
			log.error("ProductSearchServiceImpl.hotProductKeyword Occur Exception :" + e);
			ExceptionProcessorUtils.wrapperHandlerException(response,e);
		}

		return response;
    }

    private String getSearchHotWordRedisKey() {
		return SearchConstants.SEARCH_HOT_WORD_CACHE_KEY;
	}
}

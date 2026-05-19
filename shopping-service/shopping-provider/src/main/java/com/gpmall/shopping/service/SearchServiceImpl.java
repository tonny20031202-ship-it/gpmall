package com.gpmall.shopping.service;

import com.google.common.collect.Lists;
import com.gpmall.search.converter.ProductConverter;
import com.gpmall.search.dto.ProductDto;
import com.gpmall.search.dto.SearchRequest;
import com.gpmall.search.dto.SearchResponse;
import com.gpmall.search.entity.ItemDocument;
import com.gpmall.search.repository.ProductRepository;
import com.gpmall.shopping.constants.ShoppingRetCode;
import com.gpmall.shopping.utils.ExceptionProcessorUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Service;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class SearchServiceImpl {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductConverter productConverter;

    public SearchResponse search(SearchRequest request) {
        return doSearch(request, null);
    }

    public SearchResponse search(SearchRequest request, Long cid) {
        return doSearch(request, cid);
    }

    private SearchResponse doSearch(SearchRequest request, Long cid) {
        SearchResponse response = new SearchResponse();
        try {
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolean hasAnyClause = false;

            if (StringUtils.isNotBlank(request.getKeyword())) {
                boolQueryBuilder.must(QueryBuilders.matchQuery("title", request.getKeyword()));
                hasAnyClause = true;
            }

            if (cid != null) {
                boolQueryBuilder.filter(QueryBuilders.termQuery("cid", cid));
                hasAnyClause = true;
            }

            if (request.getPriceGt() != null && request.getPriceLte() != null) {
                boolQueryBuilder.filter(
                        QueryBuilders.rangeQuery("price").gt(request.getPriceGt()).lte(request.getPriceLte()));
                hasAnyClause = true;
            } else if (request.getPriceGt() != null) {
                boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").gt(request.getPriceGt()));
                hasAnyClause = true;
            } else if (request.getPriceLte() != null) {
                boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").lte(request.getPriceLte()));
                hasAnyClause = true;
            }

            if (!hasAnyClause) {
                response.setCode(ShoppingRetCode.SUCCESS.getCode());
                response.setMsg(ShoppingRetCode.SUCCESS.getMessage());
                response.setTotal(0L);
                response.setData(Collections.emptyList());
                return response;
            }

            Sort sort = null;
            if ("1".equals(request.getSort())) {
                sort = new Sort(Sort.Direction.ASC, "price");
            } else if ("-1".equals(request.getSort())) {
                sort = new Sort(Sort.Direction.DESC, "price");
            }

            Pageable pageable = new PageRequest(request.getCurrentPage() - 1, request.getPageSize());
            if (sort != null) {
                pageable = new PageRequest(request.getCurrentPage() - 1, pageable.getPageSize(), sort);
            }

            Iterable<ItemDocument> elasticRes =
                    productRepository.search(boolQueryBuilder, pageable);
            List<ItemDocument> itemDocuments = Lists.newArrayList(elasticRes);
            List<ProductDto> productDtos = productConverter.items2Dto(itemDocuments);
            response.ok(productDtos);
        } catch (Exception e) {
            log.error("SearchServiceImpl.doSearch Occur Exception :", e);
            ExceptionProcessorUtils.wrapperHandlerException(response, e);
        }
        return response;
    }
}
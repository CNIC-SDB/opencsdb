package com.linkeddata.portal.service;

import com.linkeddata.portal.entity.QueryResultForQuestion2Request;
import com.linkeddata.portal.entity.QueryResultForQuestion3Request;
import com.linkeddata.portal.entity.SearchResultEntity;
import com.linkeddata.portal.entity.TableDataEntity;
import com.linkeddata.portal.entity.semanticSearch.PathQueryResult;
import com.linkeddata.portal.entity.semanticSearch.QuestionParseResult;
import com.linkeddata.portal.entity.semanticSearch.SemanticSearchResult;
import com.linkeddata.portal.entity.semanticSearch.SparqlBuilderEntity;

import java.util.List;

/**
 * 语义检索
 *
 * @author 陈锟
 * @date 2023年3月7日15:54:42
 */
public interface SemanticSearchService {

    /**
     * 语义检索
     *
     * @param question
     * @param endpoints
     * @param queryType
     * @return SemanticSearchResult
     * @author 陈锟
     * @date 2023年3月7日15:54:42
     */
    SemanticSearchResult getSemanticSearchResult(String question, String endpoints, String queryType);

    /**
     * 判断端点是否可被访问
     *
     * @param endpoint
     * @return
     */
    Boolean isAccessible(String endpoint);

    /**
     * <h1>弃用</h1>
     * 使用 大模型+KOR 识别问句类型, 2023-10-15 后可删除
     * @param question
     * @return
     */
    @Deprecated(since = "2023-09-19")
    QuestionParseResult getQuestionType_(String question);


    /**
     * 使用 大模型+KOR 识别问句类型获取问句的类型
     *
     * @param question
     * @return
     */
    QuestionParseResult getQuestionType(String question);

    /**
     * @param x         起点
     * @param y         终点
     * @param type      问句类型
     * @param endpoints 所选端点
     * @return
     */
    List<SparqlBuilderEntity> listSparqlInSameClass(String x, String y, String type, String endpoints);

    /**
     * x和y有无关联，x,y属于同一类生成sparql语句查询
     *
     * @param sparqlList
     * @param startName  起点实体名称
     * @param endName    终点实体名称
     * @return
     */
    List<PathQueryResult> findInSameClass(List<SparqlBuilderEntity> sparqlList, String startName, String endName);


    /**
     * xiajl20230725 根据问句获取检索结果的来源信息;
     * @param question
     * @return
     */
    List<SearchResultEntity> getSearchResultEntity(String question);


    /**
     * xiajl20230725 根据问句1-4获取表格数据
     *
     * @param question
     * @return
     */
    List<TableDataEntity> getTableDataEntity(String question);

    String findImage(String uri);

    /**
     * 让大模型总结问句类型2的结果
     *
     * @param request
     * @return
     * @author chenkun
     * @since 2023年9月28日16:29:29
     */
    String queryResultForQuestion2(QueryResultForQuestion2Request request);

    /**
     * 让大模型总结问句类型3的结果
     *
     * @param request
     * @return
     * @author chenkun
     * @since 2023年9月28日16:29:29
     */
    String queryResultForQuestion3(QueryResultForQuestion3Request request);

}

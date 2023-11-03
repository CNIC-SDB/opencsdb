package com.linkeddata.portal.service.impl;

import com.linkeddata.portal.entity.ExportRequest;
import com.linkeddata.portal.entity.PageTools;
import com.linkeddata.portal.entity.es.*;
import com.linkeddata.portal.entity.mongo.Dataset;
import com.linkeddata.portal.entity.resource.*;
import com.linkeddata.portal.service.DatasetService;
import com.linkeddata.portal.service.ResourceEntityService;
import com.linkeddata.portal.service.helper.ResourceEntityHelper;
import com.linkeddata.portal.utils.RdfUtils;
import com.linkeddata.portal.utils.SemanticSearchUtils;
import com.linkeddata.portal.utils.threadpool.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 资源实体 实现类
 *
 * @author wangzhiliang
 */
@Service
@Slf4j
public class ResourceEntityServiceImpl implements ResourceEntityService {
    @Autowired(required = false)
    private MongoTemplate mongoTemplate;
    @Value("${sparql.endpoint}")
    private String endpoint;
    @javax.annotation.Resource
    private DatasetService datasetService;
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Override
    public PageTools<List<ResourceList>> getResourceList(ResourceListRequest request) {
        if( ( request.getDomain().length == 0)  && (request.getInstitution().length == 0)  && StringUtils.isBlank(request.getEsFlag()) ){
            return null;
        }
        //查询 mongo 获取所有端点
        Query query = new Query();
        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = new ArrayList<>();
        if ( null != request.getDomain() && request.getDomain().length > 0) {
            for (String domain: request.getDomain()) {
                criteriaList.add(Criteria.where("domain"). is(domain));
            }
        }
        if (null != request.getInstitution()  && request.getInstitution().length >0) {
            for (String institution: request.getInstitution()) {
                criteriaList.add(Criteria.where("unitName").is(institution));
            }
        }
        if (StringUtils.isNotBlank(request.getDatasetId())) {
            query.addCriteria(Criteria.where("id").is(request.getDatasetId()));
        }
        if(criteriaList.size()>0){
            criteria.orOperator(criteriaList.toArray(new Criteria[0]));
            query.addCriteria(criteria);
        }

        List<Dataset> dataSetList = mongoTemplate.find(query, Dataset.class);
        List<ResourceList> resourceLists = new LinkedList<>();
        long total = 0L;
        if (dataSetList.size() > 0) {
            String newEndPoint = "";
            if (StringUtils.isNotBlank(request.getDatasetId())) {
                newEndPoint = dataSetList.get(0).getSparql();
            }

            // 由于最新版virtuoso中要求graph名称必须为uri，我们目前的graph名称不为uri，因此无法进行SPARQL查询。需要将'default-graph-uri'改为'from'的形式。update by 陈锟 2023年5月19日17:15:51
            // sparql 样例值：http://ioz.semweb.csdb.cn/sparql?default-graph-uri=CoLChina_sp2000
            // 拼接FROM子句
            String fromGraph = "";
            if (newEndPoint.contains("?") && newEndPoint.contains("default-graph-uri")) {
                // 根据问号分隔
                String[] splitArr = newEndPoint.split("\\?");
                for (String str : splitArr[1].split("&")) {
                    if (!str.equals("")) {
                        String graph = str.split("=")[1];
                        if (!graph.equals("")) {
                            fromGraph += " FROM <" + graph + ">";
                        }
                    }
                }
                newEndPoint = splitArr[0];
            }

            StringBuilder subjectStr = new StringBuilder();
            RdfUtils.setPreFix(subjectStr);
            //查询主语
            subjectStr.append("SELECT DISTINCT ?s " + fromGraph + " WHERE { \n");
            //公共查询获取
            subjectStr.append(ResourceEntityHelper.getListMiddleStr(dataSetList, request).toString());
            subjectStr.append("}");
            subjectStr.append(" OFFSET ").append((request.getPageNum() - 1) * request.getPageSize());
            subjectStr.append(" limit ").append(request.getPageSize());
//            log.info("查询主语语句： {}", subjectStr.toString());
            ResultSet resultSet = null;
            if (StringUtils.isNotBlank(request.getDatasetId())) {
                resultSet = RdfUtils.queryTriple(newEndPoint, subjectStr.toString());
            } else {
                resultSet = RdfUtils.queryTriple(endpoint, subjectStr.toString());
            }

            List<Map<String, Object>> resultMapList = RdfUtils.resultEncapsulation(resultSet);
            for (int i = 0; i < resultMapList.size(); i++) {
                //查询列表
                StringBuilder listStr = new StringBuilder();
                RdfUtils.setPreFix(listStr);
                listStr.append("SELECT ?sparql ?s ?p ?o " + fromGraph + " WHERE { \n ");
                listStr.append(ResourceEntityHelper.queryTripleBySubject(dataSetList, String.valueOf(resultMapList.get(i).get("s")), request));
                //如果有多个别名 使用 orderby 主语使 多个相同主语在一起
                listStr.append("} order by (?s) ");
//                log.info("查询列表语句： {}", listStr.toString());
                ResultSet resultSetList = null;
                if (StringUtils.isNotBlank(request.getDatasetId())) {
                    resultSetList = RdfUtils.queryTriple(newEndPoint, listStr.toString());
                } else {
                    resultSetList = RdfUtils.queryTriple(endpoint, listStr.toString());
                }
                //返回数据封装
                resourceLists.add(ResourceEntityHelper.encapsulationResult(resultSetList, dataSetList));
            }
            if (StringUtils.isBlank(request.getEsFlag())) {
              //查询资源总量
              StringBuilder countStringBuilder = new StringBuilder();
              RdfUtils.setPreFix(countStringBuilder);
              countStringBuilder.append("SELECT (count(*) AS ?count)  " + fromGraph + "  WHERE { \n");
              countStringBuilder.append("\tSELECT  distinct ?s   WHERE { \n");
              countStringBuilder.append(ResourceEntityHelper.getListMiddleStr(dataSetList, request).toString());
              if (StringUtils.isBlank(request.getDatasetId())) {
                  countStringBuilder.append("\t}\n");
              } else {
                  countStringBuilder.append("\t} LIMIT 100\n");
              }
              countStringBuilder.append("}\n");
//              log.info("查询总数语句： {}", countStringBuilder.toString());
              ResultSet contResult = null;
              if (StringUtils.isNotBlank(request.getDatasetId())) {
                  contResult = RdfUtils.queryTriple(newEndPoint, countStringBuilder.toString());
              } else {
                  contResult = RdfUtils.queryTriple(endpoint, countStringBuilder.toString());
              }
              List<Map<String, Object>> countMaps = RdfUtils.resultEncapsulation(contResult);
              total = (int) countMaps.get(0).get("count");
          }
        }
        //分页
        PageTools<List<ResourceList>> page = null;
        // es 缓存 数据实体列表返回
        if (StringUtils.isNotBlank(request.getEsFlag()) && "true".equals(request.getEsFlag())) {
            page = new PageTools<>(resourceLists);
        } else {
            page = new PageTools<>(request.getPageSize(), (int) total, request.getPageNum(), resourceLists);
        }

        return page;
    }

    @Override
    public ResourceDetail getResourceDetail(ResourceDetailRequest request) {
        //查询 mongo 获取所有端点
        List<Dataset> dataSetList = mongoTemplate.findAll(Dataset.class);
        String queryRdf = ResourceEntityHelper.detailRdfQuery(dataSetList, request.getSubject());
//        log.info("查询语句 \n {}", queryRdf);
        ResultSet resultSet = RdfUtils.queryTriple(endpoint, queryRdf);
        ResourceDetail resourceDetail = null;
        List<ResourceDataSet> dataSets = new ArrayList<>();
        ResourceDataSet resourceDataSet = null;
        String sparqlUrl = "";
        String title = "";
        List<ResourceDetailLine> detailLines = new ArrayList<>();
        Map<String, String> predicateLabelMap = new HashMap<>();
        while (resultSet.hasNext()) {
            QuerySolution solution = resultSet.nextSolution();
            Resource sparql = solution.getResource("sparql");
            Resource subject = solution.getResource("s");
            Resource predicate = solution.getResource("p");
            RDFNode  object = solution.get("o");
            if (ObjectUtils.isEmpty(resourceDetail)) {
                resourceDetail = new ResourceDetail();
                resourceDetail.setSubject(subject.getURI());
                resourceDetail.setSubjectShort(SemanticSearchUtils.dealPrefixReturnShort(subject.getURI()));
            }
            //如果 pLabel 值为label代表三元组的 type 类型为 rdf:label 就要获取 object 的值
            //首先获取一个默认值， 再找到第一个英文语言的时候 赋予新的值
            if (StringUtils.isBlank(title) && RDFS.label.getURI().equals(predicate.getURI())) {
                if ("en".equals(object.asLiteral().getLanguage())) {
                    title = object.asLiteral().getString();
                    //覆盖已经存在的title
                    resourceDetail.setTitle(title.trim());
                }
                if (StringUtils.isBlank(resourceDetail.getTitle())) {
                    resourceDetail.setTitle(object.isLiteral() ? object.asLiteral().getString().trim() : "");
                }
            }
            if (StringUtils.isBlank(resourceDetail.getUnitName()) && RDF.type.getURI().equals(predicate.getURI())) {
                //放置UnitName dataSetName
                for (Dataset dataSet : dataSetList) {
                    if (sparql.getURI().equals(dataSet.getSparql())) {
                        resourceDetail.setUnitName(dataSet.getUnitName());
                        resourceDetail.setWebsite(dataSet.getWebsite());
                        resourceDetail.setPublishDate(dataSet.getPublishTime());
                        resourceDetail.setDataSetImage(dataSet.getImage());
                        resourceDetail.setDatasetName(dataSet.getTitle());
                        resourceDetail.setIdentifier(dataSet.getIdentifier());
                    }
                }
            }
            //封装每行内容
            ResourceDetailLine resourceDetailLine = new ResourceDetailLine();
            resourceDetailLine.setPredicate(predicate.getURI());
            String shortPre = SemanticSearchUtils.dealPrefixReturnShort(predicate.getURI());
            resourceDetailLine.setShortPre(shortPre);
            String preLabel = predicateLabelMap.get(shortPre);
            //如果在所有的谓语缩写本体 label 中没有 这个对应的值则去本体库图中查询
            if (StringUtils.isBlank(predicateLabelMap.get(shortPre))) {
                preLabel =  ResourceEntityHelper.queryPreLabelByOntology(predicate.getURI(),predicateLabelMap);
            }
            resourceDetailLine.setPreLabel(preLabel);
            Set<ResourceContent> contents = new HashSet<>();
            if (null != resourceDetailLine.getContents()) {
                contents.addAll(resourceDetailLine.getContents());
            }
            //宾语信息封装

            contents.add(ResourceEntityHelper.handlerResourceContent(predicate, object, dataSetList, endpoint, predicateLabelMap));
            resourceDetailLine.setContents(contents);
            //如果 sparql 端点不一样了那么说明是一个新的端点数据
            if (!sparqlUrl.equals(sparql.getURI())) {
                if (StringUtils.isNotBlank(sparqlUrl) && null != resourceDataSet) {
                    resourceDataSet.setDetailLines(detailLines);
                    dataSets.add(resourceDataSet);
                    detailLines = new ArrayList<>();
                }
                resourceDataSet = new ResourceDataSet();
                resourceDataSet.setSparql(sparql.getURI());
                for (Dataset dataset : dataSetList) {
                    if (sparql.getURI().equals(dataset.getSparql())) {
                        resourceDataSet.setDataSetName(dataset.getTitle());
                    }
                }
                sparqlUrl = sparql.getURI();
            }
            detailLines.add(resourceDetailLine);
        }
        if (null != resourceDataSet) {
            resourceDataSet.setDetailLines(detailLines);
            dataSets.add(resourceDataSet);
        }
        if (null != resourceDetail) {
            resourceDetail.setDataSets(dataSets);
        }
        return resourceDetail;
    }

    @Override
    public Map listRelationResource(String iri) {
        // 测试 iri = http://www.chemdb.csdb.cn/compound/2846-85-7
        /*
            返回的Map，返回echarts可用格式
            categories 所有数据集名称，作为图例，用于标记不同资源所属数据集，不同数据集不同颜色
           categories:[{name: "A"}, {name: "B"}, {name: "C"}, {name: "D"}, {name: "E"}, {name: "F"}, {name: "G"},…]

            从哪个节点（source）到哪个节点（target），predicate 谓语，A和B之间的关系
            links:[{source: "1",predicate:"", target: "0"}, {source: "2", target: "0"}, {source: "3", target: "0"},…]

            每个节点信息
            nodes:[{id: "0", name: "URI简写", symbolSize: 19.12381, x: -266.82776, y: 299.6904, value: 28.685715,…},…]
         */
        Map resultMap = new HashMap(16);

        // 从所有的端点中查询出数据
        List<Dataset> datasetList = datasetService.listDatasets();
        StringBuilder sparqlStr = new StringBuilder();
        RdfUtils.setPreFix(sparqlStr);
        sparqlStr.append("SELECT DISTINCT ?s ?p ?o ?endpoint ");
        sparqlStr.append("WHERE {  ");
        for (int i = 0; i < datasetList.size(); i++) {
            Dataset dataset = datasetList.get(i);
            if (i > 0) {
                sparqlStr.append("union");
            }
            sparqlStr.append("{ SERVICE SILENT <").append(dataset.getSparql()).append("> ");
            sparqlStr.append("{");
            sparqlStr.append("BIND( '").append(dataset.getSparql()).append("' AS ?endpoint ) ");
            sparqlStr.append("{ ");
            sparqlStr.append("?s ?p ?o . ");
            sparqlStr.append("FILTER ( ?s = <" + iri + "> || ?o = <" + iri + "> ) ");
            sparqlStr.append("} ");
            sparqlStr.append("UNION { ");
            sparqlStr.append("?s1 ?p1 ?s .  ?s ?p ?o . ");
            sparqlStr.append("FILTER ( ?s1 = <").append(iri).append("> ) ");
            sparqlStr.append("} ");
            sparqlStr.append("UNION { ");
            sparqlStr.append("?s ?p ?o .   ?o ?p1 ?o1 . ");
            sparqlStr.append("FILTER ( ?o1 = <").append(iri).append("> ) ");
            sparqlStr.append("} } } ");
        }
        sparqlStr.append("} LIMIT 1000 ");
        ResultSet resultSet = null;
        try {
            resultSet = RdfUtils.queryTriple(endpoint, sparqlStr.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> resourceList = RdfUtils.resultEncapsulation(resultSet);

        // endpoint去重
        Set<String> categorieSet = new HashSet();
        for (Map map : resourceList) {
            Object endpoint = map.get("endpoint");
            if (endpoint != null) {
                String endPoint = endpoint.toString();
                categorieSet.add(endPoint);
            }
        }
        // 返回 categories
        List<Map> categories = new ArrayList<>();
        for (String sparql : categorieSet) {
            Dataset dataset = datasetService.getDatasetBySparql(sparql);
            if (null != dataset) {
                Map map = new HashMap(16);
                map.put("name", dataset.getTitle());
                categories.add(map);
            }
        }

        // 返回links
        List<Map> links = new ArrayList<>();
        for (Map map : resourceList) {
            String subject = map.get("s") + "";
            String predicate = map.get("p") + "";
            String object = map.get("o") + "";
            String shortPredicate = predicate;
            if (predicate.startsWith("http")) {
                shortPredicate = SemanticSearchUtils.dealPrefixReturnShort(predicate);
            }
            Map linkMap = new HashMap(16);
            linkMap.put("source", subject.trim());
            linkMap.put("shortPredicate", shortPredicate);
            linkMap.put("predicate", predicate.trim());
            linkMap.put("target", object.trim());
            links.add(linkMap);
        }

        // 主语的nodes列表
        List<Map> subjectNodes = new ArrayList();

        /*
             新建map 以uri为key，endpoint为value
            判断此三元组是否在 去重set里，如果不在：
                 存入map
            如果在：
                判断 谓语是否是rdfs:label
                    如果是：
                        存入map，因map的key不能重复，所以会把相同的key（即谓语不是rdfs：label的替换）
                        主语存入去重set
                     如果不是：
                          不存入
             循环结束，此时map中是所有uri，endpoint的结合，且uri不重复，遍历此map。返回前端nodes数据
         */
        // 存放 主语，端点的map
        Map subjectMap = new HashMap();
        // 存放主语列表，用于去重
        Set<String> subjectSet = new HashSet<>();
        for (Map map : resourceList) {
            String subject = map.get("s") + "";
            String predicate = map.get("p") + "";
            String endpoint = map.get("endpoint") + "";
            // 判断是否已经添加过该主语
            if (subjectSet.contains(subject)) {
                // 如果添加过,判断当前三元组的谓语是否是rdf:label,如果是label则添加，否则不添加
                String shortPredicate = predicate;
                if (predicate.startsWith("http")) {
                    shortPredicate = SemanticSearchUtils.dealPrefixReturnShort(predicate);
                }
                if ("rdfs:label".equals(shortPredicate)) {
                    subjectMap.put(subject, endpoint);
                    subjectSet.add(subject);
                }
            } else {
                subjectMap.put(subject, endpoint);
                subjectSet.add(subject);
            }

            // 添加宾语，宾语要和主语放到一个map里，因为同一个主语有可能是另一个主语的宾语
            // 如果主语和宾语分别放到两个列表里，同一个主语可能会重复
            String object = map.get("o") + "";
            // 判断是否已经添加过该宾语资源
            if (subjectSet.contains(object)) {
            } else {
                subjectMap.put(object, endpoint);
                subjectSet.add(object);
            }
        }

        /*
        此时的map是多个
        subject endpoint
        subject endpoint
         */
        if (!subjectMap.isEmpty()) {
            Set<String> keySet = subjectMap.keySet();
            for (String key : keySet) {
                String sparql = subjectMap.get(key) + "";
                Dataset dataset = datasetService.getDatasetBySparql(sparql);
                Map map = new HashMap(16);
                map.put("id", key.trim());
                String shortSubject = key;
                if (key.startsWith("http")) {
                    shortSubject = SemanticSearchUtils.dealPrefixReturnShort(key);
                }
                map.put("name", shortSubject.trim());
                if (null != dataset) {
                    map.put("category", dataset.getTitle());
                } else {
                    map.put("category", "");
                }

                subjectNodes.add(map);
            }
        }

        resultMap.put("categories", categories);
        resultMap.put("links", links);
        resultMap.put("nodes", subjectNodes);

        return resultMap;
    }

    @Override
    public String exportToFileByGraph(ExportRequest request) {
        log.info("开启线程进行图数据内容导出");
        //校验参数
        if(StringUtils.isBlank(request.getExportSparql()) || StringUtils.isBlank(request.getExportFileName()) || StringUtils.isBlank(request.getExportGraph()) || StringUtils.isBlank(request.getExportPath())){
            return "参数有误";
        }
        ThreadUtil.callingThread(request);
        return "已开启子线程进行数据导出到文件请等候结果";
    }

    /**
     * 通过es获取资源列表--增加支持中心二级门户查询
     *
     * 查询流程：
     *      名称（title）或别名（label.value.value）中包含，权重最大，10000
     *      其余中包含，权重较小
     * @param request 请求参数
     * @return
     */
    @Override
    public PageTools<List<ResourceList>> getResourceListByES(ResourceListRequest request) {
        //判断是否为全文检索或二级门户检索的标记
        String datacenterId = request.getDatacenterId();
        //查询 mongo 获取identifier,作为查询字段
        Query queryMongo = new Query();
        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = new ArrayList<>();
        //全文检索
        if(null == datacenterId){
            //什么都不输入则返回空
            if( ( request.getDomain().length == 0)  && (request.getInstitution().length == 0)){
                return null;
            }

            if ( null != request.getDomain() && request.getDomain().length > 0) {
                for (String domain: request.getDomain()) {
                    criteriaList.add(Criteria.where("domain"). is(domain));
                }
            }
            if (null != request.getInstitution()  && request.getInstitution().length >0) {
                for (String institution: request.getInstitution()) {
                    criteriaList.add(Criteria.where("unitName").is(institution));
                }
            }
            if (StringUtils.isNotBlank(request.getDatasetId())) {
                queryMongo.addCriteria(Criteria.where("id").is(request.getDatasetId()));
            }
            if(criteriaList.size()>0){
                criteria.orOperator(criteriaList.toArray(new Criteria[0]));
                queryMongo.addCriteria(criteria);
            }
        }else {
            //二级门户检索
            if (StringUtils.isNotBlank(datacenterId)) {
                queryMongo.addCriteria(Criteria.where("datacenter_id").is(datacenterId));
            }
        }
        //设置高亮查询
        String pre = "<span style='color:red'>";
        String post = "</span>";
        //指定要高亮的字段将其加上头尾标签
        HighlightBuilder.Field datasetName = new HighlightBuilder.Field("datasetName").preTags(pre).postTags(post);
        HighlightBuilder.Field subjectShort = new HighlightBuilder.Field("subjectShort").preTags(pre).postTags(post);
        HighlightBuilder.Field title = new HighlightBuilder.Field("title").preTags(pre).postTags(post);
        HighlightBuilder.Field unitName = new HighlightBuilder.Field("unitName").preTags(pre).postTags(post);
        List<Dataset> dataSetList = mongoTemplate.find(queryMongo, Dataset.class);
        Map<String,Float> map = new HashMap<>();
        map.put("unitName",13f);
        map.put("datasetName",2f);
        map.put("title",10000f);
        map.put("subjectShort",QueryStringQueryBuilder.DEFAULT_BOOST);
        String condition = request.getCondition();
        condition = QueryParser.escape(condition);
        // 替换拉丁名中的 " " 为 "_",增强搜索准确度
        condition = condition.replaceAll("\s+", " ").replaceAll(" ", "_");
        QueryStringQueryBuilder queryBuilder = QueryBuilders.queryStringQuery(condition).fields(map);
//        Map<String,Float> mapForNested = new HashMap<>();
//        mapForNested.put("label.value.value",QueryStringQueryBuilder.DEFAULT_BOOST);
//        mapForNested.put("type.value.value",QueryStringQueryBuilder.DEFAULT_BOOST);
//        mapForNested.put("closeMatch.value.value",QueryStringQueryBuilder.DEFAULT_BOOST);
        //嵌套查询
        NestedQueryBuilder nestedQueryBuilderForLabel = QueryBuilders
                .nestedQuery("label.value", QueryBuilders.queryStringQuery(condition).field("label.value.value"), ScoreMode.Max)
                .innerHit(new InnerHitBuilder().setHighlightBuilder(new HighlightBuilder().preTags(pre).postTags(post).field("label.value.value")));
        NestedQueryBuilder nestedQueryBuilderForType = QueryBuilders
                .nestedQuery("type.value", QueryBuilders.queryStringQuery(condition).field("type.value.value"), ScoreMode.None)
                .innerHit(new InnerHitBuilder().setHighlightBuilder(new HighlightBuilder().preTags(pre).postTags(post).field("type.value.value")));
        NestedQueryBuilder nestedQueryBuilderForClose = QueryBuilders
                .nestedQuery("closeMatch.value", QueryBuilders.queryStringQuery(condition).field("closeMatch.value.value"), ScoreMode.None)
                .innerHit(new InnerHitBuilder().setHighlightBuilder(new HighlightBuilder().preTags(pre).postTags(post).field("closeMatch.value.value")));
        NestedQueryBuilder nestedQueryBuilderForSameAs = QueryBuilders
                .nestedQuery("sameAs.value", QueryBuilders.queryStringQuery(condition).field("sameAs.value.value"), ScoreMode.None)
                .innerHit(new InnerHitBuilder().setHighlightBuilder(new HighlightBuilder().preTags(pre).postTags(post).field("sameAs.value.value")));

        TermsQueryBuilder queryBuilderForIdentifier = null;
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if(dataSetList.size() > 0){
            List<String> identifierList = new ArrayList<>();
            for(int i = 0; i < dataSetList.size(); i++){
                identifierList.add(dataSetList.get(i).getIdentifier());
            }
            queryBuilderForIdentifier = QueryBuilders.termsQuery("identifier",identifierList);
            if(!("".equals(request.getCondition())) && (null != request.getCondition())){
                boolQueryBuilder.must(QueryBuilders.boolQuery().should(queryBuilder)
                        .should(nestedQueryBuilderForLabel)
                        .should(nestedQueryBuilderForType)
                        .should(nestedQueryBuilderForClose)
                        .should(nestedQueryBuilderForSameAs));
            }else{
                if(!StringUtils.isNotBlank(request.getDatasetId())){
                    boolQueryBuilder.must(QueryBuilders.existsQuery("title"));
                }
            }
            boolQueryBuilder.must(queryBuilderForIdentifier);
        }else{
            return null;
        }
//        MatchAllQueryBuilder matchAllQueryBuilder = QueryBuilders.matchAllQuery();
        org.springframework.data.elasticsearch.core.query.Query query = null;
//        org.springframework.data.elasticsearch.core.query.Query queryCount = null;
        query = new NativeSearchQueryBuilder().withQuery(boolQueryBuilder)
                .withHighlightFields(datasetName,subjectShort,title,unitName)
                .withPageable(PageRequest.of(request.getPageNum() - 1,request.getPageSize()))
                .build();
            //查询返回结果count
//            queryCount = new NativeSearchQueryBuilder().withQuery(boolQueryBuilder)
//                    .withPageable(PageRequest.of(0,10000))
//                    .build();
        //需要突破1W时限制再打开
//        queryCount.setTrackTotalHits(true);
//        long totalHits = elasticsearchOperations.search(query, ResourceEntity.class).getTotalHits();
        SearchHits<ResourceEntity> search = elasticsearchOperations.search(query, ResourceEntity.class);
        List<SearchHit<ResourceEntity>> searchHits = search.getSearchHits();
        long totalHits = search.getTotalHits();
        log.info("es查询返回结果数量：" + searchHits.size());
        List<ResourceEntity> list = new ArrayList<>();
        searchHits.forEach(sh->{
            Map<String, List<String>> highLightFields = sh.getHighlightFields();
            // 将高亮的内容填充到content中
            ResourceEntity content = sh.getContent();
            content.setDatasetName(highLightFields.get("datasetName") == null ? content.getDatasetName() : highLightFields.get("datasetName").get(0));
            //若嵌套对象有命中，处理高亮字段
            if(null != content.getType()){
                HashSet<RValue> typeSet = (HashSet<RValue>) content.getType().getValue();
                content.getType().setValue(dealNestedHight(sh,typeSet,"type"));
            }
            if(null != content.getLabel()){
                HashSet<RValue> labelSet = (HashSet<RValue>) content.getLabel().getValue();
                content.getLabel().setValue(dealNestedHight(sh,labelSet,"label"));
            }
            if(null != content.getCloseMatch()){
                HashSet<RValue> closeMatchSet = (HashSet<RValue>) content.getCloseMatch().getValue();
                content.getCloseMatch().setValue(dealNestedHight(sh,closeMatchSet,"closeMatch"));
            }
            if(null != content.getSameAs()){
                HashSet<RValue> sameAsSet = (HashSet<RValue>) content.getSameAs().getValue();
                content.getSameAs().setValue(dealNestedHight(sh,sameAsSet,"sameAs"));
            }
            content.setSubjectShort(highLightFields.get("subjectShort") == null ? content.getSubjectShort() : highLightFields.get("subjectShort").get(0));
            content.setTitle(highLightFields.get("title") == null ? content.getTitle() : highLightFields.get("title").get(0));
            content.setUnitName(highLightFields.get("unitName") == null ? content.getUnitName() : highLightFields.get("unitName").get(0));

            list.add(content);
        });
        //换一下返回类数据，之后看能否直接用ResourceList类
        List<ResourceList> resourceLists = new ArrayList<>();
        list.forEach(re -> {
            ResourceList resourceList = new ResourceList();
            resourceList.setSparql(re.getSparql());
            resourceList.setTitle(re.getTitle());
            resourceList.setUnitName(re.getUnitName());
            resourceList.setWebsite(re.getWebsite());
            resourceList.setDatasetName(re.getDatasetName());
            resourceList.setIdentifier(re.getIdentifier());
            resourceList.setSubject(re.getSubject());
            resourceList.setSubjectShort(re.getSubjectShort());

            RType rType = re.getType();
            ResourceType resourceType = new ResourceType();
            if(null != rType){
                resourceType.setTypeLink(rType.getTypeLink());
                resourceType.setTypeShort(rType.getTypeShort());
                HashSet<Map<String,String>> hashSet = new HashSet<>();
                Set<RValue> value = rType.getValue();
                for (RValue rValue: value
                     ) {
                    HashMap<String, String> hashMap = new HashMap<>();
                    hashMap.put(rValue.getKey(),rValue.getValue());
                    hashSet.add(hashMap);
                }
                resourceType.setValue(hashSet);
            }
            resourceList.setType(resourceType);

            RLabel rLabel = re.getLabel();
            ResourceLabel resourceLabel = new ResourceLabel();
            if(null != rLabel){
                resourceLabel.setLabelLink(rLabel.getLabelLink());
                resourceLabel.setLabelShort(rLabel.getLabelShort());
                HashSet<Map<String,String>> hashSet = new HashSet<>();
                Set<RValue> value = rLabel.getValue();
                for (RValue rValue: value
                ) {
                    HashMap<String, String> hashMap = new HashMap<>();
                    hashMap.put(rValue.getKey(),rValue.getValue());
                    hashSet.add(hashMap);
                }
                resourceLabel.setValue(hashSet);
            }
            resourceList.setLabel(resourceLabel);

            RCloseMatch rCloseMatch = re.getCloseMatch();
            ResourceCloseMatch resourceCloseMatch = new ResourceCloseMatch();
            if(null != rCloseMatch){
                resourceCloseMatch.setCloseMatchLink(rCloseMatch.getCloseMatchLink());
                resourceCloseMatch.setCloseMatchShort(rCloseMatch.getCloseMatchShort());
                HashSet<Map<String,String>> hashSet = new HashSet<>();
                Set<RValue> value = rCloseMatch.getValue();
                for (RValue rValue: value
                ) {
                    HashMap<String, String> hashMap = new HashMap<>();
                    hashMap.put(rValue.getKey(),rValue.getValue());
                    hashSet.add(hashMap);
                }
                resourceCloseMatch.setValue(hashSet);
            }
            resourceList.setCloseMatch(resourceCloseMatch);

            RSameAs rSameAs = re.getSameAs();
            ResourceSameAs resourceSameAs = new ResourceSameAs();
            if(null != rSameAs){
                resourceSameAs.setSameAsLink(rSameAs.getSameAsLink());
                resourceSameAs.setSameAsShort(rSameAs.getSameAsShort());
                HashSet<Map<String,String>> hashSet = new HashSet<>();
                Set<RValue> value = rSameAs.getValue();
                for (RValue rValue: value
                ) {
                    HashMap<String, String> hashMap = new HashMap<>();
                    hashMap.put(rValue.getKey(),rValue.getValue());
                    hashSet.add(hashMap);
                }
                resourceSameAs.setValue(hashSet);
            }
            resourceList.setSameAs(resourceSameAs);

            resourceLists.add(resourceList);
        });

        //分页
        PageTools<List<ResourceList>> page = null;
        page = new PageTools<>(request.getPageSize(), (int)totalHits, request.getPageNum(), resourceLists);
        //写到controller
        return page;
    }

    /**
     * 处理嵌套对象高亮
     */
    public HashSet<RValue> dealNestedHight(SearchHit<ResourceEntity> sh,HashSet<RValue> set,String flag){
        if(sh.getInnerHits().size() == 0){
            return set;
        }
        SearchHits<?> innerHits = sh.getInnerHits(flag + ".value");
        List<? extends SearchHit<?>> searchHits1 = innerHits.getSearchHits();
        if(searchHits1.size() == 0){
            return set;
        }
        HashSet<RValue> rValues = new HashSet<>();
        ArrayList<RValue> arrayListTemp = new ArrayList<>(set);
        for (SearchHit<?> a: searchHits1
        ) {
            //获取位置
            int offset = a.getNestedMetaData().getOffset();
            RValue rValue = arrayListTemp.get(offset);
            //将该位置设置高亮
            rValue.setValue(a.getHighlightField(flag + ".value.value").get(0));
        }
        rValues.addAll(arrayListTemp);
        return rValues;
    }
}

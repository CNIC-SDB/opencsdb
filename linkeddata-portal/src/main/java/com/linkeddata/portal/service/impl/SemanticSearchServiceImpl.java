package com.linkeddata.portal.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.linkeddata.portal.entity.*;
import com.linkeddata.portal.entity.mongo.EntityClass;
import com.linkeddata.portal.entity.semanticSearch.*;
import com.linkeddata.portal.repository.EntityClassDao;
import com.linkeddata.portal.service.FindPathService;
import com.linkeddata.portal.service.SemanticSearchService;
import com.linkeddata.portal.utils.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 语义检索
 *
 * @author 陈锟
 * @date 2023年3月7日15:54:42
 */
@Service
public class SemanticSearchServiceImpl implements SemanticSearchService {

    /**
     * 问句类型：
     * 0：不满足任一问句类型；
     * 1：问句类型1； X有何属性？
     * 2：问句类型2 X和Y有无关联？
     * 3：问句类型3 对X有影响的Y都有哪些？
     * 4：问句类型4 x的分布地有哪些特点
     */
    private final String QUESTION_TYPE1 = "1";
    private final String QUESTION_TYPE2 = "2";
    private final String QUESTION_TYPE3 = "3";
    private final String QUESTION_TYPE4 = "4";
    @Resource
    private FindPathService findPathService;
    @Resource
    private SPARQLBuilder sparqlBuilder;
    @Resource
    private EntityClassDao entityClassDao;

    @Resource
    private QuestionOneUtil questionOneUtil;


    /**
     * 用于测试
     *
     * @param args
     */
    public static void main(String[] args) {
        // 测试
        // 问句类型1：地不容有哪些属性
        // 问句类型2：地不容和COVID-19有无关联
        // 问句类型3：对COVID-19有影响的化合物都有哪些
        // 问句类型3：对新冠病毒有影响的植物都有哪些
        // 问句类型3：对Cepharanthine有影响的植物都有哪些
        // 问句类型4：不丹松的分布地有哪些特点
        // 有列表页的问句类型：问句类型3
        // 新冠涉及的所有端点
        String allEndpoints = "https://www.plantplus.cn/plantsw/sparql,http://xtipc.semweb.csdb.cn/sparql,http://chemdb.semweb.csdb.cn/sparql,http://pubmed.semweb.csdb.cn/sparql,http://clinicaltrials.semweb.csdb.cn/sparql,http://micro.semweb.csdb.cn/sparql,http://linkedgeodata.org/sparql,https://dbpedia.org/sparql";
        // queryType：graph=关系图；list=列表
        SemanticSearchResult result = new SemanticSearchServiceImpl().getSemanticSearchResult("氟碳涂料有什么特点？", allEndpoints, "graph");
    }

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
    @Override
    public SemanticSearchResult getSemanticSearchResult(String question, String endpoints, String queryType) {
        List<Date> dateList = new ArrayList<>(); // 计算时间
        dateList.add(new Date()); // 计算时间

        System.out.println("以下为语义检索测试输出"); // 陈锟本机测试用
        System.out.println("接收到问句：" + question); // 陈锟本机测试用
        /**
         * 基于模板进行问句解析，得到问句类型、问句中的实体名称
         */
        System.out.println("问句解析开始"); // 陈锟本机测试用
//        QuestionParseResult parseResult = SemanticSearchUtils.getQuestionParseResult(question);

        // 此处拼接question= 和&linkAddress= 是为了给getQuestionType 传参 2023/09/20 gaoshuai
        String query = "question=" + question;
        String[] endpointArr = endpoints.split(",");
        StringBuilder queryBuilder = new StringBuilder(query);
        for (String endpoint : endpointArr) {
            queryBuilder.append("&linkAddress=").append(endpoint);
        }
        query = queryBuilder.toString();

        QuestionParseResult parseResult = this.getQuestionType(query);
        System.out.println("问句解析成功：type=" + parseResult.getType() + "，x=" + parseResult.getX() + "，y=" + parseResult.getY()); // 陈锟本机测试用

        /**
         * 根据问句中的实体名称，分别查询各个实体的iri、type
         * 需要考虑同一个名称映射到多个实体的情况 陈锟 2023年3月23日15:49:42
         */
        System.out.println("实体解析开始"); // 陈锟本机测试用
        List<QuestionParseEntity> xList = SemanticSearchUtils.getResourceByName(parseResult.getX(), endpoints);
        List<QuestionParseEntity> yList = new ArrayList<>();
        // 只有部分问句类型才有y
        if ("2".equals(parseResult.getType())) {
            yList = SemanticSearchUtils.getResourceByName(parseResult.getY(), endpoints);
        }
        System.out.println("实体解析成功，x=" + xList + "，y=" + yList); // 陈锟本机测试用
        dateList.add(new Date()); // 计算时间 - 实体解析

        /**
         * 根据问句类型，编写并执行对应的SPARQL，封装查询结果
         */
        System.out.println("SPARQL分布式检索开始"); // 陈锟本机测试用
        SemanticSearchResult result = new SemanticSearchResult();
        // 往 result 对象中放入问句实体，以便于后面直接从 result 中获取
        result.setXName(parseResult.getX());
        result.setYName(parseResult.getY());
        result.setXList(xList);
        result.setYList(yList);
        result.setQueryType(queryType);
        result.setEndpoints(endpoints);
        if ("1".equals(parseResult.getType()) && xList != null && !xList.isEmpty()) { // 问句1：X有何属性？
//            result = SemanticSearchUtils.templateSparqlQueryForQuestion1(result);
            result = questionOneUtil.templateSparqlQueryForQuestion1(question, result);
        } else if ("2".equals(parseResult.getType()) && (xList != null && !xList.isEmpty()) && (yList != null && !yList.isEmpty())) { // 问句2：X和Y有无关联？
            result = SemanticSearchUtils.templateSparqlQueryForQuestion2(result);
        } else if ("3".equals(parseResult.getType()) && xList != null && !xList.isEmpty() && StringUtils.isNotBlank(result.getYName())) { // 问句3：对X有影响的Y都有哪些？
            result = SemanticSearchUtils.templateSparqlQueryForQuestion3(result);
            // 拥有列表页
            result.setHasList(true);
        } else if ("4".equals(parseResult.getType()) && xList != null && !xList.isEmpty()) { // 问句4：x的分布地有哪些特点
            result = SemanticSearchUtils.templateSparqlQueryForQuestion4(result);
        } else {
            return SemanticSearchUtils.generateErrorResult(); // 生成错误的result对象
        }
        System.out.println("SPARQL分布式检索成功：Nodes=" + result.getNodes().size() + "，Edges=" + result.getEdges().size()); // 陈锟本机测试用
        System.out.println("起点=" + result.getStartEntitys().size() + "，终点=" + result.getEndEntitys().size()); // 陈锟本机测试用
        dateList.add(new Date()); // 计算时间 - SPARQL分布式检索

        /**
         * 封装起点、终点节点list给前端使用，只返回iri节点，非iri节点其实不算实体
         */
        for (VisjsNode node : result.getNodes()) {
            if (result.getStartEntitys().contains(node.getId()) && node.isIriFlag()) {
                result.getStartNodes().add(node);
            } else if (result.getEndEntitys().contains(node.getId()) && node.isIriFlag()) {
                result.getEndNodes().add(node);
            }
        }

        /**
         * 如果根据问句未检索到恰当结果，生成错误的result对象
         */
        if ("graph".equals(result.getQueryType())) {
            if (result.getNodes() == null || result.getNodes().isEmpty()) {
                return SemanticSearchUtils.generateErrorResult();
            }
        }

        /**
         * 打印result对象，用于检查问题
         */
//        System.out.println(result.getNodes()); // 陈锟本机测试用
//        System.out.println(result.getEdges()); // 陈锟本机测试用

        /**
         * 根据拆分返回结果中的点和边，使得页面上可以分批动态展示
         */
        System.out.println("拆分路径开始"); // 陈锟本机测试用
        if (queryType.equals("graph")) {
            result = SemanticSearchUtils.splitPath(result);
        }
        // 路径拆分后封装语义检索答案
        // 计算展示在关系图中的边的数量
        Set<VisjsEdge> edgeSet = new HashSet<>();
        for (VisjsGroup group : result.getVisjsGroups()) {
            for (VisjsEdge edge : group.getEdges()) {
                edgeSet.add(edge);
            }
        }
        String answer = "";
        if ("1".equals(parseResult.getType())) {
            // 问句类型1
//            answer = "语义检索发现，" + result.getXName() + "的属性有" + edgeSet.size() + "个";
            answer = result.getAnswer();
        } else if ("2".equals(parseResult.getType())) {
            // 问句类型2
            answer = "语义检索发现，" + result.getXName() + "与" + result.getYName() + "间" + (result.getEdges().size() > 0 ? "" : "不") + "存在关联路径";
        } else if ("3".equals(parseResult.getType())) {
            // 问句类型3
            answer = "语义检索发现，对" + result.getXName() + "有影响的" + result.getYName() + "有" + result.getEndEntitys().size() + "个";
        } else if ("4".equals(parseResult.getType())) {
            // 问句类型4
            answer = "语义检索发现，" + result.getXName() + "的分布地的属性有" + result.getEdges().size() + "个";
        }
        result.setAnswer(answer);
        dateList.add(new Date()); // 计算时间 - 拆分路径

        /**
         * 简化返回内容，删掉一些多余的返回内容，减轻前端压力
         */
        System.out.println("简化返回内容开始"); // 陈锟本机测试用
        result.setEndpoints(null);
        result.setXList(null);
        result.setYList(null);
        result.setNodes(null);
        result.setEdges(null);
        result.setStartEntitys(null);
        result.setEndEntitys(null);
        result.setIriLabelMap(null);
        // 终点最多返回20个
        // 在名称动态获取前不返回终点
        result.setEndNodes(new ArrayList<>());
//        if (result != null && result.getEndNodes().size() > 20) {
//            result.setEndNodes(result.getEndNodes().subList(0, 20));
//        }
        // 简化visjsGroups中的所有URI，将URI变为数字，但不简化起点和终点的
        Map<String, String> tempMap = new HashMap<>();
        long tempNum = 0;
        for (VisjsGroup group : result.getVisjsGroups()) {
            // 对于点，将IRI存在一个新的字段showIri里，将ID变为数字
            for (VisjsNode node : group.getNodes()) {
                node.setShowIri(node.getId());
                if (!tempMap.containsKey(node.getId())) {
                    tempNum++;
                    String newId = String.valueOf(tempNum);
                    tempMap.put(node.getId(), newId);
                    node.setId(newId);
                } else {
                    node.setId(tempMap.get(node.getId()));
                }
            }
            // 对于边，直接将ID变为数字
            for (VisjsEdge edge : group.getEdges()) {
                // 边URI
                if (!tempMap.containsKey(edge.getUri())) {
                    tempNum++;
                    String newId = String.valueOf(tempNum);
                    tempMap.put(edge.getUri(), newId);
                    edge.setUri(newId);
                } else {
                    edge.setUri(tempMap.get(edge.getUri()));
                }
                // from
                if (!tempMap.containsKey(edge.getFrom())) {
                    tempNum++;
                    String newId = String.valueOf(tempNum);
                    tempMap.put(edge.getFrom(), newId);
                    edge.setFrom(newId);
                } else {
                    edge.setFrom(tempMap.get(edge.getFrom()));
                }
                // to
                if (!tempMap.containsKey(edge.getTo())) {
                    tempNum++;
                    String newId = String.valueOf(tempNum);
                    tempMap.put(edge.getTo(), newId);
                    edge.setTo(newId);
                } else {
                    edge.setTo(tempMap.get(edge.getTo()));
                }
            }
        }

        System.out.println("简化返回内容成功"); // 陈锟本机测试用
        dateList.add(new Date()); // 计算时间 - 简化返回内容

        if ("graph".equals(result.getQueryType())) {
            System.out.println("-------- 语义检索成功，返回关系图：路径条数：" + result.getVisjsGroups().size()); // 陈锟本机测试用
            System.out.println("语义检索答案：" + result.getAnswer()); // 陈锟本机测试用
        } else if ("list".equals(result.getQueryType())) {
            System.out.println("-------- 语义检索成功，返回列表：列表条数：" + result.getPageResultEntityList().size()); // 陈锟本机测试用
        }
        int n = 0; // 计数器
        System.out.println("实体解析用时：" + ((dateList.get(n+1).getTime() - dateList.get(n++).getTime())) + " 毫秒"); // 陈锟本机测试用
        System.out.println("SPARQL分布式检索用时：" + ((dateList.get(n+1).getTime() - dateList.get(n++).getTime())) + " 毫秒"); // 陈锟本机测试用
        System.out.println("拆分路径用时：" + ((dateList.get(n+1).getTime() - dateList.get(n++).getTime())) + " 毫秒"); // 陈锟本机测试用
        System.out.println("简化返回内容用时：" + ((dateList.get(n+1).getTime() - dateList.get(n++).getTime())) + " 毫秒"); // 陈锟本机测试用
        System.out.println("语义检索总用时：" + ((dateList.get(dateList.size() - 1).getTime() - dateList.get(0).getTime())) + " 毫秒"); // 陈锟本机测试用
        return result;
    }

    /**
     * 判断端点是否可被访问
     *
     * @param endpoint
     * @return
     */
    @Override
    public Boolean isAccessible(String endpoint) {
        String queryStr = "SELECT ?s ?p ?o  WHERE { ?s ?p ?o.} limit 1";
        ResultSet resultSet = null;
        try {
            resultSet = RdfUtils.queryTriple(endpoint, queryStr);
            if (resultSet == null || !resultSet.hasNext()) {
                return Boolean.FALSE;
            } else {
                return Boolean.TRUE;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Boolean.FALSE;
        }
    }


    /**
     * 获取问句的类型
     * @param question
     * @return
     */
    @Override
    public QuestionParseResult getQuestionType_(String question) {
        //xiajl20230718 增加问题类型5和问题类型6
        //增加“问句类型5”：根据问号拆分，拆分后的子句有任意一个满足问句类型1-4。例如：地不容根茎叶有什么特点，地不容和COVID-19有无关联?
        //增加“问句类型6”：【问句类型3？任何？】。例如：Caltha驴蹄草属都包含哪些物种？它的根、茎、叶都有什么特征？
        //先判读问句中是否有分号和问号
        //xiajl20230727 默认设置成问题类型0;
        QuestionParseResult parseResult = new QuestionParseResult();
        parseResult.setType("0");

        if (question.contains("?") || question.contains("？")|| question.contains(";") || question.contains("；") )
        {
            question = question.replace("？","?").replace(";","?").replace("；","?");
            String[] questionArray = question.split("\\?");
            if (questionArray.length > 1){
                //优先判读是问句类型6
                QuestionParseResult tempResult = SemanticSearchUtils.getQuestionParseResult(questionArray[0]);
                if (!Objects.isNull(tempResult)){
                    if (tempResult.getType().equals("3")){
                        tempResult.setType("6");
                        tempResult.setNewQuestion(questionArray[0]+"?");
                        tempResult.setRemainingQuestion(questionArray[1]+"?");
                        return tempResult;
                    }
                    else{
                        for (String str : questionArray){
                            QuestionParseResult temp = SemanticSearchUtils.getQuestionParseResult(str);
                            if (!Objects.isNull(temp)){
                                if (!temp.getType().equals("0")){
                                    temp.setType("5");
                                    temp.setNewQuestion(str+"?");
                                    return temp;
                                }
                            }
                        }
                    }
                }
            }
            else {
                parseResult = SemanticSearchUtils.getQuestionParseResult(question);
            }
        }else
        {
            parseResult = SemanticSearchUtils.getQuestionParseResult(question);
        }
        return parseResult;
    }

    @Override
    public QuestionParseResult getQuestionType(String question) {
        String result = "";
        try {
            result = HttpUtil.doGet("http://10.0.82.212:5000/getQuestionType?" + question);
//            result = HttpUtil.doGet("http://127.0.0.1:5001/getQuestionType?" + question);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        final QuestionParseResult questionParseResult = JSONObject.parseObject(result, QuestionParseResult.class);
        return questionParseResult;

    }

    /**
     * 如果x和y是同一类，返回sparql查询语句
     *
     * @param x         起点
     * @param y         终点
     * @param type      问句类型
     * @param endpoints 所选端点
     * @return
     */
    @Override
    public List<SparqlBuilderEntity> listSparqlInSameClass(String x, String y, String type, String endpoints) {
        // 存放返回前端的所有sparql语句
        List<SparqlBuilderEntity> sparqlList = new ArrayList<>();

        // 库里没有新冠病毒、新冠这几个字，转换为covid-19
        if ("新冠病毒".equals(x) || "新冠".equals(x)) {
            x = "COVID-19";
        }
        if ("新冠病毒".equals(y) || "新冠".equals(y)) {
            y = "COVID-19";
        }
        List<Map<String, String>> xEntityTypeList = new ArrayList<>();
        List<Map<String, String>> yEntityTypeList = new ArrayList<>();
        // 问句类型2 X和Y有无关联？
        if (QUESTION_TYPE2.equals(type)) {
            xEntityTypeList = findPathService.getEntityType(x, endpoints);
            yEntityTypeList = findPathService.getEntityType(y, endpoints);
        } else if (QUESTION_TYPE3.equals(type)) {
            // 问句类型3 对X有影响的Y都有哪些？
            xEntityTypeList = findPathService.getClassType(x, endpoints);
            if (xEntityTypeList.isEmpty()) {
                xEntityTypeList = findPathService.getEntityType(x, endpoints);
            }
            yEntityTypeList = findPathService.getClassType(y, endpoints);
        }
        if (!xEntityTypeList.isEmpty() && !yEntityTypeList.isEmpty()) {
            for (int i = 0; i < xEntityTypeList.size(); i++) {
                Map<String, String> xMap = xEntityTypeList.get(i);
                String xType = xMap.get("type") + "";
                String xEntityIri = xMap.get("entityIri") + "";
                String xendpoint = xMap.get("endpoint") + "";
                for (int j = 0; j < yEntityTypeList.size(); j++) {
                    Map<String, String> yMap = yEntityTypeList.get(j);
                    String yType = yMap.get("type") + "";
                    String yEntityIri = yMap.get("entityIri") + "";
                    String yendpoint = yMap.get("endpoint") + "";
                    // x和y的两个list中，只要有1个类相同就执行同类查询
//                    if (xType.equals(yType) && xendpoint.equals(yendpoint)) {
                    if (xType.equals(yType)) {
                        SparqlBuilderEntity sparqlBuilderEntity = new SparqlBuilderEntity();
                        List list = sparqlBuilder.buildQueries(xEntityIri, yEntityIri, xendpoint, Integer.valueOf(type));
                        sparqlBuilderEntity.setSparqlList(list);
                        sparqlBuilderEntity.setStart(xEntityIri);
                        sparqlBuilderEntity.setEnd(yEntityIri);
                        sparqlBuilderEntity.setEndpoint(xendpoint);
                        sparqlBuilderEntity.setQuestionType(type);
                        sparqlList.add(sparqlBuilderEntity);
                    }
                }
            }
        }
        return sparqlList;
    }

    /**
     * x和y有无关联，x,y属于同一类生成sparql语句查询
     *
     * @param sparqlList 多个端点中的sparql查询语句,每个端点内的查询语句又是多个
     * @param startName  起点名称
     * @param endName    终点名称
     * @return
     */
    @Override
    public List<PathQueryResult> findInSameClass(List<SparqlBuilderEntity> sparqlList, String startName, String endName) {
        List<PathQueryResult> list = new ArrayList<>();
        for (SparqlBuilderEntity sparqlEntity : sparqlList) {
            // 封装路径对象中的起点和终点信息，需要作为参数传递。
            // 每组路径的起点和终点可能不一样，因此放入循环里
            PathInfo pathInfo = new PathInfo();
            pathInfo.setStartIri(sparqlEntity.getStart());
            pathInfo.setEndIri(sparqlEntity.getEnd());
            pathInfo.setStartName(startName);
            pathInfo.setEndName(endName);

            List<String> sparqls = sparqlEntity.getSparqlList();
            for (String sparql : sparqls) {
                PathQueryResult result = SemanticSearchUtils.queryGraphBySparql(sparqlEntity.getQuestionType(), sparql, pathInfo);
                if (!result.getNodes().isEmpty()) {
                    Set<String> startClassNameList = this.listClassTypeByEntityUri(sparqlEntity.getStart(), sparqlEntity.getEndpoint());
                    result.setClassList(startClassNameList);
                    // 封装路径筛选时所需要的起点和终点。对于问句类型2，起点和终点与PathInfo一致
                    result.setStartIri(sparqlEntity.getStart());
                    if (QUESTION_TYPE2.equals(sparqlEntity.getQuestionType())) {
                        result.setEndIri(new HashSet<>(Arrays.asList(sparqlEntity.getEnd())));
                    } else {
                        result.setEndIri(result.getEnds());
                    }
                    list.add(result);
                }
            }
        }
        return list;
    }

    /**
     * 查询同一类查询语句中每个点所属的类名称
     *
     * @param entityUri
     * @param endpoint
     * @return
     */
    private Set<String> listClassTypeByEntityUri(String entityUri, String endpoint) {
        // 去重存放返回结果
        Set<String> classNameSet = new HashSet<>();
        String sparql = "construct { <" + entityUri + "> a ?type   } where { service silent<" + endpoint + "> { <" + entityUri + "> a ?type .} } limit 10  ";
        try {
            Model model = RdfUtils.sparqlConstruct(sparql);
            NodeIterator nodeIterator = null;
            if (model != null) {
                nodeIterator = model.listObjectsOfProperty(RDF.type);
                while (nodeIterator.hasNext()) {
                    RDFNode next = nodeIterator.next();
                    EntityClass byUri = entityClassDao.findByUri(next.toString());
                    String label = byUri.getLabel();
                    if (label != null && !"".equals(label)) {
                        classNameSet.add(label);
                    }
                }
            }

        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }
        return classNameSet;
    }

    //xiajl20230725 根据问句获取来源信息
    //xiajl20230818 更新为从python接口中获取metadata信息，根据主语，谓语从RDF获取原始文本
    //xiajl20230821 question参数为 拼接后带sparql端点的整个参数语句:
    //question=野棉花的药用部位和主要成分是什么?&Linked Plant Data=https://www.plantplus.cn/plantsw/sparql&Zoology Data=http://ioz.semweb.csdb.cn/sparql&Organchem Data=http://organchem.semweb.csdb.cn/sparql&XTBG Data=http://xtbg.semweb.csdb.cn/sparql
    @Override
    public List<SearchResultEntity> getSearchResultEntity(String question){
        // todo 20230725  应该调用大模型接口的方法
        //List<String> list = LlmUtil.getSearchResultEntity(question);
        JSONArray jsonArray = LlmUtil.getMedadataEntity(question);
        List<SearchResultEntity> resultList = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++){
            SearchResultEntity searchResultEntity = new SearchResultEntity();
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String s = jsonObject.getString("sUri");
            String p = jsonObject.getString("pUri");
            String endpoint = jsonObject.getString("linkAddress");
            String sparql="select ?x where { <" + s + "> <" + p + ">  ?x .  } LIMIT 1 " ;
            ResultSet resultSet = RdfUtils.queryTriple(endpoint,sparql);
            List<Map<String, Object>> resultMapList = RdfUtils.resultEncapsulation(resultSet);
            String doc = "";
            if (resultMapList.size() > 0){
                String sLabel = jsonObject.getString("sLabel");
                String pLabel = jsonObject.getString("pLabel");
                doc = sLabel + "的" + pLabel +"是: " + String.valueOf(resultMapList.get(0).get("x"));
                System.out.println(doc);
            }
            searchResultEntity.setMessage(doc);
            searchResultEntity.setNodeName(jsonObject.getString("nodeName"));
            searchResultEntity.setLinkAddress(jsonObject.getString("linkAddress"));
            resultList.add(searchResultEntity);
        }
        return resultList;
    }

    @Override
    public List<TableDataEntity> getTableDataEntity(String question){
        List<TableDataEntity> list = getTableData(question);
        return list;
    }

    //根据问句1--4获取表格数据
    public List<TableDataEntity> getTableData(String question){
        List<TableDataEntity> list = new ArrayList<>();
        //todo 获取数据的接口，现在先写几个测试数据
        TableDataEntity tableDataEntity = new TableDataEntity();
        tableDataEntity.setProperty("Chemical 9417");
        tableDataEntity.setPropertyLink("http://www.baidu.com");
        tableDataEntity.setSubject("CHEBI 24431");
        tableDataEntity.setSubjectLink("");
        tableDataEntity.setProvenance("DBpedia");
        tableDataEntity.setProvenanceLink("www.dbpedia.com");
        list.add(tableDataEntity);

        TableDataEntity tableDataEntity1 = new TableDataEntity();
        tableDataEntity1.setProperty("http://rdfns/common.topic.topic_equivalent_webpade");
        tableDataEntity1.setPropertyLink("http://rdfns/common.topic.topic_equivalent_webpade");
        tableDataEntity1.setSubject("http://pt.wikipedia.org/wiki/John_Rhvs-Davies");
        tableDataEntity1.setSubjectLink("http://pt.wikipedia.org/wiki/John_Rhvs-Davies");
        tableDataEntity1.setProvenance("Zoology Data");
        tableDataEntity1.setProvenanceLink("http://xtbg.semweb.csdb.cn/sparql");
        list.add(tableDataEntity1);

        TableDataEntity tableDataEntity2 = new TableDataEntity();
        tableDataEntity2.setProperty("http://dbpediaora/property/imagesize");
        tableDataEntity2.setPropertyLink("http://dbpediaora/property/imagesize");
        tableDataEntity2.setSubject("220");
        tableDataEntity2.setSubjectLink("");
        tableDataEntity2.setProvenance("DBpedia");
        tableDataEntity2.setProvenanceLink("www.dbpedia.com");
        list.add(tableDataEntity2);

        return list;
    }

    @Override
    public String findImage(String uri) {
        String query = """
                select DISTINCT ?url where {
                  ?photo <http://rs.tdwg.org/dwc/iri/toTaxon> <$uri> .
                  ?photo <http://schema.org/url> ?url .
                } limit 1000
                """.replace("$uri", uri);
        String result;
        final ResultSet resultSet = RdfUtils.queryTriple("http://xtbg.semweb.csdb.cn/sparql", query);
        if (null == resultSet) {
            return null;
        }
        while (resultSet.hasNext()) {
            final QuerySolution next = resultSet.next();
            final RDFNode rdfNode = next.get("?url");
            final String resource = rdfNode.asLiteral().getString();
            result = Strings.trimToNull(resource);
            if (null != result) {
                return result;
            }
        }
        return null;
    }

    /**
     * 让大模型总结问句类型2的结果
     *
     * @param request
     * @return
     * @author chenkun
     * @since 2023年9月28日16:29:29
     */
    @Override
    public String queryResultForQuestion2(QueryResultForQuestion2Request request) {
        // 将三元组转化为文本，体现出实体、关系、机构、类等关键信息，用于拼接在大模型问句中
        String triplesStr = this.triplesToText(request.getTriples());

        /*
        大模型问句类型2模板：

            请根据我下面提供的背景信息、关联关系、要求，来回答我的问题。

            我的问题是：“千金藤属”和“COVID-19”之间存在哪些关联关系？请详细回答。

            我已经成功获取了两者之间的所有关联关系，您需要在了解我提供的背景信息后，仔细理解我提供的关联关系。然后，根据我的要求，生成一份总结性的文本，这份总结性的文本是针对我上述问题的最终答案。

            首先，请您先了解下面这些背景信息，了解后再去阅读后面的关联关系：
              - 双引号内的是实体的名称，请将双引号内的看成是一个整体。
              - 实体名称后面的（）表示该实体的类型。
              - 每行末尾的【】表示这条信息来自于哪个机构。
              - 不同机构中的信息可能有重复，请你仔细辨别，如果是重复的，你可以合并。

            然后，下面是我查询到的“千金藤属”和“COVID-19”之间的全部关联关系，我的关系中包含了实体名称、类型、属性、机构等信息。这些关联关系其实会形成一张关系图，请您在了解上面的背景信息后，仔细理解这些关联关系：
              - “地不容”（物种）的上一级是：“千金藤属”（属）【植物所】
              - “地不容”（药材）的提取对象是：“地不容”（植物）【新疆理化所】
              - “地不容”（药材）的化学成分是：“千金藤素”（化合物）【新疆理化所】
              - “Cepharanthine”（药物）的化学成分是：“千金藤素”（化合物）【clinicaltrials.gov】
              - “Study of Oral High/Low-dose Cepharanthine Compared With Placebo in Non Hospitalized Adults With COVID-19”（临床试验）的是：“Cepharanthine”（药物）【clinicaltrials.gov】
              - “Study of Oral High/Low-dose Cepharanthine Compared With Placebo in Non Hospitalized Adults With COVID-19”（临床试验）的是：“COVID-19”（病毒）【clinicaltrials.gov】
              - “台湾千金藤”（物种）的上一级是：“千金藤属”（属）【植物所】
              - “千金藤素”（药材）的提取对象是： “台湾千金藤”（物种）【上海有机所】
              - “千金藤素”（药材）的化学成分是：“千金藤素”（化合物）【上海有机所】
              - “Cepharanthine”（药物）的化学成分是：“千金藤素”（化合物）【clinicaltrials.gov】

            接着，请生成一份总结性的文本，来回答我最开始的问题，您的答案需要满足我下面的这些要求：
              - 我提供的关联关系较为丰富，您无需逐一复述，以免显得过于冗长。
              - 你的回答必须为中文。
              - 你的回答要分点罗列，并在最后做个总结。
              - 你的回答中请务必指出这些信息对应的机构。
              - 这些是两者之间的关联关系中涉及到的所有数据类型，请将它一并整理到答案中：物种、属、药材、植物、化合物、药物、病毒。
              - 两者之间的所有关联路径共2条，请将这个统计结果一并整理到您的答案中。
              - 请您直接输出我想要的答案，而不要在开头输出“根据您提供的背景信息和关联关系，我将生成一份总结性的答案：”类似的文字。
              - 你的回答中不能出现“您”、“你”、“我”等词汇。
              - 你的回答不要太过简短。

            最后，请回答我最开始的问题，详细说明“千金藤属”和“COVID-19”之间存在哪些关联关系，你的回答是：
         */
        String llmQuestion = "请根据我下面提供的背景信息、关联关系、要求，来回答我的问题。\n" +
                "\n" +
                "我的问题是：" + request.getQuestion() + "，请详细回答。\n" +
                "\n" +
                "我已经成功获取了两者之间的所有关联关系，您需要在了解我提供的背景信息后，仔细理解我提供的关联关系。然后，根据我的要求，生成一份总结性的文本，这份总结性的文本是针对我上述问题的最终答案。\n" +
                "\n" +
                "首先，请您先了解下面这些背景信息，了解后再去阅读后面的关联关系：\n" +
                "  - 双引号内的是实体的名称，请将双引号内的看成是一个整体。\n" +
                "  - 实体名称后面的（）表示该实体的类型。\n" +
                "  - 每行末尾的【】表示这条信息来自于哪个机构。\n" +
                "  - 不同机构中的信息可能有重复，请你仔细辨别，如果是重复的，你可以合并。\n" +
                "\n" +
                "然后，下面是我查询到的“" + request.getX() + "”和“" + request.getY() + "”之间的全部关联关系，我的关系中包含了实体名称、类型、属性、机构等信息。这些关联关系其实会形成一张关系图，请您在了解上面的背景信息后，仔细理解这些关联关系：\n" +
                triplesStr +
                "\n" +
                "接着，请生成一份总结性的文本，来回答我最开始的问题，您的答案需要满足我下面的这些要求：\n" +
                "  - 我提供的关联关系较为丰富，您无需逐一复述，以免显得过于冗长。\n" +
                "  - 你的回答必须为中文。\n" +
                "  - 你的回答要分点罗列，并在最后做个总结。\n" +
                "  - 你的回答中请务必指出这些信息对应的机构。\n" +
                "  - 这些是两者之间的关联关系中涉及到的所有数据类型，请将它一并整理到答案中：" + request.getClassList().toString() + "。\n" +
                "  - 两者之间的所有关联路径共" + request.getPathNum() + "条，请将这个统计结果一并整理到您的答案中。\n" +
                "  - 请您直接输出我想要的答案，而不要在开头输出“根据您提供的背景信息和关联关系，我将生成一份总结性的答案：”类似的文字。\n" +
                "  - 你的回答中不能出现“您”、“你”、“我”等词汇。\n" +
                "  - 你的回答不要太过简短。\n" +
                "\n" +
                "最后，请回答我最开始的问题，详细说明“" + request.getX() + "”和“" + request.getY() + "”之间存在哪些关联关系，你的回答是：";
        // 使用语言模型生成答案
        String answer = LlmUtil.queryBaichuan(llmQuestion);
        return answer;
    }

    /**
     * 让大模型总结问句类型3的结果
     *
     * @param request
     * @return
     * @author chenkun
     * @since 2023年9月28日16:29:29
     */
    @Override
    public String queryResultForQuestion3(QueryResultForQuestion3Request request) {
        // 将三元组转化为文本，体现出实体、关系、机构、类等关键信息，用于拼接在大模型问句中
        String triplesStr = this.triplesToText(request.getTriples());

        /*
        大模型问句类型3模板：

            请根据我下面提供的背景信息、关联关系、要求，来回答我的问题。

            我的问题是：千金藤属都包含哪些物种？请详细回答。

            我已经成功获取了两者之间的所有关联关系，您需要在了解我提供的背景信息后，仔细理解我提供的关联关系。然后，根据我的要求，生成一份总结性的文本，这份总结性的文本是针对我上述问题的最终答案。

            首先，请您先了解下面这些背景信息，了解后再去阅读后面的关联关系：
              - 双引号内的是实体的名称，请将双引号内的看成是一个整体。
              - 实体名称后面的（）表示该实体的类型。
              - 每行末尾的【】表示这条信息来自于哪个机构。
              - 不同机构中的信息可能有重复，请你仔细辨别，如果是重复的，你可以合并。

            然后，下面是我查询到的“千金藤属”和“物种”之间的全部关联关系，我的关系中包含了实体名称、类型、属性、机构等信息。这些关联关系其实会形成一张关系图，请您在了解上面的背景信息后，仔细理解这些关联关系：
              - “埃塞俄比亚千金藤”（物种）的上一级是：“千金藤属”（属）【版纳所】
              - “安达曼千金藤”（物种）的上一级是：“千金藤属”（属）【版纳所】
              - “白线薯”（物种）的上一级是：“千金藤属”（属）【版纳所】
              - “短梗地不容”（物种）的上一级是：“千金藤属”（属）【版纳所】
              - “短柄千金藤”（物种）的上一级是：“千金藤属”（属）【版纳所】
              - “头状千金藤”（物种）的上一级是：“千金藤属”（属）【版纳所】
              - “金线吊乌龟”（物种）的上一级是：“千金藤属”（属）【版纳所】
              - “景东千金藤”（物种）的上一级是：“千金藤属”（属）【版纳所】
              - “兰屿千金藤”（物种）的上一级是：“千金藤属”（属）【版纳所】
              - “小黑藤”（物种）的上一级是：“千金藤属”（属）【版纳所】

            接着，请生成一份总结性的文本，来回答我最开始的问题，您的答案需要满足我下面的这些要求：
              - 你的回答必须为中文。
              - 我提供的关联关系较为丰富，您无需逐一复述，以免显得过于冗长。
              - 你的回答需要解释清楚每条关联路径的含义。
              - 你的回答要分点罗列，并在最后做个总结。
              - 你的回答中请务必指出这些信息对应的机构。
              - 这些是两者之间的关联关系中涉及到的所有数据类型，请将它一并整理到答案中：物种、属。
              - 请将这个统计结果一并整理到您的答案中，对千金藤属有影响的物种共有60种，共包括：埃塞俄比亚千金藤、安达曼千金藤、白线薯、短梗地不容、短柄千金藤、头状千金藤、金线吊乌龟、景东千金藤、兰屿千金藤、小黑藤 等。
              - 请您直接输出我想要的答案，而不要在开头输出“根据您提供的背景信息和关联关系，我将生成一份总结性的答案：”类似的文字。
              - 你的回答中不能出现“您”、“你”、“我”等词汇。
              - 你的回答不要太过简短。

            最后，请回答我最开始的问题，详细说明“千金藤属”和“物种”之间存在哪些关联关系，你的回答是：
         */
        String llmQuestion = "请根据我下面提供的背景信息、关联关系、要求，来回答我的问题。\n" +
                "\n" +
                "我的问题是：" + request.getQuestion() + "，请详细回答。\n" +
                "\n" +
                "我已经成功获取了两者之间的所有关联关系，您需要在了解我提供的背景信息后，仔细理解我提供的关联关系。然后，根据我的要求，生成一份总结性的文本，这份总结性的文本是针对我上述问题的最终答案。\n" +
                "\n" +
                "首先，请您先了解下面这些背景信息，了解后再去阅读后面的关联关系：\n" +
                "  - 双引号内的是实体的名称，请将双引号内的看成是一个整体。\n" +
                "  - 实体名称后面的（）表示该实体的类型。\n" +
                "  - 每行末尾的【】表示这条信息来自于哪个机构。\n" +
                "  - 不同机构中的信息可能有重复，请你仔细辨别，如果是重复的，你可以合并。\n" +
                "\n" +
                "然后，下面是我查询到的“" + request.getX() + "”和“" + request.getY() + "”之间的全部关联关系，我的关系中包含了实体名称、类型、属性、机构等信息。这些关联关系其实会形成一张关系图，请您在了解上面的背景信息后，仔细理解这些关联关系：\n" +
                triplesStr +
                "\n" +
                "接着，请生成一份总结性的文本，来回答我最开始的问题，您的答案需要满足我下面的这些要求：\n" +
                "  - 你的回答必须为中文。\n" +
                "  - 我提供的关联关系较为丰富，您无需逐一复述，以免显得过于冗长。\n" +
                "  - 你的回答需要解释清楚每条关联路径的含义。\n" +
                "  - 你的回答要分点罗列，并在最后做个总结。\n" +
                "  - 你的回答中请务必指出这些信息对应的机构。\n" +
                "  - 这些是两者之间的关联关系中涉及到的所有数据类型，请将它一并整理到答案中：" + request.getClassList().toString() + "。\n" +
                "  - 请将这个统计结果一并整理到您的答案中，对" + request.getX() + "有影响的" + request.getY() + "共有" + request.getAnswerEntityNum() + "种，共包括：" + request.getAnswerEntityList().toString() + " 等。\n" +
                "  - 请您直接输出我想要的答案，而不要在开头输出“根据您提供的背景信息和关联关系，我将生成一份总结性的答案：”类似的文字。\n" +
                "  - 你的回答中不能出现“您”、“你”、“我”等词汇。\n" +
                "  - 你的回答不要太过简短。\n" +
                "\n" +
                "最后，请回答我最开始的问题，详细说明“" + request.getX() + "”和“" + request.getY() + "”之间存在哪些关联关系，你的回答是：";
        // 使用语言模型生成答案
        String answer = LlmUtil.queryBaichuan(llmQuestion);
        return answer;
    }

    /**
     * 将三元组转化为文本，体现出实体、关系、机构、类等关键信息，用于拼接在大模型问句中；
     * <br> 例如：
     * <br> &nbsp;&nbsp; - “地不容”（物种）的上一级是：“千金藤属”（属）【植物所】
     * <br> &nbsp;&nbsp; - “地不容”（药材）的提取对象是：“地不容”（植物）【新疆理化所】
     *
     * @param triples List<MyTriple>
     * @return String 三元组文本
     * @author chenkun
     * @since 2023年10月6日12:40:37
     */
    public String triplesToText(List<MyTriple> triples) {
        StringBuilder triplesStr = new StringBuilder();
        for (MyTriple triple : triples) {
            // 主语类型label
            String sTypeLabel = SemanticSearchUtils.getTypeLabel(triple.getSType());
            if (StringUtils.isNotBlank(sTypeLabel)) sTypeLabel = "（" + sTypeLabel + "）";
            // 宾语类型label
            String oTypeLabel = "";
            if (StringUtils.isNotBlank(triple.getOType())) {
                oTypeLabel = SemanticSearchUtils.getTypeLabel(triple.getOType());
                if (StringUtils.isNotBlank(oTypeLabel)) oTypeLabel = "（" + oTypeLabel + "）";
            }
            // 将三元组拼接成文本
            String str = "  - “" + triple.getS() + "”" + sTypeLabel + "的" + triple.getP() + "是：“" + triple.getO() + "”" + oTypeLabel + "【" + triple.getApplicationName() + "】\n";
            triplesStr.append(str);
        }
        return triplesStr.toString();
    }

}

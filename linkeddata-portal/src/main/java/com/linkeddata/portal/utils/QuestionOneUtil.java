package com.linkeddata.portal.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.linkeddata.portal.entity.mongo.EntityClass;
import com.linkeddata.portal.entity.mongo.EntityProperty;
import com.linkeddata.portal.entity.semanticSearch.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 对于问句类型1
 * 查询不同sparql端点中的实体信息，将rdf转换为文本，作为prompt提交给大模型，获取大模型的回答。
 *
 * @author gaoshuai
 */
@Component
public class QuestionOneUtil {
    private static final String ENDURI = "http://end";

    @javax.annotation.Resource
    private MongoTemplate mongoTemplate;

    private String vectorContext;

    public SemanticSearchResult templateSparqlQueryForQuestion1(String question, SemanticSearchResult semanticSearchResult) {
        List<QuestionParseEntity> xList = semanticSearchResult.getXList();
        String endpoints = semanticSearchResult.getEndpoints();
        // SPARQL查询1级属性
        String[] endpointsArr = endpoints.split(",");
        StringBuilder sparql = new StringBuilder();
        sparql.append(" CONSTRUCT { ?s ?p ?o } WHERE {");
        for (int i = 0; i < endpointsArr.length; i++) {
            String sparqlPoint = endpointsArr[i];
            sparql.append("{ SERVICE SILENT <")
                    .append(sparqlPoint)
                    .append("> {  { ?s ?p ?o . ")
                    .append(" FILTER ( LANG(?o) = 'zh' && ?s IN   " + SemanticSearchUtils.getInStringByEntities(xList) + ")")
                    .append("  } ")
                    .append("union { ?s ?p ?o . FILTER ( ?s IN " + SemanticSearchUtils.getInStringByEntities(xList) + ") }")
                    .append("}}");
            if (i != endpointsArr.length - 1) {
                sparql.append(" union ");
            }
        }
        sparql.append(" }"); // 限制条数为300个，太少了展示不出想要的数据，太多了展示不好看 update by chenkun 2023年4月21日17:11:15

        Model model = ModelFactory.createDefaultModel();
        // 调用SPARQL联邦查询引擎
        Model execModel = RdfUtils.sparqlConstructWithEndpoints(sparql.toString(), semanticSearchResult.getEndpoints());
        if (execModel != null && execModel.size() > 0) {
            model.add(execModel);
        }
        String prompt = entityRdfToText(model, question, semanticSearchResult.getXName());
        // 20230927，查询金藤有哪些特征，没找到金藤相关的三元组，但是llm自己回答了，这种在数据中心没有的实体，不再回答
        if (prompt.isEmpty()) {
            semanticSearchResult.setAnswer("语义检索发现，未检索到合适的内容");
            return semanticSearchResult;
        }
        semanticSearchResult.setInfo(prompt);
//        String answer = LlmUtil.queryResultForLlmContact(prompt);
        String answer = LlmUtil.queryBaichuan(prompt);
        semanticSearchResult.setAnswer(answer);
        // 此次答案的依据
        JSONArray vector = this.queryEmbeddingContext(vectorContext, answer);
        semanticSearchResult.setVectorContext(vector);
        semanticSearchResult = this.searchNodesAndEdges(model, semanticSearchResult);
        // 查询起点、终点。对于问句类型1，有1个起点，0个终点
        // 起点固定为x
        semanticSearchResult.setStartEntitys(SemanticSearchUtils.getIrisByEntities(xList));
        // 封装propertyList，用于前端展示属性。add by 陈锟 2023年5月12日14:41:16
        semanticSearchResult.setPropertyList(SemanticSearchUtils.getPropertyList(semanticSearchResult));
        return semanticSearchResult;
    }

    /**
     * 封装SPARQL查询通用结果
     *
     * @param model
     * @param result
     * @return result
     * @author gaoshuai
     * @date 2023年3月9日13:04:42
     */
    public SemanticSearchResult searchNodesAndEdges(Model model, SemanticSearchResult result) {
        // 取出终点并移除临时标记终点的三元组
        List<String> endEntityList = new ArrayList<>();
        StmtIterator endStmtIterator = model.listStatements(null, new PropertyImpl(ENDURI), new ResourceImpl(ENDURI));
        List<Statement> statementList = new ArrayList<>();
        while (endStmtIterator.hasNext()) {
            Statement statement = endStmtIterator.nextStatement();
            endEntityList.add(statement.getSubject().getURI());
            statementList.add(statement);
        }
        result.setEndEntitys(endEntityList);
        // 移除临时标记终点的三元组
        model.remove(statementList);

        // 获取所有主语
        ResIterator resIterator = model.listSubjects();

        Set<VisjsNode> nodes = new HashSet<>();
        Set<VisjsEdge> edges = new HashSet<>();
        Set<String> iriSet = new HashSet<>();
        // 循环所有主语
        while (resIterator.hasNext()) {
            Resource subject = resIterator.nextResource();
            // 将主语加入到nodes中
            nodes.add(new VisjsNode(subject.getURI(), SemanticSearchUtils.getAppNameByResUri(subject.getURI())));
            // 将主语添加到iriSet中
            iriSet.add(subject.getURI());
            // 循环该主语的所有三元组
            StmtIterator stmtIterator = subject.listProperties();
            while (stmtIterator.hasNext()) {
                Statement statement = stmtIterator.nextStatement();
                // 将谓语添加到iriSet中
                iriSet.add(statement.getPredicate().getURI());
                VisjsNode objectNode;
                if (statement.getObject().isResource()) { // 宾语为资源实体
                    objectNode = new VisjsNode(statement.getObject().asResource().getURI(), SemanticSearchUtils.getAppNameByResUri(statement.getObject().asResource().getURI()));
                    // 将宾语添加到iriSet中
                    iriSet.add(statement.getObject().asResource().getURI());
                } else { // 宾语不为资源实体，为字面量
                    // 将宾语加入到nodes中。若宾语为字面量，则所属机构与主语相同。参照VisjsNode.id字段上的说明
                    String objectNodeId = statement.getObject().asLiteral().getValue().toString();
                    objectNode = new VisjsNode(objectNodeId, statement.getObject().asLiteral().getValue().toString(), SemanticSearchUtils.getAppNameByResUri(subject.getURI()), false);
                }
                // 将宾语加入到nodes中
                nodes.add(objectNode);
                // 将关系添加到edges中
                edges.add(new VisjsEdge(subject.getURI(), objectNode.getId(), statement.getPredicate().getURI()));
            }
        }

        // 批量查询iri的label，根据iriSet封装iriLabelMap
        Map<String, String> iriLabelMap = new HashMap<>();
        iriSet.forEach(iri -> {
            String label = SemanticSearchUtils.getLabelByIri(iri);
            iriLabelMap.put(iri, label);
        });
        // 将iriLabelMap添加到nodes、edges中
        nodes.forEach(node -> {
            String label = iriLabelMap.get(node.getId());
            if (StringUtils.isNotBlank(label)) {
                node.setLabel(label);
            }
        });
        edges.forEach(edge -> {
            String label = iriLabelMap.get(edge.getUri());
            if (StringUtils.isNotBlank(label)) {
                edge.setLabel(label);
            }
        });

        result.setNodes(nodes);
        result.setEdges(edges);
        result.setIriLabelMap(iriLabelMap);
        result.setNodesNum(nodes.size());
        return result;
    }

    /**
     * 截取资源前的域名
     *
     * @param resource
     * @return
     * @author gaoshuai
     */
    private String getResourceUrl(String resource) {
        int index_sub = resource.indexOf("//");
        int i_sub = resource.indexOf("/", index_sub + 2);
        String substring_sub = resource.substring(0, i_sub);
        return substring_sub;
    }

    /**
     * 将根据实体查询出的相关三元组转换为文本。
     *
     * @return
     * @author gaoshuai
     */
    public String entityRdfToText(Model model, String question, String entity) {
        List<String> dataCenterEndPointList = new ArrayList<>();
        dataCenterEndPointList.add("http://pubmed.semweb.csdb.cn/sparql");
        dataCenterEndPointList.add("https://www.plantplus.cn/plantsw/sparql");
        dataCenterEndPointList.add("http://micro.semweb.csdb.cn/sparql");
        dataCenterEndPointList.add("http://chemdb.semweb.csdb.cn/sparql");
        dataCenterEndPointList.add("http://clinicaltrials.semweb.csdb.cn/sparql");
        dataCenterEndPointList.add("http://tpdc.semweb.csdb.cn/sparql");
        dataCenterEndPointList.add("http://ncmi.semweb.csdb.cn/sparql");
        dataCenterEndPointList.add("http://xtipc.semweb.csdb.cn/sparql");
        dataCenterEndPointList.add("http://ioz.semweb.csdb.cn/sparql");
        dataCenterEndPointList.add("http://organchem.semweb.csdb.cn/sparql");
        dataCenterEndPointList.add("http://xtbg.semweb.csdb.cn/sparql");
        dataCenterEndPointList.add("https://semweb.scbg.ac.cn/sparql");

        dataCenterEndPointList.add("https://dbpedia.org/sparql");
        dataCenterEndPointList.add("http://linkedgeodata.org/sparql");
        dataCenterEndPointList.add("https://query.wikidata.org/sparql");
        dataCenterEndPointList.add("https://sparql.wikipathways.org/sparql");
        dataCenterEndPointList.add("https://bio2rdf.org/sparql");
        dataCenterEndPointList.add("https://sparql.europeana.eu");
        dataCenterEndPointList.add("http://ods.openlinksw.com/sparql");
        dataCenterEndPointList.add("https://sparql.uniprot.org/sparql");
        dataCenterEndPointList.add("http://uriburner.com/sparql");
        dataCenterEndPointList.add("http://lov.okfn.org/dataset/lov/sparql");

        String prefix = "你是一位严谨的科学研究人员，请你根据下面这些参考资料回答我提出的问题，你需要根据提出的问题挑选应该采用哪些参考资料，不一定要用到我所有的参考资料。我给你的参考资料是：\n";
        String templateHead = "在{1}的数据中，有以下关于{2}的信息：\n";
        StringBuilder allPrompt = new StringBuilder();
        allPrompt.append(prefix);

        // 该实体是否能查询出三元组，默认false不能
        boolean isHasContext = false;
        // 用于存储每条属性，交给向量模型作为此次查询的向量，作为问题的依据在前台显示
        Set<Document> vectorSet = new HashSet<>();
        ResIterator resIterator = model.listSubjects();
        while (resIterator.hasNext()) {
            // 用于拼接rdf转换为文本的背景信息
            StringBuilder tempSPO = new StringBuilder();

            Resource subjectResource = resIterator.next();
            String subjectUri = subjectResource.getURI();

            // 获取三元组所属端点url
            String endpointUrl = "";
            String uri_sub = subjectResource.getURI();
            String substring_sub = this.getResourceUrl(uri_sub);
            for (String endpoint : dataCenterEndPointList) {
                if (endpoint.contains(substring_sub)) {
                    endpointUrl = endpoint;
                    break;
                }
            }

            // 获取主语对应的数据中心
            String appName = SemanticSearchUtils.getAppNameByResUri(subjectUri);

            StmtIterator stmtIterator = model.listStatements(subjectResource, null, (RDFNode) null);
            while (stmtIterator.hasNext()) {
                Statement st = stmtIterator.next();

                Property predicate = st.getPredicate();
                String prdicateLabel = this.getPredicateLabel(predicate);

                RDFNode object = st.getObject();
                String objectLabel = "";

                if (object.isLiteral()) {
                    objectLabel = object.asLiteral().getString();
                    // 将宾语是英文的，且长度超过100的舍去，因为 XTBG Data中地不容的形态特征（英文）是：英文很长，llm查询速度慢，去掉后
                    // 速度明显提升，chatglm是中文模型，对英文处理也不是很好。2023/09/20
                    String language = object.asLiteral().getLanguage();
                    if ("en".equals(language) && objectLabel.length() > 100) {
                        objectLabel = "";
                    }

                } else if (predicate == RDF.type) {
                    Resource objectResource = object.asResource();
                    objectLabel = this.getObjectLabelFromMongo(objectResource);
                } else if (object.isResource()) {
                    Resource objectResource = object.asResource();
                    String uri = objectResource.getURI();
                    String substring = this.getResourceUrl(uri);
                    for (String endpoint : dataCenterEndPointList) {
                        if (endpoint.contains(substring)) {
                            objectLabel = this.getObjectLabel(objectResource);
                            break;
                        }
                    }
                }
                // 2023/10/09纯英文的字符串不要，在前端页面展示的时候易读
                if (!objectLabel.isEmpty() && (!entity.equals(objectLabel)) && !prdicateLabel.isEmpty() && !CommonUtils.isEnglishAndNumber(objectLabel)) {
                    tempSPO.append("\t" + entity + "的" + prdicateLabel + "是:" + objectLabel + ".\n");
                    if (!"别名".equals(prdicateLabel) && predicate != RDF.type) {
                        Document document = new Document();
                        document.setPage_content(prdicateLabel + ":" + objectLabel);
                        document.setEndPointUrl(endpointUrl);
                        document.setDataCenterName(appName);
                        vectorSet.add(document);
                    }
                }
            }
            tempSPO.append("\n");
            if (!tempSPO.toString().trim().isEmpty()) {
                allPrompt.append(templateHead.replace("{1}", appName).replace("{2}", entity));
                allPrompt.append(tempSPO);
                isHasContext = true;
            }
        }
        vectorContext = vectorRDF(vectorSet);
        if (!isHasContext) {
            return "";
        }
        String suffix = "\n 我的问题是：{3} 。\n 请只结合我的数据来回答，不要用你自己的知识来扩展。你的回答需要使用中文、语义完整、分条列举每个重点、最后进行总结，你的回答是：\n";
        suffix = suffix.replace("{3}", question);
        allPrompt.append(suffix);
        return allPrompt.toString();
    }

    /**
     * 用python程序，将实体的每个属性作为一条向量，在llm查询的时候作为依据在前端展示
     *
     * @param vectorSet
     * @return
     * @author gaoshuai
     */
    public String vectorRDF(Set<Document> vectorSet) {
        if (vectorSet.isEmpty()) {
            return "";
        }
        String jsonString = JSON.toJSONString(vectorSet);
        return jsonString;
    }

    /**
     * 将构造好的向量文本交给python代码，查询向量库，获得
     * 查询的相关文本，然后在前台显示
     *
     * @param vectorContext
     * @return
     */
    public JSONArray queryEmbeddingContext(String vectorContext, String question) {
        String url = "http://10.0.82.212:5000/getContext";
        String paramName1 = "context";
        String paramName2 = "question";
        String result = HttpUtil.doPost2(url, paramName1, String.valueOf(vectorContext), paramName2, question);
        JSONArray jsonArray = JSON.parseArray(result);
        return jsonArray;
    }

    /**
     * 获取主语uri的label值
     *
     * @param resource 资源
     * @return
     * @author gaoshuai
     */
    public String getObjectLabel(Resource resource) {
        String label = "";
        String label_first = "";
        String label_zh = "";
        try {
            Model model = ModelFactory.createDefaultModel();
            model.read(resource.getURI());
            StmtIterator stmtIterator = model.listStatements(resource, RDFS.label, (RDFNode) null);
            while (stmtIterator.hasNext()) {
                Statement next = stmtIterator.next();
                RDFNode object = next.getObject();
                if (object.isResource()) {
                    getObjectLabel(object.asResource());
                } else {
                    String language = object.asLiteral().getLanguage();
                    // 先记录下第一个lable值，无论什么语种
                    if (label_first.isEmpty()) {
                        label_first = object.asLiteral().getString();
                    }
                    // 遍历的时候，有中文的值就记录下来，只记录1个
                    if ("zh".equals(language) && label_zh.isEmpty()) {
                        label_zh = object.asLiteral().getString();
                    }
                }
            }
            label = label_zh.isEmpty() ? label_first : label_zh;
        } catch (Exception e) {
//            label = resource.getURI();
//            e.printStackTrace();
        }
        return label;
    }

    /**
     * 获取谓语的label
     *
     * @param resource
     * @return
     * @author gaoshuai
     */
    public String getPredicateLabel(Resource resource) {
        Criteria criteria = Criteria.where("uri").is(resource.getURI());
        Query query = new Query();
        query.addCriteria(criteria);
        EntityProperty entityProperty = mongoTemplate.findOne(query, EntityProperty.class);
        return entityProperty != null ? entityProperty.getLabel() : "";
    }

    /**
     * 当谓语是rdf:type的时候
     * 从mongodb中获取宾语label
     *
     * @param resource
     * @return
     * @author gaoshuai
     */
    public String getObjectLabelFromMongo(Resource resource) {
        Criteria criteria = Criteria.where("uri").is(resource.getURI());
        Query query = new Query();
        query.addCriteria(criteria);
        EntityClass entityClass = mongoTemplate.findOne(query, EntityClass.class);
        return entityClass != null ? entityClass.getLabel() : "";
    }

}

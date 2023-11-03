package com.linkeddata.portal.utils;


import com.linkeddata.portal.entity.es.RValue;
import com.linkeddata.portal.entity.es.ResourceEntity;
import com.linkeddata.portal.entity.mongo.Applications;
import com.linkeddata.portal.entity.mongo.EntityClass;
import com.linkeddata.portal.entity.mongo.EntityProperty;
import com.linkeddata.portal.entity.mongo.Prefix;
import com.linkeddata.portal.entity.semanticSearch.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 语义检索 - 工具类
 *
 * @author 陈锟
 * @date 2023年3月9日13:04:42
 */
@Component
public class SemanticSearchUtils {

    // spring boot 中在静态类中调用bean
    @Autowired
    private MongoTemplate tempMongoTemplate;
    private static MongoTemplate mongoTemplate;
    @Autowired
    private ElasticsearchOperations tempElasticsearchOperations;
    private static ElasticsearchOperations elasticsearchOperations;
    // @PostConstruct 注解用于执行在bean实例化后需要立即执行的初始化工作
    @PostConstruct
    public void init() {
        mongoTemplate = this.tempMongoTemplate;
        elasticsearchOperations = this.tempElasticsearchOperations;
    }

    /**
     * 要连接的服务器ip
     */
    private static final String IP = "10.0.85.83";
    /**
     * 要连接的服务器端口
     */
    private static final Integer PORT = 22;
    /**
     * 要连接的服务器账号
     */
    private static final String USERNAME = "root";
    /**
     * 要连接的服务器密码
     */
    private static final String PASSWORD = "bigdata@~!2030";

    /**
     * 语义检索时标记终点的URI，用于临时处理
     * @author 陈锟
     * @date 2023年4月6日11:02:57
     */
    private static final String ENDURI = "http://end";

    /**
     * 如果根据问句未检索到恰当结果，生成错误的result对象；
     * 通用方法
     *
     * @return SemanticSearchResult
     * @author 陈锟
     * @date 2023年3月15日12:21:33
     */
    public static SemanticSearchResult generateErrorResult() {
        SemanticSearchResult result = new SemanticSearchResult();
        result.setAnswer("语义检索发现，未检索到合适的内容");
        return result;
    }

    /**
     * 校验已选端点中是否包含模板所需要的端点；
     * 通用方法
     *
     * @param needEndpointList 模板所需要的端点
     * @param endpoints 已选端点
     * @return boolean
     * @author 陈锟
     * @date 2023年4月3日15:15:59
     */
    public static boolean checkContainsEndpoints(List<String> needEndpointList, String endpoints) {
        List<String> endpointList = Arrays.asList(endpoints.split(","));
        return endpointList.containsAll(needEndpointList);
    }

    /**
     * 根据URI查询实体名称或谓语名称
     * TODO 陈锟 后期需要变为动态查询，而不是写死；暂时只截取IRI后缀，做二期时需要分批查询label值，没有label则使用IRI后缀
     *
     * @param iri
     * @return String
     * @author 陈锟
     * @date 2023年3月16日15:48:58
     */
    public static String getLabelByIri(String iri) {
        if (iriLabelMap.containsKey(iri)) {
            return iriLabelMap.get(iri);
        } else {
            return RdfUtils.getIriSuffix(iri);
        }
    }

    /**
     * 给传来的参数加上<>,变为IRI形式
     *
     * @param uri
     * @return
     */
    public static String asIri(String uri) {
        return "<" + uri + ">";
    }

    /**
     * 根据iriList查询列表页返回实体list
     *
     * @param semanticSearchResult
     * @return List<PageResultEntity>
     * @author 高帅
     * @date 2023年3月28日14:51:02
     */
    public static SemanticSearchResult getPageResultEntityByIris(SemanticSearchResult semanticSearchResult) {
        // 返回的实体
        SemanticSearchResult semanResult = new SemanticSearchResult();
        // iriList
        List<String> iriList = semanticSearchResult.getEndEntitys();
        String endpoints = semanticSearchResult.getEndpoints();
        // 所有主语的谓语列表
        List<BasicResultEntity> predicateList = new ArrayList<>();
        List<PageResultEntity> entityList = new ArrayList<>();
        if (iriList != null && !iriList.isEmpty()) {
            // 存放所有主语的谓语，用于去重
            Set<String> predicateSet = new HashSet<>();
            StringBuilder iriBuilder = new StringBuilder();
            for (String iri : iriList) {
                String s = asIri(iri);
                iriBuilder.append(s).append(" ");
            }
            String[] splitEndpoints = endpoints.split(",");
            StringBuilder sparql = new StringBuilder();
            sparql.append(" CONSTRUCT { ?s ?p ?o .}  where {");
            for (int i = 0; i < splitEndpoints.length; i++) {
                String sparqlPoint = splitEndpoints[i];
                sparql.append("{ SERVICE SILENT <")
                        .append(sparqlPoint)
                        .append("> {  ?s ?p ?o .  VALUES ?s {  ")
                        .append(iriBuilder)
                        .append(" } } }");
                if (i != splitEndpoints.length - 1) {
                    sparql.append(" union ");
                }
            }
            sparql.append(" } limit 300 ");
            Model model = RdfUtils.sparqlConstructWithEndpoints(sparql.toString(), endpoints);
            if (null == model) {
                return null;
            }
            // 获取model的所有主语，然后对每个主语进行遍历
            ResIterator resIterator = model.listSubjects();

            while (resIterator.hasNext()) {
                PageResultEntity resultEntity = new PageResultEntity();
                // 取出这个主语
                Resource subject = resIterator.next();
                // 1.1 封装该主语的iri
                resultEntity.setIri(subject.getURI());
                // 1.2 封装该主语的label，因为label是字面量，model.listStatements(subject, null, (RDFNode) null);取不到
                NodeIterator subjectLabelIterator = model.listObjectsOfProperty(subject, RDFS.label);
                List<String> subjectLabelList = new ArrayList<>();
                if (subjectLabelIterator.hasNext()) {
                    while (subjectLabelIterator.hasNext()) {
                        RDFNode object = subjectLabelIterator.next();
                        String label = object.toString().replace("\\\"", "");
                        subjectLabelList.add(label);
                    }
                } else {
                    // 如果该IRI没有label，则从iri中截取后半部分作为label
                    String label = RdfUtils.getIriSuffix(subject.getURI());
                    subjectLabelList.add(label);
                }
                resultEntity.setLabel(subjectLabelList);

                // 直接获取该主语的所有陈述，遍历陈述的过程中判断谓语是不是type，是type的放type属性，不是type的放relationList属性
                StmtIterator subjectIterator = model.listStatements(subject, null, (RDFNode) null);
                List<BasicResultEntity> typeEntityList = new ArrayList<>();
                List<BasicResultEntity> relationEntityList = new ArrayList<>();
                Integer listSize = 0;
                while (subjectIterator.hasNext()) {
                    BasicResultEntity typeEntity = new BasicResultEntity();
                    BasicResultEntity relationEntity = new BasicResultEntity();
                    Statement thisSubjectStatement = subjectIterator.next();
                    Property predicate = thisSubjectStatement.getPredicate();
                    // 存每个主语对应对谓语，table页选择展示的谓语用 2023/4/3
                    // 重复的谓语不记录
                    if (!predicateSet.contains(predicate.getURI())) {
                        BasicResultEntity predicateEntity = new BasicResultEntity();
                        predicateEntity.setIri(predicate.getURI());
                        String predicateLabel = RdfUtils.getIriSuffix(predicate.asResource().getURI());
                        predicateEntity.setLabel(predicateLabel);
                        predicateList.add(predicateEntity);
                    }
                    predicateSet.add(predicate.getURI());
                    // 如果该主语的谓语是type则放到resultEntity的type属性
                    if (RDF.type.equals(predicate)) {
                        RDFNode object = thisSubjectStatement.getObject();
                        typeEntity.setIri(object.asResource().getURI());
                        String objectLabel = RdfUtils.getIriSuffix(object.asResource().getURI());
                        typeEntity.setLabel(objectLabel);
                        typeEntityList.add(typeEntity);
                    } else {
                        // 如果该主语的谓语不是type则放到resultEntity的type属性
                        RDFNode object = thisSubjectStatement.getObject();
                        String objectStr = object.toString();
                        if (object.isResource() && objectStr.startsWith("http")) {
                            if (listSize > 4) {
                                continue;
                            }
                            relationEntity.setIri(object.asResource().getURI());
                            String objectLabel = RdfUtils.getIriSuffix(object.asResource().getURI());
                            relationEntity.setLabel(objectLabel);
                            relationEntityList.add(relationEntity);
                            listSize++;
                        }
                    }
                }
                resultEntity.setType(typeEntityList);
                resultEntity.setRelationList(relationEntityList);
                entityList.add(resultEntity);
            }
        }
        semanResult.setPageResultEntityList(entityList);
        semanResult.setPrecateList(predicateList);
        return semanResult;
    }

    /**
     * IRI与名称的键值对。键：IRI；值：实体名称或谓语名称
     * @author 陈锟
     * @date 2023年3月16日15:48:58
     */
    private static Map<String, String> iriLabelMap = new HashMap<>();
    /**
     * 机构名称map，用于获取资源所属的机构名称
     * @author 陈锟
     * @date 2023年3月16日15:48:58
     */
    private static Map<String, String> applicationNameMap = new HashMap<>();
    static {
        /**
         * IRI与名称的键值对
         */
        // 先只放关键实体，非关键实体不放
        iriLabelMap.put("https://www.plantplus.cn/plantsw/resource/Taxon_Stephania_epigaea", "地不容");
        iriLabelMap.put("http://xtipc.semweb.csdb.cn/resource/Herb_Stephania_epigaea_Lo", "地不容");
        iriLabelMap.put("https://www.plantplus.cn/plantsw/resource/Taxon_Alhagi_sparsifolia", "骆驼刺");
        iriLabelMap.put("http://xtipc.semweb.csdb.cn/resource/Herb_Alhagi_sparsifolia", "骆驼刺");
        iriLabelMap.put("http://chemdb.semweb.csdb.cn/resource/Compound_481-49-2", "千金藤素");
        iriLabelMap.put("http://chemdb.semweb.csdb.cn/resource/Compound_520-26-3", "橙皮苷");
        iriLabelMap.put("http://id.nlm.nih.gov/mesh/Mesh_D000086382", "Mesh[COVID-19]");
        iriLabelMap.put("http://id.nlm.nih.gov/mesh/Mesh_D000086402", "Mesh[SARS-CoV-2]");
        iriLabelMap.put("http://micro.semweb.csdb.cn/resource/Mesh_D000086382", "COVID-19");
        iriLabelMap.put("http://micro.semweb.csdb.cn/resource/Mesh_D000086402", "SARS-CoV-2");
        // 先只放关键谓语，非关键谓语不放
        // 药用植物
        iriLabelMap.put("http://semanticscience.org/resource/SIO_000313", "is component part of");
        iriLabelMap.put("http://purl.obolibrary.org/obo/CIDO_0000022", "has active ingredient");
        // pubmed
        iriLabelMap.put("http://purl.obolibrary.org/obo/OMIT_0000110", "MeSH_term");
        iriLabelMap.put("http://purl.obolibrary.org/obo/OMIT_0001004", "Chemical");
        // 美国临床试验
        iriLabelMap.put("http://purl.obolibrary.org/obo/NCIT_C93360", "Study Condition");
        iriLabelMap.put("http://purl.obolibrary.org/obo/OAE_0000002", "medical intervention");

        /**
         * 机构名称map，前后顺序跟前端页面一致
         */
        // OpenCSDB数据源
        applicationNameMap.put("https://www.plantplus.cn/plantsw/", "Linked Plant Data");
        applicationNameMap.put("http://micro.semweb.csdb.cn/", "Linked Micro Data");
        applicationNameMap.put("http://chemdb.semweb.csdb.cn/", "Linked Chemdb Data");
        applicationNameMap.put("http://clinicaltrials.semweb.csdb.cn/", "Clinical Trials Data");
        applicationNameMap.put("http://pubmed.semweb.csdb.cn/", "Pubmed Biomed Data");
        applicationNameMap.put("http://tpdc.semweb.csdb.cn/", "National TP Data");
        applicationNameMap.put("http://ncmi.semweb.csdb.cn/", "National Health Data");
        applicationNameMap.put("http://xtipc.semweb.csdb.cn/", "Medicinal Plant Data");
        applicationNameMap.put("http://organchem.semweb.csdb.cn/", "Organchem Data");
        applicationNameMap.put("http://ioz.semweb.csdb.cn/", "Zoology Data"); // 动物所
        applicationNameMap.put("http://xtbg.semweb.csdb.cn/", "XTBG Data"); // 版纳所
        applicationNameMap.put("https://semweb.scbg.ac.cn/", "SCBG Data"); // 华南植物园
        // 外部数据源
        applicationNameMap.put("dbpedia.org", "DBpedia");
        applicationNameMap.put("linkedgeodata.org", "LinkedGeoData");
        applicationNameMap.put("geonames.org", "LinkedGeoData");
        applicationNameMap.put("wikidata.org", "Wikidata");
        applicationNameMap.put("sparql.wikipathways.org", "WikiPathways");
        applicationNameMap.put("bio2rdf.org", "Bio2RDF");
        applicationNameMap.put("lov.okfn.org", "Linked Open Vocabularies");
        applicationNameMap.put("sparql.europeana.eu", "Europeana");
        applicationNameMap.put("ods.openlinksw.com", "OpenLink");
        applicationNameMap.put("sparql.uniprot.org", "UniProt");
        applicationNameMap.put("uriburner.com", "URIBurner");
        applicationNameMap.put("yago-knowledge.org", "YAGO");
        applicationNameMap.put("integbio.jp", "Integbio");
        // 其他
        applicationNameMap.put("wikipedia.org", "Wikipedia");
        applicationNameMap.put("id.nlm.nih.gov/mesh", "Mesh");
    }

    /**
     * 获取公共端点的SPARQL查询地址
     * TODO 陈锟 等分布式功能开发完成后移除此方法
     *
     * @param endpoints
     * @return String
     * @author 陈锟
     * @date 2023年3月14日19:26:26
     */
    public static String getPublicSparqlEndpoint(String endpoints) {
        String endpoint = "http://semweb.csdb.cn/sparql?";
        if (StringUtils.isNotBlank(endpoints)) {
            String[] endpointArr = endpoints.replaceAll(" ", "").split(","); // 去除请求中的空格，并以英文分号拆分
            for (String str : endpointArr) {
                if ("https://www.plantplus.cn/plantsw/sparql".equals(str)) {
                    endpoint += "&default-graph-uri=cvh";
                    endpoint += "&default-graph-uri=ppbc";
                    endpoint += "&default-graph-uri=sp2000";
                } else if ("http://micro.semweb.csdb.cn/sparql".equals(str)) {
                    endpoint += "&default-graph-uri=VirusSpecies";
                    endpoint += "&default-graph-uri=VirusCategory";
                    endpoint += "&default-graph-uri=VirusStrain";
                    endpoint += "&default-graph-uri=Nucleotide";
                    endpoint += "&default-graph-uri=Gene";
                    endpoint += "&default-graph-uri=Protein";
                    endpoint += "&default-graph-uri=Structure";
                    endpoint += "&default-graph-uri=Antibody";
                    endpoint += "&default-graph-uri=Literature";
                    endpoint += "&default-graph-uri=Patent";
                    endpoint += "&default-graph-uri=Theme";
                    endpoint += "&default-graph-uri=Mesh";
                    endpoint += "&default-graph-uri=Country";
                } else if ("http://chemdb.semweb.csdb.cn/sparql".equals(str)) {
                    endpoint += "&default-graph-uri=basicInfo";
                    endpoint += "&default-graph-uri=thermochemistry";
                    endpoint += "&default-graph-uri=phaseTransition";
                    endpoint += "&default-graph-uri=estimatedProperties";
                } else if ("http://clinicaltrials.semweb.csdb.cn/sparql".equals(str)) {
                    endpoint += "&default-graph-uri=Drug";
                    endpoint += "&default-graph-uri=Trials";
                } else if ("http://ncmi.semweb.csdb.cn/sparql".equals(str)) {
                    endpoint += "&default-graph-uri=covid19";
                } else if ("http://xtipc.semweb.csdb.cn/sparql".equals(str)) {
                    endpoint += "&default-graph-uri=MedicalRes";
                } else if ("http://pubmed.semweb.csdb.cn/sparql".equals(str)) {
                    endpoint += "&default-graph-uri=Biomed";
                } else if ("http://tpdc.semweb.csdb.cn/sparql".equals(str)) {
                    endpoint += "&default-graph-uri=forestObserv";
                }
            }
        }
        return endpoint;
    }

    /**
     * 问句解析
     * 根据用户输入的问句返回问句类型、实体名称
     *
     * @param question 指定的问句
     * @return 返回map类型结果
     * @author 韩孟航
     * @date 2023年3月7日18:08:05
     */
    public static QuestionParseResult getQuestionParseResult(String question) {
        if ((null == question) || ("".equals(question))) {
            return null;
        }
        //忽略末尾问号
        int length = question.length() - 1;
        int index1 = question.lastIndexOf("？");
        int index2 = question.lastIndexOf("?");
        if (length == index1 || length == index2) {
            question = question.substring(0, question.length() - 1);
        }
        String reg1[] = {"(.*)有何属性$", "(.*)有哪些特征$", "(.*)有什么属性$", "(.*)有什么特点$",
                "(.*)有哪些属性$", "(.*)具有什么属性$", "(.*)有何特征$", "^介绍下(.*)有何特点$"};
        String reg2[] = {"(.*)和(.*)有无关联$", "(.*)和(.*)之间有什么联系$", "(.*)和(.*)有何关联$",
                "(.*)和(.*)有何相关$", "(.*)和(.*)之间有何相互作用$", "(.*)和(.*)有何关系$",
                "(.*)对(.*)有没有影响$", "(.*)会对(.*)产生什么样的影响$", "(.*)有何作用于(.*)",
                "(.*)会对(.*)有何影响$", "(.*)会如何影响(.*)"};
        //xiajl20230718
        //原来的“问句类型3”中，增加一种情况【xxx都包含哪些xxx】、【xxx包含哪些xxx】。例如：Caltha驴蹄草属都包含哪些物种？
        String reg3[] = {"对(.*)有影响的(.*)都有哪些$", "对(.*)有作用的(.*)都有哪些$", "(.*)能对哪些(.*)产生作用$",
                "(.*)可以对哪些(.*)产生影响$", "(.*)会影响哪些(.*)", "(.*)会对哪些(.*)有作用$", "(.*)对哪些(.*)有潜在作用$","(.*)都包含哪些(.*)","(.*)包含哪些(.*)","(.*)都有哪些(.*)","(.*)都包含什么(.*)","(.*)包含什么(.*)"};
        String reg4[] = {"(.*)的分布地有哪些特点$", "(.*)的分布地的特点有哪些$", "(.*)的分布地的特征有哪些$"};
        QuestionParseResult result = getQuestionParseResultDeal(question, reg1, "1");
        if (null == result) {
            result = getQuestionParseResultDeal(question, reg2, "2");
            if (null == result) {
                result = getQuestionParseResultDeal(question, reg3, "3");
                if (null == result) {
                    result = getQuestionParseResultDeal(question, reg4, "4");
                    if (null == result) {
                        result = new QuestionParseResult();
                        result.setType("0");
                    }
                }
            }
        }
        return result;
    }

    /**
     * 根据问句中的实体名称查询实体IRI
     *
     * @param entityName 实体名称
     * @param endpoints  字符串形式多个端点，以英文逗号分隔。
     * @return QuestionParseEntity
     * @author 陈锟
     * @date 2023年3月9日13:04:42
     */
    public static List<QuestionParseEntity> getResourceByName(String entityName, String endpoints) {
        if (StringUtils.isBlank(entityName)) {
            return null;
        }

        // 新冠病毒固定解析为COVID-19
        if ("新冠".equals(entityName) || "新冠病毒".equals(entityName)) {
            QuestionParseEntity questionParseEntity = new QuestionParseEntity(entityName, "http://micro.semweb.csdb.cn/resource/Mesh_D000086382");
            List<QuestionParseEntity> questionParseEntityList = new ArrayList<>();
            questionParseEntityList.add(questionParseEntity);
            return questionParseEntityList;
        }

        StringBuilder sparql = new StringBuilder();
        String[] endpointsArr = endpoints.split(",");
        sparql.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ");
        sparql.append(" CONSTRUCT { ?s rdfs:label ?label . } WHERE { ");
        for (int i = 0; i < endpointsArr.length; i++) {
            String sparqlPoint = endpointsArr[i];
            sparql.append("{ SERVICE SILENT <")
                    .append(sparqlPoint)
                    .append("> {  ?s rdfs:label ?label .   VALUES ?label { ")
                    .append("'" + entityName + "' '" + entityName + "'@zh '" + entityName + "'@en '" + entityName + "'@la '" + entityName + "'@ja }")
                    .append("  } }");
            if (i != endpointsArr.length - 1) {
                sparql.append(" union ");
            }
        }
        sparql.append(" } limit 100 ");
        Model model = RdfUtils.sparqlConstruct(sparql.toString());
        StmtIterator stmtIterator = model.listStatements();
        // 返回查询到的所有实体
        List<QuestionParseEntity> questionParseEntityList = new ArrayList<>();
        while (stmtIterator.hasNext()) {
            Statement statement = stmtIterator.nextStatement();
            QuestionParseEntity questionParseEntity = new QuestionParseEntity(entityName, statement.getSubject().getURI());
            questionParseEntityList.add(questionParseEntity);
        }
        return questionParseEntityList;
    }

    /**
     * 根据entityList封装IN条件（带圆括号）
     *
     * @param entityList
     * @return String 例如：(<uri1>, <uri2>, <uri3>)
     * @author 陈锟
     * @date 2023年3月23日16:07:57
     */
    public static String getInStringByEntities(List<QuestionParseEntity> entityList) {
        StringBuilder inString = new StringBuilder();
        inString.append("(");
        for (QuestionParseEntity entity : entityList) {
            inString.append("<").append(entity.getIri()).append(">");
            if (entityList.indexOf(entity) < entityList.size() - 1) {
                inString.append(", ");
            }
        }
        inString.append(")");
        return inString.toString();
    }

    /**
     * 根据entities封装iris
     *
     * @param entities
     * @return List<String> iris
     * @author 陈锟
     * @date 2023年3月23日16:26:49
     */
    public static List<String> getIrisByEntities(List<QuestionParseEntity> entities) {
        List<String> iris = new ArrayList<>();
        entities.forEach(entity -> {
            iris.add(entity.getIri());
        });
        return iris;
    }

    /**
     * 根据实体uri查询对应机构
     *
     * @param resourceUri
     * @return String
     * @author 陈锟
     * @date 2023年3月14日15:11:18
     */
    public static String getAppNameByResUri(String resourceUri) {
        // 未匹配到则为其他
        String applicationName = "Other";

        if (!StringUtils.isBlank(resourceUri)) {
            for (String prefix : applicationNameMap.keySet()) {
                String name = applicationNameMap.get(prefix);
                if (resourceUri.contains(prefix)) {
                    applicationName = name;
                    break;
                }
            }
        }
        return applicationName;
    }

    /**
     * 根据模板执行SPARQL查询，问句类型1
     * @param semanticSearchResult
     * @return SemanticSearchResult
     * @author 陈锟
     * @since 2023年5月12日13:49:19
     */
    public static SemanticSearchResult templateSparqlQueryForQuestion1(SemanticSearchResult semanticSearchResult) {
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
        sparql.append(" } limit 300 "); // 限制条数为300个，太少了展示不出想要的数据，太多了展示不好看 update by chenkun 2023年4月21日17:11:15
        semanticSearchResult = SemanticSearchUtils.searchNodesAndEdges(new String[]{sparql.toString()}, semanticSearchResult);
        // 查询起点、终点。对于问句类型1，有1个起点，0个终点
        // 起点固定为x
        semanticSearchResult.setStartEntitys(SemanticSearchUtils.getIrisByEntities(xList));
        // 封装propertyList，用于前端展示属性。add by 陈锟 2023年5月12日14:41:16
        semanticSearchResult.setPropertyList(getPropertyList(semanticSearchResult));
        return semanticSearchResult;
    }

    /**
     * 根据模板执行SPARQL查询，问句类型2，地不容和COVID-19有无关联
     * @param semanticSearchResult
     * @return SemanticSearchResult
     */
    public static SemanticSearchResult templateSparqlQueryForQuestion2(SemanticSearchResult semanticSearchResult) {
        List<QuestionParseEntity> xList = semanticSearchResult.getXList();
        List<QuestionParseEntity> yList = semanticSearchResult.getYList();
        // SPARQL查询地不容和COVID-19有无关联
        /*
            SPARQL原语句：

            # pubmed 论文
            CONSTRUCT {
              ?herb <http://semanticscience.org/resource/SIO_000313> ?plant .
              ?herb <http://purl.obolibrary.org/obo/CIDO_0000022> ?chem .
              ?chem <http://www.w3.org/2002/07/owl#sameAs> ?chemMesh .
              ?pubmed <http://purl.obolibrary.org/obo/OMIT_0000110> ?microMesh .
              ?pubmed <http://purl.obolibrary.org/obo/OMIT_0001004> ?chemMesh .
              ?micro <http://www.w3.org/2002/07/owl#sameAs> ?microMesh .
            }
            WHERE {
              SERVICE SILENT <http://xtipc.semweb.csdb.cn/sparql> {
                ?herb <http://semanticscience.org/resource/SIO_000313> ?plant .
                ?herb <http://purl.obolibrary.org/obo/CIDO_0000022> ?chem .
                FILTER (?plant IN (<https://www.plantplus.cn/plantsw/resource/Taxon_Stephania_epigaea>, <http://xtipc.semweb.csdb.cn/resource/Herb_Stephania_epigaea_Lo>))
              }
              SERVICE SILENT <http://chemdb.semweb.csdb.cn/sparql> {
                ?chem <http://www.w3.org/2002/07/owl#sameAs> ?chemMesh .
              }
              SERVICE SILENT <http://pubmed.semweb.csdb.cn/sparql> {
                ?pubmed <http://purl.obolibrary.org/obo/OMIT_0000110> ?microMesh .
                ?pubmed <http://purl.obolibrary.org/obo/OMIT_0001004> ?chemMesh .
              }
              SERVICE SILENT <http://micro.semweb.csdb.cn/sparql> {
                ?micro <http://www.w3.org/2002/07/owl#sameAs> ?microMesh .
                FILTER (?micro IN (<http://micro.semweb.csdb.cn/resource/Mesh_D000086382>))
              }
            }
            LIMIT 100

            # 美国临床试验
            CONSTRUCT {
              ?herb <http://semanticscience.org/resource/SIO_000313> ?plant .
              ?herb <http://purl.obolibrary.org/obo/CIDO_0000022> ?chem .
              ?chem <http://www.w3.org/2002/07/owl#sameAs> ?pubchem .
              ?trial <http://purl.obolibrary.org/obo/NCIT_C93360> ?micro .
              ?trial <http://purl.obolibrary.org/obo/OAE_0000002> ?drug .
              ?drug <http://purl.obolibrary.org/obo/CIDO_0000022> ?pubchem .
            }
            WHERE {
              SERVICE SILENT <http://xtipc.semweb.csdb.cn/sparql> {
                ?herb <http://semanticscience.org/resource/SIO_000313> ?plant .
                ?herb <http://purl.obolibrary.org/obo/CIDO_0000022> ?chem .
                FILTER (?plant IN (<https://www.plantplus.cn/plantsw/resource/Taxon_Stephania_epigaea>, <http://xtipc.semweb.csdb.cn/resource/Herb_Stephania_epigaea_Lo>))
              }
              SERVICE SILENT <http://chemdb.semweb.csdb.cn/sparql> {
                ?chem <http://www.w3.org/2002/07/owl#sameAs> ?pubchem .
              }
              SERVICE SILENT <http://clinicaltrials.semweb.csdb.cn/sparql> {
                ?trial <http://purl.obolibrary.org/obo/NCIT_C93360> ?micro .
                ?trial <http://purl.obolibrary.org/obo/OAE_0000002> ?drug .
                ?drug <http://purl.obolibrary.org/obo/CIDO_0000022> ?pubchem .
                FILTER (?micro IN (<http://micro.semweb.csdb.cn/resource/Mesh_D000086382>))
              }
            }
            LIMIT 100
         */
        String queryPubmed = "CONSTRUCT {\n" +
                "  ?herb <http://semanticscience.org/resource/SIO_000313> ?plant .\n" +
                "  ?herb <http://purl.obolibrary.org/obo/CIDO_0000022> ?chem .\n" +
                "  ?chem <http://www.w3.org/2002/07/owl#sameAs> ?chemMesh .\n" +
                "  ?pubmed <http://purl.obolibrary.org/obo/OMIT_0000110> ?microMesh .\n" +
                "  ?pubmed <http://purl.obolibrary.org/obo/OMIT_0001004> ?chemMesh .\n" +
                "  ?micro <http://www.w3.org/2002/07/owl#sameAs> ?microMesh .\n" +
                "}\n" +
                "WHERE {\n" +
                "  SERVICE SILENT <http://xtipc.semweb.csdb.cn/sparql> {\n" +
                "    ?herb <http://semanticscience.org/resource/SIO_000313> ?plant .\n" +
                "    ?herb <http://purl.obolibrary.org/obo/CIDO_0000022> ?chem .\n" +
                "    FILTER (?plant IN " + SemanticSearchUtils.getInStringByEntities(xList) + ")\n" +
                "  }\n" +
                "  SERVICE SILENT <http://chemdb.semweb.csdb.cn/sparql> {\n" +
                "    ?chem <http://www.w3.org/2002/07/owl#sameAs> ?chemMesh .\n" +
                "  }\n" +
                "  SERVICE SILENT <http://pubmed.semweb.csdb.cn/sparql> {\n" +
                "    ?pubmed <http://purl.obolibrary.org/obo/OMIT_0000110> ?microMesh .\n" +
                "    ?pubmed <http://purl.obolibrary.org/obo/OMIT_0001004> ?chemMesh .\n" +
                "  }\n" +
                "  SERVICE SILENT <http://micro.semweb.csdb.cn/sparql> {\n" +
                "    ?micro <http://www.w3.org/2002/07/owl#sameAs> ?microMesh .\n" +
                "    FILTER (?micro IN " + SemanticSearchUtils.getInStringByEntities(yList) + ")\n" +
                "  }\n" +
                "}\n" +
                "LIMIT 100";
        String queryTrial = "CONSTRUCT {\n" +
                "  ?herb <http://semanticscience.org/resource/SIO_000313> ?plant .\n" +
                "  ?herb <http://purl.obolibrary.org/obo/CIDO_0000022> ?chem .\n" +
                "  ?chem <http://www.w3.org/2002/07/owl#sameAs> ?pubchem .\n" +
                "  ?trial <http://purl.obolibrary.org/obo/NCIT_C93360> ?micro .\n" +
                "  ?trial <http://purl.obolibrary.org/obo/OAE_0000002> ?drug .\n" +
                "  ?drug <http://purl.obolibrary.org/obo/CIDO_0000022> ?pubchem .\n" +
                "}\n" +
                "WHERE {\n" +
                "  SERVICE SILENT <http://xtipc.semweb.csdb.cn/sparql> {\n" +
                "    ?herb <http://semanticscience.org/resource/SIO_000313> ?plant .\n" +
                "    ?herb <http://purl.obolibrary.org/obo/CIDO_0000022> ?chem .\n" +
                "    FILTER (?plant IN " + SemanticSearchUtils.getInStringByEntities(xList) + ")\n" +
                "  }\n" +
                "  SERVICE SILENT <http://chemdb.semweb.csdb.cn/sparql> {\n" +
                "    ?chem <http://www.w3.org/2002/07/owl#sameAs> ?pubchem .\n" +
                "  }\n" +
                "  SERVICE SILENT <http://clinicaltrials.semweb.csdb.cn/sparql> {\n" +
                "    ?trial <http://purl.obolibrary.org/obo/NCIT_C93360> ?micro .\n" +
                "    ?trial <http://purl.obolibrary.org/obo/OAE_0000002> ?drug .\n" +
                "    ?drug <http://purl.obolibrary.org/obo/CIDO_0000022> ?pubchem .\n" +
                "    FILTER (?micro IN " + SemanticSearchUtils.getInStringByEntities(yList) + ")\n" +
                "  }\n" +
                "}\n" +
                "LIMIT 100";

        // 校验已选端点中是否包含模板所需要的端点
        List<String> sparqlList = new ArrayList<>();
        // pubmed 论文
        if (checkContainsEndpoints(Arrays.asList("https://www.plantplus.cn/plantsw/sparql", "http://xtipc.semweb.csdb.cn/sparql", "http://chemdb.semweb.csdb.cn/sparql", "http://pubmed.semweb.csdb.cn/sparql", "http://micro.semweb.csdb.cn/sparql"), semanticSearchResult.getEndpoints())) {
            sparqlList.add(queryPubmed);
        }
        // 美国临床试验
        if (checkContainsEndpoints(Arrays.asList("https://www.plantplus.cn/plantsw/sparql", "http://xtipc.semweb.csdb.cn/sparql", "http://chemdb.semweb.csdb.cn/sparql", "http://clinicaltrials.semweb.csdb.cn/sparql", "http://micro.semweb.csdb.cn/sparql"), semanticSearchResult.getEndpoints())) {
            sparqlList.add(queryTrial);
        }
        // 不满足任何模板所需要的端点
        if (sparqlList.isEmpty()) {
            return semanticSearchResult;
        }
        semanticSearchResult = SemanticSearchUtils.searchNodesAndEdges(sparqlList.toArray(new String[sparqlList.size()]), semanticSearchResult);

        // 查询起点、终点。对于问句类型2，有1个起点，1个终点
        // 起点固定为x
        semanticSearchResult.setStartEntitys(SemanticSearchUtils.getIrisByEntities(xList));
        // 终点固定为y
        semanticSearchResult.setEndEntitys(SemanticSearchUtils.getIrisByEntities(yList));
        return semanticSearchResult;
    }

    /**
     * 根据模板执行SPARQL查询，问句类型3，对x有影响的y都有哪些
     * @param semanticSearchResult
     * @return SemanticSearchResult
     */
    public static SemanticSearchResult templateSparqlQueryForQuestion3(SemanticSearchResult semanticSearchResult) {
        List<QuestionParseEntity> xList = semanticSearchResult.getXList();

        List<String> covid19NameList = Arrays.asList(new String[]{"新冠", "新冠病毒", "COVID-19", "SARS-CoV-2"});
        // 问句3 对COVID-19有影响的化合物都有哪些 陈锟 2023年3月15日10:21:54
        if (covid19NameList.contains(semanticSearchResult.getXName()) && Arrays.asList(new String[]{"化合物"}).contains(semanticSearchResult.getYName())) {
            // SPARQL查询跟新冠病毒有关的化合物，新冠病毒包括：COVID-19、SARS-CoV-2
            // 起点固定为x
            semanticSearchResult.setStartEntitys(SemanticSearchUtils.getIrisByEntities(xList));
            // SPARQL原语句-pubmed论文：
            /*
                CONSTRUCT {
                  ?micro <http://www.w3.org/2002/07/owl#sameAs> ?microMesh .
                  ?pubmed <http://purl.obolibrary.org/obo/OMIT_0000110> ?microMesh .
                  ?pubmed <http://purl.obolibrary.org/obo/OMIT_0001004> ?chemMesh .
                  ?chem <http://www.w3.org/2002/07/owl#sameAs> ?chemMesh .
                  ?chem <http://end> <http://end> .
                }
                WHERE {
                  SERVICE SILENT <http://micro.semweb.csdb.cn/sparql> {
                    ?micro <http://www.w3.org/2002/07/owl#sameAs> ?microMesh .
                    FILTER (?micro IN (<http://micro.semweb.csdb.cn/resource/Mesh_D000086382>))
                  }
                  SERVICE SILENT <http://pubmed.semweb.csdb.cn/sparql> {
                    ?pubmed <http://purl.obolibrary.org/obo/OMIT_0000110> ?microMesh .
                    ?pubmed <http://purl.obolibrary.org/obo/OMIT_0001004> ?chemMesh .
                  }
                  SERVICE SILENT <http://chemdb.semweb.csdb.cn/sparql> {
                    ?chem <http://www.w3.org/2002/07/owl#sameAs> ?chemMesh .
                  }
                }
                LIMIT 20

                SPARQL原语句-美国临床试验：
                CONSTRUCT {
                  ?trial <http://purl.obolibrary.org/obo/NCIT_C93360> ?micro .
                  ?trial <http://purl.obolibrary.org/obo/OAE_0000002> ?drug .
                  ?drug <http://purl.obolibrary.org/obo/CIDO_0000022> ?pubchem .
                  ?chem <http://www.w3.org/2002/07/owl#sameAs> ?pubchem .
                  ?chem <http://end> <http://end> .
                }
                WHERE {
                  SERVICE SILENT <http://clinicaltrials.semweb.csdb.cn/sparql> {
                    ?trial <http://purl.obolibrary.org/obo/NCIT_C93360> ?micro .
                    ?trial <http://purl.obolibrary.org/obo/OAE_0000002> ?drug .
                    ?drug <http://purl.obolibrary.org/obo/CIDO_0000022> ?pubchem .
                    FILTER (?micro IN (<http://micro.semweb.csdb.cn/resource/Mesh_D000086382>))
                  }
                  SERVICE SILENT <http://chemdb.semweb.csdb.cn/sparql> {
                    ?chem <http://www.w3.org/2002/07/owl#sameAs> ?pubchem .
                  }
                }
                LIMIT 30
             */
            String wherePubmed = "WHERE {\n" +
                "  SERVICE SILENT <http://micro.semweb.csdb.cn/sparql> {\n" +
                "    ?micro <http://www.w3.org/2002/07/owl#sameAs> ?microMesh .\n" +
                "    FILTER (?micro IN " + SemanticSearchUtils.getInStringByEntities(xList) + ")\n" +
                "  }\n" +
                "  SERVICE SILENT <http://pubmed.semweb.csdb.cn/sparql> {\n" +
                "    ?pubmed <http://purl.obolibrary.org/obo/OMIT_0000110> ?microMesh .\n" +
                "    ?pubmed <http://purl.obolibrary.org/obo/OMIT_0001004> ?chemMesh .\n" +
                "  }\n" +
                "  SERVICE SILENT <http://chemdb.semweb.csdb.cn/sparql> {\n" +
                "    ?chem <http://www.w3.org/2002/07/owl#sameAs> ?chemMesh .\n" +
                "  }\n" +
                "}\n" +
                "LIMIT 20";
            String whereTrial = "WHERE {\n" +
                "  SERVICE SILENT <http://clinicaltrials.semweb.csdb.cn/sparql> {\n" +
                "    ?trial <http://purl.obolibrary.org/obo/NCIT_C93360> ?micro .\n" +
                "    ?trial <http://purl.obolibrary.org/obo/OAE_0000002> ?drug .\n" +
                "    ?drug <http://purl.obolibrary.org/obo/CIDO_0000022> ?pubchem .\n" +
                "    FILTER (?micro IN " + SemanticSearchUtils.getInStringByEntities(xList) + ")\n" +
                "  }\n" +
                "  SERVICE SILENT <http://chemdb.semweb.csdb.cn/sparql> {\n" +
                "    ?chem <http://www.w3.org/2002/07/owl#sameAs> ?pubchem .\n" +
                "  }\n" +
                "}\n" +
                "LIMIT 30";
            // 列表页
            if("list".equals(semanticSearchResult.getQueryType())){ // 列表页
                // SPARQL查询终点
                String endPubmed = "SELECT DISTINCT ( ?chem AS ?end )\n" + wherePubmed;
                String endTrial = "SELECT DISTINCT ( ?chem AS ?end )\n" + whereTrial;

                // 校验已选端点中是否包含模板所需要的端点
                List<String> sparqlList = new ArrayList<>();
                // pubmed 论文
                if (checkContainsEndpoints(Arrays.asList("http://chemdb.semweb.csdb.cn/sparql", "http://pubmed.semweb.csdb.cn/sparql", "http://micro.semweb.csdb.cn/sparql"), semanticSearchResult.getEndpoints())) {
                    sparqlList.add(endPubmed);
                }
                // 美国临床试验
                if (checkContainsEndpoints(Arrays.asList("http://chemdb.semweb.csdb.cn/sparql", "http://clinicaltrials.semweb.csdb.cn/sparql", "http://micro.semweb.csdb.cn/sparql"), semanticSearchResult.getEndpoints())) {
                    sparqlList.add(endTrial);
                }
                // 不满足任何模板所需要的端点
                if (sparqlList.isEmpty()) {
                    return semanticSearchResult;
                }
                semanticSearchResult = SemanticSearchUtils.setEndEntitysToResult(sparqlList.toArray(new String[sparqlList.size()]), semanticSearchResult);

                // 根据终点list查询列表页返回实体list
                SemanticSearchResult pageResultEntityByIris = SemanticSearchUtils.getPageResultEntityByIris(semanticSearchResult);
                semanticSearchResult.setPageResultEntityList(pageResultEntityByIris.getPageResultEntityList());
                semanticSearchResult.setPrecateList(pageResultEntityByIris.getPrecateList());
            }
            // 关系图
            else {
                // 查询起点、终点、答案。对于问句类型3，有1个起点，多个终点
                String constructPubmed = "CONSTRUCT {\n" +
                        "  ?micro <http://www.w3.org/2002/07/owl#sameAs> ?microMesh .\n" +
                        "  ?pubmed <http://purl.obolibrary.org/obo/OMIT_0000110> ?microMesh .\n" +
                        "  ?pubmed <http://purl.obolibrary.org/obo/OMIT_0001004> ?chemMesh .\n" +
                        "  ?chem <http://www.w3.org/2002/07/owl#sameAs> ?chemMesh .\n" +
                        "  ?chem <" + ENDURI + "> <" + ENDURI + "> .\n" +
                        "}\n" + wherePubmed;
                String constructTrial = "CONSTRUCT {\n" +
                        "  ?trial <http://purl.obolibrary.org/obo/NCIT_C93360> ?micro .\n" +
                        "  ?trial <http://purl.obolibrary.org/obo/OAE_0000002> ?drug .\n" +
                        "  ?drug <http://purl.obolibrary.org/obo/CIDO_0000022> ?pubchem .\n" +
                        "  ?chem <http://www.w3.org/2002/07/owl#sameAs> ?pubchem .\n" +
                        "  ?chem <" + ENDURI + "> <" + ENDURI + "> .\n" +
                        "}\n" + whereTrial;

                // 校验已选端点中是否包含模板所需要的端点
                List<String> sparqlList = new ArrayList<>();
                // pubmed 论文
                if (checkContainsEndpoints(Arrays.asList("http://chemdb.semweb.csdb.cn/sparql", "http://pubmed.semweb.csdb.cn/sparql", "http://micro.semweb.csdb.cn/sparql"), semanticSearchResult.getEndpoints())) {
                    sparqlList.add(constructPubmed);
                }
                // 美国临床试验
                if (checkContainsEndpoints(Arrays.asList("http://chemdb.semweb.csdb.cn/sparql", "http://clinicaltrials.semweb.csdb.cn/sparql", "http://micro.semweb.csdb.cn/sparql"), semanticSearchResult.getEndpoints())) {
                    sparqlList.add(constructTrial);
                }
                // 不满足任何模板所需要的端点
                if (sparqlList.isEmpty()) {
                    return semanticSearchResult;
                }
                semanticSearchResult = SemanticSearchUtils.searchNodesAndEdges(sparqlList.toArray(new String[sparqlList.size()]), semanticSearchResult);
            }
        }
        // 问句3 对新冠病毒有影响的植物都有哪些 陈锟 2023年3月28日17:15:56
        else if (covid19NameList.contains(semanticSearchResult.getXName()) && Arrays.asList(new String[]{"植物"}).contains(semanticSearchResult.getYName())) {
            // 起点固定为x
            semanticSearchResult.setStartEntitys(SemanticSearchUtils.getIrisByEntities(xList));
            /*
                SPARQL原语句-pubmed论文：
                CONSTRUCT {
                  ?micro <http://www.w3.org/2002/07/owl#sameAs> ?microMesh .
                  ?pubmed <http://purl.obolibrary.org/obo/OMIT_0000110> ?microMesh .
                  ?pubmed <http://purl.obolibrary.org/obo/OMIT_0001004> ?chemMesh .
                  ?chem <http://www.w3.org/2002/07/owl#sameAs> ?chemMesh .
                  ?herb <http://purl.obolibrary.org/obo/CIDO_0000022> ?chem .
                  ?herb <http://semanticscience.org/resource/SIO_000313> ?plant .
                  ?plant <http://end> <http://end> .
                }
                WHERE {
                  SERVICE SILENT <http://micro.semweb.csdb.cn/sparql> {
                    ?micro <http://www.w3.org/2002/07/owl#sameAs> ?microMesh .
                    FILTER (?micro IN (<http://micro.semweb.csdb.cn/resource/Mesh_D000086382>))
                  }
                  SERVICE SILENT <http://pubmed.semweb.csdb.cn/sparql> {
                    ?pubmed <http://purl.obolibrary.org/obo/OMIT_0000110> ?microMesh .
                    ?pubmed <http://purl.obolibrary.org/obo/OMIT_0001004> ?chemMesh .
                  }
                  SERVICE SILENT <http://chemdb.semweb.csdb.cn/sparql> {
                    ?chem <http://www.w3.org/2002/07/owl#sameAs> ?chemMesh .
                  }
                  SERVICE SILENT <http://xtipc.semweb.csdb.cn/sparql> {
                    ?herb <http://purl.obolibrary.org/obo/CIDO_0000022> ?chem .
                    ?herb <http://semanticscience.org/resource/SIO_000313> ?plant .
                  }
                }
                LIMIT 100

                SPARQL原语句-美国临床试验：
                CONSTRUCT {
                  ?trial <http://purl.obolibrary.org/obo/NCIT_C93360> ?micro .
                  ?trial <http://purl.obolibrary.org/obo/OAE_0000002> ?drug .
                  ?drug <http://purl.obolibrary.org/obo/CIDO_0000022> ?pubchem .
                  ?chem <http://www.w3.org/2002/07/owl#sameAs> ?pubchem .
                  ?herb <http://purl.obolibrary.org/obo/CIDO_0000022> ?chem .
                  ?herb <http://semanticscience.org/resource/SIO_000313> ?plant .
                  ?plant <http://end> <http://end> .
                }
                WHERE {
                  SERVICE SILENT <http://clinicaltrials.semweb.csdb.cn/sparql> {
                    ?trial <http://purl.obolibrary.org/obo/NCIT_C93360> ?micro .
                    ?trial <http://purl.obolibrary.org/obo/OAE_0000002> ?drug .
                    ?drug <http://purl.obolibrary.org/obo/CIDO_0000022> ?pubchem .
                    FILTER (?micro IN (<http://micro.semweb.csdb.cn/resource/Mesh_D000086382>))
                  }
                  SERVICE SILENT <http://chemdb.semweb.csdb.cn/sparql> {
                    ?chem <http://www.w3.org/2002/07/owl#sameAs> ?pubchem .
                  }
                  SERVICE SILENT <http://xtipc.semweb.csdb.cn/sparql> {
                    ?herb <http://purl.obolibrary.org/obo/CIDO_0000022> ?chem .
                    ?herb <http://semanticscience.org/resource/SIO_000313> ?plant .
                  }
                }
                LIMIT 100
             */
            String wherePubmed = "WHERE {\n" +
                    "  SERVICE SILENT <http://micro.semweb.csdb.cn/sparql> {\n" +
                    "    ?micro <http://www.w3.org/2002/07/owl#sameAs> ?microMesh .\n" +
                    "    FILTER (?micro IN " + SemanticSearchUtils.getInStringByEntities(xList) + ")\n" +
                    "  }\n" +
                    "  SERVICE SILENT <http://pubmed.semweb.csdb.cn/sparql> {\n" +
                    "    ?pubmed <http://purl.obolibrary.org/obo/OMIT_0000110> ?microMesh .\n" +
                    "    ?pubmed <http://purl.obolibrary.org/obo/OMIT_0001004> ?chemMesh .\n" +
                    "  }\n" +
                    "  SERVICE SILENT <http://chemdb.semweb.csdb.cn/sparql> {\n" +
                    "    ?chem <http://www.w3.org/2002/07/owl#sameAs> ?chemMesh .\n" +
                    "  }\n" +
                    "  SERVICE SILENT <http://xtipc.semweb.csdb.cn/sparql> {\n" +
                    "    ?herb <http://purl.obolibrary.org/obo/CIDO_0000022> ?chem .\n" +
                    "    ?herb <http://semanticscience.org/resource/SIO_000313> ?plant .\n" +
                    "  }\n" +
                    "}\n" +
                    "LIMIT 100";
            String whereTrial = "WHERE {\n" +
                    "  SERVICE SILENT <http://clinicaltrials.semweb.csdb.cn/sparql> {\n" +
                    "    ?trial <http://purl.obolibrary.org/obo/NCIT_C93360> ?micro .\n" +
                    "    ?trial <http://purl.obolibrary.org/obo/OAE_0000002> ?drug .\n" +
                    "    ?drug <http://purl.obolibrary.org/obo/CIDO_0000022> ?pubchem .\n" +
                    "    FILTER (?micro IN " + SemanticSearchUtils.getInStringByEntities(xList) + ")\n" +
                    "  }\n" +
                    "  SERVICE SILENT <http://chemdb.semweb.csdb.cn/sparql> {\n" +
                    "    ?chem <http://www.w3.org/2002/07/owl#sameAs> ?pubchem .\n" +
                    "  }\n" +
                    "  SERVICE SILENT <http://xtipc.semweb.csdb.cn/sparql> {\n" +
                    "    ?herb <http://purl.obolibrary.org/obo/CIDO_0000022> ?chem .\n" +
                    "    ?herb <http://semanticscience.org/resource/SIO_000313> ?plant .\n" +
                    "  }\n" +
                    "}\n" +
                    "LIMIT 100";
            // 列表页
            if("list".equals(semanticSearchResult.getQueryType())){ // 列表页
                // SPARQL查询终点
                String endPubmed = "SELECT DISTINCT ( ?plant AS ?end )\n" + wherePubmed;
                String endTrial = "SELECT DISTINCT ( ?plant AS ?end )\n" + whereTrial;

                // 校验已选端点中是否包含模板所需要的端点
                List<String> sparqlList = new ArrayList<>();
                // pubmed 论文
                if (checkContainsEndpoints(Arrays.asList("https://www.plantplus.cn/plantsw/sparql", "http://xtipc.semweb.csdb.cn/sparql", "http://chemdb.semweb.csdb.cn/sparql", "http://pubmed.semweb.csdb.cn/sparql", "http://micro.semweb.csdb.cn/sparql"), semanticSearchResult.getEndpoints())) {
//                    sparqlList.add(endPubmed);
                }
                // 美国临床试验
                if (checkContainsEndpoints(Arrays.asList("https://www.plantplus.cn/plantsw/sparql", "http://xtipc.semweb.csdb.cn/sparql", "http://chemdb.semweb.csdb.cn/sparql", "http://clinicaltrials.semweb.csdb.cn/sparql", "http://micro.semweb.csdb.cn/sparql"), semanticSearchResult.getEndpoints())) {
                    sparqlList.add(endTrial);
                }
                // 不满足任何模板所需要的端点
                if (sparqlList.isEmpty()) {
                    return semanticSearchResult;
                }
                semanticSearchResult = SemanticSearchUtils.setEndEntitysToResult(sparqlList.toArray(new String[sparqlList.size()]), semanticSearchResult);

                // 根据终点list查询列表页返回实体list
                SemanticSearchResult pageResultEntityByIris = SemanticSearchUtils.getPageResultEntityByIris(semanticSearchResult);
                semanticSearchResult.setPageResultEntityList(pageResultEntityByIris.getPageResultEntityList());
                semanticSearchResult.setPrecateList(pageResultEntityByIris.getPrecateList());
            }
            // 关系图
            else {
                // 查询起点、终点、答案。对于问句类型3，有1个起点，多个终点
                // SPARQL查询关系图数据
                String constructPubmed = "CONSTRUCT {\n" +
                        "  ?micro <http://www.w3.org/2002/07/owl#sameAs> ?microMesh .\n" +
                        "  ?pubmed <http://purl.obolibrary.org/obo/OMIT_0000110> ?microMesh .\n" +
                        "  ?pubmed <http://purl.obolibrary.org/obo/OMIT_0001004> ?chemMesh .\n" +
                        "  ?chem <http://www.w3.org/2002/07/owl#sameAs> ?chemMesh .\n" +
                        "  ?herb <http://purl.obolibrary.org/obo/CIDO_0000022> ?chem .\n" +
                        "  ?herb <http://semanticscience.org/resource/SIO_000313> ?plant .\n" +
                        "  ?plant <" + ENDURI + "> <" + ENDURI + "> .\n" +
                        "}\n" + wherePubmed;
                String constructTrial = "CONSTRUCT {\n" +
                        "  ?trial <http://purl.obolibrary.org/obo/NCIT_C93360> ?micro .\n" +
                        "  ?trial <http://purl.obolibrary.org/obo/OAE_0000002> ?drug .\n" +
                        "  ?drug <http://purl.obolibrary.org/obo/CIDO_0000022> ?pubchem .\n" +
                        "  ?chem <http://www.w3.org/2002/07/owl#sameAs> ?pubchem .\n" +
                        "  ?herb <http://purl.obolibrary.org/obo/CIDO_0000022> ?chem .\n" +
                        "  ?herb <http://semanticscience.org/resource/SIO_000313> ?plant .\n" +
                        "  ?plant <" + ENDURI + "> <" + ENDURI + "> .\n" +
                        "}\n" + whereTrial;

                // 校验已选端点中是否包含模板所需要的端点
                List<String> sparqlList = new ArrayList<>();
                // pubmed 论文
                if (SemanticSearchUtils.checkContainsEndpoints(Arrays.asList("https://www.plantplus.cn/plantsw/sparql", "http://xtipc.semweb.csdb.cn/sparql", "http://chemdb.semweb.csdb.cn/sparql", "http://pubmed.semweb.csdb.cn/sparql", "http://micro.semweb.csdb.cn/sparql"), semanticSearchResult.getEndpoints())) {
//                    sparqlList.add(constructPubmed);
                }
                // 美国临床试验
                if (SemanticSearchUtils.checkContainsEndpoints(Arrays.asList("https://www.plantplus.cn/plantsw/sparql", "http://xtipc.semweb.csdb.cn/sparql", "http://chemdb.semweb.csdb.cn/sparql", "http://clinicaltrials.semweb.csdb.cn/sparql", "http://micro.semweb.csdb.cn/sparql"), semanticSearchResult.getEndpoints())) {
                    sparqlList.add(constructTrial);
                }
                // 不满足任何模板所需要的端点
                if (sparqlList.isEmpty()) {
                    return semanticSearchResult;
                }
                semanticSearchResult = SemanticSearchUtils.searchNodesAndEdges(sparqlList.toArray(new String[sparqlList.size()]), semanticSearchResult);
            }
        }
        // 问句3 对Cepharanthine有影响的植物都有哪些 陈锟 2023年3月15日10:21:54
        else if (Arrays.asList(new String[]{"植物物种", "植物"}).contains(semanticSearchResult.getYName())) {
            // SPARQL查询跟任意化合物有关的植物
            // 起点固定为x
            semanticSearchResult.setStartEntitys(SemanticSearchUtils.getIrisByEntities(xList));
            /*
                SPARQL原语句：
                CONSTRUCT {
                  ?herb <http://purl.obolibrary.org/obo/CIDO_0000022> ?chem .
                  ?herb <http://semanticscience.org/resource/SIO_000313> ?plant .
                  ?plant <http://end> <http://end> .
                }
                WHERE {
                  SERVICE SILENT <http://xtipc.semweb.csdb.cn/sparql> {
                    ?herb <http://purl.obolibrary.org/obo/CIDO_0000022> ?chem .
                    ?herb <http://semanticscience.org/resource/SIO_000313> ?plant .
                    FILTER (?chem IN (<http://chemdb.semweb.csdb.cn/resource/Compound_69256-15-1>))
                  }
                }
                LIMIT 100
             */
            String where = "WHERE {\n" +
                    "  SERVICE SILENT <http://xtipc.semweb.csdb.cn/sparql> {\n" +
                    "    ?herb <http://purl.obolibrary.org/obo/CIDO_0000022> ?chem .\n" +
                    "    ?herb <http://semanticscience.org/resource/SIO_000313> ?plant .\n" +
                    "    FILTER (?chem IN " + SemanticSearchUtils.getInStringByEntities(xList) + ")\n" +
                    "  }\n" +
                    "}\n" +
                    "LIMIT 100";
            // 列表页
            if("list".equals(semanticSearchResult.getQueryType())){ // 列表页
                // SPARQL查询终点
                String end = "SELECT DISTINCT ( ?plant AS ?end )\n" + where;
                // 校验已选端点中是否包含模板所需要的端点
                List<String> sparqlList = new ArrayList<>();
                if (SemanticSearchUtils.checkContainsEndpoints(Arrays.asList("https://www.plantplus.cn/plantsw/sparql", "http://xtipc.semweb.csdb.cn/sparql", "http://chemdb.semweb.csdb.cn/sparql"), semanticSearchResult.getEndpoints())) {
                    sparqlList.add(end);
                }
                // 不满足任何模板所需要的端点
                if (sparqlList.isEmpty()) {
                    return semanticSearchResult;
                }
                semanticSearchResult = SemanticSearchUtils.setEndEntitysToResult(sparqlList.toArray(new String[sparqlList.size()]), semanticSearchResult);

                // 根据终点list查询列表页返回实体list
                SemanticSearchResult pageResultEntityByIris = SemanticSearchUtils.getPageResultEntityByIris(semanticSearchResult);
                semanticSearchResult.setPageResultEntityList(pageResultEntityByIris.getPageResultEntityList());
                semanticSearchResult.setPrecateList(pageResultEntityByIris.getPrecateList());
            }
            // 关系图
            else {
                // 查询起点、终点、答案。对于问句类型3，有1个起点，多个终点
                String construct = "CONSTRUCT {\n" +
                        "  ?herb <http://purl.obolibrary.org/obo/CIDO_0000022> ?chem .\n" +
                        "  ?herb <http://semanticscience.org/resource/SIO_000313> ?plant .\n" +
                        "  ?plant <" + ENDURI + "> <" + ENDURI + "> .\n" +
                        "}\n" + where;

                // 校验已选端点中是否包含模板所需要的端点
                List<String> sparqlList = new ArrayList<>();
                if (SemanticSearchUtils.checkContainsEndpoints(Arrays.asList("https://www.plantplus.cn/plantsw/sparql", "http://xtipc.semweb.csdb.cn/sparql", "http://chemdb.semweb.csdb.cn/sparql"), semanticSearchResult.getEndpoints())) {
                    sparqlList.add(construct);
                }
                // 不满足任何模板所需要的端点
                if (sparqlList.isEmpty()) {
                    return semanticSearchResult;
                }
                semanticSearchResult = SemanticSearchUtils.searchNodesAndEdges(sparqlList.toArray(new String[sparqlList.size()]), semanticSearchResult);
            }
        }
        return semanticSearchResult;
    }

    /**
     * 根据模板执行SPARQL查询，问句类型4：x的分布地有哪些特点
     * @param result
     * @return SemanticSearchResult
     * @author 陈锟
     * @date 2023年3月28日17:29:30
     */
    public static SemanticSearchResult templateSparqlQueryForQuestion4(SemanticSearchResult result) {
        /*
            SPARQL原语句：
            CONSTRUCT {
              ?geo ?p ?o .
            }
            WHERE {
              SERVICE SILENT <https://www.plantplus.cn/plantsw/sparql> {
                ?specimen <http://rs.tdwg.org/dwc/iri/toTaxon> ?plant .
                ?specimen <http://rs.tdwg.org/dwc/terms/county> ?county .
                FILTER (?plant IN (<https://www.plantplus.cn/plantsw/resource/Taxon_Pinus_bhutanica>))
              }
              SERVICE SILENT <http://linkedgeodata.org/sparql> {
                ?geo <http://www.geonames.org/ontology#alternateName> ?county .
                ?geo ?p ?o .
              }
            }
            LIMIT 100
         */
        String where = "WHERE {\n" +
                "  SERVICE SILENT <https://www.plantplus.cn/plantsw/sparql> {\n" +
                "    ?specimen <http://rs.tdwg.org/dwc/iri/toTaxon> ?plant .\n" +
                "    ?specimen <http://rs.tdwg.org/dwc/terms/county> ?county .\n" +
                "    FILTER (?plant IN " + SemanticSearchUtils.getInStringByEntities(result.getXList()) + ")\n" +
                "  }\n" +
                "  SERVICE SILENT <http://linkedgeodata.org/sparql> {\n" +
                "    ?geo <http://www.geonames.org/ontology#alternateName> ?county .\n" +
                "    ?geo ?p ?o .\n" +
                "  }\n" +
                "}\n" +
                "LIMIT 100";
        String construct = "CONSTRUCT {\n" +
                "  ?specimen <http://rs.tdwg.org/dwc/iri/toTaxon> <https://www.plantplus.cn/plantsw/resource/Taxon_Pinus_bhutanica> .\n" +
                "  ?specimen <http://rs.tdwg.org/dwc/terms/county> ?county .\n" +
                "  ?geo <http://www.geonames.org/ontology#alternateName> ?county .\n" +
                "  ?geo ?p ?o .\n" +
                "}\n" + where;

        // 校验已选端点中是否包含模板所需要的端点
        List<String> sparqlList = new ArrayList<>();
        if (SemanticSearchUtils.checkContainsEndpoints(Arrays.asList("https://www.plantplus.cn/plantsw/sparql", "http://linkedgeodata.org/sparql"), result.getEndpoints())) {
            sparqlList.add(construct);
        }
        // 不满足任何模板所需要的端点
        if (sparqlList.isEmpty()) {
            return result;
        }
        result = SemanticSearchUtils.searchNodesAndEdges(sparqlList.toArray(new String[sparqlList.size()]), result);

        // 查询起点、终点、答案。对于问句类型4，有1个起点，0个终点
        // 起点固定为x
        result.setStartEntitys(SemanticSearchUtils.getIrisByEntities(result.getXList()));
        return result;
    }

    /**
     * 封装propertyList，用于前端展示属性；
     * 只针对问句类型1，键：属性名称，值：多个属性值。对属性进行筛选，属性值包含中文，长度不大于10，每次请求取前3个
     *
     * @param result
     * @return List<Map < String, List < String>>>，返回结果举例：[{'名称':['地不容', '金不换']}, {'类型':['植物']}, ...]
     * @author 陈锟
     * @since 2023年5月12日14:52:56
     */
    public static List<Map<String, List<String>>> getPropertyList(SemanticSearchResult result) {
        // 专门提取label、type
        List<String> labelList = new ArrayList<>();
        List<String> typeList = new ArrayList<>();
        // 中文属性
        Map<String, List<String>> zhPropertyListMap = new HashMap<>();
        // 英文属性
        Map<String, List<String>> enPropertyListMap = new HashMap<>();
        // 将nodes转化为map，便于通过id来获取
        Map<String, VisjsNode> nodesMap = new HashMap<>();
        result.getNodes().forEach(node -> nodesMap.put(node.getId(), node));

        // 遍历图中的所有边，封装属性名称和属性值
        for (VisjsEdge edge : result.getEdges()) {
            String preUri = edge.getUri();
            VisjsNode objNode = nodesMap.get(edge.getTo());
            if (RDFS.label.getURI().equals(preUri)) {
                // 封装label，必须满足展示度高的条件
                String propertyValue = objNode.getLabel();
                if (CommonUtils.isShowLabel(propertyValue) > 0) {
                    labelList.add(propertyValue);
                }
            } else if (RDF.type.getURI().equals(preUri)) {
                // 封装type，必须满足展示度高的条件
                String propertyValue = objNode.getLabel();
                if (CommonUtils.isShowLabel(propertyValue) > 0) {
                    typeList.add(propertyValue);
                }
            } else {
                // 封装其他属性
                // 筛选展示度高的属性值
                String propertyValue = objNode.getLabel();
                int isShowLabel = CommonUtils.isShowLabel(propertyValue);
                // 封装属性到对应的propertyListMap。但注意这里先将属性名称存成uri，在后面将uri统一替换为label
                if (isShowLabel == 1) {
                    // 英文属性
                    if (!enPropertyListMap.containsKey(preUri)) {
                        enPropertyListMap.put(preUri, new ArrayList<>());
                    }
                    enPropertyListMap.get(preUri).add(propertyValue);
                } else if (isShowLabel == 2) {
                    // 中文属性
                    if (!zhPropertyListMap.containsKey(preUri)) {
                        zhPropertyListMap.put(preUri, new ArrayList<>());
                    }
                    zhPropertyListMap.get(preUri).add(propertyValue);
                }
            }
        }
        // 对于英文属性，如果在中文属性中也有，则补充到中文属性中，并从英文属性中移除
        List<String> removedPreUriList = new ArrayList<>();
        for (String preUri : enPropertyListMap.keySet()) {
            if (zhPropertyListMap.containsKey(preUri)) {
                zhPropertyListMap.get(preUri).addAll(enPropertyListMap.get(preUri));
                removedPreUriList.add(preUri);
            }
        }
        removedPreUriList.forEach(preUri -> enPropertyListMap.remove(preUri));
        // 属性值去重
        labelList = labelList.stream().distinct().collect(Collectors.toList());
        typeList = typeList.stream().distinct().collect(Collectors.toList());
        zhPropertyListMap.replaceAll((k, v) -> v.stream().distinct().collect(Collectors.toList()));
        enPropertyListMap.replaceAll((k, v) -> v.stream().distinct().collect(Collectors.toList()));

        // 封装最终返回结果
        List<Map<String, List<String>>> propertyList = new ArrayList<>();
        // 将label、type固定放在开头
        if (labelList.size() > 0) {
            Map<String, List<String>> property = new HashMap<>();
            property.put("名称", labelList);
            propertyList.add(0, property);
        }
        if (typeList.size() > 0) {
            Map<String, List<String>> property = new HashMap<>();
            property.put("类型", typeList);
            propertyList.add(propertyList.size(), property);
        }
        // 中文属性
        for (String preUri : zhPropertyListMap.keySet()) {
            if (zhPropertyListMap.get(preUri).size() > 0) {
                Map<String, List<String>> property = new HashMap<>();
                String propertyName = SemanticSearchUtils.getLabelByIri(preUri);
                property.put(propertyName, zhPropertyListMap.get(preUri));
                propertyList.add(property);
            }
        }
        // 英文属性
        for (String preUri : enPropertyListMap.keySet()) {
            if (enPropertyListMap.get(preUri).size() > 0) {
                Map<String, List<String>> property = new HashMap<>();
                String propertyName = SemanticSearchUtils.getLabelByIri(preUri);
                property.put(propertyName, enPropertyListMap.get(preUri));
                propertyList.add(property);
            }
        }
        // 返回前2个属性
        propertyList = propertyList.subList(0, Math.min(2, propertyList.size()));
        return propertyList;
    }

    /**
     * 封装SPARQL查询通用结果
     *
     * @param sparqls
     * @param result
     * @return result
     * @author 陈锟
     * @date 2023年3月9日13:04:42
     */
    public static SemanticSearchResult searchNodesAndEdges(String[] sparqls, SemanticSearchResult result) {
        Model model = ModelFactory.createDefaultModel();
        for (String sparql : sparqls) {
            // 调用SPARQL联邦查询引擎
            Model execModel = RdfUtils.sparqlConstructWithEndpoints(sparql, result.getEndpoints());
            if (execModel != null && execModel.size() > 0) {
                model.add(execModel);
            }
        }

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
            if(StringUtils.isNotBlank(label)){
                node.setLabel(label);
            }
        });
        edges.forEach(edge -> {
            String label = iriLabelMap.get(edge.getUri());
            if(StringUtils.isNotBlank(label)){
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
     * 根据SPARQL查询终点，并封装终点到 endEntitys
     *
     * @param sparqls
     * @param semanticSearchResult
     * @return SemanticSearchResult
     * @author 陈锟
     * @date 2023年3月14日12:53:08
     */
    public static SemanticSearchResult setEndEntitysToResult(String[] sparqls, SemanticSearchResult semanticSearchResult) {
        // SPARQL联邦查询语义检索的终点
        List<String> sparqlSelectEnds = new ArrayList<>();
        for (String sparql : sparqls) {
            ResultSet resultSet = RdfUtils.sparqlSelectWithEndpoints(sparql, semanticSearchResult.getEndpoints());
            while (resultSet.hasNext()) {
                QuerySolution solution =  resultSet.nextSolution();
                RDFNode rdfNode = solution.get("end");
                if (rdfNode.isResource()) {
                    sparqlSelectEnds.add(rdfNode.asResource().getURI());
                }
            }
        }

        Map<String, String> iriLabelMap = semanticSearchResult.getIriLabelMap();
        Set<String> endEntitys = new HashSet<>();
        sparqlSelectEnds.forEach(ele -> {
            // iriLabelMap必须包含此值（原则上来说肯定包含，除非有错）
            // 对实体和字面量的获取方式不同，参见iriLabelMap字段上的解释
            if (RdfUtils.isIRI(ele)) { // 实体
                endEntitys.add(ele);
            } else { // 字面量
                if (iriLabelMap.containsKey(ele)) {
                    endEntitys.add(iriLabelMap.get(ele));
                }
            }
        });
        semanticSearchResult.setEndEntitys(new ArrayList<>(endEntitys));
        return semanticSearchResult;
    }

    /**
     * 将关系图拆分成多组路径，用于页面动态展示；
     * 根据nodes和edges，封装visjsGroups
     *
     * @param semanticSearchResult
     * @return SemanticSearchResult
     * @author 韩孟航
     * @date 2023年3月9日12:31:03
     */
    public static SemanticSearchResult splitPath(SemanticSearchResult semanticSearchResult) {
        if(null == semanticSearchResult){
            return null;
        }
        // 测试代码提交前需注释 陈锟 2023年3月20日10:29:44
        //起点
        List<String> startEntitys = semanticSearchResult.getStartEntitys();

        //测试
//        List<String> startEntitys = new ArrayList<>();
//        startEntitys.add("101");

        //终点
        List<String> endEntitys = semanticSearchResult.getEndEntitys();

        //测试
//        List<String> endEntitys = new ArrayList<>();
//        endEntitys.add("104");
//        endEntitys.add("107");

        //节点
        Set<VisjsNode> nodeSet = semanticSearchResult.getNodes();

        //测试
//        Set<VisjsNode> nodeSet = new HashSet<>();
//        nodeSet.add(new VisjsNode("101","101","101"));
//        nodeSet.add(new VisjsNode("102","102","102"));
//        nodeSet.add(new VisjsNode("103","103","103"));
//        nodeSet.add(new VisjsNode("104","104","104"));
//        nodeSet.add(new VisjsNode("105","105","105"));
//        nodeSet.add(new VisjsNode("106","106","106"));
//        nodeSet.add(new VisjsNode("107","107","107"));
//        nodeSet.add(new VisjsNode("108","108","108"));
//        nodeSet.add(new VisjsNode("109","109","109"));
//        nodeSet.add(new VisjsNode("1010","1010","1010"));
//        nodeSet.add(new VisjsNode("1011","1011","1011"));
//        nodeSet.add(new VisjsNode("1012","1012","1012"));

        //边
        Set<VisjsEdge> edgeSet = semanticSearchResult.getEdges();

        //测试
//        Set<VisjsEdge> edgeSet = new HashSet<>();
//        edgeSet.add(new VisjsEdge("101","102",""));
//        edgeSet.add(new VisjsEdge("102","103",""));
//        edgeSet.add(new VisjsEdge("103","102",""));
//        edgeSet.add(new VisjsEdge("103","104",""));
//        edgeSet.add(new VisjsEdge("104","103",""));
//        edgeSet.add(new VisjsEdge("101","104","g"));
//        edgeSet.add(new VisjsEdge("101","104","k"));
//        edgeSet.add(new VisjsEdge("103","105",""));
//        edgeSet.add(new VisjsEdge("1010","105",""));
//        edgeSet.add(new VisjsEdge("1010","109",""));
//        edgeSet.add(new VisjsEdge("1011","105",""));
//        edgeSet.add(new VisjsEdge("1011","109",""));
//        edgeSet.add(new VisjsEdge("1012","105",""));
//        edgeSet.add(new VisjsEdge("1012","109",""));
//        edgeSet.add(new VisjsEdge("108","109",""));

        //复用集合
        Set<VisjsEdge> edges = new HashSet<>();
        edges.addAll(edgeSet);

        //边和点的结果组合
        List<VisjsGroup> visjsGroups = new ArrayList<>();

        //存储节点，节点开始的所有边
        Map<VisjsNode,Set<VisjsEdge>> map = new HashMap<>();
        Iterator<VisjsNode> nodeIterator = nodeSet.iterator();
        while (nodeIterator.hasNext()){
            Set<VisjsEdge> edges1 = new HashSet<>();
            VisjsNode node = nodeIterator.next();
            String nodeId = node.getId();
            //遍历每个边
            Iterator<VisjsEdge> edgeIterator = edges.iterator();
            while (edgeIterator.hasNext()){
                VisjsEdge edge = edgeIterator.next();
                if(nodeId.equals(edge.getFrom()) || nodeId.equals(edge.getTo())){
                    edges1.add(edge);
                }
            }
            map.put(node,edges1);
        }

//        System.out.println(map);
        //记录已经经过的点和边
        Map<String,Set<VisjsEdge>> pass = new HashMap<>();

        //处理路径，从起点开始，遍历到终点或已被遍历过的点则停止，记录这些点和边为一组
        // 支持多起点。update by 陈锟 2023年4月4日12:52:23
        for (String startEntity : startEntitys) {
            getVisjsGroup(startEntity, map, pass, endEntitys, new VisjsGroup(), visjsGroups);
        }

        //可以校验下是否丢边
//        if(checkLostEdges(map,visjsGroups) != 0){
//            System.out.println("lost edges");
//        }
        //去重
        removeRepeat(visjsGroups);

        //去空 update by 陈锟 2023年4月3日12:28:11
        visjsGroups = visjsGroups.stream().filter(visjsGroup -> (!visjsGroup.getNodes().isEmpty() || !visjsGroup.getEdges().isEmpty())).collect(Collectors.toList());

        semanticSearchResult.setVisjsGroups(visjsGroups);
        return semanticSearchResult;
    }

    /**
     * 非外部调用方法，用于问句解析内部调用
     * 封装下返回值处理函数
     *
     * @param question 问句
     * @param reg      正则数组
     * @param type     问句分类
     * @return map
     * @author 韩孟航
     * @date 2023年3月7日18:08:05
     */
    private static QuestionParseResult getQuestionParseResultDeal(String question, String reg[], String type) {
        QuestionParseResult result = new QuestionParseResult();
        for (int i = 0; i < reg.length; i++) {
            Pattern pattern = Pattern.compile(reg[i]);
            Matcher matcher = pattern.matcher(question);
            if (matcher.find()) {
                result.setX(matcher.group(1));
                if (type.equals("2") || type.equals("3")) {
                    result.setY(matcher.group(2));
                }
                result.setType(type);
                return result;
            }
        }
        return null;
    }

    /**
     * 韩孟航
     * 陈锟记录：
     *  递归时调用的方法，每次递归时往下走一条边
     *  Map<VisjsNode,Set<VisjsEdge>> map：全局缓存所有点的对应连接边，便于获取
     *  Map<String,Set<VisjsEdge>> pass：全局缓存历史点和路径，用于校验路径不重复
     *  VisjsGroup visjsGroup：从对应起点出发后经过的历史路径，存入visjsGroups后会清空
     *  List<VisjsGroup> visjsGroups：最终拆分后的所有路径
     */
    private static String getVisjsGroup(String startNode,Map<VisjsNode,Set<VisjsEdge>> map,Map<String,Set<VisjsEdge>> pass, List<String> endEntitys,VisjsGroup visjsGroup,List<VisjsGroup> visjsGroups){
        Set<VisjsEdge> visjsEdges = new HashSet<>();
        //起点对象
        VisjsNode startVisjsNode = null;
        for (VisjsNode visjsNode: map.keySet()
        ) {
            if(startNode.equals(visjsNode.getId())){
                visjsEdges = map.get(visjsNode);
                startVisjsNode = visjsNode;
                break;
            }
        }

        //记录下起点
        if(null == pass.get(startNode)){
            pass.put(startNode,new HashSet<>());
        }

        // 如果没有连接边或连接边都已经被遍历过了，那么说明该点是尽头，则单独存成1条路径 update by 陈锟 2023年3月29日18:40:12
        boolean hasNoEdges = false; // 是否为尽头
        if (visjsEdges.size() > 0) {
            // 连接边都已经被遍历过了
            if (pass.get(startNode).containsAll(visjsEdges)) {
                hasNoEdges = true;
            }
        }
        // 单独存成1条路径
        if (hasNoEdges) {
            if (!visjsGroup.getNodes().contains(startVisjsNode)) {
                visjsGroup.getNodes().add(startVisjsNode);
            }
            visjsGroups.add(visjsGroup);
            return "ok";
        }

        //第一次遍历
        int first = 1;
        //遍历初始点的所有边，每次遍历，存储边和点，直到遍历到终点停止
        for (VisjsEdge visjsEdge: visjsEdges
        ) {
            boolean isFront = true;
            //获取该点下的子节点
            String to = visjsEdge.getTo();
            if(to.equals(startNode)){
                to = visjsEdge.getFrom();
                isFront = false;
            }
            //临时变量
            VisjsGroup visjsGroupBak = new VisjsGroup();
            //获取已经存储的边和点
            Set<VisjsNode> visjsNodeSet = visjsGroup.getNodes();
            Set<VisjsEdge> visjsEdgeSet = visjsGroup.getEdges();

            //判断点是否被遍历过
            Set<VisjsEdge> visjsEdgeSetPass = pass.get(to);
            Set<VisjsEdge> visjsEdgeSetPass1 = pass.get(startNode);
            //遍历过子节点
            if(null != visjsEdgeSetPass){
                if(first == 1){
                    visjsNodeSet.add(startVisjsNode);
                    first++;
                }
                //如果为终点,直接返回结果
                if(endEntitys.contains(to)){
                    visjsEdgeSet.add(visjsEdge);
                    visjsGroupBak.setEdges(visjsEdgeSet);
                    visjsGroupBak.setNodes(visjsNodeSet);
                    visjsGroups.add(visjsGroupBak);
                    //加入完成一组结果，需要重新new
                    visjsGroup.setEdges(new HashSet<>());
                    visjsGroup.setNodes(new HashSet<>());
                    //记录下
                    visjsEdgeSetPass1.add(visjsEdge);
                }else{
                    //不为终点
                    if(!visjsEdgeSetPass.contains(visjsEdge)) {
                        //没遍历过该边
                        visjsEdgeSet.add(visjsEdge);
                        //并且返回下结果
                        visjsGroupBak.setEdges(visjsEdgeSet);
                        visjsGroupBak.setNodes(visjsNodeSet);
                        visjsGroups.add(visjsGroupBak);
                        //加入完成一组结果，需要重新new
                        visjsGroup.setEdges(new HashSet<>());
                        visjsGroup.setNodes(new HashSet<>());
                        //记录下
                        visjsEdgeSetPass.add(visjsEdge);
                        visjsEdgeSetPass1.add(visjsEdge);
                    }
                }
                continue;
            }else{
                //子节点未遍历过
                //加入边和点
                visjsEdgeSet.add(visjsEdge);
                //该起点头次遍历，需加入节点，之后就不用了
                if(first == 1){
                    visjsNodeSet.add(startVisjsNode);
                }
                //判断是否为终点，是终点，加入返回结果，且记录该点，继续遍历
                if(endEntitys.contains(to)){
                    //终点对象
                    VisjsNode EndVisjsNode = null;
                    for (VisjsNode visjsNode: map.keySet()
                    ) {
                        if(to.equals(visjsNode.getId())){
                            EndVisjsNode = visjsNode;
                            break;
                        }
                    }
                    //加入终点
                    visjsNodeSet.add(EndVisjsNode);
                    //加入一组返回结果
                    visjsGroupBak.setNodes(visjsNodeSet);
                    visjsGroupBak.setEdges(visjsEdgeSet);
                    visjsGroups.add(visjsGroupBak);
                    //加入完成一组结果，需要重新new
                    visjsGroup.setEdges(new HashSet<>());
                    visjsGroup.setNodes(new HashSet<>());
                    //记录下-记录终点（终点没边）
                    pass.put(to,new HashSet<>());
                }
                //若不是终点，则遍历子节点
                else{
                    //记录下
                    pass.get(startNode).add(visjsEdge);
                    Set<VisjsEdge> visjsEdgeSetTo = pass.get(to);
                    if(null == visjsEdgeSetTo){
                        Set<VisjsEdge> visjsEdgeSetTemp = new HashSet<>();
                        visjsEdgeSetTemp.add(visjsEdge);
                        pass.put(to,visjsEdgeSetTemp);
                    }else{
                        pass.get(to).add(visjsEdge);
                    }
                    getVisjsGroup(to,map,pass,endEntitys,visjsGroup,visjsGroups);
                }
            }
            first++;
        }
        return "ok";
    }

    /**
     * 校验是否丢边
     * @param map 所有边和点
     * @param visjsGroups 路径处理后返回的边和点组合
     * @return map.size()==0，则证明未丢边
     */
    private static int checkLostEdges(Map<VisjsNode,Set<VisjsEdge>> map,List<VisjsGroup> visjsGroups){
        //删除已处理的路径
        for (VisjsGroup visjsGroup: visjsGroups
        ) {
            Set<VisjsEdge> edges1 = visjsGroup.getEdges();
            for (VisjsEdge visjsEdge: edges1
            ) {
                for (Set<VisjsEdge> visjsEdge1: map.values()
                ) {
                    visjsEdge1.remove(visjsEdge);
                }
            }
        }
        //删除value为空的值
        Iterator<Map.Entry<VisjsNode, Set<VisjsEdge>>> iterator = map.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<VisjsNode, Set<VisjsEdge>> next = iterator.next();
            Set<VisjsEdge> visjsEdgeSet = next.getValue();
            if(visjsEdgeSet.size() == 0){
                iterator.remove();
            }
        }
        return map.size();
    }

    /**
     * 去重重复边和点
     * @param visjsGroups
     */
    public static void removeRepeat(List<VisjsGroup> visjsGroups){
        Set<VisjsNode> visjsNodeSet = new HashSet<>();
        Set<VisjsEdge> visjsEdgeSet = new HashSet<>();
        for (VisjsGroup visjsGroup : visjsGroups) {
            Iterator<VisjsNode> iterator1 = visjsGroup.getNodes().iterator();
            Set<VisjsNode> newVisjsNodeSet = new HashSet<>();
            while (iterator1.hasNext()) {
                VisjsNode node = iterator1.next();
                if(!visjsNodeSet.contains(node)){
                    visjsNodeSet.add(node);
                    newVisjsNodeSet.add(node);
                }
            }
            visjsGroup.setNodes(newVisjsNodeSet);

            Iterator<VisjsEdge> iterator2 = visjsGroup.getEdges().iterator();
            Set<VisjsEdge> newVisjsEdgeSet = new HashSet<>();
            while (iterator2.hasNext()) {
                VisjsEdge edge = iterator2.next();
                if(!visjsEdgeSet.contains(edge)){
                    visjsEdgeSet.add(edge);
                    newVisjsEdgeSet.add(edge);
                }
            }
            visjsGroup.setEdges(newVisjsEdgeSet);
        }
    }

    /**
     * 根据SPARQL查询关系图中的点和边。如果是问句类型3还会查询终点list；
     * 在方法queryOnePathForQuestion2、queryOnePathForQuestion3中会调用本方法
     * @param queryType 问句类型：2/3
     * @param sparql 带Service关键字的SPARQL联邦查询语句
     * @param pathInfo 路径对象，用于获取请求中的其他参数
     * @author 陈锟
     * @date 2023年4月26日10:33:15
     */
    /*
        测试语句，类型2，地不容-新冠（经过临床试验的那条路）：
        CONSTRUCT {
          ?herb <http://semanticscience.org/resource/SIO_000313> ?plant .
          ?herb <http://purl.obolibrary.org/obo/CIDO_0000022> ?chem .
          ?chem <http://www.w3.org/2002/07/owl#sameAs> ?pubchem .
          ?trial <http://purl.obolibrary.org/obo/NCIT_C93360> ?micro .
          ?trial <http://purl.obolibrary.org/obo/OAE_0000002> ?drug .
          ?drug <http://purl.obolibrary.org/obo/CIDO_0000022> ?pubchem .
        }
        WHERE {
          SERVICE SILENT <http://xtipc.semweb.csdb.cn/sparql> {
            ?herb <http://semanticscience.org/resource/SIO_000313> ?plant .
            ?herb <http://purl.obolibrary.org/obo/CIDO_0000022> ?chem .
            FILTER (?plant = <https://www.plantplus.cn/plantsw/resource/Taxon_Stephania_epigaea>)
          }
          SERVICE SILENT <http://chemdb.semweb.csdb.cn/sparql> {
            ?chem <http://www.w3.org/2002/07/owl#sameAs> ?pubchem .
          }
          SERVICE SILENT <http://clinicaltrials.semweb.csdb.cn/sparql> {
            ?trial <http://purl.obolibrary.org/obo/NCIT_C93360> ?micro .
            ?trial <http://purl.obolibrary.org/obo/OAE_0000002> ?drug .
            ?drug <http://purl.obolibrary.org/obo/CIDO_0000022> ?pubchem .
            FILTER (?micro = <http://micro.semweb.csdb.cn/resource/Mesh_D000086382>)
          }
        }
        LIMIT 100

        测试语句，类型3，新冠-植物（经过临床试验的那条路）：
        CONSTRUCT {
          ?trial <http://purl.obolibrary.org/obo/NCIT_C93360> ?micro .
          ?trial <http://purl.obolibrary.org/obo/OAE_0000002> ?drug .
          ?drug <http://purl.obolibrary.org/obo/CIDO_0000022> ?pubchem .
          ?chem <http://www.w3.org/2002/07/owl#sameAs> ?pubchem .
          ?herb <http://purl.obolibrary.org/obo/CIDO_0000022> ?chem .
          ?herb <http://semanticscience.org/resource/SIO_000313> ?plant .
          ?plant <http://end> <http://end> .
        }
        WHERE {
          SERVICE SILENT <http://clinicaltrials.semweb.csdb.cn/sparql> {
            ?trial <http://purl.obolibrary.org/obo/NCIT_C93360> ?micro .
            ?trial <http://purl.obolibrary.org/obo/OAE_0000002> ?drug .
            ?drug <http://purl.obolibrary.org/obo/CIDO_0000022> ?pubchem .
            FILTER (?micro = <http://micro.semweb.csdb.cn/resource/Mesh_D000086382>)
          }
          SERVICE SILENT <http://chemdb.semweb.csdb.cn/sparql> {
            ?chem <http://www.w3.org/2002/07/owl#sameAs> ?pubchem .
          }
          SERVICE SILENT <http://xtipc.semweb.csdb.cn/sparql> {
            ?herb <http://purl.obolibrary.org/obo/CIDO_0000022> ?chem .
            ?herb <http://semanticscience.org/resource/SIO_000313> ?plant .
          }
        }
        LIMIT 100
     */
    public static PathQueryResult queryGraphBySparql(String queryType, String sparql, PathInfo pathInfo) {
        PathQueryResult result = new PathQueryResult();

        /**
         * 执行SPARQL联邦查询
         */
        Model model = RdfUtils.sparqlConstruct(sparql);

        /**
         * 如果是问句类型3，取出终点并移除临时标记终点的三元组
         */
        if("3".equals(queryType)){
            List<String> ends = new ArrayList<>();
            // 包含终点的三元组
            List<Statement> statementList = new ArrayList<>();
            StmtIterator endStmtIterator = model.listStatements(null, new PropertyImpl(ENDURI), new ResourceImpl(ENDURI));
            // 遍历包含终点的三元组，将主语封装为终点list
            while (endStmtIterator.hasNext()) {
                Statement statement = endStmtIterator.nextStatement();
                statementList.add(statement);
                ends.add(statement.getSubject().getURI());
            }
            // 移除临时标记终点的三元组
            model.remove(statementList);
            // 将终点list添加到返回结果中
            result.setEnds(new HashSet<>(ends));
        }

        /**
         * 封装关系图中的点和边，点和边的字段与返回给前端的一致
         */
        // 获取所有主语
        ResIterator resIterator = model.listSubjects();
        Set<VisjsNode> nodes = new HashSet<>();
        Set<VisjsEdge> edges = new HashSet<>();
        // 循环所有主语
        while (resIterator.hasNext()) {
            Resource subject = resIterator.nextResource();
            // 将主语加入到nodes中
            VisjsNode subjectNode = new VisjsNode();
            subjectNode.setId(subject.getURI());
            // showIri用于页面展示和跳转
            subjectNode.setShowIri(subjectNode.getId());
            subjectNode.setLabel(SemanticSearchUtils.getLabelByIri(subjectNode.getId()));
            // 为主语的资源实体赋予中文名称、类型中文名称
            setEntityZhLabelAndType(subjectNode, pathInfo);
            subjectNode.setApplicationName(SemanticSearchUtils.getAppNameByResUri(subjectNode.getId()));
            subjectNode.setIriFlag(true);
            nodes.add(subjectNode);

            // 循环该主语的所有三元组，封装关系图中的点和边
            StmtIterator stmtIterator = subject.listProperties();
            while (stmtIterator.hasNext()) {
                Statement statement = stmtIterator.nextStatement();

                // 封装关系图中的点，添加到nodes，字段与返回给前端的一致
                VisjsNode node = new VisjsNode();
                if (statement.getObject().isResource()) { // 宾语为资源实体
                    String nodeId = statement.getObject().asResource().getURI();
                    node.setId(nodeId);
                    // showIri用于页面展示和跳转
                    node.setShowIri(nodeId);
                    node.setLabel(SemanticSearchUtils.getLabelByIri(nodeId));
                    // 为宾语的资源实体赋予中文名称、类型中文名称
                    setEntityZhLabelAndType(node, pathInfo);
                    node.setApplicationName(SemanticSearchUtils.getAppNameByResUri(nodeId));
                    node.setIriFlag(true);
                } else { // 宾语不为资源实体，为字面量
                    // 将宾语加入到nodes中。若宾语为字面量，则所属机构与主语相同。参照VisjsNode.id字段上的说明
                    String nodeId = statement.getObject().asLiteral().getValue().toString();
                    node.setId(nodeId);
                    node.setLabel(statement.getObject().asLiteral().getValue().toString());
                    node.setLabel_zh(node.getLabel());
                    node.setApplicationName(SemanticSearchUtils.getAppNameByResUri(subject.getURI()));
                    node.setIriFlag(false);
                }
                nodes.add(node);

                // 封装关系图中的边，添加到edges，字段与返回给前端的一致
                VisjsEdge edge = new VisjsEdge();
                edge.setFrom(subject.getURI());
                edge.setTo(node.getId());
                edge.setUri(statement.getPredicate().getURI());
                edge.setLabel(SemanticSearchUtils.getLabelByIri(edge.getUri()));
                // 关系的中文名称
                edge.setLabel_zh(SemanticSearchUtils.getPreLabel(edge.getUri()));
                edges.add(edge);
            }
        }

        /**
         * 因为点和边的uri比较长，为避免http传输内容过大，因此简化所有点和边的id值，将id从uri变为数字，可以较大缩小http传输大小
         */
        // uri和数字id的对应关系，key为uri，value为数字id
        Map<String, String> tempMap = new HashMap<>();
        long tempNum = 0;
        // 对于点，将点的id变为数字
        for (VisjsNode node : nodes) {
            if (!tempMap.containsKey(node.getId())) {
                String newId = String.valueOf(++tempNum);
                tempMap.put(node.getId(), newId);
                node.setId(newId);
            } else {
                node.setId(tempMap.get(node.getId()));
            }
        }
        // 对于边，将边的uri、from、to变为数字，且from、to和点的新id保持一致
        for (VisjsEdge edge : edges) {
            // uri
            if (!tempMap.containsKey(edge.getUri())) {
                String newId = String.valueOf(++tempNum);
                tempMap.put(edge.getUri(), newId);
                edge.setUri(newId);
            } else {
                edge.setUri(tempMap.get(edge.getUri()));
            }
            // from
            if (!tempMap.containsKey(edge.getFrom())) {
                String newId = String.valueOf(++tempNum);
                tempMap.put(edge.getFrom(), newId);
                edge.setFrom(newId);
            } else {
                edge.setFrom(tempMap.get(edge.getFrom()));
            }
            // to
            if (!tempMap.containsKey(edge.getTo())) {
                String newId = String.valueOf(++tempNum);
                tempMap.put(edge.getTo(), newId);
                edge.setTo(newId);
            } else {
                edge.setTo(tempMap.get(edge.getTo()));
            }
        }

        // 将点和边添加到返回结果中
        result.setNodes(nodes);
        result.setEdges(edges);
        return result;
    }

    /**
     * 根据资源实体iri查询资源实体label（资源实体必须在semweb中，因为本方法会从ES中查）
     *
     * @param iri
     * @return
     * @author chenkun
     * @since 2023年10月5日16:47:31
     */
    public static String getSemwebResLabel(String iri) {
        // 由于ES中只能根据IRI简称来检索，因此先将IRI处理为简称
        String subjectShort = iriToShort(iri);
        // 如果实体在semweb中，则从ES中查询
        if (subjectShort != null) {
            // 根据简称查询ES中的实体
            MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("subjectShort", subjectShort);
            org.springframework.data.elasticsearch.core.query.Query query = new NativeSearchQueryBuilder().withQuery(matchQueryBuilder).withPageable(PageRequest.of(0, 1)).build();
            SearchHits<ResourceEntity> searchHits = elasticsearchOperations.search(query, ResourceEntity.class);
            if (!searchHits.isEmpty()) {
                // 如果实体存在
                ResourceEntity resourceEntity = searchHits.getSearchHit(0).getContent();
                // 由于存的时候是TEXT存储，检索会分词，因此检索结果不一定是第1条，需要判断下检索到的实体与条件是否完全一致
                if (subjectShort.equals(resourceEntity.getSubjectShort())) {
                    return resourceEntity.getTitle();
                }
            }
        }

        // 如果实体不在semweb中，则用老方法获取实体的名称
        return getLabelByIri(iri);
    }

    /**
     * 根据谓语iri查询谓语label
     *
     * @param iri
     * @return
     * @author chenkun
     * @since 2023年9月28日14:18:24
     */
    public static String getPreLabel(String iri) {
        Query query = new Query();
        query.addCriteria(Criteria.where("uri").is(iri));
        EntityProperty entityProperty = mongoTemplate.findOne(query, EntityProperty.class);
        if (entityProperty != null) {
            return entityProperty.getLabel();
        }
        return getLabelByIri(iri);
    }

    /**
     * 根据类型iri查询类型label
     *
     * @param iri
     * @return
     * @author chenkun
     * @since 2023年10月5日17:12:02
     */
    public static String getTypeLabel(String iri) {
        Query query = new Query();
        query.addCriteria(Criteria.where("uri").is(iri));
        EntityClass entityClass = mongoTemplate.findOne(query, EntityClass.class);
        if (entityClass != null) {
            return entityClass.getLabel();
        }
        return "";
    }

    /**
     * 为资源实体赋予中文名称、类型中文名称
     *
     * @param node     资源实体node
     * @param pathInfo 路径对象，用于获取请求中的其他参数
     * @author chenkun
     * @since 2023年9月28日10:45:16
     */
    public static void setEntityZhLabelAndType(VisjsNode node, PathInfo pathInfo) {
        // 由于ES中只能根据IRI简称来检索，因此先将IRI处理为简称
        String subjectShort = iriToShort(node.getId());
        // 如果实体在semweb中，则从ES中查询
        if (subjectShort != null) {
            // 根据简称查询ES中的实体
            MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("subjectShort", subjectShort);
            org.springframework.data.elasticsearch.core.query.Query query = new NativeSearchQueryBuilder().withQuery(matchQueryBuilder).withPageable(PageRequest.of(0, 1)).build();
            SearchHits<ResourceEntity> searchHits = elasticsearchOperations.search(query, ResourceEntity.class);
            if (!searchHits.isEmpty()) {
                // 如果实体存在
                ResourceEntity resourceEntity = searchHits.getSearchHit(0).getContent();
                // 由于存的时候是TEXT存储，检索会分词，因此检索结果不一定是第1条，需要判断下检索到的实体与条件是否完全一致
                if (subjectShort.equals(resourceEntity.getSubjectShort())) {
                    // 由于实体的名称可能有多个，如果是用户问句中的资源实体，那么直接使用用户问句中的实体名称会更好
                    if (node.getId().equals(pathInfo.getStartIri())) {
                        // 若实体与起点相同，则使用用户问句中的起点名称
                        node.setLabel_zh(pathInfo.getStartName());
                    } else if (node.getId().equals(pathInfo.getEndIri())) {
                        // 若实体与终点相同，则使用用户问句中的终点名称
                        node.setLabel_zh(pathInfo.getEndName());
                    } else {
                        // 若实体未出现在问句中，则使用ES中的实体名称
                        // 资源实体的中文名称，由于在存入ES时已筛选过，因此直接使用即可
                        node.setLabel_zh(resourceEntity.getTitle());
                    }

                    // 资源实体的类型，从ES中获取
                    Set<RValue> typeValueSet = resourceEntity.getType().getValue();
                    // 由于类型可能有多个，因此筛选存在于类名表中的类型，找到一个即可
                    for (RValue rValue : typeValueSet) {
                        String typeIri = rValue.getKey();
                        long count = mongoTemplate.count(new Query().addCriteria(Criteria.where("uri").is(typeIri)), EntityClass.class);
                        if (count > 0) {
                            node.setType(typeIri);
                            break;
                        }
                    }
                }
            }
        }

        // 如果实体不在semweb中，则用老方法获取实体的名称
        if (StringUtils.isBlank(node.getLabel_zh())) {
            // 资源实体的中文名称
            node.setLabel_zh(node.getLabel());
        }
    }

    /**
     * 将资源实体的IRI处理为简写（目前仅对semweb里的资源实体有效）；
     * 如果实体不在semweb中则返回值可能为空；
     *
     * @param iri 资源实体iri，例如 http://chemdb.semweb.csdb.cn/resource/Compound_7732-18-5
     * @return String 实体IRI简写，例如 chemdb:Compound_7732-18-5
     * @author chenkun
     * @since 2023年9月28日14:18:46
     */
    public static String iriToShort(String iri) {
        List<Applications> applicationsList = mongoTemplate.findAll(Applications.class);
        for (Applications application : applicationsList) {
            // 判断哪个机构的命名空间包含在IRI中
            String nsPrefix = application.getBaseUrl() + "resource/";
            if (iri.contains(nsPrefix)) {
                String nsShort = iri.replace(nsPrefix, application.getDatacenterId() + ":");
                return nsShort;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        SemanticSearchResult s = new SemanticSearchResult();
       /* SemanticSearchUtils.splitPath(s);
        System.out.println(s.getVisjsGroups());*/
        String s1 = "http://chemdb.semweb.csdb.cn/resource/Compound_7732-18-5";
        String s2 = "http://chemdb.semweb.csdb.cn/resource/Compound_481-49-2";
        List list = new ArrayList<>();
        list.add(s1);
        list.add(s2);
        s.setEndEntitys(list);
        s.setEndpoints("http://chemdb.semweb.csdb.cn/sparql,http://micro.semweb.csdb.cn/sparql");//,http://micro.semweb.csdb.cn/sparql
        SemanticSearchResult pageResultEntityByIris = SemanticSearchUtils.getPageResultEntityByIris(s);
        System.out.println(pageResultEntityByIris);
        /*List<PageResultEntity> pageResultEntityByIris = SemanticSearchUtils.getPageResultEntityByIris(s);
        for (PageResultEntity pageResult : pageResultEntityByIris) {
            System.out.println(pageResult.toString());
        }*/
//        System.out.println(pageResultEntityByIris);
        /*String entityName = "地不容";
        String endpoints = "http://xtipc.semweb.csdb.cn/sparql,https://www.plantplus.cn/plantsw/sparql";
        List<QuestionParseEntity> resourceByName = SemanticSearchUtils.getResourceByName(entityName, endpoints);
        System.out.println(resourceByName);*/
        /*List list = new ArrayList();
        String endpoints = "http://xtipc.semweb.csdb.cn/sparql,https://www.plantplus.cn/plantsw/sparql";
        SemanticSearchResult semanticSearchResult = new SemanticSearchResult();

        semanticSearchResult.setEndpoints(endpoints);

        QuestionParseEntity qp1 = new QuestionParseEntity();
        qp1.setIri("http://xtipc.semweb.csdb.cn/resource/Herb_Stephania_epigaea_Lo");
        QuestionParseEntity qp2 = new QuestionParseEntity();
        qp2.setIri("https://www.plantplus.cn/plantsw/resource/Taxon_Stephania_epigaea");

        list.add(qp1);
        list.add(qp2);
        semanticSearchResult.setXList(list);
        SemanticSearchResult semanticSearchResult1 = SemanticSearchUtils.templateSparqlQueryForQuestion1(semanticSearchResult);
        System.out.println(semanticSearchResult1);*/
    }

    /**
     * 把传入uri进行替换成缩写格式（改为从mongo表中读取prefix表，不然每次新增类都需要改代码）
     *
     * @param uri
     * @return
     * @author chenkun
     * @since 2023年10月18日17:29:27
     */
    public static String dealPrefixReturnShort(String uri) {
        if (StringUtils.isBlank(uri)) {
            return uri;
        }
        /**
         * 改为从mongo表中读取，不然每次新增类都需要改代码
         */
        // 从uri中截取域名，以便于在mongo中进行模糊匹配
        // 处理前：http://chemdb.semweb.csdb.cn/resource/Compound_481-49-2
        // 处理后：http://chemdb.semweb.csdb.cn/
        int firstIndex = uri.indexOf("/", 8); // 第一个斜杠的下标
        String uriPrefix = uri.substring(0, firstIndex);

        // 从mongo中查询包含该域名的所有记录（可能有多组，比如：http://rs.tdwg.org/dwc/terms/、http://rs.tdwg.org/dwc/iri/）
        Query query = new Query();
        query.addCriteria(Criteria.where("uri").regex(uriPrefix));
        List<Prefix> prefixList = mongoTemplate.find(query, Prefix.class);
        // 从mongo查询结果中筛选符合条件的那一组
        if (prefixList != null && prefixList.size() > 0) {
            for (Prefix prefix : prefixList) {
                if(uri.contains(prefix.getUri())){
                    String result = uri.replace(prefix.getUri(), prefix.getPrefix() + ":");
                    return result;
                }
            }
        }
        return uri;

//        try {
//            uri = URLDecoder.decode(uri, "UTF-8");
//            for (PrefixEnum prefix : PrefixEnum.values()) {
//                if (uri.contains(prefix.getPrefix())) {
//                    Pattern p = Pattern.compile(prefix.getPrefix());
//                    Matcher m = p.matcher(uri);
//                    return m.replaceAll(prefix.getPrefixShort());
//                }
//            }
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//        return uri;
    }
}

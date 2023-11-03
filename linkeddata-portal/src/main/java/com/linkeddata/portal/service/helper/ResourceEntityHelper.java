package com.linkeddata.portal.service.helper;

import com.linkeddata.portal.entity.mongo.Dataset;
import com.linkeddata.portal.entity.resource.*;
import com.linkeddata.portal.utils.RdfUtils;
import com.linkeddata.portal.utils.SemanticSearchUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 资源实体帮助类
 *
 * @author wangzzhiliang
 * @date 20220908
 */
@Component
@Slf4j
public class ResourceEntityHelper {
    @Value("${public.ontology.endpoint}")
    private String publicOntologyEndpoint;
    private static String ontologyEndpoint;

    @PostConstruct
    public void init() {
        ontologyEndpoint = publicOntologyEndpoint;
    }

    public static StringBuilder getListMiddleStr(List<Dataset> dataSetList, ResourceListRequest request) {
        //查询中间的
        StringBuilder middleStr = new StringBuilder();
        for (int i = 0; i < dataSetList.size(); i++) {
            //查询所有符合要求的主语
            if (StringUtils.isBlank(request.getDatasetId())) {
                middleStr.append("\t\t{");
                middleStr.append("\t\t\tSERVICE SILENT <").append(dataSetList.get(i).getSparql()).append("> { \n");
                middleStr.append("\t\t\t\t SELECT distinct ?s WHERE { \n");
                middleStr.append("\t\t\t\t  bind(<").append(dataSetList.get(i).getSparql()).append("> as ?sparql)\n");
            }
            middleStr.append("\t\t\t\t  ?s rdf:type ?type . \n");
            if (StringUtils.isNotBlank(request.getCondition())) {
                middleStr.append("\t\t\t\t  ?s rdfs:label ?label .\n");
                middleStr.append("\t\t\t\t  filter(contains(?label, '").append(request.getCondition()).append("')) \n");
            }
            if (StringUtils.isBlank(request.getDatasetId())) {
                middleStr.append("\t\t\t\t } limit 100 \n");
                middleStr.append("\t\t\t} \n");
                middleStr.append("\t\t}\n");
            }
            if (i != dataSetList.size() - 1) {
                middleStr.append("UNION ");
            }
        }
        return middleStr;
    }


    /**
     * 封装指定主语的三元组数据 语句
     *
     * @author wangzhiliang
     * @date 20221008
     */
    public static StringBuilder queryTripleBySubject(List<Dataset> dataSetList, String subject, ResourceListRequest request) {
        //查询指定主语 所有三元组语句
        StringBuilder assignSubject = new StringBuilder();
        for (int i = 0; i < dataSetList.size(); i++) {
            if (StringUtils.isBlank(request.getDatasetId())) {
                assignSubject.append("\t\t{");
                assignSubject.append("\t\t\t SERVICE SILENT <").append(dataSetList.get(i).getSparql()).append("> { \n");
            }
            assignSubject.append("\t\t\t\t SELECT * WHERE { \n");
            assignSubject.append("\t\t\t\t  bind(<").append(dataSetList.get(i).getSparql()).append("> as ?sparql)\n");
            assignSubject.append("\t\t\t\t\t ?s ?p ?o\n");
            assignSubject.append("\t\t\t\t\t FILTER(?s = <").append(subject).append(">)\n");
            assignSubject.append("\t\t\t\t } \n");
            if (StringUtils.isBlank(request.getDatasetId())) {
                assignSubject.append("\t\t\t } \n");
                assignSubject.append("\t\t}\n");
            }
            if (i != dataSetList.size() - 1) {
                assignSubject.append(" UNION \n");
            }
        }

        return assignSubject;
    }

    /**
     * 封装查询数据的结果
     *
     * @author wangzhiliang
     * @date 20221008
     */
    public static ResourceList encapsulationResult(ResultSet resultSet, List<Dataset> dataSetList) {
        ResourceList resource = new ResourceList();
        String title = "";
        while (resultSet.hasNext()) {
            QuerySolution solution = resultSet.nextSolution();
            Resource sparql = solution.getResource("sparql");
            Resource subject = solution.getResource("s");
            Resource predValue = solution.getResource("p");
            RDFNode objValue = solution.get("o");
            if (StringUtils.isBlank(resource.getSparql())) {
                resource.setSparql(sparql.getURI());
                resource.setSubject(subject.getURI());
                resource.setSubjectShort(SemanticSearchUtils.dealPrefixReturnShort(subject.getURI()));
                for (Dataset dataSet : dataSetList) {
                    if (sparql.getURI().equals(dataSet.getSparql())) {
                        resource.setUnitName(dataSet.getUnitName());
                        resource.setWebsite(dataSet.getWebsite());
                        resource.setDatasetName(dataSet.getTitle());
                        resource.setIdentifier(dataSet.getIdentifier());
                    }
                }
            }
            if (null != predValue) {
                if (predValue.getURI().equals(RDFS.label.getURI())) {
                    //处理 title
                    if (StringUtils.isBlank(resource.getTitle())) {
                        resource.setTitle(objValue.isLiteral() ? objValue.asLiteral().getString().trim() : "");
                    }
                    if (objValue.isLiteral() && StringUtils.isBlank(title)) {
                        if ("zh".equals(objValue.asLiteral().getLanguage())) {
                            title = objValue.isLiteral() ? objValue.asLiteral().getString().trim() : "";
                            resource.setTitle(title);
                        }
                    }
                    ResourceLabel resourceLabel = new ResourceLabel();
                    resourceLabel.setLabelLink(predValue.getURI());
                    resourceLabel.setLabelShort(SemanticSearchUtils.dealPrefixReturnShort(predValue.getURI()));
                    Set<Map<String, String>> labelList = new HashSet<>();
                    Map<String, String> labelMap = new HashMap<>();
                    String labelKey = objValue.isLiteral() ? "" : objValue.asResource().getURI();
                    String labelValue = objValue.isLiteral() ? objValue.asLiteral().getString().trim() : SemanticSearchUtils.dealPrefixReturnShort(objValue.asResource().getURI());
                    if (null != resource.getLabel()) {
                        labelList.addAll(resource.getLabel().getValue());
                    }
                    labelMap.put(labelKey, labelValue);
                    labelList.add(labelMap);
                    resourceLabel.setValue(labelList);
                    resource.setLabel(resourceLabel);
                } else if (predValue.getURI().equals(RDF.type.getURI())) {
                    ResourceType resourceType = new ResourceType();
                    resourceType.setTypeLink(predValue.getURI());
                    resourceType.setTypeShort(SemanticSearchUtils.dealPrefixReturnShort(predValue.getURI()));
                    Set<Map<String, String>> typeList = new HashSet<>();
                    Map<String, String> typeMap = new HashMap<>();
                    String typeKey = objValue.isLiteral() ? "" : objValue.asResource().getURI();
                    String typeValue = objValue.isLiteral() ? objValue.asLiteral().getString().trim() : SemanticSearchUtils.dealPrefixReturnShort(objValue.asResource().getURI());
                    if (null != resource.getType()) {
                        typeList.addAll(resource.getType().getValue());
                    }
                    typeMap.put(typeKey, typeValue);
                    typeList.add(typeMap);
                    resourceType.setValue(typeList);
                    resource.setType(resourceType);
                } else if (predValue.getURI().equals(SKOS.closeMatch.getURI())) {
                    ResourceCloseMatch resourceCloseMatch = new ResourceCloseMatch();
                    resourceCloseMatch.setCloseMatchLink(predValue.getURI());
                    resourceCloseMatch.setCloseMatchShort(SemanticSearchUtils.dealPrefixReturnShort(predValue.getURI()));
                    Set<Map<String, String>> closeMatchList = new HashSet<>();
                    Map<String, String> closeMatchMap = new HashMap<>();
                    String closeMatchKey = objValue.isLiteral() ? "" : objValue.asResource().getURI();
                    String closeMatchValue = objValue.isLiteral() ? objValue.asLiteral().getString().trim() : SemanticSearchUtils.dealPrefixReturnShort(objValue.asResource().getURI());
                    if (null != resource.getCloseMatch()) {
                        closeMatchList.addAll(resource.getCloseMatch().getValue());
                    }
                    closeMatchMap.put(closeMatchKey, closeMatchValue);
                    closeMatchList.add(closeMatchMap);
                    resourceCloseMatch.setValue(closeMatchList);
                    resource.setCloseMatch(resourceCloseMatch);
                } else if (predValue.getURI().equals(OWL.sameAs.getURI())) {
                    ResourceSameAs resourceSameAs = new ResourceSameAs();
                    resourceSameAs.setSameAsLink(predValue.getURI());
                    resourceSameAs.setSameAsShort(SemanticSearchUtils.dealPrefixReturnShort(predValue.getURI()));
                    Set<Map<String, String>> sameAsList = new HashSet<>();
                    Map<String, String> sameAsMap = new HashMap<>();
                    String sameAsKey = objValue.isLiteral() ? "" : objValue.asResource().getURI();
                    String sameAsValue = objValue.isLiteral() ? objValue.asLiteral().getString().trim() : SemanticSearchUtils.dealPrefixReturnShort(objValue.asResource().getURI());
                    if (null != resource.getCloseMatch()) {
                        sameAsList.addAll(resource.getCloseMatch().getValue());
                    }
                    sameAsMap.put(sameAsKey, sameAsValue);
                    sameAsList.add(sameAsMap);
                    resourceSameAs.setValue(sameAsList);
                    resource.setSameAs(resourceSameAs);
                }
            } else {
                break;
            }
        }
        //去掉跟title 一样的lebel
        if (null != resource.getLabel() && StringUtils.isNotBlank(resource.getTitle())) {
            Set<Map<String, String>> labelValue = resource.getLabel().getValue();
            String resourceTitle = resource.getTitle();
            labelValue.removeIf(map -> map.containsValue(resourceTitle));
        }
        return resource;
    }

    /**
     * 封装详情查查询语句返回
     *
     * @author wangzhiliang
     * @date 20220914
     */
    public static String detailRdfQuery(List<Dataset> dataSetList, String subject) {
        //查询中间的
        StringBuilder queryStr = new StringBuilder();
        RdfUtils.setPreFix(queryStr);
        queryStr.append("SELECT distinct ?sparql ?s ?p ?o WHERE {\n");
        for (int i = 0; i < dataSetList.size(); i++) {
            queryStr.append("\t {\n");
            queryStr.append("\t\t SERVICE SILENT <").append(dataSetList.get(i).getSparql()).append("> {\n");
            queryStr.append("\t\t\t SELECT * WHERE {\n");
            queryStr.append("\t\t\t\t BIND( <").append(dataSetList.get(i).getSparql()).append("> as ?sparql ).  \n");
            queryStr.append("\t\t\t\t BIND(<").append(subject).append("> as ?s).  \n");
            queryStr.append("\t\t\t\t ?s ?p ?o. \n");
            queryStr.append("\t\t\t } \n");
            queryStr.append("\t\t } \n");
            queryStr.append("\t } \n");
            if (i != dataSetList.size() - 1) {
                queryStr.append(" UNION \n");
            }
        }
        queryStr.append("} orderby (?sparql)");
        return queryStr.toString();
    }

    /**
     * 封装详情的 content 数据
     * 过滤数据处理定义
     *
     * @author wangzhiliang
     * @date 20220916
     */
    private static final String[] removeStr = {"type", "closeMatch"};

    /**
     * 封装详情的 content 数据
     *
     * @author wangzhiliang
     * @date 20220916
     */
    public static ResourceContent handlerResourceContent(Resource predicate, RDFNode object, List<Dataset> dataSetList, String endpoint, Map<String, String> predicateLabelMap) {
        ResourceContent resourceContent = new ResourceContent();
        //判断宾语是不是资源是资源的话那么查询 rdf 获取 资源
        if (object.isLiteral()) {
            resourceContent.setLabel(object.asLiteral().getString().trim());
            resourceContent.setLanguage(object.asLiteral().getLanguage().trim());
        } else {
            //过滤并与为  type closeMatch 只需要展示缩写
            if (Arrays.asList(removeStr).contains(predicate.getLocalName())) {
                resourceContent.setIri(object.asResource().getURI());
                String iriShort = SemanticSearchUtils.dealPrefixReturnShort(object.asResource().getURI());
                //如果在所有的谓语缩写本体 label 中没有 这个对应的值则去本体库图中查询
                resourceContent.setIriShort(iriShort);
                String iriLabel = predicateLabelMap.get(iriShort);
                if (StringUtils.isBlank(iriLabel)) {
                    iriLabel = queryPreLabelByOntology(object.asResource().getURI(), predicateLabelMap);
                }
                resourceContent.setIriLabel(iriLabel);
            } else {
                StringBuilder queryByObject = new StringBuilder();
                RdfUtils.setPreFix(queryByObject);
                queryByObject.append("SELECT ?pred ?obj WHERE {\n");
                for (int i = 0; i < dataSetList.size(); i++) {
                    queryByObject.append("\t {\n");
                    queryByObject.append("\t\t SERVICE SILENT <").append(dataSetList.get(i).getSparql()).append("> {\n");
                    queryByObject.append("\t\t\t SELECT * WHERE {\n");
                    queryByObject.append("\t\t\t\t <").append(object).append("> ?pred ?obj . \n");
                    queryByObject.append("\t\t\t } \n");
                    queryByObject.append("\t\t } \n");
                    queryByObject.append("\t } \n");
                    if (i != dataSetList.size() - 1) {
                        queryByObject.append(" UNION \n");
                    }
                }
                queryByObject.append("}");
                ResultSet resultSet = RdfUtils.queryTriple(endpoint, queryByObject.toString());
                List<ResourceObject> resourceObjects = new ArrayList<>();
                while (resultSet.hasNext()) {
                    ResourceObject resourceObject = new ResourceObject();
                    QuerySolution solution = resultSet.nextSolution();
                    Resource objPred = solution.getResource("pred");
                    RDFNode objObj = solution.get("obj");
                    resourceObject.setObjectPre(objPred.getURI());
                    String objectPreShort = SemanticSearchUtils.dealPrefixReturnShort(objPred.getURI());
                    resourceObject.setObjectPreShort(objectPreShort);
                    //如果在谓语 map 集合中 谓语缩写本体 label 中没有 这个对应的值则去本体库图中查询
                    String objectPreLabel = predicateLabelMap.get(objectPreShort);
                    if (StringUtils.isBlank(objectPreLabel)) {
                        objectPreLabel = queryPreLabelByOntology(objPred.asResource().getURI(), predicateLabelMap);
                    }
                    resourceObject.setObjectPreLabel(objectPreLabel);
                    if (objObj.isLiteral()) {
                        resourceObject.setObjectLabel(objObj.asLiteral().getString().trim());
                        resourceObject.setObjectLabelLang(objObj.asLiteral().getLanguage().trim());
                    } else {
                        resourceObject.setObjectIsIri(objObj.asResource().getURI());
                        String objectIsIriShort = SemanticSearchUtils.dealPrefixReturnShort(objObj.asResource().getURI());
                        resourceObject.setObjectIsIriShort(objectIsIriShort);
                        //如果在谓语 map 集合中 谓语缩写本体 label 中没有 这个对应的值则去本体库图中查询
                        String objectIsIriLabel = predicateLabelMap.get(objectIsIriShort);
                        if (StringUtils.isBlank(objectIsIriLabel)) {
                            objectIsIriLabel = queryPreLabelByOntology(objObj.asResource().getURI(), predicateLabelMap);
                        }
                        resourceObject.setObjectIsIriLabel(objectIsIriLabel);
                    }
                    resourceObjects.add(resourceObject);
                }
                resourceContent.setIri(object.asResource().getURI());
                resourceContent.setIriShort(SemanticSearchUtils.dealPrefixReturnShort(object.asResource().getURI()));
                resourceContent.setObjects(resourceObjects);
            }
        }
        return resourceContent;
    }

    /**
     * 查询本体库中资源谓语的 label
     *
     * @param subject           要查询的谓语的全链接
     * @param predicateLabelMap 谓语总链接
     * @author wangzhiliang
     * @date 20221010
     */
    public static String queryPreLabelByOntology(String subject, Map<String, String> predicateLabelMap) {
        String resultLabel = "";
        StringBuilder stb = new StringBuilder();
        RdfUtils.setPreFix(stb);
        stb.append("SELECT ?o WHERE {\n");
        stb.append("\t\t\t\t <").append(subject).append(">  rdfs:label ?o  \n");
        stb.append("}\n");
        ResultSet resultSet = RdfUtils.queryTriple(ontologyEndpoint, stb.toString());
        List<Map<String, Object>> resultMap = RdfUtils.resultEncapsulation(resultSet);
        if (resultMap.size() > 0) {
            //赋值到返回
            resultLabel = String.valueOf(resultMap.get(0).get("o"));
            //添加到谓语总集合 map 中 便于后续使用
            predicateLabelMap.put(SemanticSearchUtils.dealPrefixReturnShort(subject), resultLabel);
        }
        return resultLabel;
    }
}

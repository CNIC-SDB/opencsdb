package com.linkeddata.portal.controller;

import com.linkeddata.portal.entity.semanticSearch.BasicResultEntity;
import com.linkeddata.portal.entity.semanticSearch.PageResultEntity;
import com.linkeddata.portal.entity.semanticSearch.PathInfo;
import com.linkeddata.portal.entity.semanticSearch.QuestionParseResult;
import com.linkeddata.portal.service.FindPathService;
import com.linkeddata.portal.service.SemanticSearchService;
import com.linkeddata.portal.utils.CommonUtils;
import com.linkeddata.portal.utils.RdfUtils;
import com.linkeddata.portal.utils.SemanticSearchUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 查询节点之间关系，相关控制类
 *
 * @author : gaoshuai
 * @date : 2023/4/23 10:43
 */
@Api(tags = "语义检索查询路径相关接口")
@RestController
public class FindPathController {

    /**
     * 最大路径长度
     */
    private final String MAX_DISTANCE = "6";
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
    private SemanticSearchService semanticSearchService;


    /**
     * 根据用户提问查找问句中x和y的关系
     *
     * @param endpoints 用户选择的端点，多个端点之间用英文逗号分隔，如 http://xtipc.semweb.csdb.cn/sparql,https://www.plantplus.cn/plantsw/sparql,http://chemdb.semweb.csdb.cn/sparql,http://micro.semweb.csdb.cn/sparql,http://clinicaltrials.semweb.csdb.cn/sparql,http://ncmi.semweb.csdb.cn/sparql
     * @return [PathInfo(startIri = http : / / purl.obolibrary.org / obo / CHEBI_23888, endIri = http : / / rs.tdwg.org / dwc / terms / Taxon, path = [ { } ])
     * PathInfo(startIri=http://rs.tdwg.org/dwc/terms/Taxon, endIri=http://purl.obolibrary.org/obo/CHEBI_23888, path=[{}])
     */
    @ApiOperation(value = "查询两实体之间路径")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "question", value = "页面上输入的问句", example = "地不容和covid-19有无关联", required = true),
            @ApiImplicitParam(name = "endpoints", value = "sparql端点list，多个端点用英文逗号隔开", example = "http://endpoint1,http://endpoint2", required = true)
    })
    @GetMapping("/findPath")
    public Map findPath(String endpoints, HttpServletRequest request) {
        Map map = new HashMap();
        QuestionParseResult parseResult = semanticSearchService.getQuestionType(request.getQueryString());
        String questionType = parseResult.getType();
        List<PathInfo> pathInfoList = null;
        String[] split = endpoints.split(",");
        List<String> endPointList = new ArrayList<>(Arrays.asList(split));
        String x = parseResult.getX();
        String y = parseResult.getY();
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
        if (QUESTION_TYPE2.equals(questionType)) {
            xEntityTypeList = findPathService.getEntityType(x, endpoints);
            yEntityTypeList = findPathService.getEntityType(y, endpoints);
        } else if (QUESTION_TYPE3.equals(questionType)) {
            // 问句类型3 对X有影响的Y都有哪些？
            xEntityTypeList = findPathService.getClassType(x, endpoints);
            if (xEntityTypeList.isEmpty()) {
                xEntityTypeList = findPathService.getEntityType(x, endpoints);
            }
            yEntityTypeList = findPathService.getClassType(y, endpoints);
        }
        if (!xEntityTypeList.isEmpty() && !yEntityTypeList.isEmpty()) {
            if (endPointList.contains("https://dbpedia.org/sparql")) {
                endPointList.add("http://dbpedia.org/sparql");
            }
            pathInfoList = findPathService.getPathInfo(xEntityTypeList, yEntityTypeList, endPointList, MAX_DISTANCE, x, y);
        }
        // 同一个名称对应多个iri的情况，返回
        List<Map<String, String>> xIriList = this.nameAndIris(xEntityTypeList);
        List<Map<String, String>> yIiList = this.nameAndIris(yEntityTypeList);
        map.put("xIir", xIriList);
        map.put("xName", x);
        map.put("yIir", yIiList);
        map.put("yName", y);
        map.put("pathinfo", pathInfoList);
        return map;
    }


    /**
     * 根据主语iri列表查询每个实体的谓语、宾语
     *
     * @param iriList   主语列表
     * @param endpoints 端点
     * @return Map<po, List < PageResultEntity>>
     * PageResultEntity(iri=http://chemdb.semweb.csdb.cn/resource/Compound_481-49-2, label=[Cepharanthine], type=[BasicResultEntity(iri=http://purl.obolibrary.org/obo/CHEBI_3546, label=CHEBI_3546), BasicResultEntity(iri=http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C83611, label=C83611), BasicResultEntity(iri=http://purl.obolibrary.org/obo/CHEBI_24431, label=CHEBI_24431)], relationList=[BasicResultEntity(iri=http://chemdb.semweb.csdb.cn/resource/Descriptor_Cepharanthine_InChIKey, label=Descriptor_Cepharanthine_InChIKey), BasicResultEntity(iri=http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C83611, label=C83611), BasicResultEntity(iri=http://chemdb.semweb.csdb.cn/resource/Descriptor_Cepharanthine_molecular_formula, label=Descriptor_Cepharanthine_molecular_formula), BasicResultEntity(iri=http://chemdb.semweb.csdb.cn/resource/Descriptor_Cepharanthine_molecular_weight, label=Descriptor_Cepharanthine_molecular_weight), BasicResultEntity(iri=http://chemdb.semweb.csdb.cn/resource/Descriptor_Cepharanthine_CAS_registry_number, label=Descriptor_Cepharanthine_CAS_registry_number), BasicResultEntity(iri=http://rdf.ncbi.nlm.nih.gov/pubchem/compound/CID10206, label=CID10206), BasicResultEntity(iri=http://chemdb.semweb.csdb.cn/resource/Descriptor_Cepharanthine_IUPAC_Name, label=Descriptor_Cepharanthine_IUPAC_Name), BasicResultEntity(iri=https://www.wikidata.org/wiki/Q15410888, label=Q15410888), BasicResultEntity(iri=http://id.nlm.nih.gov/mesh/C006947, label=C006947)], predicateList=null)
     * @author 高帅
     * @date 2023年4月25日16:35
     */
    @ApiOperation(value = "查询所有主语的谓语和宾语")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iriList", value = "主语列表", example = "[http://chemdb.semweb.csdb.cn/resource/Compound_7732-18-5, http://chemdb.semweb.csdb.cn/resource/Compound_481-49-2]", required = true),
            @ApiImplicitParam(name = "endpoints", value = "sparql端点list，多个端点用英文逗号隔开", example = "http://chemdb.semweb.csdb.cn/sparql,http://micro.semweb.csdb.cn/sparql", required = true)
    })
    @GetMapping("/findAllPredicateAndObject")
    public Map getPageResultEntityByIris(@RequestParam("iriList") List<String> iriList, String endpoints) {
        Map map = new HashMap(16);
        // 用户回答问题时，展示的label，通常是纯中文、纯英文这种可读性好的
        List<String> showLableList = new ArrayList<>();
        // 存放纯中文label
        Set<String> zhLabelSet = new HashSet();
        // 存放不是纯中文label
        Set<String> labelSet = new HashSet();
        // 所有主语的谓语列表
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
                org.apache.jena.rdf.model.Resource subject = resIterator.next();
                // 1.1 封装该主语的iri
                resultEntity.setIri(subject.getURI());
                // 1.2 封装该主语的label，因为label是字面量，model.listStatements(subject, null, (RDFNode) null);取不到
                NodeIterator subjectLabelIterator = model.listObjectsOfProperty(subject, RDFS.label);
                List<String> subjectLabelList = new ArrayList<>();
                String label = "";
                if (subjectLabelIterator.hasNext()) {
                    while (subjectLabelIterator.hasNext()) {
                        RDFNode object = subjectLabelIterator.next();
                        Literal literal = object.asLiteral();
                        label = literal.getString();
                        label = label.replace("\\\"", "");
                    }
                } else {
                    // 如果该IRI没有label，则从iri中截取后半部分作为label
                    label = RdfUtils.getIriSuffix(subject.getURI());
                }
                subjectLabelList.add(label);
                int showLabel = CommonUtils.isShowLabel(label);
                if (2 == showLabel) {
                    zhLabelSet.add(label);
                } else if (1 == showLabel) {
                    labelSet.add(label);
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
                // 资源所属的机构
                resultEntity.setApplicationName(SemanticSearchUtils.getAppNameByResUri(resultEntity.getIri()));
                entityList.add(resultEntity);
            }
        }
        // 纯中文的label排在前面
        showLableList.addAll(zhLabelSet);
        showLableList.addAll(labelSet);
        map.put("po", entityList);
        map.put("showLableList", showLableList);
        return map;
    }

    /**
     * 将uri拼接为<uri>
     *
     * @param uri
     * @return
     */
    private String asIri(String uri) {
        return "<" + uri + ">";
    }

    /**
     * 将一个实体对应多个iri的情况，返回一个实体名：iri,实体名：iri，的list<map>列表
     *
     * @param iriList 多个列表
     * @return
     */
    private List<Map<String, String>> nameAndIris(List<Map<String, String>> iriList) {
        List<Map<String, String>> resultList = new ArrayList<>();
        Set<String> set = new HashSet();
        if (!iriList.isEmpty()) {
            for (Map map : iriList) {
                String entityIri = map.get("entityIri") + "";
                set.add(entityIri);
            }
        }
        List<String> list = set.stream().toList();
        for (String iri : list) {
            Map map = new HashMap();
            String appNameByResUri = SemanticSearchUtils.getAppNameByResUri(iri);
            map.put("applicationName", appNameByResUri);
            map.put("iri", iri);
            resultList.add(map);
        }
        return resultList;
    }

}

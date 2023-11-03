package com.linkeddata.portal.controller;

import com.linkeddata.portal.entity.*;
import com.linkeddata.portal.entity.mongo.EntityClass;
import com.linkeddata.portal.entity.neo4j.Relation;
import com.linkeddata.portal.entity.semanticSearch.*;
import com.linkeddata.portal.repository.EntityClassDao;
import com.linkeddata.portal.service.SemanticSearchService;
import com.linkeddata.portal.service.impl.Neo4jServiceImpl;
import com.linkeddata.portal.utils.LlmUtil;
import com.linkeddata.portal.utils.RdfUtils;
import com.linkeddata.portal.utils.SemanticSearchUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;


/**
 * 语义检索
 *
 * @author 陈锟
 * @date 2023年3月7日15:54:42
 */
@Api(tags = "语义检索")
@RequestMapping("/semanticSearch")
@RestController
public class SemanticSearchController {

    @Resource
    private SemanticSearchService semanticSearchService;

    @Resource
    private Neo4jServiceImpl neo4jService;

    @Resource
    private EntityClassDao entityClassDao;

    @ApiOperation(value = "语义检索")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "question", value = "页面上输入的问句", example = "地不容有哪些属性", required = true),
            @ApiImplicitParam(name = "endpoints", value = "sparql端点list，多个端点用英文逗号隔开", example = "http://endpoint1,http://endpoint2"),
            @ApiImplicitParam(name = "queryType", value = "检索类型。路径图：graph；列表页：list。默认值：graph", example = "graph", required = true)
    })
    @PostMapping("/getSemanticSearchResult")
    public ResponseData<SemanticSearchResult> getSemanticSearchResult(String question, String endpoints, @RequestParam(required = false, defaultValue = "graph") String queryType, String apps) {
        // TODO 陈锟 临时用，根据apps封装endpoints，待语义检索新版本上线后需移除
        if (StringUtils.isBlank(endpoints)) {
            if (StringUtils.isBlank(apps)) {
                apps = "plant,micro,chemdb,clinicaltrials,ncmi,xtipc,pubmed,tpdc";
            }
            String[] appArr = apps.replaceAll(" ", "").split(","); // 去除请求中的空格，并以英文分号拆分
            endpoints = "";
            for (String app : appArr) {
                if ("plant".equals(app)) {
                    endpoints += "https://www.plantplus.cn/plantsw/sparql,";
                } else if ("micro".equals(app)) {
                    endpoints += "http://micro.semweb.csdb.cn/sparql,";
                } else if ("chemdb".equals(app)) {
                    endpoints += "http://chemdb.semweb.csdb.cn/sparql,";
                } else if ("clinicaltrials".equals(app)) {
                    endpoints += "http://clinicaltrials.semweb.csdb.cn/sparql,";
                } else if ("ncmi".equals(app)) {
                    endpoints += "http://ncmi.semweb.csdb.cn/sparql,";
                } else if ("xtipc".equals(app)) {
                    endpoints += "http://xtipc.semweb.csdb.cn/sparql,";
                } else if ("pubmed".equals(app)) {
                    endpoints += "http://pubmed.semweb.csdb.cn/sparql,";
                } else if ("tpdc".equals(app)) {
                    endpoints += "http://tpdc.semweb.csdb.cn/sparql,";
                }
            }
            if (StringUtils.isNotBlank(endpoints)) {
                endpoints = endpoints.substring(0, endpoints.length() - 1);
            }
        }

        ResponseData<SemanticSearchResult> responseData = null;
        try {
            responseData = ResponseData.success(semanticSearchService.getSemanticSearchResult(question, endpoints, queryType));
        } catch (Throwable e) {
            e.printStackTrace();
            SemanticSearchResult result = new SemanticSearchResult();
            result.setAnswer("语义检索发现，未检索到合适的内容");
            responseData = ResponseData.success(result);
        }
        return responseData;
    }

    /**
     * 判断sparql端点是否可访问
     *
     * @param endpoint
     * @return
     */
    @ApiOperation("sparql端点是否可访问")
    @GetMapping("/isAccessible")
    public Boolean isAccessibleController(String endpoint) {
        return semanticSearchService.isAccessible(endpoint);
    }

    /**
     * 请求参数说明（与接口`/findPath`中的单个PathInfo对象一致）：
     * startIri：字符串，起点iri，实体
     * endIri：字符串，终点iri，实体
     * path：路径对象list，List<List<Relation>>，例如：[ [ { start:xxx, end:xxx, ...}, ... ], [...], ... ]
     *
     * 返回值说明：
     * nodes：点list，每个点包含字段：
     *      id：一定有，短标识，仅每次返回中唯一，但全局不唯一，用于简化uri，在edges中会使用点的id
     *      label：一定有，点的名称，用于展示
     *      applicationName：一定有，机构名称，与页面上的数据源名称一致（但可能不包含在）
     *      iriFlag：一定有，是否是资源实体
     *      showIri：可能有，当iriFlag=true时才有值
     * edges：边list，每条边包含字段：
     *      from：一定有，短标识，边从哪个点出发，跟nodes.id对应
     *      to：一定有，短标识，边到达哪个点，跟nodes.id对应
     *      label：一定有，边的名称，用于展示
     * @author 陈锟
     * @date 2023年4月23日14:34:26
     */
    @ApiOperation(value = "根据每个路径对象查询点和边，适用问句类型2。适用于问句‘x和y有无关联’，其中x、y都为实体。注意需将参数放入请求体中")
    @PostMapping("/queryOnePathForQuestion2")
    public ResponseData<PathQueryResult> queryOnePathForQuestion2(@RequestBody PathInfo pathInfo) {
        final String sparql = neo4jService.getSparql2(pathInfo);
        PathQueryResult result = SemanticSearchUtils.queryGraphBySparql("2", sparql, pathInfo);
        Set<String> classNameList = this.listClassName(pathInfo);
        result.setClassList(classNameList);
        // 封装路径筛选时所需要的起点和终点。对于问句类型2，起点和终点与PathInfo一致
        result.setStartIri(pathInfo.getStartIri());
        result.setEndIri(new HashSet<>(Arrays.asList(pathInfo.getEndIri())));
        return ResponseData.success(result);
    }

    /**
     * 请求参数说明（与接口`/findPath`中的单个PathInfo对象一致）：
     *      startIri：字符串，起点iri，实体
     *      endIri：字符串，终点iri，类
     *      path：路径对象list，List<List<Relation>>，例如：[ [ { start:xxx, end:xxx, ...}, ... ], [...], ... ]
     * 返回结果同queryOnePathForQuestion2，在基础上增加1个字段
     *      ends：关系图中的终点list，仅在问句类型3中有，对x有影响的y有哪些
     *          终点uri，例如：https://www.plantplus.cn/plantsw/resource/Taxon_Stephania_epigaea
     * @author 陈锟
     * @date 2023年4月23日14:34:26
     */
    @ApiOperation(value = "根据每个路径对象查询点和边，适用问句类型3。适用于问句‘对x有影响的y都有哪些’，其中x为实体，y为类型。注意需将参数放入请求体中")
    @PostMapping("/queryOnePathForQuestion3")
    public ResponseData<PathQueryResult> queryOnePathForQuestion3(@RequestBody PathInfo pathInfo) {
        pathInfo.setEndClassIri(pathInfo.getEndIri());
        final String sparql = neo4jService.getSparql2(pathInfo);
        PathQueryResult result = SemanticSearchUtils.queryGraphBySparql("3", sparql, pathInfo);
        Set<String> classNameList = this.listClassName(pathInfo);
        result.setClassList(classNameList);
        // 封装路径筛选时所需要的起点和终点。对于问句类型3，起点与PathInfo一致，终点与ends一致
        result.setStartIri(pathInfo.getStartIri());
        result.setEndIri(result.getEnds());
        return ResponseData.success(result);
    }


    //xiajl20230719获取大语言模型返回的结果字符串
    @ResponseBody
    @PostMapping("queryResultForLlm")
    public String queryResultForLlm(HttpServletRequest request) {
        return LlmUtil.queryFromLlm(request.getQueryString());
    }

    /**
     * xiajl20230424 15:02 根据问句，获取问句类型(1，2，3，4，5)
     *
     * @param request 问题内容
     * @return String
     */
    @ApiOperation(value = "根据问句，获取问句类型(1，2，3，4，5), 问句类型为1,4,5时应用原有的处理逻辑；问句类型为2或3时应用新的处理过程。")
    @PostMapping("/getQuestionType")
    public QuestionParseResult getQuestionType(HttpServletRequest request) {
        return semanticSearchService.getQuestionType(request.getQueryString());
    }

    /**
     * 查询路径中出现的实体名称
     *
     * @param pathInfo
     * @return list<String> 例如：['药材','论文','临床试验']
     */
    private Set<String> listClassName(PathInfo pathInfo) {
        // 去重存放返回结果
        Set<String> classNameSet = new HashSet<>();
        List<Relation> path = pathInfo.getPath();
        for (Relation relation : path) {
            String startUri = relation.getStart();
            String endUri = relation.getEnd();
            EntityClass startClass = entityClassDao.findByUri(startUri);
            EntityClass endClass = entityClassDao.findByUri(endUri);
            // 如果类名表中没有此类，则直接将IRI的后缀作为此类名 update by chenkun 2023年10月8日14:53:45
            String startLabel = startClass != null ? startClass.getLabel() : RdfUtils.getIriSuffix(startUri);
            String endLabel = endClass != null ? endClass.getLabel() : RdfUtils.getIriSuffix(endUri);
            classNameSet.add(startLabel);
            classNameSet.add(endLabel);
        }
        return classNameSet;
    }

    /**
     * x和y有无关联，x,y属于同一类生成sparql语句查询
     *
     * @param x         起点
     * @param y         终点
     * @param type      问句类型
     * @param endpoints 所选端点
     * @return
     */
    @ApiOperation(value = "在同一类下进行查询")
    @PostMapping("/queryInSameClass")
    public List<PathQueryResult> queryInSameClass(String x, String y, String type, String endpoints) {
        List<SparqlBuilderEntity> sparqlBuilderEntities = semanticSearchService.listSparqlInSameClass(x, y, type, endpoints);
        List<PathQueryResult> result = null;
        if (!sparqlBuilderEntities.isEmpty()) {
            result = semanticSearchService.findInSameClass(sparqlBuilderEntities, x, y);
        }
        return result;
    }


    //xiajl20230725
    @PostMapping("/getSearchResultEntity")
    public List<SearchResultEntity> getSearchResultEntity(@RequestParam("question") String question,HttpServletRequest request){
        //输入参数只能是问句类型1--4中的一种,如果是问句类型5或刚需要转换;
        QuestionParseResult result = semanticSearchService.getQuestionType(request.getQueryString());
        if (!Objects.isNull(result)){
            String type = result.getType();
            if (type.equals("5") || type.equals("6")  ) {
                question = result.getNewQuestion();
            }
        }
        //return semanticSearchService.getSearchResultEntity(question);
        return semanticSearchService.getSearchResultEntity(request.getQueryString());
    }



    @PostMapping("/getTableDataEntity")
    public List<TableDataEntity> getTableDataEntity(@RequestParam("question") String question, HttpServletRequest request) {
        //输入参数只能是问句类型1--4中的一种,如果是问句类型5或刚需要转换;
        QuestionParseResult result = semanticSearchService.getQuestionType(request.getQueryString());
        if (!Objects.isNull(question)) {
            String type = result.getType();
            if (type.equals("5") || type.equals("6")) {
                question = result.getNewQuestion();
            }
        }
        return semanticSearchService.getTableDataEntity(question);
    }

    /**
     * 根据资源的 uri 查询是否有关联的图片，如果有返回图片地址，如果没有返回 null
     *
     * @param uri
     * @return
     */
    @RequestMapping(value = "/findImage")
    @ResponseBody
    public String findImage(String uri) {
        return semanticSearchService.findImage(uri);
    }



    //xiajl20230726  通过拼接问句获取大语言模型返回的结果字符串
    @ResponseBody
    @PostMapping("queryResultForLlmContact")
    public String queryResultForLlmContact(String questionForLlm){
        return LlmUtil.queryResultForLlmContact(questionForLlm);
    }

    /**
     * 让大模型总结问句类型2的结果
     *
     * @param request 详见实体类注释
     * @return Map
     *      answer 大模型总结后的答案
     * @author chenkun
     * @since 2023年9月28日16:29:29
     */
    @PostMapping("/queryResultForQuestion2")
    public Object queryResultForQuestion2(@RequestBody QueryResultForQuestion2Request request) {
        String answer = semanticSearchService.queryResultForQuestion2(request);
        Map<String, Object> result = new HashMap<>();
        result.put("answer", answer);
        return ResponseData.success(result);
    }

    /**
     * 让大模型总结问句类型3的结果
     *
     * @param request 详见实体类注释
     * @return Map
     *      answer 大模型总结后的答案
     * @author chenkun
     * @since 2023年9月28日16:29:29
     */
    @PostMapping("/queryResultForQuestion3")
    public Object queryResultForQuestion3(@RequestBody QueryResultForQuestion3Request request) {
        String answer = semanticSearchService.queryResultForQuestion3(request);
        Map<String, Object> result = new HashMap<>();
        result.put("answer", answer);
        return ResponseData.success(result);
    }

    /**
     * 直接由百川模型进行回答
     *
     * @author chenkun
     * @since 2023年10月7日15:56:18
     */
    @PostMapping("/queryBaichuan")
    public Object queryBaichuan(String question) {
        String answer = LlmUtil.queryBaichuan(question);
        Map<String, Object> result = new HashMap<>();
        result.put("answer", answer);
        return ResponseData.success(result);
    }

}

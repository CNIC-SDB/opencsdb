package com.linkeddata.portal.script;

import com.linkeddata.portal.entity.mongo.Dataset;
import com.linkeddata.portal.entity.mongo.Link;
import com.linkeddata.portal.service.DatasetService;
import com.linkeddata.portal.utils.RdfUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.ResultSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 计算两个数据集之间的链接数量
 *
 * @author : gaoshuai
 * @date : 2022/9/13 14:13
 */
@Slf4j
@RestController
@Api(tags = "计算数据集间关联数量")
public class CountLinks {
    @Resource
    private DatasetService datasetService;

    @Resource
    private MongoTemplate mongoTemplate;
    @Value("${sparql.endpoint}")
    private String endpoint;

    /**
     * 按领域计算数据集之间的关联关系
     */
    @ApiOperation(value = "按领域计算数据间连接数")
    @GetMapping("/countLinksByDomain")
    public void countLinks(String domain) {
        List<Dataset> datasetList = datasetService.listDatasetsByDomain(domain);
        log.info("###############{}开始执行计算数据集关联关系 ", new Date());
        for (int i = 0; i < datasetList.size(); i++) {
            String indentifier_i = datasetList.get(i).getIdentifier();
            for (int j = 0; j < datasetList.size(); j++) {
                if (i == j) {
                    continue;
                }
                String indentifier_j = datasetList.get(j).getIdentifier();
                log.info("############# 计算 {} 与 {} 之间关联数量 ##############", indentifier_i, indentifier_j);
                Dataset dataset1 = datasetList.get(i);
                Dataset dataset2 = datasetList.get(j);

                String endPoint1 = dataset1.getSparql();
                String endPoint2 = dataset2.getSparql();
                Long count = 0L;
                try {
                    count = countLinksBetweenSparql(endPoint1, endPoint2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (count > 0) {
                    log.info("###############{}更新关联关系 ", new Date());
                    updateDatasetLinks(dataset1, dataset2, count);
                }
            }
        }
        log.info("############# {} 计算数据集之间关联数量结束 ##############", new Date());
    }


    /**
     * 计算所有数据集之间的关联数量
     */
    @ApiOperation("计算所有数据集之间的关联数量")
    @GetMapping("/countAllDatasetLinks")
    public void countAllDatasetLinks() {
        List<Dataset> datasetList = datasetService.listDatasets();
        log.info("###############{}开始执行计算数据集关联关系 ", new Date());
        for (int i = 0; i < datasetList.size(); i++) {
            for (int j = 0; j < datasetList.size(); j++) {
                if (i == j) {
                    continue;
                }
                log.info("############# {} 计算 {} 与 {} 之间关联数量 ##############", new Date(), i, j);
                Dataset dataset1 = datasetList.get(i);
                Dataset dataset2 = datasetList.get(j);
                String endPoint1 = dataset1.getSparql();
                String endPoint2 = dataset2.getSparql();
                Long count = 0L;
                try {
                    count = countLinksBetweenSparql(endPoint1, endPoint2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (count > 0) {
                    log.info("###############{}更新关联关系 ", new Date());
                    updateDatasetLinks(dataset1, dataset2, count);
                }
            }
        }
        log.info("############# {} 计算数据集之间关联数量结束 ##############", new Date());
    }

    /**
     * 计算两个数据集之间的关联数量
     * dataset1 关联 dataset2 的链接数
     *
     * @param identifier1
     * @param identifier2
     */
    /**
     * 计算所有数据集之间的关联数量
     */
    @ApiOperation("计算两个数据集之间的关联数量")
    @GetMapping("/countTwoDatasetLinks")
    private Long countLinksBetweenDataset(String identifier1, String identifier2) {
        Dataset dataset1 = datasetService.getDatasetByIdentifier(identifier1);
        Dataset dataset2 = datasetService.getDatasetByIdentifier(identifier2);
        String endPoint1 = dataset1.getSparql();
        String endPoint2 = dataset2.getSparql();
        Long count = 0L;
        try {
            count = countLinksBetweenSparql(endPoint1, endPoint2);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (count > 0) {
            updateDatasetLinks(dataset1, dataset2, count);
        }
        return count;
    }


    /**
     * 计算所有数据集之间的关联数量
     */
    @ApiOperation(value = "计算一个数据集与其他数据集之间的关联数量", notes = "一个数据集与其他领域数据")
    @GetMapping("/countDatasetLinksWithOther")
    public void countDatasetLinksWithOther(String identifier, String domain) {
        List<Dataset> datasetList = datasetService.listDatasetsByDomain(domain);
        log.info("###############{}开始执行计算数据集关联关系 ", new Date());
        Dataset dataset1 = datasetService.getDatasetByIdentifier(identifier);
        String endPoint1 = dataset1.getSparql();
        for (int j = 0; j < datasetList.size(); j++) {
            Dataset dataset = datasetList.get(j);
            String identifier1 = dataset.getIdentifier();
            if (identifier.equals(identifier1)) {
                continue;
            }
            Dataset dataset2 = datasetList.get(j);
            String endPoint2 = dataset2.getSparql();
            log.info("############# {} 计算 与 {} 之间关联数量 ##############", new Date(), j);
            Long count = 0L;
            try {
                count = countLinksBetweenSparql(endPoint1, endPoint2);
            } catch (Exception e) {
                log.info(endPoint1);
                log.info(endPoint2);
                e.printStackTrace();
            }
            if (count > 0) {
                log.info("###############{}更新关联关系 ", new Date());
                updateDatasetLinks(dataset1, dataset2, count);
            }
        }
        log.info("############# {} 计算数据集之间关联数量结束 ##############", new Date());
    }

    /**
     * 计算两个sparql两个端点间的连接数，相同领域
     *
     * @param endPoint1
     * @param endPoint2
     * @return
     */
    private Long countLinksBetweenSparql(String endPoint1, String endPoint2) {
        if (endPoint1.equals(endPoint2) || StringUtils.isEmpty(endPoint1) || StringUtils.isEmpty(endPoint2)) {
            return 0L;
        }
        int i = endPoint1.lastIndexOf("http");
        String graph1 = endPoint1.substring(i);

        int i2 = endPoint2.lastIndexOf("http");
        String graph2 = endPoint2.substring(i2);

        int i1 = endPoint1.indexOf("?default");
        String sparqlPoint = endPoint1.substring(0, i1);

        Long total;
        // 端点A的宾语等于端点B的主语
        StringBuilder sparqlStr = new StringBuilder();
        sparqlStr.append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n");
        sparqlStr.append("SELECT ( COUNT(*) AS ?count ) WHERE { \n");
        sparqlStr.append(" GRAPH <");
        sparqlStr.append(graph1);
        sparqlStr.append("> { \n");
        sparqlStr.append("?s1 ?p1 ?uri . \n");
        sparqlStr.append("} \n");
        sparqlStr.append(" GRAPH <");
        sparqlStr.append(graph2);
        sparqlStr.append("> { \n");
        sparqlStr.append("?uri rdf:type ?o2 . \n");
        sparqlStr.append("} } \n");

        System.out.println("端点A的宾语等于端点B的主语##");
        System.out.println(sparqlStr.toString());

        Long count1 = RdfUtils.countTriple(sparqlPoint, sparqlStr.toString());
//        Long count1 = 0L;

       /* ResultSet resultSet = RdfUtils.queryTriple(endpoint, sparqlStr.toString());
        List<Map<String, Object>> resultMapList = RdfUtils.resultEncapsulation(resultSet);
        Map<String, Object> stringObjectMap = resultMapList.get(0);
        String count_str = stringObjectMap.get("count") + "";
        Long count1 = Long.valueOf(count_str);*/

        // 端点A的谓语等于端点B的主语
        StringBuilder sparqlStr2 = new StringBuilder();
        sparqlStr2.append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n");
        sparqlStr2.append("SELECT ( COUNT(*) AS ?count ) WHERE { \n");
        sparqlStr2.append(" GRAPH <");
        sparqlStr2.append(graph1);
        sparqlStr2.append("> { \n");
        sparqlStr2.append("  ?s1 ?uri ?o1 . \n");
        sparqlStr2.append("} \n");
        sparqlStr2.append(" GRAPH <");
        sparqlStr2.append(graph2);
        sparqlStr2.append("> { \n");
        sparqlStr2.append("?uri rdf:type ?o2 . \n");
        sparqlStr2.append("} } \n");

        System.out.println("端点A的谓语等于端点B的主语##");
        System.out.println(sparqlStr2.toString());

       /* ResultSet resultSet2 = RdfUtils.queryTriple(endpoint, sparqlStr2.toString());
        List<Map<String, Object>> resultMapList2 = RdfUtils.resultEncapsulation(resultSet2);
        Map<String, Object> stringObjectMap2 = resultMapList2.get(0);
        String count_str2 = stringObjectMap2.get("count") + "";
        Long count2 = Long.valueOf(count_str2);*/

        Long count2 = RdfUtils.countTriple(sparqlPoint, sparqlStr2.toString());
//        Long count2 = 0L;


        // 端点A的主语等于端点B的主语
        StringBuilder sparqlStr3 = new StringBuilder();
        sparqlStr3.append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n");
        sparqlStr3.append("SELECT ( COUNT(*) AS ?count ) WHERE { \n");
        sparqlStr3.append(" GRAPH <");
        sparqlStr3.append(graph1);
        sparqlStr3.append("> { \n");
        sparqlStr3.append("  ?s1 ?p1 ?o1 . \n");
        sparqlStr3.append("} \n");
        sparqlStr3.append(" GRAPH <");
        sparqlStr3.append(graph2);
        sparqlStr3.append("> { \n");
        sparqlStr3.append(" ?s1 rdf:type ?o2 . \n");
        sparqlStr3.append("} } \n");

        System.out.println("端点A的主语等于端点B的主语##");
        System.out.println(sparqlStr3.toString());

       /* ResultSet resultSet3 = RdfUtils.queryTriple(endpoint, sparqlStr3.toString());
        List<Map<String, Object>> resultMapList3 = RdfUtils.resultEncapsulation(resultSet3);
        Map<String, Object> stringObjectMap3 = resultMapList3.get(0);
        String count_str3 = stringObjectMap3.get("count") + "";
        Long count3 = Long.valueOf(count_str3);*/

        Long count3 = RdfUtils.countTriple(sparqlPoint, sparqlStr3.toString());
//        Long count3 = 0L;

        total = count1 + count2 + count3;
        return total;
    }


    /**
     * 跨领域查询数据集关联关系备份
     *
     * @param endPoint1
     * @param endPoint2
     * @return
     */
    @Deprecated
    private Long countLinksBetweenSparql_bak(String endPoint1, String endPoint2) {
        if (endPoint1.equals(endPoint2) || StringUtils.isEmpty(endPoint1) || StringUtils.isEmpty(endPoint2)) {
            return 0L;
        }

        Long total;
        // 端点A的宾语等于端点B的主语
        StringBuilder sparqlStr = new StringBuilder();
        sparqlStr.append("SELECT ( COUNT(*) AS ?count ) WHERE { \n");
        sparqlStr.append("SERVICE SILENT <");
        sparqlStr.append(endPoint1);
        sparqlStr.append("> { \n");
        sparqlStr.append("?s1 ?p1 ?uri . \n");
        sparqlStr.append("} \n");
        sparqlStr.append(" SERVICE SILENT <");
        sparqlStr.append(endPoint2);
        sparqlStr.append("> { \n");
        sparqlStr.append("?uri ?p2 ?o2 . \n");
        sparqlStr.append("} } \n");

 /*       System.out.println("端点A的宾语等于端点B的主语##");
        System.out.println(sparqlStr.toString());*/

        ResultSet resultSet = RdfUtils.queryTriple(endpoint, sparqlStr.toString());
        List<Map<String, Object>> resultMapList = RdfUtils.resultEncapsulation(resultSet);
        Map<String, Object> stringObjectMap = resultMapList.get(0);
        String count_str = stringObjectMap.get("count") + "";
        Long count1 = Long.valueOf(count_str);

        // 端点A的谓语等于端点B的主语
        StringBuilder sparqlStr2 = new StringBuilder();
        sparqlStr2.append("SELECT ( COUNT(*) AS ?count ) WHERE { \n");
        sparqlStr2.append("SERVICE SILENT <");
        sparqlStr2.append(endPoint1);
        sparqlStr2.append("> { \n");
        sparqlStr2.append("  ?s1 ?uri ?o1 . \n");
        sparqlStr2.append("} \n");
        sparqlStr2.append(" SERVICE SILENT <");
        sparqlStr2.append(endPoint2);
        sparqlStr2.append("> { \n");
        sparqlStr2.append("?uri ?p2 ?o2 . \n");
        sparqlStr2.append("} } \n");

       /* System.out.println("端点A的谓语等于端点B的主语##");
        System.out.println(sparqlStr.toString());*/

        ResultSet resultSet2 = RdfUtils.queryTriple(endpoint, sparqlStr2.toString());
        List<Map<String, Object>> resultMapList2 = RdfUtils.resultEncapsulation(resultSet2);
        Map<String, Object> stringObjectMap2 = resultMapList2.get(0);
        String count_str2 = stringObjectMap2.get("count") + "";
        Long count2 = Long.valueOf(count_str2);


        // 端点A的主语等于端点B的主语
        StringBuilder sparqlStr3 = new StringBuilder();
        sparqlStr3.append("SELECT ( COUNT(*) AS ?count ) WHERE { \n");
        sparqlStr3.append("SERVICE SILENT <");
        sparqlStr3.append(endPoint1);
        sparqlStr3.append("> { \n");
        sparqlStr3.append("  ?s1 ?p1 ?o1 . \n");
        sparqlStr3.append("} \n");
        sparqlStr3.append(" SERVICE SILENT <");
        sparqlStr3.append(endPoint2);
        sparqlStr3.append("> { \n");
        sparqlStr3.append(" ?s1 ?p2 ?o2 . \n");
        sparqlStr3.append("} } \n");

      /*  System.out.println("端点A的主语等于端点B的主语##");
        System.out.println(sparqlStr.toString());*/

        ResultSet resultSet3 = RdfUtils.queryTriple(endpoint, sparqlStr3.toString());
        List<Map<String, Object>> resultMapList3 = RdfUtils.resultEncapsulation(resultSet3);
        Map<String, Object> stringObjectMap3 = resultMapList3.get(0);
        String count_str3 = stringObjectMap3.get("count") + "";
        Long count3 = Long.valueOf(count_str3);

        total = count1 + count2 + count3;
        return total;
    }

    /**
     * 更新数据集关联其他数据集的连接数
     *
     * @param dataset1 要更新的数据集
     * @param dataset2 关联的数据集
     * @param count    dataset1 与2 关联的链接数量
     */
    private void updateDatasetLinks(Dataset dataset1, Dataset dataset2, Long count) {
        // dataset1关联的dataset2的标识符
        String identifier = dataset2.getIdentifier();
        // dataset1的所有关联关系
        List<Link> links = dataset1.getLinks();
        // 存放dataset1原来关联的数据集的标识符
        List<String> identifierList = new ArrayList<>();
        // 遍历dataset1的关联关系，如果数据集原Links关联了该数据集，就更新
        for (Link link1 : links) {
            identifierList.add(link1.getTarget());
            if (identifier.equals(link1.getTarget())) {
                link1.setValue(count.toString());
            }
        }
        if (!identifierList.contains(identifier)) {
            Link link = new Link();
            link.setTarget(identifier);
            link.setValue(count.toString());
            link.setTargetTriples(dataset2.getTriples());
            links.add(link);
        }
        dataset1.setLinks(links);
        Dataset save = mongoTemplate.save(dataset1);
    }
}

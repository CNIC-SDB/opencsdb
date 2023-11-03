package com.linkeddata.portal.script;

import com.linkeddata.portal.entity.mongo.Dataset;
import com.linkeddata.portal.service.DatasetService;
import com.linkeddata.portal.utils.RdfUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;


/**
 * 计算数据集三元组数量
 *
 * @author : gaoshuai
 * @date : 2022/10/28 10:48
 */
@RestController
public class CountTriples {

    @Resource
    private MongoTemplate mongoTemplate;
    @Resource
    private DatasetService datasetService;

    /**
     * 更新一个数据集的triples
     *
     * @param identifier
     */
    @GetMapping("updateDatasetTriple")
    public void updateDatasetTriple(String identifier) {
        countTripleAndUpdate(identifier);
    }

    /**
     * 更新某个领域内每个数据集的三元组数量
     *
     * @param domain
     */
    @GetMapping("updateDatasetTripleByDomain")
    public void updateDatasetTripleByDomain(String domain) {
        List<Dataset> datasetList = datasetService.listDatasetsByDomain(domain);
        for (Dataset dataset : datasetList) {
            String identifier = dataset.getIdentifier();
            countTripleAndUpdate(identifier);
        }
    }

    /**
     * 更新所有数据集的三元组数量
     */
    @GetMapping("updateAllDatasetTriple")
    public void updateAllDatasetTriple() {
        List<Dataset> datasetList = datasetService.listDatasets();
        for (Dataset dataset : datasetList) {
            String identifier = dataset.getIdentifier();
            countTripleAndUpdate(identifier);
        }
    }

    private String countTripleAndUpdate(String identifier) {
        Dataset dataset = datasetService.getDatasetByIdentifier(identifier);
        String sparql = dataset.getSparql();
        Long count = RdfUtils.countTriple(sparql, "select (count(*) as ?count )  where {   ?s ?p ?o.} ");
        dataset.setTriples(count);
        Dataset save = mongoTemplate.save(dataset);
        return null;
    }

}



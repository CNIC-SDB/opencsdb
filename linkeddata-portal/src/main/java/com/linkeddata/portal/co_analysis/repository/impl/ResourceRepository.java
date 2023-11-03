package com.linkeddata.portal.co_analysis.repository.impl;

import com.linkeddata.portal.co_analysis.repository.ResourceDao;
import com.linkeddata.portal.entity.mongo.Dataset;
import com.linkeddata.portal.entity.piflowDo.PiflowDatacenter;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author : gaoshuai
 * @date : 2022/12/23 17:48
 */
@Repository
public class ResourceRepository implements ResourceDao {

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public String getDataCenterId(String piflowDataCenterId) {
        Query query = new Query();
        PiflowDatacenter piflowDatacenter = mongoTemplate.findOne(query.addCriteria(Criteria.where("piflow_datacenter_id").is(piflowDataCenterId)), PiflowDatacenter.class);
        String datacenterId = piflowDatacenter.getDatacenterId();
        return datacenterId;
    }

    @Override
    public List<Dataset> listDatasetLikeSparql(String sparql) {
        Query query = new Query();
        Pattern pattern = Pattern.compile("^.*" + sparql + ".*$", Pattern.CASE_INSENSITIVE);
        query.addCriteria(Criteria.where("sparql").regex(pattern));
        List<Dataset> datasetList = mongoTemplate.find(query, Dataset.class);
        return datasetList;
    }

    @Override
    public PiflowDatacenter getDataCenterById(String id) {
        Query query = new Query();
        return mongoTemplate.findOne(query.addCriteria(Criteria.where("datacenter_id").is(id)), PiflowDatacenter.class);
    }
}

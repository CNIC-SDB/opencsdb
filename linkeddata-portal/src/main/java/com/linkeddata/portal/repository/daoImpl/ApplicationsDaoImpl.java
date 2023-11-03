package com.linkeddata.portal.repository.daoImpl;

import com.linkeddata.portal.entity.PageTools;
import com.linkeddata.portal.entity.mongo.ApplicationDetail;
import com.linkeddata.portal.entity.mongo.Applications;
import com.linkeddata.portal.entity.mongo.Dataset;
import com.linkeddata.portal.entity.resource.ResourceListRequest;
import com.linkeddata.portal.repository.ApplicationsDao;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author hanmenghang
 * @date : 2022/11/02
 */
@Repository
public class ApplicationsDaoImpl implements ApplicationsDao {
    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public PageTools<List<Applications>> findApplications(ResourceListRequest request) {
        //校验下
        if((request.getPageNum() <= 0) || (request.getPageSize() <= 0)){
            return null;
        }
        Query query = new Query()
                .skip((long)(request.getPageNum() - 1) * request.getPageSize())
                .limit(request.getPageSize());
        List<Applications> applicationsList = mongoTemplate.find(query,Applications.class);
        PageTools<List<Applications>> page = null;
        page = new PageTools<>(request.getPageSize(), applicationsList.size(), request.getPageNum(), applicationsList);
        return page;
    }
    @Override
    public ApplicationDetail getAppByDataCenterId(ResourceListRequest request) {
        //校验下
        if((null == request.getDatacenterId()) || ("".equals(request.getDatacenterId()))){
            return null;
        }
        Pattern pattern = Pattern.compile("^.*" + request.getDatacenterId() + ".*$",Pattern.CASE_INSENSITIVE);
        Criteria criteria = Criteria.where("title_url").is(pattern);
        Query query = new Query();
        query.addCriteria(criteria);
        ApplicationDetail applicationDetail= mongoTemplate.findOne(query,ApplicationDetail.class);
        if(null != applicationDetail){
            Criteria criteriaForDataset = Criteria.where("datacenter_id").is(request.getDatacenterId());
            Query queryForDataset = new Query();
            queryForDataset.addCriteria(criteriaForDataset);
            Dataset dataset = mongoTemplate.findOne(queryForDataset, Dataset.class);
            if (null != dataset) {
                applicationDetail.setEmail(dataset.getContactEmail());
                applicationDetail.setPerson(dataset.getContactName());
            }
        }
        return applicationDetail;
    }


    @Override
    public Long countRecords() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.group("null")
                        .sum("records").as("sum")
        );
        AggregationResults<Map> aggregate = mongoTemplate.aggregate(aggregation, Applications.class, Map.class);
        List<Map> mappedResults = aggregate.getMappedResults();
        Map map = mappedResults.get(0);
        if (map != null) {
            Object sum = map.get("sum");
            long l = Long.parseLong(sum.toString());
            return l;
        } else {
            return 0L;
        }
    }


    @Override
    public Long countRecords(String datacenterId) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("datacenterId").is(datacenterId)),
                Aggregation.group("null")
                        .sum("records").as("sum")
        );
        AggregationResults<Map> aggregate = mongoTemplate.aggregate(aggregation, Applications.class, Map.class);
        List<Map> mappedResults = aggregate.getMappedResults();
        if (!mappedResults.isEmpty()) {
            Map map = mappedResults.get(0);
            Object sum = map.get("sum");
            long l = Long.parseLong(sum.toString());
            return l;
        } else {
            return 0L;
        }
    }
}

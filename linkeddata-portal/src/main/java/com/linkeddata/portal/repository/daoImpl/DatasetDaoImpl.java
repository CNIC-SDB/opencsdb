package com.linkeddata.portal.repository.daoImpl;

import com.linkeddata.portal.entity.DataSetRequest;
import com.linkeddata.portal.entity.PageTools;
import com.linkeddata.portal.entity.mongo.DataDownload;
import com.linkeddata.portal.entity.mongo.DataView;
import com.linkeddata.portal.entity.mongo.Dataset;
import com.linkeddata.portal.repository.DatasetDao;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.query.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 数据集相关查询接口实现类
 *
 * @author : gaoshuai
 * @date : 2022/9/9 9:59
 */
@Repository
public class DatasetDaoImpl implements DatasetDao {
    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public List<Dataset> listDatasets() {
        return mongoTemplate.findAll(Dataset.class);
    }

    @Override
    public List<Dataset> listShowDatasets() {
        Query query = new Query();
        return mongoTemplate.find(query.addCriteria(Criteria.where("showFlag").is(true)), Dataset.class);
    }

    @Override
    public List<Dataset> listDatasetsByDomain(String domain) {
        Query query = new Query();
        return mongoTemplate.find(query.addCriteria(Criteria.where("domain").is(domain)), Dataset.class);
    }

    @Override
    public List<Dataset> listDatasetsByDatacenterId(String datacenterId) {
        Query query = new Query();
        return mongoTemplate.find(query.addCriteria(Criteria.where("datacenter_id").is(datacenterId)), Dataset.class);
    }

    @Override
    public PageTools<List<Dataset>> getDatasetList(DataSetRequest request) {

        if (null != request.getDomain() || null != request.getInstitution()) {
            if ((request.getDomain().length == 0) && (request.getInstitution().length == 0) && StringUtils.isBlank(request.getDatacenterId())) {
                return null;
            }
        }
        //查询条件
        Query filterQuery = new Query();
        //查询总数
        Query countQuery = new Query();
        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = new ArrayList<>();
        if (null != request.getDomain() && request.getDomain().length > 0) {
            for (String domain : request.getDomain()) {
                criteriaList.add(Criteria.where("domain").is(domain));
            }
        }
        if (null != request.getInstitution() && request.getInstitution().length > 0) {
            for (String institution : request.getInstitution()) {
                criteriaList.add(Criteria.where("unitName").is(institution));
            }
        }
        //根据 datacenterID 不为空时 查询数据 application 和 列表区分开  add by wangzhiliang
        if (StringUtils.isNotBlank(request.getDatacenterId())) {
            criteriaList.add(Criteria.where("datacenterId").is(request.getDatacenterId()));
        }
        if (StringUtils.isNotBlank(request.getCondition())) {
            criteriaList.add(Criteria.where("title").regex(request.getCondition()));
        }
        if (criteriaList.size() > 0) {
            criteria.orOperator(criteriaList.toArray(new Criteria[0]));
            filterQuery.addCriteria(criteria);
            countQuery.addCriteria(criteria);
        }
        filterQuery.skip((request.getPageNum() - 1) * request.getPageSize());
        filterQuery.limit(request.getPageSize());
        long count = mongoTemplate.count(countQuery, Dataset.class);
        List<Dataset> datasets = mongoTemplate.find(filterQuery, Dataset.class);
        //分页信息返回
        return new PageTools<>(request.getPageSize(), (int) count, request.getPageNum(), datasets);
    }

    @Override
    public Dataset getDatasetById(String id) {
        return mongoTemplate.findById(id, Dataset.class);
    }

    @Override
    public void downloadOneDataset(String id) {

    }

    @Override
    public void downloadDatasetsByIds(List<String> idList) {

    }

    @Override
    public List<String> listInstitutions() {
        List<String> unitNameList = mongoTemplate.findDistinct("unit_name", Dataset.class, String.class);
        return unitNameList;
    }

    @Override
    public List<String> listDomains() {
        return mongoTemplate.findDistinct("domain", Dataset.class, String.class);
    }

    @Override
    public Dataset getDatasetByIdentifier(String identifier) {
        Query query = new Query();
        return mongoTemplate.findOne(query.addCriteria(Criteria.where("identifier").is(identifier)), Dataset.class);
    }

    @Override
    public Dataset getDatasetBySparql(String sparql) {
        Query query = new Query();
        return mongoTemplate.findOne(query.addCriteria(Criteria.where("sparql").is(sparql)), Dataset.class);
    }

    @Override
    public Integer countDataset() {
        Query query = new Query();
        Long count = mongoTemplate.count(query, Dataset.class);
        return count.intValue();
    }

    @Override
    public Long countTriples() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.group("null")
                        .sum("triples").as("sum")
        );
        AggregationResults<Map> aggregate = mongoTemplate.aggregate(aggregation, Dataset.class, Map.class);
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
    public Long dataVolume() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.group("null")
                        .sum("data_volume").as("volume")
        );
        AggregationResults<Map> aggregate = mongoTemplate.aggregate(aggregation, Dataset.class, Map.class);
        List<Map> mappedResults = aggregate.getMappedResults();
        Map map = mappedResults.get(0);
        if (map != null) {
            Object volume = map.get("volume");
            long l = Long.parseLong(volume.toString());
            return l;
        } else {
            return 0L;
        }
    }

    @Override
    public Long countView() {
        Query query = new Query();
        Long count = mongoTemplate.count(query, DataView.class);
        return count;
    }

    @Override
    public Long countDataDownload() {
        Query query = new Query();
        Long count = mongoTemplate.count(query, DataDownload.class);
        return count;
    }

    @Override
    public Long countView(String datacenterId) {
        Query query = new Query();
        Long count = mongoTemplate.count(query.addCriteria(Criteria.where("datacenterId").is(datacenterId)), DataView.class);
        return count;
    }

    @Override
    public Long countDataDownload(String datacenterId) {
        Query query = new Query();
        Long count = mongoTemplate.count(query.addCriteria(Criteria.where("datacenterId").is(datacenterId)), DataDownload.class);
        return count;
    }

    @Override
    public Integer countDataset(String datacenterId) {
        Query query = new Query();
        Long count = mongoTemplate.count(query.addCriteria(Criteria.where("datacenterId").is(datacenterId)), Dataset.class);
        return count.intValue();
    }

    @Override
    public Long countTriples(String datacenterId) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("datacenterId").is(datacenterId)),
                Aggregation.group("null")
                        .sum("triples").as("sum")
        );
        AggregationResults<Map> aggregate = mongoTemplate.aggregate(aggregation, Dataset.class, Map.class);
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

    @Override
    public Long dataVolume(String datacenterId) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("datacenterId").is(datacenterId)),
                Aggregation.group("null")
                        .sum("data_volume").as("volume")
        );
        AggregationResults<Map> aggregate = mongoTemplate.aggregate(aggregation, Dataset.class, Map.class);
        List<Map> mappedResults = aggregate.getMappedResults();
        if (!mappedResults.isEmpty()) {
            Map map = mappedResults.get(0);
            Object volume = map.get("volume");
            long l = Long.parseLong(volume.toString());
            return l;
        } else {
            return 0L;
        }
    }


    @Override
    public String getIdentifierByGraph(String graph) {
        Query query = new Query();
        String pattern_name = graph;
        Pattern pattern = Pattern.compile("^.*" + pattern_name + ".*$", Pattern.CASE_INSENSITIVE);
        query.addCriteria(Criteria.where("identifier").regex(pattern));

        Dataset dataset = mongoTemplate.findOne(query, Dataset.class);
        if (null != dataset) {
            return dataset.getIdentifier();
        }
        return "";
    }

    @Override
    public List<Dataset> listDatasetsPageByTitleAndDatacenterId(String title, String datacenterId, Integer pageNum, Integer pageSize){
        Query query = new Query();
        Pattern pattern;
        if (StringUtils.isNotEmpty(title)){
            pattern = Pattern.compile("^.*" + title + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where("title").regex(pattern));
        }
        if (StringUtils.isNotEmpty(datacenterId)){
            query.addCriteria(Criteria.where("datacenter_id").is(datacenterId));
        }
        query.addCriteria(Criteria.where("hdfsPath").ne(null));
        query.skip((long) (pageNum - 1) * pageSize).limit(pageSize);
        Sort.Order order = new Sort.Order(Sort.Direction.DESC,"publish_time");
        List<Sort.Order>  orders = new ArrayList<>();
        orders.add(order);
        query.with(Sort.by(orders));
        return mongoTemplate.find(query,Dataset.class);
    }


    @Override
    public Integer countDatasetsByTitleAndDatacenterId(String title, String datacenterId){
        Query query = new Query();
        Pattern pattern;
        if (StringUtils.isNotEmpty(title)){
            pattern = Pattern.compile("^.*" + title + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where("title").regex(pattern));
        }
        if (StringUtils.isNotEmpty(datacenterId)){
            query.addCriteria(Criteria.where("datacenter_id").is(datacenterId));
        }
        query.addCriteria(Criteria.where("hdfsPath").ne(null));
        List<Dataset> list = mongoTemplate.find(query,Dataset.class);
        return list.size();
    }
}

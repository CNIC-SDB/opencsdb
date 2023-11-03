package com.linkeddata.portal.repository.daoImpl;

import com.linkeddata.portal.entity.PageTools;
import com.linkeddata.portal.entity.mongo.Applications;
import com.linkeddata.portal.entity.mongo.Specifications;
import com.linkeddata.portal.entity.resource.ResourceListRequest;
import com.linkeddata.portal.repository.SpecificationsDao;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author hanmenghang
 * @date : 2022/11/02
 */
@Repository
public class SpecificationsDaoImpl implements SpecificationsDao {
    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public PageTools<List<Specifications>> findSpecifications(ResourceListRequest request) {
        //校验下
        if((request.getPageNum() <= 0) ||  (request.getPageSize() <= 0)){
            return null;
        }
        Query query = new Query()
                .skip((long)(request.getPageNum() - 1) * request.getPageSize())
                .limit(request.getPageSize());
        List<Specifications> applicationsList = mongoTemplate.find(query,Specifications.class);
        PageTools<List<Specifications>> page = null;
        page = new PageTools<>(request.getPageSize(), applicationsList.size(), request.getPageNum(), applicationsList);
        return page;
    }
}

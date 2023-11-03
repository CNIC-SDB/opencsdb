package com.linkeddata.portal.repository.daoImpl;

import com.linkeddata.portal.entity.mongo.EntityClass;
import com.linkeddata.portal.repository.EntityClassDao;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

/**
 * @auhor xiajl
 * @date 2023/4/26 16:38
 */
@Repository
public class EntityClassDaoImpl implements EntityClassDao {
    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public List<EntityClass> findByLabel(String label) {
        Query query = new Query();
        return mongoTemplate.find(query.addCriteria(Criteria.where("label").is(label)), EntityClass.class);
    }

    @Override
    public EntityClass findByUri(String uri) {
        Query query = new Query();
        return mongoTemplate.findOne(query.addCriteria(Criteria.where("uri").is(uri)), EntityClass.class);
    }
}

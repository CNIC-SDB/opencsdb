package com.linkeddata.portal.repository.daoImpl;

import com.linkeddata.portal.entity.mongo.DataView;
import com.linkeddata.portal.repository.DataViewDao;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 浏览量服务层
 *
 * @author : gaoshuai
 * @date : 2022/10/25 11:10
 */
@Service
public class DataViewDaoImpl implements DataViewDao {
    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public void saveViewCount(String ip, String url, String datacenterId) {
        DataView dataView = new DataView();
        dataView.setIp(ip);
        dataView.setUrl(url);
        dataView.setTime(new Date());
        dataView.setDatacenterId(datacenterId);
        DataView save = mongoTemplate.save(dataView);
    }
}

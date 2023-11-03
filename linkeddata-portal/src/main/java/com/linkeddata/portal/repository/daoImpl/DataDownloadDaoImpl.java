package com.linkeddata.portal.repository.daoImpl;

import com.linkeddata.portal.entity.mongo.DataDownload;
import com.linkeddata.portal.repository.DataDownloadDao;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author : gaoshuai
 * @date : 2022/10/25 14:51
 */
@Service
public class DataDownloadDaoImpl implements DataDownloadDao {
    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public void addDownLoadCount(String ip, String url, String fileType, String datacenterId) {
        DataDownload dataDownload = new DataDownload();
        dataDownload.setIp(ip);
        dataDownload.setUrl(url);
        dataDownload.setFileType(fileType);
        dataDownload.setDatacenterId(datacenterId);
        dataDownload.setTime(new Date());
        mongoTemplate.save(dataDownload);
    }
}

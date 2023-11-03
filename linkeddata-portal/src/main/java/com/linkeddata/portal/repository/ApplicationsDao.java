package com.linkeddata.portal.repository;

import com.linkeddata.portal.entity.PageTools;
import com.linkeddata.portal.entity.mongo.ApplicationDetail;
import com.linkeddata.portal.entity.mongo.Applications;
import com.linkeddata.portal.entity.resource.ResourceListRequest;

import java.util.List;

/**
 * @author hanmenghang
 * @date : 2022/11/02
 */
public interface ApplicationsDao {
    PageTools<List<Applications>> findApplications(ResourceListRequest request);

    ApplicationDetail getAppByDataCenterId(ResourceListRequest request);

    /**
     * 按数据中心统计资源实体（主语）数量
     *
     * @return
     */
    Long countRecords(String datacenterId);

    /**
     * 统计资源实体（主语）数量
     *
     * @return
     */
    Long countRecords();
}

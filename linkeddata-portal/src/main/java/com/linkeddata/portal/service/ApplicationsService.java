package com.linkeddata.portal.service;

import com.linkeddata.portal.entity.PageTools;
import com.linkeddata.portal.entity.mongo.ApplicationDetail;
import com.linkeddata.portal.entity.mongo.Applications;
import com.linkeddata.portal.entity.resource.ResourceListRequest;

import java.util.List;

/**
 * @author hanmenghang
 * @date : 2022/11/02
 */
public interface ApplicationsService {
    PageTools<List<Applications>> findApplications(ResourceListRequest request);
    ApplicationDetail getAppByDataCenterId(ResourceListRequest request);
}

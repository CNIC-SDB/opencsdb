package com.linkeddata.portal.service.impl;

import com.linkeddata.portal.entity.PageTools;
import com.linkeddata.portal.entity.mongo.ApplicationDetail;
import com.linkeddata.portal.entity.mongo.Applications;
import com.linkeddata.portal.entity.resource.ResourceListRequest;
import com.linkeddata.portal.repository.ApplicationsDao;
import com.linkeddata.portal.service.ApplicationsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author hanmenghang
 * @date : 2022/11/02
 */
@Service
public class ApplicationsServiceImpl implements ApplicationsService {
    @Autowired
    private ApplicationsDao applicationsDao;

    @Override
    public PageTools<List<Applications>> findApplications(ResourceListRequest request) {
        return applicationsDao.findApplications(request);
    }
    @Override
    public ApplicationDetail getAppByDataCenterId(ResourceListRequest request) {
        return applicationsDao.getAppByDataCenterId(request);
    }
}

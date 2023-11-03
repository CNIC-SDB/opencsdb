package com.linkeddata.portal.service.impl;

import com.linkeddata.portal.entity.PageTools;
import com.linkeddata.portal.entity.mongo.Specifications;
import com.linkeddata.portal.entity.resource.ResourceListRequest;
import com.linkeddata.portal.repository.SpecificationsDao;
import com.linkeddata.portal.service.SpecificationsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author hanmenghang
 * @date : 2022/11/02
 */
@Service
public class SpecificationsServiceImpl implements SpecificationsService {
    @Autowired
    private SpecificationsDao specificationsDao;
    @Override
    public PageTools<List<Specifications>> findSpecifications(ResourceListRequest request) {
        return specificationsDao.findSpecifications(request);
    }
}

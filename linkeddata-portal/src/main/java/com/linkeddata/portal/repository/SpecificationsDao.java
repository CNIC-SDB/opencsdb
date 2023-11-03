package com.linkeddata.portal.repository;

import com.linkeddata.portal.entity.PageTools;
import com.linkeddata.portal.entity.mongo.Specifications;
import com.linkeddata.portal.entity.resource.ResourceListRequest;

import java.util.List;

/**
 * @author hanmenghang
 * @date : 2022/11/02
 */
public interface SpecificationsDao {
    PageTools<List<Specifications>> findSpecifications(ResourceListRequest request);
}

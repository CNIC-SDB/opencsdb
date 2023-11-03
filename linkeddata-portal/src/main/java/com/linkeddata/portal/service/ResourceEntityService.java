package com.linkeddata.portal.service;

import com.linkeddata.portal.entity.ExportRequest;
import com.linkeddata.portal.entity.PageTools;
import com.linkeddata.portal.entity.resource.ResourceDetail;
import com.linkeddata.portal.entity.resource.ResourceDetailRequest;
import com.linkeddata.portal.entity.resource.ResourceList;
import com.linkeddata.portal.entity.resource.ResourceListRequest;

import java.util.List;
import java.util.Map;

/**
 * 资源实体 service 层
 *
 * @author wangzhiliang
 */
public interface ResourceEntityService {
    /**
     * 资源信息列表
     *
     * @param request 请求参数
     * @return List<ResourceList> 资源信息集合
     * @author wangzhiliang
     * @date 20220908
     */
    PageTools<List<ResourceList>> getResourceList(ResourceListRequest request);
    /**
     * 资源信息列表
     *
     * @param request 请求参数
     * @return List<ResourceList> 资源信息集合
     * @author hmh
     * @date 20221020
     */
    PageTools<List<ResourceList>> getResourceListByES(ResourceListRequest request);

    /**
     * 资源信息列表
     *
     * @param request 请求参数实体
     * @return ResourceDetail 资源信息集合
     * @author wangzhiliang
     * @date 20220908
     */
    ResourceDetail getResourceDetail(ResourceDetailRequest request);

    /**
     * 查询资源相关节点信息
     *
     * @param iri 资源iri
     * @return
     */
    Map listRelationResource(String iri);

    /**
     * 导出三元组成为文件
     * @author wangzhiliang
     * @date 20220930
     * */
    String exportToFileByGraph(ExportRequest request);
}

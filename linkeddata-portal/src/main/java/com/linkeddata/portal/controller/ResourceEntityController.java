package com.linkeddata.portal.controller;

import com.linkeddata.portal.entity.ExportRequest;
import com.linkeddata.portal.entity.PageTools;
import com.linkeddata.portal.entity.ResponseData;
import com.linkeddata.portal.entity.resource.ResourceDetail;
import com.linkeddata.portal.entity.resource.ResourceDetailRequest;
import com.linkeddata.portal.entity.resource.ResourceList;
import com.linkeddata.portal.entity.resource.ResourceListRequest;
import com.linkeddata.portal.service.ResourceEntityService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 资源实体controller
 *
 * @author wangzhiliang
 */
@RestController
@RequestMapping("/resource")
@Api(tags = "资源实体")
public class ResourceEntityController {
    @Autowired
    private ResourceEntityService resourceEntityService;

    @ApiOperation("获取资源列表")
    @PostMapping("/list")
    @ResponseBody
    public ResponseData<PageTools<List<ResourceList>>> getResourceList(@RequestBody ResourceListRequest request) {
        return ResponseData.success(resourceEntityService.getResourceList(request));
    }

    @ApiOperation("通过ES获取资源列表")
    @PostMapping("/listByES")
    @ResponseBody
    public ResponseData<PageTools<List<ResourceList>>> getResourceListByES(@RequestBody ResourceListRequest request) {
        return ResponseData.success(resourceEntityService.getResourceListByES(request));
    }

    @ApiOperation("获取资源详情")
    @PostMapping("/detail")
    @ResponseBody
    public ResponseData<ResourceDetail> getResourceDetail(@RequestBody ResourceDetailRequest request) {
        return ResponseData.success(resourceEntityService.getResourceDetail(request));
    }

    /**
     * 查询资源相关联的三元组信息，包括所属sparql端点，所属数据集名称
     *
     * @param iri 资源iri
     * @return
     */
    @ApiOperation("查询资源相关联的三元组信息，包括所属sparql端点，所属数据集名称")
    @PostMapping("/listRelationResource")
    public ResponseData<Map> listRelationResource(String iri) {
        Map map = resourceEntityService.listRelationResource(iri);
        return ResponseData.success(map);
    }

    /**
     * 导出三元组成为文件
     * @author wangzhiliang
     * @date 20220930
     * */
    @ApiOperation("导出三元组成为文件")
    @PostMapping("/exportToFileByGraph")
    @ResponseBody
    public  ResponseData<String> exportToFileByGraph(@RequestBody ExportRequest request){
        return ResponseData.success(resourceEntityService.exportToFileByGraph(request));
    }

}

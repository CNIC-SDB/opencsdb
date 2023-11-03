package com.linkeddata.portal.controller;

import com.linkeddata.portal.entity.DataSetRequest;
import com.linkeddata.portal.entity.PageTools;
import com.linkeddata.portal.entity.ResponseData;
import com.linkeddata.portal.entity.mongo.Dataset;
import com.linkeddata.portal.service.impl.DatasetServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;


/**
 * @author : gaoshuai
 * @date : 2022/9/9 10:54
 */
@RestController
@RequestMapping("/dataset")
@Api(tags = "数据集相关接口")
public class DatasetController {
    @Resource
    private DatasetServiceImpl datasetService;


    /**
     * 获取数据集详情页面
     *
     * @author wangzhiliang
     * @date 20220919
     */
    @ApiOperation("获取数据集列表")
    @PostMapping("/getDatasetList")
    @ResponseBody
    public ResponseData<PageTools<List<Dataset>>> getDatasetList(@RequestBody DataSetRequest request) {
        return ResponseData.success(datasetService.getDatasetList(request));
    }

    /**
     * 根据数据集id查询数据集详情
     *
     * @param id 数据集id
     * @return
     */
    @ApiOperation("根据数据集id查询数据集详情")
    @GetMapping("/getDatasetById")
    public ResponseData<Dataset> getDatasetById(@ApiParam("数据集id") String id) {
        return ResponseData.success(datasetService.getDatasetById(id));
    }

    /**
     * 根据数据集标识符查询数据集详情
     *
     * @param identifier 数据集identifier
     * @return
     */
    @ApiOperation("根据数据集identifier查询数据集详情")
    @GetMapping("/getDatasetByIdentifier")
    public ResponseData<Dataset> getDatasetByIdentifier(@ApiParam("数据集标识符") String identifier) {
        return ResponseData.success(datasetService.getDatasetByIdentifier(identifier));
    }


    /**
     * 根据id下载一个数据集
     *
     * @param id 数据集id
     */
    @ApiOperation("根据id下载一个数据集")
    @GetMapping("/downloadOneDataset")
    public void downloadOneDataset(@ApiParam("数据集id") String id) {
        datasetService.downloadOneDataset(id);
    }

    /**
     * 根据id列表下载多个数据集
     *
     * @param ids 数据集id列表
     */
    @ApiOperation("根据id列表下载多个数据集")
    @GetMapping("/downloadDatasetsByIds")
    public void downloadDatasetsByIds(@ApiParam("数据集id列表") List<String> ids) {
        datasetService.downloadDatasetsByIds(ids);
    }

    /**
     * 查询所有的数据集领域
     *
     * @return
     */
    @ApiOperation("查询所有的数据集领域")
    @GetMapping("/listDomains")
    public List<String> listDomains() {
        return datasetService.listDomains();
    }

    /**
     * 查询所有机构
     *
     * @return
     */
    @ApiOperation("查询所有机构")
    @GetMapping("/listInstitutions")
    public List<String> listInstitutions() {
        return datasetService.listInstitutions();
    }


    /**
     * 列出与该数据集相关的数据集id、名称
     *
     * @param identifier 该数据集identifier
     * @return
     */
    @ApiOperation("列出与该数据集相关的数据集信息")
    @GetMapping("/listRelationDataset")
    public Map listRelationDataset(String identifier) {
        return datasetService.listRelationDataset(identifier);
    }

    /**
     * 门户首页列出所有数据集之间的关联关系
     *
     * @return
     */
    @ApiOperation("列出所有数据集之间关联关系")
    @GetMapping("/listAllDatasetRelation")
    public Map listAllDatasetRelation(String datacenterId) {
        Map map = datasetService.listAllDatasetRelation(datacenterId);
        return map;
    }


    /**
     * 首页的统计信息
     *
     * @return
     */
    @ApiOperation("首页统计信息")
    @GetMapping("/statisticsInfo")
    public Map statisticsInfo(String datacenterId) {
        Map map = datasetService.getStatisticsInfo(datacenterId);
        return map;
    }
}

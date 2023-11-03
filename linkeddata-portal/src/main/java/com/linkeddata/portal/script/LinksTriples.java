package com.linkeddata.portal.script;

import com.linkeddata.portal.service.FindLinksService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author : gaoshuai
 * @date : 2022/11/3 17:31
 */
@RestController
@Api(tags = "计算数据集与外部关联关系")
public class LinksTriples {
    @Resource
    private FindLinksService findLinksService;

    /**
     * 增加植物数据与 dbpedia 植物数据的关联
     *
     * @author wangzhiliang
     * @date 20221104
     */
    @ApiOperation("生成植物数据与 dbpedia 关联关系")
    @GetMapping("/getPlantLinksByDbpedia")
    public void getPlantLinksByDbpedia() {
        findLinksService.getPlantLinksByDbpedia();

    }

    /**
     * 增加植物数据与 dbpedia 植物数据的关联
     *
     * @author wangzhiliang
     * @date 20221104
     */
    @ApiOperation("生成微生物 protein 数据与 dbpedia 关联关系")
    @GetMapping("/getMircoProteinLinksProteinOntology")
    public void getMircoProteinLinksProteinOntology() {
        findLinksService.getMircoProteinLinksProteinOntology();

    }

    /**
     * 增加植物数据与 dbpedia 植物数据的关联
     *
     * @author wangzhiliang
     * @date 20221104
     */
    @ApiOperation("通用关联关系")
    @PostMapping("/linkedCommon")
    public void linkedCommon() {
//        findLinksService.linkedCommon();
        findLinksService.linkedLabelProperty("s", "s", "s");

    }

    @ApiOperation("关联geoNames")
    @PostMapping("/linkedGeoNames")
    public void linkedGeoNames() {
        findLinksService.linkedGeoNames("s", "s", "s");
    }

    /**
     * 增加植物数据与 dbpedia 植物数据的关联
     *
     * @author wangzhiliang
     * @date 20221104
     */
    @ApiOperation("关联gbif")
    @PostMapping("/linkGbif")
    public void linkGbif() {
        findLinksService.linkGbif();

    }

    @ApiOperation("动物所关联gbif")
    @PostMapping("/animalLinkGbif")
    public void animalLinkGbif() {
        findLinksService.animalLinkGbif();

    }

    /**
     * 删除图数据
     *
     * @author wangzhiliang
     * @date 20221212
     */
//    @ApiOperation("删除图")
//    @PostMapping("/deleteGraph")
    public void deleteGraph() {
        findLinksService.deleteGraph();
    }

    /**
     * 化合物数据关联 pubchem 数据
     *
     * @author wangzhiliang
     * @date 20221212
     */
//    @ApiOperation("化合物数据关联 pubchem 数据")
//    @GetMapping("/dealLinkPubchem")
    public void dealLinkPubchem() {
        findLinksService.dealLinkPubchem();
    }

    /**
     * 化合物数据关联 pubchem dbpedia 数据
     *
     * @author wangzhiliang
     * @date 20221228
     */
//    @ApiOperation("化合物数据关联 pubchem dbpedia 数据")
//    @GetMapping("/compoundLink")
    public void compoundLink() {
        findLinksService.compoundLink();
    }

    /**
     * 化合物数据关联 pubchem dbpedia 数据
     *
     * @author wangzhiliang
     * @date 20221228
     */
    @ApiOperation("植物关联wikidata")
    @GetMapping("/getWikiData")
    public void getWikiData() {
        findLinksService.getWikiData();
    }

    /**
     * 生成 临床试验数据rdf
     *
     * @author wangzhiliang
     * @date 20221228
     */
    @ApiOperation("生成临床试验数据rdf")
    @GetMapping("/generate")
    public void generateResearchRDF() {
        findLinksService.generateResearchRDF();
    }

    /**
     * 生成人口健康rdf
     *
     * @author wangzhiliang
     * @date 20221228
     */
    @ApiOperation("生成人口健康rdf")
    @GetMapping("/generateNCMIRDF")
    public void generateNCMIRDF() {
        findLinksService.generateNCMIRDF();
    }

    /**
     *  生成化合物跟 mesh词表关联关系
     *
     * @author wangzhiliang
     * @date 20230215
     */
    @ApiOperation("生成化合物跟 mesh词表关联关系")
    @GetMapping("/generateByMesh")
    public void generateByMesh() {
        findLinksService.generateByMesh();
    }
}

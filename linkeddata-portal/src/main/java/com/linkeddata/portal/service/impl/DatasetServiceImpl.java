package com.linkeddata.portal.service.impl;

import com.linkeddata.portal.entity.DataSetRequest;
import com.linkeddata.portal.entity.PageTools;
import com.linkeddata.portal.entity.mongo.Dataset;
import com.linkeddata.portal.entity.mongo.Link;
import com.linkeddata.portal.repository.ApplicationsDao;
import com.linkeddata.portal.repository.daoImpl.DatasetDaoImpl;
import com.linkeddata.portal.service.DatasetService;
import com.linkeddata.portal.utils.StringUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 数据集服务层实现类
 *
 * @author : gaoshuai
 * @date : 2022/9/9 10:53
 */
@Service
public class DatasetServiceImpl implements DatasetService {

    @Resource
    private DatasetDaoImpl datasetDao;

    @Resource
    private ApplicationsDao applicationsDao;

    @Override
    public List<Dataset> listDatasets() {
        return datasetDao.listDatasets();
    }

    @Override
    public List<Dataset> listShowDatasets() {
        return datasetDao.listShowDatasets();
    }

    @Override
    public List<Dataset> listDatasetsByDomain(String domain) {
        return datasetDao.listDatasetsByDomain(domain);
    }

    @Override
    public List<Dataset> listDatasetsByDatacenterId(String datacenterId) {
        return datasetDao.listDatasetsByDatacenterId(datacenterId);
    }

    @Override
    public PageTools<List<Dataset>> getDatasetList(DataSetRequest request) {
        return datasetDao.getDatasetList(request);
    }

    @Override
    public Dataset getDatasetById(String id) {
        return datasetDao.getDatasetById(id);
    }

    @Override
    public Dataset getDatasetByIdentifier(String identifier) {
        return datasetDao.getDatasetByIdentifier(identifier);
    }

    @Override
    public void downloadOneDataset(String id) {
        datasetDao.downloadOneDataset(id);
    }

    @Override
    public void downloadDatasetsByIds(List<String> idList) {
        datasetDao.downloadDatasetsByIds(idList);
    }

    @Override
    public List<String> listInstitutions() {
        return datasetDao.listInstitutions();
    }

    @Override
    public List<String> listDomains() {
        return datasetDao.listDomains();
    }

    @Override
    public Map listRelationDataset(String identifier) {
          /*
            返回的Map，返回echarts可用格式
            categories 所有数据集名称，作为图例，用于标记不同资源所属数据集，不同数据集不同颜色
           categories:[{name: "A"}, {name: "B"}, {name: "C"}, {name: "D"}, {name: "E"}, {name: "F"}, {name: "G"},…]

            从哪个节点（source）到哪个节点（target），predicate 谓语，A和B之间的关系
            links:[{source: "1",predicate:"", target: "0"}, {source: "2", target: "0"}, {source: "3", target: "0"},…]

            每个节点信息
            nodes:[{id: "0", name: "URI简写",category:"对应name的值，表示不同的分类" symbolSize: 19.12381, x: -266.82776, y: 299.6904, value: 28.685715,…},…]
         */
        String prefixUrl = "http://semweb.csdb.cn/dataset/";
        String lodPrefixUrl = "https://lod-cloud.net/dataset/";

        Dataset dataset = datasetDao.getDatasetByIdentifier(identifier);
        if (null == dataset) {
            return null;
        }

        Map<String, Object> resultMap = new HashMap(16);
        // 该数据集关联的其他数据集列表
        List<Link> linkList = dataset.getLinks();
        // 返回 categories
        List<Map> categories = new ArrayList<>();
        // 返回links
        List<Map> links = new ArrayList<>();
        // 所有数据集nodes列表
        List<Map> nodes = new ArrayList();

        // 存放所有领域,因为关联数据集的领域有可能和自己的领域重复
        Set<String> domainSet = new HashSet();
        domainSet.add(dataset.getDomain());

        // 外部的数据没有领域,统一返回领域为LOD标记为外部数据集，用于数据集详情页图谱显示颜色
        for (Link link : linkList) {
            String target = link.getTarget();
            Dataset datasetByIdentifier = this.getDatasetByIdentifier(target);

            if (datasetByIdentifier != null) {
                domainSet.add(datasetByIdentifier.getDomain());
                // 所有数据集nodes
                Map nodeMap = new HashMap();
                nodeMap.put("title", datasetByIdentifier.getIdentifier());
                nodeMap.put("identifier", datasetByIdentifier.getIdentifier());
                nodeMap.put("triples", datasetByIdentifier.getTriples());
                nodeMap.put("category", datasetByIdentifier.getDomain());
                nodeMap.put("url", datasetByIdentifier.getIdentifier());
                nodeMap.put("caption", datasetByIdentifier.getTitle());
                // 是否是外部数据集
                nodeMap.put("isExternal", false);
                nodes.add(nodeMap);
            } else {
                // 外部数据集领域默认LOD
                domainSet.add("LOD");
                Map nodeMap = new HashMap();
                nodeMap.put("title", link.getTarget());
                nodeMap.put("identifier", link.getTarget());
                nodeMap.put("triples", link.getTargetTriples());
                nodeMap.put("url", lodPrefixUrl + link.getTarget());
                nodeMap.put("category", "LOD");
                nodeMap.put("caption", link.getTarget());
                // 是否是外部数据集
                nodeMap.put("isExternal", true);
                nodes.add(nodeMap);
            }

            // 返回关联关系links
            Map map = new HashMap(16);
            map.put("source", dataset.getIdentifier());
            map.put("target", link.getTarget());
            // 关联关系数量
            map.put("value", link.getValue());
            links.add(map);
        }
        // 该数据集自己的nodes
        Map nodeMap = new HashMap();
        nodeMap.put("title", dataset.getIdentifier());
        nodeMap.put("identifier", dataset.getIdentifier());
        nodeMap.put("triples", dataset.getTriples());
        nodeMap.put("url", dataset.getIdentifier());
        nodeMap.put("category", dataset.getDomain());
        nodeMap.put("caption", dataset.getTitle());
        // 是否是外部数据集
        nodeMap.put("isExternal", false);
        nodes.add(nodeMap);

        // 返回categories
        for (String domain : domainSet) {
            Map map = new HashMap(16);
            map.put("name", domain);
            categories.add(map);
        }

        resultMap.put("categories", categories);
        resultMap.put("links", links);
        resultMap.put("nodes", nodes);
        return resultMap;
    }

    @Override
    public Dataset getDatasetBySparql(String sparql) {
        return datasetDao.getDatasetBySparql(sparql);
    }

    @Override
    public Map listAllDatasetRelation(String datacenterId) {
         /*
            返回的Map，返回echarts可用格式
            categories 所有数据集名称，作为图例，用于标记不同资源所属数据集，不同数据集不同颜色
           categories:[{name: "A"}, {name: "B"}, {name: "C"}, {name: "D"}, {name: "E"}, {name: "F"}, {name: "G"},…]

            从哪个节点（source）到哪个节点（target），predicate 谓语，A和B之间的关系
            links:[{source: "1",predicate:"", target: "0"}, {source: "2", target: "0"}, {source: "3", target: "0"},…]

            每个节点信息
            nodes:[{id: "0", name: "URI简写",category:"对应name的值，表示不同的分类" symbolSize: 19.12381, x: -266.82776, y: 299.6904, value: 28.685715,…},…]
         */
        Map<String, Object> resultMap = new HashMap(16);
        List<Dataset> datasetList;
        if (null == datacenterId || "".equals(datacenterId)) {
            // 仅查询展示在首页云图中的数据集，以免新添加的数据集未及时添加云图坐标而错乱
            datasetList = this.listShowDatasets();
//            datasetList = this.listDatasets();
        } else {
            datasetList = this.listDatasetsByDatacenterId(datacenterId);
        }

        // 返回 categories
        List<Map> categories = new ArrayList<>();
        for (Dataset dataset : datasetList) {
            Map map = new HashMap(16);
            map.put("name", dataset.getDomain());
            categories.add(map);
        }
        // 返回links
        List<Map> links = new ArrayList<>();
        for (Dataset dataset : datasetList) {
            List<Link> linkList = dataset.getLinks();
            if (linkList != null && linkList.size() > 0) {
                for (Link link : linkList) {
                    Map map = new HashMap(16);
                    // 有些关联的数据集不在我们的库中，就没有返回
                    Dataset datasetByIdentifier = this.getDatasetByIdentifier(link.getTarget());
                    if (datasetByIdentifier == null) {
                        continue;
                    }
                    map.put("source", dataset.getIdentifier());
                    map.put("target", link.getTarget());
                    // 关联关系数量
                    map.put("value", link.getValue());
                    links.add(map);
                }
            }
        }
        String prefixUrl = "http://semweb.csdb.cn/dataset/";
        // 所有数据集nodes列表
        List<Map> nodes = new ArrayList();
        for (Dataset dataset : datasetList) {
            Map map = new HashMap(16);
            map.put("id", dataset.getIdentifier());
            map.put("name", dataset.getIdentifier());
            map.put("category", dataset.getDomain());
            // 数据集内三元组数量
            map.put("triples", dataset.getTriples());
            // 链接地址
            map.put("url", prefixUrl + dataset.getIdentifier());
            map.put("caption", dataset.getTitle());
            nodes.add(map);
        }
        resultMap.put("categories", categories);
        resultMap.put("links", links);
        resultMap.put("nodes", nodes);
        return resultMap;
    }

    @Override
    public Map getStatisticsInfo(String datacenterId) {
        Map map = new HashMap(16);
        // 数据集数量
        Integer datasetCount = 0;
        // rdf数量
        Long triplesCount = 0L;
        // rdf体量
        String volume;
        // 浏览量
        Long view = 0L;
        // 下载量
        Long download = 0L;
        // 实体（主语）数量
        Long recordCount;
        if (null == datacenterId || "".equals(datacenterId.trim())) {
            view = datasetDao.countView();
            download = datasetDao.countDataDownload();
            triplesCount = datasetDao.countTriples();
            datasetCount = datasetDao.countDataset();
            recordCount = applicationsDao.countRecords();
            volume = this.datasetVolume("");
        } else {
            view = datasetDao.countView(datacenterId);
            download = datasetDao.countDataDownload(datacenterId);
            triplesCount = datasetDao.countTriples(datacenterId);
            datasetCount = datasetDao.countDataset(datacenterId);
            recordCount = applicationsDao.countRecords(datacenterId);
            volume = this.datasetVolume(datacenterId);
        }
        map.put("openDataset", datasetCount);
        map.put("rdfTriples", triplesCount);
        map.put("dataVolume", volume);
        map.put("view", view);
        map.put("download", download);
        map.put("records", recordCount);
        return map;
    }


    /**
     * 获取数据集多有rdf体量，并以接近的单位显示
     *
     * @return
     */
    private String datasetVolume(String datacenterId) {
        Long volume = 0L;
        if (null == datacenterId || "".equals(datacenterId)) {
            volume = datasetDao.dataVolume();
        } else {
            volume = datasetDao.dataVolume(datacenterId);
        }
        String volumeStr = StringUtil.readableFileSize(volume);
        return volumeStr;
    }




    @Override
    public String getIdentifierByGraph(String graph) {
        return datasetDao.getIdentifierByGraph(graph);
    }
}

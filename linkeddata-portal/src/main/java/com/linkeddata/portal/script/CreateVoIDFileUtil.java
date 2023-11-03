package com.linkeddata.portal.script;

import com.linkeddata.portal.entity.mongo.Dataset;
import com.linkeddata.portal.entity.mongo.Link;
import com.linkeddata.portal.service.DatasetService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

/**
 * 创建数据集元数据描述信息VoID
 *
 * @author : gaoshuai
 * @date : 2022/10/12 16:19
 */
@RestController
@Api(tags = "创建数据集描述文件")
public class CreateVoIDFileUtil {

    @javax.annotation.Resource
    private DatasetService datasetService;

    @Value("${downloadDir}")
    private String filePath;

    /**
     * 创建所有数据集的描述文件
     */
    @ApiOperation(value = "创建所有数据集的描述文件", notes = "文件路径/mnt/link-portal/download/{identifier}")
    @GetMapping("/createAllDatasetVoID")
    public void createDatasetMetadata() {
        try {
            List<Dataset> datasetList = datasetService.listDatasets();
            if (null == datasetList || datasetList.isEmpty()) {
                return;
            }
            for (Dataset dataset : datasetList) {
                createOneDatasetMetadata(dataset);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建某一领域的数据集描述文件
     *
     * @param domain 领域名称
     */
    @ApiOperation(value = "创建某一领域数据集的描述文件", notes = "文件路径/mnt/link-portal/download/{identifier}")
    @ApiImplicitParam(name = "domain", value = "数据集领域名称", required = true)
    @GetMapping("/createDatasetVoIDByDomain")
    public void createDatasetMetadataByDomain(String domain) {
        try {
            List<Dataset> datasetList = datasetService.listDatasetsByDomain(domain);
            if (null == datasetList || datasetList.isEmpty()) {
                return;
            }
            for (Dataset dataset : datasetList) {
                createOneDatasetMetadata(dataset);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 根据identifier创建某个数据集的描述文件
     *
     * @param identifier
     */
    @ApiOperation(value = "创建某一数据集的描述文件", notes = "文件路径/mnt/link-portal/download/{identifier}")
    @ApiImplicitParam(name = "identifier", value = "数据集identifier", required = true)
    @GetMapping("/createDatasetVoIDByIdentifier")
    public void createDatasetMetadataByIdentifier(String identifier) {
        try {
            Dataset dataset = datasetService.getDatasetByIdentifier(identifier);
            if (null == dataset) {
                return;
            }
            createOneDatasetMetadata(dataset);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建数据集的描述VoID文件
     *
     * @param dataset
     */
    private void createOneDatasetMetadata(Dataset dataset) {
        try {
            if (null == dataset) {
                return;
            }

            String identifier = dataset.getIdentifier();
            String fileName = filePath + identifier + File.separator + identifier;
            String cnicUrl = "http://semweb.csdb.cn/DS?identifier=";
            String lodUrl = "https://lod-cloud.net/dataset/";
            String fileDir = filePath + identifier;
            File file = new File(fileDir);
            if (!file.exists()) {
                boolean mkdir = file.mkdirs();
                System.out.println(mkdir);
            }
            // 创建该数据集资源
            Model model = ModelFactory.createDefaultModel();
            // namespace
            model.setNsPrefix("void", VOID.NS);
            model.setNsPrefix("xsd", XSD.NS);
            model.setNsPrefix("dcterms", DCTerms.NS);
            model.setNsPrefix("rdfs", RDFS.getURI());
            model.setNsPrefix("dcat", DCAT.NS);
            model.setNsPrefix("foaf", FOAF.NS);

            Resource thisDataset = model.createResource(cnicUrl + identifier);

            List<Link> links = dataset.getLinks();

            for (Link tmpLink : links) {
                String target = tmpLink.getTarget();
                Resource link = model.createResource();
                // 此处为关联数据集的链接地址，如果是外部的就用lod-cloud的地址
                Dataset datasetByIdentifier = datasetService.getDatasetByIdentifier(target);
                if (null == datasetByIdentifier) {
                    link.addProperty(VOID.target, model.createResource(lodUrl + tmpLink.getTarget()));
                } else {
                    link.addProperty(VOID.target, model.createResource(cnicUrl + tmpLink.getTarget()));
                }
                link.addProperty(VOID.target, thisDataset);
                link.addProperty(VOID.triples, ResourceFactory.createTypedLiteral(tmpLink.getValue()));
            }

            thisDataset.addProperty(RDF.type, VOID.Dataset);
            // 根据dataset内容对应赋值
            // 英文描述
            Map<String, String> description = dataset.getDescription();
            String enDescription = description.get("en") + "";
            thisDataset.addProperty(DCTerms.description, ResourceFactory.createLangLiteral(enDescription, "en"));
            // 发布者 label联系人名称，mbox联系人邮箱
            thisDataset.addProperty(DCTerms.publisher, model.createResource()
                    .addProperty(RDFS.label, dataset.getContactName())
                    .addProperty(FOAF.mbox, dataset.getContactEmail()));

            // 关键词可能有多个
            List<String> keywords = dataset.getKeywords();
            for (String keyword : keywords) {
                thisDataset.addProperty(DCTerms.subject, keyword);
            }
            // title
            thisDataset.addProperty(DCTerms.title, ResourceFactory.createLangLiteral(dataset.getTitle(), "en"));
            thisDataset.addProperty(VOID.sparqlEndpoint, model.createResource(dataset.getSparql()));
            thisDataset.addProperty(VOID.triples, ResourceFactory.createTypedLiteral(dataset.getTriples()));
            thisDataset.addProperty(FOAF.homepage, model.createResource(dataset.getWebsite()));

            // 以文件的形式导出
            model.write(new FileOutputStream(fileName + ".rdf"), "RDF/XML");
            model.write(new FileOutputStream(fileName + ".ttl"), "TURTLE");
            model.write(new FileOutputStream(fileName + ".nt"), "N-TRIPLE");
            model.write(new FileOutputStream(fileName + ".jsonld"), "JSON-LD");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据identifier创建元数据描述信息
     *
     * @param identifier
     * @return
     */
    public Model createVoidFileByIdentifier(String identifier) {
        try {
            Dataset dataset = datasetService.getDatasetByIdentifier(identifier);
            if (null == dataset) {
                return null;
            }
            String cnicUrl = "http://semweb.csdb.cn/DS?identifier=";
            String lodUrl = "https://lod-cloud.net/dataset/";
            // 创建该数据集资源
            Model model = ModelFactory.createDefaultModel();
            // namespace
            model.setNsPrefix("void", VOID.NS);
            model.setNsPrefix("xsd", XSD.NS);
            model.setNsPrefix("dcterms", DCTerms.NS);
            model.setNsPrefix("rdfs", RDFS.getURI());
            model.setNsPrefix("dcat", DCAT.NS);
            model.setNsPrefix("foaf", FOAF.NS);

            org.apache.jena.rdf.model.Resource thisDataset = model.createResource(cnicUrl + identifier);

            List<Link> links = dataset.getLinks();
            if (null != links && !links.isEmpty()) {
                for (Link tmpLink : links) {
                    String target = tmpLink.getTarget();
                    org.apache.jena.rdf.model.Resource link = model.createResource();
                    // 此处为关联数据集的链接地址，如果是外部的就用lod-cloud的地址
                    Dataset datasetByIdentifier = datasetService.getDatasetByIdentifier(target);
                    if (null == datasetByIdentifier) {
                        link.addProperty(VOID.target, model.createResource(lodUrl + tmpLink.getTarget()));
                    } else {
                        link.addProperty(VOID.target, model.createResource(cnicUrl + tmpLink.getTarget()));
                    }
                    link.addProperty(VOID.target, thisDataset);
                    link.addProperty(VOID.triples, ResourceFactory.createTypedLiteral(tmpLink.getValue()));
                }
            }
            thisDataset.addProperty(RDF.type, VOID.Dataset);
            // 根据dataset内容对应赋值
            // 英文描述
            Map<String, String> description = dataset.getDescription();

            if (null != description && !description.isEmpty()) {
                String enDescription = description.get("en") + "";
                thisDataset.addProperty(DCTerms.description, ResourceFactory.createLangLiteral(enDescription, "en"));
            }
            // 发布者 label联系人名称，mbox联系人邮箱
          /*  thisDataset.addProperty(DCTerms.publisher, model.createResource()
                    .addProperty(RDFS.label, dataset.getContactName())
                    .addProperty(FOAF.mbox, dataset.getContactEmail()));*/
            thisDataset.addProperty(DCTerms.publisher, model.createResource());
            if (null != dataset.getContactName()) {
                thisDataset.addProperty(RDFS.label, dataset.getContactName());
            }
            if (null != dataset.getContactEmail()) {
                thisDataset.addProperty(RDFS.label, dataset.getContactEmail());
            }

            // 关键词可能有多个
            List<String> keywords = dataset.getKeywords();
            if (null != keywords && !keywords.isEmpty()) {
                for (String keyword : keywords) {
                    thisDataset.addProperty(DCTerms.subject, keyword);
                }
            }
            // title
            if (null != dataset.getTitle()) {
                thisDataset.addProperty(DCTerms.title, ResourceFactory.createLangLiteral(dataset.getTitle(), "en"));
            }
            if (null != dataset.getSparql()) {
                thisDataset.addProperty(VOID.sparqlEndpoint, model.createResource(dataset.getSparql()));
            }
            if (null != dataset.getTriples()) {
                thisDataset.addProperty(VOID.triples, ResourceFactory.createTypedLiteral(dataset.getTriples()));
            }
            if (null != dataset.getWebsite()) {
                thisDataset.addProperty(FOAF.homepage, model.createResource(dataset.getWebsite()));
            }
            return model;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

package com.linkeddata.portal.script;

import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * RDF三元组转换为文本三元组
 * 为了给大语言模型使用，存入向量库
 * 相比第二版，修改了getLabel的方式，getLabel从版纳所的本体中获取
 */
@RestController
@Slf4j
public class RdfToTextRdf {
    static FileWriter writerPhoto;
    static FileWriter writerSubjectNull;
    /**
     * 记录已经处理过的数据，不要再去virtuoso中查找
     */
    private static Map uriMap;
    /**
     * 记录处理了多少条
     */
    private static int count = 1;
    static FileWriter writerObjectLabelNUll;
    /**
     * 记录查出的三元组总体条数
     */
    private static int totalCount = 0;
    /**
     * 记录排除的三元组数量
     */
    private static int delCount = 0;
    /**
     * 排除的photo主语的数量
     */
    private static int photoCount = 0;
    /**
     * 排除的主语lable为空的数量
     */
    private static int subjectNullCount = 0;
    /**
     * 排除的宾语为空的的数量
     */
    private static int objectNullCount = 0;

    static {
        uriMap = new HashMap();
        uriMap.put(RDFS.label.getURI(), "名称");
        uriMap.put(RDF.type.getURI(), "类型");
        uriMap.put(RDFS.subClassOf.getURI(), "父类");


    }

   /* static {
        String fileNamePhoto = "/mnt/xtbg/rdfText_photo.txt";
        String fileNameSubjectNull = "/mnt/xtbg/rdfTextSubjectNull.txt";
        String fileNameLabelNUll = "/mnt/xtbg/rdfTextObjectLabelNUll.txt";

        File filePhoto = new File(fileNamePhoto);
        File fileSubjectNull = new File(fileNameSubjectNull);
        File fileObjectLabelNUll = new File(fileNameLabelNUll);

        try {
            writerPhoto = new FileWriter(filePhoto);
            writerSubjectNull = new FileWriter(fileSubjectNull);
            writerObjectLabelNUll = new FileWriter(fileObjectLabelNUll);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    /**
     * 默认每页10000条数据
     */
    private final Long LIMIT = 10000L;

    @GetMapping("/rdfToText")
    public void rdfToText(String endpoint) {
        Date start = new Date();
        log.info("########## 程序开始 ########## " + sdf.format(start));
        convretRdfToText(endpoint);
        Date end = new Date();
        log.info("结束时间:{}", sdf.format(end));

        try {
            writerPhoto.close();
            writerSubjectNull.close();
            writerObjectLabelNUll.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*@Test
    public void methodTest() {
        String endpoint = "http://xtbg.semweb.csdb.cn/sparql";
        rdfToText(endpoint);
    }*/

    public void convretRdfToText(String endpoint) {
        long startTime = System.currentTimeMillis();

//        String fileName = "E:\\TSTOR\\rdf_to_text\\test.txt";
        String fileName = "/mnt/xtbg/rdfText.txt";

        File file = new File(fileName);

        log.info("开始加载ttl");
        long start = System.currentTimeMillis();
        Model dataModel = this.loadModel();
        long end = System.currentTimeMillis();
        log.info("加载ttl结束，用时{}秒", (end - start) / 1000);

        try {
            FileWriter writer = new FileWriter(file);


            RDFConnection conn = RDFConnectionRemote.service(endpoint).build();
            String query = "construct { ?s ?p ?o . } from <xtbgData>  where { ?s ?p ?o } ";
            loop(conn, query, 0L, writer, dataModel);
        } catch (IOException e) {
            e.printStackTrace();
        }
        long endTime = System.currentTimeMillis();
        log.info("######### 程序执行结束，用时: {} #########", (endTime - startTime) / 1000 + "s");
        log.info("总查询的三元组数量：{}，不符合要求的三元组数量：{}", totalCount, delCount);
        log.info("photo类型三元组数量：{}，主语label为空的三元组数量：{}，宾语label为空的三元组数量：{}", photoCount, subjectNullCount, objectNullCount);
    }

    public void loop(RDFConnection conn, String query, Long PageSize, FileWriter writer, Model dataModel) {
        Model model = conn.queryConstruct(query + this.offsetAndLimit(PageSize, LIMIT));
        long size = model.size();
        // 1、先查出所有三元组
        StmtIterator stmtIterator = model.listStatements();
        while (stmtIterator.hasNext()) {
            totalCount = totalCount + 1;
            Statement st = stmtIterator.next();
            Resource resource = st.getSubject();
            String uri = resource.getURI();

            // 图片类型的三元组不要
            if (uri.startsWith("http://xtbg.semweb.csdb.cn/resource/Photo_")) {
                delCount = delCount + 1;
                photoCount++;

                try {
                    writerPhoto.write(uri + ".\n");
                    writerPhoto.write(System.lineSeparator());
                    writerPhoto.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                continue;
            }
            String resourceLabel = this.getLabel(resource, dataModel);

            Property predicate = st.getPredicate();
            String prdicateLabel = this.getLabel(predicate, dataModel);


            RDFNode object = st.getObject();
            String objectLabel = "";
            if (object.isLiteral()) {
                objectLabel = object.asLiteral().getString();
            } else if (object.isResource()) {
                Resource objectResource = object.asResource();
                objectLabel = this.getLabel(objectResource, dataModel);
            }

            // 宾语label为空的不要
            if (null == objectLabel || "".equals(objectLabel)) {
                delCount = delCount + 1;
                objectNullCount++;

                try {
                    writerObjectLabelNUll.write(uri + "\t " + object.toString() + ".\n");
                    writerObjectLabelNUll.write(System.lineSeparator());
                    writerObjectLabelNUll.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                continue;
            } else if (null == resourceLabel || "".equals(resourceLabel)) {
                delCount = delCount + 1;
                subjectNullCount++;

                try {
                    writerSubjectNull.write(uri + "\t " + resource.toString() + ".\n");
                    writerSubjectNull.write(System.lineSeparator());
                    writerSubjectNull.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                continue;
            }
            try {

//                if (count < 500000) {
                writer.write(resourceLabel + "的" + prdicateLabel + "是:" + objectLabel + ".\n");
                // 每个三元组之间用空行隔开，不然向量化时会将一个数据集的所有三元组看成一个整体，导致最后提交给LLM的token太长
                writer.write(System.lineSeparator());
                writer.flush();
                System.out.println("转换第" + (count++) + "个");
             /*   } else {
                    // 打开新的 FileWriter 继续写入下一块
                    writer = new FileWriter("new_file.txt", true);
                    // 写入第二块数据
                    writer.write(resourceLabel + "的" + prdicateLabel + "是" + objectLabel + ".\n");
                    // 每个三元组之间用空行隔开，不然向量化时会将一个数据集的所有三元组看成一个整体，导致最后提交给LLM的token太长
                    writer.write(System.lineSeparator());
                    writer.flush();
                    System.out.println("转换第" + (count++) + "个");
                }*/
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        if (size > 0) {
            loop(conn, query, PageSize + size, writer, dataModel);
        }
    }

    /**
     * 拼接offset 和limit
     *
     * @param pageSize
     * @param limit
     * @return
     */
    private String offsetAndLimit(Long pageSize, Long limit) {
        return " offset " + pageSize + " LIMIT " + limit + " ";
    }

    /**
     * 加载本地模型
     *
     * @return
     */
    private Model loadModel() {
        Model model = ModelFactory.createDefaultModel();
        String inputFileName = "/mnt/xtbg/xtbgOntology.ttl";
        InputStream in = FileManager.getInternal().open(inputFileName);

        Model model2 = ModelFactory.createDefaultModel();
        String inputFileNameData = "/mnt/xtbg/xtbgData.ttl";
        InputStream inData = FileManager.getInternal().open(inputFileNameData);

        Model dataOntologyModel = ModelFactory.createDefaultModel();
        try {
            Model read = model.read(in, "", "TURTLE");
            Model dataModel = model2.read(inData, "", "TURTLE");

            dataOntologyModel.add(dataModel);
            dataOntologyModel.add(read);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dataOntologyModel;
    }

    /**
     * 从本地ttl中查询uri的label
     *
     * @param resource
     * @param model
     * @return
     */
    private String getLabel(Resource resource, Model model) {
        if (uriMap.containsKey(resource.getURI())) {
            return uriMap.get(resource.getURI()).toString();
        }
        String label = "";

        try {
            StmtIterator stmtIterator = model.listStatements(resource, RDFS.label, (RDFNode) null);
            while (stmtIterator.hasNext()) { // TODO 取第一个
                Statement next = stmtIterator.next();
                RDFNode object = next.getObject();
                if (object.isResource()) {
                    label = getLabel(object.asResource(), model);
                } else {
                    label = object.asLiteral().getString();
                }
                if (null != label && !"".equals(label)) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (null != label && !"".equals(label)) {
            uriMap.put(resource.getURI(), label);
        }
        return label;
    }


}

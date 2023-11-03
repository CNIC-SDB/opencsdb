package com.linkeddata.portal.script;

import com.linkeddata.portal.entity.mongo.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 9.6课题专用：语义网对接LLM大模型
 *
 * @author chenkun
 * @since 2023年7月6日13:59:52
 */
@RestController
@RequestMapping("/datasets96ToLLM")
public class Datasets96ToLLM {

    @Resource
    private MongoTemplate mongoTemplate;

    /**
     * 将数据集RDF转化为文本文件，用来喂给LLM（每行一个文本三元组）
     * 使用方法：修改`fileName`，在本机以`active = prod96`启动后，访问本请求
     * 请求地址：http://localhost:8081/datasets96ToLLM/datasetsToText_v1
     * 执行时间很快，2秒左右
     *
     * @author chenkun
     * @since 2023年7月6日14:00:10
     */
    @RequestMapping("/datasetsToText_v1")
    public Object datasetsToText_v1() {
        // 计时
        Date date = new Date();
        // 成功标识
        boolean successFlag = false;

        try {
            // 文件生成路径
            String fileName = "D:\\临时文件\\datasets96\\datasets.txt";
            File file = new File(fileName);
            FileWriter writer = new FileWriter(file);

            /**
             * 查询返回给协同分析的所有数据集（hdfsPath不为null）
             */
            Query query = new Query();
            query.addCriteria(Criteria.where("hdfsPath").ne(null));
            List<Dataset> datasetList = mongoTemplate.find(query, Dataset.class);

            /**
             * 遍历数据集，查询数据集RDF，转化为文本，依次写入到文件中
             */
            for (Dataset dataset : datasetList) {
                /**
                 * 连接数据集SPARQL端点，得到数据集RDF对应的model
                 */
                RDFConnection conn = RDFConnectionRemote.service(dataset.getSparql()).build();
                Model model = conn.queryConstruct("construct { ?s ?p ?o . } where { ?s a <https://schema.org/Dataset> . ?s ?p ?o . }");

                /**
                 * 在数据集中，需要构建语料的字段，陈锟与魏天珂核对，2023年7月6日15:08:04
                 */
                List<List<String>> propertyList = new ArrayList<>();
                propertyList.add(Arrays.asList("name", "名称"));
                propertyList.add(Arrays.asList("description", "简介"));
                propertyList.add(Arrays.asList("keyword", "关键词"));
                propertyList.add(Arrays.asList("subject", "学科分类"));
                propertyList.add(Arrays.asList("dataType", "文件格式"));
                propertyList.add(Arrays.asList("spatialInfo", "空间范围"));
                propertyList.add(Arrays.asList("region", "行政地区"));
                propertyList.add(Arrays.asList("timeResolution", "时间分辨率"));
                propertyList.add(Arrays.asList("spatialResolution", "空间分辨率"));
                propertyList.add(Arrays.asList("startDate", "开始时间"));
                propertyList.add(Arrays.asList("endDate", "结束时间"));
                propertyList.add(Arrays.asList("RangeDescription", "地域范围说明"));
                propertyList.add(Arrays.asList("Center", "地理中心点坐标"));
                propertyList.add(Arrays.asList("LowLeft", "地理右上角坐标"));
                propertyList.add(Arrays.asList("LowRight", "地理左下角坐标"));
                propertyList.add(Arrays.asList("UpLeft", "地理左上角坐标"));
                propertyList.add(Arrays.asList("UpRight", "地理右下角坐标"));

                /**
                 * 生成文本文件，用来喂给LLM
                 */
                writer.write("<" + dataset.getTitle() + "> <类型> <数据集>" + System.lineSeparator());
                for (List<String> property : propertyList) {
                    // 在三元组中根据属性获取属性值
                    Statement statement = model.getProperty(null, new PropertyImpl("http://skos.semweb.csdb.cn/vocabulary/dataset#" + property.get(0)));
                    // 此属性有值时才往文件中写
                    if (statement != null) {
                        writer.write("<" + dataset.getTitle() + "> <" + property.get(1) + "> <" + statement.getObject().asLiteral().getValue().toString() + ">" + System.lineSeparator());
                        // 每个三元组之间用空行隔开，不然向量化时会将一个数据集的所有三元组看成一个整体，导致最后提交给LLM的token太长
                        writer.write(System.lineSeparator());
                    }
                }
                // 多个数据集之间用2个空行隔开
                writer.write(System.lineSeparator());
                writer.write(System.lineSeparator());
            }
            writer.close();
            successFlag = true;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        // 计时
        Date date2 = new Date();
        return (successFlag ? "执行成功" : "执行失败") + "，用时：" + ((date2.getTime() - date.getTime()) / 1000 + 1) + "秒";
    }

    /**
     * 将数据集RDF转化为文本文件，用来喂给LLM（每行包括数据集完整信息）
     * 每行格式为：xxx是一个数据集。它的名称是：xxx。它的简介是：xxx。它的关键词是：xxx。。。。
     * 使用方法：修改`fileName`，在本机以`active = prod96`启动后，访问本请求
     * 请求地址：http://localhost:8081/datasets96ToLLM/datasetsToText_v2
     * 执行时间很快，2秒左右
     *
     * @author chenkun
     * @since 2023年8月15日16:44:29
     */
    @RequestMapping("/datasetsToText_v2")
    public Object datasetsToText_v2() {
        // 计时
        Date date = new Date();
        // 成功标识
        boolean successFlag = false;

        try {
            // 文件生成路径
            String fileName = "D:\\临时文件\\datasets96\\datasets.txt";
            File file = new File(fileName);
            FileWriter writer = new FileWriter(file);

            /**
             * 查询返回给协同分析的所有数据集（hdfsPath不为null）
             */
            Query query = new Query();
            query.addCriteria(Criteria.where("hdfsPath").ne(null));
            List<Dataset> datasetList = mongoTemplate.find(query, Dataset.class);

            /**
             * 遍历数据集，查询数据集RDF，转化为文本，依次写入到文件中
             */
            for (Dataset dataset : datasetList) {
                /**
                 * 连接数据集SPARQL端点，得到数据集RDF对应的model
                 */
                RDFConnection conn = RDFConnectionRemote.service(dataset.getSparql()).build();
                Model model = conn.queryConstruct("construct { ?s ?p ?o . } where { ?s a <https://schema.org/Dataset> . ?s ?p ?o . }");

                /**
                 * 在数据集中，需要构建语料的字段，陈锟与魏天珂核对，2023年7月6日15:08:04
                 */
                List<List<String>> propertyList = new ArrayList<>();
                propertyList.add(Arrays.asList("name", "名称"));
                propertyList.add(Arrays.asList("description", "简介"));
                propertyList.add(Arrays.asList("keyword", "关键词"));
//                propertyList.add(Arrays.asList("subject", "学科分类"));
//                propertyList.add(Arrays.asList("dataType", "文件格式"));
//                propertyList.add(Arrays.asList("spatialInfo", "空间范围"));
//                propertyList.add(Arrays.asList("region", "行政地区"));
//                propertyList.add(Arrays.asList("RangeDescription", "地域范围说明"));

                /**
                 * 生成文本文件，用来喂给LLM
                 */
                String str = "";
                str += "\"" + dataset.getTitle() + "\"是一个数据集。";
                for (List<String> property : propertyList) {
                    // 在三元组中根据属性获取属性值
                    Statement statement = model.getProperty(null, new PropertyImpl("http://skos.semweb.csdb.cn/vocabulary/dataset#" + property.get(0)));
                    // 此属性有值时才往文件中写
                    if (statement != null) {
                        String value = statement.getObject().asLiteral().getValue().toString();
                        str += "它的" + property.get(1) + "是：" + value + "。";
                    }
                }
                writer.write(str);
                // 多个数据集之间用2个空行隔开
                writer.write(System.lineSeparator());
                writer.write(System.lineSeparator());
            }
            writer.close();
            successFlag = true;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        // 计时
        Date date2 = new Date();
        return (successFlag ? "执行成功" : "执行失败") + "，用时：" + ((date2.getTime() - date.getTime()) / 1000 + 1) + "秒";
    }

    /**
     * 将数据集RDF转化为CSV文件，使得查看更直观
     * 使用方法：修改`fileName`，在本机以`active = prod96`启动后，访问本请求
     * 请求地址：http://localhost:8081/datasets96ToLLM/datasetsToCsv
     * 执行时间很快，本地约13秒左右
     *
     * @author chenkun
     * @since 2023年8月18日09:51:19
     */
    @RequestMapping("/datasetsToCsv")
    public Object datasetsToCsv() {
        // 计时
        Date date = new Date();
        // 成功标识
        boolean successFlag = false;

        try {
            // 文件生成路径
            String fileName = "D:\\临时文件\\datasets96\\datasets.csv";
            File file = new File(fileName);
            FileWriter writer = new FileWriter(file, Charset.forName("GBK"));

            /**
             * 查询返回给协同分析的所有数据集（hdfsPath不为null）
             */
            Query query = new Query();
            query.addCriteria(Criteria.where("hdfsPath").ne(null));
            List<Dataset> datasetList = mongoTemplate.find(query, Dataset.class);

            /**
             * 存入的字段
             */
            List<List<String>> propertyList = new ArrayList<>();
            propertyList.add(Arrays.asList("name", "名称"));
            propertyList.add(Arrays.asList("description", "简介"));
            propertyList.add(Arrays.asList("keyword", "关键词"));
            // csv表头
            String head = "";
            for (int i = 0; i < propertyList.size(); i++) {
                head += propertyList.get(i).get(1);
                if (i < propertyList.size() - 1) head += ",";
            }
            writer.write(head);
            writer.write(System.lineSeparator());

            /**
             * 遍历数据集，查询数据集RDF，转化为文本，依次写入到文件中
             */
            for (Dataset dataset : datasetList) {
                /**
                 * 连接数据集SPARQL端点，得到数据集RDF对应的model
                 */
                RDFConnection conn = RDFConnectionRemote.service(dataset.getSparql()).build();
                Model model = conn.queryConstruct("construct { ?s ?p ?o . } where { ?s a <https://schema.org/Dataset> . ?s ?p ?o . }");

                /**
                 * csv文件内容
                 */
                String row = "";
                for (int i = 0; i < propertyList.size(); i++) {
                    // 在三元组中根据属性获取属性值
                    Statement statement = model.getProperty(null, new PropertyImpl("http://skos.semweb.csdb.cn/vocabulary/dataset#" + propertyList.get(i).get(0)));
                    String value = statement != null ? statement.getObject().asLiteral().getValue().toString() : "";
                    value = escapeCsvValue(value);
                    row += i < propertyList.size() - 1 ? value + "," : value;
                }
                writer.write(row);
                writer.write(System.lineSeparator());
            }
            writer.close();
            successFlag = true;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        // 计时
        Date date2 = new Date();
        return (successFlag ? "执行成功" : "执行失败") + "，用时：" + ((date2.getTime() - date.getTime()) / 1000 + 1) + "秒";
    }

    /**
     * 对字符串中的逗号和引号进行转义，使得值不影响csv解析
     * @param value
     * @return
     */
    public static String escapeCsvValue(String value) {
        if (value.contains(",") || value.contains("\"")) {
            value = value.replace("\"", "\"\"");
            value = "\"" + value + "\"";
        }
        return value;
    }

}

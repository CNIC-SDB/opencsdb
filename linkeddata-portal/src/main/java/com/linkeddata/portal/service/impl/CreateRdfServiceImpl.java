package com.linkeddata.portal.service.impl;

import com.linkeddata.portal.entity.script.generate.GenerateOntology;
import com.linkeddata.portal.service.CreateRdfService;
import com.linkeddata.portal.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

/**
 * 创建 RDF 接口实现类
 *
 * @author wangzhiliang
 * @date 2023/2/20 10:27
 *
 * @update chenkun
 * @since 2023年5月23日16:26:03
 */
@Service
@Slf4j
public class CreateRdfServiceImpl implements CreateRdfService {

    public static void main(String[] args) {
        // 自动生成上海有机所本体
//        generateOntologyForOrganchem();
        // 使用globi关系，生成千金藤属本体
//        generateOntologyForQianjinteng();

        // 自动生成华南植物园本体
        generateOntologyForScbg();
    }

    /**
     * 自动生成华南植物园本体
     *
     * @Author 陈锟
     * @since 2023年10月13日13:52:39
     */
    public static void generateOntologyForScbg() {
        // 封装生成本体所需要的参数
        GenerateOntology generateOntology = new GenerateOntology();
        // 命名空间前缀（域名 + /ontology/）
        generateOntology.setBase("https://semweb.scbg.ac.cn/ontology/");
        // 类，表名
        generateOntology.setClassList(null);
        // 属性，字段名，中间可以有空格
        generateOntology.setDataTypeList(Arrays.asList(
                // 广东植物志电子版 DGBotData
                "Latin", "sLatin", "CName", "ONameC", "phylum", "family", "genus", "phylumc", "familyc", "genusc", "feature", "origin", "dis", "used",
                // 中国能源植物数据集 EPData
                "collectionDate", "collectionPerson", "collectionLocation", "longitude", "latitude", "altitude", "chinese", "alias", "latin", "family", "collectionPart", "lifeForm", "habitat", "lightExposure", "slopeDirection", "soil", "topography", "associatedPlants", "flowerColor", "fruitColor", "seedColor", "root", "stem", "leaf", "flowerPeriod", "germinationPeriod", "freshWeight", "dryWeight", "unitAreaQuantity", "distribution", "unitAreaYield", "individualYield", "oilContent", "use",
                // 东莞市林下经济植物名录 DGEcoPData
                "Chinese Name", "Latin", "Gensp", "Family", "Genus", "Common_Name", "Description", "Uses"
        ));
        // 属性名称list，可为空。若为空，则属性名称为字段名称；若不为空，则需与属性（dataTypeList）顺序和个数完全一致，缺一不可
        generateOntology.setDataTypeNameList(Arrays.asList(
                // 广东植物志电子版 DGBotData
                "完整拉丁名", "拉丁名", "中文名", "中文别名", "门（英文）", "科（英文）", "属（英文）", "门（中文）", "科（中文）", "属（中文）", "形态特征", "生长环境", "分布地", "用途",
                // 中国能源植物数据集 EPData
                "采集日期", "采集人员", "采集地点", "经度", "纬度", "海拔", "中文名", "别名", "拉丁名", "科名", "采集部位", "生活型", "生境", "光照", "坡向", "土壤", "地形", "伴生植物", "花色", "果色", "种子颜色", "根", "茎", "叶", "花期", "萌芽期", "湿重", "干重", "单位面积数量", "分布情况", "单位面积产量大", "单株产量", "油脂含量", "用途",
                // 东莞市林下经济植物名录 DGEcoPData
                "中文名称", "完整拉丁名", "拉丁名", "科", "属", "别名", "描述", "用途"
        ));
        // 文件名路径，文件名一般为："机构简称"+Ontology+".owl"
        generateOntology.setFilePath("D:\\临时文件\\scbgOntology.owl");
        // 生成本体调用原来的方法
//        new CreateRdfServiceImpl().generateOntology(generateOntology);

        // 将本体属性输出，可以直接复制到EXCEL中
        printExcelContent(generateOntology);
    }


    /**
     * 使用globi关系，生成千金藤属本体
     */
    public static void generateOntologyForQianjinteng() {
        // 封装生成本体所需要的参数
        GenerateOntology generateOntology = new GenerateOntology();
        // 命名空间前缀
        generateOntology.setBase("http://ioz.semweb.csdb.cn/ontology/");
        // 类
        generateOntology.setClassList(Arrays.asList());
        // 对象属性
        generateOntology.setObjectList(Arrays.asList("eatenBy", "hostOf", "interactsWith"));
        // 数据属性
        generateOntology.setDataTypeList(Arrays.asList());
        // 文件名路径，文件名一般为："机构简称"+Ontology+".ttl"
        generateOntology.setFilePath("E:\\TSTOR\\animal\\qianjintengOntology.ttl");
        // 生成本体调用原来的方法
        new CreateRdfServiceImpl().generateOntology(generateOntology);
    }

    /**
     * 生成动物所使用gloBI关系生成的本体
     */
    public static void generateOntologyForIoz() {
        // 封装生成本体所需要的参数
        GenerateOntology generateOntology = new GenerateOntology();
        // 命名空间前缀
        generateOntology.setBase("http://ioz.semweb.csdb.cn/ontology/");
        // 类
        generateOntology.setClassList(Arrays.asList());
        // 对象属性
        generateOntology.setObjectList(Arrays.asList("eatenBy", "eats", "hostOf", "interactsWith", "preysOn"));
        // 数据属性
        generateOntology.setDataTypeList(Arrays.asList());
        // 文件名路径，文件名一般为："机构简称"+Ontology+".ttl"
        generateOntology.setFilePath("E:\\TSTOR\\animal\\iozOntology.ttl");
        // 生成本体调用原来的方法
        new CreateRdfServiceImpl().generateOntology(generateOntology);
    }

    /**
     * 自动生成上海有机所本体
     *
     * @Author 陈锟
     * @since 2023年5月23日14:39:51
     */
    public static void generateOntologyForOrganchem() {
        // 封装生成本体所需要的参数
        GenerateOntology generateOntology = new GenerateOntology();
        // 命名空间前缀
        generateOntology.setBase("http://organchem.semweb.csdb.cn/ontology/");
        // 类
        generateOntology.setClassList(Arrays.asList("Reference"));
        // 对象属性
        generateOntology.setObjectList(Arrays.asList());
        // 数据属性
        generateOntology.setDataTypeList(Arrays.asList(
                // 化合物 Chemical
                "chemID", "CAS", "Mf",
                // 药物 Medicine
                "medicineID", "introduction", "medicamentType", "toxicity", "usage", "Disease", "dosage", "AdverseReact", "physicochemicalProP", "reference",
                // 天然产物 Savageness
                "GeneralStatement", "usage", "HazardToxicity", "MolecularWeight", "BoilingPoint", "Synthesis", "DevelopmentStatus", "PhysiDescription", "Solubility", "MeltingPoint", "DissociationConstant", "OpticalRotation", "PartitionCoefficient", "OtherData", "RTECSAccessionno", "Suppliers", "reference", "Density",
                // 文献 Reference
                "chem", "ID", "chem_ID"
        ));
        // 文件名路径，文件名一般为："机构简称"+Ontology+".owl"
        generateOntology.setFilePath("D:\\临时文件\\organchemOntology.owl");
        // 生成本体调用原来的方法
        new CreateRdfServiceImpl().generateOntology(generateOntology);
    }

    @Override
    public String generateOntology(GenerateOntology generateOntology) {
        String message = "";
        try {
            String base = generateOntology.getBase();
            Model model = ModelFactory.createDefaultModel();
            Map<String ,String> nsMap = new HashMap<>();
            nsMap.put("rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#");
            nsMap.put("rdfs","http://www.w3.org/2000/01/rdf-schema#");
            nsMap.put("owl","http://www.w3.org/2002/07/owl#");
            model.setNsPrefixes(nsMap);
            List<Statement> statements = new LinkedList<>();
            List<String> classList = generateOntology.getClassList();
            if(null != classList && classList.size()>0){
                for (String claes: classList) {
                    String subject =base+claes.replaceAll(" ","_");
                    statements.add(new StatementImpl(new ResourceImpl(subject), RDF.type, OWL.Class));
                    statements.add(new StatementImpl(new ResourceImpl( subject), RDFS.label, ResourceFactory.createLangLiteral(claes, Objects.requireNonNull(StringUtil.getCodeName(claes)).get(0))));
                }
            }
            List<String> dataTypeList =generateOntology.getDataTypeList();
            if(null != dataTypeList && dataTypeList.size()>0 ){
                for (int i = 0; i < dataTypeList.size(); i++) {
                    String dataType = dataTypeList.get(i);
                    String subject = base+dataType.replaceAll(" ","_");
                    statements.add(new StatementImpl(new ResourceImpl(subject), RDF.type, OWL.DatatypeProperty));
                    // 属性名称优先使用录入的中文名字字段，若未录入则使用字段名作为属性名称
                    String propertyName;
                    if (generateOntology.getDataTypeNameList() != null && generateOntology.getDataTypeNameList().size() > 0) {
                        propertyName = generateOntology.getDataTypeNameList().get(i);
                    } else {
                        propertyName = dataType;
                    }
                    statements.add(new StatementImpl(new ResourceImpl(subject), RDFS.label, ResourceFactory.createLangLiteral(propertyName, Objects.requireNonNull(StringUtil.getCodeName(propertyName)).get(0))));
                }
            }
            List<String> objectList =generateOntology.getObjectList();
            if(null != objectList && objectList.size()>0 ){
                for (String object: objectList) {
                    String subject = base+object.replaceAll(" ","_");
                    statements.add(new StatementImpl(new ResourceImpl(subject), RDF.type, OWL.ObjectProperty));
                    statements.add(new StatementImpl(new ResourceImpl(subject), RDFS.label, ResourceFactory.createLangLiteral(object, Objects.requireNonNull(StringUtil.getCodeName(object)).get(0))));
                }
            }
            model.add(statements);
            String filePathSave = generateOntology.getFilePath();
            File file = new File(filePathSave);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            RDFDataMgr.write(new FileOutputStream(filePathSave), model, RDFFormat.TTL);
            message ="生成完成";
        }catch (Exception e){
            e.printStackTrace();
            return e.getMessage();
        }
        return message;
    }

    /**
     * 将本体属性输出，可以直接复制到EXCEL中
     *
     * @Author 陈锟
     * @since 2023年10月18日15:25:29
     */
    public static void printExcelContent(GenerateOntology generateOntology) {
        // 输出属性
        if (generateOntology.getDataTypeList() != null && generateOntology.getDataTypeList().size() > 0) {
            List<String> containsPropertyList = new ArrayList<>();
            for (int i = 0; i < generateOntology.getDataTypeList().size(); i++) {
                // 属性字段名
                String property = generateOntology.getDataTypeList().get(i);
                // 将空格替换为下划线
                property = property.replaceAll(" ","_");
                // 属性中文名
                String propertyName = generateOntology.getDataTypeNameList().get(i);
                // 去重
                if (containsPropertyList.contains(property)) {
                    continue;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder = stringBuilder.append("华南植物园\t");
                stringBuilder = stringBuilder.append(propertyName).append("\t");
                stringBuilder = stringBuilder.append("\t");
                stringBuilder = stringBuilder.append(generateOntology.getBase()).append(property).append("\t");
                stringBuilder = stringBuilder.append("是");
                System.out.println(stringBuilder);
                containsPropertyList.add(property);
            }
        }
    }
}

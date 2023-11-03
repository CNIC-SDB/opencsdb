package com.linkeddata.portal.script.pubchem;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.linkeddata.portal.script.pubchem.entity.Compound;
import com.linkeddata.portal.script.pubchem.entity.Synonym;
import com.linkeddata.portal.utils.RdfUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtModel;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 下载pubchem中的化合物信息，补充到过程所
 *
 * @author 陈锟
 * @since 2023年6月6日10:46:01
 */
@Slf4j
@Service
public class DownloadPubchemService {
    @Resource
    private MongoTemplate mongoTemplate;

    /**
     * 下载pubchem中的化合物信息（手动多线程，为了可控）
     * @param threadName
     * @param tableName
     * @param threadStartCid
     * @param threadEndCid
     *
     * @author 陈锟
     * @since 2023年6月26日14:46:06
     */
    public void downloadPubchemCompounds(int threadName, int tableName, int threadStartCid, int threadEndCid) {
        // 每次请求中包含多少个化合物，由于URL长度有限制，一次最多请求350个化合物
        int reqGroupNum = 350;
        // 记录开始时间，用于在执行过程中输出用时
        Date startDate = new Date();

        System.out.println("线程：" + threadName + "，已启动，threadStartCid=" + threadStartCid + "，threadEndCid=" + threadStartCid);
        // 每组x个，放一个请求里执行
        for (int i = threadStartCid; i <= threadEndCid; i += reqGroupNum) {
            int startCid = i;
            int endCid = Math.min(i + reqGroupNum - 1, threadEndCid);
            // 记录请求成功与否
            boolean resultProperty = false;
            boolean resultSynonym = false;

            Date date = new Date();
            // 防止某个请求异常而导致整个线程异常
            try {
                // 拼接多个CID的字符串，下面多个请求中都会用到
                String cidStr = "";

                for (int cid = startCid; cid <= endCid; cid++) {
                    cidStr += cid;
                    if (cid < endCid) cidStr += ",";
                }

                /**
                 * 发送请求，获取化合物的信息（除了CAS、synonyms以外的），https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/cid/1,2,3/property/IUPACName,MolecularFormula,MolecularWeight,InChIKey,InChI,CanonicalSMILES/JSON
                 */
                String propertyUrl = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/cid/" + cidStr + "/property/IUPACName,MolecularFormula,MolecularWeight,InChIKey,InChI,CanonicalSMILES/JSON";
                JsonObject propertyResponse = getJsonResponse(propertyUrl);
                if (propertyResponse != null && propertyResponse.getAsJsonObject("PropertyTable") != null) {
                    JsonArray propertyJsonArray = propertyResponse.getAsJsonObject("PropertyTable").getAsJsonArray("Properties");
                    List<Compound> compoundList = new ArrayList<>();
                    for (int j = 0; j < propertyJsonArray.size(); j++) {
                        JsonObject jsonObject = propertyJsonArray.get(j).getAsJsonObject();
                        Compound compound = new Compound();
                        if(jsonObject.get("CID") != null) compound.setCid(jsonObject.get("CID").getAsInt());
                        if(jsonObject.get("MolecularFormula") != null) compound.setMolecularFormula(jsonObject.get("MolecularFormula").getAsString());
                        if(jsonObject.get("MolecularWeight") != null) compound.setMolecularWeight(Double.valueOf(jsonObject.get("MolecularWeight").getAsString()));
                        if(jsonObject.get("CanonicalSMILES") != null) compound.setSmiles(jsonObject.get("CanonicalSMILES").getAsString());
                        if(jsonObject.get("InChI") != null) compound.setInChI(jsonObject.get("InChI").getAsString());
                        if(jsonObject.get("InChIKey") != null) compound.setInChIKey(jsonObject.get("InChIKey").getAsString());
                        if(jsonObject.get("IUPACName") != null) compound.setIupacName(jsonObject.get("IUPACName").getAsString());
                        compoundList.add(compound);
                    }
                    // 批量保存到mongo
                    batchInsertCompound(compoundList, tableName);
                    // 进行到此处则说明请求成功
                    resultProperty = true;
                }

                /**
                 * 发送请求，获取synonyms别名，一对多，https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/cid/1,2,3/synonyms/JSON
                 */
                String synonymUrl = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/cid/" + cidStr + "/synonyms/JSON";
                JsonObject synonymResponse = getJsonResponse(synonymUrl);
                if (synonymResponse != null && synonymResponse.getAsJsonObject("InformationList") != null) {
                    JsonArray synonymJsonArray = synonymResponse.getAsJsonObject("InformationList").getAsJsonArray("Information");
                    List<Synonym> synonymList = new ArrayList<>();
                    for (int j = 0; j < synonymJsonArray.size(); j++) {
                        JsonObject jsonObject = synonymJsonArray.get(j).getAsJsonObject();
                        int cid = jsonObject.get("CID").getAsInt();
                        JsonArray valueJsonArray = jsonObject.getAsJsonArray("Synonym");
                        // n为计数器，别名太多了，最多取前5个别名
                        if (valueJsonArray != null) {
                            for (int k = 0, n = 0; k < valueJsonArray.size() && n < 5; k++) {
                                String value = valueJsonArray.get(k).getAsString();
                                // 去除纯数字，例如：7732-18-5
                                // 去除包含连续三个数字，例如：SCHEMBL3259109
                                // mysql中varchar字段最大长度为255，超出则会存不下
                                if (value.matches("[0-9\\-]+") || value.matches(".*\\d{3}.*") || value.length() >= 255) {
                                    continue;
                                }

                                Synonym synonym = new Synonym();
                                synonym.setCid(cid);
                                synonym.setSynonym(value);
                                synonymList.add(synonym);
                                n++;
                            }
                        }
                    }
                    // 批量保存到mongo
                    batchInsertSynonym(synonymList, tableName);
                    // 进行到此处则说明请求成功
                    resultSynonym = true;
                }
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                Date date2 = new Date();
                long execSeconds = (date2.getTime() - date.getTime())/1000 + 1;
                long totalSeconds = (date2.getTime() - startDate.getTime())/1000 + 1;
                long remainingTime = Double.valueOf(Double.valueOf(((threadEndCid - endCid + 1) * totalSeconds)) / (endCid - threadStartCid + 1) / 60 + 1).longValue();
                // 在控制台输出日志
                System.out.println("线程：" + threadName +
                        "，进度：" + (endCid - threadStartCid + 1) + "/" + (threadEndCid - threadStartCid + 1) +
                        "，用时：" + (totalSeconds / 60 + 1) + "分钟" +
                        "，预估剩余时间：" + remainingTime + "分钟" +
                        "，本请求用时：" + execSeconds + "秒");

                // 保存日志
                DownloadPubchemLog downloadPubchemLog = new DownloadPubchemLog();
                downloadPubchemLog.setThreadName(threadName);
                downloadPubchemLog.setStartCid(startCid);
                downloadPubchemLog.setEndCid(endCid);
                downloadPubchemLog.setResultProperty(resultProperty);
                downloadPubchemLog.setResultSynonym(resultSynonym);
                downloadPubchemLog.setExecSeconds(execSeconds);
                downloadPubchemLog.setCreateDate(new Date());
                mongoTemplate.save(downloadPubchemLog);
            }
        }

//        /**
//         * 发送请求，获取cas，分页查，https://pubchem.ncbi.nlm.nih.gov/rest/pug_view/annotations/heading/CAS/JSON?page=1
//         */
//        String casUrl = "https://pubchem.ncbi.nlm.nih.gov/rest/pug_view/annotations/heading/CAS/JSON?page=1";
//        JsonObject casResponse = getJsonResponse(casUrl);
//        JsonArray casJsonArray = casResponse.getAsJsonObject("Annotations").getAsJsonArray("Annotation");
//        List<Cas> casList = new ArrayList<>();
//        for (int i = 0; i < casJsonArray.size(); i++) {
//            JsonObject jsonObject = casJsonArray.get(i).getAsJsonObject();
//            JsonArray cidJsonArray = jsonObject.get("LinkedRecords").getAsJsonObject().get("CID").getAsJsonArray();
//            jsonObject.get("Data").getAsJsonArray().get(0).getAsJsonObject().get("Value").getAsJsonObject().get("StringWithMarkup").getAsJsonArray();
//
//            JsonArray valueJsonArray = jsonObject.getAsJsonArray("Synonym");
//            // n为计数器，别名太多了，最多取前5个别名
//            for (int j = 0, n = 0; j < valueJsonArray.size() && n < 5; j++) {
//                String value = valueJsonArray.get(j).getAsString();
//                // 去除纯数字
//                if(!value.matches("[0-9\\-]+")) {
//                    Synonym synonym = new Synonym();
//                    synonym.setCid(cid);
//                    synonym.setSynonym(value);
//                    casList.add(synonym);
//                    n++;
//                }
//            }
//            for (int j = 0; j < cidJsonArray.size(); j++) {
//
//            }
//        }
//        // 保存到mysql
//        batchInsertSynonym(casList);
    }

    /**
     * 批量保存Compound
     * @param compoundList
     */
    public void batchInsertCompound(List<Compound> compoundList, int tableName) {
        // 先批量保存到mongodb中，后面再考虑mysql分库分表
        mongoTemplate.insert(compoundList, "compound_" + tableName);

//        String INSERT_SQL = "INSERT INTO compound_" + threadName + "(cid, iupac_name, molecular_formula, molecular_weight, inchikey, inchi, smiles) VALUES (?, ?, ?, ?, ?, ?, ?)";
//        jdbcTemplate.batchUpdate(INSERT_SQL, new BatchPreparedStatementSetter() {
//            @Override
//            public void setValues(PreparedStatement ps, int i) throws SQLException {
//                Compound compound = compoundList.get(i);
//                ps.setInt(1, compound.getCid());
//                ps.setString(2, compound.getIupacName());
//                ps.setString(3, compound.getMolecularFormula());
//                ps.setDouble(4, compound.getMolecularWeight());
//                ps.setString(5, compound.getInChIKey());
//                ps.setString(6, compound.getInChI());
//                ps.setString(7, compound.getSmiles());
//            }
//
//            @Override
//            public int getBatchSize() {
//                return compoundList.size();
//            }
//        });
    }

    /**
     * 批量保存Synonym
     * @param synonymList
     */
    public void batchInsertSynonym(List<Synonym> synonymList, int tableName) {
        // 先批量保存到mongodb中，后面再考虑mysql分库分表
        mongoTemplate.insert(synonymList, "synonym_" + tableName);

//        String INSERT_SQL = "INSERT INTO synonym_" + threadName + "(cid, synonym) VALUES (?, ?)";
//        jdbcTemplate.batchUpdate(INSERT_SQL, new BatchPreparedStatementSetter() {
//            @Override
//            public void setValues(PreparedStatement ps, int i) throws SQLException {
//                Synonym synonym = synonymList.get(i);
//                ps.setInt(1, synonym.getCid());
//                ps.setString(2, synonym.getSynonym());
//            }
//
//            @Override
//            public int getBatchSize() {
//                return synonymList.size();
//            }
//        });
    }

    /**
     * 获取URL对应的JSON返回值
     *
     * @param url
     * @return
     */
    public static JsonObject getJsonResponse(String url) {
        boolean result = false;
        JsonObject jsonObject = null;
        try {
            // 创建URL对象、HttpURLConnection对象
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            // 发送请求
            conn.connect();
            // 获取响应状态码
            if (conn.getResponseCode() == 200) {
                // 获取响应内容
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                StringBuffer response = new StringBuffer();
                while ((line = in.readLine()) != null) response.append(line);
                in.close();
                // 将响应内容转化为map
                jsonObject = new Gson().fromJson(response.toString(), JsonObject.class);
                return jsonObject;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 下载pubchem中额外的化合物信息，补充到过程所（不含2D、3D相似）
     *
     * @author 陈锟
     * @since 2023年6月7日18:51:31
     */
    public static void addAdditionalFieldFromPubchemByVirtuoso() {
        /**
         * 后期若复用此脚本则需要修改的参数
         */
        // 临时存储三元组的virtuoso信息
        String virtuoso_host = "jdbc:virtuoso://10.0.89.33:1111";
        String virtuoso_user = "dba";
        String virtuoso_password = "0dabigta1357";
        String graphName = "http://pubchem-additionalField";
        String virtuosoSparqlEndpoint = "http://10.0.89.33:8890/sparql";
        // 过程所SAPRQL端点
        String chemSparqlEndpoint = "http://chemdb.semweb.csdb.cn/sparql";

        // 记录起始时间
        Date startDate = new Date();

        /**
         * 连接测试virtuoso，临时存储生成的三元组
         */
        VirtGraph graphVirt = new VirtGraph(graphName, virtuoso_host, virtuoso_user, virtuoso_password);
        VirtModel model = new VirtModel(graphVirt);

        /**
         * 查询过程所中，所有与pubchem存在关系的化合物，共 378780 条
         */
        long relationCount = RdfUtils.countTriple(chemSparqlEndpoint, "PREFIX obo: <http://purl.obolibrary.org/obo/>\n" +
                "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                "SELECT (COUNT(DISTINCT ?chem) AS ?count) WHERE {\n" +
                "  ?chem owl:sameAs ?pubchem .\n" +
                "  ?chem a obo:CHEBI_24431 .\n" +
                "  FILTER ( STRSTARTS(STR(?pubchem), 'http://rdf.ncbi.nlm.nih.gov/pubchem/compound/CID') )\n" +
                "}");

        /**
         * 在过程所中，查询化合物URI和pubchem URI；
         * 根据pubchem URI获取CID并调用pubchem在线接口；
         * 将对应字段封装成RDF三元组；
         * 每次批量查N条，往virtuoso中批量保存N条；
         */
        for (int offset = 0, limit = 500; offset < relationCount; offset += limit) {
            // 存放每次批量插入的三元组
            List<Statement> statementList = new ArrayList<>();

            // SPARQL语句，查询过程所化合物URI和pubchem URI
            ResultSet resultSet = RdfUtils.queryTriple(chemSparqlEndpoint, "PREFIX obo: <http://purl.obolibrary.org/obo/>\n" +
                    "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                    "SELECT ?chem (MAX(?pubchemTemp) AS ?pubchem) WHERE {\n" +
                    "  ?chem owl:sameAs ?pubchemTemp .\n" +
                    "  ?chem a obo:CHEBI_24431 .\n" +
                    "  FILTER ( STRSTARTS(STR(?pubchemTemp), 'http://rdf.ncbi.nlm.nih.gov/pubchem/compound/CID') )\n" +
                    "}\n" +
                    "GROUP BY ?chem\n" +
                    "OFFSET " + offset + " LIMIT " + limit);

            // 循环所有组合，生成每个组合对应的三元组
            while (resultSet.hasNext()) {
                /**
                 * 获取 过程所化合物URI、pubchem化合物CID值
                 */
                QuerySolution solution = resultSet.nextSolution();
                // ?chem，例如：http://chemdb.semweb.csdb.cn/resource/Compound_7732-18-5
                String chemUri = solution.getResource("chem").getURI();
                // ?pubchem，例如：http://rdf.ncbi.nlm.nih.gov/pubchem/compound/CID962
                String pubchemUri = solution.getResource("pubchem").asResource().getURI();
                // 截取pubchem CID值
                String pubchemCid = pubchemUri.replace("http://rdf.ncbi.nlm.nih.gov/pubchem/compound/CID", "");

                /**
                 * 校验化合物有没有导入过，若导入过则不再重复导入；
                 * 此代码在首次执行时注释，在补充执行时放开；
                 */
                if (RdfUtils.countTriple(virtuosoSparqlEndpoint + "?default-graph-uri=" + graphName,
                        "SELECT (COUNT(*) AS ?count) WHERE { <" + chemUri + "> ?p ?o . }") > 0) continue;

                /**
                 * 根据 pubchem化合物CID值，调用pubchem在线接口，获取与页面上一致的json信息；
                 */
                JsonObject pubchemJson;
                try {
                    // 创建URL对象、HttpURLConnection对象
                    // 接口样例：https://pubchem.ncbi.nlm.nih.gov/rest/pug_view/data/compound/962/JSON/?response_type=display
                    HttpURLConnection conn = (HttpURLConnection) new URL("https://pubchem.ncbi.nlm.nih.gov/rest/pug_view/data/compound/" + pubchemCid + "/JSON/?response_type=display").openConnection();
                    // 发送请求
                    conn.connect();
                    // 获取响应状态码
                    if (conn.getResponseCode() == 200) {
                        // 获取响应内容
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String line;
                        StringBuffer response = new StringBuffer();
                        while ((line = in.readLine()) != null) response.append(line);
                        in.close();
                        // 将响应内容转化为map
                        pubchemJson = new Gson().fromJson(response.toString(), JsonObject.class);
                        // 从pubchemJson中提取相应字段，注意多层嵌套
                        String IUPAC_name = "";
                        String InChI = "";
                        List<String> SynonymsList = new ArrayList<>();
                        for (JsonElement sectionElement : pubchemJson.getAsJsonObject("Record").getAsJsonArray("Section")) {
                            JsonObject section = sectionElement.getAsJsonObject();
                            String TOCHeading = section.getAsJsonObject().get("TOCHeading").getAsString();
                            if ("Names and Identifiers".equals(TOCHeading)) {
                                for (JsonElement section2Element : section.getAsJsonObject().getAsJsonArray("Section")) {
                                    JsonObject section2 = section2Element.getAsJsonObject();
                                    String TOCHeading2 = section2.getAsJsonObject().get("TOCHeading").getAsString();
                                    if("Computed Descriptors".equals(TOCHeading2)){
                                        for (JsonElement section3Element : section2.getAsJsonObject().getAsJsonArray("Section")) {
                                            JsonObject section3 = section3Element.getAsJsonObject();
                                            String TOCHeading3 = section3.getAsJsonObject().get("TOCHeading").getAsString();
                                            if ("IUPAC Name".equals(TOCHeading3)) {
                                                IUPAC_name = section3.getAsJsonObject().getAsJsonArray("Information").get(0).getAsJsonObject()
                                                        .getAsJsonObject("Value").getAsJsonArray("StringWithMarkup").get(0).getAsJsonObject()
                                                        .get("String").getAsString();
                                            } else if ("InChI".equals(TOCHeading3)) {
                                                InChI = section3.getAsJsonObject().getAsJsonArray("Information").get(0).getAsJsonObject()
                                                        .getAsJsonObject("Value").getAsJsonArray("StringWithMarkup").get(0).getAsJsonObject()
                                                        .get("String").getAsString();
                                            }
                                        }
                                    }
                                    if ("Synonyms".equals(TOCHeading2)) {
                                        for (JsonElement section3Element : section2.getAsJsonObject().getAsJsonArray("Section")) {
                                            JsonObject section3 = section3Element.getAsJsonObject();
                                            String TOCHeading3 = section3.getAsJsonObject().get("TOCHeading").getAsString();
                                            if ("Depositor-Supplied Synonyms".equals(TOCHeading3)) {
                                                for (JsonElement synonymsElement : section3.getAsJsonObject().getAsJsonArray("Information").get(0).getAsJsonObject().getAsJsonObject("Value").getAsJsonArray("StringWithMarkup")) {
                                                    JsonObject synonyms = synonymsElement.getAsJsonObject();
                                                    // 与页面上展示一致，最多取5个别名，不然太多了
                                                    if (SynonymsList.size() < 5) {
                                                        SynonymsList.add(synonyms.get("String").getAsString());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        /**
                         * 生成RDF三元组，添加到statementList
                         */
                        // IUPAC_name，位置：Information
                        statementList.add(new StatementImpl(new ResourceImpl(chemUri), new PropertyImpl("http://semanticscience.org/resource/CHEMINF_000107"), ResourceFactory.createTypedLiteral(IUPAC_name)));
                        statementList.add(new StatementImpl(new ResourceImpl(chemUri), RDFS.label, ResourceFactory.createTypedLiteral(IUPAC_name)));
                        // InChI
                        statementList.add(new StatementImpl(new ResourceImpl(chemUri), new PropertyImpl("http://semanticscience.org/resource/SIO_001387"), ResourceFactory.createTypedLiteral(InChI)));
                        // Synonyms，别名可能有多个
                        SynonymsList.forEach(synonym -> {
                            statementList.add(new StatementImpl(new ResourceImpl(chemUri), new PropertyImpl("http://purl.obolibrary.org/obo/NCIT_C52469"), ResourceFactory.createTypedLiteral(synonym)));
                            statementList.add(new StatementImpl(new ResourceImpl(chemUri), RDFS.label, ResourceFactory.createTypedLiteral(synonym)));
                        });
                        // mol_2d_img，例如：https://pubchem.ncbi.nlm.nih.gov/image/imgsrv.fcgi?cid=962
                        statementList.add(new StatementImpl(new ResourceImpl(chemUri), new PropertyImpl("https://schema.org/image"), ResourceFactory.createTypedLiteral("https://pubchem.ncbi.nlm.nih.gov/image/imgsrv.fcgi?cid=" + pubchemCid)));
                        // mol_3d_img，例如：https://pubchem.ncbi.nlm.nih.gov/image/img3d.cgi?&cid=962
                        statementList.add(new StatementImpl(new ResourceImpl(chemUri), new PropertyImpl("https://schema.org/image"), ResourceFactory.createTypedLiteral("https://pubchem.ncbi.nlm.nih.gov/image/img3d.cgi?&cid=" + pubchemCid)));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            /**
             * 将生成的RDF三元组批量保存到virtuoso中
             */
            model.add(statementList);
            /**
             * 记录当前进度
             */
            System.out.println("下载pubchem中额外的化合物信息 - 当前进度：" + Math.min(offset + limit, relationCount) + "/" + relationCount + "，当前用时：" + ((new Date().getTime() - startDate.getTime()) / 1000 / 60 + 1) + "分钟");
        }
        graphVirt.close();
    }

    /**
     * 下载pubchem中化合物之间的2D、3D相似关系，补充到过程所，仅补充过程所中有的化合物
     *
     * @param start 起始下标，可为空
     * @param end 结尾下标，可为空
     * @author 陈锟
     * @since 2023年6月8日15:04:31
     */
    public static void add2dAnd3dFromPubchemByVirtuoso(Integer start, Integer end) {
        /**
         * 后期若复用此脚本则需要修改的参数
         */
        // 临时存储三元组的virtuoso信息
        String virtuoso_host = "jdbc:virtuoso://10.0.89.33:1111";
        String virtuoso_user = "dba";
        String virtuoso_password = "0dabigta1357";
        String graphName = "http://pubchem-add2dAnd3d-" + start;
        // 过程所SAPRQL端点
        String chemSparqlEndpoint = "http://chemdb.semweb.csdb.cn/sparql";

        // 记录起始时间
        Date startDate = new Date();

        /**
         * 连接测试virtuoso，临时存储生成的三元组
         */
        VirtGraph graphVirt = new VirtGraph(graphName, virtuoso_host, virtuoso_user, virtuoso_password);
        VirtModel model = new VirtModel(graphVirt);

        /**
         * 查询过程所中所有与pubchem存在关联的化合物数量，共 378780 条
         */
        long relationCount = end != null ? end : RdfUtils.countTriple(chemSparqlEndpoint, "PREFIX obo: <http://purl.obolibrary.org/obo/>\n" +
                "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                "SELECT (COUNT(DISTINCT ?chem) AS ?count) WHERE {\n" +
                "  ?chem owl:sameAs ?pubchem .\n" +
                "  ?chem a obo:CHEBI_24431 .\n" +
                "  FILTER ( STRSTARTS(STR(?pubchem), 'http://rdf.ncbi.nlm.nih.gov/pubchem/compound/CID') )\n" +
                "}");

        /**
         * 在过程所中，查询与化合物存在2D/3D相似关系的其他化合物；
         * 每次批量查N条，往virtuoso中批量保存N条；
         */
        for (int offset = start, limit = Math.min(200, Long.valueOf(relationCount).intValue()); offset < relationCount; offset += limit) {
            // 存放每次批量插入的三元组
            List<Statement> statementList = new ArrayList<>();

            // SPARQL语句，查询过程所中所有与pubchem存在关联的化合物URI
            ResultSet resultSet = RdfUtils.queryTriple(chemSparqlEndpoint, "PREFIX obo: <http://purl.obolibrary.org/obo/>\n" +
                    "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                    "SELECT ?chem (MAX(?pubchemTemp) AS ?pubchem) WHERE {\n" +
                    "  ?chem owl:sameAs ?pubchemTemp .\n" +
                    "  ?chem a obo:CHEBI_24431 .\n" +
                    "  FILTER ( STRSTARTS(STR(?pubchemTemp), 'http://rdf.ncbi.nlm.nih.gov/pubchem/compound/CID') )\n" +
                    "}\n" +
                    "GROUP BY ?chem\n" +
                    "OFFSET " + offset + " LIMIT " + limit);

            // 循环所有组合，生成每个组合对应的三元组
            while (resultSet.hasNext()) {
                /**
                 * 获取 过程所化合物URI、pubchem化合物URI
                 */
                QuerySolution solution = resultSet.nextSolution();
                // ?chem，例如：http://chemdb.semweb.csdb.cn/resource/Compound_7732-18-5
                String chemUri = solution.getResource("chem").getURI();
                // ?pubchem，例如：http://rdf.ncbi.nlm.nih.gov/pubchem/compound/CID962
                String pubchemUri = solution.getResource("pubchem").asResource().getURI();
                // 截取pubchem CID值
                String pubchemCid = pubchemUri.replace("http://rdf.ncbi.nlm.nih.gov/pubchem/compound/CID", "");

                /**
                 * 查询pubchem中化合物之间的2D、3D相似关系
                 * 2D/3D相似关系在线接口样例：https://pubchem.ncbi.nlm.nih.gov/rest/rdf/compound/CID962/nbr.nt
                 * 接口拼接规则：pubchem化合物URI + "/nbr.nt"
                 */
                String nbrResponse = PubchemHttpUtil.doGet("https://pubchem.ncbi.nlm.nih.gov/rest/rdf/compound/CID" + pubchemCid + "/nbr.nt");
                Model tempModel = ModelFactory.createDefaultModel();
                if (StringUtils.isBlank(nbrResponse)) continue;
                try {
                    InputStream inputStream = new ByteArrayInputStream(nbrResponse.getBytes());
                    tempModel.read(inputStream, null, Lang.NTRIPLES.getLabel());
                    inputStream.close();
                } catch (Throwable e) {

                }

                /**
                 * pubchem中2D/3D相似关系存在了一起，因此遍历返回的所有三元组，区分2D/3D相似关系
                 */
                StmtIterator stmtIterator = tempModel.listStatements();
                while (stmtIterator.hasNext()) {
                    Statement statement = stmtIterator.nextStatement();
                    String preUri = statement.getPredicate().getURI();
                    String objUri = statement.getObject().asResource().getURI();

                    ResultSet tempResultSet = RdfUtils.queryTriple("http://chemdb.semweb.csdb.cn/sparql",
                            "select ?chem where { ?chem <" + OWL2.sameAs.getURI() + "> <" + objUri + "> }");
                    /**
                     * 根据pubchem URI查询化合物CAS号（通过过程所中的owl:sameAs关系来查）
                     */
                    while (tempResultSet.hasNext()) {
                        QuerySolution querySolution = tempResultSet.next();
                        String targetCas = querySolution.getResource("chem").getURI().replace("http://chemdb.semweb.csdb.cn/resource/Compound_", "");

                        /**
                         * 生成2D/3D相似关系的三元组
                         */
                        if ("http://semanticscience.org/resource/CHEMINF_000482".equals(preUri)) {
                            statementList.add(new StatementImpl(new ResourceImpl(chemUri), new PropertyImpl("http://semanticscience.org/resource/CHEMINF_000482"), new ResourceImpl(targetCas)));
                        } else if ("http://semanticscience.org/resource/CHEMINF_000483".equals(preUri)) {
                            statementList.add(new StatementImpl(new ResourceImpl(chemUri), new PropertyImpl("http://semanticscience.org/resource/CHEMINF_000483"), new ResourceImpl(targetCas)));
                        }
                    }
                }
            }
            /**
             * 将生成的RDF三元组批量保存到virtuoso中
             */
            model.add(statementList);
            /**
             * 记录当前进度
             */
            System.out.println("下载pubchem中化合物之间的2D、3D相似关系 - 当前进度：" + Math.min(offset + limit, relationCount) + "/" + relationCount + "，当前用时：" + ((new Date().getTime() - startDate.getTime()) / 1000 / 60 + 1) + "分钟");
        }
        graphVirt.close();
    }

}

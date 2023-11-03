package com.linkeddata.portal.script;

import com.google.gson.JsonElement;
import com.linkeddata.portal.utils.HttpsTrustClient;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtModel;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import java.io.*;
import java.net.URL;
@Slf4j
@RestController
@Api(tags = "生成pubmed资源文件")
public class SavePubmedDate {
    public static void main(String[] args) throws Exception {
        SavePubmedDate savePubmedDate = new SavePubmedDate();
        savePubmedDate.dealPubmedData("2021");
//        savePubmedDate.dealPubmedData("2022");






    }
    @ApiOperation(value = "生成pubmed资源文件")
    @GetMapping("/createPubmedDate/{year}")
    public String dealPubmedData(@PathVariable("year") String year) {
        String basicurl = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/";
        String content = "SARS-CoV-2[mesh]+AND+COVID-19[mesh]";
        String appKey = "47bf50eea5f9bba76e92b32622442cae1d09";
        String mindate = year + "/1/1";
        String maxdate = year + "/2/1";
        for (int i = 1; i <= 12; i++) {
            if (i == 1) {
                mindate = year + "/" + String.valueOf(i) + "/" + "1";
            } else {
                mindate = year + "/" + String.valueOf(i) + "/" + "2";
            }
            //当i=12时,
            if (i == 12) {
                maxdate = year + "/" + String.valueOf(i) + "/" + "31";
            } else {
                maxdate = year + "/" + String.valueOf(i + 1) + "/" + "1";
            }
            String medium = year + "/" + String.valueOf(i) + "/" + "15";
            String mediumP = year + "/" + String.valueOf(i) + "/" + "16";
            String dateArray[] = new String[]{mindate,mediumP,medium,maxdate};

            for(int a = 0; a < 2; a++){
                //一个月的数据，打开连接，关闭连接
                //初始化virtuoso工具---由于数据量可能比较大，会影响内存使用，因此最好插入到virtuoso中
                VirtGraph graph = new VirtGraph("pubmedData", "jdbc:virtuoso://10.0.89.33:1111", "dba", "0dabigta1357");
                VirtModel model = new VirtModel(graph);
                try {
                    String searchResult = sendHttpsPost(basicurl + "esearch.fcgi?db=pubmed&datetype=edat&usehistory=y&app_key=" + appKey + "&mindate=" + dateArray[a] + "&maxdate=" + dateArray[a+2] + "&term=" + content);
    //            System.out.println(result);
                    JSONObject searchJsonObject = XML.toJSONObject(searchResult);
    //            System.out.println(jsonObject.toString());
                    JSONObject eSearchResult = searchJsonObject.optJSONObject("eSearchResult");
                    int count = Integer.parseInt(eSearchResult.optString("Count"));
                    log.info(dateArray[a] + "-" + dateArray[a+2] + "数据量：" + count);
                    if (count > 10000) {
                        //记录下，并且不停止程序，单独处理这个月
                        log.info(dateArray[a] + "-" + dateArray[a+2] + "数据量：" + count + ",大于1W，记录下，手动处理");
                        continue;
                    } else {


                            log.info("开始获取论文数据");
                            String webEnv = eSearchResult.optString("WebEnv");
                            Thread.sleep(1000);  //暂停1s
                            String fetchResult = sendHttpsPost(basicurl + "efetch.fcgi?db=pubmed&query_key=1&app_key=" + appKey + "&WebEnv=" + webEnv);
                            JSONObject fetchJsonObject = XML.toJSONObject(fetchResult);
                            //存储article对象
                            JSONObject pubmedArticleSet = fetchJsonObject.optJSONObject("PubmedArticleSet");
                            if (null != pubmedArticleSet) {
                                JSONArray pubmedArticleList = pubmedArticleSet.optJSONArray("PubmedArticle");
                                if ((null != pubmedArticleList) && (pubmedArticleList.length() > 0)) {
                                    for (int j = 0; j < pubmedArticleList.length(); j++) {
                                        JSONObject jsonObject = pubmedArticleList.optJSONObject(j);
                                        JSONObject medlineCitation = jsonObject.optJSONObject("MedlineCitation");
                                        JSONObject pubmedData = jsonObject.optJSONObject("PubmedData");
                                        if ((null != medlineCitation) && (null != pubmedData)) {
                                            JSONObject pmidJsonObject = medlineCitation.optJSONObject("PMID");
                                            String pmid = pmidJsonObject.optString("content");     //pmid
    //                                    log.info("pmid--" + pmid);
                                            //创建资源-实例化主语
                                            Resource resource = model.createResource("http://pubmed.semweb.csdb.cn/resource/Literature_" + pmid);
                                            //固定的谓语
                                            resource.addProperty(new PropertyImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), new ResourceImpl("https://schema.org/Article"));
                                            resource.addProperty(new PropertyImpl("https://w3id.org/reproduceme#pubmedid"), ResourceFactory.createLangLiteral(pmid, "en"));

                                            JSONObject articleJsonObject = medlineCitation.optJSONObject("Article");
                                            String articleTitle = getTagContent(articleJsonObject, "ArticleTitle");
                                            addVerbAndObject(resource,"http://www.w3.org/2000/01/rdf-schema#label",articleTitle);

                                            JSONObject abstractJsonObject = articleJsonObject.optJSONObject("Abstract");
                                            //有的摘要是个数组形式
                                            JSONArray abstractTextArr =getTagContentP(abstractJsonObject,"AbstractText");
                                            if(abstractTextArr.length() > 1){
                                                System.out.println(pmid + "的abstractTextArr是数组，特殊处理---------------------");
                                            }else{
                                                String abstractText =getTagContent(abstractJsonObject,"AbstractText");
                                                addVerbAndObject(resource,"https://schema.org/abstract",abstractText);
                                            }

                                            JSONObject keywordListJsonObject = medlineCitation.optJSONObject("KeywordList");
                                            JSONArray keywordList = getTagContentP(keywordListJsonObject, "Keyword");
                                            for (int k = 0; k < keywordList.length(); k++) {
                                                JSONObject jsonObject1 = keywordList.optJSONObject(k);
                                                String keyword = jsonObject1.optString("content");  //keyword
                                                addVerbAndObject(resource,"https://schema.org/keywords",keyword);
                                            }

                                            JSONObject chemicalListJsonObject = medlineCitation.optJSONObject("ChemicalList");
                                            JSONArray chemicalList = getTagContentP(chemicalListJsonObject,"Chemical");
                                            for (int k = 0; k < chemicalList.length(); k++) {
                                                JSONObject jsonObject1 = chemicalList.optJSONObject(k);
                                                JSONObject nameOfSubstance = jsonObject1.optJSONObject("NameOfSubstance");
                                                String subMeshId = nameOfSubstance.optString("UI");  //Substances-meshId
                                                addVerbAndObject(resource,"http://purl.obolibrary.org/obo/OMIT_0001004",subMeshId);
                                            }

                                            JSONObject meshHeadingListJsonObject = medlineCitation.optJSONObject("MeshHeadingList");
                                            JSONArray MeshHeadingList = getTagContentP(meshHeadingListJsonObject,"MeshHeading");
                                            for (int k = 0; k < MeshHeadingList.length(); k++) {
                                                JSONObject jsonObject1 = MeshHeadingList.optJSONObject(k);
                                                JSONObject descriptorName = jsonObject1.optJSONObject("DescriptorName");
                                                String meshId = descriptorName.optString("UI");  //MeSH terms-meshId
                                                addVerbAndObject(resource,"http://purl.obolibrary.org/obo/OMIT_0000110",meshId);
                                            }

                                            JSONObject articleIdListJsonObject = pubmedData.optJSONObject("ArticleIdList");
                                            JSONArray articleIdList = getTagContentP(articleIdListJsonObject,"ArticleId");
                                            for (int k = 0; k < articleIdList.length(); k++) {
                                                JSONObject jsonObject1 = articleIdList.optJSONObject(k);
                                                String idType = jsonObject1.optString("IdType");
                                                if (null != idType && "pmc".equals(idType)) {
                                                    String pmcid = jsonObject1.optString("content");  //pmcid
                                                    addVerbAndObject(resource,"https://w3id.org/reproduceme#pmcid",pmcid);
                                                    break;
                                                }
                                            }
                                        } else {
                                            log.info("标记MedlineCitation和PubmedData，无实际内容！");
                                            continue;
                                        }
                                        //将这个论文存储写到文件中
    //                                RDFDataMgr.write(new FileOutputStream("F:\\linkPubmed.ttl"), model, RDFFormat.TTL);
                                    }
                                    log.info(dateArray[a] + "-" + dateArray[a+2] + "论文数据存储完成！");
                                } else {
                                    log.info(dateArray[a] + "-" + dateArray[a+2] + "无论文数据！");
                                }
                            }
                    }
                } catch (Exception e) {
                    log.info(dateArray[a] + "-" + dateArray[a+2] + "-异常：" + e.toString());
                    model.close();
                    graph.close();
                } finally {
                    model.close();
                    graph.close();

                }
            }

        }
        return "is ok";
    }

    /**
     * 校验一个标签值，并返回该标签的子标签名为tagName的内容（字符串）
     * @param jsonObject
     * @param tagName
     * @return
     */
    public String getTagContent(JSONObject jsonObject, String tagName){
        if(null != jsonObject){
            String content = jsonObject.getString(tagName);
            if(!"".equals(content)){
                return content;
            }
        }
        return "";
    }
    /**
     * 校验一个标签值，并返回该标签的子标签名为tagName的内容（列表）
     * @param jsonObject
     * @param tagName
     * @return
     */
    public JSONArray getTagContentP(JSONObject jsonObject, String tagName){
        if(null != jsonObject){
            JSONArray contentList = jsonObject.optJSONArray(tagName);
            if((null != contentList) && (contentList.length() > 0)){
                return contentList;
            }
        }
        return new JSONArray();
    }
    /**
     * 添加谓语和宾语
     * @param resource
     * @param predicate
     * @param object
     */
    public void addVerbAndObject(Resource resource,String predicate,String object){
        if(!"".equals(object)){
//            log.info(predicate + "----" + object);
            resource.addProperty(new PropertyImpl(predicate), ResourceFactory.createLangLiteral(object,"en"));
        }
    }


    public String sendHttpsPost(String url){
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            // 创建SSLContext对象，并使用我们指定的信任管理器初始化
            TrustManager[] tm = { new HttpsTrustClient() };
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, tm, new java.security.SecureRandom());

            // 从上述SSLContext对象中得到SSLSocketFactory对象
            SSLSocketFactory ssf = sslContext.getSocketFactory();
            // 打开和URL之间的连接
            URL realUrl = new URL(url);
            HttpsURLConnection conn = (HttpsURLConnection) realUrl.openConnection();
            conn.setSSLSocketFactory(ssf);
            conn.setConnectTimeout(120000);
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("content-Type", "application/json");
            conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
//            out.print(param);
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {// 使用finally块来关闭输出流、输入流
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }
}

package com.linkeddata.portal.service.helper;

import com.google.gson.*;
import com.linkeddata.portal.entity.script.findlink.NcmiClinicalDrug;
import com.linkeddata.portal.entity.script.findlink.NcmiClinicalTrail;
import com.linkeddata.portal.utils.HttpUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 搜索关联关系 辅助类 用于一些方法的业务逻辑处理
 *
 * @author wangzhiliang
 * @date 20230210
 */
public class FindLinksHelper {
    /**
     * 调用健康中心接口获取到  Clinical Trail
     * 封装  NcmiClinicalTrail 对象
     *
     * @author wangzhiliang
     * @date 20230211
     */
    public static List<NcmiClinicalTrail> getNcmiTrail(String url) {
        List<NcmiClinicalTrail> nct = new LinkedList<>();
        try {
            String response = HttpUtil.doGet(url);
            if (StringUtils.isNotBlank(response)) {
                JsonElement responseJsonElement = JsonParser.parseString(response);
                if (null != responseJsonElement) {
                    for (JsonElement jse : responseJsonElement.getAsJsonArray()) {
                        JsonElement search = jse.getAsJsonObject().get("search");
                        if (null != search) {
                            JsonElement data = search.getAsJsonObject().get("data");
                            if (null != data) {
                                if (data.isJsonArray()) {
                                    JsonElement dataJsonArray = data.getAsJsonArray();
                                    for (JsonElement jsonElement : dataJsonArray.getAsJsonArray()) {
                                        NcmiClinicalTrail clinicalTrail = new NcmiClinicalTrail();
                                        if (null != jsonElement.getAsJsonObject().get("state")) {
                                            String state = jsonElement.getAsJsonObject().get("state").getAsString();
                                            if ("Authorised".equals(state) || "Completed".equals(state)) {
                                                clinicalTrail.setState(state);
                                                if (null != jsonElement.getAsJsonObject().get("nct_number")) {
                                                    String nctNum = jsonElement.getAsJsonObject().get("nct_number").getAsString();
                                                    clinicalTrail.setNctNumber(nctNum);
                                                }
                                                if (null != jsonElement.getAsJsonObject().get("title")) {
                                                    String title = jsonElement.getAsJsonObject().get("title").getAsString();
                                                    clinicalTrail.setTitle(title);
                                                }
                                                JsonElement conditionsJsonElement = jsonElement.getAsJsonObject().get("conditions");
                                                if (null != conditionsJsonElement) {
                                                    List<String> conditions = new ArrayList<>();
                                                    if (conditionsJsonElement.isJsonArray()) {
                                                        for (JsonElement condition : conditionsJsonElement.getAsJsonArray()) {
                                                            conditions.add(condition.getAsString());
                                                        }
                                                    } else {
                                                        conditions.add(conditionsJsonElement.getAsString());
                                                    }
                                                    clinicalTrail.setConditions(conditions);
                                                }
                                                JsonElement interventionsJsonElement = jsonElement.getAsJsonObject().get("interventions");
                                                if (null != interventionsJsonElement) {
                                                    List<String> interventions = new ArrayList<>();
                                                    if (interventionsJsonElement.isJsonArray()) {
                                                        for (JsonElement intervention : interventionsJsonElement.getAsJsonArray()) {
                                                            interventions.add(intervention.getAsString());
                                                        }
                                                    } else {
                                                        interventions.add(interventionsJsonElement.getAsString());
                                                    }
                                                    clinicalTrail.setInterventions(interventions);
                                                }
                                                JsonElement locationsJsonElement = jsonElement.getAsJsonObject().get("locations");
                                                if (null != locationsJsonElement) {
                                                    clinicalTrail.setLocations(locationsJsonElement.getAsString());
                                                }
                                                if (null != jsonElement.getAsJsonObject().get("source_url")) {
                                                    clinicalTrail.setUrl(jsonElement.getAsJsonObject().get("source_url").getAsString());
                                                }
                                                nct.add(clinicalTrail);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return nct = null;
        }
        return nct;
    }

    /**
     * 调用健康中心接口获取到  drug
     * 封装  NcmiClinicalDrug 对象
     *
     * @author wangzhiliang
     * @date 20230211
     */
    public static List<NcmiClinicalDrug> getNcmiDrug(String url) {
        List<NcmiClinicalDrug> ncd = new LinkedList<>();
        try {
            String response = HttpUtil.doGet(url);
            if (StringUtils.isNotBlank(response)) {
                JsonElement responseJsonElement = JsonParser.parseString(response);
                if (null != responseJsonElement) {
                    for (JsonElement jse : responseJsonElement.getAsJsonArray()) {
                        JsonElement search = jse.getAsJsonObject().get("search");
                        if (null != search) {
                            JsonElement data = search.getAsJsonObject().get("data");
                            if (null != data) {
                                if (data.isJsonArray()) {
                                    JsonElement dataJsonArray = data.getAsJsonArray();
                                    for (JsonElement jsonElement : dataJsonArray.getAsJsonArray()) {
                                        NcmiClinicalDrug clinicalDrug = new NcmiClinicalDrug();
                                        if (null != jsonElement.getAsJsonObject().get("title")) {
                                            clinicalDrug.setTitle(jsonElement.getAsJsonObject().get("title").getAsString());
                                        }
                                        if (null != jsonElement.getAsJsonObject().get("drugbank_num")) {
                                            clinicalDrug.setDrugBankNum(jsonElement.getAsJsonObject().get("drugbank_num").getAsString());
                                        }
                                        if (null != jsonElement.getAsJsonObject().get("mechanism")) {
                                            clinicalDrug.setMechanism(jsonElement.getAsJsonObject().get("mechanism").getAsString());
                                        }
                                        if (null != jsonElement.getAsJsonObject().get("attachment")) {
                                            clinicalDrug.setAttachment(jsonElement.getAsJsonObject().get("attachment").getAsString());
                                        }
                                        if (null != jsonElement.getAsJsonObject().get("reference")) {
                                            clinicalDrug.setReference(jsonElement.getAsJsonObject().get("reference").getAsString());
                                        }
                                        if (null != jsonElement.getAsJsonObject().get("source_url")) {
                                            clinicalDrug.setSourceUrl(jsonElement.getAsJsonObject().get("source_url").getAsString());
                                        }
                                        ncd.add(clinicalDrug);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ncd = null;
        }
        return ncd;
    }
    /**
     * 拆分 intervention 含有 Drug: 数据
     *
     * @author wangzhiliang
     * @date 20220211
     * */
    public static List<String> splitDrugStr(String param){
        List<String> result = new ArrayList<>();
        param = param.replaceAll("Drug:","");
        String[] params = param.split("\\|");
        for (String str:params) {
            if(!str.contains(":")){
                result.add(str.trim());
            }
        }
        return  result;
    }


    public static void main(String[] args) {
//        getNcmiTrail("https://www.ncmi.cn/covid-19/searchListShare.do?page=1&classen=clinical_&searchText=&searchText2=&sort=language+desc;date+desc;id+desc;&sessionLang=english_en&pageNumber=10");
//    getNcmiDrug("https://www.ncmi.cn/covid-19/searchListShare.do?page=1&classen=medicine&searchText=&searchText2=&sort=language+desc;date+desc;id+desc;&sessionLang=english_en&pageNumber=10");
//     List<String> list = new LinkedList<>();
//     list.add("Drug: Alferon LDO");
//     list.add("Drug: Recombinant human interferon α1β");
//     AtomicBoolean typeFlag = new AtomicBoolean(false);
//        list.forEach(item->{
//            if(item.contains("Drug:")){
//                 typeFlag.set(true);
//            }
//        });
//
//        System.out.println(typeFlag.get());
//        String sparqlNctNum = "select * where { ?s <https://schema.org/identifier> 'EUCTR2020-001224-33-DE'}";
//        ResultSet resultSet = RdfUtils.queryTriple("http://10.0.82.94:8890/sparql/?default-graph-uri=Clinical_Trials", sparqlNctNum);
//        if (!resultSet.hasNext()) {
//            System.out.println("1");
//        }
        System.out.println(splitDrugStr("Drug: Aluminum hydroxide|Drug: Placebo|Biological: SARS-CoV"));
    }
}

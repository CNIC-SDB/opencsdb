package com.linkeddata.portal.utils;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.linkeddata.portal.entity.script.findlink.ClinicalStudyEntity;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.json.XML;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;


/**
 * 解析xml 工具类
 *
 * @author wangzhiliang
 */

public class XmlUtils {
    /**
     * 读取文件内容
     *
     * @param filePath 文件绝对路径
     * @return String 文件内容
     * @author wangzhiliang
     * @date 20230207
     */
    public static String convertXMLtoJSON(String filePath) {
        try (InputStream in = new FileInputStream(new File(filePath))) {
            String contents = IOUtils.toString(in, StandardCharsets.UTF_8);
            contents = contents.replaceAll("&#xD;", "").replaceAll("\\n", "").replaceAll("      ", " ");
            JSONObject jsonObject = XML.toJSONObject(contents);
            return jsonObject.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 递归获取文件夹下所有文件的绝对路径
     *
     * @param file 文件路径
     * @return 文件夹下所有文件的绝对路径
     */
    public static List<String> recursionFile(File file) {
        List<String> pathList = new LinkedList<>();
        File[] files = file.listFiles();
        if (null != files) {
            for (File file2 : files) {
                pathList.add(file2.getPath());
                if (file2.isDirectory()) {
                    recursionFile(file2);
                }
            }
        }
        return pathList;
    }

    /**
     * 解析 json 提取临床实验数据
     *
     * @param
     * @author wangzhiliang
     * @date 20230208
     */
    public static ClinicalStudyEntity getClinicalStudyEntity(String contents) {
        ClinicalStudyEntity cse = new ClinicalStudyEntity();
        JsonObject jsonObject = JsonParser.parseString(contents).getAsJsonObject();
        JsonObject cliStudy = jsonObject.get("clinical_study").getAsJsonObject();

        //获取 nct_id
        String nctId = cliStudy.get("id_info").getAsJsonObject().get("nct_id").getAsString();
        if (StringUtils.isBlank(nctId)) {
            return null;
        }
        cse.setNct(nctId);
        //获取 url
        String url = cliStudy.get("required_header").getAsJsonObject().get("url").getAsString();
        cse.setUrl(url);
        //获取 brief_title
        if (null != cliStudy.get("brief_title")) {
            String briefTitle = cliStudy.get("brief_title").getAsString();
            cse.setBriefTitle(briefTitle);
        }
        JsonElement conditionJsonElement = cliStudy.get("condition");
        if (null != conditionJsonElement) {
            //获取 conditions
            List<String> conditions = new ArrayList<>();
            if (conditionJsonElement.isJsonArray()) {
                JsonArray conditionJsonArray = conditionJsonElement.getAsJsonArray();
                if (conditionJsonArray.size() > 0) {
                    for (JsonElement jsonElement : conditionJsonArray) {
                        conditions.add(jsonElement.getAsString());
                    }
                    cse.setConditions(conditions);
                }
            } else {
                String condition = conditionJsonElement.getAsString();
                conditions.add(condition);
                cse.setConditions(conditions);
            }
        }

        //获取 location
        JsonElement locationJsonElement = cliStudy.get("location");
        if (null != locationJsonElement) {
            List<String> locations = new ArrayList<>();
            if (locationJsonElement.isJsonArray()) {
                JsonArray locationJsonArray = locationJsonElement.getAsJsonArray();
                if (locationJsonArray.size() > 0) {
                    for (JsonElement jsonElement : locationJsonArray) {
                        JsonElement facility = jsonElement.getAsJsonObject().get("facility");
                        String name = facility.getAsJsonObject().get("name").getAsString();
                        locations.add(name);
                    }
                    cse.setLocations(locations);
                }
            } else {
                JsonElement facility = locationJsonElement.getAsJsonObject().get("facility");
                String name = facility.getAsJsonObject().get("name").getAsString();
                locations.add(name);
                cse.setLocations(locations);
            }
        }
        //获取 intervention
        JsonElement interventionJsonElement = cliStudy.get("intervention");
        if (null != interventionJsonElement) {
            List<Map<String, Object>> drugs = new ArrayList<>();
            if (interventionJsonElement.isJsonArray()) {
                JsonArray interventionJsonArray = interventionJsonElement.getAsJsonArray();
                if (interventionJsonArray.size() > 0) {
                    for (JsonElement jsonElement : interventionJsonArray) {
                        Map<String, Object> map = new HashMap<>();
                        String type = jsonElement.getAsJsonObject().get("intervention_type").getAsString();
                        if ("Drug".equals(type)) {
                            String name = jsonElement.getAsJsonObject().get("intervention_name").getAsString();
                            String description = jsonElement.getAsJsonObject().get("description").getAsString();
                            map.put("name", name);
                            map.put("description", description);
                            List<String> otherNames = new ArrayList<>();
                            if (null != jsonElement.getAsJsonObject().get("other_name")) {
                                JsonElement otherNameNode =jsonElement.getAsJsonObject().get("other_name");
                                if(otherNameNode.isJsonArray()){
                                    for (JsonElement otherNameJsonElement : otherNameNode.getAsJsonArray()) {
                                        otherNames.add(otherNameJsonElement.getAsString());
                                    }
                                }else{
                                    otherNames.add(otherNameNode.getAsString());
                                }
                                map.put("otherName", otherNames);
                            }
                            drugs.add(map);
                        }
                    }
                    cse.setDrugs(drugs);
                }
            } else {
                Map<String, Object> map = new HashMap<>();
                String type = interventionJsonElement.getAsJsonObject().get("intervention_type").getAsString();
                if ("Drug".equals(type)) {
                    String name = interventionJsonElement.getAsJsonObject().get("intervention_name").getAsString();
                    String description = interventionJsonElement.getAsJsonObject().get("description").getAsString();
                    map.put("name", name);
                    map.put("description", description);
                    List<String> otherNames = new ArrayList<>();
                    if (null != interventionJsonElement.getAsJsonObject().get("other_name")) {
                        JsonElement otherNameNode =interventionJsonElement.getAsJsonObject().get("other_name");
                        if(otherNameNode.isJsonArray()){
                            for (JsonElement jsonElement : otherNameNode.getAsJsonArray()) {
                                otherNames.add(jsonElement.getAsString());
                            }
                        }else{
                            otherNames.add(otherNameNode.getAsString());
                        }
                        map.put("otherName", otherNames);
                    }
                    drugs.add(map);
                    cse.setDrugs(drugs);
                }
            }
        }
        //获取 brief_summary
        if (null != cliStudy.get("brief_summary")) {
            String briefSummary = cliStudy.get("brief_summary").getAsJsonObject().get("textblock").getAsString();
            cse.setBriefSummary(briefSummary);
        }
        //获取 primary_purpose
        if (null != cliStudy.get("study_design_info")) {
            if(null != cliStudy.get("study_design_info").getAsJsonObject().get("primary_purpose")){
                String primaryPurpose =cliStudy.get("study_design_info").getAsJsonObject().get("primary_purpose").getAsString();
                cse.setPrimaryPurpose(primaryPurpose);
            }

        }
        return cse;

    }

    public static void main(String[] args) {
        String[] contains ={"ncov" ,"covid","coronavirus disease","coronavirus infection","sars-cov-2"};
        String str = "COVID-19 Pneumonia";


    }
}

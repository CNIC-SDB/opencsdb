package com.linkeddata.portal.test;

import com.google.gson.Gson;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 9.6测试报告用
 */
@RestController
@RequestMapping("/myTest96")
public class MyTest96Controller {

    @RequestMapping("/getResourceListForModel")
    public Object getResourceListForModel() {
        String jsonString = "{\n" +
                "  \"total\": 1,\n" +
                "  \"code\": 200,\n" +
                "  \"data\": [\n" +
                "    {\n" +
                "\t  \"id\": \"http://cryosphere.semweb.csdb.cn/resource/Model_1\",\n" +
                "      \"dataSourceName\": \"地貌信息熵\",\n" +
                "      \"dataCenterId\": \"100106\",\n" +
                "      \"Label\": \"地貌特征;洪水判别;山洪易发位置\",\n" +
                "      \"subjectId\": \"地球科学>地质学>数字地质学\",\n" +
                "      \"description\": \"地貌信息熵算法是一种用于衡量地貌多样性和复杂性的指标，通过对地貌单位的类型和分布进行分析，计算地貌信息熵，从而提供对地貌特征的定量评估和比较。该算法在地貌分类、环境变化监测和地貌规划与管理等领域具有广泛的应用价值。\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"totalPage\": 1\n" +
                "}";
        Map result = new Gson().fromJson(jsonString, Map.class);
        return result;
    }

    @RequestMapping("/getResourceByIdForModel")
    public Object getResourceByIdForModel() {
        String jsonString = "{\n" +
                "    \"code\": 200.0,\n" +
                "    \"data\": [\n" +
                "        {\n" +
                "            \"id\": \"http://cryosphere.semweb.csdb.cn/resource/Model_1\",\n" +
                "            \"dataSourceName\": \"地貌信息熵\",\n" +
                "            \"dataCenterId\": \"100106\",\n" +
                "            \"Label\": \"地貌特征;洪水判别;山洪易发位置\",\n" +
                "            \"subjectId\": \"地球科学>地质学>数字地质学\",\n" +
                "            \"description\": \"地貌信息熵算法是一种用于衡量地貌多样性和复杂性的指标，通过对地貌单位的类型和分布进行分析，计算地貌信息熵，从而提供对地貌特征的定量评估和比较。该算法在地貌分类、环境变化监测和地貌规划与管理等领域具有广泛的应用价值。\",\n" +
                "            \"inputDataType\": \"使用沟谷比降算法时，需要以下输入数据：沟道与集水区对应栅格数据。沟道栅格数据。集水区栅格数据。河流或沟谷的地理坐标数据：包括河流或沟谷的起点和终点位置的经纬度或投影坐标。高程数据：河流或沟谷沿线的高程数据，通常以数字高程模型（DEM）或地形图形式提供。DEM数据可以是栅格格式或矢量等高线格式。\",\n" +
                "            \"inputFormat\": \"Tiff;tif;shapefile\",\n" +
                "            \"outputDataType\": \"地貌信息熵值\",\n" +
                "            \"outputFormat\": \"csv\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";
        Map result = new Gson().fromJson(jsonString, Map.class);
        return result;
    }

}

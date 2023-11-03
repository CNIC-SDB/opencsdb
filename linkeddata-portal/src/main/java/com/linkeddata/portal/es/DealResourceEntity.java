/*
package com.linkeddata.portal.es;

import com.google.gson.*;
import com.linkeddata.portal.entity.es.ResourceEntity;
import com.linkeddata.portal.entity.mongo.Dataset;
import com.linkeddata.portal.utils.RdfUtils;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.*;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;

import javax.annotation.Resource;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import java.io.IOException;
import java.sql.Array;
import java.util.*;

*/
/**
 * 资源实体-es处理
 *
 * @author hmh
 *//*

@Component
public class DealResourceEntity {
    private static final String HTTP = "http";
    private static final String HTTPS = "https";
    private static SSLConnectionSocketFactory sslsf = null;
    private static PoolingHttpClientConnectionManager cm = null;
    private static SSLContextBuilder builder = null;
    @Resource
    private MongoTemplate mongoTemplate;
    static {
        try {
            builder = new SSLContextBuilder();
            // 全部信任 不做身份鉴定
            builder.loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                    return true;
                }
            });
            sslsf = new SSLConnectionSocketFactory(builder.build(), new String[]{"SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.2"}, null, NoopHostnameVerifier.INSTANCE);
            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register(HTTP, new PlainConnectionSocketFactory())
                    .register(HTTPS, sslsf)
                    .build();
            cm = new PoolingHttpClientConnectionManager(registry);
            cm.setMaxTotal(200);//max connection
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        DealResourceEntity resourceEntity = new DealResourceEntity();
        resourceEntity.getResourceEntity();
    }

    */
/**
     * 获取resourceEntity数据
     *//*

    public void getResourceEntity() {
        List<Dataset> dataSetList = mongoTemplate.findAll(Dataset.class);
        for (Dataset dataSet: dataSetList) {
            String sparql = dataSet.getSparql();
            long count = RdfUtils.countTriple(sparql, "SELECT (COUNT(DISTINCT ?s) AS ?count) WHERE { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?type . }");
            //每次插入1000条数据
            System.out.println(count);
        }


//        Map<String, Object> map = new HashMap<>();
//        map.put("datasetId","63413cfc8431626e65245571");
//        map.put("esFlag","true");
//        map.put("pageNum",1);
//        map.put("pageSize",1);
//        map.put("condition","");
//        map.put("domain",new String[0]);
//        map.put("institution",new String[0]);
//        String result = null;
//        try {
//            result = doPost("http://10.0.85.83:8089/resource/list", map);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        System.out.println(result);
    }
    */
/**
     * doPost请求
     * http 访问 https 做了跳过ssl验证访问
     * *//*

    public static String doPost(String url, Map<String, Object> paramMap) throws Exception {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse httpResponse = null;
        String result = "";
        // 创建httpClient实例
        httpClient = getHttpClient();
        // 创建httpPost远程连接实例
        HttpPost httpPost = new HttpPost(url);
        // 配置请求参数实例
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(35000)// 设置连接主机服务超时时间
                .setConnectionRequestTimeout(35000)// 设置连接请求超时时间
                .setSocketTimeout(60000)// 设置读取数据连接超时时间
                .build();
        // 为httpPost实例设置配置
        httpPost.setConfig(requestConfig);
        // 设置请求头
        httpPost.addHeader("Content-Type", "application/json");
        // 为httpPost设置封装好的请求参数
        try {
            if (paramMap != null) {
                StringEntity stringEntity = new StringEntity(new Gson().toJson(paramMap),ContentType.create("application/json", "utf-8"));
//                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(stringEntity,"UTF-8");
                httpPost.setEntity(stringEntity);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            // httpClient对象执行post请求,并返回响应参数对象
            httpResponse = httpClient.execute(httpPost);
            // 从响应对象中获取响应内容
            HttpEntity entity = httpResponse.getEntity();
            result = EntityUtils.toString(entity);
            JsonObject jsonObject = new JsonParser().parse(result).getAsJsonObject();
            JsonElement data = jsonObject.get("data");
            JsonArray pageList = data.getAsJsonObject().getAsJsonArray("pageList");
            for(int i = 0; i < pageList.size(); i++){
                JsonElement jsonElement = pageList.get(i);
                String toString = jsonElement.getAsJsonObject().toString();
                Gson gson = new Gson();
                ResourceEntity resourceEntity = gson.fromJson(toString, ResourceEntity.class);
                System.out.println(resourceEntity.toString());
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭资源
            if (null != httpResponse) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
    public static CloseableHttpClient getHttpClient() throws Exception {
        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .setConnectionManager(cm)
                .setConnectionManagerShared(true)
                .build();
        return httpClient;
    }
}
*/

package com.linkeddata.portal.utils;


import lombok.extern.slf4j.Slf4j;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * 工具类
 * @author wangzl
 */
@Slf4j
public class HttpUtil {
    private static final String HTTP = "http";
    private static final String HTTPS = "https";
    private static SSLConnectionSocketFactory sslsf = null;
    private static PoolingHttpClientConnectionManager cm = null;
    private static SSLContextBuilder builder = null;
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
    /**
     * doGet请求
     * http 访问 https 做了跳过ssl验证访问
     * */
    public static String doGet(String url) throws Exception {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        String result = "";
        try {
            // 通过址默认配置创建一个httpClient实例
            httpClient = getHttpClient();
            // 创建httpGet远程连接实例
            HttpGet httpGet = new HttpGet(url);
            // 设置配置请求参数
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(35000)// 连接主机服务超时时间
                    .setConnectionRequestTimeout(35000)// 请求超时时间
                    .setSocketTimeout(60000)// 数据读取超时时间
                    .build();
            // 为httpGet实例设置配置
            httpGet.setConfig(requestConfig);
            // 执行get请求得到返回对象
            response = httpClient.execute(httpGet);
            // 通过返回对象获取返回数据
            HttpEntity entity = response.getEntity();
            // 通过EntityUtils中的toString方法将结果转换为字符串
            result = EntityUtils.toString(entity);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭资源
            if (null != response) {
                try {
                    response.close();
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

    /**
     * doPost请求，支持中文
     *
     * @param url        请求地址
     * @param paramName  参数名
     * @param paramValue 参数值
     * @author 陈锟
     * @since 2023年7月26日21:51:36
     */
    public static String doPost(String url, String paramName, String paramValue) {
        String result = "false";
        try {
            // 创建 HttpClient 实例
            CloseableHttpClient httpClient = HttpClients.createDefault();

            // 使用 URIBuilder 构建带有参数的 URL
            URIBuilder uriBuilder = new URIBuilder(url);
            uriBuilder.addParameter(paramName, paramValue);
            URI uri = uriBuilder.build();

            // 创建 HttpPost 请求
            HttpPost httpPost = new HttpPost(uri);

            // 发送请求并获取响应
            CloseableHttpResponse response = httpClient.execute(httpPost);

            // 处理响应
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                result = EntityUtils.toString(entity, "UTF-8");
            }

            // 关闭 HttpClient 和响应对象
            response.close();
            httpClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return result;
        }
    }

    /**
     * 接收2个参数的doPost方法
     *
     * @param url
     * @param paramName
     * @param paramValue
     * @param paramName2
     * @param paramValue2
     * @return
     */
    public static String doPost2(String url, String paramName, String paramValue, String paramName2, String paramValue2) {
        String result = "false";
        try {
            // 创建 HttpClient 实例
            CloseableHttpClient httpClient = HttpClients.createDefault();

            // 使用 URIBuilder 构建带有参数的 URL
            URIBuilder uriBuilder = new URIBuilder(url);
            uriBuilder.addParameter(paramName, paramValue);
            uriBuilder.addParameter(paramName2, paramValue2);
            URI uri = uriBuilder.build();

            // 创建 HttpPost 请求
            HttpPost httpPost = new HttpPost(uri);

            // 发送请求并获取响应
            CloseableHttpResponse response = httpClient.execute(httpPost);

            // 处理响应
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                result = EntityUtils.toString(entity, "UTF-8");
            }

            // 关闭 HttpClient 和响应对象
            response.close();
            httpClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return result;
        }
    }

    /**
     * 这个方法不知道为什么python返回的是乱码
     *
     * @param url
     * @param paramNames
     * @param paramValues
     * @return
     */
    public static String doPostArr(String url, String[] paramNames, String[] paramValues) {
        String result = "false";
        try {
            // 创建 HttpClient 实例
            CloseableHttpClient httpClient = HttpClients.createDefault();

            // 使用 URIBuilder 构建带有参数的 URL
            URIBuilder uriBuilder = new URIBuilder(url);
            for (int i = 0; i < paramNames.length; i++) {
                String paramName = URLEncoder.encode(paramNames[i].toString(), StandardCharsets.UTF_8);
                String paramValue = URLEncoder.encode(paramValues[i].toString(), StandardCharsets.UTF_8);
                uriBuilder.addParameter(paramName, paramValue);
//                uriBuilder.addParameter(paramNames[i], paramValues[i]);
            }
            URI uri = uriBuilder.build();

            // 创建 HttpPost 请求
            HttpPost httpPost = new HttpPost(uri);
//            httpPost.setHeader("Content-Type", "application/json; charset=UTF-8");

            // 发送请求并获取响应
            CloseableHttpResponse response = httpClient.execute(httpPost);

            // 处理响应
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                result = EntityUtils.toString(entity, "UTF-8");
            }

            // 关闭 HttpClient 和响应对象
            response.close();
            httpClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return result;
        }
    }

    public static CloseableHttpClient getHttpClient() throws Exception {
        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .setConnectionManager(cm)
                .setConnectionManagerShared(true)
                .build();
        return httpClient;
    }
    public static String readHttpResponse(HttpResponse httpResponse)
            throws ParseException, IOException {
        StringBuilder builder = new StringBuilder();
        // 获取响应消息实体
        HttpEntity entity = httpResponse.getEntity();
        // 响应状态
        builder.append("status:" + httpResponse.getStatusLine());
        builder.append("headers:");
        HeaderIterator iterator = httpResponse.headerIterator();
        while (iterator.hasNext()) {
            builder.append("\t" + iterator.next());
        }
        // 判断响应实体是否为空
        if (entity != null) {
            String responseString = EntityUtils.toString(entity);
            builder.append("response length:" + responseString.length());
            builder.append("response content:" + responseString.replace("\r\n", ""));
        }
        return builder.toString();
    }

}

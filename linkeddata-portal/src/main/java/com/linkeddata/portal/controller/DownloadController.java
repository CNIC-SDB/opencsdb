package com.linkeddata.portal.controller;

import com.linkeddata.portal.repository.DataDownloadDao;
import com.linkeddata.portal.script.CreateVoIDFileUtil;
import com.linkeddata.portal.service.DatasetService;
import com.linkeddata.portal.utils.StringUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 下载文件
 *
 * @author : gaoshuai
 * @date : 2022/10/8 10:30
 */
@Api(tags = "下载相关接口")
@RestController
public class DownloadController {

    @Value("${downloadDir}")
    private String fileDir;

    @Resource
    private DataDownloadDao dataDownloadDao;
    @javax.annotation.Resource
    private DatasetService datasetService;

    @javax.annotation.Resource
    private CreateVoIDFileUtil createVoIDFileUtil;

    @ApiOperation("下载rdf文件")
    @Deprecated
    @GetMapping("/download")
    public ResponseEntity<byte[]> fileDownload(HttpServletRequest request, @ApiParam("文件名称") String filename) throws Exception {
        // 指定下载文件的根路径
        String dirPath = fileDir;

        int i = filename.lastIndexOf(".");
        String identifier = filename.substring(0, i);
        // 创建该文件对象
        File file = new File(dirPath + identifier + File.separator + filename);
        // 设置响应头
        HttpHeaders headers = new HttpHeaders();
        // 通知浏览器以下载的方式打开
        filename = getFilename(request, filename);
        headers.setContentDispositionFormData("attachement", filename);
        // 定义以流的形式下载返回文件数据
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        try {
            return new ResponseEntity<>(FileUtils.readFileToByteArray(file), headers, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(FileUtils.readFileToByteArray(file), headers, HttpStatus.EXPECTATION_FAILED);
        }

    }

    private String getFilename(HttpServletRequest request, String filename) throws Exception {
        // IE不同版本User-Agent中出现的关键词
        String[] IEBrowserKeyWords = {"MSIE", "Trident", "Edge"};
        // 获取请求头代理信息
        String userAgent = request.getHeader("User-Agent");
        for (String keyword : IEBrowserKeyWords) {
            if (userAgent.contains(keyword)) {
                // IE内核浏览器，统一为UTF-8编码显示，并对转换的+进行更正
                return URLEncoder.encode(filename, "UTF-8").replace("+", " ");
            }
        }
        // 火狐等其他浏览器统一为ISO-8859-1编码显示
        return new String(filename.getBytes(StandardCharsets.UTF_8), "ISO-8859-1");


    }

    /**
     * @param response
     * @param identifier
     * @param format
     * @throws Exception
     */
    @ApiOperation("下载元数据描述Void描述文件")
    @GetMapping("/downloadVoidFile")
    public void voidFileDownload(HttpServletResponse response, String identifier, String format) {
        Model model = createVoIDFileUtil.createVoidFileByIdentifier(identifier);
        if (null == model) {
            return;
        }
        final String mimeType;
        String suffix;
        if (format == null || "".equals(format)) {
            format = "TURTLE";
            mimeType = "text/turtle;charset=UTF-8";
            suffix = "ttl";
        } else if (format.equals("Turtle")) {
            format = "TURTLE";
            mimeType = "text/turtle;charset=UTF-8";
            suffix = "ttl";
        } else if (format.equals("RDF/XML")) {
            format = "RDF/XML";
            mimeType = "application/rdf+xml;charset=UTF-8";
            suffix = "rdf";
        } else if (format.equals("N-Triples")) {
            format = "N-TRIPLES";
            mimeType = "application/n-triples;charset=UTF-8";
            suffix = "nt";
        } else if (format.equals("JSON-LD")) {
            format = "N-TRIPLES";
            mimeType = "application/n-triples;charset=UTF-8";
            suffix = "jsonld";
        } else {
            format = "TURTLE";
            mimeType = "text/turtle;charset=UTF-8";
            suffix = "ttl";
        }
        response.setContentType(mimeType);
        // 通知浏览器以下载的方式打开
        String filename = identifier + "." + suffix;
        response.setHeader("Content-Disposition", "attachment;filename=" + filename);
        response.setContentType(String.valueOf(MediaType.APPLICATION_OCTET_STREAM));
        try (PrintWriter out = response.getWriter()) {
            model.write(out, format);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 记录下载相关信息
     *
     * @param request
     * @param url          文件下载地址
     * @param fileType     文件类型
     * @param datacenterId 数据中心id
     */
    @ApiOperation("记录下载量")
    @GetMapping("/addDownloadCount")
    public void addDownloadCount(HttpServletRequest request, String url, String fileType, String datacenterId) {
        String ipAddr = StringUtil.getIpAddr(request);
        dataDownloadDao.addDownLoadCount(ipAddr, url, fileType, datacenterId);
    }

}

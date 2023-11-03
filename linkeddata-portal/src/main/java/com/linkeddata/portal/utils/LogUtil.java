package com.linkeddata.portal.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * 输出日志到指定文件
 */
@Slf4j
@Service
public class LogUtil {
    public static void writeLog(String info, String fileNamePrefix) {
        Date now = new Date();
        // 可以方便地修改日期格式
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        // 可以方便地修改日期格式
        SimpleDateFormat dateFormatFileName = new SimpleDateFormat("yyyy-MM-dd");
        String currentFileName = dateFormatFileName.format(now);
        String os = System.getProperty("os.name");
        String fileName = fileNamePrefix + currentFileName + "log.txt";//文件名及类型
        String path = "/mnt/findrelation";
        // 拼接完整连接
        if (os.toLowerCase().startsWith("win")) {
            path = "E:\\TSTOR\\";
        }
        // 查询路径是否存在不存在就创建
        File dir = new File(path);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                log.info("创建目录{}成功", path);
            }
        }

        FileWriter fw = null;
        File file = new File(path, fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
                fw = new FileWriter(file, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                fw = new FileWriter(file, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        PrintWriter pw = new PrintWriter(fw);

        String current = dateFormat.format(now);
//        pw.println(current + ":" + log + "\n");
        pw.println(info);
        try {
            fw.flush();
            pw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void writeLog(String info) {
        Date now = new Date();
        // 可以方便地修改日期格式
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        // 可以方便地修改日期格式
        SimpleDateFormat dateFormatFileName = new SimpleDateFormat("yyyy-MM-dd");
        String currentFileName = dateFormatFileName.format(now);
        String os = System.getProperty("os.name");
        // 文件名及类型
        String fileName = "findrelation" + currentFileName + "log.txt";
        String path = "/mnt/findrelation";
        // 拼接完整连接
        if (os.toLowerCase().startsWith("win")) {
            path = "E:\\TSTOR\\";
        }
        // 查询路径是否存在不存在就创建
        File dir = new File(path);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                log.info("创建目录{}成功", path);
            }
        }

        FileWriter fw = null;
        File file = new File(path, fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
                fw = new FileWriter(file, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                fw = new FileWriter(file, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        PrintWriter pw = new PrintWriter(fw);

        String current = dateFormat.format(now);
//        pw.println(current + ":" + log + "\n");
        pw.println(info);
        try {
            fw.flush();
            pw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}

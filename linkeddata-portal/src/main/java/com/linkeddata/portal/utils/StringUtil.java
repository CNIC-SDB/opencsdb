package com.linkeddata.portal.utils;


import com.linkeddata.portal.entity.EncodeEntity;
import com.linkeddata.portal.entity.RecordEntity;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 字符串工具类
 *
 * @author wangzhiliang
 * @date 20220809
 */
public class StringUtil {
    /**
     * 获取搜有 unicode 编码 区间的正则表达式
     * 如果没有您需要的可以自行添加
     *
     * @ returns List<EncodeEntity>
     */
    public static List<EncodeEntity> getCodeList() {
        List<EncodeEntity> list = new ArrayList<>();
        //中文
        list.add(new EncodeEntity("[\u4e00-\u9fa5]", "zh"));
        //英文
        list.add(new EncodeEntity("[\u0041-\u005A\u0061-\u007A]", "en"));
        //日语
        list.add(new EncodeEntity("[\u0800-\u4e00]", "ja"));
        //俄语
        list.add(new EncodeEntity("[\u0400-\u052F]", "ru"));
        //韩语
        list.add(new EncodeEntity("[\u1100-\u11FF\u3130-\u318F\uAC00-\uD7AF]", "ko"));
        //泰语
        list.add(new EncodeEntity("[\u0E00-\u0E7F]", "th"));
        //老挝
        list.add(new EncodeEntity("[\u0E80-\u0EFF]", "LA"));
        //藏语
        list.add(new EncodeEntity("[\u0F00-\u0FFF]", "bo"));
        //彝语
        list.add(new EncodeEntity("[\uA000-\uA4CF]", "yi"));
        //蒙古语
        list.add(new EncodeEntity("[\u1800-\u18AF]", "mn"));
        //缅甸语
        list.add(new EncodeEntity("[\u1000-\u109F]", "mm"));
        //高棉文
        list.add(new EncodeEntity("[\u1780-\u17FF]", "Km"));
        //拉丁文Latin
        list.add(new EncodeEntity("[\u00C0-\u02AF\u1E00-\u1EFF]", "la"));
        //希腊文Greek
        list.add(new EncodeEntity("[\u0370-\u03FF\u1F00-\u1FFF\u2C80-\u2CFF]", "gr"));
        //希伯来文 Hebrew
        list.add(new EncodeEntity("[\u0590-\u05FF]", "heb"));
        //阿拉伯文Arabic
        list.add(new EncodeEntity("[\u0600-\u06FF\u0750-\u077F]", "Ar"));
        //叙利亚文Syriac
        list.add(new EncodeEntity("[\u0700-\u074F]", "ar_SY"));

        return list;
    }

    /**
     * 获取字符串编码
     * 如果同时出现两种或两种 都返回;
     *
     * @ param name="info" 要检测的字符串
     * @ returns  所属的语言
     * @author wangzhiliang
     */
    public static List<String> getCodeName(String info) {
        List<EncodeEntity> list = getCodeList();
        List<RecordEntity> getList = new ArrayList<>();
        Pattern pat = null;
        for (EncodeEntity item : list) {
            pat = Pattern.compile(item.getReg());
            if (pat.matcher(info).find()) {
                getList.add(new RecordEntity(info.length() - pat.matcher(info).replaceAll("").length(), item.getZname()));
            }
        }
        List<String> resultList = new ArrayList<>();
        if (getList.size() > 0) {
            for (RecordEntity item : getList) {
                resultList.add(item.getZname());
            }
            return resultList;
        } else {
            System.out.println("没有找到所属的编码");
            return null;
        }
    }

    /**
     * 转换long型数字为 b,kb,mb,gb,tb,pb,eb,zb
     *
     * @param size
     * @return
     */
    public static String readableFileSize(long size) {
        if (size <= 0) {
            return "0";
        }
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    /**
     * 获取真实访问ip
     *
     * @param request
     * @return
     */
    public static String getIpAddr(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        final String UNKNOWN = "unknown";
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }
    /**
     * 转义SPARQL中的特殊字符
     */
    public static String transformSparqlStr(String str) {
        List<String[]> tempReplaceArrList = new ArrayList<>();
        int index = 0;
        tempReplaceArrList.add(new String[]{"\n", "#@#@!@#%&=" + (++index) + "&", "\\\\\\\\n"});
        tempReplaceArrList.add(new String[]{"\\\\", "#@#@!@#%&=" + (++index) + "&", "\\\\\\\\"});
        tempReplaceArrList.add(new String[]{"'", "#@#@!@#%&=" + (++index) + "&", "\\\\'"});
        tempReplaceArrList.add(new String[]{"\\+", "#@#@!@#%&=" + (++index) + "&", "\\\\\\\\+"});
        for (String[] tempReplaceArr : tempReplaceArrList) {
            String oldStr = tempReplaceArr[0];
            String tempStr = tempReplaceArr[1];
            str = str.replaceAll(oldStr, tempStr);
        }
        for (String[] tempReplaceArr : tempReplaceArrList) {
            String tempStr = tempReplaceArr[1];
            String newStr = tempReplaceArr[2];
            str = str.replaceAll(tempStr, newStr);
        }
        return str;
    }
    /**
     * 把输入流的内容转化成字符串
     * 字符串转化为输入流
     * @return
     * @author wangzhiliang
     * @date 20230123
     */
    public static InputStream getStringStream(String sInputString) {
        if (sInputString != null && !sInputString.trim().equals("")) {
            try {
                ByteArrayInputStream tInputStringStream = new ByteArrayInputStream(sInputString.getBytes());
                return tInputStringStream;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }


}

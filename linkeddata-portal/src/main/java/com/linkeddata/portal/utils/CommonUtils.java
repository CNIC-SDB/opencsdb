package com.linkeddata.portal.utils;

/**
 * 通用工具类
 *
 * @author : gaoshuai
 * @date : 2023/5/12 14:55
 */
public class CommonUtils {


    /**
     * 回答问题1时，判断lable是否可以展示到前端
     * 不能全部为数字；只有中文、英文、下划线；长度不大于10，最多出现3个空格
     *
     * @param label
     * @return 0 不符合条件，1 符合条件但不是全中文，2 符合条件且是全中文
     */
    public static int isShowLabel(String label) {
        // 此处长度需要修改为 13，因为加上了最多 3 个空格
        if (label.matches("[0-9]+") || label.length() > 13) {
            // 包含数字或长度大于 13，直接返回 0
            return 0;
        }
        // 使用了Unicode编码中的汉字范围（\u4E00-\u9FA5）来匹配中文字符
        if (!label.matches("[\\u4E00-\\u9FA5a-zA-Z_ ]+")) {
            // 不是中文、英文、下划线和空格的组合，返回 0
            return 0;
        }
        if (label.matches("[\\u4E00-\\u9FA5]+")) {
            // 中文字符串，返回 2
            return 2;
        } else {
            // 英文、下划线和空格的组合，返回 1
            return 1;
        }
    }

    /**
     * 判断字符串是否为全英文
     *
     * @param str
     * @return
     */
    public static boolean isEnglishAndNumber(String str) {
        return str.matches("[a-zA-Z0-9\\p{Punct}\\s]*");
    }


}

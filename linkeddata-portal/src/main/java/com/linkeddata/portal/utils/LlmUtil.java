package com.linkeddata.portal.utils;

import com.alibaba.fastjson2.JSONArray;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * java调用大语言模型
 */
public class LlmUtil {

    /**
     * 将文件发送给大语言模型，返回大语言模型的回答
     *
     * @param question
     * @return String
     */
    public static String queryFromLlm(String question) {
        return exexPython(question);
    }

    /**
     * 执行服务器上的python文件
     *
     * @param question，向llm模型提的问题
     * @return
     */
    private static String exexPython(String question) {
        String result = "";
        try {
            result = HttpUtil.doGet("http://10.0.82.212:5000/query?" + question);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }
  /*  private static String exexPython(String question) {
        System.out.println("进入方法exexPython，问句：" + question);
        try {
            // python命令位置。因为我电脑上有2个python，需要指定python命令的具体位置
            String command = "python";
            // python文件位置  /mnt/llm/xtbg/query_from_vector_with_llm.py
            String pyPath = "/mnt/llm/xtbg/query_from_vector_with_llm.py";
            // 问句，传入到python中的参数
            ProcessBuilder pb = new ProcessBuilder(command, pyPath, question);

            // 启动进程
            Process process = pb.start();

            // 获取进程的输出流
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

            // 读取输出结果
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            // 等待进程执行结束
            int exitCode = process.waitFor();

            // 打印输出结果
            String pythonResult = result.toString();
            System.out.println("exexPython 执行成功，答案：" + pythonResult);
            return pythonResult;
        } catch (Throwable e) {
            System.out.println("exexPython 遇到错误，问句：" + question);
            e.printStackTrace();
        }
        return "false";
    }*/

    //xiajl20230817修改：从python接口中获取metadata信息后，根据主语和谓语拼接后在RDF数据库里找到原文信息;
    public static List<String> getSearchResultEntity(String question) {
        String url = "http://10.0.82.212:5000/queryDocumentList";
        String paramName = "question";
        String answer = HttpUtil.doPost(url, paramName, question);
        String[] split = answer.split("###");
        List<String> list = new ArrayList<>();
        if (split.length > 0) {
            for (int i = 0; i < split.length; i++) {
                if (!list.contains(split[i])) {
                    list.add(split[i]);
                }
            }
        }
//        list.add("白菜、学名:Cheisdonium maus，分类名:物界被子植物直双子叶植物目要科白菜属白菜状种，野外有光的地方，生活举型·多年生营木艺·中空。");
//        list.add("多年生直立草本，具黄色液汁，蓝灰色。根茎褐色，茎直立，圆住形，聚伞状分枝，基生叶羽状全裂，烈片倒明状长圆形、宽倒卵形或被形，边缘状状浅烈或近羽状全裂，具长柄，茎生叶互生，叶片同基生叶，具短柄。花多数，排列成腋生的伞形花序，具苞片。");
//        list.add("白菜学名:Chelidonium malus是科白属的一种多年生草本物，被称为在森林砍期间速生的先锋物。该植物可长5-80厘米高。叶子是根状茎，莲座状，有两片叶子");
        return list;
    }


//xiajl20230817修改：从python接口中获取metadata信息和原文信息;
    //xiajl20230821 修改为Get请求获取值
    public static JSONArray getMedadataEntity(String question) {
        String url = "http://10.0.82.212:5000/queryMetadataList?";
        //String url = "http://localhost:5000/queryMetadataList?";
        String answer = null;
        JSONArray jsonArray = null;
        try {
            answer = HttpUtil.doGet(url + question);
            System.out.println(answer);
            jsonArray = JSONArray.parseArray(answer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonArray;
    }

    /**
     * 返回的字符串形式的列表以三个###分割
     *
     * @param question
     * @return
     */
    private static String execPythonQueryDocumentList(String question) {
        System.out.println("进入方法exexPythonQueryDocumentList，问句：" + question);
        try {
            // python命令位置。因为我电脑上有2个python，需要指定python命令的具体位置
            String command = "python";
//            String command = "D:\\miniconda\\envs\\langchain\\python.exe";
            // python文件位置  /mnt/llm/xtbg/query_from_vector_with_llm.py
            String pyPath = "/mnt/llm/xtbg/query_document_list.py";
//            String pyPath = "E:\\python_project\\chatglm_demo\\GPU服务器的模型\\query_document_list_本地.py";
//            String pyPath = "E:\\python_project\\chatglm_demo\\GPU服务器的模型\\llm_test.py";
            // 问句，传入到python中的参数
            ProcessBuilder pb = new ProcessBuilder(command, pyPath, question);

            // 启动进程
            Process process = pb.start();

            // 获取进程的输出流
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

            // 读取输出结果
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            // 等待进程执行结束
            int exitCode = process.waitFor();

            // 打印输出结果
            String pythonResult = result.toString();
            System.out.println("exexPython 执行成功，答案：" + pythonResult);
            return pythonResult;
        } catch (Throwable e) {
            System.out.println("exexPython 遇到错误，问句：" + question);
            e.printStackTrace();
        }
        return "false";
    }

    /**
     * 只查询llm模型，不接入向量
     *
     * @param question
     * @return
     */
   /* private static String execPythonQueryFromLlm(String question) {
        System.out.println("进入方法execPythonQueryFromLlm，问句：" + question);
        try {
            // python命令位置。因为我电脑上有2个python，需要指定python命令的具体位置
            String command = "python";
//            String command = "D:\\miniconda\\envs\\langchain\\python.exe";
            // python文件位置  /mnt/llm/xtbg/query_from_vector_with_llm.py
            String pyPath = "/mnt/llm/xtbg/query_from_llm_text2.py";
//            String pyPath = "E:\\python_project\\chatglm_demo\\GPU服务器的模型\\query_document_list_本地.py";
//            String pyPath = "E:\\python_project\\chatglm_demo\\GPU服务器的模型\\llm_test.py";
            // 问句，传入到python中的参数
            ProcessBuilder pb = new ProcessBuilder(command, pyPath, question);

            // 启动进程
            Process process = pb.start();

            // 获取进程的输出流
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

            // 读取输出结果
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            // 等待进程执行结束
            int exitCode = process.waitFor();

            // 打印输出结果
            String pythonResult = result.toString();
            System.out.println("exexPython 执行成功，答案：" + pythonResult);
            return pythonResult;
        } catch (Throwable e) {
            System.out.println("exexPython 遇到错误，问句：" + question);
            e.printStackTrace();
        }
        return "false";
    }*/

    /**
     * 20230726 通过拼接问句获取大语言模型返回的结果字符串
     *
     * @param question
     * @return
     */
    public static String queryResultForLlmContact(String question) {
        String url = "http://10.0.82.212:5000/queryWithOutEmbedding";
        String paramName = "question";
        String result = HttpUtil.doPost(url, paramName, question);
        return result;
    }

    /**
     * 使用百川模型进行问答
     *
     * @param question
     * @return String 答案
     */
    public static String queryBaichuan(String question) {
        Date date1 = new Date();
        String url = "http://10.0.82.212:5000/queryBaichuan";
        String paramName = "question";
        String result = HttpUtil.doPost(url, paramName, question);
        Date date2 = new Date();
        System.out.println("================================= 提交到大模型的新问句 =================================\n");
        System.out.println(question);
        System.out.println("\n================================= 用时：" + ((date2.getTime() - date1.getTime()) / 1000 + 1) + "秒 =================================\n");
        return result;
    }

    /**
     * 从大模型中根据问句1-4获取查询结果来源信息
     */
    @Test
    public void testExample1() {
        String question = "经过对多个数据源进行检索，我们发现药材、植物、病毒、化合物、药物、" +
                "临床试验等6种类型的数据可以用来建立对新冠病毒有影响的植物有的关联。通过这种语义检索，" +
                "我们发现有18种植物可以对新冠病毒产生影响,其中包括地不容、欧薄荷、水飞蓟、山丝苗、" +
                "Stephania、Stephania、Cannabis、Kanabo、Silybum、Mentha等,具体信息如下图谱和列表所示:" +
                ",根据上述内容请回答:'对新冠病毒有影响的植物都有哪些?这些植物的分布地是哪里?',请用中文回答。";
        List<String> searchResultEntity = getSearchResultEntity(question);
        System.out.println(searchResultEntity);
    }


}

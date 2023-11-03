package com.linkeddata.portal.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLEncoder;

/**
 *
 * @author wangzl
 */
public class ShellExecuteUtil {
    public static void main(String[] args) {
        // Execute the command in windows
        String spa = "CONSTRUCT+%7B%3Fs++%3Chttp%3A%2F%2Fwww.w3.org%2F2000%2F01%2Frdf-schema%23label%3E++%27%E7%89%A1%E4%B8%B9%27%40zh+.%7D+where+%7B++%3Fs++%3Chttp%3A%2F%2Fwww.w3.org%2F2000%2F01%2Frdf-schema%23label%3E++%27%E7%89%A1%E4%B8%B9%27%40zh+.+%7D";

        String command = "cmd /c \"curl https://query.wikidata.org/sparql?query="+spa+"\"";
        String output = executeCommand(command);
        System.out.println(output);
    }

    public static String executeCommand(String command) {
        StringBuffer output = new StringBuffer();
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(command);
//            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";
            while ((line = reader.readLine())!= null) {
                output.append(line + "\n");
            }
        } catch (Exception e) {
            System.out.println("CURL :"+command);
            e.printStackTrace();
        }finally {
            if(null != p){
                p.destroy();
            }

        }
        return output.toString();
    }

}

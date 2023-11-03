/*
package com.linkeddata.portal.utils;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.jcraft.jsch.*;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

*/
/**
 * 执行Shell工具类
 *
 * @author : gaoshuai
 * @date : 2023/3/28 10:09
 * <p>
 * 获取ExecuteShellUtil类实例对象
 * @return 实例
 * @date 2019/4/29 16:58
 * <p>
 * 关闭连接
 * <p>
 * 初始化
 * @param ip       远程Linux地址
 * @param port     端口
 * @param username 用户名
 * @param password 密码
 * @throws JSchException JSch异常
 * @date 2019/3/15 12:41
 * <p>
 * 执行一条命令
 * <p>
 * 执行一条命令 获取错误流中的内容
 *//*

public class ExecuteShellUtil {
    private static Session session;

    private ExecuteShellUtil() {
    }

    */
/**
 * 获取ExecuteShellUtil类实例对象
 *
 * @return 实例
 * @date 2019/4/29 16:58
 *//*

    public static ExecuteShellUtil getInstance() {
        return new ExecuteShellUtil();
    }

    */
/**
 * 关闭连接
 *//*

    public static void closeConnect() {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    */
/**
 * 初始化
 *
 * @param ip       远程Linux地址
 * @param port     端口
 * @param username 用户名
 * @param password 密码
 * @throws JSchException JSch异常
 * @date 2019/3/15 12:41
 *//*

    public void init(String ip, Integer port, String username, String password) throws JSchException {
        JSch jsch = new JSch();
        session = jsch.getSession(username, ip, port);
        session.setPassword(password);
        Properties sshConfig = new Properties();
        sshConfig.put("StrictHostKeyChecking", "no");
        session.setConfig(sshConfig);
        session.connect(1200 * 1000);
    }

    */
/**
 * 执行一条命令
 *//*

    public String execCmd(String command) throws Exception {
        // 打开执行shell指令的通道
        Channel channel = session.openChannel("exec");
        ChannelExec channelExec = (ChannelExec) channel;
        channelExec.setCommand("source /etc/profile && source ~/.bash_profile && source ~/.bashrc &&  adb devices && locale charmap");
        channelExec.setCommand(command);
        channel.setInputStream(null);
        channelExec.setErrStream(System.err);
        channel.connect();

        StringBuilder sb = new StringBuilder(16);
        try (InputStream in = channelExec.getInputStream();
             InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {
            String buffer;
            while ((buffer = reader.readLine()) != null) {
                sb.append("\n").append(buffer);
            }

            //2023-02-21 关闭流
            IoUtil.close(reader);
            IoUtil.close(isr);
            IoUtil.close(in);

            return sb.toString();
        } finally {
            if (channelExec != null && channelExec.isConnected()) {
                channelExec.disconnect();
            }
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
        }
    }

    */
/**
 * 执行一条命令 获取错误流中的内容
 *//*

    public String execCmdErrContent(String command) throws Exception {
        // 打开执行shell指令的通道
        Channel channel = session.openChannel("exec");
        ChannelExec channelExec = (ChannelExec) channel;
        channelExec.setCommand(command);
        channel.setInputStream(null);
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        channelExec.setErrStream(err);
        channel.connect();
        StringBuilder sb = new StringBuilder(16);
        try (InputStream in = channelExec.getErrStream();
             InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {
            String buffer;
            while ((buffer = reader.readLine()) != null) {
                sb.append("\n").append(buffer);
            }

            //2023-02-21 关闭流
            IoUtil.close(reader);
            IoUtil.close(isr);
            IoUtil.close(in);
            IoUtil.close(err);

            if (StrUtil.contains(sb.toString(), "没有那个文件或目录")) {
                return "";
            } else {
                return sb.toString();
            }
        } finally {
            if (channelExec != null && channelExec.isConnected()) {
                channelExec.disconnect();
            }
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
        }
    }
}
*/

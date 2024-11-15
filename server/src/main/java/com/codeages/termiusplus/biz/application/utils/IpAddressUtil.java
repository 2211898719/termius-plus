package com.codeages.termiusplus.biz.application.utils;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.lionsoul.ip2region.xdb.Searcher;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * IP地址Util
 */
@Slf4j
public class IpAddressUtil {

    // ip2region.xdb 文件地址常量（本地xdb文件路径）
    public static String XDB_PATH = "/Users/hongjunlong/Downloads/csdn-ip2region.xdb";

    /**
     * 获取IP地址：
     */
    public static String getIpAddress(HttpServletRequest request) {
        String ipAddress = null;
        try {
            ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress != null && ipAddress.length() != 0 && !"unknown".equalsIgnoreCase(ipAddress)) {
                // 多次反向代理后会有多个ip值，第一个ip才是真实ip
                if (ipAddress.contains(",")) {
                    ipAddress = ipAddress.split(",")[0];
                }
            }
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("HTTP_CLIENT_IP");
            }
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.error("获取IP地址异常,{}", e.getMessage());
        }
        return ipAddress;
    }

    /**
     * 获取mac地址
     */
    public static String getMacIpAddress() {
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            byte[] macAddressBytes = NetworkInterface.getByInetAddress(inetAddress).getHardwareAddress();
            // 将mac地址拼装成String
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < macAddressBytes.length; i++) {
                if (i != 0) {
                    sb.append("-");
                }
                // mac[i] & 0xFF 是为了把byte转化为正整数
                String s = Integer.toHexString(macAddressBytes[i] & 0xFF);
                sb.append(s.length() == 1 ? 0 + s : s);
            }
            return sb.toString().trim().toUpperCase();
        } catch (Exception e) {
            log.error("Mac获取IP地址异常,{}", e.getMessage());
        }
        return "";
    }

    /**
     * 方法一：完全基于ip2region.xdb文件，对用户ip地址进行转换
     * 注：并发调用时，每个线程需创建一个独立的searcher对象 单独使用。
     */
    public static String getIpPossessionByFile(String ip) {
        if (StringUtils.isNotEmpty(ip)) {
            try {
                // 1、创建 searcher 对象
                Searcher searcher = Searcher.newWithFileOnly(XDB_PATH);
                // 2、查询
                long sTime = System.nanoTime();
                String region = searcher.search(ip);
                long cost = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - sTime);
                region = region.replace("|0", "");
                //log.info("{地区: {}, IO操作数: {}, 耗时: {} μs}", region, searcher.getIOCount(), cost);
                return region;
            } catch (Exception e) {
                log.error("获取IP地址异常：{} ", e.getMessage());
                throw new RuntimeException("获取IP地址异常");
            }
        }
        return "未知";
    }

    /**
     * 方法二：缓存 VectorIndex 索引，对用户ip地址进行转换
     * 注：每个线程需要单独创建一个独立的 Searcher 对象，但是都共享全局变量 vIndex 缓存。
     */
    public static String getCityInfoByVectorIndex(String ip) {
        if (StringUtils.isNotEmpty(ip)) {
            try {
                // 1、从 XDB_PATH 中预先加载 VectorIndex 缓存，并且作为全局变量，后续反复使用。
                byte[] vIndex = Searcher.loadVectorIndexFromFile(XDB_PATH);
                // 2、使用全局的 vIndex 创建带 VectorIndex 缓存的查询对象。
                Searcher searcher = Searcher.newWithVectorIndex(XDB_PATH, vIndex);
                // 3、查询
                long sTime = System.nanoTime();
                String region = searcher.search(ip);
                long cost = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - sTime);
                region = region.replace("|0", "");
                //log.info("{地区: {}, IO操作数: {}, 耗时: {} μs}", region, searcher.getIOCount(), cost);
                return region;
            } catch (Exception e) {
                log.error("获取IP地址异常 ", e);
                throw new RuntimeException("获取IP地址异常");
            }
        }
        return "未知";
    }

    /**
     * 方法三：缓存整个 xdb 数据，对用户ip地址进行转换
     * 注：并发使用时，用整个 xdb 数据缓存创建的查询对象可以安全的用于并发，也就是你可以把这个 searcher 对象做成全局对象去跨线程访问。
     */
    public static String getCityInfoByMemorySearch(String ip) {
        if (StringUtils.isNotEmpty(ip)) {
            try {
                // 1、从 XDB_PATH 加载整个 xdb 到内存。
                byte[] cBuff = Searcher.loadContentFromFile(XDB_PATH);
                // 2、使用上述的 cBuff 创建一个完全基于内存的查询对象。
                Searcher searcher = Searcher.newWithBuffer(cBuff);
                // 3、查询
                long sTime = System.nanoTime();
                String region = searcher.search(ip);
                long cost = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - sTime);
                region = region.replace("|0", "");
                //log.info("{地区: {}, IO操作数: {}, 耗时: {} μs}", region, searcher.getIOCount(), cost);
                return region;
            } catch (Exception e) {
                log.error("获取IP地址异常：{} ", e.getMessage());
                throw new RuntimeException("获取IP地址异常");
            }
        }
        return "未知";
    }

    /**
     * 方法四：在线获取IP地址
     * 注：通过别人或者官网提供的API接口去实现查询的功能，弊端就是特别依赖别人的服务器，一旦服务器宕机就无法访问了。
     */
    public static String getIpAddressByOnline(String ip) {

        try {
            //1、创建 URLConnction
            URL url = new URL("http://ip-api.com/json/" + ip + "?lang=zh-CN");

            //2、设置connection的属性
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("content-type", "application/json; charset=utf-8");

            //3.连接
            connection.connect();

            //4.获取内容
            InputStream inputStream = connection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            //System.out.println(sb);

            String str = sb.toString();
            if (StringUtils.isNotEmpty(str)) {
                // string转map
                Gson gson = new Gson();
                Map<String, Object> map = new HashMap<>();
                map = gson.fromJson(str, map.getClass());
                String country = (String) map.get("country");
                String city = (String) map.get("city");
                String regionName = (String) map.get("regionName");
                //log.info("【国家】{}，【城市】{}，【地区】{}", country, city, regionName);
                return country + "|" + city + "|" + regionName;
            }
        } catch (Exception e) {
            log.error("在线查询IP地址异常，{}", e.getMessage());
            throw new RuntimeException("在线查询IP地址异常");
        }
        return null;
    }

    /**
     * 根据IP地址 获取归属地
     */
    public static String getIpPossession(String ipAddress) {
        if (StringUtils.isNotEmpty(ipAddress)) {
            ipAddress = ipAddress.replace("|", " ");
            String[] cityList = ipAddress.split(" ");
            if (cityList.length > 0) {
                // 国内的显示到具体的省
                if ("中国".equals(cityList[0])) {
                    if (cityList.length > 1) {
                        return cityList[1];
                    }
                }
                // 国外显示到国家
                return cityList[0];
            }
        }
        return "未知";
    }

}

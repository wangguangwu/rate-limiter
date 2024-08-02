package com.wangguangwu.distributedratelimiter.util;

import com.wangguangwu.distributedratelimiter.context.RequestContext;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 获取客户端 IP 地址的工具方法。
 * 通过分析请求头中的多个可能的 IP 地址字段，尝试获取真实的客户端 IP。
 *
 * @author wangguangwu
 */
public class IpAddressUtil {

    private static final String UNKNOWN = "unknown";

    /**
     * 获取客户端 IP 地址。
     * <p>
     * 该方法通过检查多个常见的 HTTP 请求头字段，尝试获取客户端的真实 IP 地址。
     * 如果所有的字段都无法提供有效的 IP 地址，则返回请求的远程地址。
     * </p>
     *
     * @return 客户端的 IP 地址
     */
    public static String getIpAddress() {
        // 获取当前 HTTP 请求
        HttpServletRequest request = RequestContext.getRequest();

        // 尝试从 "x-forwarded-for" 头字段中获取 IP 地址
        String ip = request.getHeader("x-forwarded-for");

        // 如果 "x-forwarded-for" 头字段无效，尝试从其他头字段中获取 IP 地址
        if (isIpInvalid(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (isIpInvalid(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }

        // 如果上述头字段都无法提供有效的 IP 地址，则使用请求的远程地址
        if (isIpInvalid(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }

    /**
     * 判断 IP 地址是否无效。
     * <p>
     * 如果 IP 地址为空，或者值为 "unknown"（忽略大小写），则认为 IP 地址无效。
     * </p>
     *
     * @param ip 待检查的 IP 地址
     * @return 如果 IP 地址无效，返回 true；否则返回 false
     */
    private static boolean isIpInvalid(String ip) {
        return ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip);
    }
}
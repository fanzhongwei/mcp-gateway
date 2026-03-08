package com.mmyf.commons.util.logback;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import ch.qos.logback.core.PropertyDefinerBase;

/**
 * 日志ip获取Property
 * LogbackIpGetProperty
 * @author Teddy
 * @version 1.0
 */
@Slf4j
public class LogbackIpGetProperty extends PropertyDefinerBase {

    /**默认值*/
    private final static String DEFAULT_VALUE = "ip";

    @Override
    public String getPropertyValue() {
        Enumeration<NetworkInterface> netInterfaces = null;
        List<String> ipList = new ArrayList<String>();
        try {
            netInterfaces = NetworkInterface.getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface ni = netInterfaces.nextElement();
                Enumeration<InetAddress> ips = ni.getInetAddresses();
                while (ips.hasMoreElements()) {
                    InetAddress inet = ips.nextElement();
                    if (inet instanceof Inet4Address && (!inet.isLoopbackAddress())) {
                        ipList.add(inet.getHostAddress());
                    }
                }
            }
            if (CollectionUtils.isEmpty(ipList)) {
                InetAddress addr = InetAddress.getLocalHost();
                ipList.add(addr.getHostAddress());
                if (log.isDebugEnabled()) {
                    log.debug(String.format("读取到本机IP地址为【%s】", addr.getHostAddress()));
                }
            }
            return StringUtils.join(ipList.toArray(), "_");
        } catch (Exception e) {
            log.error("获取本级网络IP地址出现异常", e);
        }
        return DEFAULT_VALUE;

    }

}

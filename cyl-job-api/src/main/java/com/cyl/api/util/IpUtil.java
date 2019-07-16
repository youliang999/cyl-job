package com.cyl.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.regex.Pattern;

public class IpUtil {
    private static final Logger logger = LoggerFactory.getLogger(IpUtil.class);
    private static final String ANYHOST = "0.0.0.0";
    private static final String LOCALHOST = "127.0.0.1";
    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");
    private static volatile InetAddress LOCAL_ADDRESS = null;

    public IpUtil() {
    }

    private static boolean isValidAddress(InetAddress address) {
        if (address != null && !address.isLoopbackAddress()) {
            String name = address.getHostAddress();
            return name != null && !"0.0.0.0".equals(name) && !"127.0.0.1".equals(name) && IP_PATTERN.matcher(name).matches();
        } else {
            return false;
        }
    }

    private static boolean isValidV6Address(Inet6Address address) {
        boolean preferIpv6 = Boolean.getBoolean("java.net.preferIPv6Addresses");
        if (!preferIpv6) {
            return false;
        } else {
            try {
                return address.isReachable(100);
            } catch (IOException var3) {
                return false;
            }
        }
    }

    private static InetAddress normalizeV6Address(Inet6Address address) {
        String addr = address.getHostAddress();
        int i = addr.lastIndexOf(37);
        if (i > 0) {
            try {
                return InetAddress.getByName(addr.substring(0, i) + '%' + address.getScopeId());
            } catch (UnknownHostException var4) {
                logger.debug("Unknown IPV6 address: ", var4);
            }
        }

        return address;
    }

    private static InetAddress getLocalAddress0() {
        InetAddress localAddress = null;

        try {
            localAddress = InetAddress.getLocalHost();
            if (localAddress instanceof Inet6Address) {
                Inet6Address address = (Inet6Address)localAddress;
                if (isValidV6Address(address)) {
                    return normalizeV6Address(address);
                }
            } else if (isValidAddress(localAddress)) {
                return localAddress;
            }
        } catch (Throwable var7) {
            logger.error(var7.getMessage(), var7);
        }

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (null == interfaces) {
                return localAddress;
            }

            while(interfaces.hasMoreElements()) {
                try {
                    NetworkInterface network = (NetworkInterface)interfaces.nextElement();
                    Enumeration addresses = network.getInetAddresses();

                    while(addresses.hasMoreElements()) {
                        try {
                            InetAddress address = (InetAddress)addresses.nextElement();
                            if (address instanceof Inet6Address) {
                                Inet6Address v6Address = (Inet6Address)address;
                                if (isValidV6Address(v6Address)) {
                                    return normalizeV6Address(v6Address);
                                }
                            } else if (isValidAddress(address)) {
                                return address;
                            }
                        } catch (Throwable var6) {
                            logger.error(var6.getMessage(), var6);
                        }
                    }
                } catch (Throwable var8) {
                    logger.error(var8.getMessage(), var8);
                }
            }
        } catch (Throwable var9) {
            logger.error(var9.getMessage(), var9);
        }

        return localAddress;
    }

    public static InetAddress getLocalAddress() {
        if (LOCAL_ADDRESS != null) {
            return LOCAL_ADDRESS;
        } else {
            InetAddress localAddress = getLocalAddress0();
            LOCAL_ADDRESS = localAddress;
            return localAddress;
        }
    }

    public static String getIp() {
        return getLocalAddress().getHostAddress();
    }

    public static String getIpPort(int port) {
        String ip = getIp();
        return getIpPort(ip, port);
    }

    public static String getIpPort(String ip, int port) {
        return ip == null ? null : ip.concat(":").concat(String.valueOf(port));
    }

    public static Object[] parseIpPort(String address) {
        String[] array = address.split(":");
        String host = array[0];
        int port = Integer.parseInt(array[1]);
        return new Object[]{host, port};
    }
}

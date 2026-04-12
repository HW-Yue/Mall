package cn.bugstack.mcp.server.computer.test;

import java.util.Properties;

public class ApiTest {

    public static void main(String[] args) {
        // 获取系统属性
        Properties properties = System.getProperties();

        // 操作系统名称
        String osName = properties.getProperty("os.name");
        // 操作系统版本
        String osVersion = properties.getProperty("os.version");
        // 操作系统架构
        String osArch = properties.getProperty("os.arch");
        // 用户的账户名称
        String userName = properties.getProperty("user.name");
        // 用户的主目录
        String userHome = properties.getProperty("user.home");
        // 用户的当前工作目录
        String userDir = properties.getProperty("user.dir");
        // Java 运行时环境版本
        String javaVersion = properties.getProperty("java.version");

        System.out.println("操作系统名称: " + osName);
        System.out.println("操作系统版本: " + osVersion);
        System.out.println("操作系统架构: " + osArch);
        System.out.println("用户名称: " + userName);
        System.out.println("用户主目录: " + userHome);
        System.out.println("用户当前工作目录: " + userDir);
        System.out.println("Java 版本: " + javaVersion);

        // 根据操作系统执行特定的命令来获取更多信息
        if (osName.toLowerCase().contains("win")) {
            // Windows特定的代码
            getWindowsSpecificInfo();
        } else if (osName.toLowerCase().contains("mac")) {
            // macOS特定的代码
            getMacSpecificInfo();
        } else if (osName.toLowerCase().contains("nix") || osName.toLowerCase().contains("nux")) {
            // Linux特定的代码
            getLinuxSpecificInfo();
        }
    }

    private static void getWindowsSpecificInfo() {
        // Windows特定的系统信息获取
        try {
            Process process = Runtime.getRuntime().exec("systeminfo");
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream(), java.nio.charset.Charset.forName("GBK"))
            );
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void getMacSpecificInfo() {
        // macOS特定的系统信息获取
        try {
            Process process = Runtime.getRuntime().exec("system_profiler SPHardwareDataType");
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void getLinuxSpecificInfo() {
        // Linux特定的系统信息获取
        try {
            Process process = Runtime.getRuntime().exec("lshw -short");
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

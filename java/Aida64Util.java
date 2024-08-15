package com.ly.irobot.utils;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinReg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.TreeMap;

public class Aida64Util {
    private static final Logger log = LoggerFactory.getLogger(Aida64Util.class);
    private static final String AIDA64_SHARED_MEMORY_NAME = "AIDA64_SensorValues";
    private static final String AIDA64_SHARED_REGISTRY_PATH = "Software\\FinalWire\\AIDA64\\SensorValues";

    /**
     * 使用前确保Aida64中开启 共享注册表
     * @return 传感器信息
     */
    public static TreeMap<String,Object> getSensorsInfoFromRegistry() {
        TreeMap<String,Object> registry = null;
        try {
            registry = Advapi32Util.registryGetValues(WinReg.HKEY_CURRENT_USER, AIDA64_SHARED_REGISTRY_PATH);
        } catch (Exception exception) {
            log.error("无法获取Aida64共享注册表信息! Error Season: {}", exception.getMessage());
        }
        return registry;
    }

    /***
     * 使用前确保Aida64中开启 共享内存
     * @return 传感器信息
     */
    public static String getSensorsInfoFromSharedMemory() {
        String info = sharedMemoryRead();
        return null == info || "".equals(info) ? null:"<root>" + info + "</root>";
    }

    /***
     * 读取共享内存数据
     * @return 传感器信息,失败返回 null
     */
    private static String sharedMemoryRead() {
        WinNT.HANDLE hMapping = null;
        Pointer pBuffer = null;
        byte[] dOutput = null;
        try {
            WinBase.SYSTEM_INFO systemInfo = new WinBase.SYSTEM_INFO();
            Kernel32.INSTANCE.GetSystemInfo(systemInfo);
            int pageSize = systemInfo.dwPageSize.intValue();

            // 调用 OpenFileMappingA 函数
            hMapping = Kernel32.INSTANCE.OpenFileMappingA(WinBase.FILE_MAP_READ, false, AIDA64_SHARED_MEMORY_NAME);
            if (hMapping == null || hMapping.equals(WinBase.INVALID_HANDLE_VALUE)) {
                log.error("无法打开Aida64共享内存文件映射对象。Kernel32.dll [OpenFileMappingA] Error code: {}", Kernel32.INSTANCE.GetLastError());
                return null;
            }

            // 调用 MapViewOfFile 函数
            pBuffer = Kernel32.INSTANCE.MapViewOfFile(hMapping, WinBase.FILE_MAP_READ, 0, 0, 0);
            if (pBuffer == null) {
                log.error("无法创建Aida64共享内存映射视图。Kernel32.dll [MapViewOfFile] Error code: {}" , Kernel32.INSTANCE.GetLastError());
                return null;
            }
            // 根据实际长度创建字节数组
            dOutput = new byte[16384];

            // 重新读取一次数据，填充到 dOutput 中
            int offset = 0;
            while (true) {
                // 从共享内存中读取指定大小的数据到字节数组
                byte[] buffer = pBuffer.getByteArray(0, pageSize);
                if (buffer == null || buffer.length == 0) {
                    break;
                }
                System.arraycopy(buffer, 0, dOutput, offset, buffer.length);
                offset += buffer.length;
                // 更新指针以读取下一页数据
                pBuffer = pBuffer.share(pageSize);
                // 检查是否已经达到了缓冲区的末尾
                if (buffer[buffer.length - 1] == 0) {
                    break;
                }
            }
            System.out.println("offset="+offset);
        } catch (Exception ex) {
            log.error("Aida64共享内存读取出错 ex:{}", ex.getMessage());
            return null;
        } finally {
            // 取消映射视图
            if (pBuffer != null) Kernel32.INSTANCE.UnmapViewOfFile(pBuffer);
            // 关闭文件映射对象
            if (hMapping != null) Kernel32.INSTANCE.CloseHandle(hMapping);
        }

        return new String(dOutput, StandardCharsets.UTF_8);
    }

    /**
     * 对应kernel32.dll中的函数
     */
    private interface Kernel32 extends com.sun.jna.Library {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);
        void GetSystemInfo(WinBase.SYSTEM_INFO lpSystemInfo);
        WinNT.HANDLE OpenFileMappingA(int dwDesiredAccess, boolean bInheritHandle, String lpName);
        Pointer MapViewOfFile(WinNT.HANDLE hFileMappingObject, int dwDesiredAccess, long dwFileOffsetHigh, long dwFileOffsetLow, long dwNumberOfBytesToMap);
        void UnmapViewOfFile(Pointer lpBaseAddress);
        void CloseHandle(WinNT.HANDLE hObject);
        int GetLastError();
    }
}

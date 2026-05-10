package com.osh.ai.assistant.common.util;

import com.osh.ai.assistant.common.constants.CommonConstants;
import com.osh.ai.assistant.common.ex.BizEx;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import cn.hutool.core.io.FileUtil;

import java.io.File;


@Slf4j
public class PathUtil {
    /**
     * 校验是否是绝对路径,并创建目录
     */
    public static void checkAndCreateIfNotExist(String absPath) {
        File file = new File(absPath);
        if (!file.isAbsolute()) {
            // new File("a/bc")会在项目根目录创建
            throw new BizEx("必须指定绝对路径");
        }
        createDir(file);
    }

    /**
     * 如果目录不存在则创建
     * @param dirFile 待创建目录
     */
    public static void createDir(File dirFile) {
        try {
            FileUtils.forceMkdir(dirFile);
        }catch (Exception e) {
            log.error("make dir error,dir:{}",dirFile.getAbsolutePath(),e);
            throw new BizEx("创建文件夹:"+ dirFile.getAbsolutePath() + "失败");
        }
    }

    /**
     * 路径统一转为/分隔后并以/结尾返回
     */
    public static String normalize(String dirAbsPath) {
        // 统一转为/,但尾部不一定有/
        String normalized = FileUtil.normalize(dirAbsPath);
        if (normalized.endsWith(CommonConstants.SLASH)) {
            return dirAbsPath;
        }
        return dirAbsPath + CommonConstants.SLASH;
    }
}

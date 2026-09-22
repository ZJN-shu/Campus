package com.campus.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.campus.config.OssConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class OssService {

    @Resource
    private OSS ossClient;

    @Resource
    private OssConfig ossConfig;

    /** 允许的上传目录（防路径穿越） */
    private static final Set<String> ALLOWED_DIRS = Set.of("avatar", "post", "product", "common", "errand");

    /** 各目录最大文件大小（字节） */
    private static final Map<String, Long> DIR_MAX_SIZE = Map.of(
            "avatar", 5 * 1024 * 1024L,   // 头像 5MB
            "post", 10 * 1024 * 1024L,     // 帖子图片 10MB
            "product", 10 * 1024 * 1024L,  // 商品图片 10MB
            "common", 10 * 1024 * 1024L,   // 通用 10MB
            "errand", 5 * 1024 * 1024L     // 跑腿 5MB
    );

    /**
     * 图片文件魔数表（用于校验文件真实类型，防止伪造扩展名）
     */
    private static final Map<String, byte[]> MAGIC_BYTES = new LinkedHashMap<>();
    static {
        MAGIC_BYTES.put("jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        MAGIC_BYTES.put("png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
        MAGIC_BYTES.put("gif", new byte[]{0x47, 0x49, 0x46, 0x38});
        MAGIC_BYTES.put("webp", new byte[]{0x52, 0x49, 0x46, 0x46}); // RIFF....WEBP
    }

    /**
     * 上传文件
     *
     * @param file 上传的文件
     * @param dir  存储目录（avatar / post / product / common / errand）
     * @return 文件访问 URL
     */
    public String upload(MultipartFile file, String dir) {
        // 1. 校验目录合法性（防路径穿越）
        validateDir(dir);

        // 2. 校验文件（大小 + 类型 + 魔数）
        validateFile(file, dir);

        // 3. 生成唯一文件名：日期目录/UUID.扩展名
        String realExt = detectExtension(file);
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String objectKey = String.format("%s/%s/%s%s", dir, dateDir,
                UUID.randomUUID().toString().replace("-", ""), realExt);

        // 4. 上传到 OSS
        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());
            // 设置缓存控制：浏览器缓存 7 天
            metadata.setCacheControl("max-age=604800");

            ossClient.putObject(ossConfig.getBucketName(), objectKey, inputStream, metadata);

            String url = ossConfig.getUrlPrefix() + "/" + objectKey;
            log.info("文件上传成功: dir={}, size={}, url={}", dir, file.getSize(), url);
            return url;

        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 生成签名 URL（临时访问私有文件）
     *
     * @param objectKey      OSS 对象 Key
     * @param expireMinutes  过期时间（分钟）
     * @return 签名 URL
     */
    public String generateSignedUrl(String objectKey, int expireMinutes) {
        Date expiration = new Date(System.currentTimeMillis() + (long) expireMinutes * 60 * 1000);
        URL signedUrl = ossClient.generatePresignedUrl(ossConfig.getBucketName(), objectKey, expiration);
        return signedUrl.toString();
    }

    /**
     * 删除文件
     *
     * @param fileUrl 文件 URL
     */
    public void delete(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(ossConfig.getUrlPrefix())) {
            return;
        }
        String objectKey = fileUrl.substring(ossConfig.getUrlPrefix().length() + 1);
        // 防止路径穿越
        if (objectKey.contains("..")) {
            log.warn("检测到路径穿越尝试: {}", fileUrl);
            return;
        }
        try {
            ossClient.deleteObject(ossConfig.getBucketName(), objectKey);
            log.info("文件删除成功: {}", objectKey);
        } catch (Exception e) {
            log.error("文件删除失败: {}", objectKey, e);
        }
    }

    // ==================== 安全校验方法 ====================

    /**
     * 校验目录合法性
     */
    private void validateDir(String dir) {
        if (dir == null || !ALLOWED_DIRS.contains(dir)) {
            throw new RuntimeException("不允许的上传目录: " + dir);
        }
    }

    /**
     * 校验文件：大小 + Content-Type + 魔数
     */
    private void validateFile(MultipartFile file, String dir) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        // 大小校验（按目录区分）
        long maxSize = DIR_MAX_SIZE.getOrDefault(dir, 10 * 1024 * 1024L);
        if (file.getSize() > maxSize) {
            throw new RuntimeException("文件大小不能超过 " + (maxSize / 1024 / 1024) + "MB");
        }

        // Content-Type 校验
        String contentType = file.getContentType();
        Set<String> allowedTypes = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new RuntimeException("不支持的文件类型: " + contentType);
        }

        // 魔数校验（读取文件头真实字节，防止伪造扩展名）
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = is.read(header);
            if (read < 4) {
                throw new RuntimeException("文件内容异常：过小");
            }
            if (!matchesMagicBytes(header)) {
                throw new RuntimeException("文件内容与扩展名不匹配，疑似伪造文件");
            }
        } catch (IOException e) {
            throw new RuntimeException("文件校验失败: " + e.getMessage());
        }
    }

    /**
     * 检测文件魔数，返回真实扩展名
     */
    private String detectExtension(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            is.read(header);

            if (matchesBytes(header, MAGIC_BYTES.get("jpg"))) return ".jpg";
            if (matchesBytes(header, MAGIC_BYTES.get("png"))) return ".png";
            if (matchesBytes(header, MAGIC_BYTES.get("gif"))) return ".gif";
            // WebP: RIFF????WEBP
            if (matchesBytes(header, MAGIC_BYTES.get("webp")) && header.length >= 12
                    && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50) {
                return ".webp";
            }
        } catch (IOException e) {
            log.warn("魔数检测失败，使用默认扩展名", e);
        }
        return ".jpg";
    }

    /**
     * 检查文件头是否匹配任一已知魔数
     */
    private boolean matchesMagicBytes(byte[] header) {
        for (Map.Entry<String, byte[]> entry : MAGIC_BYTES.entrySet()) {
            if (matchesBytes(header, entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 字节前缀匹配
     */
    private boolean matchesBytes(byte[] data, byte[] magic) {
        if (data == null || magic == null || data.length < magic.length) return false;
        for (int i = 0; i < magic.length; i++) {
            if (data[i] != magic[i]) return false;
        }
        return true;
    }
}

package com.campus.controller;

import com.campus.dto.Result;
import com.campus.service.OssService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/upload")
public class UploadController {

    @Resource
    private OssService ossService;

    /**
     * 上传单个文件
     *
     * @param file 文件
     * @param dir  存储目录（avatar / post / product）
     * @return 文件 URL
     */
    @PostMapping
    public Result upload(@RequestParam("file") MultipartFile file,
                         @RequestParam(value = "dir", defaultValue = "common") String dir) {
        String url = ossService.upload(file, dir);
        return Result.ok(url);
    }

    /**
     * 批量上传文件
     *
     * @param files 文件数组
     * @param dir   存储目录
     * @return 文件 URL 列表
     */
    @PostMapping("/batch")
    public Result uploadBatch(@RequestParam("files") MultipartFile[] files,
                              @RequestParam(value = "dir", defaultValue = "common") String dir) {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(ossService.upload(file, dir));
        }
        return Result.ok(urls);
    }

    /**
     * 删除文件
     *
     * @param fileUrl 文件 URL
     */
    @DeleteMapping
    public Result delete(@RequestParam("url") String fileUrl) {
        ossService.delete(fileUrl);
        return Result.ok();
    }
}

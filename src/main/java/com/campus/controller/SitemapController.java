package com.campus.controller;

import com.campus.entity.Post;
import com.campus.entity.Product;
import com.campus.service.IPostService;
import com.campus.service.IProductService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * SEO Sitemap 控制器：生成 sitemap.xml 供搜索引擎抓取
 */
@RestController
public class SitemapController {

    @Resource
    private IPostService postService;

    @Resource
    private IProductService productService;

    private static final String BASE_URL = "https://campus.example.com";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @GetMapping(value = "/sitemap.xml", produces = "application/xml;charset=UTF-8")
    public String sitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // 静态页面
        addUrl(xml, "/", "1.0", "daily");
        addUrl(xml, "/posts", "0.9", "hourly");
        addUrl(xml, "/market", "0.8", "daily");
        addUrl(xml, "/errand", "0.7", "daily");
        addUrl(xml, "/hot", "0.8", "daily");

        // 帖子详情页
        try {
            List<Post> posts = postService.list();
            for (Post post : posts) {
                String lastmod = post.getUpdateTime() != null ? post.getUpdateTime().format(FMT) : "";
                addUrl(xml, "/posts/" + post.getId(), "0.6", "weekly", lastmod);
            }
        } catch (Exception ignored) {}

        // 商品详情页
        try {
            List<Product> products = productService.list();
            for (Product product : products) {
                String lastmod = product.getUpdateTime() != null ? product.getUpdateTime().format(FMT) : "";
                addUrl(xml, "/market/" + product.getId(), "0.6", "weekly", lastmod);
            }
        } catch (Exception ignored) {}

        xml.append("</urlset>");
        return xml.toString();
    }

    private void addUrl(StringBuilder xml, String path, String priority, String changefreq) {
        addUrl(xml, path, priority, changefreq, null);
    }

    private void addUrl(StringBuilder xml, String path, String priority, String changefreq, String lastmod) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(BASE_URL).append(path).append("</loc>\n");
        if (lastmod != null && !lastmod.isEmpty()) {
            xml.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        }
        xml.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }
}

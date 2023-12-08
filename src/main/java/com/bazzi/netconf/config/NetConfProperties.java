package com.bazzi.netconf.config;

import com.bazzi.netconf.handler.WrappedNodeHelper;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.File;

@ConfigurationProperties(prefix = "netconf")
public class NetConfProperties {

    /**
     * yang文件本地存放目录
     */
    private String path;

    /**
     * 启动时是否加载并解析yang文件
     */
    private boolean loadOnStartup = false;

    @PostConstruct
    public void init() {
        if (isLoadOnStartup())
            WrappedNodeHelper.loadOnStartupFromBasePath();
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        Assert.isTrue(StringUtils.hasLength(path), "yang文件本地存放目录不能为空");
        String cleanPath = StringUtils.cleanPath(path);
        if (cleanPath.endsWith("/"))
            cleanPath = cleanPath.substring(0, path.length() - 1);
        File file = new File(cleanPath);
        Assert.isTrue(file.exists(), "yang文件本地存放目录不存在，请先创建目录");
        Assert.isTrue(file.isDirectory(), "yang文件本地存放目录不是目录，请检查");
        this.path = cleanPath;
        WrappedNodeHelper.setPath(getPath());
    }

    public boolean isLoadOnStartup() {
        return loadOnStartup;
    }

    public void setLoadOnStartup(boolean loadOnStartup) {
        this.loadOnStartup = loadOnStartup;
    }
}

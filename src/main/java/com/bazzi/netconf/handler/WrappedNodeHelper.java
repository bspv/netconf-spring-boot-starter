package com.bazzi.netconf.handler;

import com.bazzi.netconf.bean.WrappedNode;
import com.bazzi.netconf.bean.WrappedYangNode;
import com.bazzi.netconf.entity.SshConf;
import com.bazzi.netconf.util.NetConfUtil;
import com.bazzi.netconf.util.YangParser;
import com.cisco.stbarth.netconf.anc.NetconfException;
import com.cisco.stbarth.netconf.anc.NetconfSession;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.vaadin.data.TreeData;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.*;

public class WrappedNodeHelper {
    private static final Logger logger = LoggerFactory.getLogger(WrappedNodeHelper.class);
    private static final YangParser yangParser = new YangParser();

    private static final LoadingCache<String, WrappedNode> cache = Caffeine.newBuilder().maximumSize(128).recordStats().build(k -> load(k, null, true));

    private static String PATH;

    public static void setPath(String path) {
        WrappedNodeHelper.PATH = path;
    }

    public static WrappedNode findRootNodeByDevice(String device) {
        return cache.get(device);
    }

    public static WrappedNode findRootNodeByDevice(String device, SshConf sshConf) {
        return cache.get(device, k -> load(k, sshConf, false));
    }

    public static void loadOnStartupFromBasePath() {
        String basePath = PATH;
        File baseFile = new File(basePath);
        if (!baseFile.exists() || !baseFile.isDirectory())
            return;
        File[] files = baseFile.listFiles();
        if (files == null)
            return;
        for (File file : files) {
            //查询缓存来触发缓存加载
            findRootNodeByDevice(file.getName());
        }
    }

    public static void clear(String device) {
        cache.invalidate(device);
    }

    public static void update(String device) {
        clear(device);
        findRootNodeByDevice(device);
    }

    public static void update(String device, SshConf sshConf) {
        clear(device);
        findRootNodeByDevice(device, sshConf);
    }

    public static String state() {
        return cache.stats().toString();
    }

    public static WrappedNode load(String device, SshConf sshConf, boolean isLocal) {
        if (isLocal)
            return loadLocal(device);
        return loadRemote(device, sshConf);
    }

    private static WrappedNode loadLocal(String device) {
        try {
            String path = PATH + File.separatorChar + device;
            path = StringUtils.cleanPath(path);
            File file = new File(path);
            if (!file.exists() || !file.isDirectory())
                return null;
            yangParser.clear();
            yangParser.setCacheDirectory(path);
            yangParser.retrieveSchemas(path);
            yangParser.parse();
            SchemaContext schemaContext = yangParser.getSchemaContext();

            return getFromSchemaContext(schemaContext);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }


    private static WrappedNode loadRemote(String device, SshConf sshConf) {
        NetconfSession session = null;
        try {
            session = NetConfUtil.createSession(sshConf);
            String path = PATH + File.separatorChar + device;
            path = StringUtils.cleanPath(path);
            File file = new File(path);
            if (!file.exists() || !file.isDirectory()) {
                boolean mkdir = file.mkdir();
            }
            yangParser.clear();
            yangParser.setCacheDirectory(path);
            Map<String, String> availableSchemas = yangParser.getAvailableSchemas(session);
            yangParser.retrieveSchemas(session, availableSchemas);
            yangParser.parse();
            SchemaContext schemaContext = yangParser.getSchemaContext();

            return getFromSchemaContext(schemaContext);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (NetconfException.ProtocolException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    private static WrappedNode getFromSchemaContext(SchemaContext schemaContext) {
        if (schemaContext == null)
            return null;
        List<String> fieldQuery = Collections.singletonList("");
        TreeData<WrappedYangNode> data = new TreeData<>();
        for (Module module : schemaContext.getModules()) {
            new WrappedYangNode(module).addToTree(data, fieldQuery);
        }
        WrappedNode root = new WrappedNode();
        Map<WrappedNode, List<WrappedYangNode>> map = new HashMap<>(Map.of(root, data.getRootItems()));
        Queue<WrappedNode> q = new LinkedList<>(Collections.singleton(root));
        while (!q.isEmpty()) {
            int size = q.size();
            for (int i = 0; i < size; i++) {
                WrappedNode currentNode = q.poll();
                List<WrappedYangNode> wrappedYangNodes = map.getOrDefault(currentNode, new ArrayList<>());
                map.remove(currentNode);
                for (WrappedYangNode wrappedYangNode : wrappedYangNodes) {
                    WrappedNode wrappedNode = new WrappedNode(currentNode, wrappedYangNode);
                    assert currentNode != null;
                    currentNode.addChild(wrappedNode);
                    List<WrappedYangNode> children = data.getChildren(wrappedYangNode);
                    map.put(wrappedNode, children);
                    q.add(wrappedNode);
                }
            }
        }

        return root;
    }
}

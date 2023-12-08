package com.bazzi.netconf.service.impl;

import com.bazzi.netconf.bean.ErrorCode;
import com.bazzi.netconf.bean.WrappedNode;
import com.bazzi.netconf.entity.NodeData;
import com.bazzi.netconf.entity.Result;
import com.bazzi.netconf.entity.SshConf;
import com.bazzi.netconf.entity.XmlNodeData;
import com.bazzi.netconf.handler.WrappedNodeHelper;
import com.bazzi.netconf.service.NetConfService;
import com.bazzi.netconf.util.NetConfUtil;
import com.bazzi.netconf.util.StringUtil;
import com.bazzi.netconf.util.XmlUtil;
import com.cisco.stbarth.netconf.anc.NetconfException;
import com.cisco.stbarth.netconf.anc.NetconfSession;
import com.cisco.stbarth.netconf.anc.XMLElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class NetConfServiceImpl implements NetConfService {
    private static final Logger logger = LoggerFactory.getLogger(NetConfServiceImpl.class);

    @Override
    public Result<List<Map<String, String>>> search(String device, String name) {
        if (StringUtil.isEmpty(device) || StringUtil.isEmpty(name))
            return Result.failure(ErrorCode.FAIL, "请求参数有误，请检查！");
        WrappedNode root = WrappedNodeHelper.findRootNodeByDevice(device);
        if (root == null)
            return Result.failure(ErrorCode.FAIL, "没找到" + device + "对应的yang信息");
        List<Map<String, String>> dataList = new ArrayList<>();
        List<WrappedNode> wrappedNodes = root.searchByName(name);
        for (WrappedNode wrappedNode : wrappedNodes) {

            StringBuilder sb = new StringBuilder(wrappedNode.getName());
            while (!wrappedNode.getParent().isNullNode()) {
                wrappedNode = wrappedNode.getParent();
                sb.insert(0, wrappedNode.getName() + "/");
            }

            dataList.add(Map.of(sb.toString(), wrappedNode.getNamespace()));
        }
        return Result.success(dataList);
    }

    @Override
    public Result<NodeData> get(String device, String path, SshConf sshConf) {
        return get(device, path, null, null, sshConf);
    }

    @Override
    public Result<NodeData> get(String device, String path, String namespace, SshConf sshConf) {
        return get(device, path, namespace, null, sshConf);
    }

    @Override
    public Result<NodeData> get(String device, String path, Map<String, String> conditions, SshConf sshConf) {
        return get(device, path, null, conditions, sshConf);
    }

    @Override
    public Result<NodeData> get(String device, String path, String namespace, Map<String, String> conditions, SshConf sshConf) {
        Map<String, Map<String, String>> conditionsMap = new HashMap<>();
        conditionsMap.put(path, conditions);
        Result<Map<String, Result<NodeData>>> rs = baseBatchGet(device, Collections.singletonList(path), namespace, conditionsMap, sshConf);
        if (rs.isSuccess()) {
            Map<String, Result<NodeData>> data = rs.getData();
            return data.get(path);
        }
        return Result.failure(rs.getCode(), rs.getMessage());
    }

    @Override
    public Result<Map<String, Result<NodeData>>> batchGet(String device, List<String> pathList, SshConf sshConf) {
        return baseBatchGet(device, pathList, null, null, sshConf);
    }

    @Override
    public Result<Map<String, Result<NodeData>>> batchGet(String device, List<String> pathList, String namespace, SshConf sshConf) {
        return baseBatchGet(device, pathList, namespace, null, sshConf);
    }

    @Override
    public Result<Map<String, Result<NodeData>>> batchGet(String device, List<String> pathList, Map<String, Map<String, String>> conditionsMap, SshConf sshConf) {
        return baseBatchGet(device, pathList, null, conditionsMap, sshConf);
    }

    @Override
    public Result<Map<String, Result<NodeData>>> baseBatchGet(String device, List<String> pathList, String namespace, Map<String, Map<String, String>> conditionsMap, SshConf sshConf) {
        if (StringUtil.isEmpty(device) || pathList == null || pathList.isEmpty() || sshConf == null)
            return Result.failure(ErrorCode.FAIL, "请求参数有误，请检查！");
        WrappedNode rootNode = WrappedNodeHelper.findRootNodeByDevice(device);
        if (rootNode == null)
            return Result.failure(ErrorCode.FAIL, "没找到" + device + "对应的yang信息");

        NetconfSession session = NetConfUtil.createSession(sshConf);
        Map<String, Result<NodeData>> resultMap = new HashMap<>();
        for (String path : pathList) {
            List<WrappedNode> wrappedNodes = rootNode.searchFromRootByPath(path, namespace);
            if (wrappedNodes == null || wrappedNodes.isEmpty()) {
                resultMap.put(path, Result.failure(ErrorCode.FAIL, "没找到" + path + "对应的节点"));
                continue;
            }
            if (wrappedNodes.size() > 1) {
                resultMap.put(path, Result.failure(ErrorCode.FAIL, "找到多个" + path + "对应的节点"));
                continue;
            }

            try {
                WrappedNode wrappedNode = wrappedNodes.get(0);
                XMLElement getXMLElement = XmlUtil.get(wrappedNode);

                if (conditionsMap != null && conditionsMap.containsKey(path))
                    XmlUtil.fillCondition(getXMLElement, wrappedNode, conditionsMap.get(path));

                XMLElement reply = session.call(getXMLElement);
                resultMap.put(path, Result.success(new XmlNodeData(reply)));
            } catch (NetconfException e) {
                logger.error(e.getMessage(), e);
                resultMap.put(path, Result.failure(ErrorCode.FAIL, e.getMessage()));
            }
        }
        return Result.success(resultMap);
    }

    @Override
    public Result<String> merge(String device, String path, String val, SshConf sshConf) {
        return merge(device, path, val, null, null, sshConf);
    }

    @Override
    public Result<String> merge(String device, String path, String val, String namespace, SshConf sshConf) {
        return merge(device, path, val, namespace, null, sshConf);
    }

    @Override
    public Result<String> merge(String device, String path, String val, Map<String, String> conditions, SshConf sshConf) {
        return merge(device, path, val, null, conditions, sshConf);
    }

    @Override
    public Result<String> merge(String device, String path, String val, String namespace, Map<String, String> conditions, SshConf sshConf) {
        Map<String, Map<String, String>> conditionsMap = new HashMap<>();
        conditionsMap.put(path, conditions);
        Result<Map<String, Result<String>>> rs = baseBatchMerge(device, Map.of(path, val), namespace, conditionsMap, sshConf);
        if (rs.isSuccess()) {
            Map<String, Result<String>> data = rs.getData();
            return data.get(path);
        }
        return Result.failure(rs.getCode(), rs.getMessage());
    }

    @Override
    public Result<Map<String, Result<String>>> batchMerge(String device, Map<String, String> mergeMap, SshConf sshConf) {
        return baseBatchMerge(device, mergeMap, null, null, sshConf);
    }

    @Override
    public Result<Map<String, Result<String>>> batchMerge(String device, Map<String, String> mergeMap, String namespace, SshConf sshConf) {
        return baseBatchMerge(device, mergeMap, namespace, null, sshConf);
    }

    @Override
    public Result<Map<String, Result<String>>> batchMerge(String device, Map<String, String> mergeMap, Map<String, Map<String, String>> conditionsMap, SshConf sshConf) {
        return baseBatchMerge(device, mergeMap, null, conditionsMap, sshConf);
    }

    @Override
    public Result<Map<String, Result<String>>> baseBatchMerge(String device, Map<String, String> mergeMap, String namespace, Map<String, Map<String, String>> conditionsMap, SshConf sshConf) {
        if (StringUtil.isEmpty(device) || mergeMap == null || mergeMap.isEmpty() || sshConf == null)
            return Result.failure(ErrorCode.FAIL, "请求参数有误，请检查！");
        WrappedNode rootNode = WrappedNodeHelper.findRootNodeByDevice(device);
        if (rootNode == null)
            return Result.failure(ErrorCode.FAIL, "没找到" + device + "对应的yang信息");

        NetconfSession session = NetConfUtil.createSession(sshConf);
        Map<String, Result<String>> resultMap = new HashMap<>();
        for (String path : mergeMap.keySet()) {

            List<WrappedNode> wrappedNodes = rootNode.searchFromRootByPath(path, namespace);
            if (wrappedNodes == null || wrappedNodes.isEmpty()) {
                resultMap.put(path, Result.failure(ErrorCode.FAIL, "没找到" + path + "对应的节点"));
                continue;
            }
            if (wrappedNodes.size() > 1) {
                resultMap.put(path, Result.failure(ErrorCode.FAIL, "找到多个" + path + "对应的节点"));
                continue;
            }

            WrappedNode wrappedNode = wrappedNodes.get(0);
            XMLElement merge = XmlUtil.merge(wrappedNode);

            XmlUtil.fillValue(merge, wrappedNode, mergeMap.get(path));

            if (conditionsMap != null && conditionsMap.containsKey(path))
                XmlUtil.fillCondition(merge, wrappedNode, conditionsMap.get(path));

            try {
                XMLElement reply = session.call(merge);
                resultMap.put(path, Result.success("ok"));
            } catch (NetconfException e) {
                logger.error(e.getMessage(), e);
                resultMap.put(path, Result.failure(ErrorCode.FAIL, e.getMessage()));
            }
        }
        return Result.success(resultMap);
    }

    @Override
    public Result<String> delete(String device, String path, SshConf sshConf) {
        return delete(device, path, null, null, sshConf);
    }

    @Override
    public Result<String> delete(String device, String path, String namespace, SshConf sshConf) {
        return delete(device, path, namespace, null, sshConf);
    }

    @Override
    public Result<String> delete(String device, String path, Map<String, String> conditions, SshConf sshConf) {
        return delete(device, path, null, conditions, sshConf);
    }

    @Override
    public Result<String> delete(String device, String path, String namespace, Map<String, String> conditions, SshConf sshConf) {
        Map<String, Map<String, String>> conditionsMap = new HashMap<>();
        conditionsMap.put(path, conditions);
        Result<Map<String, Result<String>>> rs = baseBatchDelete(device, Collections.singletonList(path), namespace, conditionsMap, sshConf);
        if (rs.isSuccess()) {
            Map<String, Result<String>> data = rs.getData();
            return data.get(path);
        }
        return Result.failure(rs.getCode(), rs.getMessage());
    }

    @Override
    public Result<Map<String, Result<String>>> batchDelete(String device, List<String> pathList, SshConf sshConf) {
        return baseBatchDelete(device, pathList, null, null, sshConf);
    }

    @Override
    public Result<Map<String, Result<String>>> batchDelete(String device, List<String> pathList, String namespace, SshConf sshConf) {
        return baseBatchDelete(device, pathList, namespace, null, sshConf);
    }

    @Override
    public Result<Map<String, Result<String>>> batchDelete(String device, List<String> pathList, Map<String, Map<String, String>> conditionsMap, SshConf sshConf) {
        return baseBatchDelete(device, pathList, null, conditionsMap, sshConf);
    }

    @Override
    public Result<Map<String, Result<String>>> baseBatchDelete(String device, List<String> pathList, String namespace, Map<String, Map<String, String>> conditionsMap, SshConf sshConf) {
        if (StringUtil.isEmpty(device) || pathList == null || pathList.isEmpty() || sshConf == null)
            return Result.failure(ErrorCode.FAIL, "请求参数有误，请检查！");
        WrappedNode rootNode = WrappedNodeHelper.findRootNodeByDevice(device);
        if (rootNode == null)
            return Result.failure(ErrorCode.FAIL, "没找到" + device + "对应的yang信息");

        NetconfSession session = NetConfUtil.createSession(sshConf);
        Map<String, Result<String>> resultMap = new HashMap<>();
        for (String path : pathList) {

            List<WrappedNode> wrappedNodes = rootNode.searchFromRootByPath(path, namespace);
            if (wrappedNodes == null || wrappedNodes.isEmpty()) {
                resultMap.put(path, Result.failure(ErrorCode.FAIL, "没找到" + path + "对应的节点"));
                continue;
            }
            if (wrappedNodes.size() > 1) {
                resultMap.put(path, Result.failure(ErrorCode.FAIL, "找到多个" + path + "对应的节点"));
                continue;
            }

            WrappedNode wrappedNode = wrappedNodes.get(0);
            XMLElement delete = XmlUtil.delete(wrappedNode);

            if (conditionsMap != null && conditionsMap.containsKey(path))
                XmlUtil.fillCondition(delete, wrappedNode, conditionsMap.get(path));

            try {
                XMLElement reply = session.call(delete);
                resultMap.put(path, Result.success("ok"));
            } catch (NetconfException e) {
                logger.error(e.getMessage(), e);
                resultMap.put(path, Result.failure(ErrorCode.FAIL, e.getMessage()));
            }
        }
        return Result.success(resultMap);
    }
}

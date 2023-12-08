package com.bazzi.netconf.service;


import com.bazzi.netconf.entity.NodeData;
import com.bazzi.netconf.entity.Result;
import com.bazzi.netconf.entity.SshConf;

import java.util.List;
import java.util.Map;

public interface NetConfService {
    Result<List<Map<String, String>>> search(String device, String name);

    Result<NodeData> get(String device, String path, SshConf sshConf);

    Result<NodeData> get(String device, String path, String namespace, SshConf sshConf);

    Result<NodeData> get(String device, String path, Map<String, String> conditions, SshConf sshConf);

    Result<NodeData> get(String device, String path, String namespace, Map<String, String> conditions, SshConf sshConf);

    Result<Map<String, Result<NodeData>>> batchGet(String device, List<String> pathList, SshConf sshConf);

    Result<Map<String, Result<NodeData>>> batchGet(String device, List<String> pathList, String namespace, SshConf sshConf);

    Result<Map<String, Result<NodeData>>> batchGet(String device, List<String> pathList, Map<String, Map<String, String>> conditionsMap, SshConf sshConf);

    Result<Map<String, Result<NodeData>>> baseBatchGet(String device, List<String> pathList, String namespace, Map<String, Map<String, String>> conditionsMap, SshConf sshConf);

    Result<String> merge(String device, String path, String val, SshConf sshConf);

    Result<String> merge(String device, String path, String val, String namespace, SshConf sshConf);

    Result<String> merge(String device, String path, String val, Map<String, String> conditions, SshConf sshConf);

    Result<String> merge(String device, String path, String val, String namespace, Map<String, String> conditions, SshConf sshConf);

    Result<Map<String, Result<String>>> batchMerge(String device, Map<String, String> mergeMap, SshConf sshConf);

    Result<Map<String, Result<String>>> batchMerge(String device, Map<String, String> mergeMap, String namespace, SshConf sshConf);

    Result<Map<String, Result<String>>> batchMerge(String device, Map<String, String> mergeMap, Map<String, Map<String, String>> conditionsMap, SshConf sshConf);

    Result<Map<String, Result<String>>> baseBatchMerge(String device, Map<String, String> mergeMap, String namespace, Map<String, Map<String, String>> conditionsMap, SshConf sshConf);

    Result<String> delete(String device, String path, SshConf sshConf);

    Result<String> delete(String device, String path, String namespace, SshConf sshConf);

    Result<String> delete(String device, String path, Map<String, String> conditions, SshConf sshConf);

    Result<String> delete(String device, String path, String namespace, Map<String, String> conditions, SshConf sshConf);

    Result<Map<String, Result<String>>> batchDelete(String device, List<String> pathList, SshConf sshConf);

    Result<Map<String, Result<String>>> batchDelete(String device, List<String> pathList, String namespace, SshConf sshConf);

    Result<Map<String, Result<String>>> batchDelete(String device, List<String> pathList, Map<String, Map<String, String>> conditionsMap, SshConf sshConf);

    Result<Map<String, Result<String>>> baseBatchDelete(String device, List<String> pathList, String namespace, Map<String, Map<String, String>> conditionsMap, SshConf sshConf);

}

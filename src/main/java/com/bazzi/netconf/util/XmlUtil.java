package com.bazzi.netconf.util;

import com.bazzi.netconf.bean.WrappedNode;
import com.cisco.stbarth.netconf.anc.Netconf;
import com.cisco.stbarth.netconf.anc.XMLElement;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.CaseSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class XmlUtil {
    public static XMLElement get(WrappedNode node) {
        XMLElement root = new XMLElement(Netconf.NS_NETCONF, "get");
        XMLElement getE = node.createNetconfTemplate(null, null);
        root.createChild("filter").withChild(getE);
        return root;
    }

    public static XMLElement batchGet(List<WrappedNode> wrappedNodeList) {
        XMLElement root = new XMLElement(Netconf.NS_NETCONF, "get");
        XMLElement filterNode = new XMLElement(Netconf.NS_NETCONF, "filter");

        Map<WrappedNode, XMLElement> cacheMap = new HashMap<>();
        for (WrappedNode wrappedNode : wrappedNodeList) {
            filterNode.withChild(createNetconfTemplate(null, wrappedNode, cacheMap));
        }

        root.withChild(filterNode);
        return root;
    }

    public static XMLElement merge(WrappedNode node) {
        XMLElement root = new XMLElement(Netconf.NS_NETCONF, "edit-config");
        root.createChild("target").withChild("running");
        XMLElement mergeE = node.createNetconfTemplate("", null);
        root.createChild("config").withChild(mergeE);
        return root;
    }

    public static XMLElement delete(WrappedNode node) {
        XMLElement root = new XMLElement(Netconf.NS_NETCONF, "edit-config");
        root.createChild("target").withChild("running");
        root.withTextChild("default-operation", "none");
        XMLElement deleteE = node.createNetconfTemplate("delete", null);
        root.createChild("config").withChild(deleteE);
        return root;
    }

    public static void fillValue(XMLElement xmlElement, WrappedNode node, String value) {
        if (xmlElement == null || node == null || value == null || value.isEmpty())
            return;
        String name = node.getName();
        Element element = xmlElement.getElement();
        NodeList nodeList = element.getElementsByTagName(name);
        Node item = nodeList.item(0);
        item.setTextContent(value);
    }

    public static void fillCondition(XMLElement xmlElement, WrappedNode node, Map<String, String> paramMap) {
        if (xmlElement == null || node == null || paramMap == null || paramMap.isEmpty())
            return;
        Element element = xmlElement.getElement();
        WrappedNode parent = node.getParent();
        if (parent != null && parent.getDataSchemaNode() instanceof ListSchemaNode) {
            ListSchemaNode listNode = (ListSchemaNode) parent.getDataSchemaNode();
            for (QName key : listNode.getKeyDefinition()) {
                String keyName = key.getLocalName();
                if (paramMap.containsKey(keyName)) {
                    NodeList nodeList = element.getElementsByTagName(keyName);
                    Node item = nodeList.item(0);
                    item.setTextContent(paramMap.get(keyName));
                }
            }
        }
    }

    private static XMLElement createNetconfTemplate(XMLElement data, WrappedNode curNode, Map<WrappedNode, XMLElement> cacheMap) {
        XMLElement element = new XMLElement(curNode.getNamespace(), curNode.getName());

        // If we have associated data from the peer populate it in the XML template
        for (WrappedNode node = curNode.getParent(); node != null && node.getDataSchemaNode() != null; node = node.getParent()) {
            if (!(node.getDataSchemaNode() instanceof CaseSchemaNode) && !(node.getDataSchemaNode() instanceof ChoiceSchemaNode)) {
                if (data != null)
                    data = data.getParent();

                XMLElement parentXmlElement = cacheMap.getOrDefault(node, new XMLElement(node.getNamespace(), node.getName()));
                element = parentXmlElement.withChild(element);
                if (node.getDataSchemaNode() instanceof ListSchemaNode) {
                    ListSchemaNode listNode = (ListSchemaNode) node.getDataSchemaNode();
                    for (QName key : listNode.getKeyDefinition()) {
                        String keyName = key.getLocalName();
                        String keyNS = key.getNamespace().toString();

                        Optional<XMLElement> first = element.getFirst(keyNS, keyName);
                        if (first.isPresent())
                            continue;

                        Optional<XMLElement> keyV = Optional.ofNullable(data).flatMap(d -> d.getFirst(keyNS, keyName));
                        if (keyV.isPresent())
                            element.withChild(keyV.get().clone());
                        else
                            element.withChild(keyNS, keyName);
                    }
                }
                cacheMap.putIfAbsent(node, element);
            }
        }

        return curNode.getModule() == null ? element : null;
    }
}

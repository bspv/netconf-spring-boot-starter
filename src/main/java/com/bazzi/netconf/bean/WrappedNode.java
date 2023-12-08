package com.bazzi.netconf.bean;

import com.cisco.stbarth.netconf.anc.Netconf;
import com.cisco.stbarth.netconf.anc.XMLElement;
import com.visionvera.netconf.util.StringUtil;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.*;
import org.opendaylight.yangtools.yang.model.api.Module;

import java.util.*;
import java.util.stream.Collectors;

public class WrappedNode {
    WrappedNode parent;
    Set<WrappedNode> child;

    private DataSchemaNode dataSchemaNode;
    private Module module;

    private String namespace;
    private String name;
    private String prefix;
    private String description;

    public WrappedNode() {
    }

    public WrappedNode(WrappedNode parent, DataSchemaNode node) {
        this.parent = parent;
        this.dataSchemaNode = node;
        this.namespace = node.getQName().getNamespace().toString();
        this.name = node.getQName().getLocalName();
        this.description = node.getDescription().orElse("");
    }

    public WrappedNode(WrappedNode parent, WrappedYangNode wrappedYangNode) {
        this.parent = parent;
        this.module = wrappedYangNode.getModule();
        DataSchemaNode node = wrappedYangNode.getNode();
        this.dataSchemaNode = node;
        this.namespace = node.getQName().getNamespace().toString();
        this.name = node.getQName().getLocalName();
        this.description = node.getDescription().orElse("");
    }

    public void addChild(WrappedNode wrappedNode) {
        if (child == null)
            child = new HashSet<>();
        child.add(wrappedNode);
    }

    public List<WrappedNode> searchByName(String name) {
        List<WrappedNode> resList = new ArrayList<>();
        if (StringUtil.equals(name, getName()))
            resList.add(this);
        Queue<WrappedNode> q = new LinkedList<>(child);
        while (!q.isEmpty()) {
            int size = q.size();
            for (int i = 0; i < size; i++) {
                WrappedNode cur = q.poll();
                if (StringUtil.equals(cur.getName(), name))
                    resList.add(cur);

                Set<WrappedNode> children = cur.getChild();
                if (children != null && !children.isEmpty())
                    q.addAll(children);
            }
        }
        return resList;
    }

    public WrappedNode searchByNameAndNS(String name, String namespace) {
        if (StringUtil.equals(name, getName()) && StringUtil.equals(getNamespace(), namespace))
            return this;
        Queue<WrappedNode> q = new LinkedList<>(child);
        while (!q.isEmpty()) {
            int size = q.size();
            for (int i = 0; i < size; i++) {
                WrappedNode cur = q.poll();
                if (StringUtil.equals(cur.getName(), name) && StringUtil.equals(cur.getNamespace(), namespace))
                    return cur;

                if (StringUtil.equals(cur.getNamespace(), namespace)) {
                    Set<WrappedNode> children = cur.getChild();
                    if (children != null && !children.isEmpty())
                        q.addAll(children);
                }
            }
        }
        return null;
    }

    public List<WrappedNode> searchFromRootByPath(String path) {
        List<WrappedNode> result = new ArrayList<>();
        if (path == null || path.isEmpty())
            return result;
        path = path.startsWith("/") ? path.substring(1) : path;
        String[] pathArr = path.split("/");

        Queue<WrappedNode> q = new LinkedList<>(child);
        for (int i = 0; i < pathArr.length; i++) {
            String curName = pathArr[i];
            if (q.isEmpty())
                return result;
            boolean find = false;
            int size = q.size();
            for (int j = 0; j < size; j++) {
                WrappedNode cur = q.poll();
                if (cur != null && StringUtil.equals(cur.getName(), curName)) {
                    find = true;
                    if (i == pathArr.length - 1)
                        result.add(cur);
                    Set<WrappedNode> curChild = cur.getChild();
                    if (curChild != null)
                        q.addAll(curChild);
                }
            }
            if (!find)
                return result;
        }

        return result;
    }

    public List<WrappedNode> searchFromRootByPath(String path, String namespace) {
        List<WrappedNode> wrappedNodes = searchFromRootByPath(path);
        if (namespace == null || namespace.isEmpty())
            return wrappedNodes;
        return wrappedNodes.stream().filter(e -> namespace.equals(e.getNamespace())).collect(Collectors.toList());
    }

    public String getType() {
        if (dataSchemaNode instanceof AnyxmlSchemaNode)
            return "anyxml";
        else if (dataSchemaNode instanceof CaseSchemaNode)
            return "case";
        else if (dataSchemaNode instanceof ChoiceSchemaNode)
            return "choice";
        else if (dataSchemaNode instanceof ContainerSchemaNode)
            return "container";
        else if (dataSchemaNode instanceof LeafSchemaNode)
            return "leaf";
        else if (dataSchemaNode instanceof LeafListSchemaNode)
            return "leaf-list";
        else if (dataSchemaNode instanceof ListSchemaNode)
            return "list";
        else
            return "module";
    }

    public boolean isNullNode() {
        return (name == null || name.isEmpty()) && (namespace == null || namespace.isEmpty());
    }

    public XMLElement createNetconfTemplate(String operation, XMLElement data) {
        XMLElement element = new XMLElement(namespace, name);

        if (operation != null && operation.isEmpty()) {
            if (data != null)
                element = data;

            // Remove any meta-attributes we may have added elsewhere
            element.withAttribute("expand", null);
            element.withAttribute("root", null);
            addChildren(element, dataSchemaNode);
        } else if (operation != null || data != null) {
            if (operation != null && !operation.isEmpty())
                element.withAttribute(Netconf.NS_NETCONF, "operation", operation);

            if (dataSchemaNode instanceof ListSchemaNode) {
                // For lists, we need to include key leafs
                for (QName key : ((ListSchemaNode) dataSchemaNode).getKeyDefinition()) {
                    String keyNS = key.getNamespace().toString();
                    String keyName = key.getLocalName();
                    Optional<XMLElement> keyE = Optional.ofNullable(data).flatMap(x -> x.getFirst(keyNS, keyName));
                    if (keyE.isPresent())
                        element.withChild(keyE.get().clone());
                    else
                        element.createChild(keyNS, keyName);
                }
            }
        }

        // If we have associated data from the peer populate it in the XML template
        for (WrappedNode node = this.parent; node != null && node.dataSchemaNode != null; node = node.parent) {
            if (!(node.dataSchemaNode instanceof CaseSchemaNode) && !(node.dataSchemaNode instanceof ChoiceSchemaNode)) {
                if (data != null)
                    data = data.getParent();

                element = new XMLElement(node.namespace, node.name).withChild(element);
                if (node.dataSchemaNode instanceof ListSchemaNode) {
                    ListSchemaNode listNode = (ListSchemaNode) node.dataSchemaNode;
                    for (QName key : listNode.getKeyDefinition()) {
                        String keyName = key.getLocalName();
                        String keyNS = key.getNamespace().toString();

                        Optional<XMLElement> keyV = Optional.ofNullable(data).flatMap(d -> d.getFirst(keyNS, keyName));
                        if (keyV.isPresent())
                            element.withChild(keyV.get().clone());
                        else
                            element.withChild(keyNS, keyName);
                    }
                }
            }
        }

//        return Optional.ofNullable(module == null ? element : null);
        return module == null ? element : null;
    }

    private void addChildren(XMLElement element, DataSchemaNode node) {
        if (node instanceof DataNodeContainer) {
            DataNodeContainer container = (DataNodeContainer) node;
            for (DataSchemaNode child : container.getChildNodes()) {
                String childNS = child.getQName().getNamespace().toString();
                String childName = child.getQName().getLocalName();
                Optional<XMLElement> dataChildElement = element.getFirst(childNS, childName);
                XMLElement childElement = dataChildElement.isPresent() ?
                        dataChildElement.get() : element.createChild(childNS, childName);

                childElement.withAttribute("expand", null);
                childElement.withAttribute("root", null);

                addChildren(childElement, child);

                if (!dataChildElement.isPresent()) {
                    if (child instanceof LeafSchemaNode || child instanceof LeafListSchemaNode)
                        childElement.withText(child.getDescription().orElse(""));

                    element.withComment(childElement.toString().replaceAll("\\s+$", ""));
                    childElement.remove();
                }
            }
        }
    }

    public WrappedNode getParent() {
        return parent;
    }

    public void setParent(WrappedNode parent) {
        this.parent = parent;
    }

    public Set<WrappedNode> getChild() {
        return child;
    }

    public void setChild(Set<WrappedNode> child) {
        this.child = child;
    }

    public DataSchemaNode getDataSchemaNode() {
        return dataSchemaNode;
    }

    public void setDataSchemaNode(DataSchemaNode dataSchemaNode) {
        this.dataSchemaNode = dataSchemaNode;
    }

    public Module getModule() {
        return module;
    }

    public void setModule(Module module) {
        this.module = module;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WrappedNode that = (WrappedNode) o;
        return Objects.equals(namespace, that.namespace) && Objects.equals(name, that.name) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, name, description);
    }

    @Override
    public String toString() {
        return "WrappedNode{" +
                "name='" + name + '\'' +
                ", namespace='" + namespace + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}

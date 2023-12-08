package com.bazzi.netconf.entity;


import com.cisco.stbarth.netconf.anc.XMLElement;
import org.springframework.beans.BeanUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


public class XmlNodeData implements NodeData {
    private Element element;

    private XmlNodeData() {
    }

    public XmlNodeData(XMLElement xmlElement) {
        this.element = xmlElement.getElement();
    }

    public String getSingleValue(String name) {
        if (element == null || name == null || name.isEmpty())
            return null;
        NodeList nodeList = element.getElementsByTagName(name);
        int length = nodeList.getLength();
        if (length == 0)
            return null;
        return nodeList.item(0).getTextContent();
    }

    public <T> T getWithType(Class<T> clazz) {
        try {
            T t = clazz.getDeclaredConstructor().newInstance();
            Field[] declaredFields = clazz.getDeclaredFields();
            int c = 0;
            for (Field f : declaredFields) {
                String singleValue = getSingleValue(f.getName());
                if (singleValue != null && !singleValue.isEmpty()
                        && setField(t, f.getName(), singleValue)) {
                    c++;
                }
            }
            return c > 0 ? t : null;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> List<T> getListWithType(Class<T> clazz, String name) {
        try {
            List<T> list = new ArrayList<>();

            NodeList nodeList = element.getElementsByTagName(name);
            int length = nodeList.getLength();
            if (length == 0)
                return null;
            NodeList nodes = nodeList.item(0).getChildNodes();
            int len = nodes.getLength();
            for (int i = 0; i < len; i++) {
                T t = clazz.getDeclaredConstructor().newInstance();
                NodeList childNodes = nodes.item(i).getChildNodes();
                for (int j = 0; j < childNodes.getLength(); j++) {
                    Node item = childNodes.item(j);
                    String localName = item.getLocalName();
                    String value = item.getTextContent();
                    setField(t, localName, value);
                }
                list.add(t);
            }
            return list;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> boolean setField(T t, String fieldName, Object value) {
        if (t == null || fieldName == null || fieldName.isEmpty())
            return false;
        try {
            String setMethodName = "set" + fieldName.substring(0, 1).toUpperCase()
                    + fieldName.substring(1);

            Class<?> propertyType = BeanUtils.findPropertyType(fieldName, t.getClass());
            Method setMethod = BeanUtils.findDeclaredMethod(t.getClass(), setMethodName, propertyType);
            if (setMethod != null)
                setMethod.invoke(t, value);

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

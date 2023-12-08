package com.bazzi.netconf.entity;

import java.util.List;

public interface NodeData {
    String getSingleValue(String name);

    <T> T getWithType(Class<T> clazz);

    <T> List<T> getListWithType(Class<T> clazz, String name);
}

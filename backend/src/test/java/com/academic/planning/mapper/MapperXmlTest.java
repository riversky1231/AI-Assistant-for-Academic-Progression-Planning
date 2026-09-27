package com.academic.planning.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MapperXmlTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "mapper/SchoolMapper.xml",
            "mapper/UserMapper.xml",
            "mapper/PermissionMapper.xml",
            "mapper/RecommendationMapper.xml"
    })
    void mapperXmlShouldParse(String resource) {
        InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        assertNotNull(input, resource + " should exist");
        Configuration configuration = new Configuration();
        new XMLMapperBuilder(input, configuration, resource, new HashMap<>()).parse();
    }
}

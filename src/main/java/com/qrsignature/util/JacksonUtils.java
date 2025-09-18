package com.qrsignature.util;


import cn.hutool.core.bean.BeanUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;

/**
 * Jackson 工具类，封装了 jackson 的常用操作，使之使用更加简单
 *
 * @author waitmoon
 */
@Slf4j
public final class JacksonUtils {
    //filter ice beans&ignores
    public final static ObjectMapper mapper = new ObjectMapper();

    private final static ObjectMapper withNullMapper = new ObjectMapper();

    private static void init(ObjectMapper objectMapper) {
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        objectMapper.registerModule(javaTimeModule);
        objectMapper.setLocale(Locale.CHINA);
        objectMapper.findAndRegisterModules();
        //忽略大小写，解决ORACLE返回字段为大写。
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        //去掉默认的时间戳格式
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        //设置为中国上海时区
        objectMapper.setTimeZone(TimeZone.getTimeZone(ZoneId.systemDefault()));
        //序列化时，日期的统一格式
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA));
        //			//序列化处理
        objectMapper.findAndRegisterModules();
        //失败处理
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //单引号处理
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        //反序列化时，属性不存在的兼容处理s
        objectMapper.getDeserializationConfig().withoutFeatures(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        //日期格式化
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    static {
        init(mapper);
        init(withNullMapper);
    }

    public static String toJsonString(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn(e.getMessage(), e);
        }
        return null;
    }

    public static String toJson(JsonNode node) {
        if (node == null) {
            return "";
        }
        try {
            return mapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            log.warn(e.getMessage(), e);
        }
        return "";
    }

    public static byte[] toJsonBytes(Object obj) {
        try {
            return mapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            log.warn(e.getMessage(), e);
        }
        return null;
    }

    public static Object toMap(JsonNode jsonNode) {
        Map<String, Object> map = new HashMap<>();
        if (jsonNode == null || jsonNode.size() <= 0) {
            return map;
        }
        if (jsonNode instanceof ArrayNode) {
            return mapper.convertValue(jsonNode, List.class);
        } else {
            return mapper.convertValue(jsonNode, HashMap.class);
        }
    }

    public static <T> T convert(JsonNode node, Class<T> clazz) {
        if (node == null) {
            return null;
        }
        try {
            return mapper.convertValue(node, clazz);
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
        return null;
    }

    public static <T> List<T> convertList(JsonNode node, Class<T> clazz) {
        if (node == null) {
            return null;
        }
        try {
            List<Map<String, Object>> list = mapper.convertValue(node,
                    new TypeReference<List<Map<String, Object>>>() {
                    });
            List<T> result = new ArrayList<>();
            for (var item : list) {
                T instance = BeanUtil.toBean(item, clazz);
                result.add(instance);
            }
            return result;
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
        return null;
    }

    public static <T> T readJson(String json, Class<T> clazz) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return mapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.warn(e.getMessage(), e);
//            throw new RuntimeException(e);
        }
        return null;
    }

    // 新增的方法
    public static <T> T convertValue(Object fromValue, Class<T> toValueType) {
        if (fromValue == null) {
            return null;
        }
        try {
            return mapper.convertValue(fromValue, toValueType);
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
        return null;
    }

    public static <T> List<T> readList(String json, Class<T> clazz) {
        List<T> list = new ArrayList<>();
        if (!StringUtils.hasText(json)) {
            return list;
        }

        try {
            List<T> result = new ArrayList<>();
            List<Map<String, Object>> maps = mapper.readValue(json,
                    new TypeReference<List<Map<String, Object>>>() {
                    });
            for (var item : maps) {
                T instance = BeanUtil.toBean(item, clazz);
                result.add(instance);
            }
            return result;
        } catch (JsonProcessingException e) {
            log.warn(e.getMessage(), e);
//            throw new RuntimeException(e);
        }
        return list;
    }

    public static <T> T readJson(String json, TypeReference<T> ref) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return mapper.readValue(json, ref);
        } catch (JsonProcessingException e) {
            log.warn(e.getMessage(), e);
        }
        return null;
    }

    public static <T> T readJsonBytes(byte[] jsonBytes, Class<T> clazz) throws IOException {
        return mapper.readValue(jsonBytes, clazz);
    }

    public static JsonNode readTree(String json) {
        if (json == null || json.equals("{}")) {
            return null;
        }
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return null;
        }
    }

    public static boolean isJson(String json) {
        try {
            mapper.readTree(json);
            return true;
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return false;
        }
    }

    public static boolean isJsonObject(String json) {
        try {
            JsonNode node = mapper.readTree(json);
            return node.isObject();
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return false;
        }
    }

    public static boolean isJsonArray(String json) {
        try {
            JsonNode node = mapper.readTree(json);
            return node.isArray();
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return false;
        }
    }
}

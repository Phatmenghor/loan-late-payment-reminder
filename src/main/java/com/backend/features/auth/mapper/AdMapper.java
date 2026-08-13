package com.backend.features.auth.mapper;

import com.backend.features.auth.dto.response.AdLogResponse;
import com.backend.features.auth.dto.response.AdUserDto;
import com.backend.features.auth.model.AdLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface AdMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "traceId", source = "traceId")
    @Mapping(target = "clientIp", source = "clientIp")
    @Mapping(target = "apiKey", source = "apiKey")
    AdLogResponse toAdLogResponse(AdLog adLog);

    @Mapping(target = "samaccountName",    expression = "java(str(attrs, \"sAMAccountName\"))")
    @Mapping(target = "displayName",      expression = "java(str(attrs, \"displayName\"))")
    @Mapping(target = "cn",               expression = "java(str(attrs, \"cn\"))")
    @Mapping(target = "givenName",        expression = "java(str(attrs, \"givenName\"))")
    @Mapping(target = "sn",               expression = "java(str(attrs, \"sn\"))")
    @Mapping(target = "mail",             expression = "java(str(attrs, \"mail\"))")
    @Mapping(target = "department",       expression = "java(str(attrs, \"department\"))")
    @Mapping(target = "title",            expression = "java(str(attrs, \"title\"))")
    @Mapping(target = "telephoneNumber",  expression = "java(str(attrs, \"telephoneNumber\"))")
    @Mapping(target = "mobile",           expression = "java(str(attrs, \"mobile\"))")
    @Mapping(target = "company",          expression = "java(str(attrs, \"company\"))")
    @Mapping(target = "distinguishedName",expression = "java(str(attrs, \"distinguishedName\"))")
    @Mapping(target = "lastLoginAt",      ignore = true)
    AdUserDto toAdUserDto(Map<String, Object> attrs);

    default String str(Map<String, Object> attrs, String key) {
        Object val = attrs.get(key);
        return val != null ? val.toString() : null;
    }

    @SuppressWarnings("unchecked")
    default List<String> list(Map<String, Object> attrs, String key) {
        Object val = attrs.get(key);
        if (val == null) return null;
        if (val instanceof List) {
            return ((List<Object>) val).stream().map(Object::toString).collect(Collectors.toList());
        }
        return List.of(val.toString());
    }
}

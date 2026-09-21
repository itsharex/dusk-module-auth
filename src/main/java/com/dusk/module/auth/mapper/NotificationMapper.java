package com.dusk.module.auth.mapper;

import com.dusk.common.rpc.auth.dto.notification.CreateNotificationInput;
import com.dusk.module.auth.dto.notification.NotificationOutput;
import com.dusk.module.auth.entity.Notification;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * @author : kefuming
 * @date : 2025/10/19 19:51
 */
@Mapper
public interface NotificationMapper {
    NotificationMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(NotificationMapper.class);

    @Mapping(source = "pageNavigation", target = "pageNavigation", qualifiedByName = "objectToJson")
    Notification createDtoToEntity(CreateNotificationInput dto);

    NotificationOutput toOutDto(Notification entity);

    @Named("objectToJson")
    default String objectToJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new ObjectMapper().writeValueAsString(value);
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to serialize pageNavigation", e);
        }
    }
}

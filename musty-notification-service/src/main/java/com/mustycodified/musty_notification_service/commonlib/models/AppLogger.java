package com.mustycodified.musty_notification_service.commonlib.models;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppLogger implements Serializable {
    @Serial
    private static final long serialVersionUID = -5168759314415854175L;

    private transient Object headerData;

    private PrimaryData queryData;

    private transient Object payloadData;

}

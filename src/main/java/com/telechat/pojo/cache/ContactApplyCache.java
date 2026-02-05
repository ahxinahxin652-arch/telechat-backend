package com.telechat.pojo.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContactApplyCache {
    private Long contactApplyId;
    private Long userId;
    private String status;
    private LocalDateTime createdTime;
}

/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/12/21 下午9:04
 */
package com.telechat.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor

@Builder
public class ContactApplyVO {
    private Long id;
    private Long userId;
    private String avatar;
    private String nickname;
    private String status;
    private LocalDateTime createTime;
}

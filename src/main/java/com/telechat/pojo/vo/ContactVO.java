/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/12/21 下午11:22
 */
package com.telechat.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

@Builder
public class ContactVO {
    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String remark;
}

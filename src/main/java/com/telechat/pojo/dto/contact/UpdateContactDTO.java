/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/12/22 下午12:04
 */
package com.telechat.pojo.dto.contact;

import lombok.Data;

@Data
public class UpdateContactDTO {
    private Long contactId;
    private String remark;
}

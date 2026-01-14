/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/12/30 下午9:26
 */
package com.telechat.pojo.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContactsCache {
    private Long contactId;
    private Long friendId;
    private String remark;
}

/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/12/21 下午11:12
 */
package com.telechat.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

@TableName("contact")
public class Contact {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long friendId;
    private Long conversationId;
    private String remark;
    private LocalDateTime createdTime;
}

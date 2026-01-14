/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/8/21 下午5:24
 */
package com.telechat.pojo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserInfoDTO {
    // TODO 校验annotation
    @Size(min = 1, max = 60, message = "昵称长度应在1-20个字以内")
    private String nickname;

    @NotNull(message = "性别不能为空")
    private Byte gender;

    @Size(max = 255, message = "个人简介长度不能超过255个字符")
    private String bio;
}

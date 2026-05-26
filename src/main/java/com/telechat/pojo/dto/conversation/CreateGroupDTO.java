package com.telechat.pojo.dto.conversation;

import lombok.Data;

import java.util.List;

@Data
public class CreateGroupDTO {
    private List<Long> memberIds;
}
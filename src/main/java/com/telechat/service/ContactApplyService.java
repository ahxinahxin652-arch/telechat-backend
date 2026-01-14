package com.telechat.service;

import com.telechat.pojo.dto.contact.ContactApplyHandleDTO;
import com.telechat.pojo.vo.ContactApplyVO;

import java.util.List;

public interface ContactApplyService {
    /**
    * 添加联系人
     * @param userId 用户ID
     * @param contactUserName 用户名
     *
     * @return boolean
    * */
    boolean addContactApply(Long userId, String contactUserName);

    /**
    * 获取联系人申请列表【未处理】
     * @param userId 用户ID
     *
     * @return List<ContactApplyVO>
    * */
    List<ContactApplyVO> applyList(Long userId);

    /**
    * 处理联系人申请
     * @param userId 用户ID
     * @param contactApplyHandleDTO 处理联系人申请DTO
     *
     * @return boolean
    * */
    boolean handleApply(Long userId, ContactApplyHandleDTO contactApplyHandleDTO);
}

package com.telechat.service;

import com.telechat.pojo.dto.contact.UpdateContactDTO;
import com.telechat.pojo.vo.ContactVO;

import java.util.List;

public interface ContactService {
    /**
     * 获取联系人列表
     *
     * @param userId 用户ID
     * @return List<ContactVO>
     */
    List<ContactVO> list(Long userId);

    /**
     * 删除联系人
     *
     * @param id     联系人ID
     * @param userId 用户ID
     * @return boolean
     */
    boolean delete(Long id, Long userId);

    /**
     * 修改联系人
     *
     * @param updateContactDTO 修改联系人DTO
     * @param userId 用户id
     *
     * @return boolean
     */
    boolean update(UpdateContactDTO updateContactDTO, Long userId);
}

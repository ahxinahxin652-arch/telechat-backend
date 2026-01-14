/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/12/21 下午8:49
 */
package com.telechat.service.impl;

import com.telechat.constant.ExceptionConstant;
import com.telechat.exception.exceptions.ContactException;
import com.telechat.mapper.dao.ContactApplyDao;
import com.telechat.mapper.dao.ContactDao;
import com.telechat.mapper.dao.ConversationDao;
import com.telechat.mapper.dao.ConversationMemberDao;
import com.telechat.pojo.dto.contact.ContactApplyHandleDTO;
import com.telechat.pojo.entity.Contact;
import com.telechat.pojo.entity.ContactApply;
import com.telechat.pojo.entity.Conversation;
import com.telechat.pojo.entity.ConversationMember;
import com.telechat.pojo.enums.ConversationType;
import com.telechat.pojo.vo.ContactApplyVO;
import com.telechat.service.ContactApplyService;
import com.telechat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContactApplyServiceImpl implements ContactApplyService {

    @Autowired
    private UserService userService;

    @Autowired
    private ContactDao contactDao;

    @Autowired
    private ContactApplyDao contactApplyDao;

    @Autowired
    private ConversationDao conversationDao;

    @Autowired
    private ConversationMemberDao conversationMemberDao;

    /**
     * 添加联系申请
     *
     * @param userId    用户ID
     * @param contactUserName 用户名
     */
    @Transactional
    @Override
    public boolean addContactApply(Long userId, String contactUserName) {
        // 添加联系人
        Long contactId = userService.getUserIdByUsername(contactUserName);
        if(contactId == null){
            throw new ContactException(ExceptionConstant.NOT_EXIST_CODE,ExceptionConstant.USER_NOT_EXIST_MSG);
        }

        // 校验是否已添加联系人
        Contact contact = contactDao.selectByUserIdAndFriendId(userId, contactId);
        if(contact != null){
            throw new ContactException(ExceptionConstant.ALREADY_EXIST_CODE,ExceptionConstant.CONTACT_ALREADY_EXIST_MSG);
        }

        // 校验之前是否存在该联系申请
        ContactApply contactApply = contactApplyDao.selectByUserIdAndFriendId(userId, contactId);
        if(contactApply != null){
            // 删除之前的联系申请
            contactApplyDao.deleteById(contactApply.getId());
        }

        // 添加联系人
        ContactApply contactApplyTmp = ContactApply.builder()
                .userId(userId)
                .friendId(contactId)
                .status("PENDING")
                .build();

        contactApplyDao.insert(contactApplyTmp);
        return true;
    }

    /**
     * 获取联系人申请列表【未处理】
     *
     * @param userId 用户ID
     * @return List<ContactApplyVO>
     */
    @Override
    public List<ContactApplyVO> applyList(Long userId) {
        List<ContactApply> contactApplies = contactApplyDao.selectApplyList(userId, "PENDING");
        return contactApplies.stream()
                .map(contactApply -> ContactApplyVO.builder()
                        .id(contactApply.getId())
                        .userId(contactApply.getUserId())
                        .avatar(userService.getUserById(contactApply.getUserId()).getAvatar())
                        .nickname(userService.getUserById(contactApply.getUserId()).getNickname())
                        .status(contactApply.getStatus())
                        .createTime(contactApply.getCreatedTime())
                        .build())
                .toList();
    }

    /**
     * 处理联系人申请
     *
     * @param userId                用户ID
     * @param contactApplyHandleDTO 处理联系人申请DTO
     * @return boolean
     */
    @Transactional
    @Override
    public boolean handleApply(Long userId, ContactApplyHandleDTO contactApplyHandleDTO) {
        Long contactApplyId = contactApplyHandleDTO.getContactId();
        ContactApply contactApply = contactApplyDao.selectById(contactApplyId);
        // 校验联系是否存在
        if(contactApplyId == null){
            throw new ContactException(ExceptionConstant.NOT_EXIST_CODE,ExceptionConstant.CONTACT_NOT_EXIST_MSG);
        }
        // 校验联系的friendId是否属于当前用户
        if(!contactApply.getFriendId().equals(userId)){
            throw new ContactException(ExceptionConstant.NOT_ALLOWED_CODE,ExceptionConstant.CONTACT_NOT_ALLOWED_MSG);
        }
        // 校验联系是否已处理
        if(!contactApply.getStatus().equals("PENDING")){
            throw new ContactException(ExceptionConstant.NOT_ALLOWED_CODE,ExceptionConstant.CONTACT_ALREADY_HANDLE_EXCEPTION_MSG);
        }
        if(contactApplyHandleDTO.isAgree()){ // 同意
            // 更新联系状态
            contactApply.setStatus("ACCEPTED");
            contactApplyDao.updateById(contactApply);

            LocalDateTime now = LocalDateTime.now();

            // 添加私聊会话
            Conversation conversation = Conversation.builder()
                    .type(ConversationType.getName(1))
                    .status(true)
                    .createdTime(now)
                    .updatedTime(now)
                    .build();

            conversationDao.insert(conversation);

            // 添加联系人
            Contact contact = Contact.builder()
                    .userId(contactApply.getUserId())
                    .friendId(contactApply.getFriendId())
                    .remark(null)
                    .conversationId(conversation.getId())
                    .createdTime(now)
                    .build();

            contactDao.insert(contact);

            // 添加私聊会话成员（A和B）
            ConversationMember conversationMemberA = ConversationMember.builder()
                    .conversationId(conversation.getId())
                    .userId(contactApply.getUserId())
                    .isMuted(false)
                    .isDeleted(false)
                    .lastReadMessageId(null)
                    .joinedTime(now)
                    .build();

            ConversationMember conversationMemberB = ConversationMember.builder()
                    .conversationId(conversation.getId())
                    .userId(contactApply.getFriendId())
                    .isMuted(false)
                    .isDeleted(false)
                    .lastReadMessageId(null)
                    .joinedTime(now)
                    .build();

            conversationMemberDao.insert(conversationMemberA);
            conversationMemberDao.insert(conversationMemberB);
            return true;
        }
        else{ // 拒绝
            // 修改联系
            contactApply.setStatus("REJECTED");
            contactApplyDao.updateById(contactApply);
        }
        return false;
    }
}

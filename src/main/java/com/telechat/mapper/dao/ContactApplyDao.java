/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/12/21 下午8:59
 */
package com.telechat.mapper.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.telechat.mapper.ContactApplyMapper;
import com.telechat.pojo.entity.ContactApply;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ContactApplyDao {
    @Autowired
    private ContactApplyMapper contactApplyMapper;


    /**
     * 根据联系申请ID查询联系申请
     *
     * @param contactApplyId 联系申请ID
     * @return ContactApply
     */
    public ContactApply selectById(Long contactApplyId) {
        return contactApplyMapper.selectById(contactApplyId);
    }

    /**
     * 插入联系申请
     *
     * @param contactApply 联系申请
     */
    public void insert(ContactApply contactApply) {
        contactApplyMapper.insert(contactApply);
    }


    /**
     * 更新联系申请
     *
     * @param contactApply 联系申请
     */
    public void updateById(ContactApply contactApply) {
        contactApplyMapper.updateById(contactApply);
    }

    /**
     * 获取联系人申请列表
     *
     * @param userId 用户ID
     * @param status 状态
     * @return List<Contact>
     */
    public List<ContactApply> selectApplyList(Long userId, String status) {
        LambdaQueryWrapper<ContactApply> queryWrapper = Wrappers.lambdaQuery(ContactApply.class)
                .eq(ContactApply::getFriendId, userId)
                .eq(ContactApply::getStatus, status);
        return contactApplyMapper.selectList(queryWrapper);
    }

    /**
     * 根据用户ID和好友ID查询联系申请
     * @param userId 用户ID
     * @param contactId 好友ID
     * @return ContactApply
     *  */
    public ContactApply selectByUserIdAndFriendId(Long userId, Long contactId) {
        LambdaQueryWrapper<ContactApply> queryWrapper = Wrappers.lambdaQuery(ContactApply.class)
                .eq(ContactApply::getUserId, userId)
                .eq(ContactApply::getFriendId, contactId);
        return contactApplyMapper.selectOne(queryWrapper);
    }

    /**
     * 删除联系申请
     * @param id 联系申请ID
     *  */
    public void deleteById(Long id) {
        contactApplyMapper.deleteById(id);
    }
}

/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/12/21 下午11:13
 */
package com.telechat.mapper.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.telechat.mapper.ContactMapper;
import com.telechat.pojo.entity.Contact;
import com.telechat.pojo.vo.ContactVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ContactDao {
    @Autowired
    private ContactMapper contactMapper;

    /**
     * 添加联系人
     * @param contact 联系人
     * */
    public void insert(Contact contact) {
        contactMapper.insert(contact);
    }

    /**
     * 根据联系人ID查询联系人
     * @param id 联系人ID
     *
     * @return Contact
     * */
    public Contact selectById(Long id) {
        return contactMapper.selectById(id);
    }

    /**
     * 获取联系人列表
     * @param userId 用户ID
     *
     * @return List<Contact>
     * */
    public List<Contact> list(Long userId) {
        LambdaQueryWrapper<Contact> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Contact::getUserId, userId);
        return contactMapper.selectList(queryWrapper);
    }

    /**
     * 根据用户ID和联系人ID查询联系人
     * @param userId 用户ID
     * @param contactId 联系人ID
     *
     * @return Contact
    * */
    public Contact selectByUserIdAndFriendId(Long userId, Long contactId) {
        LambdaQueryWrapper<Contact> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Contact::getUserId, userId)
                .eq(Contact::getFriendId, contactId);

        return contactMapper.selectOne(queryWrapper);
    }

    /**
     * 删除联系人
     * @param id 联系人ID
     * */
    public void delete(Long id) {
        contactMapper.deleteById(id);
    }

    /**
     * 修改联系人
     * @param contact 联系人
     * */
    public void updateById(Contact contact) {
        contactMapper.updateById(contact);
    }
}

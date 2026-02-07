/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/12/21 下午8:59
 */
package com.telechat.mapper.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.telechat.mapper.ContactApplyMapper;
import com.telechat.pojo.entity.ContactApply;
import com.telechat.pojo.enums.ContactApplyStatus; // 引入枚举
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository; // 建议使用 Repository

import java.util.List;

/**
 * 联系人申请数据访问层
 * 封装 Mybatis-Plus 的 Mapper 操作
 */
@Repository // 语义比 Component 更准确，代表数据访问组件
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
     * @param contactApply 联系申请实体
     */
    public void insert(ContactApply contactApply) {
        contactApplyMapper.insert(contactApply);
    }

    /**
     * 更新联系申请
     *
     * @param contactApply 联系申请实体
     */
    public void updateById(ContactApply contactApply) {
        contactApplyMapper.updateById(contactApply);
    }

    /**
     * 获取联系人申请列表
     * <p>优化点：</p>
     * 1. 参数改为枚举类型
     * 2. 增加按创建时间倒序排序 (最新的在最上面)
     *
     * @param userId 接收者ID (friend_id)
     * @param status 申请状态 (枚举)
     * @return List<ContactApply>
     */
    public List<ContactApply> selectApplyList(Long userId, ContactApplyStatus status) {
        LambdaQueryWrapper<ContactApply> queryWrapper = Wrappers.lambdaQuery(ContactApply.class)
                .eq(ContactApply::getFriendId, userId) // 查询发给我的
                .eq(ContactApply::getStatus, status)   // 匹配状态 (Mybatis-Plus会自动提取枚举的code值)
                .orderByDesc(ContactApply::getCreatedTime); // 体验优化：按时间倒序
        return contactApplyMapper.selectList(queryWrapper);
    }

    /**
     * 根据用户ID和好友ID查询联系申请
     * 用于检查是否重复申请，或者获取之前的申请记录
     *
     * @param userId    发起者ID
     * @param contactId 接收者ID (friendId)
     * @return ContactApply
     */
    public ContactApply selectByUserIdAndFriendId(Long userId, Long contactId) {
        LambdaQueryWrapper<ContactApply> queryWrapper = Wrappers.lambdaQuery(ContactApply.class)
                .eq(ContactApply::getUserId, userId)
                .eq(ContactApply::getFriendId, contactId);
        // 这里不需要限制 status，因为业务逻辑是：只要两个人之间有记录（哪怕是已拒绝的），可能都需要查出来判断
        return contactApplyMapper.selectOne(queryWrapper);
    }

    /**
     * 删除联系申请
     *
     * @param id 联系申请ID
     */
    public void deleteById(Long id) {
        contactApplyMapper.deleteById(id);
    }

    /**
     * 查询未读申请数量
     *
     * @param userId 查询该用户的未读好友申请
     * @return count
     */
    public Long getUnreadContactApplyCount(Long userId) {
        LambdaQueryWrapper<ContactApply> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ContactApply::getFriendId, userId)
                .eq(ContactApply::getIsRead, false); // 0
        return contactApplyMapper.selectCount(queryWrapper);
    }

    /**
     * 标记所有申请为已读状态
     *
     * @param userId 已读该用户的好友申请
     */
    public void markAll(Long userId) {
        LambdaUpdateWrapper<ContactApply> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ContactApply::getFriendId, userId)
                .eq(ContactApply::getIsRead, false)
                .set(ContactApply::getIsRead, true);
        contactApplyMapper.update(null, updateWrapper);
    }
}

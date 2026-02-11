/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/12/21 下午11:14
 */
package com.telechat.controller.user;

import com.telechat.pojo.dto.contact.UpdateContactDTO;
import com.telechat.pojo.result.Result;
import com.telechat.pojo.vo.ContactVO;
import com.telechat.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contact")
@Tag(name = "联系人接口")
@Slf4j
public class ContactController {

    @Autowired
    private ContactService contactService;

    @Operation(summary = "联系人列表")
    @GetMapping("/list")
    public Result<List<ContactVO>> list() {
        // 获取用户id
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("获取联系人列表: {}", userId);
        List<ContactVO> contactList = contactService.list(userId);
        return Result.success(contactList);
    }

    @Operation(summary = "删除联系人")
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Long id) {
        // 获取用户id
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("删除联系人: {}", id);
        boolean isDeleted = contactService.delete(id, userId);
        if (isDeleted) {
            return Result.success("删除成功");
        } else {
            return Result.error(400, "删除失败");
        }
    }

    // todo 加上修改备注，标签之类的，或者做其他拓展
    @Operation(summary = "更新联系人")
    @PutMapping("/update")
    public Result<String> update(@RequestBody UpdateContactDTO updateContactDTO) {
        // 获取用户id
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("更新联系人: {}", updateContactDTO);
        return contactService.update(updateContactDTO, userId) ? Result.success("更新成功") : Result.error(500, "更新失败");
    }
}

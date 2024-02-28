package com.atguigu.auth.controller;

import com.atguigu.auth.service.SysRoleService;
import com.atguigu.common.config.exception.GuiguException;
import com.atguigu.common.result.Result;
import com.atguigu.model.system.SysRole;
import com.atguigu.vo.system.AssignRoleVo;
import com.atguigu.vo.system.SysRoleQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

//      localhost:8800/admin/system/sysRole/findALl

@Api(tags = "角色管理接口")
@RestController
@RequestMapping("/admin/system/sysRole")
public class SysRoleController {


    @Autowired
    private SysRoleService sysRoleService;

//    1、根据用户获取角色数据
    @ApiOperation("根据用户获取角色数据")
    @GetMapping("/toAssign/{adminId}")
    public Result toAssign(@PathVariable Long adminId) {
        Map map = sysRoleService.findRoleByAdminId(adminId);
        return Result.ok(map);
    }


//    2、根据用户分配角色

    @ApiOperation("根据用户分配角色")
    @PostMapping("/doAssign")
    public Result doAssign(@RequestBody AssignRoleVo assignRoleVo){
        sysRoleService.doAssign(assignRoleVo);

        return Result.ok();
    }


    //     查询所有角色
//
//        public List<SysRole> findALl() {
//        List<SysRole> list = sysRoleService.list();
//        return list;
//    }
    @ApiOperation("查询所有角色")
    @GetMapping("/findAll")
    public Result findAll() {

//        try {
//            int a = 10 / 0;
//        } catch (Exception e) {
//            throw new GuiguException(20001,"出现自定义异常");
//        }
        List<SysRole> list = null;
        list = sysRoleService.list();
        return Result.ok(list);
    }

    // TODO: 2023/7/1 在其他的controller中也加入该注解
    @PreAuthorize("hasAuthority('bnt.sysRole.list')")
    @ApiOperation("条件分页查询")
    @GetMapping("{page}/{limit}")
    public Result PageQueryRole(@PathVariable Long page,
                                @PathVariable Long limit,
                                SysRoleQueryVo sysRoleQueryVo) {

        Page<SysRole> sysRolePage = new Page<>(page, limit);

        LambdaQueryWrapper<SysRole> sysRoleLambdaQueryWrapper = new LambdaQueryWrapper<>();
        String roleName = sysRoleQueryVo.getRoleName();
        if (!StringUtils.isEmpty(roleName)) {
            sysRoleLambdaQueryWrapper.like(SysRole::getRoleName, roleName);
        }
        Page<SysRole> rolePage = sysRoleService.page(sysRolePage, sysRoleLambdaQueryWrapper);

        return Result.ok(rolePage);
    }


    @PreAuthorize("hasAuthority('bnt.sysRole.add')")
    @ApiOperation("添加角色 ")
    @PostMapping("save")
    public Result save(@RequestBody SysRole sysRole) {

        boolean is_success = sysRoleService.save(sysRole);

        if (is_success) {
            return Result.ok();
        } else {
            return Result.fail();
        }

    }

    @PreAuthorize("hasAuthority('bnt.sysRole.list')")
    @ApiOperation("查询")
    @GetMapping("get/{id}")
    public Result get(@PathVariable Long id) {
        SysRole byId = sysRoleService.getById(id);
        return Result.ok(byId);
    }


    @PreAuthorize("hasAuthority('bnt.sysRole.update')")
    @ApiOperation("修改角色")
    @PutMapping("update")
    public Result update(@RequestBody SysRole sysRole) {

        boolean is_success = sysRoleService.updateById(sysRole);

        if (is_success) {
            return Result.ok();
        } else {
            return Result.fail();
        }

    }

    @PreAuthorize("hasAuthority('bnt.sysRole.remove')")
    @ApiOperation("根据id删除")
    @DeleteMapping("remove/{id}")
    public Result remove(@PathVariable Long id) {

        boolean is_success = sysRoleService.removeById(id);
        if (is_success) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    @PreAuthorize("hasAuthority('bnt.sysRole.remove')")
    @ApiOperation("批量删除")
    @DeleteMapping("batchRemove")
    public Result batchRemove(@RequestBody List<Long> idList) {
        boolean is_success = sysRoleService.removeByIds(idList);
        if (is_success) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

}

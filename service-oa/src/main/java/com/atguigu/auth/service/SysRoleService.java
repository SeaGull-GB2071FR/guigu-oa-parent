package com.atguigu.auth.service;

import com.atguigu.model.system.SysRole;
import com.atguigu.vo.system.AssignRoleVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface SysRoleService extends IService<SysRole> {
    //    1、根据用户获取角色数据
    Map<String, Object> findRoleByAdminId(Long id);

    //    2、根据用户分配角色
    void doAssign(AssignRoleVo assignRoleVo);

}

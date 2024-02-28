package com.atguigu.auth.service;

import com.atguigu.model.system.SysMenu;
import com.atguigu.vo.system.AssignMenuVo;
import com.atguigu.vo.system.RouterVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 菜单表 服务类
 * </p>
 *
 * @author atguigu
 * @since 2023-05-15
 */
public interface SysMenuService extends IService<SysMenu> {
//    获取菜单列表

    List<SysMenu> findNodes();

    void removeMenuById(Long id);

//    根据角色获取菜单
    List<SysMenu> findSysMenuByRoleId(Long roleId);

//    给角色分配权限
    void doAssign(AssignMenuVo assginMenuVo);

    //4 根据用户id获取用户可以操作的菜单列表
    List<RouterVo> findUserMenuList(Long id);

    //5 根据用户id获取用户可以操作的按钮列表
    List<String> findUserPermsList(Long id);
}

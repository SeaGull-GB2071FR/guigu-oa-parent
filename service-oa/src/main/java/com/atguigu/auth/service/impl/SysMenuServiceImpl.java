package com.atguigu.auth.service.impl;

import com.atguigu.auth.mapper.SysMenuMapper;
import com.atguigu.auth.service.SysMenuService;
import com.atguigu.auth.service.SysRoleMenuService;
import com.atguigu.auth.util.MenuHelper;
import com.atguigu.common.config.exception.GuiguException;
import com.atguigu.model.system.SysMenu;
import com.atguigu.model.system.SysRole;
import com.atguigu.model.system.SysRoleMenu;
import com.atguigu.vo.system.AssignMenuVo;
import com.atguigu.vo.system.MetaVo;
import com.atguigu.vo.system.RouterVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 菜单表 服务实现类
 * </p>
 *
 * @author atguigu
 * @since 2023-05-15
 */
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Autowired
    private SysRoleMenuService sysRoleMenuService;

    List<SysMenu> sysMenuList = null;


    //    获取菜单列表

    @Override
    public List<SysMenu> findNodes() {
        List<SysMenu> sysMenus = baseMapper.selectList(null);
        if (CollectionUtils.isEmpty(sysMenus)) return null;

        //构建树形数据
        List<SysMenu> result = MenuHelper.buildTree(sysMenus);
        return result;
    }

    //
    @Override
    public void removeMenuById(Long id) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getParentId, id);
        Integer Count = baseMapper.selectCount(wrapper);
        if (Count > 0) {
            throw new GuiguException(201, "菜单不能删除 ");
        }
        baseMapper.deleteById(id);
    }

    //根据角色获取菜单
    @Override
    public List<SysMenu> findSysMenuByRoleId(Long roleId) {
//         1、查询所有菜单- 添加条件，状态为1
        LambdaQueryWrapper<SysMenu> sysMenuLambdaQueryWrapper = new LambdaQueryWrapper<>();
        sysMenuLambdaQueryWrapper.eq(SysMenu::getStatus, 1);
        List<SysMenu> sysMenusList = baseMapper.selectList(sysMenuLambdaQueryWrapper);
//         2、根据角色id roleid查询，角色菜单关系表里面 角色id 和菜单id对应
        LambdaQueryWrapper<SysRoleMenu> sysRoleMenuLambdaQueryWrapper = new LambdaQueryWrapper<>();
        sysRoleMenuLambdaQueryWrapper.eq(SysRoleMenu::getRoleId, roleId);
        List<SysRoleMenu> sysRoleMenuList = sysRoleMenuService.list(sysRoleMenuLambdaQueryWrapper);
//        3、根据获取菜单id，获取对应菜单对象
//        3.1 拿菜单id 和所有菜单集合里面id进行比较，如果相同封装
        List<Long> menuIdList = sysRoleMenuList.stream().map(c -> c.getMenuId()).collect(Collectors.toList());

        sysMenusList.stream().forEach(item -> {
            item.setSelect(menuIdList.contains(item));
        });
//        4、返回规定格式菜单列表
//
        List<SysMenu> sysMenus = MenuHelper.buildTree(sysMenusList);
        return sysMenus;
    }

    //给角色分配权限
    @Override
    public void doAssign(AssignMenuVo assignMenuVo) {
        LambdaQueryWrapper<SysRoleMenu> sysRoleLambdaQueryWrapper = new LambdaQueryWrapper<>();
        sysRoleLambdaQueryWrapper.eq(SysRoleMenu::getRoleId, assignMenuVo.getRoleId());
        sysRoleMenuService.remove(sysRoleLambdaQueryWrapper);

        for (Long sysMenu : assignMenuVo.getMenuIdList()) {
            if (StringUtils.isEmpty(sysMenu))
                continue;
            SysRoleMenu sysRoleMenu = new SysRoleMenu();
            sysRoleMenu.setRoleId(assignMenuVo.getRoleId());
            sysRoleMenu.setMenuId(sysMenu);
            sysRoleMenuService.save(sysRoleMenu);
        }


    }

    //4 根据用户id获取用户可以操作的菜单列表
    @Override
    public List<RouterVo> findUserMenuList(Long id) {

        //1 判断当前用户是否是管理员 userid=1是管理员
        //1.1 如果是管理员 查询所有菜单列表
        if (id.longValue() == 1) {
            LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysMenu::getStatus, 1);
            wrapper.orderByAsc(SysMenu::getSortValue);
            sysMenuList = baseMapper.selectList(wrapper);
        } else {
            //1.2 如果不是管理员 根据userid 查询可以操作菜单列表
            //多表联查:用户关系表、角色菜单关系表、菜单表
            sysMenuList = baseMapper.findMenuListByUserId(id);
        }
        //2 把查询出来数据列表构建框架要求的路由数据结构
        //使用菜单操作工具类构建树形结构
        List<SysMenu> sysMenuTreeList = MenuHelper.buildTree(sysMenuList);
        //构建成框架要求的路由结构
        List<RouterVo> routerList = this.buildRouter(sysMenuTreeList);


        return routerList;
    }

    //构建成框架要求的路由结构
    private List<RouterVo> buildRouter(List<SysMenu> menus) {
        List<RouterVo> routers = new LinkedList<RouterVo>();
        for (SysMenu menu : menus) {
            RouterVo router = new RouterVo();
            router.setHidden(false);
            router.setAlwaysShow(false);
            router.setPath(getRouterPath(menu));
            router.setComponent(menu.getComponent());
            router.setMeta(new MetaVo(menu.getName(), menu.getIcon()));
            //下一层数据部分
            List<SysMenu> children = menu.getChildren();
            //如果当前是菜单，需将按钮对应的路由加载出来，如：“角色授权”按钮对应的路由在“系统管理”下面
            if (menu.getType().intValue() == 1) {
                List<SysMenu> hiddenMenuList = children.stream()
                        .filter(item -> !StringUtils.isEmpty(item.getComponent()))
                        .collect(Collectors.toList());
                for (SysMenu hiddenMenu : hiddenMenuList) {
                    RouterVo hiddenRouter = new RouterVo();
                    hiddenRouter.setHidden(true);
                    hiddenRouter.setAlwaysShow(false);
                    hiddenRouter.setPath(getRouterPath(hiddenMenu));
                    hiddenRouter.setComponent(hiddenMenu.getComponent());
                    hiddenRouter.setMeta(new MetaVo(hiddenMenu.getName(), hiddenMenu.getIcon()));
                    routers.add(hiddenRouter);
                }
            } else {
                if (!CollectionUtils.isEmpty(children)) {
                    if (children.size() > 0) {
                        router.setAlwaysShow(true);
                    }
                    router.setChildren(buildRouter(children));
                }
            }
            routers.add(router);
        }
        return routers;
    }


    //5 根据用户id获取用户可以操作的按钮列表
    @Override
    public List<String> findUserPermsList(Long id) {
        //1 判断当前用户是否是管理员 userid=1是管理员
        //1.1 如果是管理员 查询所有菜单列表.
        if (id.longValue() == 1) {
            LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysMenu::getStatus, 1);
            sysMenuList = baseMapper.selectList(wrapper);
        } else {
            //1.2 如果不是管理员 根据userid 查询可以操作菜单列表
            //多表联查:用户关系表、角色菜单关系表、菜单表
            sysMenuList = baseMapper.findMenuListByUserId(id);
        }
        //2 从查询出来的数据里面 获取可以操作按钮值的list集合 返回
        List<String> list = sysMenuList.stream().filter(item -> item.getType() == 2).map(item -> item.getPerms()).collect(Collectors.toList());


        return list;
    }

    /**
     * 获取路由地址 * * @param menu 菜单信息 * @return 路由地址
     */
    public String getRouterPath(SysMenu menu) {
        String routerPath = "/" + menu.getPath();
        if (menu.getParentId().intValue() != 0) {
            routerPath = menu.getPath();
        }
        return routerPath;
    }
}

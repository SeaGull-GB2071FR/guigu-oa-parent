package com.atguigu.auth.service.impl;

import com.atguigu.auth.service.SysMenuService;
import com.atguigu.auth.service.SysUserService;
import com.atguigu.model.system.SysUser;
import com.atguigu.security.custom.CustomUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
@SuppressWarnings("all")
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysMenuService sysMenuService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //根据用户名进行查询
        SysUser sysUser = sysUserService.getUserByUserName(username);
        if (null == sysUser) {
            throw new UsernameNotFoundException("用户名不存在！");
        }

        if (sysUser.getStatus().intValue() == 0) {
            throw new RuntimeException("账号已停用");
        }

//        根据userid查询用户操作权限数据
        List<String> userPermsList = sysMenuService.findUserPermsList(sysUser.getId());

//        创建list集合，用于封装最终权限数据
        ArrayList<SimpleGrantedAuthority> authList = new ArrayList<>();

//        查询list集合遍历
        for (String perm :
                userPermsList) {
            authList.add(new SimpleGrantedAuthority(perm.trim()));
        }

        //根据用户名（userid） 查询用户操作权限数据，封装返回
        //Collections.emptyList() 权限数据集合
        return new CustomUser(sysUser, authList);
    }
}
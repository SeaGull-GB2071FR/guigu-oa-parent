package com.atguigu.auth.service.impl;

import com.atguigu.auth.mapper.SysRoleMapper;
import com.atguigu.auth.service.SysRoleService;
import com.atguigu.auth.service.SysUserRoleService;
import com.atguigu.model.system.SysRole;
import com.atguigu.model.system.SysUserRole;
import com.atguigu.vo.system.AssignRoleVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Autowired
    private SysUserRoleService sysUserRoleService;

    @Override
    public Map<String, Object> findRoleByAdminId(Long id) {
        //查询所有的角色
        List<SysRole> allRolesList = baseMapper.selectList(null);
        //拥有的角色id 角色用户关系表，查询id对应角色的id
        LambdaQueryWrapper<SysUserRole> sysUserRoleLambdaQueryWrapper = new LambdaQueryWrapper<>();
        sysUserRoleLambdaQueryWrapper.eq(SysUserRole::getRoleId,id);
        List<SysUserRole> existUserRoleList = sysUserRoleService.list(sysUserRoleLambdaQueryWrapper);
        List<Long> existRoleIdList = existUserRoleList.stream().map(c -> c.getRoleId()).collect(Collectors.toList());
        //对角色进行分类   找到对应角色信息
        //根据角色id到所有的角色的list集合进行比较
        ArrayList<SysRole> assginRoleList = new ArrayList<>();
        for (SysRole sysRole:
                allRolesList) {
            if (existRoleIdList.contains(sysRole.getId())){
                assginRoleList.add(sysRole);
            }
            
        }

        Map<String, Object> roleMap = new HashMap<>();
        roleMap.put("assginRoleList", assginRoleList);
        roleMap.put("allRolesList", allRolesList);
        return roleMap;

    }

    @Override
    public void doAssign(AssignRoleVo assignRoleVo) {
        //删除用户
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, assignRoleVo.getUserId());
        sysUserRoleService.remove(wrapper);
        //重新分配
        for(Long roleId : assignRoleVo.getRoleIdList()) {
            if(StringUtils.isEmpty(roleId))
                continue;
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(assignRoleVo.getUserId());
            userRole.setRoleId(roleId);
            sysUserRoleService.save(userRole);
        }

    }
}

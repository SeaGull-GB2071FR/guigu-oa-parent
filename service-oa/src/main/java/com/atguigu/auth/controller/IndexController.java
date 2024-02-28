package com.atguigu.auth.controller;


import com.atguigu.auth.service.SysMenuService;
import com.atguigu.auth.service.SysUserService;
import com.atguigu.common.config.exception.GuiguException;
import com.atguigu.common.jwt.JwtHelper;
import com.atguigu.common.result.Result;
import com.atguigu.common.utils.MD5;
import com.atguigu.model.system.SysUser;
import com.atguigu.vo.system.LoginVo;
import com.atguigu.vo.system.RouterVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "后台登录管理")
@RestController
@RequestMapping("/admin/system/index")
public class IndexController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysMenuService sysMenuService;

    //    login
    @PostMapping("login")
    public Result login(@RequestBody LoginVo loginVo) {
//        code	20000
//        data	Object { token: "admin-token" }
        Map<String, Object> map = new HashMap<>();
//        map.put("token", "admin-token");
//        return Result.ok(map);

        //1、获取输入用户名和密码

        //2、根据用户名查询数据库
        String username = loginVo.getUsername();
        LambdaQueryWrapper<SysUser> sysUserLambdaQueryWrapper = new LambdaQueryWrapper<>();
        sysUserLambdaQueryWrapper.eq(SysUser::getUsername,username);
        SysUser sysUser = sysUserService.getOne(sysUserLambdaQueryWrapper);
        //3、用户信息是否存在
        if (sysUser == null) {
            throw new GuiguException(201,"用户不存在");
        }
        //4、判断密码
        //数据库的MD5密码
        String password_db = sysUser.getPassword();
        String password_input = MD5.encrypt(loginVo.getPassword());

        if (!password_db.equals(password_input)) {
            throw new GuiguException(201,"密码错误");
        }
        //5、判断用户是否被禁用
        if (sysUser.getStatus().intValue() == 0) {
            throw new GuiguException(201,"用户被禁用");
        }
        //6、使用jwt根据用户id和用户名称生成token字符串
        String token = JwtHelper.createToken(sysUser.getId(), sysUser.getUsername());
        map.put("token",token);
        //7、返回
        return Result.ok(map);
    }

//    Info

    //       {
//            "data": {
//            "roles": [
//            "admin"
//		],
//            "introduction": "I am a super administrator",
//                    "avatar": "https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif",
//                    "name": "Super Admin"
//           }
//        }

    @GetMapping("info")
    public Result Info(HttpServletRequest request) {
        //1 从请求头获取用户信息（获取请求头token字符串）
        String token = request.getHeader("token");

        //2 从token字符串获取用户id 或者用户名称
        Long userId =  JwtHelper.getUserId(token);

        //3 根据id查询数据库，把用户信息获取出来
        SysUser sysUser = sysUserService.getById(userId);

        //4 根据用户id获取用户可以操作的菜单列表
        //查询数据库动态构建路由结构
        List<RouterVo> routerVoList = sysMenuService.findUserMenuList(userId);

        //5 根据用户id获取用户可以操作的按钮列表
        List<String> permsList = sysMenuService.findUserPermsList(userId);


        Map<String, Object> map = new HashMap<>();
        map.put("roles", "[admin]");
        map.put("name", sysUser.getName());
        map.put("avatar", "https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");

        //返回用户可以操作按钮
        map.put("buttons", permsList);
        //返回用户可以操作菜单
        map.put("routers", routerVoList);
        return Result.ok(map);
    }

    @PostMapping("logout")
    public Result logout() {
        return Result.ok();
    }
}

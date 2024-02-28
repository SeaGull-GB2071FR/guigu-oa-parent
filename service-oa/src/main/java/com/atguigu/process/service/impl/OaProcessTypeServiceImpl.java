package com.atguigu.process.service.impl;

import com.atguigu.model.process.ProcessTemplate;
import com.atguigu.model.process.ProcessType;
import com.atguigu.process.mapper.OaProcessTypeMapper;
import com.atguigu.process.service.OaProcessTemplateService;
import com.atguigu.process.service.OaProcessTypeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 审批类型 服务实现类
 * </p>
 *
 * @author atguigu
 * @since 2023-08-30
 */
@Service
public class OaProcessTypeServiceImpl extends ServiceImpl<OaProcessTypeMapper, ProcessType> implements OaProcessTypeService {

    @Autowired
    private OaProcessTemplateService processTemplateService;


    //    查询所有审批分类和每个分类所有审批模板
    @Override
    public List<ProcessType> findProcessType() {
//    1 查询所有审批分类，返回list集合
        List<ProcessType> processTypes = baseMapper.selectList(null);

//    2 遍历返回所有审批分类list集合

        for (ProcessType processType :
                processTypes) {
            //    3 得到每个审批分类，根据审批分类id查询对应审批模板
            Long TypeId = processType.getId();
            //    根据审批分类id查询对应审批模板
            LambdaQueryWrapper<ProcessTemplate> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(ProcessTemplate::getProcessTypeId, TypeId);
            List<ProcessTemplate> list = processTemplateService.list(lambdaQueryWrapper);

            //    4 根据审批分类id查询对应审批模板数据（List）封装刀每个审批分类对象里面
            processType.setProcessTemplateList(list);
        }


        return processTypes;
    }
}

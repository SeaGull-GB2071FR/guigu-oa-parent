package com.atguigu.process.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.auth.service.SysUserService;
import com.atguigu.model.process.Process;
import com.atguigu.model.process.ProcessRecord;
import com.atguigu.model.process.ProcessTemplate;
import com.atguigu.model.system.SysUser;
import com.atguigu.process.mapper.OaProcessMapper;
import com.atguigu.process.service.OaProcessRecordService;
import com.atguigu.process.service.OaProcessService;
import com.atguigu.process.service.OaProcessTemplateService;
import com.atguigu.security.custom.LoginUserInfoHelper;
import com.atguigu.vo.process.ApprovalVo;
import com.atguigu.vo.process.ProcessFormVo;
import com.atguigu.vo.process.ProcessQueryVo;
import com.atguigu.vo.process.ProcessVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.EndEvent;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.InputStream;
import java.sql.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

/**
 * <p>
 * 审批类型 服务实现类
 * </p>
 *
 * @author atguigu
 * @since 2023-09-01
 */
@Service
public class OaProcessServiceImpl extends ServiceImpl<OaProcessMapper, Process> implements OaProcessService {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private OaProcessTemplateService processTemplateService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private OaProcessRecordService oaProcessRecordService;

    @Autowired
    private HistoryService historyService;

    //    审批管理列表
    @Override
    public IPage<ProcessVo> selectPage(Page<ProcessVo> pageParam, ProcessQueryVo processQueryVo) {
        IPage<ProcessVo> PageParam = baseMapper.selectPage(pageParam, processQueryVo);
        return PageParam;
    }

    //    部署流程定义
    @Override
    public void deployByZip(String deployPath) {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(deployPath);
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        //    部署
        Deployment deployment = repositoryService.createDeployment().addZipInputStream(zipInputStream).deploy();

        System.out.println(deployment.getId());
        System.out.println(deployment.getName());
    }

    @Override
    public void startUp(ProcessFormVo processFormVo) {
//        1 根据当前用户id获取用户信息
        SysUser sysUser = sysUserService.getById(LoginUserInfoHelper.getUserId());

//        2 根据审批模板id把模板信息查询
        ProcessTemplate processTemplate = processTemplateService.getById(processFormVo.getProcessTemplateId());

//        3 保存提交审批信息到业务表, oa_process
        Process process = new Process();
//          将一个对象中的属性复制到另一个空的对象中，找到相同的属性然后复制过来
        BeanUtils.copyProperties(processFormVo, process);
        String workNo = System.currentTimeMillis() + "";
        process.setProcessCode(workNo);

        process.setUserId(LoginUserInfoHelper.getUserId());
        process.setFormValues(processFormVo.getFormValues());
        process.setTitle(sysUser.getName() + "发起" + processTemplate.getName() + "申请");
        process.setStatus(1);// 审批中的状态
        baseMapper.insert(process);

//        4 启动流程实例 - RuntimeService
//        4.1 流程定义key
        String processDefinitionKey = processTemplate.getProcessDefinitionKey();

//        4.2 业务key processId
        String businessKey = String.valueOf(process.getId());

//        4.3 流程参数 form表单json 数据，转换map集合
        String formValues = processFormVo.getFormValues();
//        formData
        JSONObject jsonObject = JSON.parseObject(formValues);
        JSONObject formData = jsonObject.getJSONObject("formData");

//        遍历formData得到内容 ，封装map集合
        HashMap<String, Object> map = new HashMap<>();
        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }

        HashMap<String, Object> variables = new HashMap<>();
        variables.put("data", map);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey, businessKey, variables);
//        5 查询下一个审批人
        List<Task> list = this.getCurrentTaskList(processInstance.getId());
        ArrayList<String> nameList = new ArrayList<>();
        // todo 是admin申请，结果是张三审批
//        已解决：ACTIVITI 中的流程图的 参数和数据库中的参数不一样

        for (Task task : list
        ) {
            String assigneeName = task.getAssignee();
            SysUser user = sysUserService.getUserByUserName(assigneeName);
            if (user == null) continue;
            String name = user.getName();
            nameList.add(name);
//        TODO 6 推送消息

        }

//        7 业务和流程关联 更新oa_process 数据
        process.setProcessInstanceId(processInstance.getId());
        process.setDescription("等待" + StringUtils.join(nameList.toArray(), ",") + "审批");
        baseMapper.updateById(process);

//        记录操作审批信息记录
        oaProcessRecordService.record(process.getId(), 1, "发起申请");
    }


    //    查询待完成任务列表
    @Override
    public IPage<ProcessVo> findPending(IPage<Process> processPage) {

        //  1 封装查询条件，根据当前登录的用户名称
        TaskQuery query = taskService.createTaskQuery().taskAssignee(LoginUserInfoHelper.getUsername())
                .orderByTaskCreateTime()
                .desc();
        long totalCount = query.count();
        //  2 调用方法分页条件查询，返回 List 集合，待办任务集合
        //  第一个参数：开始位置
        //  第二个参数；每页显示记录数
        int begin = (int) ((processPage.getCurrent() - 1) * processPage.getSize());
        int size = (int) processPage.getSize();
        List<Task> TaskList = query.listPage(begin, size);

        //  3 封装返回 List 集合数据 到 List<ProcessVo> 里面
        //  List<Task> -- List<ProcessVo>
        ArrayList<ProcessVo> processVoList = new ArrayList<>();
        for (Task task :
                TaskList) {
            //  从task获取流程实例id
            String processInstanceId = task.getProcessInstanceId();
            //  根据流程实例id获取实例对象
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            //  从流程实例对象获取业务key
            String businessKey = processInstance.getBusinessKey();
            if (businessKey == null) continue;
            //  根据业务key获取Process对象
            long processId = Long.parseLong(businessKey);
            Process process = baseMapper.selectById(processId);

            //  Process对象 复制 ProcessVo对象
            ProcessVo processVo = new ProcessVo();
            BeanUtils.copyProperties(process, processVo);
            processVo.setTaskId(task.getId());
            //  放到最终list集合processVoList
            processVoList.add(processVo);

        }


        //  4 封装返回 iPage 对象
        IPage<ProcessVo> page = new Page<ProcessVo>(processPage.getCurrent(), processPage.getSize(), totalCount);

        page.setRecords(processVoList);
        return page;
    }


    //    查看审批详情信息
    @Override
    public Map<String, Object> show(Long id) {
        //1 根据流程id获取流程信息Process
        Process process = baseMapper.selectById(id);

        //2 根据流程id获取流程记录信息
        LambdaQueryWrapper<ProcessRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessRecord::getProcessId, id);
        List<ProcessRecord> processRecordList = oaProcessRecordService.list(wrapper);

        //3 根据模板id查询模板信息
        ProcessTemplate processTemplate = processTemplateService.getById(id);


        //4判断当前用户是否可以审批
        //可以看到信息不一定能审批，不能重复审批
        boolean isApprove = false;
        List<Task> taskList = this.getCurrentTaskList(process.getProcessInstanceId());
        for (Task task : taskList) {
//            判断当前任务审批人是否是当前用户
            if (task.getAssignee().equals(LoginUserInfoHelper.getUsername())) {
                isApprove = true;
            }

        }

        //5 查询数据封装到map集合，返回
        HashMap<String, Object> map = new HashMap<>();
        map.put("process", process);
        map.put("processRecordList", processRecordList);
        map.put("processTemplate", processTemplate);
        map.put("isApprove", isApprove);


        return map;
    }

    //    审批
    @Override
    public void approve(ApprovalVo approvalVo) {
        //1     从 approvalVo 获取任务 id ,根据任务 id 获取流程变量
        String taskId = approvalVo.getTaskId();
        Map<String, Object> variables = taskService.getVariables(taskId);
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            System.out.println(entry.getKey());
            System.out.println(entry.getValue());
        }

        //2     判断审批状态值
        //2.1   状态值 =1 审批通过
        //2.2   状态值 =-1 驳回，流程直接结束
        if (approvalVo.getStatus() == 1) {

            taskService.complete(taskId, variables);
        } else {

            this.endTask(taskId);
        }

        //3     记录审批相关过程信息 oa_process_record
        String description = (approvalVo.getStatus().intValue() == 1) ? "已通过" : "驳回";
        oaProcessRecordService.record(approvalVo.getProcessId(), approvalVo.getStatus(), description);

        //4     查询下一个审批人，更新流程表记录 process 表记录
        Process process = baseMapper.selectById(approvalVo.getProcessId());
        //      查询任务
        List<Task> taskList = this.getCurrentTaskList(process.getProcessInstanceId());
        if (!CollectionUtils.isEmpty(taskList)) {
            List<String> assignList = new ArrayList<>();
            for (Task task : taskList) {
                String assignee = task.getAssignee();
                SysUser sysUser = sysUserService.getUserByUserName(assignee);
                assignList.add(sysUser.getName());

                //  todo 公众号消息推送

            }

            //  更新process流程信息
            process.setDescription("等待" + StringUtils.join(assignList.toArray(), ",") + "审批");
            process.setStatus(1);
        } else {
            if (approvalVo.getStatus().intValue() == 1) {
                process.setDescription("审批完成（同意）");
                process.setStatus(2);
            } else {
                process.setDescription("审批完成（拒绝）");
                process.setStatus(-1);
            }
        }
        //推送消息给申请人
        baseMapper.updateById(process);


    }

    @Override
    public IPage<ProcessVo> findProcessed(Page<Process> pageParam) {


        //  封装查询条件
        //  调用方法条件分页查询，返回List集合
        //  遍历返回List集合，封装List<ProcessVo>
        //  IPage.封装分页查询所有数据，返回

        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(LoginUserInfoHelper.getUsername())
                .finished().orderByTaskCreateTime().desc();
        List<HistoricTaskInstance> list = query.listPage((int) ((pageParam.getCurrent() - 1) * pageParam.getSize()), (int) pageParam.getSize());
        long totalCount = query.count();

        List<ProcessVo> processList = new ArrayList<>();
        for (HistoricTaskInstance item : list) {
            String processInstanceId = item.getProcessInstanceId();
            Process process = this.getOne(new LambdaQueryWrapper<Process>().eq(Process::getProcessInstanceId, processInstanceId));
            ProcessVo processVo = new ProcessVo();
            if (process == null) continue;
            BeanUtils.copyProperties(process, processVo);
            processVo.setTaskId("0");
            processList.add(processVo);
        }
        IPage<ProcessVo> page = new Page(pageParam.getCurrent(), pageParam.getSize(), totalCount);
        page.setRecords(processList);
        return page;
    }

    @Override
    public IPage<ProcessVo> findStarted(Page<ProcessVo> pageParam) {
        ProcessQueryVo processQueryVo = new ProcessQueryVo();
        processQueryVo.setUserId(LoginUserInfoHelper.getUserId());
        IPage<ProcessVo> page = baseMapper.selectPage(pageParam, processQueryVo);
        for (ProcessVo item : page.getRecords()) {
            item.setTaskId("0");
        }
        return page;
    }


    @SuppressWarnings("all")
    private void endTask(String taskId) {
        //1 根据任务 id 获取任务对象 Task
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

        //2 获取流程定义模型 BpmnModel
        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());

        //3 获取结束流向节点
        List<EndEvent> endEventList = bpmnModel.getMainProcess().findFlowElementsOfType(EndEvent.class);
        if (CollectionUtils.isEmpty(endEventList))
            return;

        //4 当前流向节点
        FlowNode endEvent = (FlowNode) endEventList.get(0);

        FlowNode currentFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(task.getTaskDefinitionKey());

        //  临时保存当前活动的原始方向
        List originalSequenceFlowList = new ArrayList<>();
        originalSequenceFlowList.addAll(currentFlowNode.getOutgoingFlows());

        //5 清理当前流动方向
        currentFlowNode.getOutgoingFlows().clear();

        //6 清理活动方向
        SequenceFlow newSequenceFlow = new SequenceFlow();
        newSequenceFlow.setId("newSequenceFlowId");
        newSequenceFlow.setSourceFlowElement(currentFlowNode);
        newSequenceFlow.setTargetFlowElement(endEvent);

        List newSequenceFlowList = new ArrayList<>();
        newSequenceFlowList.add(newSequenceFlow);

        //7 当前节点指问新方问
        currentFlowNode.setOutgoingFlows(newSequenceFlowList);

        //8 完成当前任务
        taskService.complete(task.getId());


    }

    //      当前任务列表
    private List<Task> getCurrentTaskList(String id) {
        List<Task> taskList = taskService.createTaskQuery().processInstanceId(id).list();

        return taskList;
    }
}

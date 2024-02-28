package com.atguigu.auth.activiti;


import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class ProcessTest {

    //    注入RepositoryService
    @Autowired
    private RepositoryService repositoryService;

    //    注入runtimeService
    @Autowired
    private RuntimeService runtimeService;

    //    注入TaskService
    @Autowired
    private TaskService taskService;

    //    注入HistoryService
    @Autowired
    private HistoryService historyService;

    //    单个挂起
    @Test
    public void SingleSuspendProcessInstance() {
        String processInstanceId = "6cb732d0-3a89-11ee-9f36-005056c00001";

        ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();

        boolean suspended = instance.isSuspended();

        if (suspended) {
            runtimeService.activateProcessInstanceById(processInstanceId);
        } else {
            runtimeService.suspendProcessInstanceById(processInstanceId);
        }
    }

    //    全部流程挂起
    @Test
    public void suspendProcessInstanceAll() {
        //1 获取流程定义对象
        ProcessDefinition qingjia = repositoryService.createProcessDefinitionQuery().processDefinitionKey("qingjia").singleResult();

        //2 调用流程定义对象的方法判断当前状态： 挂起 激活
        boolean suspended = qingjia.isSuspended();

        //3 判断如果挂起，实现激活
        if (suspended) {
            //第一个参数流程定义id
            //第二个参数是否激活true
            //第三个参数时间周
            repositoryService.activateProcessDefinitionById(qingjia.getId(), true, null);
        } else {
            //如果是激活状态，就挂起
            repositoryService.suspendProcessDefinitionById(qingjia.getId(), true, null);
        }
    }

    //    创建流程实例，指定BusinessKey
    @Test
    public void startUpProcessAddBusinessKey() {
        ProcessInstance instance = runtimeService.startProcessInstanceById("qingjia", "1001");
        System.out.println(instance.getBusinessKey());
    }

    //    查询已经处理的任务
    @Test
    public void findCompleteTask() {
        String assign = "zhangsan";

        List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(assign)
                .finished()
                .list();
        for (HistoricTaskInstance historicTaskInstance :
                list) {
            System.out.println("流程实例id : " + historicTaskInstance.getProcessInstanceId());
            System.out.println("任务id : " + historicTaskInstance.getId());
            System.out.println("任务负责人 : " + historicTaskInstance.getAssignee());
            System.out.println("任务名称 : " + historicTaskInstance.getName());

        }
    }

    //    处理当前任务
    @Test
    public void completeTask() {
        String assign = "zhangsan";

//        查询负责人需要处理的任务，返回一条
        Task task = taskService.createTaskQuery()
                .taskAssignee(assign)
                .singleResult();

        //  完成任务，参数：任务id
        taskService.complete(task.getId());
    }

    //    查询个人的代办任务 --- zhangsan
    @Test
    public void findTaskList() {
        String assign = "zhangsan";

        List<Task> list = taskService.createTaskQuery()
                .taskAssignee(assign)
                .list();

        for (Task t :
                list) {
            System.out.println("流程实例Id : " + t.getProcessInstanceId());
            System.out.println("任务Id : " + t.getId());
            System.out.println("任务负责人 : " + t.getAssignee());
            System.out.println("任务名称 : " + t.getName());


        }
    }

    //    启动流程实例
    @Test
    public void startProcess() {
        ProcessInstance startProcessInstanceByKey = runtimeService.startProcessInstanceByKey("qingjia");
        System.out.println("流程定义id : " + startProcessInstanceByKey.getProcessInstanceId());
        System.out.println("流程实例id : " + startProcessInstanceByKey.getId());
        System.out.println("流程活动id : " + startProcessInstanceByKey.getActivityId());
    }


    //    单个文件的部署
    @Test
    public void deployProcess() {

        //    流程部署
        Deployment deploy = repositoryService.createDeployment()
                .addClasspathResource("process/qingjia.bpmn20.xml")
                .addClasspathResource("process/qingjia.png")
                .name("请假申请流程")
                .deploy();
        System.out.println(deploy.getId());
        System.out.println(deploy.getName());

    }

}

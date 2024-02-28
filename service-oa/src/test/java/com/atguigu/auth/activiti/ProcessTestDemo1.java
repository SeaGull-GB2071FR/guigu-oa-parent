package com.atguigu.auth.activiti;

import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class ProcessTestDemo1 {

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

    /**
     * 流程定义部署
     */
    @Test
    public void deployProcess() {
        // 流程部署
        Deployment deploy = repositoryService.createDeployment()
                .addClasspathResource("process/jiaban.bpmn20.xml")
                .addClasspathResource("process/jiaban.png")
                .name("加班申请流程")
                .deploy();
        System.out.println(deploy.getId());
        System.out.println(deploy.getName());
    }

    /**
     * 启动流程实例
     */
    @Test
    public void startUpProcess() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("assignee1", "zhangsan");
        variables.put("assignee2", "lisi");
        //创建流程实例,我们需要知道流程定义的key
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("jiaban", variables);
        //输出实例的相关信息
        System.out.println("流程定义id：" + processInstance.getProcessDefinitionId());
        System.out.println("流程实例id：" + processInstance.getId());
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
}

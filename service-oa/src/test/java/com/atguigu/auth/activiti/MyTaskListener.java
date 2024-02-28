package com.atguigu.auth.activiti;

import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;

public class MyTaskListener implements TaskListener {
    @Override
    public void notify(DelegateTask delegateTask) {
        if (delegateTask.getName().equals("经理审批")) {
//            分配任务
            delegateTask.setAssignee("");
        } else if (delegateTask.getName().equals("人事审批")) {
//
            delegateTask.setAssignee("");
        }
    }
}

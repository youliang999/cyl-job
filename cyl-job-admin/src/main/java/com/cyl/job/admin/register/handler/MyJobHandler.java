package com.cyl.job.admin.register.handler;

import com.cyl.api.model.ResponseModel;
import com.cyl.job.core.handler.IJobHandler;

public class MyJobHandler extends IJobHandler {

    public void init() {
        System.out.println("MyJobHandler init...");
    }

    @Override
    public ResponseModel<String> execute(String param) throws Exception {
        System.out.println("MyJobHandler execute...");
        return ResponseModel.SUCCESS;
    }
}

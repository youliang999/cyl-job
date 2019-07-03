package com.cyl.job.admin.core.route.strategy;

import com.cyl.job.admin.core.route.ExecutorRouter;
import com.cyl.job.core.biz.model.ResponseModel;
import com.cyl.job.core.biz.model.TriggerParam;
import java.util.List;

public class ExecutorRouteFirst extends ExecutorRouter {

    @Override
    public ResponseModel<String> route(TriggerParam triggerParam, List<String> addressList) {
        return new ResponseModel<>(addressList.get(0));
    }
}

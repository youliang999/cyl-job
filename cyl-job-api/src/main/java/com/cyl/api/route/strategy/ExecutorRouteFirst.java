package com.cyl.api.route.strategy;

import com.cyl.api.model.ResponseModel;
import com.cyl.api.model.TriggerParam;
import com.cyl.api.route.ExecutorRouter;
import java.util.List;

public class ExecutorRouteFirst extends ExecutorRouter {

    @Override
    public ResponseModel<String> route(TriggerParam triggerParam, List<String> addressList) {
        return new ResponseModel<>(addressList.get(0));
    }
}

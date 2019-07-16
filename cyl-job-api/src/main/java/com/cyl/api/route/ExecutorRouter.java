package com.cyl.api.route;

import com.cyl.api.model.ResponseModel;
import com.cyl.api.model.TriggerParam;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ExecutorRouter {
    protected static Logger logger = LoggerFactory.getLogger(ExecutorRouter.class);

    public abstract ResponseModel<String> route(TriggerParam triggerParam, List<String> addressList);
}

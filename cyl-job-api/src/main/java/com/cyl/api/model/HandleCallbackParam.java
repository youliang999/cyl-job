package com.cyl.api.model;

import java.io.Serializable;

public class HandleCallbackParam implements Serializable {
    private static final long serialVersionUID = 42L;

    private int logId;
    private long logDateTim;

    private ResponseModel<String> executeResult;

    public HandleCallbackParam(){}
    public HandleCallbackParam(int logId, long logDateTim, ResponseModel<String> executeResult) {
        this.logId = logId;
        this.logDateTim = logDateTim;
        this.executeResult = executeResult;
    }

    public int getLogId() {
        return logId;
    }

    public void setLogId(int logId) {
        this.logId = logId;
    }

    public long getLogDateTim() {
        return logDateTim;
    }

    public void setLogDateTim(long logDateTim) {
        this.logDateTim = logDateTim;
    }

    public ResponseModel<String> getExecuteResult() {
        return executeResult;
    }

    public void setExecuteResult(ResponseModel<String> executeResult) {
        this.executeResult = executeResult;
    }

    @Override
    public String toString() {
        return "HandleCallbackParam{" +
                "logId=" + logId +
                ", logDateTim=" + logDateTim +
                ", executeResult=" + executeResult +
                '}';
    }
}

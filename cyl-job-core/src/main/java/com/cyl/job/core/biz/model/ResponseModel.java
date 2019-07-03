package com.cyl.job.core.biz.model;

import java.io.Serializable;

public class ResponseModel<T> implements Serializable {
    private static final long serialVersionUID = -1809024665986995739L;

    public static final int SUCCESS_CODE = 200;
    public static final int FAIL_CODE = 500;

    public static final ResponseModel<String> SUCCESS = new ResponseModel<String>(null);
    public static final ResponseModel<String> FAIL = new ResponseModel<String>(FAIL_CODE, null);

    private int code;
    private String msg;
    private T content;

    public ResponseModel(){}
    public ResponseModel(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
    public ResponseModel(T content) {
        this.code = SUCCESS_CODE;
        this.content = content;
    }

    public int getCode() {
        return code;
    }
    public void setCode(int code) {
        this.code = code;
    }
    public String getMsg() {
        return msg;
    }
    public void setMsg(String msg) {
        this.msg = msg;
    }
    public T getContent() {
        return content;
    }
    public void setContent(T content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "ResponsModel [code=" + code + ", msg=" + msg + ", content=" + content + "]";
    }
}

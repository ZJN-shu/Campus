package com.campus.dto;

import lombok.Data;

@Data
public class Result {
    private Boolean success;
    private String errorMsg;
    private Object data;
    private Long total;

    public static Result ok() {
        Result r = new Result();
        r.setSuccess(true);
        return r;
    }

    public static Result ok(Object data) {
        Result r = new Result();
        r.setSuccess(true);
        r.setData(data);
        return r;
    }

    public static Result ok(Object data, Long total) {
        Result r = new Result();
        r.setSuccess(true);
        r.setData(data);
        r.setTotal(total);
        return r;
    }

    public static Result fail(String errorMsg) {
        Result r = new Result();
        r.setSuccess(false);
        r.setErrorMsg(errorMsg);
        return r;
    }
}
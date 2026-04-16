package com.marrylink.common;

import lombok.Data;

@Data
public class Result<T> {
    private String code;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.setCode("00000");
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    public static <T> Result<T> ok(T data, String message) {
        Result<T> r = new Result<>();
        r.setCode("00000");
        r.setMessage(message);
        r.setData(data);
        return r;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> error(String message) {
        Result<T> r = new Result<>();
        r.setCode("B0001" +
                "");
        r.setMessage(message);
        return r;
    }

    public static <T> Result<T> error(String code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }
}

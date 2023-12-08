package com.bazzi.netconf.entity;


import com.bazzi.netconf.bean.ErrorCode;

import java.io.Serializable;

public class Result<T> implements Serializable {
    private static final long serialVersionUID = 7706141257361229950L;
    private T data;
    private boolean success = true;
    private String message = "";
    private String code = "";

    public Result() {
    }

    public Result(T data) {
        this.data = data;
    }

    public void setError(String code, String message) {
        this.code = code;
        this.message = message;
        this.success = false;
    }

    /**
     * 构建一个data数据的成功结果
     *
     * @param data 数据
     * @param <T>  泛型类型
     * @return 成功结果
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setData(data);
        return result;
    }

    /**
     * 构建一个以错误码和提示信息的失败结果
     *
     * @param code    错误码
     * @param message 提示信息
     * @param <T>     泛型类型
     * @return 失败结果
     */
    public static <T> Result<T> failure(String code, String message) {
        Result<T> result = new Result<>();
        result.setError(code, message);
        return result;
    }

    /**
     * 构建一个错误枚举的失败结果
     *
     * @param errorCode 错误枚举
     * @param <T>       泛型类型
     * @return 失败结果
     */
    public static <T> Result<T> failure(ErrorCode errorCode) {
        return failure(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 构建一个错误枚举和提示信息的失败结果
     *
     * @param errorCode 错误类型
     * @param message   提示信息
     * @param <T>       泛型类型
     * @return 失败结果
     */
    public static <T> Result<T> failure(ErrorCode errorCode, String message) {
        return failure(errorCode.getCode(), message);
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "Result{" +
                "data=" + data +
                ", success=" + success +
                ", message='" + message + '\'' +
                ", code='" + code + '\'' +
                '}';
    }
}

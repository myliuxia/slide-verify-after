/*
 * Copyright (c) 2019, Ruanfuyi. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.example.vc.demo;

/**
 * 自定义业务异常
 * 
 * @author yaz
 */
public class BusinessException extends RuntimeException {
	private static final long serialVersionUID = 1948676916148047247L;
	private Integer code;
	private Boolean success;

	public BusinessException(Integer code, String message, Boolean success, Throwable cause) {
		super(message, cause);
		this.success = success;
		this.code = code;
	}

	public BusinessException(Integer code, String message, Boolean success) {
		super(message);
		this.code = code;
		this.success = success;
	}
	public BusinessException(String message) {
		super(message);
		this.code = 500;
		this.success = false;
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public Boolean getSuccess() {
		return success;
	}

	public void setSuccess(Boolean success) {
		this.success = success;
	}
}

/*
 * Copyright (c) 2019, Ruanfuyi. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.keyway.captcha.common.exception;

/**
 * 自定义业务异常
 * 
 * @author yaz
 */
public class CaptchaException extends RuntimeException {
	private static final long serialVersionUID = 1948676916148047247L;
	private Integer code;
	private Boolean success;

	public CaptchaException(Integer code, String message, Boolean success, Throwable cause) {
		super(message, cause);
		this.success = success;
		this.code = code;
	}

	public CaptchaException(Integer code, String message, Boolean success) {
		super(message);
		this.code = code;
		this.success = success;
	}
	public CaptchaException(String message) {
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

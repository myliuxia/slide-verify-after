/*
 * Copyright (c) 2019, Ruanfuyi. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.keyway.captcha.common.http;

/**
 * 调用响应消息头
 * 
 * @author yaz
 *
 */
public class ResultHead {
	/**
	 * 消息头
	 */
	private Integer code;
	/**
	 * 消息描述
	 */
	private String message;

	/**
	 * 是否成功
	 */
	private Boolean success;

	public ResultHead(Integer code, String message, Boolean success) {
		super();
		this.code = code;
		this.message = message;
		this.success = success;
	}

	public ResultHead() {
		super();
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Boolean getSuccess() {
		return success;
	}

	public void setSuccess(Boolean success) {
		this.success = success;
	}
}

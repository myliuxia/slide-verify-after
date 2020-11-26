/*
 * Copyright (c) 2019, Ruanfuyi. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.keyway.captcha.common.http;

/**
 * 通用调用结果对象
 * 
 * @author yaz
 *
 */
public class Result {
	/**
	 * 调用识别标志
	 */
	private ResultHead head;
	/**
	 * 调用响应数据
	 */
	private Object data;

	public ResultHead getHead() {
		return head;
	}

	public void setHead(ResultHead head) {
		this.head = head;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

}

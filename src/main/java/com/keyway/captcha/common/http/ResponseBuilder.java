/*
 * Copyright (c) 2019, Ruanfuyi. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.keyway.captcha.common.http;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * 响应构建器
 * 
 * @author yaz
 *
 */
public class ResponseBuilder {
	/**
	 * 构建响应
	 *
	 * @param data 数据
	 * @return 流式构建对象
	 */
	public static Response data(Object data) {
		return new Response() {
			private MultiValueMap<String, String> headers = new HttpHeaders();
			private HttpStatus httpStatus = HttpStatus.OK;
			private Result result = new Result();

			@Override
			public Response header(String key, String value) {
				headers.add(key, value);
				return this;
			}

			@Override
			public Response tip(ResultHead head) {
				result.setHead(head);
				return this;
			}

			@Override
			public Response tip(Integer code, String message,Boolean success) {
				result.setHead(new ResultHead(code, message,success));
				return this;
			}

			@Override
			public ResponseEntity<Result> build() {
				result.setData(data);
				if (result.getHead() == null) {// 如果没有设置提示代码，则和http状态保持一致
					//仅当代码为200时为正常请求
					if(httpStatus.value() ==200){
						result.setHead(new ResultHead(this.httpStatus.value(), this.httpStatus.name(),true));
					}else {
						result.setHead(new ResultHead(this.httpStatus.value(), this.httpStatus.name(),false));
					}

				}
				return new ResponseEntity<>(this.result, this.headers, this.httpStatus);
			}

			@Override
			public Response status(HttpStatus httpStatus) {
				Assert.notNull(httpStatus, "http status is null");
				this.httpStatus = httpStatus;
				return this;
			}

		};
	}

	public interface Response {
		/**
		 * 设置http响应头
		 * 
		 * @param httpStatus http状态码
		 * @return 响应对象
		 */
		Response status(HttpStatus httpStatus);

		/**
		 * 设置http头
		 * 
		 * @param key   header key
		 * @param value header value
		 * @return 响应对象
		 */
		Response header(String key, String value);

		/**
		 * 设置提示
		 * 
		 * @param tip 提示
		 * @return 响应对象
		 */
		Response tip(ResultHead tip);

		/**
		 * 设置提示
		 * 
		 * @param code 代码
		 * @param message 消息
		 * @return 响应对象
		 */
		Response tip(Integer code, String message, Boolean success);

		/**
		 * 构建响应实体
		 * 
		 * @return 响应实体
		 */
		ResponseEntity<Result> build();
	}

}

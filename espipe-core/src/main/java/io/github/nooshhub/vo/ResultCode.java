/*
 * Copyright 2021-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.nooshhub.vo;

/**
 * Result code is for the front-end to identify error types.
 *
 * @author Neal Shan
 * @since 2022/7/30
 */
public enum ResultCode implements StatusCode {

    /**
     * success.
     */
    SUCCESS(10000, "success"),
    /**
     * public error.
     */
    PUBLIC_ERROR(10001, "public error"),
    /**
     * internal error.
     */
    INTERNAL_ERROR(10002, "internal error");

    // status code
    private int code;

    // status code detail
    private String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public int getCode() {
        return this.code;
    }

    @Override
    public String getMsg() {
        return this.msg;
    }

}

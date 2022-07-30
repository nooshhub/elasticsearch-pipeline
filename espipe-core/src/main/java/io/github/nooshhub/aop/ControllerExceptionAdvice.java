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

package io.github.nooshhub.aop;

import io.github.nooshhub.exception.EspipeException;
import io.github.nooshhub.vo.ResultCode;
import io.github.nooshhub.vo.ResultVo;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Wrap exception as ResultVO.
 *
 * @author Neal Shan
 * @since 2022/7/30
 */
@RestControllerAdvice
public class ControllerExceptionAdvice {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResultVo handleIllegalArgumentException(IllegalArgumentException ex) {
        return new ResultVo(ResultCode.PUBLIC_ERROR, ex.getMessage());
    }

    @ExceptionHandler(EspipeException.class)
    public ResultVo handleIllegalArgumentException(EspipeException ex) {
        return new ResultVo(ResultCode.PUBLIC_ERROR, ex.getMessage());
    }

}

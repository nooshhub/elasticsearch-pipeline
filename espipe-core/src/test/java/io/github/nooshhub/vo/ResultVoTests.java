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

import io.github.nooshhub.dao.JdbcDaoTests;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ResultVo}
 *
 * @author Neal Shan
 * @since 2022/8/1
 */
@SpringBootTest
public class ResultVoTests {

    @Test
    public void success() {
        String mock_data = "data";
        ResultVo vo = new ResultVo(mock_data);
        assertThat(vo.getData()).isEqualTo(mock_data);
        assertThat(vo.getCode()).isEqualTo(ResultCode.SUCCESS.getCode());
        assertThat(vo.getMsg()).isEqualTo(ResultCode.SUCCESS.getMsg());
    }

    @Test
    public void publicError() {
        String mock_data = "data";
        ResultVo vo = new ResultVo(ResultCode.PUBLIC_ERROR, mock_data);
        assertThat(vo.getData()).isEqualTo(mock_data);
        assertThat(vo.getCode()).isEqualTo(ResultCode.PUBLIC_ERROR.getCode());
        assertThat(vo.getMsg()).isEqualTo(ResultCode.PUBLIC_ERROR.getMsg());
    }

    @Test
    public void statusWithoutData() {
        ResultVo vo = new ResultVo(ResultCode.INTERNAL_ERROR);
        assertThat(vo.getData()).isNull();
        assertThat(vo.getCode()).isEqualTo(ResultCode.INTERNAL_ERROR.getCode());
        assertThat(vo.getMsg()).isEqualTo(ResultCode.INTERNAL_ERROR.getMsg());
    }

}

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

package io.github.nooshhub;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * IO Utils
 * <p>
 * References: https://howtodoinjava.com/java/io/java-read-file-to-string-examples/
 * https://stackoverflow.com/questions/309424/how-do-i-read-convert-an-inputstream-into-a-string-in-java
 * https://www.baeldung.com/convert-input-stream-to-string
 *
 * @author Neal Shan
 * @since 6/4/2022
 */
public class IOUtils {

	/**
	 * get input stream by file path
	 * @param filePath file path
	 * @return input stream
	 */
	public static InputStream getInputStream(String filePath) {
		if (filePath == null) {
			throw new EspipeException("File is not found by " + filePath);
		}

		InputStream ins = Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath);
		if (ins == null) {
			throw new EspipeException("File is not found by " + filePath);
		}

		return ins;
	}

	/**
	 * get string by file path
	 * @param filePath file path
	 * @return file content as string
	 */
	public static String getContent(String filePath) {
		InputStream ins = getInputStream(filePath);
		return new BufferedReader(new InputStreamReader(ins, StandardCharsets.UTF_8)).lines()
				.collect(Collectors.joining("\n"));
	}

}

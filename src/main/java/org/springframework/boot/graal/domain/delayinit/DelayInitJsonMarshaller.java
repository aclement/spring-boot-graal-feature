/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.graal.domain.delayinit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONObject;

/**
 * Marshaller to write {@link DelayInitDescriptor} as JSON.
 *
 * @author Andy Clement
 */
public class DelayInitJsonMarshaller {

	private static final int BUFFER_SIZE = 4098;

	public void write(DelayInitDescriptor metadata, OutputStream outputStream)
			throws IOException {
		try {
			DelayInitJsonConverter converter = new DelayInitJsonConverter();
			JSONObject jsonObject = converter.toJsonArray(metadata);
			outputStream.write(jsonObject.toString(2).getBytes(StandardCharsets.UTF_8));
		}
		catch (Exception ex) {
			if (ex instanceof IOException) {
				throw (IOException) ex;
			}
			if (ex instanceof RuntimeException) {
				throw (RuntimeException) ex;
			}
			throw new IllegalStateException(ex);
		}
	}
	
	public static DelayInitDescriptor read(String input) throws Exception {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))) {
			return read(bais);
		}
	}

	public static DelayInitDescriptor read(byte[] input) throws Exception {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(input)) {
			return read(bais);
		}
	}

	public static DelayInitDescriptor read(InputStream inputStream) throws Exception {
		DelayInitDescriptor metadata = toDelayInitDescriptor(new JSONObject(toString(inputStream)));
		return metadata;
	}
	
	private static DelayInitDescriptor toDelayInitDescriptor(JSONObject object) throws Exception {
		DelayInitDescriptor rd = new DelayInitDescriptor();
		JSONArray array = object.getJSONArray("delayClassInitialization");
		for (int i=0;i<array.length();i++) {
			rd.add(array.getJSONObject(i).getString("class"));
		}
		return rd;
	}
	
//	private static FieldDescriptor toFieldDescriptor(JSONObject object) throws Exception {
//		String name = object.getString("name");
//		boolean allowWrite = object.optBoolean("allowWrite");
//		return new FieldDescriptor(name,allowWrite);
//	}
//
//	private static MethodDescriptor toMethodDescriptor(JSONObject object) throws Exception {
//		String name = object.getString("name");
//		JSONArray parameterTypes = object.optJSONArray("parameterTypes");
//		List<String> listOfParameterTypes = null;
//		if (parameterTypes != null) {
//			listOfParameterTypes = new ArrayList<>();
//			for (int i=0;i<parameterTypes.length();i++) {
//				listOfParameterTypes.add(parameterTypes.getString(i));
//			}
//		}
//		return new MethodDescriptor(name, listOfParameterTypes);
//	}

	private static String toString(InputStream inputStream) throws IOException {
		StringBuilder out = new StringBuilder();
		InputStreamReader reader = new InputStreamReader(inputStream,
				StandardCharsets.UTF_8);
		char[] buffer = new char[BUFFER_SIZE];
		int bytesRead;
		while ((bytesRead = reader.read(buffer)) != -1) {
			out.append(buffer, 0, bytesRead);
		}
		return out.toString();
	}

}

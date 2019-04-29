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
package org.springframework.boot.graal.support;

import java.io.InputStream;
import java.util.Objects;

import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;
import org.springframework.boot.graal.domain.delayinit.DelayInitDescriptor;
import org.springframework.boot.graal.domain.delayinit.DelayInitJsonMarshaller;

public class DelayInitialization {

	public DelayInitDescriptor compute() {
		try {
			InputStream s = this.getClass().getResourceAsStream("/delayInitialization.json");
			return DelayInitJsonMarshaller.read(s);
		} catch (Exception e) {
			return null;
		}
	}

	public void register(BeforeAnalysisAccess access) {
		DelayInitDescriptor did = compute();
		System.out.println("SBG: delaying initialization of #"+did.getClasses().size()+" classes");
		did.getClasses().stream()
				.map(access::findClassByName).filter(Objects::nonNull)
				.forEach(RuntimeClassInitialization::delayClassInitialization);
	}

}

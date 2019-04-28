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

package org.springframework.boot.graal.support;

import java.io.InputStream;

import org.springframework.boot.graal.domain.delayinit.DelayInitDescriptor;
import org.springframework.boot.graal.domain.delayinit.DelayInitJsonMarshaller;

public class DelayInitialization {

	public void compute() throws Exception {
		InputStream s = this.getClass().getResourceAsStream("/delayInitialization.json");
		System.out.println(s);
		DelayInitDescriptor read = DelayInitJsonMarshaller.read(s);
		System.out.println(read);
	}
}

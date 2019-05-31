package org.springframework.support.graal;

import java.io.File;

import org.junit.Test;

public class TypeSystemTest {

	@Test
	public void test() throws Exception {
		File file = new File("./target/classes");
		System.out.println(file.getCanonicalPath());
	}

}

package org.springframework.support.graal.type;

import java.util.Arrays;

import org.springframework.boot.graal.type.TypeSystem;

public class TypeSystemTest {
	
	public static void main(String[] args) {
		TypeSystem scanner = new TypeSystem(Arrays.asList(
				"/Users/aclement/gits2/spring-boot-graal-processor/test-projects/latestboot/toshare220m2/demo/target/classes",
				"/Users/aclement/.m2/repository/org/springframework/boot/spring-boot-autoconfigure/2.2.0.M2/spring-boot-autoconfigure-2.2.0.M2.jar",
				"/Users/aclement/gits/spring-boot-graal-feature/target/classes",
				"/Users/aclement/.m2/repository/org/springframework/boot/spring-boot/2.1.2.RELEASE/spring-boot-2.1.2.RELEASE.jar",
				"/Users/aclement/.m2/repository/org/springframework/spring-context/5.1.5.RELEASE/spring-context-5.1.5.RELEASE.jar"));
		System.out.println("X");
		System.out.println("@Configuration: \n"
				+ scanner.findTypesAnnotated("Lorg/springframework/context/annotation/Configuration;", true)
						.contains("com/example/demo/DemoApplication"));

		System.out.println("Looking at DemoApplication");
//		AnnotationInfo annotationInfo = scanner.annotatedTypes.get("com/example/demo/DemoApplication");
//		System.out.println(annotationInfo.toAnnotationString());
//		System.out.println("metas:" + annotationInfo.getMetaAnnotations());
	}
}

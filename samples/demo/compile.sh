#!/usr/bin/env bash
mvn clean install

export JAR="demo-0.0.1-SNAPSHOT.jar"
rm demo
printf "Unpacking $JAR"
rm -rf unpack
mkdir unpack
cd unpack
jar -xvf ../target/$JAR >/dev/null 2>&1
cp -R META-INF BOOT-INF/classes

printf "\n\nRemoving a class from spring-boot-2.2.0.M2.jar because of an open graal issue."
# try other options to fix this whilst waiting on fix: add the jar containing that to the build just to get round this? 
# spring-boot-2.2.0.M2.jar 
../adjustConfigurationSpringBoot.sh org.springframework.boot.jdbc.EmbeddedDatabaseConnection

cd BOOT-INF/classes
export LIBPATH=`find ../../BOOT-INF/lib | tr '\n' ':'`
export CP=.:$LIBPATH
# This would run it here... (as an exploded jar)
#java -classpath $CP com.example.demo.DemoApplication

export CP=$CP:/Users/aclement/gits/spring-boot-graal-feature/target/spring-boot-graal-feature-0.5.0.BUILD-SNAPSHOT.jar:/Users/aclement/.m2/repository/org/ow2/asm/asm-tree/7.1/asm-tree-7.1.jar:/Users/aclement/.m2/repository/org/ow2/asm/asm/7.1/asm-7.1.jar

printf "\n\nCompile\n"
#  -Dorg.springframework.boot.logging.LoggingSystem=org.springframework.boot.logging.java.JavaLoggingSystem \
native-image \
  -Dio.netty.noUnsafe=true \
  --no-server -H:Name=demo -H:+ReportExceptionStackTraces \
  --allow-incomplete-classpath --report-unsupported-elements-at-runtime \
  -cp $CP com.example.demo.DemoApplication

mv demo ../../..

printf "\n\nCompiled app (demo)\n"
cd ../../..
time ./demo


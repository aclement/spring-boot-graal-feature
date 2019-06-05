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

cd BOOT-INF/classes
export LIBPATH=`find ../../BOOT-INF/lib | tr '\n' ':'`
export CP=.:$LIBPATH

# This would run it here... (as an exploded jar)
#java -classpath $CP com.example.demo.DemoApplication

# Our feature being on the classpath is what triggers it
export CP=$CP:../../../../../target/spring-boot-graal-feature-0.5.0.BUILD-SNAPSHOT.jar

printf "\n\nCompile\n"
native-image \
  -Dio.netty.noUnsafe=true \
  --no-server \
  -H:Name=demo \
  -H:+ReportExceptionStackTraces \
  --no-fallback \
  --allow-incomplete-classpath \
  --report-unsupported-elements-at-runtime \
  -cp $CP com.example.demo.DemoApplication

# -DremoveUnusedAutoconfig=true \
mv demo ../../..

printf "\n\nCompiled app (demo)\n"
cd ../../..
time ./demo


#!/usr/bin/env bash
mvn -DskipTests clean package

export JAR="orm-0.0.1.BUILD-SNAPSHOT.jar"
rm orm
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
  -H:Name=orm \
  -H:+ReportExceptionStackTraces \
  --no-fallback \
  --allow-incomplete-classpath \
  --report-unsupported-elements-at-runtime \
  --initialize-at-build-time=org.springframework.transaction.annotation.Isolation,org.springframework.transaction.annotation.Propagation,org.springframework.http.HttpStatus \
  -DremoveUnusedAutoconfig=true \
  -cp $CP app.main.SampleApplication

  #--debug-attach \
mv orm ../../..

printf "\n\nCompiled app (demo)\n"
cd ../../..
time ./orm -Dhibernate.dialect=org.hibernate.dialect.H2Dialect


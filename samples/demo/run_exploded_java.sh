
cd unpack/BOOT-INF/classes
export LIBPATH=`find ../../BOOT-INF/lib | tr '\n' ':'`
export CP=.:$LIBPATH

#cp ../../../logging.properties .
echo "Running..."
java -classpath $CP com.example.demo.DemoApplication

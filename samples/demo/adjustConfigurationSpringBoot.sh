# Repack spring-boot-autoconfiguration removing the specified class
export SBA=`find . -name "spring-boot-2.2.0.M2.jar"`
rm -rf repackSBA
mkdir repackSBA
cd repackSBA
jar -xf ../$SBA 

# find . -name "*.class" > ../../list.txt
for i in "$@"
do
  echo "Removing class $i from spring-boot-2.2.0.M2"
  rm `echo $i | sed 's/\./\//g'`*.class
done

rm ../$SBA
jar -cMf ../$SBA .
cd ..
rm -rf repackSBA

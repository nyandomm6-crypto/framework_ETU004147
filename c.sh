mkdir -p bin

javac -cp "lib/*" \
-d bin \
src/main/java/huhu/controler/*.java 

jar cvf framework.jar -C bin .
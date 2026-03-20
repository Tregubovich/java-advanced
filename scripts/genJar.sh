javac \
  -d out \
  -cp ../../java-advanced-2026/artifacts/info.kgeorgiy.java.advanced.implementor.jar:../../java-advanced-2026/artifacts/info.kgeorgiy.java.advanced.implementor.tools.jar \
  ../java-solutions/info/kgeorgiy/ja/tregubovich/implementor/Implementor.java
jar cfm Implementor.jar MANIFEST.MF -C out .
rm -rf out

# NOTE: переменные
# NOTE: encoding

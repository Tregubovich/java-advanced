#!/bin/bash

class_path="../../java-advanced-2026/modules/info.kgeorgiy.java.advanced.implementor"
javadoc \
  -d "../javadoc" \
  -private \
  --class-path "${class_path}:${class_path}.tools" \
  "../../java-advanced/java-solutions/info/kgeorgiy/ja/tregubovich/implementor/Implementor.java"

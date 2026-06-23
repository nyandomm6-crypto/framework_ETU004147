#!/bin/bash

echo "nettoyage..."
rm -rf build
mkdir -p build/classes

echo "compilation..."
javac -cp "lib/*" -d build/classes $(find . -name "*.java")

if [ $? -ne 0 ]; then
    echo "erreur de compilation"
    exit 1
fi

echo "creation du jar..."
jar cf FrameworkSpringMVC.jar -C build/classes .

echo "termine..."
echo "Fichier généré : FrameworkSpringMVC.jar"
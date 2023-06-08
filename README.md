# NGFF-Converter

## License
NGFF-converter is distributed under the terms of the GPL license. Please see LICENSE.txt for further details.

## Build and run:

    ./gradlew clean build
    cd build/distributions
    unzip *.zip
    cd NGFF-Converter-0.1-SNAPSHOT
    ./bin/NGFF-Converter

### Ubuntu
You should install openjdk-17

    sudo apt install openjdk-17-jdk
    
In case you get this error or similar

    Native library (linux-x86-64/libblosc.so) not found in resource path
    
You should also install

    sudo apt-get install libblosc-dev

## Project skeleton created using a combination of:

- https://github.com/openjfx/javafx-maven-archetypes/tree/master/javafx-archetype-fxml
- https://github.com/openjfx/samples/tree/master/CommandLine/Non-modular/Gradle
- https://openjfx.io/openjfx-docs/ (`Runtime Images > Non-Modular project`)


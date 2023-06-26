# NGFF-Converter

## License
NGFF-converter is distributed under the terms of the GPL license. Please see LICENSE.txt for further details.

## Build and run:

    ./gradlew clean build
    cd build/distributions
    unzip *.zip
    cd NGFF-Converter-0.1-SNAPSHOT
    ./bin/NGFF-Converter

### Dependencies
NGFF-Converter has the following requirements/dependencies:
- Java 16+
- JavaFX
- [bioformats2raw](https://github.com/glencoesoftware/bioformats2raw)
- [raw2ometiff](https://github.com/glencoesoftware/raw2ometiff)
- [blosc](https://github.com/Blosc/c-blosc)

See `build.gradle` for more precise versioning.

### Windows & MacOS
Download prebuilt, signed binaries for these platforms [here](https://www.glencoesoftware.com/products/ngff-converter/).

### Ubuntu
Running from source is possible via the following steps:

Install openjdk-17

    sudo apt install openjdk-17-jdk
    
Install the blosc dependency

    sudo apt-get install libblosc-dev

Clone and run the repo

## Project skeleton created using a combination of:

- https://github.com/openjfx/javafx-maven-archetypes/tree/master/javafx-archetype-fxml
- https://github.com/openjfx/samples/tree/master/CommandLine/Non-modular/Gradle
- https://openjfx.io/openjfx-docs/ (`Runtime Images > Non-Modular project`)


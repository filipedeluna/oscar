# OSCAR Noise Injector - Prototype

### Requirements (Tested on)

- Apache Maven 3.6.3
- OpenJDK 17.0.2 (Project compiles to Java 9)

### Running Instructions

```sh
mvn clean
mvn compile
mvn exec:java -Dexec.mainClass=oscar.Main -Dexec.args="-h"
```

##### OSCAR arguments

```
oscar <targetfile> <mainclass> <outputdirectory>
```

##### Instrumented program arguments

```
Usage:
        java [java_options] <mainclass> [oscar_options]
                (to execute a class)
        or: java -jar <mainclass> [oscar_options]
                (to execute a jar file)

OSCAR options include:
        -a --args                       String                          Inject arguments into the program
        -c --config_file                String                          Set config file location to load
        -co --console-output            Flag            False           Enable output of noising locations signals to console
        -fo --file-output               Flag            False           Enable output of noising locations signals to a file
        -lfo --lazy-file-output         Flag            False           Enable lazy output of noising locations signals to a file
        -M --max_sleep_length           Long            0               Set maximum sleep length
        -m --min_sleep_length           Long            400             Set minimum sleep length
        -d --disable-noise              Flag            False           Disable all noise
        -np --noise-placements          List<String>    All             Set the list of active noise placements.
        -nc --noise-categories          List<String>    All             Set the list of active noise categories.
        -pnp --print-noise-placements   Flag            -               Print all possible noise placements.
        -v --version                    Flag            -               Print OSCAR version.
        -vb --verbose                   Flag            False           Enable full logging.
        -q --quiet                      Flag            False           Disable logging.
        -h --help                       Flag            False           Print Help.

OSCAR Noise Injector 2022
```

###### Note:

- Sleep lengths should be integers
- Injected arguments need to be inside quotes as such: "-a -b -c"

### Examples:

##### Account

1. Compile the Account program source code

```sh
cd data/cflash/account
javac *.java
cd ../../..
```

2. Use OSCAR to instrument the Account program's bytecode with the noising engine's
   logic. OSCAR will wrap the original program in its routine. The outputted program will be in 
   the folder "output".

```sh
mvn exec:java -Dexec.mainClass=oscar.Main -Dexec.args="data/cflash/account/Main.class Main 
output"
```

3. Run the instrumented program without any noise
```sh
cd output
java Main -d
```

4. Run the instrumented program with random noise between 0 and 100ms
```sh
java Main -m 0 -M 100
```

5. Run the instrumented program, only noising synchronized blocks and methods, and lazily output 
   the trace to a file.

```sh
java Main -m 0 -M 100 -nc sb -lfo
```

6. Run the instrumented program, with specific arguments.

```sh
java Main -a "5"
```
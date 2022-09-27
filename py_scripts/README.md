# OSCAR Noise Injector - Testscript

### Requirements (Tested on)

- Python 3.1.0
- Python Libraries
  - os
  - shutil
  - subprocess
  - Levenshtein 
  - numpy 
  - pathlib
  - tqdm 
- python-pip 21.2.3
- python-venv

### Running Instructions

```sh
python3 -m venv venv
source venv/bin/activate
pip install python-levenshtein numpy pathlib

```

##### Testscript arguments

```
testscript [-h] [-c COUNT] [-j] [-dt] program_dir executable program_args
```

###### Note:

program_args must be a single string for multiple args such as "-a 5 -b 20"

##### Instrumented program arguments

```
usage: testscript [-h] [-c COUNT] [-j] [-dt] program_dir executable program_args

Automate OSCAR's noise injection routine.

positional arguments:
  program_dir           location of program to run.
  executable            Name of program's main class to run or jar file name.
  program_args          Arguments to be passed to program.

options:
  -h, --help            show this help message and exit
  -c COUNT, --count COUNT
                        Number of times to run program.
  -j, --jar             Run program as a jar.
  -dt, --disable_thread_ids
                        Disable thread ID parsing.

Run with argument -h for help.
```

### Examples:

##### Account

1. Run the generated Account program jar 10 times with 3 threads and no noise

```sh
python3 testscript.py ../output oscar_out.jar "-a 3 -d" -c 10 -j
```

2. Run the generated Account program jar 20 times with no arguments and disable thread id parsing

```sh
python3 testscript.py ../output oscar_out.jar -c 20 -j -dt
```

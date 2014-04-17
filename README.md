# tracefp - Stacktrace fingerprinting for Java

Ok, you found a site with a sloppy configuration which shows you a stack
trace... now what? How to find out which libraries are used here (so you
can look for specific vulnerabilities)?

tracefp is an effort do determine the library version(s) used by
matching the information of the stack trace with the line numbers of the
actual code.


# Manual dependencies

## BytecodeParser

Clone BytecodeParser from https://github.com/sgodbillon/BytecodeParser.git and run

    mvn source:jar install


# Build IDE projects

## IntelliJ IDEA

Use the sbt command

    gen-idea no-sbt-build-module

to generate the project files. Not necessary if you use a recent version
of IntelliJ.

# Usage

## Scan libraries

Call

    tracefp scanlib *group* *artifact* [*version*]

to get the call trace of a library. If *version* is omitted, all
versions will be scanned.


Call

    tracefp analyze *stacktracefile*

to parse a stacktrace and try to match the libraries' fingerprints.


# License

This program is written by Stefan Schlott. It is published under the
GPL-2 license.


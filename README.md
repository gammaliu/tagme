TAGME V1.1 - HOW TO
===================

COPYRIGHT
=========

   Copyright 2014 - A3 lab (Dipartimento di Informatica, Università di Pisa)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

Please note that some of TAGME's dependencies, are licensed under the terms of
the GPLv3 or LGPLv3 licenses. Please check license and copying information for
all dependency libraries (see below).


AUTHORS' NOTE
=============

Even if the live demo of TAGME that is available at
https://services.d4science.org/web/tagme/documentation
has been queried more than 400 millions of times since its introduction in
2011, and has been able to handle thousands of queries per minute without any
issue (hence proving a certain stability), this code has still to be considered
an academic prototype: it is the result of several refinement iterations and
multiple researchers have put their hands on the code with a "trial and error"
approach.
  
For this reason, it is not meant be used in production environments or 
critical applications. As stated in the license, the software is distributed 
as is, without warranties or conditions of any kind.

If you are using this software for your researches and you are going to publish
results based on TAGME annotation process, please add this citation to your 
paper:

> [Paolo Ferragina](http://pages.di.unipi.it/ferragina/), Ugo Scaiella:
> [Fast and Accurate Annotation of Short Texts with Wikipedia Pages](http://ieeexplore.ieee.org/document/6035657/).
> IEEE Software 29(1): 70-75 (2012)

For more information on TagMe and some of its applications, please visit:
http://acube.di.unipi.it/tagme/

REQUIREMENTS
============

TAGME requires Java 6 to compile, run and process Wikipedia data. Apache Ant
tool ( http://ant.apache.org/ ) is required to build the code, download and
to process Wikipedia data.

Minimum RAM required to run TAGME is about 2 gigabytes. More resources are
required to index Wikipedia data. See below for further details.

DEPENDENCIES
============

The following is the directory structure required to build the code:

    ./
      src/
      lib/
      ext_lib/
      preproc_lib/

`./src/` directory contains TAGME's source files, provided within this package.

The following artifacts are required to build and run TAGME. Standard Maven
notation has been used to identify them: `<groupId>:<artifactId>:<version>`.
You can download those libraries from http://search.maven.org, or use the
following ant task

    $ ant get-deps

The directory `./lib/` must contain all libraries required to compile and run
TAGME:

    com.martiansoftware:jsap:2.1.jar
    commons-beanutils:commons-beanutils:1.8.3
    commons-codec:commons-codec:1.5
    commons-collections:commons-collections:3.2.1
    commons-configuration:commons-configuration:1.7
    commons-io:commons-io:2.0.1
    commons-lang:commons-lang:2.6
    commons-logging:commons-logging:1.1.1
    it.unimi.dsi:dsiutils:2.0.4
    it.unimi.dsi:fastutil:6.4.1
    it.unimi.dsi:sux4j:3.0.2
    it.unimi.dsi:webgraph:3.0.4
    org.apache.commons:commons-digester3:3.0
    org.apache.lucene:lucene-core:3.4.0
    org.json:json:20131018
    log4j:log4j:1.2.16
    snowball (provided within the package)

The directory `./ext_lib/` must contain all libraries required to compile TAGME,
but are not required when running it:

    org.apache.tomcat:catalina:6.0.37
    javax.servlet:servlet-api:2.4

The directory `./preproc_lib/` must contain all libraries required to during
pre-processing of Wikipedia data:

    javax.mail:mailapi:1.4.3
    com.sun.mail:smtp:1.4.4

BUILDING
========

Ant build file is provided within the package. You can run the command from the
base directory

    $ ant jar

to build TAGME. A jar file named `./tagme.jar` will be created inside the base
directory.

CONFIGURATION
=============

The configuration file has to be provided using JVM system properties from
command line

    -Dtagme.config=<path_to_config_file>

A sample configuration is provided within this package, look at the file
`./config.sample.xml`. Also, the file `./config.template.xml` contains a model
of the configuration that can be used as a reference like an XML DTD.

Finally, a log4j configuration file is provided, look at `./log4j.xml`.

FAST VS LIGHT MODE
==================

TAGME supports for two execution modes: the 'fast' one that pre-load several
data into memory and needs for several GBs of heap space, and the 'light' one
that requires less memory but is also slower.

In order to run TAGME in fast mode, two parameters must be set as follow (using
XPath-like notation):

    /tagme/settings(parsing)/data = TERNARY_TRIE
    /tagme/settings(annotation)/relatedness = MATRIX

Using the above settings, you need for approximatively 16G of RAM to use English
Wikipedia and 6G of RAM to use Italian. The JVM Heap Space has to be set
accordingly, using JVM properties. Eg, to use both Italian and English (about
24G ) you must include this to java command line: `-Xmx24G`. Alternatively, you
can reduce the memory consumption, removing those two settings. In this case,
2G of RAM are enough to run both Italian and English. Obviously, annotation
process will be less faster.

RUNNING
=======

Before running TAGME you have to process Wikipedia sources in order to create
data that is needed at runtime. This process may take several hours and it is
detailed in the next sections.

Once the data is available, you can run TAGME. First of all, the initialization
process has to be executed, by calling the method

    it.acubelab.tagme.config.TagmeConfig.init();

This will read the configuration, set the logging (logging framework is Log4j)
and load data structures.

Main class for annotating texts is `it.acubelab.tagme.wrapper.Annotator`. The
constructor accepts a String identifying a language code (can be `"it"` or `"en"`)
and provides few methods to get annotations from a text. Namely the method

    List<Annotation> getAnnotationList(String to_annot)

can be used to annotate the string `"to_annot"`. A list of Annotation objects is
returned.

Check the source code and JavaDoc of `it.acubelab.tagme.wrapper.Annotator` class
for further details.

CODE SAMPLES
============

A couple of code samples are provided within this package in the `samples` 
folder:

    ./
      samples/
        Example1.java
        Example2.java

Both classes contain a simple main method that can be used to understand 
the main TAGME's objects, how to access data structures, how to annotate
texts and get the results.

You can compile them providing all dependencies and TAGME classes in the
classpath of java compiler (you must first compile TAGME using ant script as
detailed above)

    $ javac -cp lib/*:ext_lib/*:bin/ samples/Example1.java

then you can run it using:

    $ java -cp lib/*:ext_lib/*:bin/:samples/ \
            -Xmx16G -Dtagme.config=<path to tagme config> \
            Example1

It may take some time to load into memory all required data, based on the
configuration you have selected (see details above).

TAGME'S REPOSITORY
==================

TAGME requires several pre-processed data structures for annotating. Those
datasets are build from Wikipedia source files (see below) and are stored within
a directory that is called TAGME repository. The absolute path of this
directory has to be specified in the TAGME's configuration file. See the
configuration sample for further details.

STOPWORD REPOSITORY
===================

A set of files containing stopword lists is provided within this package (look
at `./stopwords/` directory). The directory containing this set of file is the
stopword repository and the absolute path has to be specified in the TAGME's
configuration file. See the configuration sample for further details.

INDEXING
========

TAGME repository can be built from Wikipedia dumps provided by the Wikimedia
Foundation at http://dumps.wikimedia.org/ . Additionally, information about
article categories are extracted from a DBpedia dataset, that can be found at
http://downloads.dbpedia.org/

TAGME repository has the following structure:

    <repository root>/
      it/
        source/
        ...
      en/
        source/
        ...
      wikipatterns.properties

The `wikipatterns.properties` file is the one that is provided within this package
and must be copied in the base directory of the repository.

An Ant task can be used to download all required datasets from Wikipedia and
DBpedia:

    $ ant get-source -Dlang=... -Ddd=... -Ddbpedia=... -Dtargetdir=...

where:

* `lang` can be `it` (Italian) or `en` (English)
* `dd` is the version of the Wikipedia dump in the format YYYYMMDD (the date of
  the snapshot). See http://dumps.wikimedia.org/backup-index.html for further
  details.
* `dbpedia` is the version of DBpedia, in the format `X.Y`. See
  http://downloads.dbpedia.org/ for additional details.
* `targetdir` is the directory where files will be stored, ie `<repository
  root>/it/source` for Italian or `<repository root>/en/source` for English.
  
This task downloads and extracts Wikipedia and DBpedia data. Note that for
English Wikipedia, this requires about 90G of disk space. Additionally, 
the process generates several datasets and to complete the indexing you should
need for about 180 GB.

When all data has been downloaded, another Ant task can be executed to index
Wikipedia/DBpedia data.

    $ ant index.all -Dconfig.file=... -Dmem=... -Dmailto=... -Dlang=

where:

* `lang` can be `it` (Italian) or `en` (English)
* `config.file` is the absolute path to the TAGME config file, where the
  repository path, log4j configuration file path and other parameters are
  specified
* `mem` is the amount of JVM heap space to allocate for the process (basically
  you need for the same amount of memory that is required to run TAGME)
* `mailto` (optional) the email address where a notification of the end of the
  process will be sent. An SMTP server must be installed in the machine. 

This task creates all data structures, also the ones used in fast mode, so the
task itself requires a lot of memory (see above). If you need to generate data
just to run in 'light mode', you can execute this Ant task:

    $ ant index.light -Dconfig.file=... -Dmem=... -Dmailto=... -Dlang=

Indexing may take several hours (about 40 hours for English wikipedia), so it
is recommended running it with a tool like `screen` or `tmux`.

If you are using the log4j configuration file attached to this package, the 
output of the process is redirect to the standard output, that Ant redirects
to a file that will be create for each task run. You can find this file in 
`./logs/` directory. Ant task takes care to generate a unique a file name for
each task run.

Disclaimer by Aurélien Géron
============================

I am not the original author of this project. I contacted
[Paolo Ferragina](http://pages.di.unipi.it/ferragina/), who provided me with
this code under the Apache 2.0 License, and kindly authorized me to publish it
on GitHub. I made a few minor modifications before the first commit:

* Renamed `LICENSE.txt` to `LICENSE`, and `README.txt` to `README.md`, and updated `build.xml` accordingly.
* Made purely cosmetic changes to this `README.md` file and added this final section.
* Added the `.gitignore` file.

Feel free to clone & submit pull requests.

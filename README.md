Topic modeling with MALLET (with a little help)
===============================================

This project wraps [MALLET](http://mallet.cs.umass.edu/)
(a machine learning toolkit written in [Java](www.java.com/)) with some simplified interfaces
and utilities written in [Scala](http://www.scala-lang.org/), a programming
language that runs on the Java Virtual Machine.
It also uses the [Apache POI](http://poi.apache.org/) library to export
MALLET topic model data to Excel spreadsheets.

Installing Maven
------------------

This project uses [Apache Maven](http://maven.apache.org/)
as a build and dependency management system. Once you have Java and Maven
installed, Maven will take care of downloading all other necessary libraries
(including MALLET itself) from the
[Maven Central Repository](http://search.maven.org/).

If you're on a Mac, you already have Maven installed—you can open a terminal
and type the following to confirm this:

    mvn -version

Note that if you're running Lion, you may need to install Java first, but
that's a one-liner—just type:

    java

Into a terminal and follow the directions.

Most Linux distributions include Maven in their package manager—if you're on
Linux it's worth searching your package manager before installing it from
scratch.

Installing Maven on Windows is a little more complicated, but there are a
number of resources that can help, starting with
[the official Maven documentation](http://maven.apache.org/guides/getting-started/windows-prerequisites.html).

Installing this project
-----------------------

Next you need to grab this repository. You can download
[a zip archive](https://github.com/umd-mith/topic-modeling/archive/master.zip)
of these files, or check out the repository, if you have
[Git](http://git-scm.com/) installed on your machine:

    git clone https://github.com/umd-mith/topic-modeling.git

Or if you have your own [GitHub](https://github.com/) account, you can fork
this repository and then check out your fork.

That's all the setup you need to do.

Project files
-------------

The Scala source tree is in the `src/main/scala` directory. Other resources
are in the `src/main/resources` directory (which currently only contains a
copy of MALLET's English-language stopword list, as a convenience).

There are two main Scala packages:

 * `edu.umd.mith.topic.mallet` supports working with MALLET models.
 * `edu.umd.mith.topic.io` supports export to Excel spreadsheets.

By default models are written to the `models` directory and spreadsheets to
the `results` directory. There's some public domain example data from the
[HathiTrust Digital Library](http://www.hathitrust.org/)
in the `example` directory (a selection of nine nineteenth-century publications
on either music or homeopathy).

Running the tools
-----------------

The simplest thing you can do with this software is train a topic model on a
data set. First you need to get your texts into one of the two formats
specified [here](http://mallet.cs.umass.edu/import.php). The directory format
is the simpler of the two: you just need to have a single directory that
contains directories (probably corresponding to something like volumes) that
contains plain text files that will be treated as your documents.
The example data from the HathiTrust illustrates this layout, with pages as
documents.

To train the topic model, you can run the following, for example, from the `topic-modeling`
directory that you either just downloaded or cloned:

    mvn compile exec:java \
      -Dexec.mainClass="edu.umd.mith.topic.mallet.Trainer" \
      -Dexec.args="homeopathy-and-music example/data 40"

The last line should contain three space-separated arguments:

  1. An identifier (no spaces) for the experiment: `homeopathy-and-music`.
  2. The path to the directory or file in the MALLET import format: `example/data`.
  3. The number of topics: `40`.

The first time you run this command it will download all of the necessary
dependencies, including MALLET and Apache POI. These will be cached locally,
so this step generally won't need to be repeated in the future. Maven will
then compile the project's Scala code if necessary, train the topic model,
and save the model and a spreadsheet to files in the output directories:

    models/homeopathy-and-music-2012-12-18-122917.model
    results/homeopathy-and-music-2012-12-18-122917.xlsx

Where the file name is the experiment identifier provided above, with a timestamp.

By default the generated spreadsheet includes four worksheets:

 1. A list of documents with their topic distributions.
 2. A list of the words associated with each topic.
 3. A list of the probabilities matching each word in the preceding sheet.
 4. A list of the most similar document-document pairs, using the symmetrized [Kullback-Leibler divergence](http://en.wikipedia.org/wiki/Kullback%E2%80%93Leibler_divergence) of the documents' topic distributions.

If you've already installed and run MALLET yourself, you may find it more
useful to export your MALLET models to a spreadsheet in this format.
To do this you can run the following command:

    mvn compile exec:java \
      -Dexec.mainClass="edu.umd.mith.topic.io.CreateSpreadsheet" \
      -Dexec.args="example.model example.xslx"

Here the first argument in the last line is your existing model (`example.model`),
and the second is the filename that will be used for the generated spreadsheet (`example.xslx`).

Digging into the code
---------------------

There are a number of places where the code can be adapted relatively easily.
The training operation in the previous section is driven by this file, for example:

    src/main/scala/edu/umd/mith/topic/mallet/training.scala

There are a number of options specified in this file (random seed, hyperparameter estimation, etc.)
that can be edited in a straightforward way.

The following file also contains an example of how one can work programmatically with
instances of MALLET's
[ParallelTopicModel](http://mallet.cs.umass.edu/api/cc/mallet/topics/ParallelTopicModel.html):

    src/main/scala/edu/umd/mith/topic/mallet/model.scala

Which can be useful if you want to have access to information that the MALLET
command line tools don't expose.


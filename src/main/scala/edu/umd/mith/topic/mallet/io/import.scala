/*
 * #%L
 * MITH Topic Modeling Utilities
 * %%
 * Copyright (C) 2011 - 2012 Maryland Institute for Technology in the Humanities
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package edu.umd.mith.topic.mallet.io

import java.io._
import java.nio.charset.Charset
import java.util.regex.Pattern

import cc.mallet.pipe._
import cc.mallet.pipe.iterator._
import cc.mallet.types._
import cc.mallet.util._

import scala.collection.JavaConverters._

/** A factory for data sets.
  *
  * Responsible for determining whether a path is a file or a directory.
  */
object DataSet {
  def apply(path: String) = {
    val f = new File(path)
    if (f.isDirectory) new DirectoryDataSet(f) else new FileDataSet(f)
  }
}

/** Shared data set functionality (files and directories). */
trait DataSet {
  val encoding = Charset.defaultCharset.displayName
  val tokenPattern = Pattern.compile(CharSequenceLexer.LEX_ALPHA.toString)

  val stopwordFilter = new TokenSequenceRemoveStopwords(
    new File(this.getClass.getResource("/stopwords.txt").toURI),
    encoding, false, false, false
  )

  /** Any input mode-specific pipes, if necessary. */
  def inputPipes: Seq[Pipe]

  def pipes = new Target2Label +:
    (inputPipes ++
    Seq(
    new CharSequenceRemoveHTML,
    new CharSequence2TokenSequence(tokenPattern),
    new TokenSequenceLowercase,
    stopwordFilter,
    new TokenSequence2FeatureSequence
  ))

  val instances = new InstanceList(new SerialPipes(pipes.asJava))

  def getInstances(): InstanceList
}

/** An implementation for files. */ 
class FileDataSet(file: File) extends DataSet {
  def inputPipes = Seq.empty[Pipe]

  val reader = new BufferedReader(
    new InputStreamReader(new FileInputStream(file), encoding)
  )

  val linePattern = Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$")

  def getInstances() = {
    instances.addThruPipe(new CsvIterator(reader, linePattern, 3, 2, 1))
    reader.close()
    instances
  }
}

/** And one for directories. */
class DirectoryDataSet(dir: File) extends DataSet {
  def inputPipes = Seq(
    new SaveDataInSource,
    new Input2CharSequence(encoding)
  )

  def getInstances() = {
    instances.addThruPipe(
      new FileIterator(
        dir.listFiles.filter(_.isDirectory).sorted,
        FileIterator.STARTING_DIRECTORIES,
        true
      )
    )
    instances
  }
}


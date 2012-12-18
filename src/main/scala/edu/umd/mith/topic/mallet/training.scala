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
package edu.umd.mith.topic.mallet

import cc.mallet.topics._
import edu.umd.mith.topic.mallet.io._
import java.io._

/** A simple example of how to run MALLET with some reasonable defaults. */
case class Trainer(path: String, name: String, numTopics: Int) {
  /** Alpha parameter: Smoothing over topic distribution. */
  val alpha = 50.0

  /** Beta parameter: Smoothing over unigram distribution. */
  val beta = 0.01

  /** Random seed for the Gibbs sampler.
    *
    * If None, uses system time. Set to e.g. Some(1) to specify a fixed seed.
    */
  val seed: Option[Int] = None

  /** We initialize the model. */
  val model = new ParallelTopicModel(numTopics, alpha, beta)

  /** And the seed. */
  seed.foreach(model.setRandomSeed)

  /** And add the data. */
  model.addInstances(DataSet(path).getInstances())

  /** Change if you have (and want to use) multiple processors. */
  model.setNumThreads(1)

  /** Some reasonable defaults. */
  model.setNumIterations(1000)
  model.setOptimizeInterval(10)
  model.setBurninPeriod(200)
  model.setSymmetricAlpha(false)

  /** And finally we can actually run the training. */
  model.estimate()

  /** Next we put together a timestamp and name for our output files. */
  val dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd-HHmmss")

  val withTime = "%s-%s".format(
    name,
    dateFormat.format(new java.util.Date())
  )

  /* And our file paths. */
  val modelName = "models%c%s.model".format(File.separatorChar, withTime)
  val spreadsheetName = "results%c%s.xlsx".format(File.separatorChar, withTime)

  /** First we write the model. */
  val outputStream = new ObjectOutputStream(new FileOutputStream(modelName))
  outputStream.writeObject(model)
  outputStream.close()

  /** And then the spreadsheet. */ 
  import edu.umd.mith.topic.io._

  /** We compose the individual sheets we want. */
  val spreadsheet = Spreadsheet(
    DocTopicSheet,
    TopicWordFormSheet,
    TopicWordProbSheet,
    DocDocEdgesSheet()
  )

  /** Fill them with the data from the model. */
  spreadsheet.fill(new MalletModel(model, 0.1))

  /** And write them to disk. */
  spreadsheet.write(new File(spreadsheetName))
}

/** The driver that lets us pass in arguments from the command line. */
object Trainer extends App {
  val trainer = Trainer(args(1), args(0), args(2).toInt)
}


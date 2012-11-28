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
package edu.umd.mith.topic

import com.google.common.collect.MinMaxPriorityQueue
import edu.umd.mith.topic.util._
import scala.collection.SortedMap

trait Document {
  def id: String
  def tokens: IndexedSeq[String]
}

/** Represents a model trained on a corpus of documents. */
trait Model {
  trait AssignedDocument extends Document {
    def tokenTopics: IndexedSeq[Int]
    def topics: IndexedSeq[Double]
  }

  trait TopicWord {
    def count: Int
    def prob: Double
    def smoothed: Double
  }

  /** The word types seen in the corpus, in an unspecified order. */
  def vocabulary: IndexedSeq[String]

  /** An arbitrarily ordered list of topics.
    *
    * Note that topics represented here as mappings from word types to a
    * count and probablity.
    */
  def topics: IndexedSeq[Map[String, TopicWord]]

  /** An ordered mapping from document identifiers to topic assignments.
    *
    * Note that the ordering is alphabetical, and may not reflect the original
    * ordering in the corpus.
    */
  def documents: SortedMap[String, AssignedDocument]

  /** Total word token counts. */
  lazy val wordCounts: Map[String, Int] = {
    import scala.collection.mutable.Map
    val totals = Map.empty[String, Int].withDefaultValue(0)
    this.topics.foreach(_.foreach {
      case (t, w) => totals(t) += w.count
    })
    totals.toMap
  }

  /** A sorted list of words that occur at least some number of times. */
  def topWords(cutoff: Int): IndexedSeq[(String, Int)] =
    this.wordCounts.filter(p => p._2 >= cutoff).toIndexedSeq.sortBy(-_._2)

  /** Topics represented as vectors */ 
  def topicVectors(cutoff: Int): IndexedSeq[IndexedSeq[Double]] = {
    val words = this.topWords(cutoff).map(_._1)
    val madeCutoff = words.toSet
    this.topics.map { topic =>
      val counts = topic.filter(
        p => madeCutoff(p._1)
      ).mapValues(_.count).withDefaultValue(0)
      val total = counts.map(_._2).sum.toDouble
      words.map(counts(_) / total)
    }
  }

  /** A table listing symmetrized KL-divergence between pairs of topics. */
  def topicTopicTable =
    IndexedSeq.tabulate(this.topics.size, this.topics.size) {
      (i, j) => skld(this.vocabulary.map {
        word => (this.topics(i)(word).smoothed, this.topics(j)(word).smoothed)
      })
    }

  /** A "table" listing sym. KL-divergence between pairs of documents. */ 
  def docDocTable: Iterator[((String, String), Double)] =
    this.documents.iterator.zipWithIndex.flatMap { case ((xi, xd), i) =>
      this.documents.iterator.drop(i + 1).map { case (yi, yd) =>
        (xi, yi) -> skld(xd.topics zip yd.topics) 
      }
    }

  /** A version of the document-document table that only includes a fixed
    * number of the strongest document-document links.
    */
  def docDocTableFixed(size: Int): IndexedSeq[((String, String), Double)] = {
    val queue = MinMaxPriorityQueue.orderedBy(
      Ordering.by((p: ((String, String), Double)) => p._2)
    ).maximumSize(size).create[((String, String), Double)]

    this.docDocTable.foreach(i => queue.add(i))
    IndexedSeq.fill(queue.size)(queue.pollFirst)
  }
}

/** Simple case class implementations of helper traits. */
trait ModelDefaults { self: Model =>
  case class SimpleAssignedDocument(
    id: String,
    tokens: IndexedSeq[String],
    tokenTopics: IndexedSeq[Int],
    topics: IndexedSeq[Double]
  ) extends AssignedDocument

  case class SimpleTopicWord(
    count: Int,
    prob: Double,
    smoothed: Double
  ) extends TopicWord
}


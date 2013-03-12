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

import cc.mallet.types.FeatureSequence
import cc.mallet.topics.ParallelTopicModel
import edu.umd.mith.topic._
import edu.umd.mith.topic.util._
import java.io.File
import scala.collection.JavaConversions._
import scala.collection.SortedMap

/** An implementation of our Model trait for MALLET models. */
class MalletModel(val model: ParallelTopicModel, val delta: Double)
  extends Model with ModelDefaults {
  def this(file: File, delta: Double) = this(ParallelTopicModel.read(file), delta)
  def this(file: File) = this(file, 1.0)

  val vocabulary: IndexedSeq[String] =
    this.model.getAlphabet.toArray.map(_.asInstanceOf[String]).toIndexedSeq

  lazy val topics: IndexedSeq[Map[String, TopicWord]] =
    this.model.getSortedWords.map { topic =>
      val words = topic.iterator.map { word =>
        this.vocabulary(word.getID) -> word.getWeight.round.toInt
      }.toSeq

      val total = words.map(_._2).sum.toDouble
      val smoothedTotal = total + (this.vocabulary.size * this.delta)
      val default = SimpleTopicWord(0, 0.0, this.delta / smoothedTotal)

      words.map { case (word, count) =>
        word -> SimpleTopicWord(
          count, count / total, (count + this.delta) / smoothedTotal
        )
      }.toMap.withDefaultValue(default)
    }.toIndexedSeq

  lazy val documents: SortedMap[String, AssignedDocument] =
    this.model.getData.zipWithIndex.foldLeft(
      SortedMap.empty[String, AssignedDocument]
    ) { case (map, (document, i)) =>
      val id = document.instance.getName.toString
      val label = document.instance.getLabeling.toString
      val topicAssignments = document.topicSequence.getFeatures
      val tokenIds =
        document.instance.getData.asInstanceOf[FeatureSequence].getFeatures

      map.updated(
        id, 
        SimpleAssignedDocument(
          id,
          label,
          tokenIds.map(this.vocabulary),
          topicAssignments.toIndexedSeq,
          this.model.getTopicProbabilities(document.topicSequence)
        )
      )
    }
}


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
package edu.umd.mith.topic.io

import java.io.{ File, FileOutputStream }
import org.apache.poi.ss.usermodel.{ Cell, Row, Sheet => PoiSheet, Workbook }
import scala.collection.JavaConverters._

import edu.umd.mith.topic.Model

abstract class Sheet(val title: String) {
  def fill(sheet: PoiSheet, model: Model)
}

object DocTopicSheet extends Sheet("Document topic dists") {
  def fill(sheet: PoiSheet, model: Model) {
    val header = sheet.createRow(0)
    header.createCell(0).setCellValue("Document identifier")
    
    (0 until model.topics.size).foreach { i =>
      header.createCell(i + 1).setCellValue("topic-%02d".format(i))
    }

    model.documents.iterator.zipWithIndex.foreach {
      case ((document, probs), i) =>
        val row = sheet.createRow(i + 1)
        row.createCell(0).setCellValue(document)
        probs.topics.zipWithIndex.foreach { case (prob, j) =>
          row.createCell(j + 1).setCellValue(prob)
        }
    }

    sheet.setColumnWidth(0, 256 * 24)
  }
}

object TopicWordFormSheet extends Sheet("Topic word dists (forms)") {
  def fill(sheet: PoiSheet, model: Model) {
    model.topics.zipWithIndex.foreach { case (topic, i) =>
      val row = sheet.createRow(i)

      row.createCell(0).setCellValue("topic-%02d".format(i))

      topic.toSeq.sortBy(-_._2.count).zipWithIndex.foreach {
        case ((word, _), j) => row.createCell(j + 1).setCellValue(word)
      } 
    }
  }
}

object TopicWordProbSheet extends Sheet("Topic word dists (probs)") {
  def fill(sheet: PoiSheet, model: Model) {
    model.topics.zipWithIndex.foreach { case (topic, i) =>
      val row = sheet.createRow(i)

      row.createCell(0).setCellValue("topic-%02d".format(i))

      topic.toSeq.sortBy(-_._2.count).zipWithIndex.foreach {
        case ((_, count), j) => row.createCell(j + 1).setCellValue(count.prob)
      } 
    }
  }
}

case class DocDocEdgesSheet(size: Int = 2048)
  extends Sheet("Document-document edges") {
  def fill(sheet: PoiSheet, model: Model) {
    val header = sheet.createRow(0)
    header.createCell(0).setCellValue("First document ID")
    header.createCell(1).setCellValue("Second document ID")
    header.createCell(2).setCellValue("Symmetrized KL-divergence")

    model.docDocTableFixed(size).zipWithIndex.foreach {
      case (((xi, yi), p), k) =>
        val row = sheet.createRow(k + 1)
        row.createCell(0).setCellValue(xi)
        row.createCell(1).setCellValue(yi)
        row.createCell(2).setCellValue(p)
    }
  }
}

case class DocTopicEdgesSheet(threshhold: Double = 0.1)
  extends Sheet("Document-topic edges") {
  def fill(sheet: PoiSheet, model: Model) {
    val header = sheet.createRow(0)
    header.createCell(0).setCellValue("Document ID")
    header.createCell(1).setCellValue("Topic ID")
    header.createCell(2).setCellValue("Proportion")
    
    model.documents.iterator.flatMap { case (di, assignment) =>
      assignment.topics.zipWithIndex.filter(_._1 >= threshhold).map {
        case (p, j) => (p, (di, j))
      }
    }.filter(_._1 >= threshhold).zipWithIndex.foreach {
      case ((p, (di, j)), k) =>
        val row = sheet.createRow(k + 1)
        row.createCell(0).setCellValue(di)
        row.createCell(1).setCellValue(j)
        row.createCell(2).setCellValue(p)
    }
  }
}

case class Spreadsheet(sheets: Sheet*) {
  val book: Workbook = new org.apache.poi.xssf.streaming.SXSSFWorkbook(512)

  def fill(model: Model) {
    this.sheets.foreach { sheet =>
      sheet.fill(this.book.createSheet(sheet.title), model)
    }
  }

  def write(out: File) {
    val stream = new FileOutputStream(out)
    book.write(stream)
    stream.close()
  }
}

object CreateSpreadsheet extends App {
  import edu.umd.mith.topic.mallet.MalletModel
  val spreadsheet = Spreadsheet(
    DocTopicSheet,
    TopicWordFormSheet,
    TopicWordProbSheet,
    DocTopicEdgesSheet(),
    DocDocEdgesSheet(10)
  )

  spreadsheet.fill(new MalletModel(new File(args(0)), 0.1))
  spreadsheet.write(new File(args(1)))
}


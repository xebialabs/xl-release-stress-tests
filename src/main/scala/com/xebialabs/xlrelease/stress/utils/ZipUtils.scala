package com.xebialabs.xlrelease.stress.utils

import java.io.{File, FileOutputStream, InputStream}
import java.util.zip.{ZipEntry, ZipInputStream, ZipOutputStream}

import com.xebialabs.xlrelease.stress.utils.ResourceManagement._
import org.apache.commons.io.IOUtils

import scala.util.Try

object ZipUtils {
  def processEntries[B](zipFile: InputStream)(doStuff: ZipInputStream => ZipEntry => B): Seq[B] = {
    using(new ZipInputStream(zipFile)) { zipInputStream =>
      val doThing = doStuff(zipInputStream)
      Stream.continually(Try(zipInputStream.getNextEntry).getOrElse(null))
        .takeWhile(_ != null) // while not EOF and not corrupted
        .map(doThing)
        .force
    }
  }

  def extract(zipFile: InputStream, dest: File): Unit = {
    dest.mkdirs()
    processEntries(zipFile) { zis =>
      entry =>
        val fileName = entry.getName
        val newFile = new File(dest, fileName)
        new File(newFile.getParent).mkdirs()
        if (entry.isDirectory) {
          newFile.mkdir()
        } else {
          val fos = new FileOutputStream(newFile)
          IOUtils.copy(zis, fos)
          fos.close()
        }
    }
  }

  def create(archive: String, files: File*): ZipOutputStream = {
    import java.io.{BufferedInputStream, FileInputStream, FileOutputStream}
    import java.util.zip.{ZipEntry, ZipOutputStream}

    val zip = new ZipOutputStream(new FileOutputStream(archive))

    files.foreach { file =>
      zip.putNextEntry(new ZipEntry(file.getName))
      val in = new BufferedInputStream(new FileInputStream(file))
      var b = in.read()
      while (b > -1) {
        zip.write(b)
        b = in.read()
      }
      in.close()
      zip.closeEntry()
    }
    zip.close()
    zip
  }

}
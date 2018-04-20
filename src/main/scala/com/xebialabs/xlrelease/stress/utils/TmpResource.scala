package com.xebialabs.xlrelease.stress.utils

import java.io.{File, FileOutputStream}

import com.xebialabs.xlrelease.stress.utils.ResourceManagement.using
import org.apache.commons.io.IOUtils


object TmpResource {
  def apply[A](path: String, prefix: String = "tmpFile", suffix: String = ".tmp"): File = {
    val in = this.getClass.getClassLoader.getResourceAsStream(path)
    val tmpFile = File.createTempFile(prefix, suffix)
    tmpFile.deleteOnExit()
    using(new FileOutputStream(tmpFile)) { out =>
      IOUtils.copy(in, out)
    }
    tmpFile
  }
}

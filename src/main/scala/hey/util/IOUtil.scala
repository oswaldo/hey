/*
 * Copyright 2019 Oswaldo C. Dantas Jr
 *
 * SPDX-License-Identifier: MIT
 */

package hey.util

import java.io.Closeable
import java.nio.file.{Files, Path, Paths}

object IOUtil {

  def homePath: String = System.getProperty("user.home")

  def fileExists(p: Path): Boolean = Files.exists(p) && Files.isRegularFile(p)

  def fileExists(path: String): Boolean = fileExists(Paths.get(path))

  def readBytes(path: String): Option[Array[Byte]] =
    readBytes(Paths.get(path))

  def readBytes(p: Path): Option[Array[Byte]] = {
    if (Files.exists(p) && Files.isRegularFile(p)) {
      Some(Files.readAllBytes(p))
    } else {
      None
    }
  }

  def readString(path: String): Option[String] =
    readBytes(path).map(new String(_))

  def using[A, B <: Closeable](closeable: B)(f: B => A): A = {
    try {
      f(closeable)
    } finally {
      closeable.close()
    }
  }

}

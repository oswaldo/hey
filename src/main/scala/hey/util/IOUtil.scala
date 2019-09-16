/*
 * Copyright 2019 Oswaldo C. Dantas Jr
 *
 * SPDX-License-Identifier: MIT
 */

package hey.util

import java.nio.file.{Files, Path, Paths}

object IOUtil {

  def homePath: String = System.getProperty("user.home")

  def fileExists(p: Path): Boolean = Files.exists(p) && Files.isRegularFile(p)

  def fileExists(path: String): Boolean = fileExists(Paths.get(path))

  def readBytes(path: String): Option[Array[Byte]] = {
    val p = Paths.get(path)
    if (Files.exists(p) && Files.isRegularFile(p)) {
      Some(Files.readAllBytes(Paths.get(path)))
    } else {
      None
    }
  }

  def readString(path: String): Option[String] =
    readBytes(path).map(new String(_))

}

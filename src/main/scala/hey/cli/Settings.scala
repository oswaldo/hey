/*
 * Copyright 2019 Oswaldo C. Dantas Jr
 *
 * SPDX-License-Identifier: MIT
 */

package hey.cli

import hey.util.IOUtil
import org.ekrich.config.{Config, ConfigFactory}
import hey.cli.Settings.implicits._
import hey.util.IOUtil.homePath

object Settings:

  val settingsPath = s"$homePath/.hey/hey.conf"

  private def readConfig(path: String): Option[Config] =
    IOUtil.readString(path).map(ConfigFactory.parseString)

  def read(path: String): Option[Settings] =
    readConfig(path).map(new Settings(_))

  object implicits:

    implicit class RichConfig(val underlying: Config) extends AnyVal:
      def getOptionalBoolean(path: String): Option[Boolean] =
        optional(path, underlying.getBoolean)

      def getOptionalString(path: String): Option[String] =
        optional(path, underlying.getString)

      def getStringOrElse(path: String, default: => String): String =
        getOrElse(path, underlying.getString, default)

      private def optional[T](path: String, f: String => T): Option[T] =
        if underlying.hasPath(path) then Some(f(path))
        else None

      private def getOrElse[T](
          path: String,
          f: String => T,
          default: => T
      ): T =
        optional(path, f).getOrElse(default)

class Settings(config: Config = ConfigFactory.empty()):
  private val defaultEmptyString = ""
  val defaultServerGroup: String =
    config.getStringOrElse("hey.defaults.serverGroup", defaultEmptyString)
  val defaultServiceName: String =
    config.getStringOrElse("hey.defaults.serviceName", defaultEmptyString)
  val defaultContainerName: String =
    config.getStringOrElse("hey.defaults.containerName", defaultEmptyString)

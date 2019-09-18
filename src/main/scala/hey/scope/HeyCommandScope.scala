/*
 * Copyright 2019 Oswaldo C. Dantas Jr
 *
 * SPDX-License-Identifier: MIT
 */

package hey.scope

import hey.cli.Settings
import hey.scope.Verbosity.Full
import hey.util.ProcessUtil
import scopt.{OParser, OParserBuilder, Read}

trait HeyCommandScope {

  implicit val scoptBuilder: OParserBuilder[HeyCommandConfig] =
    OParser.builder[HeyCommandConfig]

  def scope: String

  def description: String

  def elements: List[HeyElement[_]]

  def scoptDefinition(
      implicit scoptBuilder: OParserBuilder[HeyCommandConfig]
  ): OParser[_, HeyCommandConfig] = {
    import scoptBuilder._
    def childrenDefinition(e: HeyElement[_]): OParser[_, HeyCommandConfig] = {
      if (e.children.isEmpty) {
        e.scoptDefinition
      } else {
        e.scoptDefinition.children(e.children.map(childrenDefinition): _*)
      }
    }
    cmd(scope)
      .action((_, c) => c.copy(commandScope = scope))
      .text(description)
      .children(
        elements.map(
          e => childrenDefinition(e)
        ): _*
      )
  }

  val validate: HeyCommandConfig => Option[String] = _ => None

  def proceed(c: HeyCommandConfig): Boolean =
    c.commandScope == scope && elements.view
      .map(_.proceed(c))
      .find(!_)
      .getOrElse(true)
}

object Verbosity {
  val Full = "full"
}

object CommandScope {
  val Hey = "hey"
  val Ansible = "ansible"
  val Docker = "docker"
  val Sbt = "sbt"
  val Git = "git"
}

object Command {
  val Echo = "echo"
  val Status = "status"
  val Restart = "restart"
  val Stop = "stop"
  val ContainerBash = "bash"
  val Purge = "purge"
  val Test = "test"
  val Squash = "squash"
  val Checkout = "checkout"
}

object HeyCommandConfig {

  val settings: Settings =
    Settings.read(Settings.settingsPath).getOrElse(new Settings())

  object implicits {

    implicit class HeyString(s: String) {
      def commanded(implicit c: HeyCommandConfig): Boolean = c.command == s
      def scoped(implicit c: HeyCommandConfig): Boolean = c.commandScope == s
    }

  }
}

case class HeyCommandConfig(
    verbosity: String = Full,
    commandScope: String = "",
    command: String = "",
    serverGroup: String = HeyCommandConfig.settings.defaultServerGroup,
    serviceName: String = HeyCommandConfig.settings.defaultServiceName,
    containerBash: Boolean = false,
    containerName: String = HeyCommandConfig.settings.defaultContainerName,
    debug: Boolean = false,
    testNameEnding: String = "",
    purge: Boolean = false,
    targetBranch: String = ""
) {
  val fullVerbosity: Boolean = verbosity == Full

  def commandedAny(s: String*): Boolean = s.toSet.contains(command)

}

sealed abstract class HeyElement[T: Read] {

  val name: String
  val scoptElement: String => OParser[T, HeyCommandConfig]
  val scoptAction: (T, HeyCommandConfig) => HeyCommandConfig
  val abbreviation: Option[String]
  val description: Option[String]
  val commandAndArguments: HeyCommandConfig => List[String]
  val contextMessage: HeyCommandConfig => String
  val required: Boolean
  val confirmationMessage: Option[String]
  val children: List[HeyElement[_]] = Nil

  implicit val scoptBuilder: OParserBuilder[HeyCommandConfig]

  def scoptDefinition: OParser[T, HeyCommandConfig] = {

    val optionals
        : List[OParser[T, HeyCommandConfig] => OParser[T, HeyCommandConfig]] =
      List(
        c => abbreviation.map(c.abbr).getOrElse(c),
        c => description.map(c.text).getOrElse(c),
        c => if (required) c.required() else c.optional()
      )

    optionals.foldLeft(
      scoptElement(name)
        .action(scoptAction)
    )((c, f) => f(c))
  }

  def proceedAfterExecuting: Boolean = true

  def execute(
      c: HeyCommandConfig
  ): Unit = {

    val callDescription: String = List(
      description,
      Option(contextMessage(c)).map(m => if (m.isEmpty) m else s"($m)")
    ).filter(s => s.isDefined && !s.get.isEmpty).flatten.mkString(" ")
    ProcessUtil.execute(
      c,
      callDescription,
      confirmationMessage,
      commandAndArguments(c)
    )
  }

  def proceed(c: HeyCommandConfig): Boolean = {
    if (c.commandedAny(name)) {
      execute(c)
    }
    proceedAfterExecuting
  }
}

class HeyOption[T: Read](
    override val name: String,
    override val scoptAction: (T, HeyCommandConfig) => HeyCommandConfig =
      (_: T, c: HeyCommandConfig) => c,
    override val abbreviation: Option[String] = None,
    override val description: Option[String] = None,
    override val commandAndArguments: HeyCommandConfig => List[String] = _ =>
      Nil,
    override val contextMessage: HeyCommandConfig => String = _ => "",
    override val required: Boolean = false,
    override val confirmationMessage: Option[String] = None,
    override val children: List[HeyElement[_]] = Nil
)(implicit val scoptBuilder: OParserBuilder[HeyCommandConfig])
    extends HeyElement[T] {

  import scoptBuilder._
  override val scoptElement: String => OParser[T, HeyCommandConfig] =
    opt[T]
}

class HeyCommand(
    override val name: String,
    commandAction: HeyCommandConfig => HeyCommandConfig = c => c,
    override val abbreviation: Option[String] = None,
    override val description: Option[String] = None,
    override val commandAndArguments: HeyCommandConfig => List[String] = _ =>
      Nil,
    override val contextMessage: HeyCommandConfig => String = _ => "",
    override val required: Boolean = false,
    override val confirmationMessage: Option[String] = None,
    override val children: List[HeyElement[_]] = Nil
)(implicit val scoptBuilder: OParserBuilder[HeyCommandConfig])
    extends HeyElement[Unit] {

  import scoptBuilder._
  override val scoptElement: String => OParser[Unit, HeyCommandConfig] =
    cmd

  override val scoptAction: (Unit, HeyCommandConfig) => HeyCommandConfig =
    (_, c) => commandAction(c)
}

class HeyArgument[T: Read](
    override val name: String,
    override val scoptAction: (T, HeyCommandConfig) => HeyCommandConfig =
      (_: T, c: HeyCommandConfig) => c,
    override val abbreviation: Option[String] = None,
    override val description: Option[String] = None,
    override val commandAndArguments: HeyCommandConfig => List[String] = _ =>
      Nil,
    override val contextMessage: HeyCommandConfig => String = _ => "",
    override val required: Boolean = false,
    override val confirmationMessage: Option[String] = None,
    override val children: List[HeyElement[_]] = Nil
)(implicit val scoptBuilder: OParserBuilder[HeyCommandConfig])
    extends HeyElement[T] {

  import scoptBuilder._
  override val scoptElement: String => OParser[T, HeyCommandConfig] =
    arg[T]
}

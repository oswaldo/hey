/*
 * Copyright 2019 Oswaldo C. Dantas Jr
 *
 * SPDX-License-Identifier: MIT
 */

package hey.cli

import hey.scope._
import hey.scope.HeyCommandConfig
import scopt.{OParser, OParserBuilder}

object HeyCli:
  val DefaultSupportedScopes: List[HeyCommandScope] =
    List(new AnsibleScope, new DockerScope, new SbtScope, new GitScope)

trait HeyCli(supportedScopes: List[HeyCommandScope] = DefaultSupportedScopes):

  implicit val scoptBuilder: OParserBuilder[HeyCommandConfig] =
    OParser.builder[HeyCommandConfig]
  import scoptBuilder._

  private val parser: OParser[Unit, HeyCommandConfig] =
    OParser.sequence(
      programName("hey"), {
        val elements = List(
          head("hey", "0.1"),
          opt[String]("verbosity")
            .abbr("vb")
            .action((x, c) => c.copy(verbosity = x))
            .optional()
            .text(s"defaults to full. any other value means silent")
        ) ++
          supportedScopes.map(_.scoptDefinition) :+ note(
            s"You can define default values for command options at the hocon file ${Settings.settingsPath}"
          ) :+
          checkConfig(validate)
        elements
      }: _*
    )

  private def generalValidation(c: HeyCommandConfig): Option[String] =
    if c.commandScope.isEmpty then
      Some("at least one of the supported commands should have been called")
    else None

  private def validate(c: HeyCommandConfig): Either[String, Unit] =
    generalValidation(c).map(failure).getOrElse {
      supportedScopes.view
        .map(scope => scope.validate(c))
        .find(_.isDefined)
        .flatten match
        case Some(failureMessage) =>
          failure(failureMessage)
        case None =>
          success
    }

  private def run(c: HeyCommandConfig): Unit =
    supportedScopes.map(_.proceed(c)).find(!_)

  def main(args: Array[String]): Unit =
    OParser.parse(parser, args, HeyCommandConfig()) match
      case Some(c) =>
        run(c)
      case None =>
      // bad arguments. nothing to do
    sys.exit()

object HeyMain extends HeyCli()

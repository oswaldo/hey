/*
 * Copyright 2019 Oswaldo C. Dantas Jr
 *
 * SPDX-License-Identifier: MIT
 */

package hey.scope

import hey.scope.Command.{Purge, Test}
import hey.scope.CommandScope.Sbt
import hey.util.ProcessUtil._
import scopt.{OParser, OParserBuilder}

class SbtScope(
    override implicit val scoptBuilder: OParserBuilder[HeyCommandConfig] =
      OParser.builder[HeyCommandConfig]
) extends HeyCommandScope {

  override val scope: String = Sbt

  override val description: String = "Sbt related commands"

  private val DebugOption: HeyOption[Unit] = new HeyOption[Unit](
    name = "debug",
    scoptAction = (_, c) => c.copy(debug = true),
    abbreviation = Some("d"),
    description = Some("if the process should be started in debug mode")
  )

  private val PurgeCommand = new HeyCommand(
    name = Purge,
    commandAction = c => c.copy(command = Purge),
    description = Some("Removes target folders"),
    commandAndArguments =
      evalArguments(_, "rm -rf target; rm -rf project/target"),
    confirmationMessage = Some(
      "This will remove target and project/target folders. Are you sure? (If you requested some other sbt command, it will still be executed)"
    )
  )

  private val TestNameEndingArgument = new HeyArgument[String](
    name = "<suffix>",
    scoptAction = (x, c) => c.copy(testNameEnding = x),
    description = Some(
      "This avoids having to use the FQCN, prepending an * to the call"
    )
  )

  private val TestCommand = new HeyCommand(
    name = Test,
    commandAction = c => c.copy(command = Test),
    description = Some(
      "Runs all sbt tests or the ones matching the given suffix"
    ),
    children = TestNameEndingArgument :: Nil,
    commandAndArguments = c =>
      "sbt" :: (if (c.debug) List("-Ddebug=1") else Nil) ++ List(
        if (c.testNameEnding.isEmpty) {
          "test"
        } else {
          s"""testOnly *${c.testNameEnding}"""
        }
      )
  )

  override val elements: List[HeyElement[_]] =
    List(
      DebugOption,
      PurgeCommand,
      TestCommand
    )

}

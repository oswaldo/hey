/*
 * Copyright 2019 Oswaldo C. Dantas Jr
 *
 * SPDX-License-Identifier: MIT
 */

package hey.scope

import hey.cli.Settings.settingsPath
import hey.scope.CommandScope.Docker
import hey.scope.Command.ContainerBash
import scopt.{OParser, OParserBuilder}

class DockerScope(
    override implicit val scoptBuilder: OParserBuilder[HeyCommandConfig] =
      OParser.builder[HeyCommandConfig]
) extends HeyCommandScope:

  override val scope: String = Docker

  override val description: String = "Docker related commands"

  def dockerContextMessage(c: HeyCommandConfig) =
    s"serverGroup: ${c.containerName}"

  override val validate: HeyCommandConfig => Option[String] = { c =>
    if c.commandScope == Docker && c.containerName.isEmpty then
      Some(
        s"for docker commands, containerName must be defined, through command line option on in the conf file in $settingsPath"
      )
    else None
  }

  private val ContainerNameOption = new HeyOption[String](
    name = "containerName",
    scoptAction = (x, c) => c.copy(containerName = x),
    abbreviation = Some("cn"),
    description = Some("which container should I execute at")
  )

  private val BashCommand = new HeyCommand(
    ContainerBash,
    commandAction = c => c.copy(command = ContainerBash),
    description = Some("Runs bash on the defined containerName"),
    commandAndArguments = c =>
      List(
        "docker",
        "exec",
        "-it",
        c.containerName,
        "bash",
        "-l"
      ),
    contextMessage = dockerContextMessage
  )

  override val elements: List[HeyElement[_]] =
    List(
      ContainerNameOption,
      BashCommand
    )

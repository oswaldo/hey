/*
 * Copyright 2019 Oswaldo C. Dantas Jr
 *
 * SPDX-License-Identifier: MIT
 */

package hey.scope

import hey.scope.Command.{Restart, Status, Stop}
import hey.scope.CommandScope.Ansible
import hey.cli.Settings.settingsPath
import scopt.{OParser, OParserBuilder}
class AnsibleScope(
    override implicit val scoptBuilder: OParserBuilder[HeyCommandConfig] =
      OParser.builder[HeyCommandConfig]
) extends HeyCommandScope:
  override val scope: String = Ansible

  override val description: String = "Ansible related commands"

  def ansibleContextMessage(c: HeyCommandConfig) =
    s"serverGroup: ${c.serverGroup}, serviceName: ${c.serviceName}"

  override val validate: HeyCommandConfig => Option[String] = { c =>
    if c.commandScope == Ansible && (c.serverGroup.isEmpty || c.serviceName.isEmpty) then
      Some(
        s"for ansible commands, serverGroup and serviceName must be defined, through command line options on in the conf file in $settingsPath"
      )
    else
      None
  }

  private val ServerGroupOption = new HeyOption[String](
    name = "serverGroup",
    scoptAction = (x, c) => c.copy(serverGroup = x),
    abbreviation = Some("sg"),
    description = Some("which servers should I send a command to")
  )

  private val ServiceNameOption = new HeyOption[String](
    name = "serviceName",
    scoptAction = (x, c) => c.copy(serviceName = x),
    abbreviation = Some("sn"),
    description = Some("which service should respond to the command")
  )

  private val StatusCommand = new HeyCommand(
    Status,
    commandAction = c => c.copy(command = Status),
    abbreviation = Some("st"),
    description = Some("Returns the systemd status from those servers"),
    commandAndArguments = c =>
      List(
        "ansible",
        c.serverGroup,
        "-a",
        s"systemctl status ${c.serviceName}.service",
        "-v"
      ),
    contextMessage = ansibleContextMessage
  )

  private val RestartCommand = new HeyCommand(
    Restart,
    commandAction = c => c.copy(command = Restart),
    description = Some("(Re)starts those servers"),
    commandAndArguments = c =>
      List(
        "ansible",
        c.serverGroup,
        "-b",
        "-m",
        "service",
        "-a",
        s"name=${c.serviceName} state=restarted",
        "-v"
      ),
    contextMessage = ansibleContextMessage
  )

  private val StopCommand = new HeyCommand(
    Stop,
    commandAction = c => c.copy(command = Stop),
    description = Some("Stops those servers"),
    commandAndArguments = c =>
      List(
        "ansible",
        c.serverGroup,
        "-b",
        "-m",
        "service",
        "-a",
        s"name=${c.serviceName} state=stopped",
        "-v"
      ),
    contextMessage = ansibleContextMessage
  )

  override val elements: List[HeyElement[_]] =
    List(
      ServerGroupOption,
      ServiceNameOption,
      StatusCommand,
      RestartCommand,
      StopCommand
    )


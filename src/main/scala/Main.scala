/*
 * Copyright 2019 Oswaldo C. Dantas Jr
 *
 * SPDX-License-Identifier: MIT
 */

import scopt.OptionParser

import sys.process.Process

object Main {

  private case class HeyCommandConfig(
      serverGroup: String = "",
      serviceName: String = "",
      statusCommand: Boolean = false,
      restartCommand: Boolean = false,
      stopCommand: Boolean = false
  )

  private val parser = new OptionParser[HeyCommandConfig]("hey") {
    head("hey", "0.1")

    opt[String]("serverGroup")
      .required()
      .abbr("sg")
      .action((x, c) => c.copy(serverGroup = x))
      .text(
        s"which servers should I send a command to"
      )

    opt[String]("serviceName")
      .required()
      .abbr("sn")
      .action((x, c) => c.copy(serviceName = x))
      .text(
        s"which service should respond to the command"
      )

    cmd("status")
      .abbr("st")
      .action((_, c) => c.copy(statusCommand = true))
      .text(s"Returns the systemd status from those servers")

    cmd("restart")
      .action((_, c) => c.copy(restartCommand = true))
      .text(s"(Re)starts those servers")

    cmd("stop")
      .action((_, c) => c.copy(stopCommand = true))
      .text(s"Stops those servers")

    checkConfig(
      c =>
        if (c.restartCommand && c.stopCommand) {
          failure("restart and stop command should not be used simultaneously")
        } else if (!(c.statusCommand || c.restartCommand || c.stopCommand)) {
          failure(
            "at least one of the supported commands should have been called"
          )
        } else {
          success
        }
    )

  }

  def execute(description: String, command: String): Unit = {
    println(description)
    println(Process(command) !!)
  }

  def main(args: Array[String]): Unit =
    parser.parse(args, HeyCommandConfig()) match {
      case Some(c) =>
        if (c.statusCommand) {
          execute(
            "Requesting service status",
            s"""ansible ${c.serverGroup} -a "systemctl status ${c.serviceName}.service""""
          )
        }
        if (c.restartCommand) {
          execute(
            "Requesting service restart",
            s"""ansible ${c.serverGroup} -b -m service -a "name=${c.serviceName} state=restarted""""
          )
        }
        if (c.stopCommand) {
          execute(
            "Requesting service restart",
            s"""ansible ${c.serverGroup} -b -m service -a "name=${c.serviceName} state=stopped""""
          )
        }

      case None =>
      //bad arguments. nothing to do
    }
}

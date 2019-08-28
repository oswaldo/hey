/*
 * Copyright 2019 Oswaldo C. Dantas Jr
 *
 * SPDX-License-Identifier: MIT
 */

import java.io.{InputStream, OutputStream}

import scopt.OptionParser

object Main {

  private case class HeyCommandConfig(echo: String = "",
                                      verbosity: String = "full",
                                      commandScope: String = "",
                                      serverGroup: String = "",
                                      serviceName: String = "",
                                      statusCommand: Boolean = false,
                                      restartCommand: Boolean = false,
                                      stopCommand: Boolean = false,
                                      printVersion: Boolean = false) {
    val fullVerbosity = verbosity == "full"
  }

  private val parser = new OptionParser[HeyCommandConfig]("hey") {
    head("hey", "0.1")

    cmd("echo")
      .text(
        s"Prints the value from the process itself and again from OS echo command for process execution test purposes"
      )
      .children(
        arg[String]("<value>")
          .required()
          .action((x, c) => c.copy(echo = x))
      )

    opt[Unit]("version")
      .text(s"Prints version information")
      .action((_, c) => c.copy(printVersion = true))
      .optional()

    opt[String]("verbosity")
      .abbr("vb")
      .action((x, c) => c.copy(verbosity = x))
      .optional()
      .text(s"defaults to full. any other value means silent")

    cmd("ansible")
      .action((_, c) => c.copy(commandScope = "ansible"))
      .text("ansible related commands")
      .children(
        opt[String]("serverGroup")
          .required()
          .abbr("sg")
          .action((x, c) => c.copy(serverGroup = x))
          .text(s"which servers should I send a command to"),
        opt[String]("serviceName")
          .required()
          .abbr("sn")
          .action((x, c) => c.copy(serviceName = x))
          .text(s"which service should respond to the command"),
        cmd("status")
          .abbr("st")
          .action((_, c) => c.copy(statusCommand = true))
          .text(s"Returns the systemd status from those servers"),
        cmd("restart")
          .action((_, c) => c.copy(restartCommand = true))
          .text(s"(Re)starts those servers"),
        cmd("stop")
          .action((_, c) => c.copy(stopCommand = true))
          .text(s"Stops those servers"),
        checkConfig(
          c =>
            if (c.restartCommand && c.stopCommand) {
              failure(
                "restart and stop command should not be used simultaneously"
              )
            } else if (!(!c.echo.isEmpty || c.printVersion || c.statusCommand || c.restartCommand || c.stopCommand)) {
              failure(
                "at least one of the supported commands should have been called"
              )
            } else {
              success
          }
        )
      )

  }

  def execute(c: HeyCommandConfig,
              description: String,
              command: String,
              arguments: String*): Unit = {

    def printIfSome(s: String, error: Boolean = false): Unit =
      if (c.fullVerbosity && !s.isEmpty) {
        if (error) System.err.println(s) else println(s)
      }

    printIfSome(description)
    val commandAndArguments = command :: arguments.toList
    printIfSome(s"Will execute the following command: ${commandAndArguments
      .map(arg => if (arg.contains(" ")) s""""$arg"""" else arg)
      .mkString(" ")}")

    //region using java classes because scala process handling need threads and that is not yet supported by scala-native
    try {
      val process =
        Runtime.getRuntime.exec(commandAndArguments.toArray)

      def forward(in: InputStream, out: OutputStream): Unit = {
        val bufferSize = 1024
        val buffer = new Array[Byte](bufferSize)
        var read = 0
        while ({
          read = in.read(buffer)
          read != -1
        }) out.write(buffer, 0, read)
      }
      forward(process.getInputStream, System.out)
      forward(process.getErrorStream, System.err)
      process.waitFor()
      printIfSome(
        s"Process ended. Alive: ${process.isAlive}. Exit value: ${process.exitValue()}",
        error = process.exitValue() != 0
      )
      //endregion
    } catch {
      case e: Throwable =>
        println("Failed to execute command")
        e.printStackTrace()
        System.exit(-1)
    }
  }

  def main(args: Array[String]): Unit =
    parser.parse(args, HeyCommandConfig()) match {
      case Some(c) =>
        println(c.echo)
        if (c.printVersion) {
          execute(c, "Request ansible version", "ansible", "--version")
        }
        if (!c.echo.isEmpty) {
          execute(c, "Call OS echo command", "echo", c.echo)
        }
        if (c.statusCommand) {
          execute(
            c,
            "Requesting service status",
            "ansible",
            c.serverGroup,
            "-a",
            s"""systemctl status ${c.serviceName}.service""",
            "-v"
          )
        }
        if (c.restartCommand) {
          execute(
            c,
            "Requesting service restart",
            s"""ansible ${c.serverGroup} -b -m service -a "name=${c.serviceName} state=restarted""""
          )
        }
        if (c.stopCommand) {
          execute(
            c,
            "Requesting service restart",
            s"""ansible ${c.serverGroup} -b -m service -a "name=${c.serviceName} state=stopped""""
          )
        }

      case None =>
      //bad arguments. nothing to do
    }
}

/*
 * Copyright 2019 Oswaldo C. Dantas Jr
 *
 * SPDX-License-Identifier: MIT
 */

import java.io.{InputStream, OutputStream}

import org.ekrich.config.ConfigFactory
import scopt.OptionParser

object Main {

  private val settingsPath = s"${IOUtil.homePath}/.hey/hey.conf"
  private val settings =
    Settings.read(settingsPath).getOrElse(new Settings())

  private case class HeyCommandConfig(
      echo: String = "",
      verbosity: String = "full",
      commandScope: String = "",
      serverGroup: String = settings.defaultServerGroup,
      serviceName: String = settings.defaultServiceName,
      statusCommand: Boolean = false,
      restartCommand: Boolean = false,
      stopCommand: Boolean = false,
      printVersion: Boolean = false,
      containerBash: Boolean = false,
      containerName: String = settings.defaultContainerName,
      debug: Boolean = false,
      testNameEnding: String = "",
      purge: Boolean = false
  ) {
    val fullVerbosity = verbosity == "full"
  }

  private val parser = new OptionParser[HeyCommandConfig]("hey") {
    head("hey", "0.1")

    opt[Unit]("version")
      .text(s"Prints version information")
      .action((_, c) => c.copy(printVersion = true))
      .optional()

    opt[String]("verbosity")
      .abbr("vb")
      .action((x, c) => c.copy(verbosity = x))
      .optional()
      .text(s"defaults to full. any other value means silent")

    cmd("echo")
      .text(
        s"Prints the value from the process itself and again from OS echo command for process execution test purposes"
      )
      .children(
        arg[String]("<value>")
          .required()
          .action((x, c) => c.copy(echo = x))
      )

    cmd("ansible")
      .action((_, c) => c.copy(commandScope = "ansible"))
      .text("ansible related commands")
      .children(
        opt[String]("serverGroup")
          .abbr("sg")
          .action((x, c) => c.copy(serverGroup = x))
          .withFallback(() => settings.defaultServerGroup)
          .text(s"which servers should I send a command to"),
        opt[String]("serviceName")
          .abbr("sn")
          .action((x, c) => c.copy(serviceName = x))
          .withFallback(() => settings.defaultServiceName)
          .text(s"which service should respond to the command"),
        cmd("status")
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
            } else if (!(!c.echo.isEmpty || c.printVersion || c.statusCommand || c.restartCommand || c.stopCommand || c.containerBash || c.commandScope == "sbt")) {
              failure(
                "at least one of the supported commands should have been called"
              )
            } else if (c.commandScope == "ansible" && (c.serverGroup.isEmpty || c.serviceName.isEmpty)) {
              failure(
                s"for ansible commands, serverGroup and serviceName must be defined, through command line options on in the conf file in $settingsPath"
              )
            } else {
              success
            }
        )
      )

    note(
      s"You can define default values for command options at the hocon file $settingsPath"
    )

    cmd("docker")
      .action((_, c) => c.copy(commandScope = "docker"))
      .text("docker related commands")
      .children(
        opt[String]("containerName")
          .abbr("cn")
          .action((x, c) => c.copy(serverGroup = x))
          .withFallback(() => settings.defaultContainerName)
          .text(s"which container should I execute at"),
        cmd("bash")
          .action((_, c) => c.copy(containerBash = true))
          .text(s"Runs bash on the defined containerName")
      )

    cmd("sbt")
      .action((_, c) => c.copy(commandScope = "sbt"))
      .text("sbt related commands")
      .children(
        opt[Unit]("debug")
          .abbr("d")
          .action((_, c) => c.copy(debug = true))
          .optional()
          .text(s"if the process should be started in debug mode"),
        opt[Unit]("purge")
          .abbr("p")
          .action((_, c) => c.copy(purge = true))
          .optional()
          .text(s"if target folders should be removed"),
        cmd("testOnly")
          .text(
            s"Runs sbt in test only mode for the tests matching the argument"
          )
          .children(
            arg[String]("<testNameEnding>")
              .text(
                "This avoids having to use the FQCN, prepending an * to the call"
              )
              .required()
              .action((x, c) => c.copy(testNameEnding = x))
          )
      )

  }

  def confirm[T](
      f: => T,
      message: String = "Are you sure you want to continue?"
  ): Option[T] = {
    println(s"$message [y|n]")
    var k = -1
    var printHelp = true
    while (k != 'y' && k != 'n') {
      if (k != -1 && printHelp) {
        println(
          "Enter y if you want to execute the action, otherwise, n to abort"
        )
        printHelp = false
      }
      k = Console.in.read()
    }
    k match {
      case 'y' => Option(f)
      case 'n' => None
    }
  }

  def eval(c: HeyCommandConfig, description: String, script: String): Unit =
    execute(c, description, "sh" :: "-c" :: script :: Nil)

  def execute(
      c: HeyCommandConfig,
      description: String,
      command: String,
      arguments: String*
  ): Unit =
    execute(c, description, command :: arguments.toList)

  def execute(
      c: HeyCommandConfig,
      description: String,
      commandAndArguments: List[String]
  ): Unit = {

    def printIfSome(s: String, error: Boolean = false): Unit =
      if (c.fullVerbosity && !s.isEmpty) {
        if (error) System.err.println(s) else println(s)
      }

    printIfSome(description)
    printIfSome(s"Will execute the following command: ${commandAndArguments
      .map(arg => if (arg.contains(" ")) s""""$arg"""" else arg)
      .mkString(" ")}")

    //region using java classes because scala process handling need threads and that is not yet supported by scala-native
    try {
      val process =
        new ProcessBuilder().inheritIO.command(commandAndArguments: _*).start()
      Runtime.getRuntime.exec(commandAndArguments.toArray)
      process.waitFor()
      printIfSome(
        s"Process ended. Exit value: ${process.exitValue()}",
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

  def contextMessage(c: HeyCommandConfig) =
    s"serverGroup: ${c.serverGroup}, serviceName: ${c.serviceName}"

  def main(args: Array[String]): Unit = {
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
            s"Requesting service status (${contextMessage(c)})",
            "ansible",
            c.serverGroup,
            "-a",
            s"systemctl status ${c.serviceName}.service",
            "-v"
          )
        }
        if (c.restartCommand) {
          execute(
            c,
            s"Requesting service restart (${contextMessage(c)})",
            "ansible",
            c.serverGroup,
            "-b",
            "-m",
            "service",
            "-a",
            s"name=${c.serviceName} state=restarted",
            "-v"
          )
        }
        if (c.stopCommand) {
          execute(
            c,
            s"Requesting service stop (${contextMessage(c)})",
            "ansible",
            c.serverGroup,
            "-b",
            "-m",
            "service",
            "-a",
            s"name=${c.serviceName} state=stopped",
            "-v"
          )
        }

        if (c.containerBash) {
          execute(
            c,
            s"Bashing into container (${contextMessage(c)})",
            "docker",
            "exec",
            "-it",
            c.containerName,
            "bash"
          )
        }

        if (c.purge) {
          confirm(
            eval(
              c,
              "Purging sbt target folders",
              "rm -rf target; rm -rf project/target"
            ),
            "This will remove target and project/target folders. Are you sure? (If you requested some other sbt command, it will still be executed)"
          )
        }

        if (!c.testNameEnding.isEmpty) {
          val sbtArgs = (if (c.debug) List("-Ddebug=1") else Nil) ++ List(
            s"""testOnly *${c.testNameEnding}"""
          )
          execute(
            c,
            s"Starting sbt test(s) (${contextMessage(c)})",
            "sbt",
            sbtArgs: _*
          )
        }

      case None =>
      //bad arguments. nothing to do
    }
    sys.exit()
  }
}

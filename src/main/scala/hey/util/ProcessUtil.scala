/*
 * Copyright 2019 Oswaldo C. Dantas Jr
 *
 * SPDX-License-Identifier: MIT
 */

package hey.util

import hey.scope.HeyCommandConfig
import hey.util.IOUtil._

object ProcessUtil:

  private def confirm[T](
      f: => T,
      message: String = "Are you sure you want to continue?"
  ): Option[T] =
    println(s"$message [y|n]")
    var k = -1
    var printHelp = true
    while k != 'y' && k != 'n' do
      if k != -1 && printHelp then
        println(
          "Enter y if you want to execute the action, otherwise, n to abort"
        )
        printHelp = false
      k = Console.in.read()
    k match
      case 'y' => Option(f)
      case 'n' => None

  private def confirmBoolean(
      message: String = "Are you sure you want to continue?"
  ): Boolean =
    confirm[Boolean](true, message).getOrElse(false)

  private def printIfSome(
      c: HeyCommandConfig,
      s: String,
      error: Boolean = false
  ): Unit =
    if c.fullVerbosity && s.trim.nonEmpty then if error then System.err.println(s) else println(s)

  def evalArguments(
      c: HeyCommandConfig,
      script: HeyCommandConfig => String
  ): List[String] =
    List("bash", "-c", script(c))

  def eval(
      c: HeyCommandConfig,
      callDescription: String,
      confirmationMessage: Option[String],
      script: HeyCommandConfig => String
  ): Unit =
    execute(c, callDescription, confirmationMessage, evalArguments(c, script))

  def execute(
      c: HeyCommandConfig,
      callDescription: String,
      confirmationMessage: Option[String],
      command: String,
      arguments: String*
  ): Unit =
    execute(
      c,
      callDescription,
      confirmationMessage,
      command :: arguments.toList
    )

  def execute(
      c: HeyCommandConfig,
      callDescription: String,
      confirmationMessage: Option[String],
      commandAndArguments: List[String]
  ): Unit =
    if confirmationMessage.forall(confirmBoolean) then
      printIfSome(c, callDescription)
      if commandAndArguments.isEmpty then
        printIfSome(
          c,
          "No command would be executed :/ There might be a problem with the implementation of the given HeyElement"
        )
      else
        printIfSome(
          c,
          s"Will execute the following command: ${commandAndArguments
              .map(arg => if arg.contains(" ") then s""""$arg"""" else arg)
              .mkString(" ")}"
        )

        try
          // region using java classes because scala process handling need threads and that is not yet supported by scala-native
          val process =
            new ProcessBuilder().inheritIO
              .command(commandAndArguments: _*)
              .start()
          Runtime.getRuntime.exec(commandAndArguments.toArray)
          process.waitFor()
          // endregion
          printIfSome(
            c,
            s"Process ended. Exit value: ${process.exitValue()}",
            error = process.exitValue() != 0
          )
        catch
          case e: Throwable =>
            println("Failed to execute command")
            e.printStackTrace()
            System.exit(-1)

/*
 * Copyright 2019 Oswaldo C. Dantas Jr
 *
 * SPDX-License-Identifier: MIT
 */

package hey.scope

import hey.scope.Command.Squash
import hey.scope.CommandScope.Git
import hey.util.ProcessUtil._
import scopt.{OParser, OParserBuilder}

class GitScope(
    override implicit val scoptBuilder: OParserBuilder[HeyCommandConfig] =
      OParser.builder[HeyCommandConfig]
) extends HeyCommandScope {

  override val scope: String = Git

  override val description: String = "Git related commands"

  private val TargetBranchArgument = new HeyArgument[String](
    name = "<targetBranch>",
    scoptAction = (x, c) => c.copy(targetBranch = x),
    description = Some(
      "Defaults to master"
    )
  )

  private val SquashCommand = new HeyCommand(
    name = Squash,
    commandAction = c => c.copy(command = Squash),
    description = Some(
      "Resets index to target branch, allowing all changes to be in a single commit to be done afterwards."
    ),
    children = TargetBranchArgument :: Nil,
    commandAndArguments = evalArguments(
      _,
      c =>
        s"""git reset $$(git merge-base ${if (c.targetBranch.isEmpty) {
          "master"
        } else {
          c.targetBranch
        }} $$(git rev-parse --abbrev-ref HEAD))"""
    ),
    confirmationMessage = Some(
      s"""After this, you will have to add and commit your changes with something like:
         |git add -A
         |git commit -m "Replacement message for the squashed commit"
         |And afterwards force push the changes case you had pushed before (which might be a problem for others working with copies of this branch. Stop if you are not sure what you are doing).
         |Continue?
         |""".stripMargin
    )
  )

  override val elements: List[HeyElement[_]] =
    List(
      SquashCommand
    )

}

/*
 * Copyright 2019 Oswaldo C. Dantas Jr
 *
 * SPDX-License-Identifier: MIT
 */

package hey.scope

import hey.scope.Command.{Checkout, Squash}
import hey.scope.CommandScope.Git
import hey.util.IOUtil._
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

  private val CheckoutCommand = new HeyCommand(
    name = Checkout,
    commandAction = c => c.copy(command = Checkout),
    description = Some(
      "Checks out the given branch name or partial name (if only one match is found)"
    ),
    children = TargetBranchArgument :: Nil,
    commandAndArguments = evalArguments(
      _,
      c => {
        val branch = raw""""${c.targetBranch}""""
        raw"""
              |#!/usr/bin/env bash
              |#as in bxm's reply to https://stackoverflow.com/questions/11340309/switch-branch-in-git-by-partial-name
              |
              |branch=$branch
              |[ -z "$$branch" ] && { echo -e "Please provide one search string" ; exit 1 ; }
              |MATCHES=( $$(git branch -a --color=never | sed -r 's|^[* ] (remotes/origin/)?||' | sort -u | grep -E "^((feature|bugfix|release|hotfix)/)?([A-Z]+-[1-9][0-9]*-)?${c.targetBranch}") )
              |case $${#MATCHES[@]} in
              |  ( 0 ) echo "No branches matched '${c.targetBranch}'" ; exit 1  ;;
              |  ( 1 ) git checkout "$${MATCHES[0]}"      ; exit $$? ;;
              |esac
              |echo "Ambiguous search '${c.targetBranch}'; returned $${#MATCHES[@]} matches:"
              |
              |for ITEM in "$${MATCHES[@]}" ; do
              |  echo -e "  $${ITEM}"
              |done
              |exit 1
              |""".stripMargin
      }
    )
  )

  override val elements: List[HeyElement[_]] =
    List(
      SquashCommand,
      CheckoutCommand
    )

}

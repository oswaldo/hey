/*
 * Copyright 2019 Oswaldo C. Dantas Jr
 *
 * SPDX-License-Identifier: MIT
 */

package hey.scope

import hey.cli.Settings.settingsPath
import hey.scope.CommandScope.SwaggerCodegenCli
import hey.scope.Command.Generate
import scopt.{OParser, OParserBuilder}
import java.io._

import hey.util.IOUtil._
import hey.util.ProcessUtil.evalArguments

class SwaggerCodegenCliScope(
    override implicit val scoptBuilder: OParserBuilder[HeyCommandConfig] =
      OParser.builder[HeyCommandConfig]
) extends HeyCommandScope {
  override val scope: String = SwaggerCodegenCli

  override val description: String = "swagger-codegen-cli related commands"

  def swaggerCodegenCliContextMessage(c: HeyCommandConfig) =
    s"generatorName: ${c.generatorName}, inputFile: ${c.inputFile}, outputFolder: ${c.outputFolder}"

  override val validate: HeyCommandConfig => Option[String] = { c =>
    if (c.commandScope == SwaggerCodegenCli && (c.generatorName.isEmpty || c.inputFile.isEmpty || c.outputFolder.isEmpty)) {
      Some(
        s"for swagger-codegen-cli commands, generatorName, inputFile and outputFolder must be defined, through command line options on in the conf file in $settingsPath"
      )
    } else {
      None
    }
  }

  private val GeneratorNameOption = new HeyOption[String](
    name = "generatorName",
    scoptAction = (x, c) => c.copy(generatorName = x),
    abbreviation = Some("gn"),
    description = Some("which generator should I use")
  )

  private val InputFileOption = new HeyOption[String](
    name = "inputFile",
    scoptAction = (x, c) => c.copy(inputFile = x),
    abbreviation = Some("i"),
    description = Some("which swagger file should be used as input")
  )

  private val OutputFolderOption = new HeyOption[String](
    name = "outputFolder",
    scoptAction = (x, c) => c.copy(outputFolder = x),
    abbreviation = Some("o"),
    description = Some(
      "which folder should be used to output the generated files (CAVEAT: this implementation uses docker in a way that the output folder will be in a child of the same folder where the inputFile lives)"
    )
  )

  private val GenerateCommand = new HeyCommand(
    Generate,
    commandAction = c => c.copy(command = Generate),
    description = Some("Generates code from a given swagger inputFile"),
    commandAndArguments = evalArguments(
      _,
      c => {
        val swaggerFilePath = absoluteParentPath(c.inputFile)
        val swaggerFileName = fileName(c.inputFile)
        val generator = c.generatorName
        val output = c.outputFolder
        raw"""docker run --rm -v $swaggerFilePath:/local swaggerapi/swagger-codegen-cli-v3 generate \
         |    -i /local/$swaggerFileName \
         |    -l $generator \
         |    -o /local/$output""".stripMargin
      }
    ),
    contextMessage = swaggerCodegenCliContextMessage
  )

  override val elements: List[HeyElement[_]] =
    List(
      GenerateCommand,
      GeneratorNameOption,
      InputFileOption,
      OutputFolderOption
    )

}

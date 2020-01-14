# hey

[![CircleCI](https://circleci.com/gh/oswaldo/hey.svg?style=svg)](https://circleci.com/gh/oswaldo/hey) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/2fcfcc93e5fc4a3fac50890202e3bcde)](https://www.codacy.com/manual/oswaldodantas/hey?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=oswaldo/hey&amp;utm_campaign=Badge_Grade)

During development there is a whole lot of commands and tools you need to keep track of, with completely different syntaxes between them and sometimes even the same tool has different syntaxes for different operations in the same scope.

Tired of repeatedly looking at sometimes cryptic examples to remember how to do a simple operation while at the end of the day I don't even care about the tool I used to achieve it, I'm starting this pet project to experiment with Scala-native to interface with the different tools, giving them a more sane abstraction level.

The first operations I intend to simplify are to request and change the status of a service running in multiple servers in a non intrusive manner.

For that, I'll be forwarding calls to Ansible. For comparison, this is an ad-hoc way to get the status and restart some service in a server group using Ansible:

```bash
# for the status
ansible someServerGroup -a "systemctl status some-service-name.service"

# for restarting the service
ansible someServerGroup -b -m service -a "name=some-service-name state=restarted"
```

This is already better than a lot we find in the command line world, but still, hard to remember, full of arguments and not really uniform. What I want is something as easy to remember and uniform as this:

```bash
#for the status
hey status -sg someServerGroup -sn some-service-name

# for restarting the service
hey restart -sg someServerGroup -sn some-service-name
```

## Status

Currently the code is in a very initial, prototype, not really working state. The text here is mostly a mix of stuff currently being experimented with and notes about how I got there, so although they worked for me at some point, there is no guarantee that following the steps would lead to any usable result. Still, I would recommend watching this repo as I expect to have something usable soon :)

1. Install Ansible: `sudo apt install ansible`
2. Add server groups to the `/etc/ansible/hosts` file:

   ```yaml
   webservers:
     hosts:
       someHostRange[1:12].some.domain
   ```

3. Install Scala native compilation OS dependencies. We could have an Ansible playbook in the future to install all dependencies but for now there are a couple of manual steps that might be needed (YMMV as development started on machines that already had -many- other dependencies installed, let me know if any message pops-up about missing dependencies besides the ones covered below)
  1. This was needed on an Ubuntu 18.04: `sudo apt install clang libgc-dev libunwind-dev`
  2. Manual build/install is required for this one: <https://github.com/google/re2/wiki/Install>. If you are on macOS (tested with Catalina) and have brew installed, just call `brew install re2`
5. `sbt nativeLink`
6. You can use the natively compiled `hey` tool by calling `target/scala-2.11/hey-out`

In the future, sbt native packager will be used to produce installation packages so the tool can be used with `sudo apt install hey.deb; hey webservers status`

Meanwhile, you could do a link to the target and have it available with a proper name from anywhere. For instance:

```bash
#if didn't have the ~/bin folder before
cd
mkdir bin

#if you are on Ubuntu, you need to reload .profile to be able to call commands from ~/bin
source .profile
#if you use some special OS/shell combination, please refer to its documentation on how would be the recommended approach to this
#with zsh for instance you need to uncomment the line adding ~/bin to the path in .zshrc

#go back to the project folder
cd -
#then link the thing
ln -s target/scala-2.11/hey-out ~/bin/hey
#then `cd` into wherever you want and...
```

```text
hey
Error: at least one of the supported commands should have been called
hey 0.1
Usage: hey [ansible|docker|sbt|git] [options] <args>...

  -vb, --verbosity <value>
                           defaults to full. any other value means silent
Command: ansible [status|restart|stop] [options]
Ansible related commands
  -sg, --serverGroup <value>
                           which servers should I send a command to
  -sn, --serviceName <value>
                           which service should respond to the command
Command: ansible status
Returns the systemd status from those servers
Command: ansible restart
(Re)starts those servers
Command: ansible stop
Stops those servers
Command: docker [bash] [options]
Docker related commands
  -cn, --containerName <value>
                           which container should I execute at
Command: docker bash
Runs bash on the defined containerName
Command: sbt [purge|test] [options] [<suffix>]
Sbt related commands
  -d, --debug              if the process should be started in debug mode
Command: sbt purge
Removes target folders
Command: sbt test
Runs all sbt tests or the ones matching the given suffix
  <suffix>                 This avoids having to use the FQCN, prepending an * to the call
Command: git [squash|checkout] [<targetBranch>] [<targetBranch>]
Git related commands
Command: git squash
Resets index to target branch, allowing all changes to be in a single commit to be done afterwards.
  <targetBranch>           Defaults to master
Command: git checkout
Checks out the given branch name or partial name (if only one match is found)
  <targetBranch>           Defaults to master
You can define default values for command options at the hocon file ~/.hey/hey.conf
```

## Adding new scope and commands

Suppose we want to simplify the use of swagger-codegen-cli in a way that, just calling `hey swagger-codegen-cli generate -gn go-server -i swagger.yaml -o out/gogogo` would be the same as 
```bash
docker run --rm -v /Users/odantas/tmp:/local swaggerapi/swagger-codegen-cli-v3 generate \
     -i /local/swagger.yaml \
     -l go-server \
     -o /local/out/gogogo
```

1. Add a string constant for the new scope to the `CommandScope` object. That will be used for instance as the command name as in `hey swagger-codegen-cli...`, so you would add `al SwaggerCodegenCli = "swagger-codegen-cli"`.

2. If some command argument is probably going to be very repetitive to the user, add support for default values in `Settings`, which can latter be overridden by the user in `~/.hey/hey.conf`

3. Check if `HeyCommandConfig` has the properties needed to hold all arguments for the command. The idea here is that the arguments can be reused between commands, so we could have a same config that in the future could be used in chained commands (not implemented yet, but at some point you could do `hey hey build --inputFile X` and if the context would mean so, take the same file through a series of commands to get to the final result)

4. Check if `Command` object has a command strings that would be appropriate to the goals achieved by the tool. Usually a tool is able to achieve multiple different goals (like a compression tool can be used to compress a file, test it and so on. Sometimes a tool has a default behaviour where no argument is needed (like calling `sbt` enters interactive mode) and for those, a string command would still need to be created (so `sbt` could become something like `hey sbt interactive` and you would have the command object to code some wanted behaviour around that)

5. Create a new class which `extends HeyCommandScope` in the scope package with a meaningful name relating to the project being wrapped. For instance `SwaggerCodegenCliScope`. A good starting point is just taking any of the existing ones as a template.

> This example is starting the tool from a docker container to exemplify that the scope and command names should target the objective of the commands offered, not the means to it.
> It might be that the implementation switches at some point from docker to natively installed or portable or whatever. From this project's user point of view, it should just look stable as `hey do-it-the-best-you-can-and-dont-change-arguments-every-couple-of-releases :)`
> Although it is not required, it is recommended that direct command scopes follow the name of the command being integrated, so if something is `x-y-z-very-long`, that is what you use unless there is a reasoning how to make it slightly different (like dropping some version suffix as the intention is simplify, hide away any complexity not related with the goal of the command, but avoiding to change it so much that the user cannot infer which tool is actually being called)

6. Register the new command by adding a reference to it in the `supportedScopes` list in Main object.

7. `sbt nativeLink`

## Roadmap

### Done

* Useful aliases for ansible, git, sbt and docker
* Architecture to easy the tool extension
* Config file for default option and argument values
* Usage examples

### Short term

* More aliases for supported commands
* "Summary verbosity"
* Architecture documentation
* Giter8 integration and hey scope template

### Middle term

* Installers
* Useful aliases for npm, maven, apt, zip, tar, ssh, cron and other common server and developer tools
* Modularization, so new commands and aliases are realized in external libraries
* Automation for configuring dependencies
* "Configurable scopes" (specific scope values could become values in a boilerplate free/ noise free yaml/hocon/json file)

### Long term

* Command dependency management and composition
* Assistant like behaviour (`hey hey rebuild then redeploy then check and loadTest tomorrow`)

## License

```license
MIT License

Copyright (c) 2019 Oswaldo C. Dantas Jr

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

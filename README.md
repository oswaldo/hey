# hey

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

3. Install Scala native compilation OS packages (tested with Ubuntu): `sudo apt install clang libgc-dev libunwind-dev`
4. We could have an Ansible playbook in the future to install all dependencies but for now, besides the other packages, manual build/install is required for this one: <https://github.com/google/re2/wiki/Install>
5. `sbt nativeLink`
6. You can use the natively compiled `hey` tool by calling `target/scala-2.11/hey-out`

In the future, sbt native packager will be used to produce installation packages so the tool can be used with `sudo apt install hey.deb; hey webservers status`

Meanwhile, you could do a link to the target and have it available with a proper name from anywhere. For instance:

```bash
#if didn't have the ~/bin folder before
cd
mkdir ~/bin
source .profile
#then link the thing
ln target/scala-2.11/hey-out ~/bin/hey
#then `cd` into wherever you want and...
```

```text
  hey
   Error: at least one of the supported commands should have been called
   hey 0.1
   Usage: hey [status|restart|stop] [options]

     -sg, --serverGroup <value>
                              which servers should I send a command to
   Command: status
   Returns the systemd status from those servers
   Command: restart
   (Re)starts those servers
   Command: stop
   Stops those servers
```

## License

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

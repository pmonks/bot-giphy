[![Build Status](https://travis-ci.org/pmonks/bot-giphy.svg?branch=master)](https://travis-ci.org/pmonks/bot-giphy)
[![BCH compliance](https://bettercodehub.com/edge/badge/pmonks/bot-giphy?branch=master)](https://bettercodehub.com/)
[![Open Issues](https://img.shields.io/github/issues/pmonks/bot-giphy.svg)](https://github.com/pmonks/bot-giphy/issues)
[![Dependencies Status](https://versions.deps.co/pmonks/bot-giphy/status.svg)](https://versions.deps.co/pmonks/bot-giphy)
[![Average time to resolve an issue](http://isitmaintained.com/badge/resolution/pmonks/bot-giphy.svg)](http://isitmaintained.com/project/pmonks/bot-giphy "Average time to resolve an issue")
[![License](https://img.shields.io/github/license/pmonks/bot-giphy.svg)](https://github.com/pmonks/bot-giphy/blob/master/LICENSE)

# Giphy bot

A small [Symphony](http://www.symphony.com/) bot that responds to `/giphy` commands in messages to return a random Giphy
image for the subsequent terms.

For example sending the message `/giphy facepalm` to a chat where the bot is present might result in the bot responding
with this image:

![Facepalm](https://media2.giphy.com/media/AjYsTtVxEEBPO/giphy.gif)

## Installation

For now Giphy bot is available in source form only, so fire up your favourite git client and get cloning!

## Configuration

Giphy bot is configured via a single, optional [EDN](https://github.com/edn-format/edn) file that may be specified on the
command line via the "-c" command line option.  You can also provide a "-h" command line option to get help on all of the
command line options the bot supports.

### Configuration File Location and Loading Mechanism

The configuration file is traditionally called `config.edn` (but may be called anything you like) and may be stored anywhere
that can be read by the bot's JVM process via standard POSIX file I/O.  It's loaded using the [aero](https://github.com/juxt/aero)
library - see the [aero documentation](https://github.com/juxt/aero/blob/master/README.md) for details on the various advanced
options aero supports.

The bot ships with a [default `config.edn` file](https://github.com/pmonks/bot-giphy/blob/master/resources/config.edn)
that will be read if a config file is not specified on the command line.  This file delegates basically all configuration to
environment variables, allowing the administrator to deploy and run the bot as a standalone uberjar, and configure it exclusively
from the runtime environment.

Please refer to the [default `config.edn` file](https://github.com/pmonks/bot-giphy/blob/master/resources/config.edn)
for details on using these environment variables.  Their use is not described here.

### A Note on Security

**The bot's configuration includes sensitive information (certificate locations and passwords), so please be extra careful
to secure this configuration, however you choose to manage it (in a file, environment variables, etc.).**

### Configuration File Format

The configuration file is structured as follows:

```edn
{
  :symphony-coords {
    :pod-id           "<id of pod to connect to - will autopopulate whichever of the 4 URLs aren't provided. (optional - see below)>"
    :session-auth-url "<the URL of the session authentication endpoint. (optional - see below)>"
    :key-auth-url     "<The URL of the key authentication endpoint. (optional - see below)>"
    :agent-api-url    "<The URL of the agent API. (optional - see below)>"
    :pod-api-url      "<The URL of the Pod API. (optional - see below)>"
    :trust-store      ["<path to Java truststore>"        "<password of truststore>"]
    :user-cert        ["<path to bot user's certificate>" "<password of bot user's certificate>"]
    :user-email       "<bot user's email address>"
  }
  :jolokia-config {
    "host" "<jolokia-server-host>"
    "port" "<jolokia-server-port-as-a-string>"
  }
  :giphy {
    :api-key    "<Giphy API key>"       ; See https://developers.giphy.com/
    :rating     "<Giphy image rating>"  ; See https://developers.giphy.com/docs/
    :timeout-ms <timeout (in ms) for Giphy API calls>
    :proxy-host "<optional, HTTP proxy hostname>"
    :proxy-port <optional, HTTP proxy port>
  }
  :accept-connections-interval <minutes>    ; Optional - defaults to 30 minutes
  :admin-emails ["user1@domain.tld" "user2@domain.tld"]    ; Optional
}
```

#### :symphony-coords

The coordinates of the various endpoints, certificates, knickknacks and geegaws that the bot needs in order to connect to a
Symphony pod.  This map is passed directly to the
[clj-symphony library's `connect` function](https://symphonyoss.github.io/clj-symphony/clj-symphony.connect.html#var-connect),
and has the same semantics as what's described there.

#### :jolokia-config

The configuration of the [Jolokia](https://jolokia.org/) library, used to support server-side ops monitoring of the bot.
This map is passed directly to Jolokia's [`JolokiaServerConfig` constructor](https://github.com/rhuss/jolokia/blob/master/agent/jvm/src/main/java/org/jolokia/jvmagent/JolokiaServerConfig.java#L92).
See the [default Jolokia property file](https://github.com/rhuss/jolokia/blob/master/agent/jvm/src/main/resources/default-jolokia-agent.properties)
for a full list of the supported configuration options and their default values, and note that all
keys and values in this map MUST be strings (this is a Jolokia requirement).

#### :giphy

Configuration that controls how the bot calls the Giphy APIs.

#### :accept-connections-interval

The interval (in minutes) that the bot will use to check for and accept incoming cross-pod connection requests.  If not
specified, defaults to 30 minutes.

#### :admin-emails

A list of administrator email addresses.  These users will be able to interact with the bot via ChatOps (1:1 chats with the bot
in Symphony).  Administrators should say `help` to the bot to get a list of the available admin commands.

### Logging Configuration

Giphy bot uses the [logback](https://logback.qos.ch/) library for logging, and ships with a
[reasonable default `logback.xml` file](https://github.com/pmonks/bot-giphy/blob/master/resources/logback.xml).
Please review the [logback documentation](https://logback.qos.ch/manual/configuration.html#configFileProperty) if you
wish to override this default logging configuration.

## Usage

For now, you can run Giphy bot either directly or as a Docker image.

### Direct Execution

```
$ lein git-info-edn
$ lein run -- -c <path to EDN configuration file>
```

or

```
$ lein do git-info-edn, uberjar
...
$ java -jar ./target/bot-giphy-standalone.jar -c <path to EDN configuration file>
```

### Dockerised Execution

To build the container:

```
$ docker build -t bot-giphy .
```

To run the container:

```
$ # Interactively:
$ docker run -v /path/to/config/directory:/etc/opt/bot-giphy:ro bot-giphy
$ # In the background:
$ docker run -d -v /path/to/config/directory:/etc/opt/bot-giphy:ro bot-giphy
```

Where `/path/to/config/directory` should be replaced with the fully qualified path of the configuration directory
_on the Docker host_.  This configuration directory must contain:

 1. the service account certificate and truststore that the bot should use
 2. a `config.edn` file (in the format described above), that points to the certificates using `/etc/opt/bot-giphy` as the base path (that's where the configuration folder is mounted _within_ the container)

 And it may optionally also contain:
 1. blacklist files (see above for details)
 2. a logback configuration file

You can also use Docker Compose, by running:

```
$ docker-compose up -d
```

This assumes that the `etc` directory contains the certificate, truststore, and `config.edn` file, as described above.

## Developer Information

[GitHub project](https://github.com/pmonks/bot-giphy)

[Bug Tracker](https://github.com/pmonks/bot-giphy/issues)

<!--
### Branching Structure

This project has two permanent branches called `master` and `dev`.  `master` is a
[GitHub protected branch](https://help.github.com/articles/about-protected-branches/) and cannot be pushed to directly -
all pushes (from project team members) and pull requests (from the wider community) must be made against the `dev`
branch.  The project team will periodically merge outstanding changes from `dev` to `master`.

All commits to the `dev` branch automatically trigger redeployment of the instance of the bot that's configured to run against the
[Foundation's Open Developer Platform (ODP)](https://symphonyoss.atlassian.net/wiki/spaces/FM/pages/37847084/Open+Developer+Platform).
All commits to the `master` branch automatically trigger redeployment of the instance of the bot that's configured to run
against [the Foundation's production pod](https://foundation.symphony.com/).
-->

## License

Copyright 2018 Peter Monks

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

SPDX-License-Identifier: [Apache-2.0](https://spdx.org/licenses/Apache-2.0)

### 3rd Party Licenses

To see the full list of licenses of all third party libraries used by this project, please run:

```shell
$ lein licenses :csv | cut -d , -f3 | sort | uniq
```

To see the dependencies and licenses in detail, run:

```shell
$ lein licenses
```


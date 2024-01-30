| | | | |
|---:|:---:|:---:|:---:|
| [**release**](https://github.com/pmonks/embroidery/tree/release) | [![CI](https://github.com/pmonks/embroidery/actions/workflows/ci.yml/badge.svg?branch=release)](https://github.com/pmonks/embroidery/actions?query=workflow%3ACI+branch%3Arelease) | [![Dependencies](https://github.com/pmonks/embroidery/actions/workflows/dependencies.yml/badge.svg?branch=release)](https://github.com/pmonks/embroidery/actions?query=workflow%3Adependencies+branch%3Arelease) | [![Vulnerabilities](https://github.com/pmonks/embroidery/actions/workflows/vulnerabilities.yml/badge.svg?branch=release)](https://pmonks.github.io/embroidery/nvd/dependency-check-report.html) |
| [**dev**](https://github.com/pmonks/embroidery/tree/dev)  | [![CI](https://github.com/pmonks/embroidery/actions/workflows/ci.yml/badge.svg?branch=dev)](https://github.com/pmonks/embroidery/actions?query=workflow%3ACI+branch%3Adev) | [![Dependencies](https://github.com/pmonks/embroidery/actions/workflows/dependencies.yml/badge.svg?branch=dev)](https://github.com/pmonks/embroidery/actions?query=workflow%3Adependencies+branch%3Adev) | [![Vulnerabilities](https://github.com/pmonks/embroidery/actions/workflows/vulnerabilities.yml/badge.svg?branch=dev)](https://github.com/pmonks/embroidery/actions?query=workflow%3Avulnerabilities+branch%3Adev) |

[![Latest Version](https://img.shields.io/clojars/v/com.github.pmonks/embroidery)](https://clojars.org/com.github.pmonks/embroidery/) [![Open Issues](https://img.shields.io/github/issues/pmonks/embroidery.svg)](https://github.com/pmonks/embroidery/issues) [![License](https://img.shields.io/github/license/pmonks/embroidery.svg)](https://github.com/pmonks/embroidery/blob/dev/LICENSE)

<img alt="embroidery logo: a cross stitch rendition of the Clojure logo" align="right" width="25%" src="embroidery-logo.png">

# embroidery

A micro-library for Clojure that provides versions of `pmap` and `future` that have first class support for virtual threads on JVMs that support them, and which transparently falls back on Clojure core `pmap` and `future` when virtual threads are not supported.  These features are opt-in; this library does _not_ monkey patch core Clojure or mess with the thread pools etc. that it sets up.  It has no dependencies, other than on Clojure and any supported JVM, and is [less than 100 lines of code](https://github.com/pmonks/embroidery/tree/dev/src/embroidery).

Note that Clojure versions prior to 1.12 use `synchronized` blocks in the language and core library, which will reduce performance since that construct pins virtual threads to platform threads (see [JEP-444](https://openjdk.org/jeps/444) and search for the first occurrence of the word "synchronized" for details).  Despite this, performance for I/O bound workloads that leverage virtual threads can be substantially better than the same workload running on platform threads, even on older Clojure versions.

## Installation

`embroidery` is available as a Maven artifact from [Clojars](https://clojars.org/com.github.pmonks/embroidery).

### Trying it Out

#### Clojure CLI

```shell
$ clj -Sdeps '{:deps {com.github.pmonks/embroidery {:mvn/version "RELEASE"}}}'
```

#### Leiningen

```shell
$ lein try com.github.pmonks/embroidery
```

#### deps-try

```shell
$ deps-try com.github.pmonks/embroidery
```

### Demo

Let's use `Thread/sleep` as a reasonable facsimile of a blocking I/O workload.

Setup:

```clojure
(require '[embroidery.api :as e])

(defn blocking-workload [n] (doall (e/pmap* (fn [_] (Thread/sleep 1000)) (range n))))
;=> #'user/blocking-workload

(def cores (.availableProcessors (Runtime/getRuntime)))
;=> #'user/cores
```

On JVMs that support virtual threads (note that the exact results will vary somewhat from run to run and machine to machine):

```clojure
;; First we run as many parallel jobs as there are CPU cores
(let [f (future (time (blocking-workload cores)))]
  (Thread/sleep 250)
  (println "Platform threads:" (count (Thread/getAllStackTraces)))
  @f
  nil)
;=> Platform threads: 11
;=> "Elapsed time: 1005.569084 msecs"
;=> nil

;; Then we run way more parallel jobs than there are CPU cores
(let [f (future (time (blocking-workload (* 1000 cores))))]
  (Thread/sleep 250)
  (println "Platform threads:" (count (Thread/getAllStackTraces)))
  @f
  nil)
;=> Platform threads: 22
;=> "Elapsed time: 1027.093 msecs"
;=> nil
```

On JVMs that don't support virtual threads (where embroidery falls back on using vanilla `clojure.core/pmap`):

```clojure
;; First we run as many parallel jobs as there are CPU cores
(let [f (future (time (blocking-workload cores)))]
  (Thread/sleep 250)
  (println "Platform threads:" (count (Thread/getAllStackTraces)))
  @f
  nil)
;=> Platform threads: 20
;=> "Elapsed time: 1009.354875 msecs"
;=> nil

;; Then we run way more parallel jobs than there are CPU cores
(let [f (future (time (blocking-workload (* 1000 cores))))]
  (Thread/sleep 250)
  (println "Platform threads:" (count (Thread/getAllStackTraces)))
  @f
  nil)
;=> Platform threads: 38
;=> "Elapsed time: 440221.9605 msecs"
;=> nil
```

While it could be argued that this is merely highlighting a limitation of `clojure.core/pmap`'s (i.e. its fixed size thread pool), the reality is that there are good reasons for it to be implemented that way, and the alternatives on JVMs without virtual threads (i.e. spinning up several thousand platform threads for this workload) have substantial downsides and likely still won't perform as well as virtual threads do.

## Usage

[API documentation is available here](https://pmonks.github.io/embroidery/), or [here on cljdoc](https://cljdoc.org/d/com.github.pmonks/embroidery/), and the [unit tests](https://github.com/pmonks/embroidery/blob/dev/test/embroidery/api_test.clj) are also worth perusing to see worked examples.

## Contributor Information

[Contributing Guidelines](https://github.com/pmonks/embroidery/blob/release/.github/CONTRIBUTING.md)

[Bug Tracker](https://github.com/pmonks/embroidery/issues)

[Code of Conduct](https://github.com/pmonks/embroidery/blob/release/.github/CODE_OF_CONDUCT.md)

### Developer Workflow

This project uses the [git-flow branching strategy](https://nvie.com/posts/a-successful-git-branching-model/), and the permanent branches are called `release` and `dev`.  Any changes to the `release` branch are considered a release and auto-deployed (JARs to Clojars, API docs to GitHub Pages, etc.).

For this reason, **all development must occur either in branch `dev`, or (preferably) in temporary branches off of `dev`.**  All PRs from forked repos must also be submitted against `dev`; the `release` branch is **only** updated from `dev` via PRs created by the core development team.  All other changes submitted to `release` will be rejected.

### Build Tasks

`embroidery` uses [`tools.build`](https://clojure.org/guides/tools_build). You can get a list of available tasks by running:

```
clojure -A:deps -T:build help/doc
```

Of particular interest are:

* `clojure -T:build test` - run the unit tests
* `clojure -T:build lint` - run the linters (clj-kondo and eastwood)
* `clojure -T:build ci` - run the full CI suite (check for outdated dependencies, run the unit tests, run the linters)
* `clojure -T:build install` - build the JAR and install it locally (e.g. so you can test it with downstream code)

Please note that the `release` and `deploy` tasks are restricted to the core development team (and will not function if you run them yourself).

## License

Copyright © 2023 Peter Monks

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

SPDX-License-Identifier: [Apache-2.0](https://spdx.org/licenses/Apache-2.0)

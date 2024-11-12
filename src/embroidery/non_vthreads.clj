;
; Copyright © 2023 Peter Monks
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.
;
; SPDX-License-Identifier: MPL-2.0
;

(in-ns 'embroidery.api)

(def virtual-threads-in-use?
  "Are virtual threads in use on this JVM?"
  false)

(def pmap*
  "Version of [pmap](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/pmap)
  which uses JVM 21+ virtual threads when available, one per item in `coll`.

  Notes:

  * degrades to vanilla [pmap](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/pmap)
    on JVMs that don't support virtual threads
  * virtual thread version is _not_ lazy"
  pmap)

(defn bounded-pmap*
  "Version of [pmap](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/pmap)
  which uses JVM 21+ virtual threads when available, but will chunk the work up
  such that at most `n` concurrent virtual threads will be used (useful for
  workloads where system resource constraints could be exceeded e.g. maximum
  number of open file handles).

  Notes:

  * degrades to vanilla [pmap](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/pmap)
    on JVMs that don't support virtual threads
  * virtual thread version is partially lazy (results are computed eagerly, but
    merged lazily using [concat](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/concat))
  * non virtual thread version ignores the `n` argument (since [pmap](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/pmap)
    already chunks `coll`)
  * each invocation of `bounded-pmap*` utilises an independent set of virtual
    threads, so parallel invocations may exceed system resource constraints"
  [_ f coll]
  (pmap f coll))

(def future-call*
  "Version of [future-call](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/future-call)
  that uses JVM 21+ virtual threads when available.

  Notes:

  * degrades to vanilla [future-call](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/future-call)
    on JVMs that don't support virtual threads"
  future-call)

(defmacro future*
  "Version of [future](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/future)
  which uses JVM 21+ virtual threads when available.

  Notes:

  * degrades to vanilla [future](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/future)
    on JVMs that don't support virtual threads"
  [& body]
  `(future ~@body))

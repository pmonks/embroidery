;
; Copyright © 2023 Peter Monks
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
;
; SPDX-License-Identifier: Apache-2.0
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

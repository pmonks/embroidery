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
  true)

(def ^:private embroidery-vthread-factory (delay (-> (Thread/ofVirtual)
                                                     (.name "embroidery-virtual-thread-" 0)
                                                     (.factory))))

(defn- new-vthread-executor
  "Create a new virtual thread per task executor."
  ^java.util.concurrent.ExecutorService []
  (java.util.concurrent.Executors/newThreadPerTaskExecutor @embroidery-vthread-factory))

(defn pmap*
  "Version of [pmap](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/pmap)
  which uses JVM 21+ virtual threads when available, one per item in `coll`.

  Notes:

  * degrades to vanilla [pmap](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/pmap)
    on JVMs that don't support virtual threads
  * virtual thread version is _not_ lazy"
  [f coll]
  (let [executor (new-vthread-executor)
        futures  (mapv #(.submit executor (reify java.util.concurrent.Callable (call [_] (f %)))) coll)
        ret      (mapv #(.get ^java.util.concurrent.Future %) futures)]
    (.shutdownNow executor)
    (sequence ret)))   ; Note: we don't use `seq` here, since we don't want nil punning when `ret` is empty

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
    chunks `coll` itself, based on the number of CPUs)
  * each invocation of `bounded-pmap*` utilises an independent set of virtual
    threads, so parallel invocations may exceed system resource constraints"
  [n f coll]
  (let [chunk-size (max 1 (int (Math/ceil (/ (count coll) n))))  ; clojure.math/ceil only added in Clojure 1.11
        chunks     (partition-all chunk-size coll)
        results    (pmap* #(mapv f %) chunks)]
    (apply concat results)))

(def ^:private future-vthread-executor (delay (new-vthread-executor)))

(defn future-call*
  "Version of [future-call](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/future-call)
  that uses JVM 21+ virtual threads when available.

  Notes:

  * degrades to vanilla [future-call](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/future-call)
    on JVMs that don't support virtual threads"
  [f]
  (let [f   ^clojure.lang.IFn (#'clojure.core/binding-conveyor-fn f)
        fut ^java.util.concurrent.Future (.submit ^java.util.concurrent.ExecutorService @future-vthread-executor ^Callable f)]
    (reify
     clojure.lang.IDeref
     (deref [_] (#'clojure.core/deref-future fut))
     clojure.lang.IBlockingDeref
     (deref
      [_ timeout-ms timeout-val]
      (#'clojure.core/deref-future fut timeout-ms timeout-val))
     clojure.lang.IPending
     (isRealized [_] (.isDone fut))
     java.util.concurrent.Future
      (get [_] (.get fut))
      (get [_ timeout unit] (.get fut timeout unit))
      (isCancelled [_] (.isCancelled fut))
      (isDone [_] (.isDone fut))
      (cancel [_ interrupt?] (.cancel fut interrupt?)))))

(defmacro future*
  "Version of [future](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/future)
  which uses JVM 21+ virtual threads when available.

  Notes:

  * degrades to vanilla [future](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/future)
    on JVMs that don't support virtual threads"
  [& body]
  `(future-call* (^{:once true} fn* [] ~@body)))

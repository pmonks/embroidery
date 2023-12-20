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

(def ^:private embroidery-vthread-factory (delay (-> (Thread/ofVirtual)
                                                     (.name "embroidery-virtual-thread-" 0)
                                                     (.factory))))

(defn- new-vthread-executor
  "Create a new virtual thread per task executor."
  ^java.util.concurrent.ExecutorService []
  (java.util.concurrent.Executors/newThreadPerTaskExecutor @embroidery-vthread-factory))

(defn pmap*
  "Version of clojure.core/pmap which uses JDK 21+ virtual threads when available."
  [f coll]
  (let [executor (new-vthread-executor)
        futures  (mapv #(.submit executor (reify java.util.concurrent.Callable (call [_] (f %)))) coll)
        ret      (mapv #(.get ^java.util.concurrent.Future %) futures)]
    (.shutdownNow executor)
    ret))

(def ^:private future-vthread-executor (delay (new-vthread-executor)))

(defn- future-call*
  "Version of future-call that uses JDK 21+ virtual threads."
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

(defn future*
  "Version of clojure.core/future which uses JDK 21+ virtual threads when available."
  [& body] `(future-call* (^{:once true} fn* [] ~@body)))

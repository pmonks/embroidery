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

(ns embroidery.api-test
  (:require [clojure.test   :refer [deftest testing is]]
            [embroidery.api :refer [virtual-threads-in-use? pmap* bounded-pmap* future*]]))

(if virtual-threads-in-use?
  (println "✅ Virtual threads supported on this JVM")
  (println "❎ Virtual threads not supported on this JVM"))

(defn valid=
  [expected actual]
  (and (seq? actual)
       (= expected actual)))

(def alphabet [:a :b :c :d :e :f :g :h :i :j :k :l :m :n :o :p :q :r :s :t :u :v :w :x :y :z])

(defn- now-ms
  []
  (System/currentTimeMillis))

(defn- time-fn
  "Runs f, returning how long (in ms) it took to execute. The results of f are
  discarded, so it should internally de-lazy any lazy results if those need to
  be realised in order to determine an accurate runtime."
  [f]
  (let [start (now-ms)]
    (f)
    (- (now-ms) start)))

(defn- slow-identity
  "clojure.core/identity, but takes ~100ms"
  [x]
  (Thread/sleep 100)
  x)

(deftest pmap*-tests
  (testing "nil, empty input"
    (is (valid= '() (pmap* nil nil)))
    (is (valid= '() (pmap* nil '())))
    (is (valid= '() (pmap* nil [])))
    (is (valid= '() (pmap* identity nil)))
    (is (valid= '() (pmap* identity '())))
    (is (valid= '() (pmap* identity []))))
  (testing "non-empty input"
    (is (valid= '(:a) (pmap* identity [:a])))
    (is (valid= '(:a) (pmap* identity '(:a))))
    (is (valid= '(1 2 3 4 5 6 7 8 9 10) (pmap* inc (range 10)))))
  (when virtual-threads-in-use?
    (testing "virtual thread run times"
      (is (<= (time-fn #(doall (pmap* slow-identity alphabet))) 200))          ; This should run in 100ms + vthread overhead
      (is (<= (time-fn #(doall (pmap* slow-identity (range 10000)))) 200)))))  ; This should also run in 100ms + vthread overhead

(deftest bounded-pmap*-tests
  (testing "nil, empty input"
    (is (valid= '() (bounded-pmap* 1 nil nil)))
    (is (valid= '() (bounded-pmap* 2 nil '())))
    (is (valid= '() (bounded-pmap* 3 nil [])))
    (is (valid= '() (bounded-pmap* 4 identity nil)))
    (is (valid= '() (bounded-pmap* 5 identity '())))
    (is (valid= '() (bounded-pmap* 6 identity []))))
  (testing "non-empty input"
    (is (valid= '(:a) (bounded-pmap* 100 identity [:a])))
    (is (valid= '(:a) (bounded-pmap* 100 identity '(:a))))
    (is (valid= '(1 2 3 4 5 6 7 8 9 10) (bounded-pmap* 1     inc (range 10))))
    (is (valid= '(1 2 3 4 5 6 7 8 9 10) (bounded-pmap* 3     inc (range 10))))
    (is (valid= '(1 2 3 4 5 6 7 8 9 10) (bounded-pmap* 10    inc (range 10))))
    (is (valid= '(1 2 3 4 5 6 7 8 9 10) (bounded-pmap* 10000 inc (range 10))))
    (is (valid= '([:a] [:b])            (bounded-pmap* 100   #(identity [%]) [:a :b]))))
  (when virtual-threads-in-use?
    (testing "virtual thread run times"
      (is (<= (time-fn #(doall (bounded-pmap*   26 slow-identity alphabet)))       200))     ; This should run in 100ms + vthread overhead
      (is (<= (time-fn #(doall (bounded-pmap*   13 slow-identity alphabet)))       300))     ; This should run in 200ms + vthread overhead
      (is (<= (time-fn #(doall (bounded-pmap*    7 slow-identity alphabet)))       500))     ; This should run in 400ms + vthread overhead
      (is (<= (time-fn #(doall (bounded-pmap* 1000 slow-identity (range 10000)))) 2000)))))  ; This should run in 1000ms + vthread overhead

(deftest future*-tests
  (testing "empty input"
    (is (not (nil? (future*))))
    (is (nil? @(future*)))
    (is (nil? @(future* nil))))
  (testing "non-empty input"
    (is (= :a @(future* :a)))
    (is (= '(1 2 3 4 5 6 7 8 9 10) @(future* (map inc (range 10))))))
  (testing "timeouts"
    (is (= :timed-out     (deref (future* (Thread/sleep 100) :not-timed-out) 10  :timed-out)))
    (is (= :not-timed-out (deref (future* (Thread/sleep 10)  :not-timed-out) 100 :timed-out)))))

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
            [embroidery.api :refer [pmap* future*]]))

(defn- valid=
  [expected actual]
  (and (seq? actual)
       (= expected actual)))

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
    (is (valid= '(1 2 3 4 5 6 7 8 9 10) (pmap* inc (range 10))))))

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

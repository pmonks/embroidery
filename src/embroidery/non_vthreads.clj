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

; Fallback on vanilla pmap
(def pmap*
  "Version of clojure.core/pmap which uses JDK 21+ virtual threads when available.

Note: virtual thread version is _not_ lazy."
     pmap)

; Fallback on vanilla future-call
(def future-call*
  "Version of clojure.core/future-call that uses JDK 21+ virtual threads when available."
  future-call)

; Fallback on vanilla future
(defmacro future*
  "Version of clojure.core/future which uses JDK 21+ virtual threads when available."
  [& body]
  `(future ~@body))

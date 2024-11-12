;
; Copyright © 2023 Peter Monks
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.
;
; SPDX-License-Identifier: MPL-2.0
;

#_{:clj-kondo/ignore [:unresolved-namespace]}
(defn set-opts
  [opts]
  (assoc opts
         :lib          'com.github.pmonks/embroidery
         :version      (pbr/calculate-version 1 0)
         :prod-branch  "release"
         :write-pom    true
         :validate-pom true
         :pom          {:description      "A Clojure micro-library for leveraging virtual threads on JVMs that support them."
                        :url              "https://github.com/pmonks/embroidery"
                        :licenses         [:license   {:name "MPL-2.0" :url "https://www.mozilla.org/en-US/MPL/2.0/"}]
                        :developers       [:developer {:id "pmonks" :name "Peter Monks" :email "pmonks+embroidery@gmail.com"}]
                        :scm              {:url                  "https://github.com/pmonks/embroidery"
                                           :connection           "scm:git:git://github.com/pmonks/embroidery.git"
                                           :developer-connection "scm:git:ssh://git@github.com/pmonks/embroidery.git"
                                           :tag                  (tc/git-tag-or-hash)}
                        :issue-management {:system "github" :url "https://github.com/pmonks/embroidery/issues"}}
         :codox        {:namespaces ['embroidery.api]
                        :metadata   {:doc/format :markdown}}
         :eastwood     {:exclude-linters [:unused-ret-vals-in-try :no-ns-form-found]}))

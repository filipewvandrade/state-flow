(ns state-flow.cljtest
  (:require [cats.core :as m]
            [clojure.test :as ctest :refer [is]]
            [matcher-combinators.test]
            [matcher-combinators.core :as matcher-combinators]
            [state-flow.core :as core]
            [state-flow.state :as state]))

(defn ^:private match-expr
  [desc value checker]
  (let [test-name (symbol (clojure.string/replace desc " " "-"))]
    (list `ctest/testing desc (list `is (list 'match? checker value)))))

(defmacro match+meta
  [desc value checker meta]
  (with-meta (match-expr desc value checker) meta))

(defn match-probe
  ([state matcher params]
   (m/fmap second
           (core/probe state
                       #(matcher-combinators/match? (matcher-combinators/match matcher %))
                       params)))
  ([state matcher]
   (match-probe state matcher {})))

(defmacro match?
  "Builds a clojure.test assertion using matcher combinators"
  [desc value checker & forms]
  (let [the-meta (meta &form)
        params   (if (map? (first forms)) (first forms) {})]
    `(core/flow ~desc
       [full-desc# (core/get-description)]
       (if (state/state? ~value)
         (m/mlet [extracted-value# (match-probe ~value ~checker ~params)]
           (state/wrap-fn #(do (match+meta full-desc# extracted-value# ~checker ~the-meta)
                               extracted-value#)))
         (state/wrap-fn #(do (match+meta full-desc# ~value ~checker ~the-meta)
                             ~value))))))

(defmacro defflow
  {:arglists '([name & flows]
               [name parameters & flows])}
  [name & forms]
  (let [[parameters & flows] (if (map? (first forms))
                               forms
                               (cons {} forms))
        wrapper              (or (:wrapper-fn parameters)
                                 (fn [flow] (flow)))
        flow-parameters      (dissoc parameters :wrapper-fn)]
    `(ctest/deftest ~name
       (~wrapper (core/run* ~flow-parameters (core/flow ~(str name) ~@flows))))))
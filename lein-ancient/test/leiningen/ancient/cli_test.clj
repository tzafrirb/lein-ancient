(ns leiningen.ancient.cli-test
  (:require [midje.sweet :refer :all]
            [leiningen.ancient.cli :refer :all]))

(tabular
  (fact "about the default settings"
        (let [[flags rst] (parse [])]
          rst => empty?
          flags => map?
          (flags ?k) => ?v))
  ?k                ?v
  :check-clojure?   false
  :dependencies?    true
  :plugins?         false
  :profiles?        true
  :java-agents?     true
  :qualified?       false
  :snapshots?       false
  :interactive?     false
  :colors?          true
  :print?           false
  :recursive?       false
  :only             []
  :exclude          []
  :tests?           true)

(defmacro with-logging
  [b & body]
  `(binding [leiningen.core.main/*info* ~b
             *err* *out*]
     ~@body))

(fact "about unknown flags."
      (with-logging false
        (parse [":unknown"]))
      => (throws
           clojure.lang.ExceptionInfo
           #"not recognized"))

(fact "about deprecated flags."
      (with-out-str
        (with-logging true
          (parse [":get"])))
      => #"no longer supported")

(fact "about unapplicable flags."
      (with-out-str
        (with-logging true
          (parse [":print"] :exclude [:print])))
      => #"not applicable")

(tabular
  (fact "about rest arguments."
        (let [[_ rst] (parse ?args)]
          rst => ?rst))
  ?args                      ?rst
  ["hello"]                  ["hello"]
  ["hello" "world"]          ["hello" "world"]
  ["--" "hello"]             ["hello"]
  [":print" "hello" "world"] ["hello" "world"]
  [":print" "--" ":print"]   [":print"])

(let [defaults (first (parse []))
      dis (partial apply dissoc)]
  (tabular
    (fact "about flag effects"
          (let [[flags _] (parse ?args)
                ks (keys ?effects)]
            (dis flags ks) => (dis defaults ks)
            (select-keys flags ks) => ?effects))
    ?args                    ?effects
    [":all"]                 {:dependencies? true, :plugins? true,
                              :java-agents? true}
    [":allow-all"]           {:snapshots? true, :qualified? true}
    [":allow-snapshots"]     {:snapshots? true}
    [":allow-qualified"]     {:qualified? true}
    [":check-clojure"]       {:check-clojure? true}
    [":interactive"]         {:interactive? true}
    [":plugins"]             {:dependencies? false, :plugins? true,
                              :java-agents? false}
    [":all" ":plugins"]      {:dependencies? true, :plugins? true,
                              :java-agents? true}
    [":plugins" ":all"]      {:dependencies? true, :plugins? true,
                              :java-agents? true}
    [":java-agents"]         {:dependencies? false, :plugins? false,
                              :java-agents? true}
    [":no-colors"]           {:colors? false}
    [":no-colours"]          {:colors? false}
    [":no-profiles"]         {:profiles? false}
    [":no-tests"]            {:tests? false}
    [":tests"]               {:tests? true}
    [":print"]               {:print? true, :tests? false}
    [":print" ":tests"]      {:print? true, :tests? true}
    [":tests" ":print"]      {:print? true, :tests? true}
    [":recursive"]           {:recursive? true}))

(let [defaults (first (parse []))
      dis (partial apply dissoc)]
  (tabular
    (fact "about setting effects"
          (let [[flags args] (parse ?args)
                ks (keys ?effects)]
            (dis flags ks) => (dis defaults ks)
            (select-keys flags ks) => ?effects
            args => empty?))
    ?args                    ?effects
    [":only" "a,b"]          {:only [:a :b]}
    [":exclude" "x,y"]       {:exclude [:x :y]}))

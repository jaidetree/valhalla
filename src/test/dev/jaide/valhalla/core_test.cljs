(ns dev.jaide.valhalla.core-test
  (:require
   [cljs.test :refer [deftest testing is]]
   [dev.jaide.valhalla.core :as v]
   [dev.jaide.valhalla.context :as ctx]))

(deftest ok-test
  (testing "Ok values"
    (is (= (v/ok "a-value")
           [:v/ok "a-value"]))))

(deftest ok?-test
  (testing "returns true on ok values"
    (is (= (v/ok? (v/ok "good"))
           true)))
  (testing "returns false on not-ok values"
    (is (= (v/ok? (v/error "Bad value"))
           false))))

(deftest error-test
  (testing "Error values"
    (is (= (v/error "No good")
           [:v/error "No good"]))))

(deftest validate-test
  (testing "validate"
    (testing "passes on valid input"
      (let [res (v/validate (v/string) "test")]
        (is (= (:status res) :v/pass))
        (is (= (:input res) "test"))
        (is (= (:output res) "test"))))

    (testing "fails on invalid input"
      (let [res (v/validate (v/string) 5)]
        (is (= (:status res) :v/fail))
        (is (= (:input res) 5))
        (is (= (:output res) nil))
        (is (= (:errors res) [{:path []
                               :message "Expected string, got 5"}])))))

  (testing "throws with invalid validator arg"
    (is (thrown? :default (v/validate nil 5)))))

(deftest assert-valid-test
  (testing "assert-valid"
    (testing "returns result when valid"
      (let [res (v/assert-valid (v/number) 5)]
        (is (= (:status res) :v/pass))
        (is (= (:errors res) nil))
        (is (= (:output res) 5))))
    (testing "throws when invalid"
      (is (thrown? :default (v/assert-valid
                             (v/vector-tuple
                              [(v/number) (v/keyword) (v/string)])
                             ["str" 5 :kw]))))))

(deftest errors->string-test
  (testing "Formats errors to string"
    (is (= (v/errors->string
            [{:path [:a] :message "Error 1"}
             {:path [:b] :message "Error 2"}
             {:path [:c] :message "Error 3"}])
           "a: Error 1\nb: Error 2\nc: Error 3"))))

(deftest string-test
  (testing "String passes on string inputs"
    (is (= ((v/string) (ctx/create :value "a-string" :input "a-string"))
           [:v/ok "a-string"])))

  (testing "String fails on non-string inputs"
    (testing "> nil"
      (is (= ((v/string) (ctx/create :input "test" :value nil))
             [:v/error "Expected string, got nil"])))
    (testing "> number"
      (is (= ((v/string) (ctx/create :input 5))
             [:v/error "Expected string, got 5"]))))

  (testing "Supports custom messages"
    (testing "Static string"
      (is (= ((v/string {:message "Value is invalid"})
              (ctx/create :input "test" :value nil))
             [:v/error "Value is invalid"])))
    (testing "functions"
      (is (= ((v/string {:message (fn [{:keys [value]}]
                                    (str "Invalid " (pr-str value)))})
              (ctx/create :input "test" :value nil))
             [:v/error "Invalid nil"])))))

(deftest number-test
  (testing "Number passes on number inputs"
    (is (= ((v/number) (ctx/create :value 5 :input 5))
           [:v/ok 5])))

  (testing "Number fails on non-number inputs"
    (testing "> nil"
      (is (= ((v/number) (ctx/create :input nil :value nil))
             [:v/error "Expected number, got nil"])))
    (testing "> string"
      (is (= ((v/number) (ctx/create :input "test"))
             [:v/error "Expected number, got \"test\""])))))

(deftest numeric-test
  (testing "Number passes on numeric strings inputs"
    (is (= ((v/numeric) (ctx/create :value "5" :input "5"))
           [:v/ok "5"])))

  (testing "Numeric fails on non-numeric strings"
    (testing "> nil"
      (is (= ((v/numeric) (ctx/create :input nil :value nil))
             [:v/error "Expected numeric string, got nil"])))
    (testing "> string"
      (is (= ((v/numeric) (ctx/create :input "test"))
             [:v/error "Expected numeric string, got \"test\""])))))

(deftest string->number-test
  (testing "string->number parses numeric strings inputs"
    (is (= ((v/string->number) (ctx/create :value "5" :input "5"))
           [:v/ok 5])))

  (testing "string->number fails on non-numeric strings"
    (testing "> nil"
      (is (= ((v/string->number) (ctx/create :input nil :value nil))
             [:v/error "Expected numeric string, got nil"])))
    (testing "> string"
      (is (= ((v/string->number) (ctx/create :input "test"))
             [:v/error "Expected numeric string, got \"test\""])))))

(deftest boolean-test
  (testing "Parses boolean inputs"
    (testing "> true"
      (is (= ((v/boolean) (ctx/create :value true :input true))
             [:v/ok true])))

    (testing "> false"
      (is (= ((v/boolean) (ctx/create :value false :input false))
             [:v/ok false]))))

  (testing "Boolean fails on non-boolean inputs"
    (testing "> nil"
      (is (= ((v/boolean) (ctx/create :input nil :value nil))
             [:v/error "Expected boolean, got nil"])))
    (testing "> string"
      (is (= ((v/boolean) (ctx/create :input "test"))
             [:v/error "Expected boolean, got \"test\""])))))

(deftest string->boolean-test
  (testing "Parses boolean string inputs"
    (testing "> true"
      (is (= ((v/string->boolean) (ctx/create :value "true"))
             [:v/ok true])))

    (testing "> false"
      (is (= ((v/string->boolean) (ctx/create :value "false"))
             [:v/ok false]))))

  (testing "string->boolean fails on non-boolean inputs"
    (testing "> nil"
      (is (= ((v/string->boolean) (ctx/create :value nil))
             [:v/error "Expected boolean-string, got nil"])))
    (testing "> string"
      (is (= ((v/string->boolean) (ctx/create :value "test"))
             [:v/error "Expected boolean-string, got \"test\""])))))

(deftest keyword-test
  (testing "Validates keyword inputs"
    (is (= ((v/keyword) (ctx/create :value :test-value))
           [:v/ok :test-value])))

  (testing "keyword fails on non-keyword inputs"
    (testing "> nil"
      (is (= ((v/keyword) (ctx/create :value nil))
             [:v/error "Expected keyword, got nil"])))
    (testing "> string"
      (is (= ((v/keyword) (ctx/create :value "test"))
             [:v/error "Expected keyword, got \"test\""])))))

(deftest string->keyword-test
  (testing "Parses keyword-string inputs"
    (testing "with : prefix"
      (is (= ((v/string->keyword) (ctx/create :value ":test-value"))
             [:v/ok :test-value])))
    (testing "without : prefix"
      (is (= ((v/string->keyword) (ctx/create :value "test-value"))
             [:v/ok :test-value])))
    (testing "with namespace prefix"
      (is (= ((v/string->keyword) (ctx/create :value "v/test-value"))
             [:v/ok :v/test-value]))))

  (testing "string->keyword fails invalid input"
    (testing "> nil"
      (is (= ((v/string->keyword) (ctx/create :value nil))
             [:v/error "Expected keyword-string, got nil"])))
    (testing "> number"
      (is (= ((v/string->keyword) (ctx/create :value 5))
             [:v/error "Expected keyword-string, got 5"])))
    (testing "> invalid strings"
      (is (= ((v/string->keyword) (ctx/create :value "hello world"))
             [:v/error "Expected keyword-string, got \"hello world\""])))))

(deftest symbol-test
  (testing "Validates symbol inputs"
    (is (= ((v/symbol) (ctx/create :value (symbol "test-value")))
           [:v/ok (symbol "test-value")])))

  (testing "symbol fails invalid input"
    (testing "> nil"
      (is (= ((v/keyword) (ctx/create :value nil))
             [:v/error "Expected keyword, got nil"])))
    (testing "> string"
      (is (= ((v/keyword) (ctx/create :value "test"))
             [:v/error "Expected keyword, got \"test\""])))))

(deftest string->symbol-test
  (testing "Parses symbol-string inputs"
    (testing "without namespace"
      (is (= ((v/string->symbol) (ctx/create :value "test-value"))
             [:v/ok 'test-value])))
    (testing "with namespace"
      (is (= ((v/string->symbol) (ctx/create :value "some-ns/a-symbol"))
             [:v/ok 'some-ns/a-symbol]))))

  (testing "string->keyword fails invalid input"
    (testing "> nil"
      (is (= ((v/string->symbol) (ctx/create :value nil))
             [:v/error "Expected symbol-string, got nil"])))
    (testing "> number"
      (is (= ((v/string->symbol) (ctx/create :value 5))
             [:v/error "Expected symbol-string, got 5"])))
    (testing "> invalid strings"
      (is (= ((v/string->symbol) (ctx/create :value "hello world"))
             [:v/error "Expected symbol-string, got \"hello world\""])))))

(deftest regex-test
  (testing "regex validates matching inputs"
    (is (= ((v/regex "[-a-z0-9]+") (ctx/create :value "test-value"))
           [:v/ok "test-value"])))

  (testing "regex fails invalid input"
    (testing "> nil"
      (is (= ((v/regex "[-a-z]+") (ctx/create :value nil))
             [:v/error "Expected string matching [-a-z]+, got nil"])))
    (testing "> non-matching string"
      (is (= ((v/regex "[-a-z]+") (ctx/create :value "test99"))
             [:v/error "Expected string matching [-a-z]+, got \"test99\""])))))

(deftest uuid-test
  (testing "uuid validates uuid inputs"
    (is (= ((v/uuid) (ctx/create :value "a5cdc5f3-b23c-4e80-a7e5-00e5f60a190d"))
           [:v/ok "a5cdc5f3-b23c-4e80-a7e5-00e5f60a190d"])))

  (testing "uuid fails invalid input"
    (testing "> nil"
      (is (= ((v/uuid) (ctx/create :value nil))
             [:v/error "Expected UUID string, got nil"])))
    (testing "> non-matching string"
      (is (= ((v/uuid) (ctx/create :value "test"))
             [:v/error "Expected UUID string, got \"test\""])))))

(deftest nil-test
  (testing "nil"
    (testing "validates nil inputs"
      (is (= ((v/nil-value) (ctx/create :value nil))
             [:v/ok nil])))

    (testing "fails invalid input"
      (testing "> number"
        (is (= ((v/nil-value) (ctx/create :value 5))
               [:v/error "Expected nil, got 5"])))
      (testing "> string"
        (is (= ((v/nil-value) (ctx/create :value "test"))
               [:v/error "Expected nil, got \"test\""]))))))

(deftest vector-test
  (testing "vector"
    (testing "validates vectors with same type"
      (is (= ((v/vector (v/number))
              (ctx/create :value [1 2 3 4 5]))
             [:v/ok [1 2 3 4 5]])))
    (testing "fails invalid input"
      (testing "> mixed types"
        (is (= ((v/vector (v/number)) (ctx/create :value [:kw "str" 'sym nil]))
               [:v/errors [{:path [0] :message "Expected number, got :kw"}
                           {:path [1] :message "Expected number, got \"str\""}
                           {:path [2] :message "Expected number, got sym"}
                           {:path [3] :message "Expected number, got nil"}]]))))))

(deftest vector-tuple-test
  (testing "vector-tuple"
    (testing "validates tuple-like vectors"
      (is (= ((v/vector-tuple
               [(v/number)
                (v/keyword)]) (ctx/create :value [5 :test]))
             [:v/ok [5 :test]])))
    (testing "fails invalid input"
      (testing "> uneven forms"
        (is (= ((v/vector-tuple
                 [(v/number)
                  (v/keyword)]) (ctx/create :value [5 :test "hello"]))
               [:v/error "Expected vector-tuple of length 2, got [5 :test \"hello\"]"])))
      (testing "> invalid input"
        (is (= ((v/vector-tuple
                 [(v/number)
                  (v/keyword)
                  (v/string)]) (ctx/create :value ["str" 500 :kw]))
               [:v/errors [{:path [0] :message "Expected number, got \"str\""}
                           {:path [1] :message "Expected keyword, got 500"}
                           {:path [2] :message "Expected string, got :kw"}]]))))))

(deftest list-test
  (testing "list"
    (testing "validates lists with same type")
    (testing "fails invalid input"
      (testing "> mixed types"
        (is (= ((v/list (v/number)) (ctx/create :value '(:kw "str" 'sym nil)))
               [:v/errors [{:path [0] :message "Expected number, got :kw"}
                           {:path [1] :message "Expected number, got \"str\""}
                           {:path [2] :message "Expected number, got (quote sym)"}
                           {:path [3] :message "Expected number, got nil"}]]))))))

(deftest list-tuple-test
  (testing "list-tuple"
    (testing "validates mixed tuples"
      (is (= ((v/list-tuple
               (list (v/number) (v/keyword)))
              (ctx/create :value '(5 :test)))
             [:v/ok [5 :test]])))
    (testing "fails invalid input"
      (testing "> uneven forms"
        (is (= ((v/list-tuple
                 (list (v/number) (v/keyword)))
                (ctx/create :value '(5 :test "hello")))
               [:v/error "Expected list-tuple of length 2, got (5 :test \"hello\")"])))
      (testing "> invalid input"
        (is (= ((v/list-tuple
                 (list (v/number) (v/keyword) (v/string)))
                (ctx/create :value '("str" 500 :kw)))
               [:v/errors [{:path [0] :message "Expected number, got \"str\""}
                           {:path [1] :message "Expected keyword, got 500"}
                           {:path [2] :message "Expected string, got :kw"}]]))))))

(deftest set-test
  (testing "set"
    (testing "validates sets with same type"
      (is (= ((v/set (v/number))
              (ctx/create :value #{1 2 3 4 5}))
             [:v/ok #{1 2 3 4 5}])))

    (testing "fails invalid input"
      (testing "> mixed types"
        (is (= ((v/list (v/number)) (ctx/create :value '(:kw "str" 'sym nil)))
               [:v/errors [{:path [0] :message "Expected number, got :kw"}
                           {:path [1] :message "Expected number, got \"str\""}
                           {:path [2] :message "Expected number, got (quote sym)"}
                           {:path [3] :message "Expected number, got nil"}]]))))))

(deftest hash-map-test
  (testing "hash-map"
    (testing "passes"
      (testing "valid key and value validators"
        (is (= ((v/hash-map (v/keyword) (v/number))
                (ctx/create :value {:a 1
                                    :b 2
                                    :c 3}))
               [:v/ok {:a 1
                       :b 2
                       :c 3}])))
      (testing "valid value validator"
        (is (= ((v/hash-map (v/number))
                (ctx/create :value {:a 1
                                    :b 2
                                    :c 3}))
               [:v/ok {:a 1
                       :b 2
                       :c 3}]))))
    (testing "fails"
      (testing "invalid hash-map value"
        (is (= ((v/hash-map (v/keyword) (v/number))
                (ctx/create :value
                            (seq {:a 1
                                  :b 2
                                  :c 3})))
               [:v/error "Expected hash-map, got ([:a 1] [:b 2] [:c 3])"])))
      (testing "invalid sub-validator input"
        (is (= ((v/hash-map (v/number))
                (ctx/create :value {1 :a
                                    2 :b
                                    3 :c}))
               [:v/errors
                [{:path [0 0] :message "Expected keyword, got 1"}
                 {:path [0 1] :message "Expected number, got :a"}
                 {:path [1 0] :message "Expected keyword, got 2"}
                 {:path [1 1] :message "Expected number, got :b"}
                 {:path [2 0] :message "Expected keyword, got 3"}
                 {:path [2 1] :message "Expected number, got :c"}]]))))))

(deftest record-test
  (testing "record"
    (testing "passes"
      (testing "valid record values"
        (let [res ((v/record
                    {:a (v/string)
                     :b (v/number)
                     :c (v/string->number)})
                   (ctx/create
                    :input {:a "str"
                            :b 5
                            :c "45.5"}))]
          (is (= res [:v/ok {:a "str"
                             :b 5
                             :c 45.5}]))))

      (testing "nested records"
        (let [res ((v/record
                    {:a (v/record {:b (v/string)})})
                   (ctx/create :input {:a {:b "str"}}))]
          (is (= res [:v/ok {:a {:b "str"}}]))))

      (testing "Supports hash-map with record keys"
        (is (= ((v/record {:test (v/hash-map (v/record {:a (v/number)})
                                             (v/keyword))})
                (ctx/create :value {:test {{:a 1} :b}}))
               [:v/ok {:test {{:a 1} :b}}]))))

    (testing "fails"
      (testing "throws error if not given a map"
        (is (thrown? :default (v/validate (v/record :other) {:a 1 :b 2}))))
      (testing "non-hash-map value"
        (let [res (v/validate
                   (v/record {:a (v/string)
                              :b (v/number)
                              :c (v/string->number)})
                   nil)]
          (is (= (:status res) :v/fail))
          (is (= (:errors res) [{:path [] :message "Expected hash-map record, got nil"}]))
          (is (= (:output res) nil))))

      (testing "collects all errors"
        (let [res (v/validate
                   (v/record {:a (v/string)
                              :b (v/number)
                              :c (v/string->number)})
                   {:a 5
                    :b "5"
                    :c "abc"})]
          (is (= (:status res) :v/fail))
          (is (= (:errors res) [{:path [:a] :message "Expected string, got 5"}
                                {:path [:b] :message "Expected number, got \"5\""}
                                {:path [:c] :message "Expected numeric string, got \"abc\""}]))
          (is (= (:output res) nil))))))

  (testing "supports custom error messages"
    (let [res (v/validate
               (v/record {:a (v/string)
                          :b (v/number {:message (fn [{:keys [value]}]
                                                   (str "Expected it to be cool, but it's not " (pr-str value)))})
                          :c (v/string->number)})
               {:a 5
                :b "5"
                :c "abc"})]
      (is (= (:status res) :v/fail))
      (is (= (:errors res) [{:path [:a] :message "Expected string, got 5"}
                            {:path [:b] :message "Expected it to be cool, but it's not \"5\""}
                            {:path [:c] :message "Expected numeric string, got \"abc\""}]))
      (is (= (:output res) nil)))))

(deftest assert-test
  (testing "assert"
    (testing "validates given a predicate"
      (is (= ((v/assert some?)
              (ctx/create :value 5))
             [:v/ok 5])))

    (testing "fails invalid input"
      (testing "> falsey predicate"
        (is (= ((v/assert number?)
                (ctx/create :value "str"))
               [:v/error "Assert failed, got \"str\""])))
      (testing "> invalid predicate"
        (is (thrown? :default (v/assert "str" {})))))))

(deftest instance-test
  (testing "instance"
    (testing "validates given a class fn"
      (is (= ((v/instance js/String)
              (ctx/create :value "str"))
             [:v/ok "str"])))

    (testing "fails invalid input"
      (testing "> falsey instance"
        (is (= ((v/instance js/Number)
                (ctx/create :value "str"))
               [:v/error "Expected instance of Number, got \"str\""])))
      (testing "> invalid predicate"
        (is (thrown? :default (v/instance :test {})))))))

(deftest date-test
  (testing "date"
    (testing "validates js date objects"
      (let [date (js/Date.)]
        (is (= ((v/date)
                (ctx/create :value date))
               [:v/ok date]))))

    (testing "fails invalid input"
      (testing "> random string"
        (is (= ((v/date)
                (ctx/create :value "not-a-date"))
               [:v/error "Expected instance of Date, got \"not-a-date\""])))
      (testing "> random date")
      (is (= ((v/date)
              (ctx/create :value (js/Date. (js/Date.parse "whatever"))))
             [:v/error "Expected valid date, got Invalid Date"])))))

(deftest string->date-test
  (testing "string->date"
    (testing "parses valid date-string"
      (is (= ((v/string->date)
              (ctx/create :value "2025-03-23"))
             [:v/ok (js/Date. "2025-03-23")])))

    (testing "fails invalid input"
      (testing "> invalid value"
        (is (= ((v/string->date)
                (ctx/create :value :kw))
               [:v/error "Expected valid date-string, got :kw"])))
      (testing "> invalid date-string"
        (is (= ((v/string->date)
                (ctx/create :value "2025-13-32"))
               [:v/error "Expected valid date-string, got \"2025-13-32\""]))))))

(deftest num->date-test
  (testing "num->date"
    (testing "parses valid timestamp floats"
      (let [ts (js/Date.now)]
        (is (= ((v/number->date)
                (ctx/create :value ts))
               [:v/ok (js/Date. ts)]))))

    (testing "fails invalid input"
      (testing "> invalid value"
        (is (= ((v/number->date)
                (ctx/create :value :kw))
               [:v/error "Expected valid timestamp, got :kw"])))
      (testing "> invalid number"
        (is (= ((v/string->date)
                (ctx/create :value -5))
               [:v/error "Expected valid date-string, got -5"]))))))

(deftest date->string-test
  (testing "date->string"
    (testing "formats date objects to strings"
      (is (let [date (js/Date. (js/Date.parse "2025-03-23"))]
            (= ((v/date->string)
                (ctx/create :value date))
               [:v/ok (.toISOString date)]))))

    (testing "fails invalid input"
      (testing "> invalid value"
        (is (= ((v/date->string)
                (ctx/create :value :kw))
               [:v/error "Expected valid date, got :kw"])))
      (testing "> invalid date"
        (is (= ((v/date->string)
                (ctx/create :value (js/Date. "invalid")))
               [:v/error "Expected valid date, got \"Invalid Date\""]))))))

(deftest date->number-test
  (testing "date->number"
    (testing "formats date objects to numbers"
      (is (let [date (js/Date. (js/Date.parse "2025-03-23"))]
            (= ((v/date->number)
                (ctx/create :value date))
               [:v/ok (.getTime date)]))))

    (testing "fails invalid input"
      (testing "> invalid value"
        (is (= ((v/date->number)
                (ctx/create :value :kw))
               [:v/error "Expected valid date, got :kw"])))
      (testing "> invalid date"
        (is (= ((v/date->number)
                (ctx/create :value (js/Date. "invalid")))
               [:v/error "Expected valid date, got \"Invalid Date\""]))))))

(deftest nilable-test
  (testing "nilable"
    (testing "passes"
      (testing "on nil values"
        (is (= ((v/nilable (v/number))
                (ctx/create :value nil))
               [:v/ok nil])))
      (testing "validator values"
        (is (= ((v/nilable (v/number))
                (ctx/create :value 5))
               [:v/ok 5])))
      (testing "complex values"
        (is (= ((v/record {:data (v/nilable (v/assert map?))})
                (ctx/create :value {:data {:a 1}}))
               [:v/ok {:data {:a 1}}]))))
    (testing "fails"
      (testing "invalid validator input"
        (is (= ((v/nilable (v/number))
                (ctx/create :value "str"))
               [:v/error "Expected number, got \"str\""]))))))

(deftest enum-test
  (testing "nilable"
    (testing "passes"
      (testing "accepted videos"
        (is (= ((v/enum [:a :b :c])
                (ctx/create :value :b))
               [:v/ok :b]))))
    (testing "fails"
      (testing "invalid value type"
        (is (= ((v/enum [:a :b :c])
                (ctx/create :value "str"))
               [:v/error "Expected keyword one of :a, :b, :c, got \"str\""])))
      (testing "invalid enum type"
        (is (thrown? :default ((v/enum "str")
                               (ctx/create :value "str"))))))))

(deftest literal-test
  (testing "literal"
    (testing "passes"
      (testing "matching string literals"
        (is (= ((v/literal "str")
                (ctx/create :value "str"))
               [:v/ok "str"])))
      (testing "matching keyword literals"
        (is (= ((v/literal :kw)
                (ctx/create :value :kw))
               [:v/ok :kw]))))
    (testing "fails"
      (testing "non-matching literal values"
        (is (= ((v/literal "str")
                (ctx/create :value :kw))
               [:v/error "Expected literal \"str\", got :kw"]))))))

(deftest chain-test
  (testing "chain"
    (testing "passes"
      (testing "multiple, sequential validations"
        (is (= ((v/chain
                 (v/string)
                 (v/string->number)
                 (fn [{:keys [value]}]
                   (v/ok (js/Math.round value))))
                (ctx/create :value "55.39"))
               [:v/ok 55]))))
    (testing "fails"
      (testing "first failing validator"
        (is (= ((v/chain
                 (v/string)
                 (v/keyword)
                 (v/string->number))
                (ctx/create :value "55.5"))
               [:v/errors [{:path [] :message "Expected keyword, got \"55.5\""}]]))))))

(deftest union-test
  (testing "union"
    (testing "passes"
      (testing "first passing validator"
        (is (= ((v/union
                 (v/string)
                 (v/keyword)
                 (v/number))
                (ctx/create :value :kw))
               [:v/ok :kw])))
      (testing "nested validators"
        (is (= ((v/union
                 (v/vector
                  (v/union (v/string)
                           (v/keyword)))
                 (v/number))
                (ctx/create :value [:kw "str"]))
               [:v/ok [:kw "str"]]))
        (is (= ((v/union
                 (v/vector
                  (v/union (v/string)
                           (v/keyword)))
                 (v/number))
                (ctx/create :value 5))
               [:v/ok 5]))))
    (testing "fails"
      (testing "last failing validator"
        (is (= ((v/union
                 (v/string)
                 (v/number)
                 (v/keyword))
                (ctx/create :value nil))
               [:v/errors [{:path [] :message "Expected keyword, got nil"}]]))))))

(deftest default-test
  (testing "default"
    (testing "passes"
      (testing "if nil with default value"
        (is (= ((v/default
                  (v/number)
                  0)
                (ctx/create :value nil))
               [:v/ok 0])))

      (testing "if nil with default function"
        (is (= ((v/default
                  (v/number)
                  (fn [{:keys [value]}]
                    {:from value :to true}))
                (ctx/create :value nil))
               [:v/ok {:from nil :to true}])))
      (testing "if not nil"
        (is (= ((v/default
                  (v/keyword)
                  0)
                (ctx/create :value :kw))
               [:v/ok :kw]))))
    (testing "fails"
      (testing "forwards validator error"
        (is (= ((v/default
                  (v/keyword)
                  :not-found)
                (ctx/create :value "str"))
               [:v/error "Expected keyword, got \"str\""]))))))

(declare task)

(def task
  (v/lazy
   #(v/record
     {:title (v/string)
      :tasks (v/vector task)})))

(def task-data
  {:title "Parent"
   :tasks [{:title "Child"
            :tasks []}]})

(deftest lazy-test
  (testing "lazy"
    (testing "passes"
      (testing "valid sub-validator data"
        (is (= ((v/lazy
                 (fn []
                   (v/number)))
                (ctx/create :value 5))
               [:v/ok 5])))
      (testing "recursive validators"
        (is (= (task
                (ctx/create :value task-data))
               [:v/ok task-data]))))
    (testing "fails"
      (testing "non-function args"
        (is (= ((v/lazy :kw)
                (ctx/create :value 5))
               [:v/error "Expected validator function, got 5"])))
      (testing "invalid sub-validator data"
        (is (= ((v/lazy #(v/number))
                (ctx/create :value :kw))
               [:v/error "Expected number, got :kw"]))))))


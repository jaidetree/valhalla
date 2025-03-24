(ns jaidetree.valhalla.core-test
  (:require
   [cljs.test :refer [deftest testing is]]
   [jaidetree.valhalla.core :as v]
   [jaidetree.valhalla.context :as ctx]))

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
  (testing "validate passes on valid input"
    (let [res (v/validate (v/string) "test")]
      (is (= (:status res) :v/pass))
      (is (= (:input res) "test"))
      (is (= (:output res) "test"))))

  (testing "validate fails on invalid input"
    (let [res (v/validate (v/string) 5)]
      (is (= (:status res) :v/fail))
      (is (= (:input res) 5))
      (is (= (:output res) nil))
      (is (= (:errors res) [{:path []
                             :message "Expected string, got 5"}])))))

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
    (testing "validates lists with same type"
      (is (= ((v/list (v/number))
              (ctx/create :value (list 1 2 3 4 5)))
             [:v/ok (list 1 2 3 4 5)])))
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
  (testing "validates a hash-map"
    (let [res (v/validate
               (v/hash-map {:a (v/string)
                            :b (v/number)
                            :c (v/string->number)})
               {:a "test1"
                :b 5
                :c "45.5"})]
      (is (= (:status res) :v/pass))
      (is (= (:errors res) nil))
      (is (= (:output res) {:a "test1"
                            :b 5
                            :c 45.5}))))

  (testing "fails with a non-hash-map value"
    (let [res (v/validate
               (v/hash-map {:a (v/string)
                            :b (v/number)
                            :c (v/string->number)})
               nil)]
      (is (= (:status res) :v/fail))
      (is (= (:errors res) [{:path [] :message "Expected hash-map, got nil"}]))
      (is (= (:output res) nil))))

  (testing "collects all errors"
    (let [res (v/validate
               (v/hash-map {:a (v/string)
                            :b (v/number)
                            :c (v/string->number)})
               {:a 5
                :b "5"
                :c "abc"})]
      (is (= (:status res) :v/fail))
      (is (= (:errors res) [{:path [:a] :message "Expected string, got 5"}
                            {:path [:b] :message "Expected number, got \"5\""}
                            {:path [:c] :message "Expected numeric string, got \"abc\""}]))
      (is (= (:output res) nil))))

  (testing "supports custom error messages"
    (let [res (v/validate
               (v/hash-map {:a (v/string)
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


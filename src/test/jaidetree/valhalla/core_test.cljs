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
             [:v/error "Expected string, got 5"])))))

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


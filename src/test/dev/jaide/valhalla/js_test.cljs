(ns dev.jaide.valhalla.js-test
  (:require
   [clojure.pprint :refer [pprint]]
   [cljs.test :refer [deftest testing is]]
   [dev.jaide.valhalla.core :as v]
   [dev.jaide.valhalla.js :as vjs]
   [dev.jaide.valhalla.context :as ctx]))

(deftest object-test
  (testing "object"
    (testing "passes"
      (testing "js-objects"
        (is (= ((vjs/object
                 {:a (v/string)
                  :b (v/keyword)
                  :c (v/number)})
                (ctx/create :value #js {:a "str" :b :kw :c 5}))
               [:v/ok {:a "str"
                       :b :kw
                       :c 5}]))))
    (testing "fails"
      (testing "invalid values"
        (let [result ((vjs/object
                       {:a (v/string)
                        :b (v/keyword)
                        :c (v/number)})
                      (ctx/create :value #js {:a :kw :b 5 :c "str"}))]
          (is (= result
                 [:v/errors [{:path [:a] :message "Expected string, got :kw"}
                             {:path [:b] :message "Expected keyword, got 5"}
                             {:path [:c] :message "Expected number, got \"str\""}]])))))))

(deftest array-test
  (testing "array"
    (testing "passes"
      (testing "js-arrays"
        (is (= ((vjs/array (v/number))
                (ctx/create :value #js [1 2 3 4 5]))
               [:v/ok [1 2 3 4 5]]))))
    (testing "fails"
      (testing "invalid values"
        (is (= ((vjs/array (v/number))
                (ctx/create :value #js ["str" "str2" "str3"]))
               [:v/errors [{:path [0] :message "Expected number, got \"str\""}
                           {:path [1] :message "Expected number, got \"str2\""}
                           {:path [2] :message "Expected number, got \"str3\""}]]))))))

(deftest array-tuple-test
  (testing "array-tuple"
    (testing "passes"
      (testing "tuple arrays with mixed types"
        (is (= ((vjs/array-tuple [(v/keyword) (v/number)])
                (ctx/create :value #js [:kw 55]))
               [:v/ok [:kw 55]]))))
    (testing "fails"
      (testing "invalid values"
        (is (= ((vjs/array-tuple [(v/keyword) (v/number)])
                (ctx/create :value #js [55 :kw]))
               [:v/errors [{:path [0] :message "Expected keyword, got 55"}
                           {:path [1] :message "Expected number, got :kw"}]]))))))

(deftest iterable->array-test
  (testing "iterable->array-test"
    (testing "passes"
      (testing "parsing array-like values"
        (is (= ((v/chain
                 (vjs/iterable->array)
                 (vjs/array (v/string)))
                (ctx/create :value (js/Set. #js ["str" "str2" "str3"])))
               [:v/ok ["str" "str2" "str3"]]))))
    (testing "fails"
      (testing "non-iterable types"
        (is (= ((vjs/iterable->array)
                (ctx/create :value #js {:kw "str"}))
               [:v/error "Expected iterable, got #js {:kw \"str\"}"])))
      (testing "sub-validator failures"
        (is (= ((v/chain
                 (vjs/iterable->array)
                 (vjs/array (v/string)))
                (ctx/create :value #js [:kw 5]))
               [:v/errors [{:path [0] :message "Expected string, got :kw"}
                           {:path [1] :message "Expected string, got 5"}]]))))))


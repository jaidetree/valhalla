(ns jaidetree.valhalla.core-test
  (:require
   [cljs.test :refer [deftest testing is]]
   [jaidetree.valhalla.core :as v]))

(deftest ok-test
  (testing "ok returns successful result"
    (is (= (v/ok "value")
           {:result :v/ok
            :errors []
            :data   "value"}))))

(deftest error-msg-test
  (testing "error returns failed result with no value"
    (is (= (v/error "Expected test error")
           {:result :v/fail
            :errors [{:path []
                      :message "Expected test error"}]
            :data nil}))))

(deftest error-msg-value-test
  (testing "error returns failed result with value"
    (is (= (v/error "Expected test error" "bad-value")
           {:result :v/fail
            :errors [{:path []
                      :message "Expected test error"}]
            :data "bad-value"}))))

(deftest error-path-msg-value-test
  (testing "error returns failed result with path, message, value"
    (is (= (v/error [:a :b] "Expected test error" {:a {:b "bad-value"}})
           {:result :v/fail
            :errors [{:path [:a :b]
                      :message "Expected test error"}]
            :data {:a {:b "bad-value"}}}))))

(deftest errors-test
  (testing "errors returns failed result with set of errors"
    (is (= (v/errors [[:a :b] "Expected test error"] {:a {:b "bad-value"}})
           {:result :v/fail
            :errors [[:a :b] "Expected test error"]
            :data {:a {:b "bad-value"}}}))))

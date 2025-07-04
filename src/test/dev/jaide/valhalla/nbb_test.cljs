(ns dev.jaide.valhalla.nbb-test
  (:require
   [cljs.test :as t :refer [async deftest is testing]]
   [dev.jaide.valhalla.core :as v]))

(def validator
  (v/record {:test (v/string)}))

(deftest validation-test
  []
  (testing "nbb"
    (testing "can validate"
      (is
       (= (v/validate
           validator
           {:test "It works"})
          {:status :v/pass
           :input {:test "It works"}
           :output {:test "It works"}})))))

(defn -main
  []
  (t/run-tests 'dev.jaide.valhalla.nbb-test))

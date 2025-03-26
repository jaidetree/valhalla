(ns jaidetree.valhalla.integration-test
  (:require
   [clojure.pprint :refer [pprint]]
   [cljs.test :refer [deftest testing is]]
   [jaidetree.valhalla.core :as v]))

(def task
  (v/hash-map
   {:id (v/string)
    :title (v/string)
    :status (v/enum [:todo
                     :in-progress
                     :complete])}))

(def action
  (v/union
   (v/hash-map
    {:type (v/literal "clock-in")
     :data (v/hash-map {:id (v/string)
                        :time (v/date->string)})})
   (v/hash-map
    {:type (v/literal "clock-out")
     :data (v/hash-map {:id (v/numeric)
                        :time (v/chain
                               (v/date)
                               (v/date->string))})})))

(def validator-1
  (v/vector task))

(def test-data-1
  [{:id "test"
    :title "Task"
    :status :todo}])

(def validator-2
  (v/hash-map
   {:kw (v/keyword)
    :str (v/string)
    :tuple (v/vector-tuple
            [(v/number)
             (v/string)])
    :state (v/hash-map
            {:status (v/enum [:loading :running :closing])
             :id (v/string)})
    :tasks (v/vector task)
    :action action}))

(def test-data-2
  {:kw :kw
   :str "str"
   :tuple [5 "str"]
   :state {:status :loading
           :id "xyz-123"}
   :tasks [{:id "test"
            :title "Task"
            :status :todo}]
   :action {:type "clock-in"
            :data {:id "123"
                   :time (js/Date. 1742931932578)}}})

(deftest vector-hash-maps-test
  (testing "vector of hash-maps"
    (testing "passes"
      (let [result (v/validate validator-1 test-data-1)]
        (is (= (get-in result [:output])
               test-data-1))))))

(deftest composite-test
  (testing "complex hash-map"
    (let [result (v/validate validator-2 test-data-2)]
      (is (= nil (:errors result)))
      (is (= (:output result)
             {:kw :kw
              :str "str"
              :tuple [5 "str"]
              :state {:status :loading
                      :id "xyz-123"}
              :tasks [{:id "test"
                       :title "Task"
                       :status :todo}]
              :action {:type "clock-in"
                       :data {:id "123"
                              :time "2025-03-25T19:45:32.578Z"}}})))))

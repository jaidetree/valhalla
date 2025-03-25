(ns jaidetree.valhalla.context-test
  (:require
   [cljs.test :refer [deftest testing is]]
   [jaidetree.valhalla.context :as ctx]))

(deftest make-context-test
  (testing "returns context shape with no args"
    (let [ctx (ctx/create)]
      (is (= (:value ctx) nil))
      (is (= (:path ctx) []))
      (is (= (:errors ctx) []))
      (is (= (:input ctx) nil))
      (is (= (:output ctx) nil))))

  (testing "returns context shape with input"
    (is (= (ctx/create {:input "a-input"})
           {:value "a-input"
            :path []
            :errors []
            :input "a-input"
            :output nil})))

  (testing "returns context shape with path"
    (is (= (ctx/create :path [:a])
           {:value nil
            :path [:a]
            :errors []
            :input nil
            :output nil})))

  (testing "returns context shape with data"
    (is (= (ctx/create :value {:a {:b "a-input"}})
           {:value {:a {:b "a-input"}}
            :path []
            :errors []
            :input {:a {:b "a-input"}}
            :output nil})))

  (testing "returns context shape with errors"
    (is (= (ctx/create :errors [{:path [:a :b]
                                 :message "Error"}])
           {:value nil
            :path []
            :errors [{:path [:a :b]
                      :message "Error"}]
            :input nil
            :output nil}))))

(deftest update-value-test
  (testing "updates context value"
    (let [res (-> (ctx/create)
                  (ctx/update-value "new-value"))]
      (is (= (:value res) "new-value")))))

(deftest append-path-test
  (testing "updates context path"
    (let [prev (ctx/create)
          next (ctx/append-path prev :a)]
      (is (= (:path prev) []))
      (is (= (:path next) [:a])))))

(deftest update-path-test
  (testing "replaces last context path"
    (let [res (-> (ctx/create)
                  (ctx/update-path [:a :b]))]
      (is (= (:path res) [:a :b])))))

(deftest replace-path-test
  (testing "replace-path"
    (testing "replaces context path at index"
      (let [res (-> (ctx/create :path [:a :b :c :d]
                                :input {:a {:b {:c {:d "d-str"
                                                    :e "e-str"}}}})
                    (ctx/replace-path 3 :e))]
        (is (= (:path res) [:a :b :c :e]))
        (is (= (:value res) "e-str"))))
    (testing "supports vectors"
      (let [res (-> (ctx/create :path [:a 2]
                                :input {:a [:b :c :d :e]})
                    (ctx/replace-path 1 3))]
        (is (= (:path res) [:a 3]))
        (is (= (:value res) :e))))
    (testing "supports lists"
      (let [res (-> (ctx/create :path [0]
                                :input (list :b :c :d :e))
                    (ctx/replace-path 0 2))]
        (is (= (:path res) [2]))
        (is (= (:value res) :d))))))

(deftest append-error-test
  (testing "append error string"
    (let [res (-> (ctx/create)
                  (ctx/raise-error "Some error"))]
      (is (= (:errors res)
             [{:path [] :message "Some error"}]))))

  (testing "append error map"
    (let [res (-> (ctx/create)
                  (ctx/raise-error {:path [:a :b]
                                    :message "An error"}))]
      (is (= (:errors res)
             [{:path [:a :b] :message "An error"}])))))

(deftest update-output-test
  (testing "update context output"
    (let [res (-> (ctx/create)
                  (ctx/update-output "output"))]
      (is (= (:output res) "output"))))

  (testing "update output with path"
    (let [res (-> (ctx/create :output {})
                  (ctx/update-output [:a :b] "output"))]
      (is (= (:output res) {:a {:b "output"}})))))

(deftest enter-path-test
  (testing "updates both path and value"
    (let [res (-> (ctx/create :input {:a {:b "a-value"}})
                  (ctx/path> :a))]
      (is (= (:path res) [:a]))
      (is (= (:value res) {:b "a-value"}))
      (let [res-b (ctx/path> res :b)]
        (is (= (:path res-b) [:a :b]))
        (is (= (:value res-b) "a-value"))))))

(deftest leave-path-test
  (testing "updates both path and value"
    (let [res (-> (ctx/create :input {:a {:b "a-value"}}
                              :path [:a :b])
                  (ctx/path<))]
      (is (= (:path res) [:a]))
      (is (= (:value res) {:b "a-value"}))
      (let [res-b (ctx/path< res)]
        (is (= (:path res-b) []))
        (is (= (:value res-b) {:a {:b "a-value"}}))))))

(deftest accrete
  (testing "updates both value and output"
    (let [res (-> (ctx/create :input {:a {:b "a-value"}}
                              :path [:a :b]
                              :output {})
                  (ctx/accrete "a-value"))]
      (is (= (:value res) "a-value"))
      (is (= (:output res) {:a {:b "a-value"}})))))


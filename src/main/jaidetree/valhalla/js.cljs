(ns jaidetree.valhalla.js
  (:refer-clojure :exclude [array])
  (:require
   [clojure.pprint :refer [pprint]]
   [jaidetree.valhalla.core :refer [error ok errors result-case]]
   [jaidetree.valhalla.context :as ctx]
   [jaidetree.valhalla.utils :refer [msg-fn] :as u]))

(defn replace-path
  [ctx idx path]
  (let [prev-path (vec (take idx (:path ctx)))
        new-path (conj prev-path path)]
    (-> ctx
        (assoc :path new-path)
        (assoc :value (aget (:input ctx)
                            (if (keyword? path)
                              (name path)
                              path))))))

(defn- object-validators
  [& {:keys [context validator-kvs path-index]}]
  (let [context (assoc context :output {})]
    (->> validator-kvs
         (reduce
          (fn [ctx [key validator]]
            (let [ctx (replace-path ctx path-index key)
                  result (validator ctx)]
              (result-case result
                           :ok (fn [value]
                                 (-> ctx
                                     (assoc :value value)
                                     (assoc-in [:output key] value)))
                           :err (fn [error]
                                  (ctx/raise-error ctx error))
                           :errs (fn [errors]
                                   (ctx/raise-errors ctx errors)))))
          context))))

(defn object
  "Creates a validator that checks if a value is a JavaScript object and validates specific properties.
   
   Arguments:
   - validators-map - A map of property names to validator functions
   
   Options:
   - :message - Custom error message function or string
   
   Returns a validator function that accepts a context and returns a result
   with a map of validated property-value pairs if successful."
  [validators-map & [opts]]
  (fn [{:keys [value] :as context}]
    (let [message (msg-fn (:message opts)
                          (fn [{:keys [value]}]
                            (str "Expected js-object, got " (u/stringify value))))]
      (if (not (object? value))
        (error (message context))
        (let [idx (count (:path context))
              ctx (object-validators
                   {:context context
                    :validator-kvs validators-map
                    :path-index idx})]
          (if (empty? (:errors ctx))
            (ok (:output ctx))
            (errors (:errors ctx))))))))

(defn- array-validators
  [& {:keys [context js-array validator path-index]}]
  (let [context (assoc context :output [])]
    (-> js-array
        (.reduce
         (fn [ctx value key]
           (let [ctx (-> ctx
                         (replace-path path-index key)
                         (assoc :value value))
                 result (validator ctx)]
             (result-case result
                          :ok (fn [value]
                                (-> ctx
                                    (assoc :value value)
                                    (assoc-in [:output key] value)))
                          :err (fn [error]
                                 (ctx/raise-error ctx error))
                          :errs (fn [errors]
                                  (ctx/raise-errors ctx errors)))))
         context))))

(defn array
  "Creates a validator that checks if a value is a JavaScript array and validates each element.
   
   Arguments:
   - validator - A validator function to apply to each element
   
   Options:
   - :message - Custom error message function or string
   
   Returns a validator function that accepts a context and returns a result
   with an array of validated elements if successful."
  [validator & [opts]]
  (fn [{:keys [value] :as context}]
    (let [message (msg-fn (:message opts)
                          (fn [{:keys [value]}]
                            (str "Expected js-object, got " (u/stringify value))))]
      (if (not (array? value))
        (error (message context))
        (let [idx (count (:path context))
              ctx (array-validators
                   {:context context
                    :validator validator
                    :js-array value
                    :path-index idx})]
          (if (empty? (:errors ctx))
            (ok (:output ctx))
            (errors (:errors ctx))))))))

(defn- array-tuple-validators
  [& {:keys [context validator-kvs path-index]}]
  (let [context (assoc context :output [])]
    (->> validator-kvs
         (reduce
          (fn [ctx [key validator]]
            (let [ctx (replace-path ctx path-index key)
                  result (validator ctx)]
              (result-case result
                           :ok (fn [value]
                                 (-> ctx
                                     (assoc :value value)
                                     (assoc-in [:output key] value)))
                           :err (fn [error]
                                  (ctx/raise-error ctx error))
                           :errs (fn [errors]
                                   (ctx/raise-errors ctx errors)))))
          context))))

(defn array-tuple
  "Creates a validator that checks if a value is a JavaScript array with specific validators for each position.
   
   Arguments:
   - validators-seq - A sequence of validator functions, one for each position
   
   Options:
   - :message - Custom error message function or string
   
   Returns a validator function that accepts a context and returns a result
   with an array of validated elements if successful. The input array must have
   the same length as the validators sequence."
  [validators-seq & [opts]]
  (fn [{:keys [value] :as context}]
    (let [message (msg-fn (:message opts)
                          (fn [{:keys [value]}]
                            (str "Expected js-array-tuple, got " (u/stringify value))))]
      (if (and (array? value)
               (= (.-length value) (count validators-seq)))
        (let [idx (count (:path context))
              ctx (array-tuple-validators
                   {:context context
                    :validator-kvs (map-indexed vector validators-seq)
                    :path-index idx})]
          (if (empty? (:errors ctx))
            (ok (:output ctx))
            (errors (:errors ctx))))
        (error (message context))))))

(defn iterable->array
  "Creates a validator that converts a JavaScript iterable object to an array.
   
   Options:
   - :message - Custom error message function or string
   
   Returns a validator function that accepts a context and returns a result
   with a JavaScript array created from the iterable if successful."
  ([] (iterable->array {}))
  ([opts]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected iterable, got " (u/stringify value))))]
       (try
         (if (js-iterable? value)
           (ok (js/Array.from value))
           (throw (js/Error. :fail)))
         (catch :default _err
           (error (message context))))))))

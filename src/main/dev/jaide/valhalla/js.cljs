(ns dev.jaide.valhalla.js
  (:refer-clojure :exclude [array])
  (:require
   [clojure.pprint :refer [pprint]]
   [dev.jaide.valhalla.core :refer [error ok errors result-case]]
   [dev.jaide.valhalla.context :as ctx]
   [dev.jaide.valhalla.utils :refer [msg-fn] :as u]))

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

(defn- record-validators
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

(defn record
  "Validates if a value is a JavaScript object and validates specific properties.

   Arguments:
   - validators-map - A map of property names to validator functions

   Options:
   - :message - Custom error message function or string

   Returns a validator function that accepts a context and returns a result
   with a map of validated property-value pairs if successful."
  [validators-map & [opts]]
  (assert (map? validators-map) "Validators must be a hash-map of keys to validators")
  (fn [{:keys [value] :as context}]
    (let [message (msg-fn (:message opts)
                          (fn [{:keys [value]}]
                            (str "Expected js-object, got " (u/stringify value))))]
      (if (or (js/Array.isArray value)
              (not (instance? js/Object value)))
        (error (message context))
        (let [idx (count (:path context))
              ctx (record-validators
                   {:context context
                    :validator-kvs validators-map
                    :path-index idx})]
          (if (empty? (:errors ctx))
            (ok (:output ctx))
            (errors (:errors ctx))))))))

(defn- object-validators
  [& {:keys [context validator path-index]}]
  (let [context (assoc context :output {})]
    (-> (:value context)
        (js/Object.entries)
        (.reduce
         (fn [ctx [key value]]
           (let [ctx (-> ctx
                         (replace-path path-index key)
                         (ctx/update-value value))

                 result (validator ctx)]
             (result-case result
                          :ok (fn [value]
                                (-> ctx
                                    (assoc :value value)
                                    (assoc-in [:output (keyword key)] value)))
                          :err (fn [error]
                                 (ctx/raise-error ctx error))
                          :errs (fn [errors]
                                  (ctx/raise-errors ctx errors)))))
         context))))

(defn object
  "Validates if a value is a JavaScript object with same value type of unknown size.

   Arguments:
   - validator - A validator function

   Options:
   - :message - Custom error message function or string

   Returns a validator function that accepts a context and returns a result
   with a map of validated property-value pairs if successful."
  [validator & [opts]]
  (assert (fn? validator) "Validator must be a function")
  (fn [{:keys [value] :as context}]
    (let [message (msg-fn (:message opts)
                          (fn [{:keys [value]}]
                            (str "Expected js-object, got " (u/stringify value))))]
      (if (not (object? value))
        (error (message context))
        (let [idx (count (:path context))
              ctx (object-validators
                   {:context context
                    :validator validator
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
  "Validates if a value is a JavaScript array and validates each element.

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
                            (str "Expected js-array, got " (u/stringify value))))]
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
  "Validates if a value is a JavaScript array with specific validators for each position.

   Arguments:
   - validators-seq - A sequence of validator functions, one for each position

   Options:
   - :message - Custom error message function or string

   Returns a validator function that accepts a context and returns a result
   with an array of validated elements if successful. The input array must have
   the same length as the validators sequence."
  [validators-seq & [opts]]
  (assert (sequential? validators-seq) "Validators must be a vector or list")
  (fn [{:keys [value] :as context}]
    (let [message (msg-fn (:message opts)
                          (fn [{:keys [value]}]
                            (str "Expected js-array-tuple of length " (count validators-seq) ", got " (u/stringify value))))]
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

(defn iterable->vector
  "Converts a JavaScript iterable object to an array.

   Options:
   - :message - Custom error message function or string

   Returns a validator function that accepts a context and returns a result
   with a JavaScript array created from the iterable if successful."
  ([] (iterable->vector {}))
  ([opts]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected iterable, got " (u/stringify value))))]
       (try
         (if (js-iterable? value)
           (ok (-> (js/Array.from value)
                   (.reduce (fn [vec v]
                              (conj vec v))
                            [])))
           (throw (js/Error. :fail)))
         (catch :default _err
           (error (message context))))))))

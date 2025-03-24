(ns jaidetree.valhalla.core
  (:refer-clojure :exclude [assert hash-map boolean
                            keyword list set symbol
                            uuid vector])
  (:require
   [clojure.core :as cc]
   [clojure.pprint :refer [pprint]]
   [clojure.string :as s]
   [jaidetree.valhalla.context :as ctx]
   [jaidetree.valhalla.utils :as u]))

(defn ok
  [value]
  [:v/ok value])

(defn error
  [message]
  [:v/error message])

(defn errors
  [errors]
  [:v/errors errors])

(defn ok?
  [[status _value]]
  (= status :v/ok))

(defn pass
  [& {:keys [input output]}]
  {:status :v/pass
   :input  input
   :output output})

(defn fail
  [& {:keys [input errors]}]
  {:status :v/fail
   :errors errors
   :input  input
   :output nil})

(defn result-case
  [[status val-or-msg] & {:keys [ok err errs]
                          :or {ok identity
                               err identity
                               errs identity}}]
  (case status
    :v/ok     (ok val-or-msg)
    :v/error  (err val-or-msg)
    :v/errors (errs val-or-msg)
    (throw (js/Error. (str "Could not match status type " (u/stringify status))))))

(defn valid?
  [result]
  (= (:status result) :v/pass))

(defn validate
  [validator-fn input & [opts]]
  (let [context (ctx/create :input input)
        result (validator-fn context)]
    (result-case result
                 :ok (fn [value]
                       (-> context
                           (ctx/accrete value)
                           (pass)))
                 :err (fn [error]
                        (-> context
                            (ctx/raise-error error)
                            (fail)))
                 :errs (fn [errors]
                         (-> context
                             (ctx/raise-errors errors)
                             (fail))))))

(defn- msg-fn
  [str-or-fn default-fn]
  (cond
    (string? str-or-fn) (constantly str-or-fn)
    (fn? str-or-fn) str-or-fn
    :else default-fn))

(defn- with-ctx
  "
  Syntax sugar for operating on a collection within a threading macro
  "
  [f ctx]
  (f ctx))

(defn string
  ([] (string {}))
  ([opts]
   (fn [{:keys [value _path] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected string, got " (u/stringify value))))]
       (if (string? value)
         (ok value)
         (error (message context)))))))

(defn number
  ([] (number {}))
  ([opts]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected number, got " (u/stringify value))))]
       (if (number? value)
         (ok value)
         (error (message context)))))))

(defn numeric
  ([] (numeric {}))
  ([opts]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected numeric string, got " (u/stringify value))))
           number-value (js/Number.parseFloat value 10)]
       (if (not (js/Number.isNaN number-value))
         (ok value)
         (error (message context)))))))

(defn string->number
  ([] (string->number {}))
  ([opts]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected numeric string, got " (u/stringify value))))]
       (try
         (let [value (js/Number.parseFloat value)]
           (if (not (js/Number.isNaN value))
             (ok value)
             (error (message context))))
         (catch :default _err
           (error (message context))))))))

(defn boolean
  ([] (boolean {}))
  ([opts]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected boolean, got " (u/stringify value))))]
       (if (boolean? value)
         (ok value)
         (error (message context)))))))

(defn string->boolean
  ([] (string->boolean {}))
  ([opts]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected boolean-string, got " (u/stringify value))))]
       (try
         (if (not (string? value))
           (error (message context))
           (let [value (s/lower-case value)]
             (case value
               "true" (ok true)
               "false" (ok false)
               (error (message context)))))
         (catch :default _err
           (error (message context))))))))

(defn keyword
  ([] (keyword {}))
  ([opts]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected keyword, got " (u/stringify value))))]
       (if (keyword? value)
         (ok value)
         (error (message context)))))))

(defn string->keyword
  ([] (string->keyword {}))
  ([opts]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected keyword-string, got " (u/stringify value))))]
       (if (or (not (string? value))
               (not (re-matches #"^:?[a-zA-Z][-_a-zA-Z0-9\/]*$" value)))
         (error (message context))
         (try
           (let [value (s/replace value #"^:" "")]
             (ok (cc/keyword value)))
           (catch :default _err
             (error (message context)))))))))

(defn symbol
  ([] (symbol {}))
  ([opts]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected symbol, got " (u/stringify value))))]
       (if (symbol? value)
         (ok value)
         (error (message context)))))))

(defn string->symbol
  ([] (string->symbol {}))
  ([opts]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected symbol-string, got " (u/stringify value))))]
       (if (or (not (string? value))
               (not (re-matches #"^[a-zA-Z][-_a-zA-Z0-9\/]*$" value)))
         (error (message context))
         (try
           (ok (cc/symbol value))
           (catch :default _err
             (error (message context)))))))))

(defn regex
  ([regex-str]
   (regex regex-str {}))
  ([regex-str opts]
   (cc/assert (string? regex-str) "Expected a regex pattern string")
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected string matching " regex-str ", got " (u/stringify value))))]
       (if (not (string? value))
         (error (message context))
         (try
           (let [pattern (re-pattern regex-str)]
             (if (re-matches pattern value)
               (ok value)
               (error (message context))))
           (catch :default _err
             (error (message context)))))))))

(defn uuid
  ([] (uuid {}))
  ([opts]
   (regex "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
          (merge opts
                 {:message (fn [{:keys [value]}]
                             (str "Expected UUID string, got " (u/stringify value)))}))))

(defn nil-value
  ([] (nil-value {}))
  ([opts]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected nil, got " (u/stringify value))))]
       (if (nil? value)
         (ok value)
         (error (message context)))))))

(defn reduce-validators
  [& {:keys [context validator-kvs path-index]}]
  (->> validator-kvs
       (reduce
        (fn [ctx [key validator]]
          (let [ctx (ctx/replace-path ctx path-index key)
                result (validator ctx)]
            (result-case result
                         :ok (fn [value]
                               (ctx/accrete ctx value))
                         :err (fn [error]
                                (ctx/raise-error ctx error))
                         :errs (fn [errors]
                                 (ctx/raise-errors ctx errors)))))
        context)))

(defn vector
  ([validator & [opts]]
   (cc/assert (fn? validator) "Validator is not a function")
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected vector, got " (u/stringify value))))]
       (if (vector? value)
         (let [idx (count (:path context))
               ctx (reduce-validators
                    {:context context
                     :path-index idx
                     :validator-kvs (map-indexed cc/vector (repeat (count value) validator))})]
           (if (empty? (:errors ctx))
             (ok (vec (vals (:output ctx))))
             (errors (:errors ctx))))
         (error (message context)))))))

(defn vector-tuple
  ([validators & [opts]]
   (cc/assert (vector? validators) "Validators are not a vector")
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected vector-tuple of length "
                                  (count validators)
                                  ", got " (u/stringify value))))]
       (if (and (vector? value)
                (= (count validators) (count value)))
         (let [idx (count (:path context))
               ctx (reduce-validators
                    {:context context
                     :path-index idx
                     :validator-kvs (map-indexed cc/vector validators)})]
           (if (empty? (:errors ctx))
             (ok (vec (vals (:output ctx))))
             (errors (:errors ctx))))

         (error (message context)))))))

(defn list
  ([validator & [opts]]
   (cc/assert (fn? validator) "Validator is not a function")
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected list, got " (u/stringify value))))]
       (if (list? value)
         (let [idx (count (:path context))
               ctx (reduce-validators
                    {:context context
                     :path-index idx
                     :validator-kvs (map-indexed cc/list (repeat (count value) validator))})]
           (if (empty? (:errors ctx))
             (ok (vals (:output ctx)))
             (errors (:errors ctx))))
         (error (message context)))))))

(defn list-tuple
  ([validators & [opts]]
   (cc/assert (sequential? validators) "Validators are not a vector")
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected list-tuple of length "
                                  (count validators)
                                  ", got " (u/stringify value))))]
       (if (and (list? value)
                (= (count validators) (count value)))
         (let [idx (count (:path context))
               ctx (reduce-validators
                    {:context context
                     :path-index idx
                     :validator-kvs (map-indexed cc/vector validators)})]
           (if (empty? (:errors ctx))
             (ok (vals (:output ctx)))
             (errors (:errors ctx))))
         (error (message context)))))))

(defn set
  [validator & [opts]]
  (cc/assert (fn? validator) "Validator is not a function")
  (fn [{:keys [value] :as context}]
    (let [message (msg-fn (:message opts)
                          (fn [{:keys [value]}]
                            (str "Expected set, got " (u/stringify value))))]
      (if (set? value)
        (let [idx (count (:path context))
              ctx (reduce-validators
                   {:context context
                    :path-index idx
                    :validator-kvs (map-indexed cc/list (repeat (count value) validator))})]
          (if (empty? (:errors ctx))
            (ok (into #{} (vals (:output ctx))))
            (errors (:errors ctx))))
        (error (message context))))))

(defn hash-map
  [validators-map & [opts]]
  (fn [{:keys [value] :as context}]
    (let [message (msg-fn (:message opts)
                          (fn [{:keys [value]}]
                            (str "Expected hash-map, got " (u/stringify value))))]
      (if (not (map? value))
        (error (message context))
        (let [idx (count (:path context))
              ctx (reduce-validators
                   {:context context
                    :validator-kvs validators-map
                    :path-index idx})]
          (if (empty? (:errors ctx))
            (ok (:output ctx))
            (errors (:errors ctx))))))))

(defn assert
  ([pred?] (assert pred? {}))
  ([pred? opts]
   (cc/assert (fn? pred?) "Predicate must be a function")
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Assert failed, got " (u/stringify value))))]
       (try
         (if (true? (pred? value))
           (ok value)
           (throw (js/Error. :fail)))
         (catch :default _err
           (error (message context))))))))

(defn instance
  ([class-fn] (instance class-fn {}))
  ([class-fn opts]
   (cc/assert (js-fn? class-fn) "Class function required")
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected instance of " (.-name class-fn) ", got " (u/stringify value))))]
       (try
         (if (or (instance? class-fn value)
                 (= (type value) class-fn))
           (ok value)
           (throw (js/Error. :fail)))
         (catch :default _err
           (error (message context))))))))

(defn- date?
  [date]
  (not (js/Number.isNaN (.getTime date))))

;; @TODO Clean up with chain?
(defn date
  [& [opts]]
  (fn [context]
    (let [result ((instance js/Date) context)
          message (msg-fn (:message opts)
                          (fn [{:keys [value]}]
                            (str "Expected valid date, got " (.toString value))))]
      (result-case result
                   :ok   (fn [value]
                           (if (date? value)
                             (ok value)
                             (error (message context))))
                   :err  error
                   :errs errors))))

(defn string->date
  ([] (string->date {}))
  ([opts]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected valid date-string, got " (u/stringify value))))]
       (try
         (cc/assert (string? value))
         (let [date (js/Date. (js/Date.parse value))]
           (if (date? date)
             (ok date)
             (throw (js/Error. "Invalid Date"))))
         (catch :default _err
           (error (message context))))))))

(defn number->date
  ([] (number->date {}))
  ([opts]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected valid timestamp, got " (u/stringify value))))]
       (try
         (cc/assert (float? value))
         (let [date (js/Date. value)]
           (if (date? date)
             (ok date)
             (throw (js/Error. "Invalid Date"))))
         (catch :default _err
           (error (message context))))))))

(defn date->string
  ([] (date->string {}))
  ([opts]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected valid date, got " (u/stringify value))))]
       (try
         (if (date? value)
           (ok (.toISOString value))
           (throw (js/Error. :invalid)))
         (catch :default _err
           (error (message context))))))))

(defn date->number
  ([] (date->number {}))
  ([opts]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected valid date, got " (u/stringify value))))]
       (try
         (if (date? value)
           (ok (.getTime value))
           (throw (js/Error. :invalid)))
         (catch :default _err
           (error (message context))))))))

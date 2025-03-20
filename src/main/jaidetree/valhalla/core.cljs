(ns jaidetree.valhalla.core
  (:refer-clojure :exclude [hash-map boolean])
  (:require
   [clojure.core :as cc]
   [clojure.pprint :refer [pprint]]
   [clojure.string :as s]
   [jaidetree.valhalla.context :as ctx]))

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
    (throw (js/Error. (str "Could not match status type " (pr-str status))))))

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

(defn string
  ([] (string {}))
  ([opts]
   (fn [{:keys [value _path] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected string, got " (pr-str value))))]
       (if (string? value)
         (ok value)
         (error (message context)))))))

(defn number
  ([] (number {}))
  ([opts]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected number, got " (pr-str value))))]
       (if (number? value)
         (ok value)
         (error (message context)))))))

(defn numeric
  ([] (numeric {}))
  ([opts]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected numeric string, got " (pr-str value))))
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
                             (str "Expected numeric string, got " (pr-str value))))
           value (js/Number.parseFloat value)]
       (if (not (js/Number.isNaN value))
         (ok value)
         (error (message context)))))))

(defn boolean
  ([] (boolean {}))
  ([opts]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected boolean, got " (pr-str value))))]
       (if (boolean? value)
         (ok value)
         (error (message context)))))))

(defn string->boolean
  ([] (string->boolean {}))
  ([opts]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected boolean-string, got " (pr-str value))))]
       (if (not (string? value))
         (error (message context))
         (let [value (s/lower-case value)]
           (case value
             "true" (ok true)
             "false" (ok false)
             (error (message context)))))))))

(defn with-ctx
  [f ctx]
  (f ctx))

(defn hash-map
  [validators-map & [opts]]
  (fn [{:keys [value] :as context}]
    (let [message (msg-fn (:message opts)
                          (fn [{:keys [value]}]
                            (str "Expected hash-map, got " (pr-str value))))]
      (if (not (map? value))
        (error (message context))
        (let [[first-key] (first validators-map)
              context (ctx/path> context first-key)]
          (->> validators-map
               (reduce
                (fn [ctx [key validator]]
                  (let [ctx (if (= first-key key)
                              ctx
                              (ctx/replace-path ctx key))
                        result (validator ctx)]
                    (result-case result
                                 :ok (fn [value]
                                       (ctx/accrete ctx value))
                                 :err (fn [error]
                                        (ctx/raise-error ctx error))
                                 :errs (fn [errors]
                                         (ctx/raise-errors ctx errors)))))

                context)
               (with-ctx
                 (fn [ctx]
                   (if (empty? (:errors ctx))
                     (ok (:output ctx))
                     (errors (:errors ctx)))))))))))


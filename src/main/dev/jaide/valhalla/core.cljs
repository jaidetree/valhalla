(ns dev.jaide.valhalla.core
  (:refer-clojure :exclude [assert hash-map boolean
                            keyword list set symbol
                            uuid vector ->])
  (:require
   [clojure.core :as cc]
   [clojure.string :as s]
   [dev.jaide.pprint :refer [pprint]]
   [dev.jaide.valhalla.context :as ctx]
   [dev.jaide.valhalla.utils :refer [msg-fn] :as u]))

(defn ok
  "Creates a success result with the given value.

  Returns a vector with :v/ok status and the value."
  [value]
  [:v/ok value])

(defn error
  "Creates an error result with the given message.

  Returns a vector with :v/error status and the error message."
  [message]
  [:v/error message])

(defn errors
  "Creates a result containing multiple errors.

  Returns a vector with :v/errors status and a collection of error messages."
  [errors]
  [:v/errors errors])

(defn ok?
  "Checks if a result is successful.

  Returns true if the result has :v/ok status, false otherwise."
  [[status _value]]
  (= status :v/ok))

(defn pass
  "Creates a successful validation result.

  Returns a map with :v/pass status and the input and output values."
  [& {:keys [input output]}]
  {:status :v/pass
   :input  input
   :output output})

(defn fail
  "Creates a failed validation result.

  Returns a map with :v/fail status, the input value, and error messages."
  [& {:keys [input errors]}]
  {:status :v/fail
   :errors errors
   :input  input
   :output nil})

(defn result-case
  "Pattern matches on a validation result and applies the appropriate handler function.

  Takes a result vector [status value] and handler functions for each possible status.
  Returns the result of applying the matching handler to the value."
  [[status val-or-msg] & {:keys [ok err errs ctx]
                          :or {ok identity
                               err identity
                               errs identity
                               ctx identity}}]
  (case status
    :v/ok     (ok val-or-msg)
    :v/error  (err val-or-msg)
    :v/errors (errs val-or-msg)
    :v/ctx    (ctx val-or-msg)
    (throw (js/Error. (str "Could not match status type " (u/stringify status))))))

(defn valid?
  "Checks if a validation result is successful.

  Returns true if the result has :v/pass status, false otherwise."
  [result]
  (= (:status result) :v/pass))

(defn validate
  "Validates an input value using the provided validator function.

  Creates a validation context with the input, applies the validator,
  and returns a validation result map (pass or fail)."
  [validator-fn input & [opts]]
  (cc/assert (fn? validator-fn) "Validator must be a function")
  (let [context (ctx/create :input input)
        result (validator-fn context)]
    (result-case result
                 :ok (fn [value]
                       (cc/-> context
                              (ctx/accrete value)
                              (pass)))
                 :err (fn [error]
                        (cc/-> context
                               (ctx/raise-error error)
                               (fail)))
                 :errs (fn [errors]
                         (cc/-> context
                                (ctx/raise-errors errors)
                                (fail))))))
(defn errors->string
  "Formats a list of error hash-maps into a line-separated string

  Arguments:
  - errors - Sequence of error hash-maps with :path vectors and :message str

  Returns a string of all error messages"
  [errors]
  (cc/->> (for [error errors]
            (str (s/join "." (cc/->> (:path error)
                                     (map #(s/replace (pr-str %) #"^:" ""))))
                 ": " (:message error)))
          (s/join "\n")))

(defn assert-valid
  "Validates input and throws an error if invalid.

  Options:
  - :message - Custom error message function or string

  Returns the validation result if valid
  "
  [validator input & {:keys [message]}]
  (let [result (validate validator input)
        message (cond (string? message) (constantly message)
                      (fn? message)     message
                      :else             (fn [{:keys [errors]}]
                                          (str "ValidationError:\n" (errors->string errors))))]
    (if (valid? result)
      result
      (throw (js/Error. (message result))))))

(defn parse
  "Validates input and throws an error if invalid.

  Options:
  - :message - Custom error message function or string

  Returns the parsed result if valid
  "
  [validator input & {:keys [_message] :as opts}]
  (let [result (assert-valid validator input opts)]
    (:output result)))

(defn- finite?
  [num]
  (js/Number.isFinite num))

(defn string
  "Validates if a value is a string.

  Options:
  - :message - Custom error message function or string

  Returns a validator function that accepts a context and returns a result."
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
  "Validates if a value is a number.

  Options:
  - :message - Custom error message function or string

  Returns a validator function that accepts a context and returns a result."
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
  "Validates if a value can be parsed as a number.

  Options:
  - :message - Custom error message function or string

  Returns a validator function that accepts a context and returns a result.
  The original string value is returned if valid."
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
  "Converts a string to a number.

  Options:
  - :message - Custom error message function or string
  - :accept-numbers - Boolean to accept pre-transformed numbers

  Returns a validator function that accepts a context and returns a result
  with the parsed number value if successful."
  ([] (string->number {}))
  ([{:keys [accept-numbers] :as opts}]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected numeric string, got " (u/stringify value))))]
       (try
         (cond
           (and accept-numbers (finite? value))
           (ok value)

           (not (string? value))
           (throw (js/Error. :fail))

           :else
           (let [value (js/Number.parseFloat value)]
             (if (finite? value)
               (ok value)
               (throw (js/Error. :fail)))))
         (catch :default _err
           (error (message context))))))))

(defn boolean
  "Validates if a value is a boolean.

  Options:
  - :message - Custom error message function or string

  Returns a validator function that accepts a context and returns a result."
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
  "Converts a string ('true' or 'false') to a boolean.

  Options:
  - :message - Custom error message function or string
  - :accept-booleans - Boolean to accept pre-transformed values

  Returns a validator function that accepts a context and returns a result
  with the parsed boolean value if successful."
  ([] (string->boolean {}))
  ([{:keys [accept-booleans] :as opts}]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected boolean-string, got " (u/stringify value))))]
       (try
         (cond
           (and accept-booleans (boolean? value))
           (ok value)

           (string? value)
           (let [value (s/lower-case value)]
             (case value
               "true" (ok true)
               "false" (ok false)
               (error (message context))))

           :else
           (throw (js/Error. :invalid)))
         (catch :default _err
           (error (message context))))))))

(defn keyword
  "Validates if a value is a keyword.

  Options:
  - :message - Custom error message function or string

  Returns a validator function that accepts a context and returns a result."
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
  "Converts a string to a keyword.

  Options:
  - :message - Custom error message function or string
  - :accept-keywords - Boolean to accept pre-transformed keywords

  Returns a validator function that accepts a context and returns a result
  with the converted keyword if successful."
  ([] (string->keyword {}))
  ([{:keys [accept-keywords] :as opts}]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected keyword-string, got " (u/stringify value))))]
       (try
         (cond
           (and accept-keywords (keyword? value))
           (ok value)

           (and (string? value)
                (some? (re-find #"^:?[a-zA-Z][-_a-zA-Z0-9\/]*$" value)))
           (let [value (s/replace value #"^:" "")]
             (ok (cc/keyword value)))

           :else (throw (js/Error. :invalid)))
         (catch :default _err
           (error (message context))))))))

(defn symbol
  "Validates if a value is a symbol.

  Options:
  - :message - Custom error message function or string

  Returns a validator function that accepts a context and returns a result."
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
  "Converts a string to a symbol.

  Options:
  - :message - Custom error message function or string
  - :accept-symbols - Boolean to accept pre-transformed symbols

  Returns a validator function that accepts a context and returns a result
  with the converted symbol if successful. The string must match the pattern
  for valid symbols (starting with a letter followed by valid characters)."
  ([] (string->symbol {}))
  ([{:keys [accept-symbols] :as opts}]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected symbol-string, got " (u/stringify value))))]
       (try
         (cond
           (and accept-symbols (symbol? value))
           (ok value)

           (and (string? value)
                (some? (re-find #"^[a-zA-Z][-_a-zA-Z0-9\/]*$" value)))
           (ok (cc/symbol value))

           :else
           (throw (js/Error. :invalid)))
         (catch :default _err
           (error (message context))))))))

(defn regex
  "Validates if a string matches a regular expression pattern.

  Arguments:
  - regex-str - The regular expression pattern as a string

  Options:
  - :message - Custom error message function or string

  Returns a validator function that accepts a context and returns a result
  with the original string if it matches the pattern."
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
  "Validates if a string is a valid UUID.

  Options:
  - :message - Custom error message function or string

  Returns a validator function that accepts a context and returns a result
  with the original UUID string if valid."
  ([] (uuid {}))
  ([opts]
   (regex "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
          (merge opts
                 {:message (fn [{:keys [value]}]
                             (str "Expected UUID string, got " (u/stringify value)))}))))

(defn nil-value
  "Validates if a value is nil.

  Options:
  - :message - Custom error message function or string

  Returns a validator function that accepts a context and returns a result."
  ([] (nil-value {}))
  ([opts]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected nil, got " (u/stringify value))))]
       (if (nil? value)
         (ok value)
         (error (message context)))))))

(defn- reduce-validators
  [& {:keys [context validator-kvs path-index output]}]
  (let [context (ctx/update-output context output)]
    (cc/->> validator-kvs
            (reduce
             (fn [ctx [key validator]]
               (let [ctx (ctx/replace-path ctx path-index key)
                     result (validator (cc/-> ctx
                                              (assoc :errors [])))]
                 (result-case result
                              :ok (fn [value]
                                    (cc/-> ctx
                                           (ctx/accrete key value)))
                              :err (fn [error]
                                     (ctx/raise-error ctx error))
                              :errs (fn [errors]
                                      (ctx/raise-errors ctx errors)))))
             context))))

(defn- vector-validator
  [{:keys [context validator path-index]}]
  (let [error-idx (count (:errors context))
        context (cc/-> context
                       (ctx/update-output [])
                       (assoc :value-idx 0)
                       (assoc :output-idx 0)
                       (assoc :ok true))]
    (cc/->> (:value context)
            (reduce
             (fn [ctx _value]
               (let [{:keys [value-idx output-idx]} ctx
                     ctx (cc/-> ctx
                                (ctx/replace-path path-index value-idx))
                     result (validator (cc/-> ctx
                                              (assoc :errors [])))]
                 (result-case result
                              :ok (fn [value]
                                    (cc/-> (if (:ok ctx)
                                             (ctx/accrete ctx output-idx value)
                                             ctx)
                                           (update :output-idx inc)
                                           (update :value-idx inc)))

                              :err (fn [error]
                                     (cc/-> ctx
                                            (ctx/raise-error error)
                                            (assoc :ok false)
                                            (update :value-idx inc)))

                              :errs (fn [errors]
                                      (cc/-> ctx
                                             (ctx/raise-errors errors)
                                             (assoc :ok false)
                                             (update :value-idx inc))))))
             context))))

(defn vector
  "Applies a validator to every item in a vector

  Arguments:
  - validator - A validator function to apply to each element

  Options:
  - :message - Custom error message function or string

  Returns a validator function that accepts a context and returns a result
  with a vector of validated elements if successful."
  ([validator & [opts]]
   (cc/assert (fn? validator) "Validator is not a function")
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected vector, got " (u/stringify value))))]
       (if (vector? value)
         (let [idx (count (:path context))
               ctx (vector-validator
                    {:context context
                     :validator validator
                     :path-index idx})]
           (if (empty? (:errors ctx))
             (ok (vec (:output ctx)))
             (errors (:errors ctx))))
         (error (message context)))))))

(defn vector-tuple
  "Validates if a value is a vector with specific validators for each position.

  Arguments:
  - validators - A vector of validator functions, one for each position

  Options:
  - :message - Custom error message function or string

  Returns a validator function that accepts a context and returns a result
  with a vector of validated elements if successful. The input vector must have
  the same length as the validators vector."
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
                     :validator-kvs (map-indexed cc/vector validators)
                     :output []})]
           (if (empty? (:errors ctx))
             (ok (vec (:output ctx)))
             (errors (:errors ctx))))

         (error (message context)))))))

(defn list
  "Validates if a value is a list and validates each element.

  Arguments:
  - validator - A validator function to apply to each element

  Options:
  - :message - Custom error message function or string

  Returns a validator function that accepts a context and returns a result
  with a sequence of validated elements if successful."
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
                     :validator-kvs (map-indexed cc/list (repeat (count value) validator))
                     :output []})]
           (if (empty? (:errors ctx))
             (ok (vals (:output ctx)))
             (errors (:errors ctx))))
         (error (message context)))))))

(defn list-tuple
  "Validates if a value is a list with specific validators for each position.

  Arguments:
  - validators - A sequence of validator functions, one for each position

  Options:
  - :message - Custom error message function or string

  Returns a validator function that accepts a context and returns a result
  with a sequence of validated elements if successful. The input list must have
  the same length as the validators sequence."
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
                     :validator-kvs (map-indexed cc/vector validators)
                     :output []})]
           (if (empty? (:errors ctx))
             (ok (seq (:output ctx)))
             (errors (:errors ctx))))
         (error (message context)))))))

(defn set
  "Validates if a value is a set and validates each element.

  Arguments:
  - validator - A validator function to apply to each element

  Options:
  - :message - Custom error message function or string

  Returns a validator function that accepts a context and returns a result
  with a set of validated elements if successful."
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
                    :validator-kvs (map-indexed cc/list (repeat (count value) validator))
                    :output []})]
          (if (empty? (:errors ctx))
            (ok (into #{} (:output ctx)))
            (errors (:errors ctx))))
        (error (message context))))))

(defn- extract-errors
  [status result]
  (result-case
   [status result]
   :ok (fn [_value] [])
   :err (fn [error]
          [error])
   :errors (fn [errors]
             errors)))

(defn- hash-map-error
  [err ctx idx path]
  (if (string? err)
    {:path (into (:path ctx) [idx path]) :message err}
    (update err :path #(into % [idx path]))))

(defn- map->seq
  "Transforms a hash-map into sequence temporarily. This supports accurate
  pathing so that 0 refers to the key and 1 the value, making errors more
  useful refering to the key or value. Ensure not used in final context
  as it would inadvertently transform the output type.

  Arguments:
  - ctx - Validation context with :input :path :errors :output attrs

  Returns ctx with input converted.
  "
  [ctx]
  (let [{:keys [input path]} ctx]
    (if (map? (get-in input path))
      (update-in ctx (cons :input path) vec)
      ctx)))

(defn- hash-map-validators
  [& {:keys [context k-val v-val path-index]}]
  (let [context (ctx/update-output context {})]
    (cc/->> (:value context)
            (reduce
             (fn [ctx [key value]]
               (let [index (:index ctx)
                     [k-status k-result]
                     (cc/-> ctx
                            (map->seq)
                            (update-in [:path] into [index 0])
                            (ctx/update-value key)
                            (k-val))
                     [v-status v-result]
                     (cc/-> ctx
                            (ctx/replace-path path-index key)
                            (ctx/update-value value)
                            (v-val))]

                 (if (and (= k-status :v/ok) (= v-status :v/ok))
                   (cc/-> ctx
                          (update :index inc)
                          (ctx/accrete k-result v-result))
                   (let [k-errors (cc/->> (extract-errors k-status k-result)
                                          (map #(hash-map-error % ctx index 0)))
                         v-errors (cc/->> (extract-errors v-status v-result)
                                          (map #(hash-map-error % ctx index 1)))
                         errors (concat k-errors v-errors)]
                     (cc/-> ctx
                            (update :index inc)
                            (ctx/raise-errors errors))))))
             (assoc context :index 0)))))

(defn hash-map
  "Validates if a value is a hash-map with a key and value type.

  Arguments:
  - key - A key validator function
  - value - A value validator function

  Options:
  - :message - Custom error message function or string

  Returns a validator function that accepts a context and returns a result
  with a hash-map of unknown size."
  ([value-validator]  (hash-map (keyword) value-validator {}))
  ([key-validator value-validator]  (hash-map key-validator value-validator {}))
  ([key-validator value-validator opts]
   (cc/assert (fn? key-validator) "Key validator must be a function")
   (cc/assert (fn? value-validator) "Value validator must be a function")
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected hash-map, got " (u/stringify value))))]
       (if (map? value)
         (let [idx (count (:path context))
               ctx (hash-map-validators
                    {:context context
                     :path-index idx
                     :k-val key-validator
                     :v-val value-validator})]
           (if (empty? (:errors ctx))
             (ok (:output ctx))
             (errors (:errors ctx))))
         (error (message context)))))))

(defn record
  "Validates if a value is a hash-map and validates specific keys.

  Arguments:
  - validators-map - A hash-map of keys to validator functions

  Options:
  - :message - Custom error message function or string

  Returns a validator function that accepts a context and returns a result
  with a map of validated key-value pairs if successful."
  [validators-map & [opts]]
  (cc/assert (map? validators-map) "Validators must be a hash-map with keywords and validator functions")
  (fn [{:keys [value] :as context}]
    (let [message (msg-fn (:message opts)
                          (fn [{:keys [value]}]
                            (str "Expected hash-map record, got " (u/stringify value))))]
      (if (not (map? value))
        (error (message context))
        (let [idx (count (:path context))
              ctx (reduce-validators
                   {:context context
                    :validator-kvs validators-map
                    :path-index idx
                    :output {}})]
          (if (empty? (:errors ctx))
            (ok (:output ctx))
            (errors (:errors ctx))))))))

(defn assert
  "Validates if a value satisfies a predicate function.

  Arguments:
  - pred? - A predicate function that returns true for valid values

  Options:
  - :message - Custom error message function or string

  Returns a validator function that accepts a context and returns a result
  with the original value if the predicate returns true."
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
  "Validates if a value is an instance of a specific class.

  Arguments:
  - class-fn - A JavaScript constructor function or class

  Options:
  - :message - Custom error message function or string

  Returns a validator function that accepts a context and returns a result
  with the original value if it's an instance of the specified class."
  ([class-fn] (instance class-fn {}))
  ([class-fn opts]
   (cc/assert (fn? class-fn) "Class function required")
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
  (and (instance? js/Date date)
       (finite? (.getTime date))))

(defn date
  "Validates if a value is a valid JavaScript Date object.

  Options:
  - :message - Custom error message function or string

  Returns a validator function that accepts a context and returns a result
  with the original Date object if valid."
  [& [opts]]
  (fn [{:keys [value] :as context}]
    (let [message (msg-fn (:message opts)
                          (fn [{:keys [value]}]
                            (str "Expected valid Date, got " (u/stringify value))))]
      (if (date? value)
        (ok value)
        (error (message context))))))

(defn string->date
  "Converts a string to a JavaScript Date object.

  Options:
  - :message - Custom error message function or string
  - :accept-dates - Boolean to accept pre-transformed, valid dates

  Returns a validator function that accepts a context and returns a result
  with a Date object if the string can be parsed as a valid date."
  ([] (string->date {}))
  ([{:keys [accept-dates] :as opts}]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected valid date-string, got " (u/stringify value))))]
       (try
         (if (and accept-dates
                  (date? value))
           (ok value)
           (do
             (cc/assert (string? value))
             (let [unixtime (js/Date.parse value)
                   date (if (finite? unixtime)
                          (js/Date. unixtime)
                          (throw (js/Error. "Invalid Date")))]
               (if (date? date)
                 (ok date)
                 (throw (js/Error. "Invalid Date"))))))
         (catch :default _err
           (error (message context))))))))

(defn number->date
  "Converts a number (timestamp) to a JavaScript Date object.

  Options:
  - :message - Custom error message function or string
  - :accept-dates - Boolean to accept pre-transformed, valid dates

  Returns a validator function that accepts a context and returns a result
  with a Date object if the number represents a valid timestamp."
  ([] (number->date {}))
  ([{:keys [accept-dates] :as opts}]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected valid timestamp, got " (u/stringify value))))]
       (try
         (if (and accept-dates
                  (date? value))
           (ok value)
           (do
             (cc/assert (finite? value))
             (let [date (js/Date. value)]
               (if (date? date)
                 (ok date)
                 (throw (js/Error. "Invalid Date"))))))
         (catch :default _err
           (error (message context))))))))

(defn- iso8601?
  [s]
  (some? (re-find #"[\d]{4}-[\d]{2}-[\d]{2}T[\d]{2}:[\d]{2}:[\d]{2}.[\d]{3}(?:Z|[-+][\d]{2}:[\d]{2})" s)))

(comment
  (cc/-> (js/Date.) (.toISOString))
  (iso8601? (cc/-> (js/Date.) (.toISOString)))
  (iso8601? (cc/-> (js/Date.) (.toISOString) (.replace #"Z" "-04:00"))))

(defn date->string
  "Converts a JavaScript Date object to an ISO string.

  Options:
  - :message - Custom error message function or string
  - :accept-strings - Boolean to accept pre-transformed, pattern-tested stings.

  Returns a validator function that accepts a context and returns a result
  with the ISO string representation of the date."
  ([] (date->string {}))
  ([{:keys [accept-strings] :as opts}]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected valid date, got " (u/stringify value))))]
       (try
         (cond
           (and accept-strings
                (string? value)
                (iso8601? value)) (ok value)
           (date? value) (ok (.toISOString value))
           :else (throw (js/Error. :invalid)))
         (catch :default _err
           (error (message context))))))))

(defn date->number
  "Converts a JavaScript Date object to a timestamp number.

  Options:
  - :message - Custom error message function or string
  - :accept-numbers - Boolean to accept pre-transformed numbers

  Returns a validator function that accepts a context and returns a result
  with the timestamp (milliseconds since epoch) of the date."
  ([] (date->number {}))
  ([{:keys [accept-numbers] :as opts}]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected valid date, got " (u/stringify value))))]
       (try
         (cond
           (and accept-numbers (number? value)) (ok value)
           (date? value) (ok (.getTime value))
           :else (throw (js/Error. :invalid)))
         (catch :default _err
           (error (message context))))))))

(defn nilable
  "Creates a validator that allows nil values or validates non-nil values.

  Arguments:
  - validator - A validator function to apply to non-nil values

  Returns a validator function that accepts a context and returns a result
  with nil for nil values or the result of applying the validator to non-nil values."
  [validator]
  (fn [{:keys [value] :as context}]
    (if (nil? value)
      (ok nil)
      (validator context))))

(defn enum
  "Validates if a value is one of a set of keywords.

  Arguments:
  - kws - A collection of keywords representing valid enum values

  Options:
  - :message - Custom error message function or string

  Returns a validator function that accepts a context and returns a result
  with the original keyword if it's one of the specified enum values."
  ([kws] (enum kws {}))
  ([kws opts]
   (cc/assert (every? keyword? kws) "Enum values must be keywords")
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected keyword one of " (s/join ", " kws) ", got " (u/stringify value))))]
       (try
         (if (and (keyword? value)
                  (contains? (cc/set kws) value))
           (ok value)
           (throw (js/Error. :fail)))
         (catch :default _err
           (error (message context))))))))

(defn literal
  "Validates if a value equals an expected literal value.

  Arguments:
  - expected - The exact value to match against

  Options:
  - :message - Custom error message function or string

  Returns a validator function that accepts a context and returns a result
  with the original value if it equals the expected value."
  ([expected] (literal expected {}))
  ([expected opts]
   (fn [{:keys [value] :as context}]
     (let [message (msg-fn (:message opts)
                           (fn [{:keys [value]}]
                             (str "Expected literal " (u/stringify expected) ", got " (u/stringify value))))]
       (try
         (if (= value expected)
           (ok value)
           (throw (js/Error. :fail)))
         (catch :default _err
           (error (message context))))))))

(defn- chain-validators
  [{:keys [validators context]}]
  (cc/->> validators
          (reduce
           (fn [ctx validator]
             (let [result (validator ctx)]
               (result-case result
                            :ok (fn [value]
                                  (cc/-> context
                                         (ctx/accrete value)))
                            :err (fn [error]
                                   (cc/-> context
                                          (ctx/raise-error error)
                                          (fail)
                                          (reduced)))
                            :errs (fn [errors]
                                    (cc/-> context
                                           (ctx/raise-errors errors)
                                           (fail)
                                           (reduced))))))
           context)))

(defn chain
  "Creates a validator that applies multiple validators in sequence.

  Arguments:
  - validators - A sequence of validator functions to apply in order

  Returns a validator function that accepts a context and returns a result.
  Each validator is applied to the result of the previous validator.
  If any validator fails, the chain stops and returns the error."
  [& validators]
  (fn [{:keys [] :as context}]
    (let [ctx (chain-validators
               {:validators validators
                :context    context})]
      (if (empty? (:errors ctx))
        (ok (:output ctx))
        (errors (:errors ctx))))))

(def ->
  "Creates a validator that applies multiple validators in sequence.

  Arguments:
  - validators - A sequence of validator functions to apply in order

  Returns a validator function that accepts a context and returns a result.
  Each validator is applied to the result of the previous validator.
  If any validator fails, the chain stops and returns the error."
  chain)

(defn- union-validators
  [{:keys [validators context]}]
  (let [errors-idx (count (:errors context))]
    (cc/->> validators
            (reduce
             (fn [ctx validator]
               (let [ctx (cc/-> ctx
                                (update :errors #(vec (take errors-idx %))))
                     result (validator ctx)]
                 (result-case
                  result
                  :ok (fn [value]
                        (cc/-> context
                               (ctx/accrete value)
                               (pass)
                               (reduced)))
                  :err (fn [error]
                         (cc/-> context
                                (ctx/clear-errors)
                                (ctx/raise-error error)))
                  :errs (fn [errors]
                          (cc/-> context
                                 (ctx/clear-errors)
                                 (ctx/raise-errors errors))))))
             context))))

(defn union
  "Creates a validator that tries multiple validators and succeeds if any one succeeds.

  Arguments:
  - validators - A sequence of validator functions to try

  Returns a validator function that accepts a context and returns a result.
  Each validator is tried in order until one succeeds. If all validators fail,
  returns the errors from the last validator."
  [& validators]
  (fn [{:keys [_value] :as context}]
    (let [ctx (union-validators
               {:validators validators
                :context    (ctx/clear-errors context)})]
      (if (empty? (:errors ctx))
        (ok (:output ctx))
        (errors (:errors ctx))))))

(defn default
  "Creates a validator that provides a default value for nil inputs.

  Arguments:
  - validator - A validator function to apply to non-nil values
  - default-value-or-fn - A value or function to use as default for nil values

  Returns a validator function that accepts a context and returns a result.
  If the input is nil, returns the default value or the result of calling
  the default function with the context."
  [validator default-value-or-fn]
  (fn [{:keys [value] :as context}]
    (if (nil? value)
      (ok (if (fn? default-value-or-fn)
            (default-value-or-fn context)
            default-value-or-fn))
      (validator context))))

(defn lazy
  "Creates a validator that lazily evaluates a validator function.

  Arguments:
  - validator-fn - A function that returns a validator function

  Options:
  - :message - Custom error message function or string

  Returns a validator function that accepts a context and returns a result.
  The validator-fn is called to get the actual validator only when needed,
  which allows for recursive validator definitions."
  [validator-fn & [opts]]
  (fn [context]
    (let [message (msg-fn (:message opts)
                          (fn [{:keys [value]}]
                            (str "Expected validator function, got " (u/stringify value))))]
      (try
        (if (fn? validator-fn)
          (let [validator (validator-fn)]
            (validator context))
          (throw (js/Error. :fail)))
        (catch :default _err
          (error (message context)))))))

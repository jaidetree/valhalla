<img alt="Valhalla Logo" src="doc/valhalla-logo.svg" style="width: 500px; height:auto;" />

# Valhalla

A ClojureScript validation library for parsing and validating data with an emphasis on intuitive interfaces and JavaScript interoperability.

## Introduction

Valhalla is a ClojureScript validation library designed to make data validation and parsing straightforward and developer-friendly. It provides a simple yet powerful API for defining validation schemas and generating user-friendly error messages.

### Why not just use spec?

While [clojure.spec](https://clojure.org/guides/spec) is a powerful tool for data validation, Valhalla offers several advantages:

- **Intuitive interface**: Valhalla provides a more intuitive collection of combinators, reflects the shape of your data
- **Improved JS interop**: First-class support for JavaScript data structures and seamless conversion between JS and ClojureScript
- **User-friendly error messages**: Detailed, customizable error messages that help users understand what went wrong

### Tradeoffs

- Runtime only, no static analysis
- Less extensible, can be customized using with-redefs
- Aimed for app development vs library contract verification

## Installation

Add Valhalla to your project using your preferred dependency management tool:

### deps.edn

```clojure
{:deps {dev.jaide/valhalla {:mvn/version "2025.3.28"}}}
```

### Leiningen/Boot

```clojure
[dev.jaide/valhalla "2025.3.28"]
```

## Example

Here's a quick example to get you started with Valhalla:

### Validating a record

```clojure
(ns my-app.core
  (:require [dev.jaide.valhalla.core :as v]))

;; Define a validation schema
(def user-schema
  (v/record
    {:name (v/string)
     :age (v/number)
     :email (v/string {:pattern #"^[^@]+@[^@]+\.[^@]+$"})
     :roles (v/array (v/string))}))

;; Validate data
(def valid-user
  {:name "Jane Doe"
   :age 30
   :email "jane@example.com"
   :roles ["admin" "user"]})

(v/validate user-schema valid-user)
;; => {:status :v/pass
;;     :input {:name "Jane Doe"
;;             :age 30
;;             :email "jane@example.com"
;;             :roles ["admin" "user"]}
;;     :output {:name "Jane Doe",
;;              :age 30,
;;              :email "jane@example.com",
;;              :roles ["admin" "user"]}}

;; Invalid data will return errors
(def invalid-user
  {:name "John Doe"
   :age "not a number"
   :email "not-an-email"
   :roles "admin"})

(v/validate user-schema invalid-user)
;; => {:status :v/fail,
;;     :errors [...detailed error information...]}
```

### Custom error messages

You can customize error messages for better user experience:

```clojure
(def user-schema
  (v/record
    {:name (v/string {:message "Name must be a string"})
     :age (v/number {:message "Age must be a number"})
     :email (v/string {:pattern #"^[^@]+@[^@]+\.[^@]+$"
                       :message "Please provide a valid email address"})
     :roles (v/array (v/string)
                     {:message (fn [{:keys [value]}]
                                (str "Roles must be an array of strings, got " (pr-str value)})}))
```

### Converting JS data to Clojure data

Valhalla makes it easy to work with JavaScript data:

```clojure
(ns my-app.core
  (:require [dev.jaide.valhalla.js :as jsv]))

;; Define a schema for JS objects
(def js-user-schema
  (jsv/object
    {:name (jsv/string)
     :age (jsv/number)
     :preferences (jsv/object
                    {:theme (jsv/string)
                     :notifications (jsv/boolean)})}))

;; Parse JS data into ClojureScript data
(def js-user #js {:name "Alice"
                  :age 25
                  :preferences #js {:theme "dark"
                                   :notifications true}})

(v/validate js-user-schema js-user)
;; => {:status :v/pass
;;     :input #js {:name "Alice"
;;                 :age 25
;;                 :preferences #js {:theme "dark"
;;                                   :notifications true}}
;;     :output {:name "Alice", :age 25, :preferences {:theme "dark", :notifications true}}}
```

## API

- **assert-valid** - Applies a validator to data, throws an error if invalid
  Arguments:
  - `validator` - A validator function
  - `input` - Input data to validate
  - `opts` - Optional keyword settings

  Options:
  - `:message` - A string or function to format validator output. Only called if validator fails

  Returns a hash-map like the following if input is valid:

  ```clojure
  {:status :v/pass
   :input  original-input
   :output parsed-output-shape}
  ```

  By default, it will stringify error messages with `v/errors->string`.

- **errors->string** - Format a collection of validation errors into a line-separated string
  Arguments:
  - `errors` - A collection of error hash-maps with `:path` vector and `:message` string

  Example:
  ```clojure
  (v/errors->string [{:path [:a :z] :message "Expected string, got :kw"}
                     {:path [:b :y] :message "Expected number, got \"str\""}
                     {:path [:c :x] :message "Expected keyword, got 5"}]
  ;; #=>
  ;;  "a.z: Expected string, got :kw
  ;;  b.y: Expected number, got \"str\"
  ;;  c.x: Expected keyword, got 5"
  ```

- **validate** - Applies a validator to data returns the result hash-map
  Arguments:
  - `validator` - A validator function
  - `input` - Input data to validate

  Options:
  - No options supported yet. Let me know if you can think of any useful ones!

  Returns a hash-map like the following if input is valid:

  ```clojure
  {:status :v/pass
   :input  original-input
   :output parsed-output-shape}
  ```

  Returns a hash-map like the following if input is invalid:

  ```clojure
  {:status :v/fail
   :input  original-input
   :errors [{:path [:a] :message "Expected string, got 5"}
            {:path [:b] :message "Expected keyword, got \"str\""}
            {:path [:c] :message "Expected number, got :kw"}]
   :output nil}
  ```
  There is also a utility function called `v/pass?` for testing the results, it can be used like the following:

  ```clojure
  (let [result (v/validate (v/number) 5)]
    (if (v/pass? result)
      "Success"
      "Failure"))
  ```

## Validators

Valhalla provides a set of validators for different data types:

### Primitive Values

- **boolean** - Validates if a value is a boolean.
  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected boolean, got [value]"

  Example:
  ```clojure
  (v/validate (v/boolean) true)  ; Valid
  (v/validate (v/boolean) "true")  ; Invalid - returns error
  (v/validate (v/boolean {:message "Must be true or false"}) 123)  ; Invalid with custom message
  ```
- **keyword** - Validates if a value is a keyword.
  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected keyword, got [value]"

  Example:
  ```clojure
  (v/validate (v/keyword) :user/admin)  ; Valid
  (v/validate (v/keyword) "not-a-keyword")  ; Invalid - returns error
  (v/validate (v/keyword {:message "Must be a keyword"}) 123)  ; Invalid with custom message
  ```
- **nil-value** - Validates if a value is nil.
  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected nil, got [value]"

  Example:
  ```clojure
  (v/validate (v/nil-value) nil)  ; Valid
  (v/validate (v/nil-value) "something")  ; Invalid - returns error
  ```
- **number** - Validates if a value is a number.
  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected number, got [value]"

  Example:
  ```clojure
  (v/validate (v/number) 42)  ; Valid
  (v/validate (v/number) 3.14)  ; Valid
  (v/validate (v/number) "42")  ; Invalid - returns error
  ```

- **numeric** - Validates if a string contains a numeric value.
  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected numeric string, got [value]"

  Example:
  ```clojure
  (v/validate (v/numeric) "42")  ; Valid - returns "42" (preserves string)
  (v/validate (v/numeric) "3.14")  ; Valid - returns "3.14" (preserves string)
  (v/validate (v/numeric) "not-a-number")  ; Invalid - returns error
  ```

- **string** - Validates if a value is a string.
  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected string, got [value]"

  Example:
  ```clojure
  (v/validate (v/string) "hello")  ; Valid
  (v/validate (v/string) 42)  ; Invalid - returns error
  ```

- **symbol** - Validates if a value is a symbol.
  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected symbol, got [value]"

  Example:
  ```clojure
  (v/validate (v/symbol) 'my-symbol)  ; Valid
  (v/validate (v/symbol) 'user/function)  ; Valid
  (v/validate (v/symbol) :keyword)  ; Invalid - returns error
  ```

### Coercion

- **string->boolean** - Parses a string into a boolean
  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected string-boolean, got [value]"

  Example:
  ```clojure
  (v/validate (v/string->boolean) "true")  ; Valid
  (v/validate (v/string->boolean) "false")  ; Valid
  (v/validate (v/string->boolean) :keyword)  ; Invalid - returns error
  ```

- **string->keyword** - Parses a string into a keyword
  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected string-keyword, got [value]"

  Example:
  ```clojure
  (v/validate (v/string->keyword) "keyword")  ; Valid
  (v/validate (v/string->keyword) "hello world")  ; Invalid - returns error
  (v/validate (v/string->keyword) :keyword)  ; Invalid - returns error
  ```

- **string->number** - Parses a string into a number
  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected string-number, got [value]"

  Example:
  ```clojure
  (v/validate (v/string->number) "5")  ; Valid
  (v/validate (v/string->number) "100.42")  ; Valid
  (v/validate (v/string->number) :keyword)  ; Invalid - returns error
  ```

- **string->symbol** - Parses a string into a symbol
  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected string-symbol, got [value]"

  Example:
  ```clojure
  (v/validate (v/string->symbol) "a-sym")  ; Valid
  (v/validate (v/string->symbol) "a-ns/a-sym")  ; Valid
  (v/validate (v/string->symbol) 55)  ; Invalid - returns error
  ```

### Advanced

- **assert** - Validates if a value satisfies a predicate function
  Arguments:
  - `predicate-fn` A function that takes a value and returns true or false

  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Assert failed, got [value]"

  Example:
  ```clojure
  (v/validate (v/assert string?) "a-sym")  ; Valid
  (v/validate (v/assert string?) 55)  ; Invalid - returns error
  ```

- **enum** - Validates if a value is within a sequence of values
  Arguments:
  - `keyword-args` A sequence of possible keyword values

  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected keyword of [kws], got [value]"

  Example:
  ```clojure
  (v/validate (v/enum [:a :b :c]) :a)  ; Valid
  (v/validate (v/enum [:a :b :c]) :d)  ; Invalid - returns error
  (v/validate (v/enum [:a :b :c]) "c")  ; Invalid - returns error
  ```

- **instance** - Validates if a value is an instance of an expected class
  Arguments:
  - `class-function` A class constructor function

  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected instance of [name], got [value]"

  Example:
  ```clojure
  (v/validate (v/instance js/Date) (js/Date.))  ; Valid
  (v/validate (v/instance js/Date) "str") ; Invalid - returns error
  ```

- **literal** - Validates if a value equals an expected literal value
  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected literal [expected], got [value]"

  Example:
  ```clojure
  (v/validate (v/literal "str") "str")  ; Valid
  (v/validate (v/literal "str") "other") ; Invalid - returns error
  ```

- **regex** - Validates if a value matches an expected regex pattern string
  Arguments:
  - `pattern-string` A regex string to test values against

  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected string matching [pattern], got [value]"

  Example:
  ```clojure
  (v/validate (v/regex "[a-z0-9]+") "kebab-case")  ; Valid
  (v/validate (v/regex "[a-z0-9]+") "PascalCase") ; Invalid - returns error
  ```

- **uuid** - Validates if a value is a valid UUID
  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected UUID string, got [value]"

  Example:
  ```clojure
  (v/validate (v/uuid) "d888f669-f177-49c4-a2f2-bedfc4bb6f61")  ; Valid
  (v/validate (v/uuid) "other-string") ; Invalid - returns error
  (v/validate (v/uuid) :other-type) ; Invalid - returns error
  ```

### Optionality

- **nilable** - Creates a validator that allows nil values or validates non-nil values
  Options:
  - `validator` - A validator function to pass through if non-nil

  Example:
  ```clojure
  (v/validate (v/nilable (v/string)) nil)  ; Valid
  (v/validate (v/nilable (v/string)) "other-string") ; Valid
  (v/validate (v/nilable (v/string)) :other-type) ; Invalid - returns error
  ```

### Collections

- **hash-map** - Validates if a value is a hash-map with a key and value type
  Arguments:
  - `key-validator` - A validator function for each key, defaults to `keyword`
  - `value-validator` - A validator function for each value

  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected hash-map, got [value]"

  Example:
  ```clojure
  (v/validate (v/hash-map (v/string)) {:key "value"})  ; Valid
  (v/validate (v/hash-map (v/string) (v/string)) {"key" "value"})  ; Valid
  (v/validate (v/hash-map (v/string) (v/string)) {55 "value"})  ; Invalid - returns error
  (v/validate (v/hash-map (v/string) (v/string)) :other)  ; Invalid - returns error
  ```

- **list** - Validates if a value is a list and validates each element
  Arguments:
  - `value-validator` - A validator function for each value

  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected list, got [value]"

  Example:
  ```clojure
  (v/validate (v/list (v/string)) '("str" "str1" "str2"))  ; Valid
  (v/validate (v/list (v/string)) '(:kw "str1" "str2"))  ; Invalid - returns error
  (v/validate (v/list (v/string)) ["str"])  ; Invalid - returns error
  (v/validate (v/list (v/string)) :other)  ; Invalid - returns error
  ```

- **record** - Validates if a value is a hash-map and validates specific keys
  Arguments:
  - `hash-map` - A hash-map mapping keys to validator functions

  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected hash-map record, got [value]"

  Example:
  ```clojure
  (v/validate
    (v/hash-map {:a (v/string)
                 :b (v/number)
                 :c (v/keyword)}) {:a "str" :b 5 :c :kw})  ; Valid
  (v/validate
    (v/hash-map {:a (v/string)
                 :b (v/number)
                 :c (v/keyword)}) {:a "str" :b 5 :c :kw :d 'sym})  ; Invalid - returns error
  (v/validate
    (v/hash-map {:a (v/string)
                 :b (v/number)
                 :c (v/keyword)}) {:a "str" :b "str2" :c :kw})  ; Invalid - returns error
  (v/validate
    (v/hash-map {:a (v/string)
                 :b (v/number)
                 :c (v/keyword)}) [:a :b :c])  ; Invalid - returns error
  ```

- **set** - Validates if a value is a set and validates each element
  Arguments:
  - `value-validator` - A validator for every value in a set

  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected set, got [value]"

  Example:
  ```clojure
  (v/validate (v/set (v/keyword)) #{:a :b :c})  ; Valid
  (v/validate (v/set (v/keyword)) #{:a "str" :c})  ; Invalid - returns error
  (v/validate (v/set (v/keyword)) :oops)  ; Invalid - returns error
  ```

- **vector** - Applies a validator to every item in a vector
  Arguments:
  - `value-validator` - A validator for every value in a vector

  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected vector, got [value]"

  Example:
  ```clojure
  (v/validate (v/vector (v/keyword)) [:a :b :c])  ; Valid
  (v/validate (v/vector (v/keyword)) [:a "str" :c])  ; Invalid - returns error
  (v/validate (v/vector (v/keyword)) :oh-no)  ; Invalid - returns error
  ```

### Tuples

- **vector-tuple** - Validates if a value is a vector with specific validators for each position
  Arguments:
  - `validators` - A vector of validator functions

  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected vector-tuple of length [size], got [value]"

  Example:
  ```clojure
  (v/validate (v/vector-tuple [(v/keyword)
                               (v/string)
                               (v/number)]) [:a "str" 5])  ; Valid
  (v/validate (v/vector-tuple [(v/keyword)
                               (v/string)
                               (v/number)]) [5 :a "str"])  ; Invalid - returns error
  (v/validate (v/vector-tuple [(v/keyword)
                               (v/string)
                               (v/number)]) [:a "str" 5 :b])  ; Invalid - returns error
  (v/validate (v/vector-tuple [(v/keyword)
                               (v/string)
                               (v/number)]) :other)  ; Invalid - returns error
  ```

- **list-tuple** - Validates if a value is a list with specific validators for each position
  Arguments:
  - `validators` - A list of validator functions

  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected list-tuple of length [size], got [value]"

  Example:
  ```clojure
  (v/validate (v/list-tuple (list (v/keyword)
                                  (v/string)
                                  (v/number))) [:a "str" 5])  ; Valid
  (v/validate (v/list-tuple (list (v/keyword)
                                  (v/string)
                                  (v/number))) [5 :a "str"])  ; Invalid - returns error
  (v/validate (v/list-tuple (list (v/keyword)
                                  (v/string)
                                  (v/number))) [:a "str" 5 :b])  ; Invalid - returns error
  (v/validate (v/list-tuple (list (v/keyword)
                                  (v/string)
                                  (v/number))) :other)  ; Invalid - returns error
  ```

### JS Date

- **date** - Validates if a value is a valid JavaScript Date object
  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected date, got [value]"

  Example:
  ```clojure
  (v/validate (v/date) (js/Date.))  ; Valid
  (v/validate (v/date) 1743138078984)  ; Invalid - returns error
  (v/validate (v/date) :other)  ; Invalid - returns error

- **string->date** - Converts a string to a JavaScript Date object. Uses `js/Date.parse` under the hood.
  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected string-date, got [value]"

  Example:
  ```clojure
  (v/validate (v/string->date) "2025-03-28T05:04:24.923Z")  ; Valid
  (v/validate (v/string->date) "other")  ; Invalid - returns error
  (v/validate (v/string->date) :other)  ; Invalid - returns error
  ```

- **number->date** - Converts a number to a JavaScript Date object. Uses `(new js/Date)` under the hood.
  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected valid timestamp, got [value]"

  Example:
  ```clojure
  (v/validate (v/number->date) 1743138078984)  ; Valid
  (v/validate (v/number->date) -5)  ; Invalid - returns error
  (v/validate (v/number->date) :other)  ; Invalid - returns error
  ```

- **date->string** - Converts a JavaScript Date object to an ISO string
  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected date, got [value]"

  Example:
  ```clojure
  (v/validate (v/date->string) (js/Date))  ; Valid
  (v/validate (v/date->string) :other)  ; Invalid - returns error
  ```

- **date->number** - Converts a JavaScript Date object to a number
  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected date, got [value]"

  Example:
  ```clojure
  (v/validate (v/date->number) (js/Date))  ; Valid
  (v/validate (v/date->number) :other)  ; Invalid - returns error
  ```

### Combinators

- **chain** - Creates a validator that applies multiple validators in sequence
  Arguments:
  - `& validators` - Variadic list of validator functions

  Example:
  ```clojure
  (v/validate (v/chain (v/string) (v/string->number)) "5")  ; Valid
  (v/validate (v/chain (v/string) (v/string->number)) "str")  ; Invalid - returns error
  ```

- **union** - Creates a validator that tries multiple validators and succeeds if any one succeeds
  Arguments:
  - `& validators` - Variadic list of validator functions

  Example:
  ```clojure
  (v/validate (v/union (v/string) (v/number)) "5")  ; Valid
  (v/validate (v/union (v/string) (v/number)) 5)  ; Valid
  (v/validate (v/union (v/string) (v/number)) :other)  ; Invalid - returns error
  ```

- **default** - Creates a validator that provides a default value for nil inputs
  Arguments:
  - `validator` - Variadic list of validator functions
  - `default-value-or-fn` - Default value or function, is only used if value is nil

  Example:
  ```clojure
  (v/validate (v/default (v/string) "default") "str")  ; Valid
  (v/validate (v/default (v/string) "default") nil)  ; Valid
  (v/validate (v/default (v/string) (fn [] "default")) nil)  ; Valid
  (v/validate (v/default (v/string) "default") :other)  ; Invalid - returns error
  ```

- **lazy** - Creates a validator that lazily evaluates a validator function
  Arguments:
  - `make-validator-fn` - Function that returns a validator function

  Example:
  ```clojure
  (declare task)
  (def task (v/lazy
             (fn []
               (v/record {:title (v/string)
                          :tasks (v/vector task)}))))

  (v/validate task {:title "str"
                    :tasks [{:title "str2" :tasks []}])  ; Valid
  (v/validate task {:title :other
                    :tasks {:title "str2" :tasks []}])  ; Invalid - returns error
  ```


### JS Interop

Import the library like as follows:

```clojure
(ns my-namespace.core
  (:require [dev.jaide.valhalla.js :as vjs]))
```

- **array** - Validates if a value is a js-array and parses into a vector
  Arguments:
  - `validator` - A validator to apply to each array item

  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected js-array, got [value]"

  Example:
  ```clojure
  (v/validate
    (vjs/array (v/string)) #js ["str" "str1" "str2"])  ; Valid
  (v/validate
    (vjs/array (v/string)) #js ["str" "str2" :kw])  ; Invalid - returns error
  (v/validate
    (vjs/array (v/string)) #js {:a 1 :b 2 :c 3})  ; Invalid - returns error
  ```

- **object** - Validates if a value is a JavaScript object with same value type of unknown size
  Arguments:
  - `validator` - A validator function applied to each value

  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected js-object, got [value]"

  Example:
  ```clojure
  (v/validate
   (vjs/object (v/string)) #js {:a "str" :b "str2" :c "str3"})  ; Valid
  (v/validate
   (vjs/record (v/string)) #js {:a "str" :b "str2" :c :kw})  ; Invalid - returns error
  (v/validate
   (vjs/record (v/string)) #js [:a :b :c])  ; Invalid - returns error
  ```

- **record** - Validates if a value is a js-object and validates specific keys
  Arguments:
  - `validator-hash-map` - A hash-map mapping keys to validator functions

  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected js-object, got [value]"

  Example:
  ```clojure
  (v/validate
    (vjs/record {:a (v/string)
                 :b (v/number)
                 :c (v/keyword)}) #js {:a "str" :b 5 :c :kw})  ; Valid
  (v/validate
    (vjs/record {:a (v/string)
                 :b (v/number)
                 :c (v/keyword)}) #js {:a "str" :b 5 :c :kw :d 'sym})  ; Invalid - returns error
  (v/validate
    (vjs/record {:a (v/string)
                 :b (v/number)
                 :c (v/keyword)}) #js {:a "str" :b "str2" :c :kw})  ; Invalid - returns error
  (v/validate
    (vjs/record {:a (v/string)
                 :b (v/number)
                 :c (v/keyword)}) #js [:a :b :c])  ; Invalid - returns error
  ```

- **iterable->vector** - Validates if a value is a js-array and parses into a vector
  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected js-object, got [value]"

  Example:
  ```clojure
  (v/validate
    (vjs/iterable-vector) (js/Set. #js ["str" 5 :kw]))  ; Valid
  (v/validate
    (vjs/iterable-vector) (js/Set. #js ["str" "str2" "str3"]))  ; Valid
  (v/validate
    (vjs/iterable-vector) #js {:a 1 :b 2 :c 3})  ; Invalid - returns error
  ```

## Writing Custom Validators

Valhalla is flexible, it is simple to create custom validators.

### Basic Custom Validator

A validator in Valhalla is a function that takes a value and returns either the validated value or an error result. Here's a simple example of a custom email validator:

```clojure
(ns my-app.validators
  (:require [dev.jaide.valhalla.core :as v]))

(defn email-validator
  "Validates that a string is a valid email address"
  ([] (email-validator {}))
  ([{:keys [message] :as opts}]
   (let [email-regex #"^[^@]+@[^@]+\.[^@]+$"
         message (cond
                   (fn? message) message
                   (string? message) (constantly message)
                   :else
                   (fn [{:keys [value] :as context}]
                     (str "Invalid email address, got " (pr-str value)]
     (fn [{:keys [value] :as context}]
       (if (and (string? value) (re-matches email-regex value))
         (v/ok value)
         (v/error message))))))

;; Usage:
(def user-schema
  (v/record
    {:name (v/string)
     :email (email-validator {:message "Please enter a valid email"})}))
```

### Validators with Transformations

Custom validators can also transform data during validation:

```clojure
(defn trim-string
  "Validates a string and trims whitespace"
  ([] (trim-string {}))
  ([opts]
   (fn [{:keys [value]}]
     (let [result (v/string value opts)]
       (if (v/ok? result)
         (let [[_status value] result]
           (v/ok (clojure.string/trim value))
         result)))))
```


### Integration with Existing Schemas

Custom validators can be used anywhere standard validators are used:

```clojure
(def advanced-schema
  (v/record
    {:name (v/string)
     :email (email-validator)
     :bio (trim-string {:message "Bio must be at least 10 characters"})}))
```

## Credits

Valhalla is inspired by [@badrap/valita](https://github.com/badrap/valita), a TypeScript validation library.

## Testing

Run tests automatically with the following:

```bash
npx shadow-cljs watch test
```

Build the tests, and run manually to target an individual test:

```bash
npx shadow-cljs watch test --config-merge '{:autorun false}'
```

Then in another terminal run:

```bash
npx nodemon -w build/js build/js/node-tests.js --test=dev.jaide.valhalla.core-test/hash-map-test
```

### Deploying

Run the following to deploy to clojars. This will probably only work if you're me but it is a useful reminder

```bash
clj -T:build deploy
```

## Support

If you encounter any issues or would like to request a new validator, please [create an issue](https://github.com/jaidetree/valhalla/issues) on our GitHub repository. We appreciate your feedback and contributions!

## License

[GPL-3.0 License](LICENSE)

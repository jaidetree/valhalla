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
{:deps {dev.jaide/valhalla {:mvn/version "0.1.0"}}}
```

### Leiningen/Boot

```clojure
[dev.jaide/valhalla "0.1.0"]
```

## Example

Here's a quick example to get you started with Valhalla:

### Validating a record

```clojure
(ns my-app.core
  (:require [dev.jaide.valhalla.core :as v]))

;; Define a validation schema
(def user-schema
  (v/object
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

(v/parse user-schema invalid-user)
;; => {:success false, :errors [...detailed error information...]}
```

### Custom error messages

You can customize error messages for better user experience:

```clojure
(def user-schema
  (v/object
    {:name (v/string {:message "Name must be a string"})
     :age (v/number {:message "Age must be a number"})
     :email (v/string {:pattern #"^[^@]+@[^@]+\.[^@]+$"
                       :message "Please provide a valid email address"})
     :roles (v/array (v/string) {:message "Roles must be an array of strings"})}))
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

## Validators

Valhalla provides a set of validators for different data types:

### Primitive Values

- **boolean** - Validates if a value is a boolean.
  ```clojure
  (v/boolean)  ; Default options
  (v/boolean {:message "Custom error message"})  ; With custom error message
  ```

  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected boolean, got [value]"

  Example:
  ```clojure
  (v/validate (v/boolean) true)  ; Valid
  (v/validate (v/boolean) "true")  ; Invalid - returns error
  (v/validate (v/boolean {:message "Must be true or false"}) 123)  ; Invalid with custom message
  ```

  Related validators:
  - `string->boolean` - Converts string "true"/"false" to boolean values

- **keyword** - Validates if a value is a keyword.
  ```clojure
  (v/keyword)  ; Default options
  (v/keyword {:message "Custom error message"})  ; With custom error message
  ```

  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected keyword, got [value]"

  Example:
  ```clojure
  (v/validate (v/keyword) :user/admin)  ; Valid
  (v/validate (v/keyword) "not-a-keyword")  ; Invalid - returns error
  (v/validate (v/keyword {:message "Must be a keyword"}) 123)  ; Invalid with custom message
  ```

  Related validators:
  - `string->keyword` - Converts strings to keywords, validating they match the pattern for valid keywords

- **nil-value** - Validates if a value is nil.
  ```clojure
  (v/nil-value)  ; Default options
  (v/nil-value {:message "Custom error message"})  ; With custom error message
  ```

  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected nil, got [value]"

  Example:
  ```clojure
  (v/validate (v/nil-value) nil)  ; Valid
  (v/validate (v/nil-value) "something")  ; Invalid - returns error
  ```

  Related validators:
  - `nilable` - Makes another validator accept nil values

- **number** - Validates if a value is a number.
  ```clojure
  (v/number)  ; Default options
  (v/number {:message "Custom error message"})  ; With custom error message
  ```

  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected number, got [value]"

  Example:
  ```clojure
  (v/validate (v/number) 42)  ; Valid
  (v/validate (v/number) 3.14)  ; Valid
  (v/validate (v/number) "42")  ; Invalid - returns error
  ```

  Related validators:
  - `string->number` - Converts strings to numbers
  - `numeric` - Validates if a string can be parsed as a number

- **numeric** - Validates if a string contains a numeric value.
  ```clojure
  (v/numeric)  ; Default options
  (v/numeric {:message "Custom error message"})  ; With custom error message
  ```

  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected numeric string, got [value]"

  Example:
  ```clojure
  (v/validate (v/numeric) "42")  ; Valid - returns "42" (preserves string)
  (v/validate (v/numeric) "3.14")  ; Valid - returns "3.14" (preserves string)
  (v/validate (v/numeric) "not-a-number")  ; Invalid - returns error
  ```

  Related validators:
  - `string->number` - Converts strings to numbers
  - `number` - Validates if a value is a number

- **string** - Validates if a value is a string.
  ```clojure
  (v/string)  ; Default options
  (v/string {:message "Custom error message"})  ; With custom error message
  ```

  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected string, got [value]"

  Example:
  ```clojure
  (v/validate (v/string) "hello")  ; Valid
  (v/validate (v/string) 42)  ; Invalid - returns error
  ```

  Related validators:
  - `regex` - Validates if a string matches a pattern

- **symbol** - Validates if a value is a symbol.
  ```clojure
  (v/symbol)  ; Default options
  (v/symbol {:message "Custom error message"})  ; With custom error message
  ```

  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected symbol, got [value]"

  Example:
  ```clojure
  (v/validate (v/symbol) 'my-symbol)  ; Valid
  (v/validate (v/symbol) 'user/function)  ; Valid
  (v/validate (v/symbol) :keyword)  ; Invalid - returns error
  ```

  Related validators:
  - `string->symbol` - Converts strings to symbols

### Coercion

- **string->boolean** - Parses a string into a boolean
  ```clojure
  (v/string->boolean)  ; Default options
  (v/string->boolean {:message "Custom error message"})  ; With custom error message
  ```

  Options:
  - `:message` - Custom error message function or string. If not provided, defaults to "Expected boolean-string, got [value]"

  Example:
  ```clojure
  (v/validate (v/string->boolean) "true")  ; Valid
  (v/validate (v/string->boolean) "false")  ; Valid
  (v/validate (v/string->boolean) :keyword)  ; Invalid - returns error
  ```

  Related validators:
  - `boolean` - Validates boolean values

- string->keyword
- string->number
- string->symbol

### Advanced

- assert
- enum
- instance
- literal
- regex
- uuid

### Optionality

- nilable

### Collections

- hash-map
- list
- record
- set
- vector

### Tuples

- vector-tuple
- list-tuple

### JS Date

- date
- string->date
- number->date
- date->string
- date->number

### Combinators

- chain
- union
- default
- lazy

### JS Interop

- object
- array
- iterable->array

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
  (v/object
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
  (v/object
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

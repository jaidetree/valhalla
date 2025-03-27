# Valhalla

A ClojureScript validation library for parsing and validating data with an emphasis on intuitive interfaces and JavaScript interoperability.

## Introduction

Valhalla is a ClojureScript validation library designed to make data validation and parsing straightforward and developer-friendly. It provides a simple yet powerful API for defining validation schemas and generating user-friendly error messages.

### Why not just use spec?

While [clojure.spec](https://clojure.org/guides/spec) is a powerful tool for data validation, Valhalla offers several advantages:

- **Intuitive interface**: Valhalla provides a simpler, more direct API that's easier to learn and use
- **Improved JS interop**: First-class support for JavaScript data structures and seamless conversion between JS and ClojureScript
- **User-friendly error messages**: Detailed, customizable error messages that help users understand what went wrong

## Installation

Add Valhalla to your project using your preferred dependency management tool:

### deps.edn

```clojure
{:deps {dev.jaidetree/valhalla {:mvn/version "0.1.0"}}}
```

### Leiningen/Boot

```clojure
[dev.jaidetree/valhalla "0.1.0"]
```

## Example

Here's a quick example to get you started with Valhalla:

### Validating a record

```clojure
(ns my-app.core
  (:require [jaidetree.valhalla.core :as v]))

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

(v/parse user-schema valid-user)
;; => {:name "Jane Doe", :age 30, :email "jane@example.com", :roles ["admin" "user"]}

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
  (:require [jaidetree.valhalla.js :as jsv]))

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

(jsv/parse js-user-schema js-user)
;; => {:name "Alice", :age 25, :preferences {:theme "dark", :notifications true}}
```

## Validators

Valhalla provides a rich set of validators for different data types:

- `string`: Validates strings with optional constraints like min/max length, pattern matching
- `number`: Validates numbers with optional constraints like min/max values
- `boolean`: Validates boolean values
- `array`: Validates arrays/vectors with optional item validation
- `object`: Validates objects/maps with specified key-value validations
- `optional`: Makes a field optional
- `nullable`: Allows a field to be null
- `enum`: Validates against a set of allowed values
- `any`: Accepts any value
- `union`: Validates against multiple possible schemas

## Credits

Valhalla is inspired by [@badrap/valita](https://github.com/badrap/valita), a TypeScript validation library.

## Support

If you encounter any issues or would like to request a new validator, please [create an issue](https://github.com/jaidetree/valhalla/issues) on our GitHub repository. We appreciate your feedback and contributions!

## License

[MIT License](LICENSE)

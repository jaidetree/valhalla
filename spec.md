# ClojureScript Validation Library - Developer Implementation Specification

## 1. Project Overview

### 1.1 Purpose
A functional validation library for ClojureScript inspired by @badrap/valita that provides an intuitive API for defining and validating ClojureScript types with helpful error messages. The library focuses on runtime validation with a composable, functional approach while maintaining zero external dependencies.

### 1.2 Key Requirements
- Provide an intuitive, functional API for validation
- Support all native ClojureScript types
- Generate clear, helpful error messages
- Enable composition of validators
- Support nested and recursive data structures
- Facilitate JavaScript interoperability
- Maintain no external dependencies

### 1.3 Out of Scope
- Asynchronous validation
- Static/compile-time type checking
- Schema evolution/migration (for initial version)

## 2. Architecture

### 2.1 Core Design Principles
- **Functional approach**: All validators are functions that return functions
- **Composability**: Validators can be combined to create complex validation rules
- **Pure functions**: No side effects or global state
- **Immutability**: Input data is never mutated

### 2.2 Module Structure
```
src/
  validatron/                # Main namespace
    core.clj                 # Core validation functions
    types.clj                # Basic type validators
    collections.clj          # Collection validators
    composites.clj           # Composite validators
    interop.clj              # JavaScript interop
    errors.clj               # Error handling utilities
    i18n.clj                 # Internationalization
test/
  validatron/
    core_test.clj            # Unit tests for core functions
    types_test.clj           # Tests for basic type validators
    collections_test.clj     # Tests for collection validators
    composites_test.clj      # Tests for composite validators
    interop_test.clj         # Tests for JavaScript interop
    integration_test.clj     # Integration tests
```

## 3. API Specification

### 3.1 Core Result Types
```clojure
;; Success result
{:result :v/ok
 :data <validated-data>}

;; Failure result
{:result :v/fail
 :errors [[[:path :to :field] "Error message"]]
 :data <input-data>}
```

### 3.2 Context Object
All validator functions receive a context object with:
```clojure
{:value <current-value>      ;; The value being validated
 :path [<path-elements>]     ;; Current path in the data structure
 :data <complete-data>}      ;; Complete data structure being validated
```

### 3.3 Core Utility Functions
```clojure
(v/ok value)                 ;; Create success result
(v/error "message")          ;; Create error result
(v/format-error key args)    ;; Format internationalized error
```

### 3.4 Basic Type Validators

#### Implementation Pattern
```clojure
(defn string 
  ([] (string {}))
  ([opts]
   (fn [context]
     (let [{:keys [value path]} context
           message (or (:message opts)
                       (fn [{:keys [value path]}]
                         (str "Field " (clojure.string/join "." path)
                              " does not match type string got "
                              (type value))))]
       (if (string? value)
         {:result :v/ok
          :data value}
         {:result :v/fail
          :errors [[path (if (fn? message)
                           (message context)
                           message)]]
          :data value})))))
```

#### Basic Types API
```clojure
(v/string)        ;; Validates strings
(v/int)           ;; Validates integers
(v/number)        ;; Validates numbers
(v/boolean)       ;; Validates booleans
(v/keyword)       ;; Validates keywords
(v/symbol)        ;; Validates symbols
(v/uuid)          ;; Validates UUIDs
(v/nil)           ;; Validates nil values
(v/any)           ;; Accepts any value
```

### 3.5 Collection Validators

#### Implementation Pattern
```clojure
(defn vector-of
  ([item-validator] (vector-of item-validator {}))
  ([item-validator opts]
   (fn [{:keys [value path] :as context}]
     (if-not (vector? value)
       (v/error (format "Expected vector, got %s" (type value)))
       (let [item-results (map-indexed
                            (fn [idx item]
                              (let [item-context (assoc context
                                                  :value item
                                                  :path (conj path idx))]
                                ((if (fn? item-validator)
                                   item-validator
                                   (constantly item-validator))
                                 item-context)))
                            value)
             errors (mapcat
                      (fn [result]
                        (when (= (:result result) :v/fail)
                          (:errors result)))
                      item-results)]
         (if (seq errors)
           {:result :v/fail
            :errors errors
            :data value}
           {:result :v/ok
            :data (mapv :data item-results)}))))))
```

#### Collections API
```clojure
(v/vector (v/string))              ;; Vector of strings
(v/vector-tuple [(v/int) (v/string) (v/boolean)])  ;; Vector with specific types at positions
(v/list (v/number))                ;; List of numbers
(v/set (v/keyword))                ;; Set of keywords
(v/hash-map {:id (v/string)        ;; Hash map with specified shape
             :age (v/int)})
```

### 3.6 Special Type Validators

#### Implementation Pattern
```clojure
(defn nilable
  [validator]
  (fn [{:keys [value] :as context}]
    (if (nil? value)
      {:result :v/ok
       :data nil}
      (validator context))))

(defn optional
  [validator]
  (fn [{:keys [value] :as context}]
    (if (or (nil? value) (= value :not-found))
      {:result :v/ok
       :data nil}
      (validator context))))
```

#### Special Types API
```clojure
(v/nilable (v/string))             ;; String or nil
(v/optional (v/int))               ;; Field can be missing or be an integer
(v/enum [:foo :bar :baz])          ;; Must be one of the specified values
(v/literal "exact-value")          ;; Must match exactly
(v/union (v/string) (v/int))       ;; Either string or integer
(v/lazy #(recursive-schema))       ;; For recursive schemas
(v/ref other-schema)               ;; Reference to another schema
```

### 3.7 Composition Functions

#### Implementation Pattern
```clojure
(defn chain
  [& validators]
  (fn [{:keys [value] :as context}]
    (reduce
      (fn [result validator]
        (if (= (:result result) :v/fail)
          result
          (validator (assoc context :value (:data result)))))
      {:result :v/ok, :data value}
      validators)))

(defn custom
  [validator-fn]
  (fn [context]
    (validator-fn context)))
```

#### Composition API
```clojure
(v/chain (v/string)                ;; Chain validators (logical AND)
         (fn [{:keys [value path data]}]
           (if (seq value)
             (v/ok value)
             (v/error "String cannot be empty"))))

(v/union (v/string) (v/int))       ;; Value must satisfy at least one validator (logical OR)

(v/when #(= (:type %) :business)   ;; Conditional validation
        (v/string)
        (v/any))

(v/custom                          ;; Custom validator
  (fn [{:keys [value path data]}]
    (if (valid? value)
      (v/ok value)
      (v/error "Custom error message"))))

(v/default (v/string) "default")   ;; Provide default value for missing fields
```

### 3.8 JavaScript Interop

#### Implementation Pattern
```clojure
(defn js-object
  ([] (js-object {}))
  ([opts]
   (fn [{:keys [value path] :as context}]
     (if (and (not (nil? value))
              (= (type value) js/Object)
              (not (js/Array.isArray value)))
       {:result :v/ok
        :data (js->clj value :keywordize-keys true)}
       {:result :v/fail
        :errors [[path (str "Expected JavaScript object, got " (type value))]]
        :data value}))))
```

#### JS Interop API
```clojure
(v/js-object)                      ;; Validates JavaScript objects
(v/js-array)                       ;; Validates JavaScript arrays
(v/js-array-like)                  ;; Validates array-like objects (NodeList, etc.)
(v/js-class js/Date)               ;; Validates instance of specific JS class
```

### 3.9 Environment-Specific Validation

```clojure
(defn with-env
  [validators env-config]
  (fn [value]
    (if ((:environments env-config) (:current-env env-config))
      ((apply v/chain validators) value)
      (v/ok value))))
```

## 4. Error Handling

### 4.1 Error Message Format
```clojure
[[:path :to :field] "Error message"]
```

### 4.2 Custom Error Messages
```clojure
;; String message
(v/string {:message "Username must be a string"})

;; Function-based message
(v/string {:message (fn [{:keys [value path]}]
                      (str "Invalid value at " (clojure.string/join "." path)
                           ": expected string, got " (type value)))})
```

### 4.3 Internationalization
```clojure
(def translations
  {:en {:string/type "Field %s must be a string, got %s"
        :number/type "Field %s must be a number, got %s"}
   :es {:string/type "El campo %s debe ser una cadena, obtuvo %s"
        :number/type "El campo %s debe ser un número, obtuvo %s"}})

(defn format-error
  ([key args] (format-error key args :en))
  ([key args locale]
   (let [template (get-in translations [locale key] (get-in translations [:en key]))
         format-fn (or (get formatters locale) default-formatter)]
     (format-fn template args))))
```

## 5. Testing Plan

### 5.1 Unit Testing

#### Basic Type Validators Tests
- Test each validator with valid inputs
- Test each validator with invalid inputs
- Test custom error messages
- Test function-based error messages

#### Collection Validators Tests
- Test empty collections
- Test collections with valid items
- Test collections with invalid items
- Test nested collections

#### Composite Validators Tests
- Test chained validators
- Test conditional validators
- Test union validators
- Test custom validators

#### JavaScript Interop Tests
- Test validation of JavaScript objects
- Test validation of JavaScript arrays
- Test validation of array-like objects
- Test validation of class instances

### 5.2 Property-Based Testing
- Test validators with random inputs using test.check
- Test compositions of validators with random inputs
- Test recursive structures with varying depths

### 5.3 Integration Testing
- Test complex validation scenarios
- Test realistic data structures
- Test internationalization
- Test environment-specific validation

### 5.4 Performance Testing
- Benchmark validation of large data structures
- Compare performance with and without optimizations
- Test memory usage

## 6. Implementation Plan

### 6.1 Phase 1: Core Functionality
- Implement basic result types
- Implement core utility functions
- Implement basic type validators
- Write unit tests for core components

### 6.2 Phase 2: Collection Validators
- Implement vector validators
- Implement list validators
- Implement set validators
- Implement hash-map validators
- Write unit tests for collection validators

### 6.3 Phase 3: Special Type Validators
- Implement nilable validator
- Implement optional validator
- Implement enum validator
- Implement literal validator
- Implement union validator
- Implement lazy and ref validators
- Write unit tests for special type validators

### 6.4 Phase 4: Composition Functions
- Implement chain function
- Implement union function
- Implement when function
- Implement custom function
- Implement default function
- Write unit tests for composition functions

### 6.5 Phase 5: JavaScript Interop
- Implement js-object validator
- Implement js-array validator
- Implement js-array-like validator
- Implement js-class validator
- Write unit tests for JavaScript interop

### 6.6 Phase 6: Error Handling and Internationalization
- Implement error formatting
- Implement internationalization
- Write unit tests for error handling

### 6.7 Phase 7: Performance Optimization
- Implement lazy validation
- Implement early termination
- Implement memoization
- Implement schema compilation
- Write performance tests

## 7. Example Usage

### 7.1 Basic Validation
```clojure
(def user-schema
  (v/hash-map
    {:id (v/string)
     :age (v/int)
     :email (v/string)}))

(user-schema {:id "123", :age 25, :email "user@example.com"})
;; => {:result :v/ok, :data {:id "123", :age 25, :email "user@example.com"}}

(user-schema {:id 123, :age "25", :email "user@example.com"})
;; => {:result :v/fail, 
;;     :errors [[[:id] "Field id does not match type string got number"]
;;              [[:age] "Field age does not match type int got string"]], 
;;     :data {:id 123, :age "25", :email "user@example.com"}}
```

### 7.2 Complex Validation
```clojure
;; Email validator
(def email-validator
  (v/chain 
    (v/string)
    (fn [{:keys [value]}]
      (if (re-matches #"^[^@]+@[^@]+\.[^@]+$" value)
        (v/ok value)
        (v/error "Invalid email format")))))

;; User with optional fields and conditional validation
(def user-schema
  (v/hash-map
    {:id (v/string)
     :type (v/enum [:personal :business])
     :email email-validator
     :tax-id (v/custom 
               (fn [{:keys [value data]}]
                 (if (= (:type data) :business)
                   (if (string? value)
                     (v/ok value)
                     (v/error "Tax ID must be a string for business accounts"))
                   (v/ok value))))
     :profile (v/nilable 
                (v/hash-map
                  {:name (v/string)
                   :age (v/int)}))
     :settings (v/default 
                 (v/hash-map
                   {:notifications (v/boolean)})
                 {:notifications true})}))
```

### 7.3 Recursive Data Structures
```clojure
(declare category-schema)

(def category-schema
  (v/lazy
    #(v/hash-map
       {:id (v/string)
        :name (v/string)
        :subcategories (v/vector (v/ref category-schema))})))

(category-schema
  {:id "cat1"
   :name "Main Category"
   :subcategories [{:id "sub1"
                    :name "Subcategory"
                    :subcategories []}]})
```

## 8. Performance Considerations

### 8.1 Lazy Validation
Validators should only validate as deep as needed and stop processing at the first error unless explicitly configured to collect all errors.

### 8.2 Memoization
Validator results for repeated structures should be cached to avoid redundant validation.

### 8.3 Schema Compilation
Schemas should be compiled into optimized validator functions at definition time rather than interpreting the schema at validation time.

### 8.4 Incremental Validation
When revalidating a previously validated structure, only changed parts should be revalidated.

## 9. Project Management

### 9.1 Version Management
- Use date-based versioning (e.g., YYYY.MM.DD)
- Breaking changes should be moved to new namespaces or packages

### 9.2 Documentation
- Include docstrings for all public functions
- Provide examples for each validator
- Document error messages and customization options

### 9.3 Testing
- Aim for 100% test coverage of core functionality
- Include both unit tests and property-based tests
- Include realistic usage examples in tests

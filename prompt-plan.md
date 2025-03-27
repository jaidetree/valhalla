# ClojureScript Validation Library - Implementation Blueprint

## Overall Development Strategy

The implementation will follow these key principles:
1. **Test-driven development** - Write tests first, then implement the code
2. **Incremental progress** - Build functionality in small, testable chunks
3. **Composability** - Ensure each component works well with others
4. **Strong foundations** - Get the core utilities right before building more complex features

## Implementation Phases

Here's the detailed blueprint, broken down into phases and steps:

### Phase 1: Core Foundation

This phase establishes the foundational elements of the library:

1. Project setup and basic utilities
2. Core result types (success/failure)
3. Context object implementation
4. Basic validation infrastructure

### Phase 2: Basic Type Validators

This phase implements the fundamental type validators:

1. String validator
2. Number/Integer validators
3. Boolean, Keyword, Symbol validators
4. UUID, nil, and "any" validators

### Phase 3: Collection Validators

This phase adds support for validating collections:

1. Vector validators (basic, vector-of, tuple)
2. List and Set validators
3. Hash-map validators
4. Nested collection support

### Phase 4: Special Type Validators & Composition

This phase implements special validators and composition functions:

1. Nilable and Optional validators
2. Enum and Literal validators
3. Chain, Union, and When functions
4. Custom validators and defaults

### Phase 5: Advanced Features

This phase adds more advanced functionality:

1. Recursive schemas with lazy and ref validators
2. JavaScript interop validators
3. Error handling and i18n
4. Performance optimizations

## Implementation Prompts

The following prompts guide a code-generation LLM through each step of the implementation process.

---

### Phase 1: Core Foundation Prompts

#### Prompt 1.1: Project Setup

# ClojureScript Validation Library - Project Setup

We're starting development on a new ClojureScript validation library called "valhalla". The library is a functional validation library with zero external dependencies, inspired by @badrap/valita.

## Task
Create the initial project structure with:
1. A deps.edn file with necessary ClojureScript and testing dependencies
2. The basic directory structure
3. A minimal namespace for the core functionality
4. A minimal test namespace

## Expected Directory Structure:

```
src/
  jaidetree/
    valhalla/
      core.cljs         # Core validation functions
test/
  jaidetree/
    valhalla/
      core_test.cljs    # Tests for core functionality
```

## Requirements:
- Use recent stable versions of dependencies
- Include cljs.test for testing
- Ensure tests can be run easily using standard ClojureScript test runners
- Keep the implementation simple but extensible

Write the code for these files with minimal implementations to verify the setup works.

#### Prompt 1.2: Core Result Types

# ClojureScript Validation Library - Core Result Types

Now let's implement the core result types for our validation library. In our library, validation functions will return either a success or failure result.

## Task
Implement the core result types and functions in the `dev.jaide.valhalla.core` namespace:

1. Success result: `{:result :v/ok, :data <validated-data>}`
2. Failure result: `{:result :v/fail, :errors [[[:path] "Error message"]], :data <input-data>}`
3. Implement `ok` function that wraps a value in a success result
4. Implement `error` function that creates a failure result with a message

## Test Cases
Create tests in `dev.jaide.valhalla.core-test` namespace that verify:
1. `(ok value)` returns `{:result :v/ok, :data value}`
2. `(error "message")` returns `{:result :v/fail, :errors [[[] "message"]], :data nil}`
3. `(error "message" value)` returns `{:result :v/fail, :errors [[[] "message"]], :data value}`
4. `(error [[:path] "message"] value)` returns `{:result :v/fail, :errors [[[:path] "message"]], :data value}`

## Requirements
- Functions should be pure (no side effects)
- Implementation should be simple and focused
- Add docstrings to all public functions
- Ensure tests pass

#### Prompt 1.3: Context Object

# ClojureScript Validation Library - Context Object

Let's implement the context object that will be passed to all validators. The context object contains information about the current validation process.

## Task
Extend the `dev.jaide.valhalla.core` namespace to implement:

1. Context object structure: `{:value <current-value>, :path [<path-elements>], :data <complete-data>}`
2. Function `make-context` to create a new context object
3. Function `update-context-path` to add an element to the path
4. Function `update-context-value` to update the value in the context

## Test Cases
Add tests that verify:
1. `(make-context value)` returns `{:value value, :path [], :data value}`
2. `(make-context value data)` returns `{:value value, :path [], :data data}`
3. `(update-context-path context :key)` adds `:key` to the path
4. `(update-context-path context 0)` adds `0` to the path
5. `(update-context-value context new-value)` updates the `:value` key

## Requirements
- Functions should be pure
- Context objects should be immutable
- Maintain the existing `ok` and `error` functions
- Ensure all tests pass

#### Prompt 1.4: Validator Function Pattern

# ClojureScript Validation Library - Validator Function Pattern

Now let's define the pattern for validator functions. Each validator will be a function that returns another function that takes a context object and returns a result.

## Task
Extend the `dev.jaide.valhalla.core` namespace to implement:

1. Function `make-validator` that creates a validator function from a predicate function and error message
2. Function `validate` that applies a validator to a value
3. Function `validator?` that checks if a function is a validator

## Example Validator Pattern
```clojure
(defn string
  ([] (string {}))
  ([opts]
   (fn [context]
     (let [{:keys [value path]} context
           message (or (:message opts)
                       (fn [{:keys [value path]}]
                         (str "Expected string, got " (type value))))]
       (if (string? value)
         (ok value)
         (error (if (fn? message)
                  (message context)
                  message)
                value))))))
```

## Test Cases
Add tests that verify:
1. `(make-validator string? "Not a string")` returns a function
2. `((make-validator string? "Not a string") (make-context "test"))` returns a success result
3. `((make-validator string? "Not a string") (make-context 123))` returns a failure result
4. `(validate (make-validator string? "Not a string") "test")` returns a success result
5. `(validate (make-validator string? "Not a string") 123)` returns a failure result
6. `(validator? (make-validator string? "Not a string"))` returns true
7. `(validator? identity)` returns false

## Requirements
- Validators should follow the functional pattern shown in the example
- Error messages should be either strings or functions that take a context
- `validate` should handle both plain values and contexts
- All tests should pass

### Phase 2: Basic Type Validators Prompts

#### Prompt 2.1: String Validator

# ClojureScript Validation Library - String Validator

Let's implement our first real validator: the string validator. This will serve as a template for other basic type validators.

## Task
Create a new namespace `dev.jaide.valhalla.types` and implement:

1. String validator that checks if a value is a string
2. Support for custom error messages
3. Support for function-based error messages
4. Integration with the core validation functions

## Implementation Guidelines
The string validator should:
- Take an optional options map with a `:message` key
- Return a validator function that follows our established pattern
- Use the core `ok` and `error` functions

## Test Cases
Create a `dev.jaide.valhalla.types-test` namespace with tests that verify:
1. `(string)` creates a validator that accepts strings
2. `(validate (string) "test")` returns a success result
3. `(validate (string) 123)` returns a failure result
4. `(validate (string {:message "Custom error"}) 123)` returns a failure with "Custom error"
5. `(validate (string {:message (fn [ctx] (str "Got: " (:value ctx)))}) 123)` returns a failure with "Got: 123"
6. Error results should include proper path information

## Requirements
- Follow the functional pattern established in the core namespace
- Add docstrings to all public functions
- Ensure tests pass

#### Prompt 2.2: Number and Integer Validators

# ClojureScript Validation Library - Number and Integer Validators

Let's implement validators for numbers and integers, building on our string validator pattern.

## Task
Extend the `dev.jaide.valhalla.types` namespace to implement:

1. Number validator that checks if a value is a number
2. Integer validator that checks if a value is an integer
3. Support for the same options as the string validator

## Implementation Guidelines
Use the pattern established by the string validator:
- Take an optional options map
- Support custom error messages
- Support function-based error messages

## Test Cases
Add tests to `dev.jaide.valhalla.types-test` that verify:
1. `(number)` creates a validator that accepts numbers
2. `(validate (number) 123)` returns a success result
3. `(validate (number) "test")` returns a failure result
4. `(validate (number) 123.45)` returns a success result
5. `(integer)` creates a validator that accepts integers
6. `(validate (integer) 123)` returns a success result
7. `(validate (integer) 123.45)` returns a failure result
8. Custom error messages work for both validators

## Requirements
- Follow the same pattern as the string validator
- Ensure proper error messages for each validator type
- Maintain consistency with the established API
- Ensure all tests pass

#### Prompt 2.3: Boolean, Keyword, and Symbol Validators

# ClojureScript Validation Library - Boolean, Keyword, and Symbol Validators

Let's continue implementing our basic type validators by adding support for booleans, keywords, and symbols.

## Task
Extend the `dev.jaide.valhalla.types` namespace to implement:

1. Boolean validator that checks if a value is a boolean
2. Keyword validator that checks if a value is a keyword
3. Symbol validator that checks if a value is a symbol
4. Maintain consistent API with existing validators

## Implementation Guidelines
Follow the same pattern as previous validators:
- Each validator should take an optional options map
- Support custom error messages
- Support function-based error messages

## Test Cases
Add tests to `dev.jaide.valhalla.types-test` that verify:
1. `(boolean)` creates a validator that accepts boolean values
2. `(validate (boolean) true)` returns a success result
3. `(validate (boolean) false)` returns a success result
4. `(validate (boolean) "true")` returns a failure result
5. `(keyword)` creates a validator that accepts keywords
6. `(validate (keyword) :test)` returns a success result
7. `(validate (keyword) "test")` returns a failure result
8. `(symbol)` creates a validator that accepts symbols
9. `(validate (symbol) 'test)` returns a success result
10. `(validate (symbol) "test")` returns a failure result
11. Custom error messages work for all validators

## Requirements
- Maintain consistency with existing validators
- Ensure clear and descriptive error messages
- Follow the established functional pattern
- Add docstrings to all public functions

#### Prompt 2.4: UUID, Nil, and Any Validators

# ClojureScript Validation Library - UUID, Nil, and Any Validators

Let's complete our set of basic type validators by implementing validators for UUIDs, nil values, and a special "any" validator that accepts any value.

## Task
Extend the `dev.jaide.valhalla.types` namespace to implement:

1. UUID validator that checks if a value is a valid UUID
2. Nil validator that checks if a value is nil
3. Any validator that accepts any value
4. Maintain consistent API with existing validators

## Implementation Guidelines
Follow the same pattern as previous validators:
- Each validator should take an optional options map
- Support custom error messages
- Support function-based error messages
- The "any" validator should always return a success result

## Test Cases
Add tests to `dev.jaide.valhalla.types-test` that verify:
1. `(uuid)` creates a validator that accepts UUID objects
2. `(validate (uuid) (uuid/random-uuid))` returns a success result
3. `(validate (uuid) "not-a-uuid")` returns a failure result
4. `(nil)` creates a validator that accepts only nil
5. `(validate (nil) nil)` returns a success result
6. `(validate (nil) "not-nil")` returns a failure result
7. `(any)` creates a validator that accepts any value
8. `(validate (any) "anything")` returns a success result
9. `(validate (any) nil)` returns a success result
10. `(validate (any) 123)` returns a success result
11. Custom error messages work for all validators

## Requirements
- Maintain consistency with existing validators
- Ensure proper error messages for each validator type
- Add docstrings to all public functions
- Ensure all tests pass

### Phase 3: Collection Validators Prompts

#### Prompt 3.1: Basic Vector Validator

# ClojureScript Validation Library - Basic Vector Validator

Now let's start implementing collection validators, beginning with a basic vector validator.

## Task
Create a new namespace `dev.jaide.valhalla.collections` and implement:

1. Basic vector validator that checks if a value is a vector
2. Support for custom error messages
3. Integration with existing core validation functions

## Implementation Guidelines
The vector validator should:
- Take an optional options map with a `:message` key
- Return a validator function that follows our established pattern
- Use the core `ok` and `error` functions

## Test Cases
Create a `dev.jaide.valhalla.collections-test` namespace with tests that verify:
1. `(vector)` creates a validator that accepts vectors
2. `(validate (vector) [])` returns a success result
3. `(validate (vector) [1 2 3])` returns a success result
4. `(validate (vector) "not-a-vector")` returns a failure result
5. `(validate (vector) '(1 2 3))` returns a failure result (lists are not vectors)
6. `(validate (vector {:message "Custom error"}) "not-a-vector")` returns a failure with "Custom error"
7. Error results should include proper path information

## Requirements
- Follow the functional pattern established in previous validators
- Add docstrings to public functions
- Ensure tests pass

#### Prompt 3.2: Vector-of Validator

# ClojureScript Validation Library - Vector-of Validator

Let's implement a more advanced vector validator that validates each item in a vector against another validator.

## Task
Extend the `dev.jaide.valhalla.collections` namespace to implement:

1. Vector-of validator that applies a validator to each item in a vector
2. Proper path tracking for errors in nested items
3. Support for custom error messages
4. Collection of all errors from all items

## Implementation Guidelines
The vector-of validator should:
- Take an item validator and an optional options map
- Check if the value is a vector first, then validate each item
- Collect errors from all invalid items
- Update the context path for each item using the index
- Return a vector with all validated items on success

## Test Cases
Add tests to `dev.jaide.valhalla.collections-test` that verify:
1. `(vector-of (types/string))` creates a validator for vectors of strings
2. `(validate (vector-of (types/string)) ["a" "b" "c"])` returns a success result
3. `(validate (vector-of (types/string)) ["a" 1 "c"])` returns a failure result
4. The failure result contains an error for index 1 (the number)
5. `(validate (vector-of (types/string)) "not-a-vector")` returns a failure about not being a vector
6. `(validate (vector-of (types/number)) [1 2 3])` returns a success result
7. Error paths should include the vector indices (e.g., path [0], [1], etc.)
8. Custom error messages work for the vector check

## Requirements
- Handle empty vectors correctly
- Validate all items even if some are invalid
- Follow established functional patterns
- Maintain consistent API with other validators
- Add docstrings to public functions

#### Prompt 3.3: Vector-tuple Validator

# ClojureScript Validation Library - Vector-tuple Validator

Let's implement a vector-tuple validator that applies different validators to specific positions in a vector.

## Task
Extend the `dev.jaide.valhalla.collections` namespace to implement:

1. Vector-tuple validator that takes a sequence of validators for specific positions
2. Check for vector length matching the number of validators
3. Proper path tracking for errors in specific positions
4. Support for custom error messages

## Implementation Guidelines
The vector-tuple validator should:
- Take a sequence of validators and an optional options map
- Check if the value is a vector first, then check length
- Apply each validator to the corresponding position in the vector
- Collect errors from all invalid positions
- Update the context path for each item using the index
- Return a vector with all validated items on success

## Test Cases
Add tests to `dev.jaide.valhalla.collections-test` that verify:
1. `(vector-tuple [(types/string) (types/number) (types/boolean)])` creates a tuple validator
2. `(validate (vector-tuple [(types/string) (types/number) (types/boolean)]) ["test" 123 true])` succeeds
3. `(validate (vector-tuple [(types/string) (types/number)]) ["test" "not-a-number"])` fails for position 1
4. `(validate (vector-tuple [(types/string) (types/number)]) ["test" 123 "extra"])` fails for length
5. `(validate (vector-tuple [(types/string) (types/number)]) ["test"])` fails for length
6. `(validate (vector-tuple [(types/string) (types/number)]) "not-a-vector")` fails for not being a vector
7. Error paths should include the vector indices (e.g., path [0], [1], etc.)
8. Custom error messages work for the vector check and length check

## Requirements
- Validate the vector length matches the number of validators
- Follow established functional patterns
- Add docstrings to public functions
- Ensure tests pass

#### Prompt 3.4: List and Set Validators

# ClojureScript Validation Library - List and Set Validators

Let's implement validators for lists and sets, following the same patterns as our vector validators.

## Task
Extend the `dev.jaide.valhalla.collections` namespace to implement:

1. Basic list validator that checks if a value is a list
2. List-of validator that applies a validator to each item in a list
3. Basic set validator that checks if a value is a set
4. Set-of validator that applies a validator to each item in a set
5. Support for custom error messages

## Implementation Guidelines
These validators should:
- Follow the same patterns as the vector validators
- Take appropriate validators for the "of" versions
- Collect errors from all invalid items
- Update the context path for each item using appropriate indexing
- Return validated collections on success

## Test Cases
Add tests to `dev.jaide.valhalla.collections-test` that verify:
1. `(list)` creates a validator that accepts lists
2. `(validate (list) '(1 2 3))` returns a success result
3. `(validate (list) [1 2 3])` returns a failure result (vectors are not lists)
4. `(list-of (types/number))` creates a validator for lists of numbers
5. `(validate (list-of (types/number)) '(1 2 3))` returns a success result
6. `(validate (list-of (types/number)) '(1 "two" 3))` returns a failure result
7. `(set)` creates a validator that accepts sets
8. `(validate (set) #{1 2 3})` returns a success result
9. `(validate (set) [1 2 3])` returns a failure result
10. `(set-of (types/keyword))` creates a validator for sets of keywords
11. `(validate (set-of (types/keyword)) #{:a :b :c})` returns a success result
12. `(validate (set-of (types/keyword)) #{:a "b" :c})` returns a failure result
13. Custom error messages work for all validators

## Requirements
- Maintain consistency with vector validators
- Handle empty collections correctly
- Follow established functional patterns
- Add docstrings to public functions
- Ensure tests pass

#### Prompt 3.5: Hash-map Validator

# ClojureScript Validation Library - Hash-map Validator

Let's implement validators for hash-maps, which are a key part of our validation library.

## Task
Extend the `dev.jaide.valhalla.collections` namespace to implement:

1. Basic hash-map validator that checks if a value is a hash-map
2. Hash-map validator that validates specific keys against specific validators
3. Proper path tracking for errors in nested keys
4. Support for custom error messages

## Implementation Guidelines
The hash-map validator should:
- Take a map of keys to validators and an optional options map
- Check if the value is a map first
- Apply each validator to the corresponding key in the map
- Collect errors from all invalid values
- Update the context path for each key
- Return a map with all validated values on success

## Test Cases
Add tests to `dev.jaide.valhalla.collections-test` that verify:
1. `(hash-map)` creates a validator that accepts hash-maps
2. `(validate (hash-map) {})` returns a success result
3. `(validate (hash-map) "not-a-map")` returns a failure result
4. `(hash-map {:name (types/string) :age (types/number)})` creates a validator for a specific map shape
5. `(validate (hash-map {:name (types/string) :age (types/number)}) {:name "John" :age 30})` succeeds
6. `(validate (hash-map {:name (types/string) :age (types/number)}) {:name "John" :age "thirty"})` fails
7. `(validate (hash-map {:name (types/string) :age (types/number)}) {:name "John"})` fails for missing key
8. `(validate (hash-map {:name (types/string) :age (types/number)}) {:name "John" :age 30 :extra "value"})` fails for extra key
9. Error paths should include the map keys (e.g., path [:name], [:age], etc.)
10. Custom error messages work for the map check

## Requirements
- Allow for nested validation (validators can validate nested structures)
- Follow established functional patterns
- Add docstrings to public functions
- Ensure tests pass

### Phase 4: Special Type Validators & Composition Prompts

#### Prompt 4.1: Nilable and Optional Validators

# ClojureScript Validation Library - Nilable and Optional Validators

Now let's implement two special type validators: nilable and optional. These are common validators that modify the behavior of other validators.

## Task
Create a new namespace `dev.jaide.valhalla.composites` and implement:

1. Nilable validator that allows a value to be nil or pass another validator
2. Optional validator that allows a value to be missing or pass another validator
3. Integration with existing validators

## Implementation Guidelines
These validators should:
- Take another validator as input
- For nilable: return success if the value is nil, otherwise apply the other validator
- For optional: return success if the value is missing or nil, otherwise apply the other validator
- Wrap the other validator's result appropriately

## Test Cases
Create a `dev.jaide.valhalla.composites-test` namespace with tests that verify:
1. `(nilable (types/string))` creates a validator that accepts strings or nil
2. `(validate (nilable (types/string)) "test")` returns a success result
3. `(validate (nilable (types/string)) nil)` returns a success result
4. `(validate (nilable (types/string)) 123)` returns a failure result
5. `(optional (types/number))` creates a validator that accepts numbers or missing values
6. `(validate (optional (types/number)) 123)` returns a success result
7. `(validate (optional (types/number)) nil)` returns a success result
8. `(validate (optional (types/number)) :not-found)` returns a success result
9. `(validate (optional (types/number)) "not-a-number")` returns a failure result
10. The nilable and optional validators preserve the path information

## Requirements
- Follow established functional patterns
- Handle nil and missing values correctly
- Integrate with existing validators
- Add docstrings to public functions
- Ensure tests pass

#### Prompt 4.2: Enum and Literal Validators

# ClojureScript Validation Library - Enum and Literal Validators

Let's implement enum and literal validators to validate against specific values or sets of values.

## Task
Extend the `dev.jaide.valhalla.composites` namespace to implement:

1. Enum validator that checks if a value is one of a set of specified values
2. Literal validator that checks if a value equals a specific value
3. Support for custom error messages

## Implementation Guidelines
These validators should:
- Enum validator takes a collection of valid values and an options map
- Literal validator takes a specific value and an options map
- Return success if the value matches, otherwise return an appropriate error
- Support custom error messages

## Test Cases
Add tests to `dev.jaide.valhalla.composites-test` that verify:
1. `(enum [:red :green :blue])` creates a validator that accepts those specific keywords
2. `(validate (enum [:red :green :blue]) :red)` returns a success result
3. `(validate (enum [:red :green :blue]) :yellow)` returns a failure result
4. `(validate (enum ["red" "green" "blue"]) "red")` returns a success result
5. `(validate (enum ["red" "green" "blue"]) "yellow")` returns a failure result
6. `(literal :expected-value)` creates a validator that accepts only that specific value
7. `(validate (literal :expected-value) :expected-value)` returns a success result
8. `(validate (literal :expected-value) :unexpected-value)` returns a failure result
9. `(validate (literal 42) 42)` returns a success result
10. `(validate (literal 42) 43)` returns a failure result
11. Custom error messages work for both validators

## Requirements
- Follow established functional patterns
- Create clear and descriptive error messages
- Add docstrings to public functions
- Ensure tests pass

#### Prompt 4.3: Chain and Union Functions

# ClojureScript Validation Library - Chain and Union Functions

Let's implement two key composition functions: chain and union. These allow for combining validators in different ways.

## Task
Extend the `dev.jaide.valhalla.composites` namespace to implement:

1. Chain function that applies multiple validators sequentially (logical AND)
2. Union function that tries multiple validators and succeeds if any succeed (logical OR)
3. Support for custom error messages

## Implementation Guidelines
These functions should:
- Chain takes multiple validators and applies them in sequence, stopping at first failure
- Union takes multiple validators and tries each, succeeding if any succeed
- Chain passes the result of one validator as input to the next
- Union collects errors from all validators if all fail
- Both should support custom error messages

## Test Cases
Add tests to `dev.jaide.valhalla.composites-test` that verify:
1. `(chain (types/string) #(if (seq %) (core/ok %) (core/error "Empty string")))` creates a validator that checks for non-empty strings
2. `(validate (chain (types/string) #(if (seq %) (core/ok %) (core/error "Empty string"))) "test")` succeeds
3. `(validate (chain (types/string) #(if (seq %) (core/ok %) (core/error "Empty string"))) "")` fails
4. `(validate (chain (types/string) #(if (seq %) (core/ok %) (core/error "Empty string"))) 123)` fails at the first validator
5. `(union (types/string) (types/number))` creates a validator that accepts strings or numbers
6. `(validate (union (types/string) (types/number)) "test")` succeeds
7. `(validate (union (types/string) (types/number)) 123)` succeeds
8. `(validate (union (types/string) (types/number)) :not-string-or-number)` fails
9. The union error message should combine errors from all validators
10. Chain should properly pass the value from one validator to the next

## Requirements
- Follow established functional patterns
- Create clear and combined error messages for union
- Ensure chain passes validated data between validators
- Add docstrings to public functions
- Ensure tests pass

#### Prompt 4.4: When and Custom Validators

# ClojureScript Validation Library - When and Custom Validators

Let's implement conditional and custom validators to add more flexibility to our library.

## Task
Extend the `dev.jaide.valhalla.composites` namespace to implement:

1. When function that conditionally applies a validator based on a predicate
2. Custom function for creating arbitrary validators with custom logic
3. Support for custom error messages

## Implementation Guidelines
These functions should:
- When takes a predicate function, a then-validator, and an optional else-validator
- Custom takes a validation function that handles the logic and returns a result
- Both should follow the established validator pattern
- Both should support path tracking and error formatting

## Test Cases
Add tests to `dev.jaide.valhalla.composites-test` that verify:
1. `(when #(= (:type %) :number) (types/number) (types/string))` creates a conditional validator
2. `(validate (when #(= (:type %) :number) (types/number) (types/string)) {:type :number, :value 123})` succeeds
3. `(validate (when #(= (:type %) :number) (types/number) (types/string)) {:type :string, :value "test"})` succeeds
4. `(validate (when #(= (:type %) :number) (types/number) (types/string)) {:type :number, :value "not-a-number"})` fails
5. `(validate (when #(= (:type %) :string) (types/number) (types/string)) {:type :other, :value 123})` fails
6. `(custom (fn [{:keys [value]}] (if (> value 0) (core/ok value) (core/error "Must be positive"))))` creates a custom validator
7. `(validate (custom (fn [{:keys [value]}] (if (> value 0) (core/ok value) (core/error "Must be positive")))) 10)` succeeds
8. `(validate (custom (fn [{:keys [value]}] (if (> value 0) (core/ok value) (core/error "Must be positive")))) -5)` fails
9. Both validators should preserve path information
10. Custom error messages should work for both validators

## Requirements
- Follow established functional patterns
- Ensure conditionals work with the context object
- Custom validators should be able to return any valid result
- Add docstrings to public functions
- Ensure tests pass

#### Prompt 4.5: Default Function

# ClojureScript Validation Library - Default Function

Let's implement a default function that provides default values for missing or nil fields.

## Task
Extend the `dev.jaide.valhalla.composites` namespace to implement:

1. Default function that wraps a validator and provides a default value if the input is nil or missing
2. Integration with existing validators
3. Support for function-based default values

## Implementation Guidelines
The default function should:
- Take a validator and a default value or function
- If the input is nil or :not-found, use the default value
- Otherwise, apply the validator to the input
- Support both static default values and function-based defaults

## Test Cases
Add tests to `dev.jaide.valhalla.composites-test` that verify:
1. `(default (types/string) "default")` creates a validator with a default value
2. `(validate (default (types/string) "default") "test")` returns a success result with "test"
3. `(validate (default (types/string) "default") nil)` returns a success result with "default"
4. `(validate (default (types/string) "default") :not-found)` returns a success result with "default"
5. `(validate (default (types/number) 0) "not-a-number")` returns a failure result
6. `(default (types/number) #(rand-int 100))` creates a validator with a function-based default
7. `(validate (default (types/number) #(rand-int 100)) nil)` returns a success result with a random number
8. Default values should be passed through the validator if they don't match the validator's type
9. Function-based defaults should be called when needed and the result validated

## Requirements
- Follow established functional patterns
- Default values should be validated by the provided validator
- Add docstrings to public functions
- Ensure tests pass

### Phase 5: Advanced Features Prompts

#### Prompt 5.1: Lazy and Ref Validators

# ClojureScript Validation Library - Lazy and Ref Validators

Let's implement lazy and ref validators to support recursive and self-referential data structures.

## Task
Extend the `dev.jaide.valhalla.composites` namespace to implement:

1. Lazy validator that defers schema evaluation until validation time
2. Ref validator that references another validator
3. Support for recursive and self-referential schemas

## Implementation Guidelines
These validators should:
- Lazy takes a function that returns a validator
- Ref takes a var or atom containing a validator
- Both are used to break cycles in schema definitions
- Both should follow the established validator pattern

## Test Cases
Add tests to `dev.jaide.valhalla.composites-test` that verify:
1. `(lazy #(types/string))` creates a validator that behaves like string validator
2. `(validate (lazy #(types/string)) "test")` returns a success result
3. `(validate (lazy #(types/string)) 123)` returns a failure result
4. `(def tree-node (atom nil))` and `(reset! tree-node (collections/hash-map {:value (types/string) :children (collections/vector-of (ref tree-node))}))` creates a recursive tree schema
5. `(validate @tree-node {:value "root" :children []})` succeeds
6. `(validate @tree-node {:value "root" :children [{:value "child" :children []}]})` succeeds
7. `(validate @tree-node {:value "root" :children [{:value 123 :children []}]})` fails
8. `(validate @tree-node {:value "root" :children [{:value "child" :children 123}]})` fails
9. Deeply nested structures should validate correctly
10. Circular references should not cause infinite recursion

## Requirements
- Handle recursive structures correctly
- Avoid infinite recursion
- Follow established functional patterns
- Add docstrings to public functions
- Ensure tests pass

#### Prompt 5.2: JavaScript Interop Validators

# ClojureScript Validation Library - JavaScript Interop Validators

Let's implement validators for JavaScript objects and arrays to facilitate interoperability with JavaScript.

## Task
Create a new namespace `dev.jaide.valhalla.interop` and implement:

1. JS object validator that validates JavaScript objects
2. JS array validator that validates JavaScript arrays
3. JS array-like validator for objects with array-like behavior
4. JS class validator for checking instanceof relationships
5. Conversion between JS and CLJS data structures

## Implementation Guidelines
These validators should:
- JS object checks if a value is a plain JavaScript object
- JS array checks if a value is a JavaScript array
- JS array-like checks if a value has array-like properties
- JS class checks if a value is an instance of a specific JavaScript class
- All should support conversion to ClojureScript data structures

## Test Cases
Create a `dev.jaide.valhalla.interop-test` namespace with tests that verify:
1. `(js-object)` creates a validator that accepts JavaScript objects
2. `(validate (js-object) #js {})` returns a success result
3. `(validate (js-object) #js [])` returns a failure result
4. `(js-array)` creates a validator that accepts JavaScript arrays
5. `(validate (js-array) #js [1 2 3])` returns a success result
6. `(validate (js-array) #js {})` returns a failure result
7. `(js-array-like)` creates a validator that accepts array-like objects
8. `(validate (js-array-like) #js [1 2 3])` returns a success result
9. `(js-array-like)` should accept objects with length properties and numeric indices
10. `(js-class js/Date)` creates a validator that accepts Date instances
11. `(validate (js-class js/Date) (js/Date.))` returns a success result
12. `(validate (js-class js/Date) "not-a-date")` returns a failure result
13. Conversion to ClojureScript should work correctly for each validator type

## Requirements
- Handle JavaScript types correctly
- Convert between JS and CLJS appropriately
- Follow established functional patterns
- Add docstrings to public functions
- Ensure tests pass

#### Prompt 5.3: Error Handling and Internationalization

# ClojureScript Validation Library - Error Handling and Internationalization

Let's implement improved error handling and internationalization support for our validation library.

## Task
Create new namespaces `dev.jaide.valhalla.errors` and `dev.jaide.valhalla.i18n` and implement:

1. Enhanced error formatting utilities
2. Support for error message templates
3. Internationalization system for error messages
4. Integration with existing validators

## Implementation Guidelines
The error handling system should:
- `format-error` function that formats error messages with arguments
- Translation lookup system that supports multiple languages
- Format function that applies arguments to translated templates
- Update existing validators to use the i18n system

## Test Cases
Create test namespaces `dev.jaide.valhalla.errors-test` and `dev.jaide.valhalla.i18n-test` with tests that verify:
1. `(format-error :string/type ["field" "number"])` returns a formatted error message
2. `(format-error :string/type ["field" "number"] :en)` returns an English error message
3. `(format-error :string/type ["field" "number"] :es)` returns a Spanish error message
4. Add translations for at least English and one other language
5. Update core validators to use the i18n system
6. Test that validators use the correct error messages
7. Test that custom error messages still take precedence

## Requirements
- Support multiple languages
- Make error messages more helpful and consistent
- Allow for custom error formatters
- Ensure backward compatibility with existing code
- Add docstrings to public functions
- Ensure tests pass

#### Prompt 5.4: Performance Optimization

# ClojureScript Validation Library - Performance Optimization

Let's implement performance optimizations for our validation library to make it more efficient.

## Task
Create a new namespace `dev.jaide.valhalla.perf` and implement:

1. Lazy validation that stops at the first error
2. Memoization for repeated validations
3. Schema compilation for optimized validation
4. Performance testing utilities

## Implementation Guidelines
The performance optimizations should:
- Lazy mode that returns after the first error
- Memoization to cache validation results for identical inputs
- Schema compilation that pre-computes as much as possible
- Benchmark utilities to compare performance

## Test Cases
Create a `dev.jaide.valhalla.perf-test` namespace with tests that verify:
1. `(with-lazy-validation (validate complex-schema invalid-data))` stops at the first error
2. `(memoize-validator (types/string))` returns a memoized validator
3. Test that memoized validators return the same result for the same input
4. `(compile-schema complex-schema)` returns an optimized validator
5. Test that compiled schemas perform better than regular schemas
6. Add benchmarks for various validation scenarios
7. Compare performance between different optimization strategies

## Requirements
- Maintain correctness while improving performance
- Optimize for common validation scenarios
- Provide clear API for performance options
- Ensure backward compatibility
- Add docstrings to public functions
- Ensure tests pass

#### Prompt 5.5: Integration Tests and Examples

# ClojureScript Validation Library - Integration Tests and Examples

Let's create comprehensive integration tests and examples to demonstrate the capabilities of our validation library.

## Task
Create a new namespace `dev.jaide.valhalla.integration` and implement:

1. Comprehensive integration tests for the entire library
2. Realistic examples of validation scenarios
3. Performance comparisons
4. Documentation examples

## Implementation Guidelines
The integration tests should:
- Test complex combinations of validators
- Validate realistic data structures
- Test all features working together
- Provide usage examples for documentation

## Test Cases
Create a `dev.jaide.valhalla.integration-test` namespace with tests that verify:
1. Complex user schema with multiple validators
2. Nested data structures with various validators
3. Recursive data structures like trees or graphs
4. JavaScript interop scenarios
5. Error handling and i18n integration
6. Performance comparisons for different approaches

## Example Schema
```clojure
(def user-schema
  (collections/hash-map
    {:id (types/string)
     :type (composites/enum [:personal :business])
     :email (composites/chain
              (types/string)
              (fn [{:keys [value]}]
                (if (re-matches #"^[^@]+@[^@]+\.[^@]+$" value)
                  (core/ok value)
                  (core/error "Invalid email format"))))
     :tax-id (composites/when
               #(= (:type %) :business)
               (types/string)
               (composites/optional (types/string)))
     :profile (composites/nilable
                (collections/hash-map
                  {:name (types/string)
                   :age (types/int)}))
     :settings (composites/default
                 (collections/hash-map
                   {:notifications (types/boolean)})
                 {:notifications true})}))
```

## Requirements
- Test all library features together
- Create realistic validation scenarios
- Provide examples for documentation
- Ensure correct behavior in complex scenarios
- Add docstrings to example functions
- Ensure all tests pass


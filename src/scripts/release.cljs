(ns scripts.release
  (:require
   [clojure.string :as s]
   [promesa.core :as p]
   ["child_process" :as cp]
   ["fs" :as fs]
   ["readline" :as readline]))

(def repo-url "https://github.com/jaidetree/valhalla")

(def files ["build.edn" "package.json" "package-lock.json" "README.md"])
(def snapshot-files #{"build.edn" "README.md"})

(defn- exit
  [code]
  (js/process.exit code))

(defn help
  []
  (println "\nUSAGE:
  nbb -m scripts.release version YYYY.M.D
    Update the version string across known files

  nbb -m scripts.release create YYYY.M.D
    Checkout main, update the version, commit changes, create tag, push to origin")
  (exit 1))

(def date-pattern #"[\d]{4}\.(?!0)[\d]{1,2}\.(?!0)[\d]{1,2}")
(def version-pattern #"[\d]{4}\.[\d]{1,2}\.[\d]{1,2}(?:-SNAPSHOT)?")

(comment
  (re-find date-pattern "2025.03.05")
  (re-find date-pattern "2025.3.5")
  (re-find version-pattern "2025.03.05-SNAPSHOT"))

(defn assert-date
  [input]
  (assert (not (nil? (re-find date-pattern input)))
          (str "Expected date input in YYYY.M.D format, got " input)))

(defn- slurp
  [filepath]
  (.readFileSync fs filepath #js {:encoding "utf-8"}))

(defn- spit
  [filepath contents]
  (.writeFileSync fs filepath contents #js {:encoding "utf-8"}))

(defn update-version
  "Updates version across files given valid date string with no leading zeros

  Arguments:
  - date - YYYY.M.D[-SNAPSHOT] string

  Returns nil"
  [date & _args]
  (let [[date suffix] (s/split (js/String date) #"-")]
    (assert-date date)
    (doseq [file files]
      (let [contents (slurp file)
            version (if (and suffix (contains? snapshot-files file))
                      (str date "-" (s/upper-case (or suffix "")))
                      date)]
        (spit file (s/replace contents version-pattern version))))
    (println "Updated version to" (if suffix (str date "-" suffix) date))))

(defn flatten-args
  [args]
  (->> args
       (mapcat (fn [arg]
                 (cond
                   (map? arg) (list arg)
                   (s/starts-with? arg "\"") (list arg)
                   :else (s/split arg #" "))))))

(comment
  (flatten-args ["git commit -m" "\"my message\"" "branch --HEAD"]))

(defn parse-args
  [args]
  (let [[cmd & args] (flatten-args args)
        arg-or-opts (last args)]
    (if (map? arg-or-opts)
      [cmd (flatten-args (butlast args)) arg-or-opts]
      [cmd (flatten-args args) {}])))

(comment
  (parse-args ["git commit -m" "\"my message\"" "branch --HEAD" {:stdio ["inherit" "inherit" "inherit"]}]))

(defn $
  "A helper for running shell commands synchronously

  Arguments:
  - & args - Variadic strings can be space separated, separate strings, strings
             that begin with \" will be treated as a single argument

  Returns a hash-map with the status, signal, stdout, and stderr strings"
  [& args]
  (let [[cmd args opts] (parse-args args)
        result (.spawnSync cp
                           cmd
                           (clj->js args)
                           (-> (merge
                                {:encoding "utf-8"
                                 :shell true
                                 :stdio ["pipe" "pipe" "pipe"]}
                                opts)
                               (clj->js)))]

    #_(println cmd (s/join " " args))

    {:status (.-status result)
     :signal (.-signal result)
     :stdout (.-stdout result)
     :stderr (.-stderr result)}))

(defn- lines
  [s]
  (->> (s/split s #"\n")
       (filter #(not (s/blank? %)))))

(defn- squote
  [& s]
  (str "\"" (s/join "" s) "\""))

(defn- uncommitted-files
  "Get list of modified but uncommitted files from git

  Returns nil or a sequence of files"
  []
  (let [{:keys [stdout] :as result} ($ "git diff-index --name-status HEAD")
        files (->> (s/trim stdout)
                   (lines))]
    (if (zero? (count files))
      nil
      files)))

(defn- create-rl
  []
  (.createInterface
   readline
   #js {:input js/process.stdin
        :output js/process.stdout}))

(defn read
  "Read a line of input from the user

  Arguments:
  - prompt - A string like (Y/n) [n]
  - [default] - Optional value to return if no input was provided

  Returns a promise that yields the user input"
  [prompt & [default]]
  (let [rl (create-rl)]
    (p/create
     (fn [resolve _reject]
       (.question rl (str prompt ": ")
                  (fn [input]
                    (let [input (s/trim input)]
                      (.close rl)
                      (resolve
                       (if (s/blank? input)
                         default
                         input)))))))))

(defn create-release
  "Prepares a release and generates the URL to draft the release tag

  Arguments:

  - date-version - YYYY.M.D format string representing the release version

  Returns a promise that yields nil "
  [date-version & _args]
  (assert-date date-version)
  (let [date-version (s/upper-case date-version)
        release-url (str repo-url "/releases/new?tag=" date-version "&title=" date-version)]
    (p/do
      (println "\nCheckout main branch")
      ($ "git checkout main")

      (when-let [files (uncommitted-files)]
        (println "\nError: Working directory is not clean")
        (println (->> files
                      (s/join "\n")))
        (exit 1))

      (println "\nUpdating version to" date-version)
      (update-version date-version)

      (when-not (uncommitted-files)
        (println "Error: No files were modified")
        (exit 1))

      ($ "git add" (s/join " " files))
      ($ "git commit -m" (squote "chore: Update version to " date-version))

      (println "\nCreating git tag")
      ($ "git tag -a" (squote date-version) "-m" (squote "Prepare release " date-version))

      (println "\nPushing git tag and main branch\n")
      ($ "git push origin main" date-version "--force-with-lease"
         {:stdio "inherit"})

      (println "")
      (println release-url)

      (println "\nOpen release page in browser?")
      (p/let [input (read "(Y/n) [n]" "n")]
        (when (s/starts-with? (s/lower-case input) "y")
          ($ "open" (squote release-url)
             {:stdio "inherit"}))
        nil))))

(defn -main
  "Updates the version string across files. Only includes -SNAPSHOT suffix in
  build.edn and README.md

  Arguments:
  - help - Show usage
  - YYYY.M.D[-SNAPSHOT] - Updates the version across files. No leading zeros.

  Usage:
  nbb -m scripts.version 2025.04.16-SNAPSHOT"
  [subcmd & args]
  (try
    (p/do
      (case (s/lower-case subcmd)
        "help" (help)

        "version" (apply update-version args)

        "create" (apply create-release args)

        (help)))
    (catch :default error
      (println (.-message error))
      (help))))

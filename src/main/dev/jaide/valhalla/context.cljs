(ns dev.jaide.valhalla.context
  (:refer-clojure :exclude [get-in])
  (:require
   [clojure.pprint :refer [pprint]]))

(defn unset?
  [v]
  (= v ::unset))

(defn create
  [& {:keys [value path errors input output]
      :or {input ::unset
           path []
           errors []
           value ::unset
           output ::unset}}]
  {:value (cond
            (not (unset? value)) value
            (not (unset? input)) input
            :else nil)
   :path path
   :errors errors
   :input (cond
            (not (unset? input)) input
            (not (unset? value)) value
            :else nil)
   :output (cond
             (not (unset? output)) output
             :else nil)})

(defn update-value
  [ctx value]
  (assoc ctx :value value))

(defn clear-errors
  [ctx]
  (assoc ctx :errors []))

(defn raise-error
  [ctx err]
  (let [error (if (string? err)
                {:path (:path ctx) :message err}
                err)]
    (update ctx :errors conj error)))

(defn raise-errors
  [ctx errors]
  (->> errors
       (reduce
        (fn [ctx error]
          (raise-error ctx error))
        ctx)))

(defn update-output
  [ctx output]
  (assoc ctx :output output))

(defn update-path
  [ctx path]
  (assoc ctx :path path))

(defn append-path
  [ctx path]
  (update ctx :path conj path))

(defn- get-in
  [m ks]
  (->> ks
       (reduce
        (fn [src path]
          (cond
            (map? src)    (get src path)
            (vector? src) (get src path)
            (list? src)   (nth src path)
            (set? src)    (nth (seq src) path)
            :else         (throw
                           (js/Error. (str "custom get-in couldn't navigate "
                                           (pr-str src) " at " (pr-str path))))))
        m)))

(defn replace-path
  ([ctx path]
   (replace-path ctx (count (:path ctx))) path)
  ([ctx idx path]
   (let [prev-path (vec (take idx (:path ctx)))
         new-path (conj prev-path path)]
     (-> ctx
         (update-path new-path)
         (update-value (get-in ctx (cons :input new-path)))))))

(defn replace-path-no-value
  ([ctx path]
   (replace-path-no-value ctx (count (:path ctx))) path)
  ([ctx idx path]
   (let [prev-path (vec (take idx (:path ctx)))
         new-path (conj prev-path path)]
     (-> ctx
         (update-path new-path)))))

(defn path>
  [ctx path]
  (let [new-path (conj (:path ctx) path)]
    (-> ctx
        (update-path new-path)
        (update-value (get-in ctx (cons :input new-path))))))

(defn path<
  [ctx]
  (let [new-path (vec (butlast (:path ctx)))]
    (-> ctx
        (update-path new-path)
        (update-value (get-in ctx (cons :input new-path))))))

(defn accrete
  "
  Used after a successful validation or parse, stores the updated value and
  builds the output shape
  "
  ([ctx value]
   (-> ctx
       (update-value value)
       (update-output value)))
  ([ctx key value]
   (-> ctx
       (update-value value)
       (assoc-in [:output key] value))))


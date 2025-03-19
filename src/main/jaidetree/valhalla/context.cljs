(ns jaidetree.valhalla.context)

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

(defn update-path
  [ctx path]
  (assoc ctx :path path))

(defn append-path
  [ctx path]
  (update ctx :path conj path))

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
  ([ctx output]
   (if (vector? output)
     (let [[path value] output]
       (update-output ctx path value))
     (update-output ctx (:path ctx) output)))
  ([ctx path value]
   (assoc-in ctx (cons :output path) value)))

(defn replace-path
  [ctx path]
  (let [prev-path (vec (butlast (:path ctx)))
        new-path (conj prev-path path)]
    (-> ctx
        (update-path new-path)
        (update-value (get-in ctx (cons :input new-path))))))

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
  [ctx value]
  (let [{:keys [path]} ctx]
    (-> ctx
        (update-value value)
        (update-output path value))))


(ns dev.jaide.valhalla.utils)

(defn stringify
  [value]
  (try
    (-> (cond
          (string? value) (js/String value)
          (instance? js/Date value) (js/String value)
          :else value)
        (pr-str))
    (catch :default _err
      (pr-str value))))

(defn msg-fn
  [str-or-fn default-fn]
  (cond
    (string? str-or-fn) (constantly str-or-fn)
    (fn? str-or-fn) str-or-fn
    :else default-fn))


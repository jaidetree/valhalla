(ns jaidetree.valhalla.utils)

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

(ns jaidetree.valhalla.core)

(defn success
  [data]
  {:result :v/ok
   :errors []
   :data   data})

(defn fail
  [data errors]
  {:result :v/fail
   :errors errors
   :data    data})

(defn ok
  [value]
  (success value))

(defn error
  ([message]
   (fail nil [{:path []
               :message message}]))
  ([message value]
   (fail value [{:path []
                 :message message}]))
  ([path message value]
   (fail value [{:path path
                 :message message}])))

(defn errors
  [errs value]
  (fail value errs))

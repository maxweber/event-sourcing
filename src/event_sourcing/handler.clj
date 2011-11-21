(ns event-sourcing.handler)

(defn transfer
  ([source-key target-key]
     (fn [target source]
       (assoc target target-key (get source source-key))))
  ([key]
     (transfer key key)))

(defn transfer->
  [keyseq]
  (fn [target source]
    (merge target (select-keys source keyseq))))


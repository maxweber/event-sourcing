(ns event-sourcing.handler)

(defn transfer-handler
  [target-key source-key]
  (fn [target source]
    (assoc target target-key (get source source-key))))

(defn merge-handler
  [keyseq]
  (fn [target source]
    (merge target (select-keys source keyseq))))


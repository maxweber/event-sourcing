(ns event-sourcing.core)

(defn replay-fn [get-handlers]
  (fn [model event]
    (let [handlers (get-handlers event)]
      (reduce (fn [model handler]
            (handler model event)) model handlers))))

(defn to-version? [version event-number-key]
  (fn [event]
    (<= (get event event-number-key) version)))

(defn relevant-events [model-version event-number-key events]      
  (drop-while (to-version? model-version event-number-key) events))

(defn replay-events-fn [replay]
  (fn [model events]
    (reduce replay model events)))

(defn transfer-handler [target-key source-key]
  (fn [target source]
    (assoc target target-key (get source source-key))))

(defn merge-handler [keyseq]
  (fn [target source]
    (merge target (select-keys source keyseq))))

(defn version-handler [version-key event-number-key]
  (transfer-handler version-key event-number-key))

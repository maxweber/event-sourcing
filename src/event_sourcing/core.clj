(ns event-sourcing.core)

(defn if-event [event-name f]
  (vary-meta f assoc ::event event-name))

(defn- apply-handlers [handlers model event]
  (if handlers
    (reduce (fn [model handler]
              (handler model event)) model handlers)
    model))

(defn replay-fn [handlers event-key version-key event-number-key]
  (let [handlers (reduce (fn [m handler]
                           (let [event-name (::event (meta handler))]
                             (update-in m [event-name] #(vec (conj % handler))))) {} handlers)]
    (fn [model event]
      (let [event-name (get event event-key)
            handlers (get handlers event-name)
            new-model (apply-handlers handlers model event)]
        (assoc new-model version-key (get event event-number-key))))))

(defn to-version? [version event-number-key]
  (fn [event]
    (<= (get event event-number-key) version)))

(defn replay-events-fn [handlers event-key version-key event-number-key]
  (let [replay (replay-fn handlers event-key version-key event-number-key)]
    (fn [model events]
      (let [model-version (get model version-key)
            events (if model-version
                     (drop-while (to-version? model-version event-number-key) events)
                     events)]
        (reduce replay model events)))))

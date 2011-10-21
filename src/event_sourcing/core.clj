(ns event-sourcing.core)

(defn if-event [relevant-event f]
  {:pre [(or (fn? relevant-event) (string? relevant-event))]}
  (if (fn? relevant-event)
    (vary-meta f assoc ::event? relevant-event)
    (vary-meta f assoc ::event relevant-event)))

(defn- apply-handlers [handlers model event]
  (if handlers
    (reduce (fn [model handler]
              (handler model event)) model handlers)
    model))

(defn- prepare-handlers [handlers]
  (reduce (fn [m handler]
            (let [relevant-event (::event (meta handler))]
              (update-in m [relevant-event] #(vec (conj % handler))))) {} handlers))

(defn replay-fn [handlers event-key version-key event-number-key]
  (let [handlers-per-event-name (prepare-handlers (filter #(::event (meta %)) handlers))
        handlers-event-predicate (filter #(::event? (meta %)) handlers)
        relevant-handlers (fn [event]
                            (let [event-name (get event event-key)]
                              (concat (get handlers-per-event-name event-name)
                                      (filter #((::event? (meta %)) event) handlers-event-predicate))))]
    (fn [model event]
      (let [event-name (get event event-key)
            handlers (relevant-handlers event)
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

(ns event-sourcing.core
  (:use [event-sourcing.handler :only [transfer-handler]]))

(def ^{:dynamic true} *position-keyword* :_pos)

(defn position-fn [model-or-event]
  (get model-or-event *position-keyword* -1))

(defn transfer-position-fn [model last-event]
  (assoc model *position-keyword* last-event))

(defn replay-fn [get-handlers]
  (fn [model event]
    (let [handlers (get-handlers event)]
      (reduce (fn [model handler]
                (handler model event)) model handlers))))

(defn load-state-fn
  ([replay transfer-position-fn]
     (fn [model events]
       (let [model (reduce replay model events)]
         (transfer-position-fn model (last events)))))
  ([replay]
     (load-state-fn transfer-position-fn)))

(defn events-current-state
  ([model events position-fn]
     (let [model-version (position-fn model)]
       (drop-while (fn [event] (<= (position-fn event) model-version)) events)))
  ([model events]
     (events-current-state model events position-fn)))

(defn events-historic-state
  ([model events max-position position-fn]
     (take-while (fn [event] (<= (position-fn event) max-position))
                 (events-current-state model events position-fn)))
  ([model events max-position]
     (events-historic-state model events max-position position-fn)))

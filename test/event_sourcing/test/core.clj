(ns event-sourcing.test.core
  (:use event-sourcing.core
        clojure.contrib.core
        [lazytest.describe
         :only [describe it given do-it using testing]]
        [lazytest.expect :only [expect]]))

;a shopping cart scenario

(def aggregate-id "123")

(def event0 {:_event "shoppingcart_created"
             :_aggregate aggregate-id
             :_number 0})

(def item1-id "456")

(def event1 {:_event "shoppingcart_item_added"
             :_number 1
             :item item1-id})

(def item2-id "789")

(def event2 {:_event "shoppingcart_item_added"
             :_number 2
             :item item2-id})

(def event3 (assoc event1 :_number 3))

(def event4 {:_event "shoppingcart_item_removed"
             :_number 4
             :item item2-id})

(def events [event0 event1 event2 event3 event4])

(def handler0 (if-event "shoppingcart_created"
                        (fn [model event]
                          (assoc model
                            :_aggregate (:_aggregate event)
                            :_type "shoppingcart"))))

(def handler1 (if-event "shoppingcart_item_added"
                        (fn [model event]
                          (let [item-id (:item event)
                                quantity (get-in model [:items item-id] 0)]
                            (assoc-in model [:items item-id] (inc quantity))))))

(def handler2 (if-event "shoppingcart_item_removed"
                        (fn [model event]
                          (let [item-id (:item event)
                                quantity (get-in model [:items item-id])
                                new-quantity (dec quantity)]
                            (if (= 0 new-quantity)
                              (dissoc-in model [:items item-id])
                              (assoc-in model [:items item-id] new-quantity))))))

(def handlers [handler0 handler1 handler2])

(describe "Event sourcing"
  (given [event-number-key :_number
          extract #(select-keys % [:_aggregate :_type :items])
          replay-events (replay-events-fn handlers :_event :_version event-number-key)
          expected-current-state {:_aggregate aggregate-id
                                  :_type "shoppingcart"
                                  :items {item1-id 2}}]
    (given [current-state (replay-events {} events)]
      (it "should build the current state out of a stream of events"
        (= expected-current-state
           (extract current-state)))
      (it "should set the version-key attribute of the model to the number of the last applied event"
        (= (:_number (last events)) (:_version current-state))))
    (given [version 3
            events-till-version-3 (take-while (to-version? version event-number-key) events)
            state (replay-events {} events-till-version-3)]
      (it "should can build the state of a given version"
        (= {:_aggregate aggregate-id
            :_type "shoppingcart"
            :items {item1-id 2
                    item2-id 1}}
           (extract state)))
      (given [current-state (replay-events state events)]
        (it "should can build the current state starting from a snapshot state"
          (= expected-current-state
             (extract current-state)))))))

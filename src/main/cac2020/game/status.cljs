(ns cac2020.game.status
  (:require-macros [cac2020.macro :as m])
  (:require [clojure.string :as string]
            ["pixi.js" :as pixi]
            ["va5" :as va5]
            [cac2020.property :as p :include-macros true]
            [cac2020.util :as util]
            [cac2020.space :as space]
            [cac2020.dataurl :as dataurl]
            [cac2020.pos :as pos]
            [cac2020.pointer :as pointer]
            [cac2020.pixi.util :as putil]
            [cac2020.tween :as tween]
            ))



(def goal-dist 20000)
;;; NB: lodge-posに実際のlodgeがあるが、ゴール判定はその-6m地点にあり、
;;;     そこからエンディング内移動で右に+2m進む
(def goal-adjust 400)
(def lodge-pos (+ goal-dist goal-adjust))



(defn update-status-label! [a-state & [status-label]]
  (let [
        status-label (or status-label (:tree/status-label @a-state))
        ;{:keys [space-spd-x space-spd-y space-spd-z space-spd-yaw space-spd-pitch]} @a-state
        score (or (:score @a-state) 0)
        ^js player-layer (:tree/player-layer @a-state)
        elapsed-sec (:elapsed-sec @a-state)
        travel-point (or
                       (when player-layer
                         (.-x player-layer))
                       0)
        travel-meter (int (/ travel-point 100))
        text (str ""
                  "TRAVEL: " travel-meter "m"
                  "\n"
                  "  TIME: " elapsed-sec
                  "\n"
                  " SCORE: " score
                  )
        ]
    (when-let [bar (:tree/status-bar @a-state)]
      (util/sync-status-bar bar (/ travel-point goal-dist)))
    (p/set! status-label :text text)))










(defn make-ui-layer [a-state s-w s-h]
  (let [status-label (p/set! (util/make-label ""
                                              :font-size 32
                                              :align "left"
                                              )
                             :anchor/x 0
                             :anchor/y 0
                             :x 16
                             :y 16
                             :name (name :tree/status-label))
        b (fn [k label xr yr f]
            (p/set! (util/make-button label f :padding 32)
                    :x (* s-w xr)
                    :y (* s-h yr)
                    :name (name k)))]
    (update-status-label! a-state status-label)
    [:tree/ui-layer
     {:visible false}
     (p/set! (util/make-status-bar :w (- 960 32)
                                   :h 16
                                   :anchor-x 0
                                   :anchor-y 1
                                   :outline-size 2
                                   :empty-size 2
                                   :outline-color 0x7F7F7F
                                   :empty-color 0x000000
                                   :fuel-color 0x1FBF7F
                                   )
             :x 16
             :y 56
             :name (name :tree/status-bar))
     status-label
     (b :tree/ui-action-button
        "ACTION"
        0.5
        0.9
        (fn [b]
          (if-not (and
                    (= :game (:mode @a-state))
                    true ; TODO
                    )
            (util/cancel-button-effect!)
            (let []
              (util/se! :se/submit-psg)
              ;; TODO
              ))))
     ]))



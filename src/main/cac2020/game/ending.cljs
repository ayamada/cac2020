(ns cac2020.game.ending
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
            [cac2020.game.status :as status]
            ))





;(gain-clear-score! a-state (.-x goal) (- (.-y goal) 32))
;(gain-clear-score! a-state (.-x player-layer) (.-y player-sp))
(defn- gain-clear-score! [a-state x y]
  (let [layer (:tree/scroll-near-effect-layer @a-state)
        score-base 10000
        elapsed-sec (:elapsed-sec @a-state)
        score (- score-base elapsed-sec)
        text (str "-" elapsed-sec "\n" score-base)]
    (swap! a-state update :score + score)
    (swap! a-state update :score max 0)
    ;(util/se! :se/coin-psg)
    (status/update-status-label! a-state)
    (util/effect-score! layer text x y 240)))


(defn emit! [a-state ^js goal goal-pos]
  (swap! a-state assoc :mode :ending)
  (util/bgm! nil)
  (let [^js lodge-sp (aget (.-children goal) 0)
        ^js player-layer (:tree/player-layer @a-state)
        ^js player-sp (:tree/player-sp @a-state)
        ;; NB: プレイヤーが飛び込むのはgoalの扉の位置なので、補正する必要がある
        goal-x (+ (.-x goal) -12)
        goal-y (+ (.-y goal) 24)
        ;; TODO: もうちょっと継続が分かりやすいようにしたい。ただ今回は最小構成にしたいのでcore/asyncは使いたくない。いい方法を考えたいが…
        congratulations! (fn [& _]
                           (p/set! (:tree/gameclear-layer @a-state)
                                   :visible true)
                           (util/se! :se/lvup-midi))
        c6 (fn [_]
             ;; TODO
             (gain-clear-score! a-state (.-x goal) (.-y goal))
             (congratulations!))
        c5 (fn [_]
             (swap! a-state assoc :space-spd-pitch 0)
             ;(tween/wait! player-layer
             ;             240
             ;             (congratulations!))
             (let [p-x (tween/pp (.-x goal) (- goal-pos 192))
                   p-y (tween/pp (.-y goal) -400)
                   p-s (tween/pp (p/get goal :scale/x) 0)

                   p-sx (tween/pp (:space-spd-x @a-state) 1)
                   p-sy (tween/pp (:space-spd-y @a-state) -2)
                   p-sz (tween/pp (:space-spd-z @a-state) 0.01)
                   p-syaw (tween/pp (:space-spd-yaw @a-state) 0)
                   p-sp (tween/pp (:space-spd-pitch @a-state) 2)
                   jump -192]
               (tween/register! goal
                                300
                                (fn [^js o progress]
                                  (swap! a-state
                                         assoc
                                         :space-spd-x (tween/ap p-sx progress)
                                         :space-spd-y (tween/ap p-sy progress)
                                         :space-spd-z (tween/ap p-sz progress)
                                         :space-spd-yaw (tween/ap p-syaw progress)
                                         :space-spd-pitch (tween/ap p-sp progress))
                                  (let [p-sin (Math/sin (* Math/PI progress))
                                        s (tween/ap p-s progress)]
                                    (p/set! o
                                            :x (tween/ap p-x progress)
                                            :y (+ (tween/ap p-y progress)
                                                  (* p-sin jump))
                                            :scale/x s
                                            :scale/y s)))
                                c6)))
        c4 (fn []
             (let [ground-layer (:tree/ground-layer @a-state)
                   ground-far (:tree/ground-far @a-state)
                   ground-mid (:tree/ground-mid @a-state)
                   ground-near (:tree/ground-near @a-state)
                   ground-nearest (:tree/ground-nearest @a-state)
                   end-y (+ 960 256)
                   p-far (tween/pp (.-y ground-far) end-y)
                   p-mid (tween/pp (.-y ground-mid) end-y)
                   p-near (tween/pp (.-y ground-near) end-y)
                   p-nearest (tween/pp (.-y ground-nearest) end-y)]
               (swap! a-state assoc :space-spd-pitch -1)
               (tween/register! ground-layer
                                300
                                (fn [^js o progress]
                                  (p/set! ground-far :y (tween/ap p-far progress))
                                  (p/set! ground-mid :y (tween/ap p-mid progress))
                                  (p/set! ground-near :y (tween/ap p-near progress))
                                  (p/set! ground-nearest :y (tween/ap p-nearest progress)))
                                c5)))
        c3 (fn [_]
             (util/vibrate! 1000)
             (util/se! :se/launch-psg :volume 0.5)
             (let [^js flame (p/set! (util/make-flame-sp 0.5)
                                     :x 0
                                     :y 32
                                     :alpha 0)
                   ttl-frames 120
                   start-y (.-y goal)
                   p-y (tween/pp start-y (+ start-y -64))
                   far-object-layer (:tree/far-object-layer @a-state)
                   near-object-layer (:tree/near-object-layer @a-state)
                   ]
               (.addChild goal flame)
               (tween/vibrate! lodge-sp ttl-frames 8 0 nil true)
               (tween/register! goal
                                ttl-frames
                                (fn [^js o progress]
                                  (p/set! flame :alpha progress)
                                  ;; NB: ここは「goalのみfar-object-layerにある、goal以外のobjは全部near-object-layerにある」という前提で、邪魔な他objを非表示にしている。この前提を崩した場合はここも直す事！
                                  (p/set! near-object-layer :alpha (- 1 progress))
                                  (p/set! goal :y (tween/ap p-y progress)))
                                c4)))
        c2 (fn [_]
             (tween/wait! player-layer
                          30
                          (fn [_]
                            (tween/vibrate! lodge-sp 120 0 8 c3 true))))
        p-pl-x (tween/pp (.-x player-layer) goal-x)
        p-pl-y (tween/pp (.-y player-layer) goal-y)
        p-pl-s (tween/pp 1 0.25)
        p-sp-y (tween/pp (.-y player-sp) -128)
        c1 (fn [_]
             (status/update-status-label! a-state)
             (tween/wait! player-layer
                          30
                          (fn [_]
                            (tween/change-alpha! player-layer 60 0 c2))))
        ]
    (tween/wait! player-layer
                 60
                 (fn [_]
                   (util/vibrate! 50)
                   (util/se! :se/puyo-psg :volume 0.5)
                   (util/effect-smoke! (:tree/scroll-near-effect-layer @a-state)
                                       15
                                       :x (.-x player-layer)
                                       :y (.-y player-layer)
                                       :size 64)
                   (tween/register! player-layer
                                    120
                                    (fn [_ progress]
                                      (let [p-sin (Math/sin (* Math/PI progress))
                                            s (tween/ap p-pl-s progress)]
                                        (p/set! player-layer
                                                :x (tween/ap p-pl-x progress)
                                                :y (tween/ap p-pl-y progress)
                                                :scale/x s
                                                :scale/y s)
                                        (p/set! player-sp :y (/ (tween/ap p-sp-y p-sin) s))))
                                    c1)))))




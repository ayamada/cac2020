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
            ))





(defn- resolve-flame1-texes []
  (let [tex-src (putil/->tex :flame1)
        tex0 (.clone tex-src)
        tex1 (.clone tex-src)
        tex2 (.clone tex-src)
        setup-frame! (fn [idx t]
                       (let [frame (.-frame t)
                             w 80
                             h 96
                             x (* (mod idx 3) w)
                             y (* (quot idx 3) h)]
                         (set! (.-x frame) x)
                         (set! (.-y frame) y)
                         (set! (.-width frame) w)
                         (set! (.-height frame) h)
                         (.updateUvs t)
                         t))]
    (to-array (map-indexed setup-frame!
                           [tex0 tex1 tex2]))))


(defn- make-flame-sp [& [scale]]
  (let [scale (or scale 1)]
    (p/set! (pixi/AnimatedSprite. (resolve-flame1-texes))
            :animation-speed 0.5
            :anchor/x 0.5
            :anchor/y 1
            :scale/x scale
            :scale/y (- scale))))



(defn emit! [a-state ^js goal goal-pos update-status-label!]
  (swap! a-state assoc :mode :ending)
  (util/bgm! nil)
  (let [^js lodge-sp (aget (.-children goal) 0)
        ^js player-layer (:tree/player-layer @a-state)
        ^js player-sp (:tree/player-sp @a-state)
        ;; NB: プレイヤーが飛び込むのはgoalの扉の位置なので、補正する必要がある
        goal-x (+ (.-x goal) -12)
        goal-y (+ (.-y goal) 24)
        ;; TODO: もうちょっと継続が分かりやすいようにしたい。ただ今回は最小構成にしたいのでcore/asyncは使いたくない。いい方法を考えたいが…
        c6 (fn [_]
             ;; TODO
             )
        c5 (fn [_]
             (swap! a-state assoc :space-spd-pitch 0)
             (tween/wait! player-layer
                          120
                          (fn [_]
                            (p/set! (:tree/gameclear-layer @a-state) :visible true)
                            (util/se! :se/lvup-midi)))
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
                   end-y 1024
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
             (util/vibrate! 500)
             (util/se! :se/launch-psg)
             (let [^js flame (p/set! (make-flame-sp 0.5)
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
               (.play flame)
               (tween/vibrate! lodge-sp ttl-frames 4 0)
               (tween/register! goal
                                ttl-frames
                                (fn [^js o progress]
                                  (p/set! flame :alpha progress)
                                  ;; NB: ここは「goalのみfar-object-layerにある、goal以外のobjは全部near-object-layerにある」という前提で、邪魔な他objを非表示にしている。この前提を崩した場合はここも直す事！
                                  (p/set! near-object-layer :alpha (- 1 progress))
                                  (p/set! goal :y (tween/ap p-y progress)))
                                c4)))
        c2 (fn [_]
             ;; TODO: タイムボーナスをスコアに反映
             (update-status-label!)
             (tween/wait! player-layer
                          30
                          (fn [_]
                            (tween/vibrate! lodge-sp 120 0 4 c3))))
        p-pl-x (tween/pp (.-x player-layer) goal-x)
        p-pl-y (tween/pp (.-y player-layer) goal-y)
        p-pl-s (tween/pp 1 0.25)
        p-sp-y (tween/pp (.-y player-sp) -128)
        c1 (fn [_]
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




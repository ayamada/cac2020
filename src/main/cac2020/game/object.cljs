(ns cac2020.game.object
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
            [cac2020.game.ending :as ending]
            [cac2020.game.status :as status]
            ))

;;; TODO: 地形の配置について
;;; - 基本的には :pudding で壁を作る
;;; - 前半は飛び石みたいに楽に行けるようにする、後半は迷路っぽく組む
;;; - グループ定義してmapで座標をいじりconcatする
;;; - ...

(defn- connected-puddings [x y]
  [{:x x :y y :k :pudding}
   {:x (+ x 100) :y y :k :pudding}])

(defn- tunneled-puddings [x y]
  [{:x x :y y :k :pudding}
   {:x x :y (- y 192) :k :pudding}])

(defn- twin-puddings [x y]
  [{:x x :y y :k :pudding}
   {:x (+ x 150) :y y :k :pudding}])


(def map-bp
  (sort-by (juxt :x :y)
           (concat nil
                   ;; 序盤の簡単エリア
                   [
                    {:x 1000 :y 0 :k :pudding}
                    {:x 1600 :y 0 :k :pudding}
                    {:x 1900 :y -50 :k :cherry}
                    {:x 2200 :y 0 :k :pudding}
                    {:x 2700 :y 0 :k :pudding}
                    {:x 3200 :y 0 :k :pudding}
                    {:x 3700 :y 0 :k :pudding}
                    ]
                   ;; 敵の顔見せ
                   [{:x 4200 :y -640 :k :enemy-w}
                    {:x 4800 :y 0 :k :enemy-b}]
                   [
                    {:x 5400 :y 0 :k :pudding}
                    {:x 6200 :y 0 :k :pudding}
                    {:x 6500 :y -200 :k :cherry}
                    {:x 7000 :y -240 :k :enemy-c}
                    {:x 7800 :y 0 :k :pudding}
                    {:x 8200 :y 0 :k :pudding}
                    {:x 9100 :y 0 :k :enemy-b}
                    {:x 9600 :y 0 :k :pudding}
                    {:x 10000 :y -640 :k :enemy-w}
                    {:x 10000 :y 0 :k :pudding}
                    {:x 10500 :y 0 :k :pudding}
                    {:x 11200 :y 0 :k :enemy-b}
                    ]
                   (connected-puddings 11800 0)
                   (tunneled-puddings 12000 0)
                   (tunneled-puddings 12100 0)
                   (tunneled-puddings 12200 0)
                   (tunneled-puddings 12300 0)
                   (tunneled-puddings 12400 0)
                   (tunneled-puddings 12500 0)
                   [{:x 12500 :y -480 :k :enemy-c}]
                   (tunneled-puddings 12600 0)
                   (tunneled-puddings 12700 0)
                   (tunneled-puddings 12800 0)
                   (tunneled-puddings 12900 0)
                   [{:x 13000 :y -100 :k :cherry}]
                   (twin-puddings 13800 0)
                   [{:x 14200 :y -200 :k :cherry}]
                   [{:x 14400 :y -640 :k :enemy-w}]
                   (twin-puddings 14400 0)
                   [{:x 14800 :y -200 :k :cherry}]
                   (twin-puddings 15000 0)
                   [
                    ;{:x 16000 :y -640 :k :enemy-w} ; これは難易度が上がりすぎる
                    {:x 16000 :y 0 :k :pudding}
                    {:x 16100 :y -80 :k :pudding}
                    {:x 16200 :y -160 :k :pudding}
                    {:x 16300 :y -240 :k :pudding}
                    {:x 16400 :y -320 :k :pudding}
                    {:x 16600 :y -240 :k :enemy-c}
                    {:x 16700 :y -640 :k :enemy-w}
                    ]
                   [
                    {:x 17000 :y 0 :k :enemy-b}
                    {:x 18000 :y 0 :k :enemy-b}
                    {:x 19000 :y 0 :k :enemy-b}
                    {:x 17000 :y -640 :k :enemy-w}
                    {:x 18000 :y -640 :k :enemy-w}
                    {:x 19000 :y -640 :k :enemy-w}
                    {:x 17500 :y -240 :k :enemy-c}
                    {:x 18000 :y -240 :k :enemy-c}
                    {:x 18500 :y -240 :k :enemy-c}
                    {:x 19000 :y -240 :k :enemy-c}
                    {:x 19500 :y -240 :k :enemy-c}
                    ]
                   nil)))

(defn make-map []
  (atom map-bp))




(defonce ^js tmp-rect (pixi/Rectangle. 0 0 0 0))



;;; TODO: 本当はマクロ化した方がよい
(defonce a-object-defs (atom {}))
(defn defobj [k & args]
  (swap! a-object-defs assoc k (util/args->map args)))




(def bounce-se-puyo!
  (util/make-play-se-periodically 0.2 :se/puyo-psg :volume 1))



(defn emit-near-smoke! [a-state x y]
  (util/effect-smoke! (:tree/scroll-near-effect-layer @a-state)
                      15
                      :x x
                      :y y
                      :size 64
                      ))



(defn bounce-player! [a-state ^js o power & [bounce-direction]]
  (assert (or
            (nil? bounce-direction)
            (#{:left :right :up :down} bounce-direction)))
  (let [{:keys [last-spd]} @a-state
        ^js player-layer (:tree/player-layer @a-state)
        ^js player-sp (:tree/player-sp @a-state)
        old-p-x (.-x player-layer)
        old-p-y (.-y player-sp)
        p-w (.-width player-sp)
        p-h (.-height player-sp)
        p-anchor (.-anchor player-sp)
        objdef (@a-object-defs (p/get o :--object-type))
        {:keys [anchor-x anchor-y hit-w hit-h]} objdef
        o-x (.-x o)
        o-y (.-y o)
        o-w hit-w
        o-h hit-h
        o-anchor-x anchor-x
        o-anchor-y anchor-y
        p-center-x (putil/calc-center old-p-x p-w (.-x p-anchor))
        p-center-y (putil/calc-center old-p-y p-h (.-y p-anchor))
        o-center-x (putil/calc-center o-x o-w o-anchor-x)
        o-center-y (putil/calc-center o-y o-h o-anchor-y)
        e-x (/ (+ p-center-x o-center-x) 2)
        e-y (/ (+ p-center-y o-center-y) 2)
        dist-x (- o-center-x p-center-x)
        dist-y (- o-center-y p-center-y)
        bounce-direction (or
                           bounce-direction
                           (if (< (Math/abs dist-y) (Math/abs dist-x))
                             ;; 横方向の方が遠い＝横に反動
                             (if (pos? dist-x)
                               :left ; pよりoが右なのでpは左に飛ぶ
                               :right) ; oよりpが右なのでpは右に飛ぶ
                             ;; 縦方向の方が遠い＝縦に反動
                             (if (pos? dist-y)
                               :up
                               :down)))
        delta-p-x (case bounce-direction
                    :left (- power)
                    :right power
                    (:up :down) 0)
        delta-p-y (case bounce-direction
                    :up (- power)
                    :down power
                    (:left :right) 0)
        new-p-x (+ old-p-x delta-p-x)
        new-p-y (+ old-p-y delta-p-y)
        old-spd-x (pos/->x last-spd)
        old-spd-y (pos/->y last-spd)
        new-spd-x (case bounce-direction
                    :left (- power)
                    :right power
                    (:up :down) old-spd-x)
        new-spd-y (case bounce-direction
                    :up (- power)
                    :down power
                    (:left :right) old-spd-y)
        ]
    (pos/set! last-spd new-spd-x new-spd-y)
    (p/set! player-layer :x new-p-x)
    (p/set! player-sp :y new-p-y)
    (emit-near-smoke! a-state e-x e-y)
    nil))

(defn vibrate-obj-sp! [^js o power]
  (let [^js sp (aget (.-children o) 0)
        r-power (inc (* 2 power))]
    (set! (.-x sp) (- (rand-int r-power) power))
    (set! (.-y sp) (- (rand-int r-power) power))
    nil))



(defn spawn! [a-state k x y & [far? back?]]
  (let [object-layer (if far?
                       (:tree/far-object-layer @a-state)
                       (:tree/near-object-layer @a-state))
        objdef (@a-object-defs k)
        _ (assert objdef)
        {:keys [anchor-x
                anchor-y
                scale
                hit-w
                hit-h
                tex-fn
                init-fn
                move-fn
                collide-fn
                ]} objdef
        ^js hit-info (pixi/Rectangle. anchor-x anchor-y hit-w hit-h)
        ^js obj (p/set! (pixi/Container.)
                        :x x
                        :y y
                        :--object-type k
                        :--hit-info hit-info)
        ^js tex (tex-fn)
        ;; tex-fnが直にdobjを返した場合はそのまま適用する
        ^js sp (if-not (instance? pixi/Texture tex)
                 tex
                 (putil/->sp tex))
        ;; TODO: dsをつける事を検討
        ]
    (when-let [^js a (.-anchor sp)]
      (p/set! a :x anchor-x :y anchor-y))
    (p/set! sp
            :scale/x scale
            :scale/y scale)
    (.addChild obj sp)
    (if back?
      (.addChildAt object-layer obj 0)
      (.addChild object-layer obj))
    (when init-fn
      (init-fn a-state obj x y))
    obj))








(def spawn-distance-threshold 1024)

(defn update-visited-distance! [a-state new-x delta-frames]
  (let [visited-distance-pos (:visited-distance-pos @a-state)
        checked-x (pos/->x visited-distance-pos)]
    (when (< checked-x new-x)
      (pos/set-x! visited-distance-pos new-x)
      (let [sight-x (+ new-x spawn-distance-threshold)
            a-object-map (:object-map @a-state)]
        (loop []
          (when-let [next-obj (first @a-object-map)]
            (let [{:keys [k x y]} next-obj]
              (when (< x sight-x)
                (spawn! a-state k x y false)
                (swap! a-object-map rest)
                (recur)))))))))

(defn move-object! [a-state ^js obj delta-frames]
  (let [object-type (p/get obj :--object-type)
        move-fn (:move-fn (@a-object-defs object-type))]
    (when move-fn
      (move-fn a-state obj delta-frames))))

;;; NB: 衝突によって消滅しないobjは次フレームでもまた衝突している率が高いので、
;;;     連続衝突を避ける処理を入れる事！
(defn collide-object! [a-state ^js obj]
  (let [object-type (p/get obj :--object-type)
        collide-fn (:collide-fn (@a-object-defs object-type))]
    (when (or
            (not (:wait-stop? @a-state))
            (= :lodge object-type))
      (when collide-fn
        (collide-fn a-state obj)))))



(defn process-layer! [a-state layer delta-frames p-x p-y]
  (m/doseq-array-backward [^js obj (.-children layer)]
    ;; TODO: 本当はp-x p-yは毎回取り直す必要があるが…
    (let [^js hit-info (p/get obj :--hit-info)
          too-far-distance 1024
          old-x (.-x obj)
          old-y (.-y obj)]
      (move-object! a-state obj delta-frames)
      (when-not (putil/destroyed? obj)
        (let [
              new-x (.-x obj)
              new-y (.-y obj)
              hit-w (.-width hit-info)
              hit-h (.-height hit-info)
              ]
          ;; TODO: ↓フレーム飛びによる貫通対策も必要！プレイヤーの前座標と現座標の線分、オブジェクトの前座標と現座標の線分、それぞれが一定以上長かったら衝突判定を複数回に分ける感じで
          (p/set! tmp-rect
                  :x (- new-x (* hit-w 0.5))
                  :y (- new-y (* hit-h 0.5))
                  :width hit-w
                  :height hit-h)
          ;; TODO: なぜかobjに下からさわった時の当たり判定がかなり甘い、原因を調べて直す事
          (when (.contains tmp-rect p-x p-y)
            (collide-object! a-state obj))
          ;; NB: y方向の消失判定は省略(スクロールゲーなのでなくても問題ない)
          (when (< (+ new-x too-far-distance) p-x)
            (putil/dea! obj)))))))


(defn- gain-score! [a-state score x y]
  (let [layer (:tree/scroll-near-effect-layer @a-state)
        text (str score)]
    (swap! a-state update :score + score)
    (status/update-status-label! a-state)
    (util/effect-score! layer text x y 120)
    nil))

(defn- emit-star! [a-state size x y]
  (util/effect-smoke! (:tree/scroll-near-effect-layer @a-state)
                      60
                      :quantity 8
                      :speed 4
                      :size size
                      :x x
                      :y y
                      :tex-fn dataurl/star))

(defn- emit-nova! [a-state x y]
  (let [
        ]
    (util/se! :se/paan-psg)
    (util/effect-nova! (:tree/scroll-near-effect-layer @a-state)
                       30
                       x
                       y
                       :start-size 32
                       :end-size 512
                       :start-alpha 1
                       :end-alpha 0
                       )
    (util/vibrate! 200)
    ))




(defobj :pudding
  :anchor-x 0.5
  :anchor-y 0.85
  :scale 8
  :hit-w 144
  :hit-h 160
  :tex-fn dataurl/pudding
  :move-fn nil
  :collide-fn (fn [a-state ^js o]
                (bounce-se-puyo!)
                (bounce-player! a-state o 4)
                (util/vibrate! 100)
                (tween/vibrate! (aget (.-children o) 0) 30 8)
                ;(p/set! (:tree/ui-action-button @a-state) :visible true)
                ;(putil/dea! o)
                (when-not (p/get o :--gained?)
                  (p/set! o :--gained? true)
                  (util/se! :se/coin-psg)
                  (gain-score! a-state 100 (.-x o) (- (.-y o) 32)))))


(defobj :lodge
  :anchor-x 0.5
  :anchor-y 0.5
  :scale 0.25
  :hit-w 256
  :hit-h 1024
  :tex-fn #(putil/->tex :lodge)
  :move-fn (fn [a-state ^js o delta-frames]
             ;; TODO: 本当はカメラのフォーカス位置も計算に含める必要あり(現在はフォーカス位置はスタート地点以外固定なので省略している)
             (let [^js player-layer (:tree/player-layer @a-state)
                   p-x (.-x player-layer)
                   x (+ (* 0.25 status/lodge-pos) (* 0.75 p-x))]
               (p/set! o :x x)))
  :collide-fn (fn [a-state o]
                (let [last-spd (:last-spd @a-state)]
                  (pos/set-x! last-spd 0))
                (if-not (:wait-stop? @a-state)
                  (swap! a-state assoc :wait-stop? true)
                  (let [^js player-sp (:tree/player-sp @a-state)]
                    (when (zero? (.-y player-sp))
                      (ending/emit! a-state o status/lodge-pos))))))

(defn- std-obj-move! [a-state ^js o delta-frames]
  (let [mode (p/get o :--mode)
        std-x (p/get o :--std-x)
        std-y (p/get o :--std-y)
        amp-x (p/get o :--amp-x)
        amp-y (p/get o :--amp-y)
        span (p/get o :--span)
        old-elapsed (p/get o :--elapsed)
        _ (assert (and mode std-x std-y amp-x amp-y span old-elapsed))
        tmp-elapsed (+ old-elapsed delta-frames)
        new-elapsed (mod tmp-elapsed span)
        action-span (p/get o :--action-span)
        action-fn (p/get o :--action-fn)
        progress (/ new-elapsed span)
        x (case mode
            :random (+ (.-x o)
                       (* delta-frames (- (rand-int (inc (* 2 amp-x))) amp-x)))
            :proceed (+ (.-x o) (* delta-frames amp-x))
            :sin (+ std-x (* amp-x (Math/sin (* 2 Math/PI progress))))
            :sincos (+ std-x (* amp-x (Math/sin (* 2 Math/PI progress)))))
        y (case mode
            :random (+ (.-y o)
                       (* delta-frames (- (rand-int (inc (* 2 amp-y))) amp-y)))
            :proceed (+ (.-y o) (* delta-frames amp-y))
            :sin (+ std-y (* amp-y (Math/sin (* 2 Math/PI progress))))
            :sincos (+ std-y (* amp-y (Math/cos (* 2 Math/PI progress)))))]
    (p/set! o :x x :y y :--elapsed new-elapsed)
    (when (and action-span action-fn)
      (when-not (= (quot old-elapsed action-span)
                   (quot tmp-elapsed action-span))
        (action-fn a-state o)))))

(defn- shoot-look-ahead-bullet! [a-state x y]
  (let [
        ;; TODO: 難しいのであとで
        ;^js obj (spawn! a-state :look-ahead-bullet x y nil true)
        ^js obj (spawn! a-state :proceed-bullet x y nil true)
        ]
    ))

(defobj :cherry
  :anchor-x 0.5
  :anchor-y 1.5
  :scale 4
  :hit-w 128
  :hit-h 128
  :tex-fn dataurl/cherry
  :init-fn (fn [a-state ^js o x y & args]
             (p/set! o
                     :--std-x x
                     :--std-y y
                     :--amp-x 0
                     :--amp-y 64
                     :--span 120
                     :--elapsed 0
                     :--mode :sin
                     :--action-fn nil
                     )
             )
  :move-fn std-obj-move!
  :collide-fn (fn [a-state ^js o]
                (let [x (.-x o)
                      y (- (.-y o) 32)]
                  (util/se! :se/coin-psg)
                  (emit-star! a-state 32 x y)
                  (gain-score! a-state 500 x y)
                  (putil/dea! o))))

(def cube-scale 2)
(def cube-anchor-x 0.5)
(def cube-anchor-y 1.0)
(def cube-hit-w 64)
(def cube-hit-h 128)

(defn- cube-collide! [a-state ^js o]
  (let [x (.-x o)
        y (- (.-y o) 32)]
    (bounce-player! a-state o 8 :left)
    (emit-nova! a-state x y)
    (putil/dea! o)))

(defobj :enemy-w
  :anchor-x cube-anchor-x
  :anchor-y cube-anchor-y
  :scale cube-scale
  :hit-w cube-hit-w
  :hit-h cube-hit-h
  :tex-fn util/make-cube-w-sp
  :init-fn (fn [a-state ^js o x y & args]
             (p/set! o
                     :--std-x x
                     :--std-y y
                     :--amp-x 128
                     :--amp-y 0
                     :--span 240
                     :--elapsed 0
                     :--mode :sin
                     :--action-span 60
                     :--action-fn (fn [a-state ^js o]
                                    (shoot-look-ahead-bullet! a-state
                                                              (.-x o)
                                                              (- (.-y o) 32)))))
  :move-fn (fn [a-state ^js o delta-frames]
             ;(vibrate-obj-sp! o 4)
             (std-obj-move! a-state o delta-frames))
  :collide-fn cube-collide!)

(defobj :enemy-b
  :anchor-x cube-anchor-x
  :anchor-y cube-anchor-y
  :scale cube-scale
  :hit-w cube-hit-w
  :hit-h cube-hit-h
  :tex-fn util/make-cube-b-sp
  :init-fn (fn [a-state ^js o x y & args]
             (p/set! o
                     :--std-x x
                     :--std-y y
                     :--amp-x 128
                     :--amp-y 0
                     :--span 240
                     :--elapsed 0
                     :--mode :sin
                     :--action-fn nil))
  :move-fn (fn [a-state ^js o delta-frames]
             (vibrate-obj-sp! o 4)
             (std-obj-move! a-state o delta-frames))
  :collide-fn cube-collide!)

(defobj :enemy-c
  :anchor-x cube-anchor-x
  :anchor-y cube-anchor-y
  :scale cube-scale
  :hit-w cube-hit-w
  :hit-h cube-hit-h
  :tex-fn util/make-cube-c-sp
  :init-fn (fn [a-state ^js o x y & args]
             (p/set! o
                     :--std-x x
                     :--std-y y
                     :--amp-x 128
                     :--amp-y 128
                     :--span 90
                     :--elapsed 0
                     :--mode :sincos
                     :--action-span 90
                     :--action-fn (fn [a-state ^js o]
                                    (let [x (.-x o)
                                          y (- (.-y o) 32)
                                          angle (rand (* 2 Math/PI))
                                          ax (* 8 (Math/sin angle))
                                          ay (* 8 (Math/cos angle))
                                          ^js b (spawn! a-state :proceed-bullet x y nil true)]
                                      (p/set! b
                                              :--amp-x ax
                                              :--amp-y ay)))))
  :move-fn (fn [a-state ^js o delta-frames]
             ;(vibrate-obj-sp! o 4)
             (std-obj-move! a-state o delta-frames))
  :collide-fn cube-collide!)

(defobj :proceed-bullet
  :anchor-x 0.5
  :anchor-y 1.5
  :scale 2
  :hit-w 64
  :hit-h 128
  :tex-fn putil/sp16x16
  :init-fn (fn [a-state ^js o x y & args]
             (p/set! o
                     :--std-x x
                     :--std-y y
                     :--amp-x 0
                     :--amp-y 8
                     :--span 120
                     :--elapsed 0
                     :--mode :proceed
                     :--action-fn nil))
  :move-fn (fn [a-state ^js o delta-frames]
             (vibrate-obj-sp! o 4)
             (std-obj-move! a-state o delta-frames)
             (let [y (.-y o)]
               (when (or
                       ;; TODO: 弾丸に限り、xが右に行き過ぎても消したい。しかし判定が面倒…
                       (< y -1024)
                       (< 512 y))
                 (putil/dea! o))))
  :collide-fn cube-collide!)



;;;     - 配置可能なdefobjをどんどん追加しましょう。内容と動きも決めましょう
;;;         - bullet (敵が射出するもの) - どの敵が射出するのも、速度遅めの先読み弾。当たると爆発(:nova)、常に左にノックバックされる
;;;         - :banana - 動かず配置、取ると攻撃可能、 :se/grow-psg を鳴らしてボタンをオンに
;;;         - :orange - 動かず配置、取ると攻撃可能その2。 :banana と両方あればより強い
;;;         - bullet (主人公が射出するもの) - 敵に当たると倒して得点
;;;         - dataurl/leaf - 取ると50点？斜めに流れる
;;;         - :korokke128x96x14 - ？？？
;;;         - :dishkorokke112x68x14 - ？？？
;;;         - :driftcat2020 (白) - 出番なしの予定
;;;         - :driftcat2020 (黒) - 出番なし
;;;         - :driftcat2020 (虎) - 出番なし


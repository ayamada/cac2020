(ns cac2020.game
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
            [cac2020.game.ending :as ending]
            [cac2020.pixi.util :as putil]
            [cac2020.tween :as tween]
            ))


;;; 「おうちかえる！」という台詞付きのサムネを用意しましょう



;;; - タイムの管理と表示
;;;     - delta-framesによる判定は多分よくない(一時停止している時にタイムも止まる)、リアル時間ベースのタイム管理の方が多分よい
;;;     - va5を使うか？悩ましいところ…
;;;         - これが一番楽だと思う。タッチ起動はスタートボタンで満たしている筈
;;;     - タイムを左上に表示させる
;;;     - タイムが1秒増える毎にlabelを更新する(毎フレーム更新はしない)



;;; - トマトマ同様、TRAVELに重ねるようにして進行度バーを表示する
;;;     - とりあえずゴール距離を数値で表示するだけでよいか？
;;;     - ゴール距離は (Math/round (/ (- goal-pos 400) 100)) で求められる



;;; - 進行度に応じてオブジェクトを適切に配置していくシステムを用意しましょう
;;;     - プリンをランダム？固定位置？配置してみましょう

;;; - defobj回りを別namespaceに隔離。 cac2020.game.object あたりで



;;; NB: goal-posに実際のlodgeがあるが、ゴール判定はその-6m地点にあり、そこからエンディング内移動で右に+2m進む。なのでgoal-posは+400しておくとよい
(def goal-pos 10400)



;;; 画面上のこの座標が、プレイヤーの論理座標原点(0,0)になる
;;; (ただしyは下向きのままなので注意)
(def player-screen-left 128)
(def player-screen-bottom (- 800 32))





;;; TODO: singletonなのはよくない。lifecycle管理側からstateを渡せるようにするか、lifecycleそのものにatomを含めるようにする方向でなんとかするのを検討する事
(def a-state (atom {}))


(defn- preload-audio! []
  (util/load-audio! nil
                    ;; 使用される可能性の早い順に並べる事
                    :se/caret-psg
                    :se/submit-psg
                    :se/puyo-psg
                    :bgm/bsbs__LE661501
                    :se/launch-psg
                    :se/lvup-midi
                    ;; TODO: 以下は使うかどうか分からない
                    ;:se/yarare-psg
                    ))

(defn- preload! [app cont]
  (putil/load-textures! cont
                        ;; 以下は利用中
                        :ground
                        :lodge
                        :flame1 ; endingで使う
                        ;; 以下は使う予定だがまだ使ってない
                        :nova
                        :orange
                        :banana
                        :cube3
                        ;; 以下は使うかどうか分からない
                        ;:driftcat2020
                        ;:korokke128x96x14
                        ;:dishkorokke112x68x14
                        ))



(defn- emit-ranking-atsumaru! [score]
  (let [board-id 1
        ^js atsumaru (aget js/window "RPGAtsumaru")]
    (.then (.. atsumaru -experimental -scoreboards board-id score)
           (fn [& _]
             (.then (.display (.. atsumaru -experimental -scoreboards) board-id)
                    (fn [e]
                      (js/console.log e)
                      nil)))
           (fn [e]
             (js/console.log e)
             nil))))


(defn- emit-ranking! [_]
  (util/se! :se/submit-psg)
  (let [score (or (:score @a-state) 0)]
    (cond
      (aget js/window "RPGAtsumaru") (emit-ranking-atsumaru! score)
      ;; TODO: 他のランキングシステムへの対応
      :else (when ^boolean js/goog.DEBUG
              (prn 'emit-ranking! score)))
    nil))


(defn- can-use-ranking? []
  (boolean (or
             (aget js/window "RPGAtsumaru")
             ;; TODO: 他のランキングシステムへの対応
             js/goog.DEBUG)))


(defn- make-gameclear-layer [s-w s-h]
  [:tree/gameclear-layer
   {:visible false}
   (p/set! (util/make-label "Congratulations!"
                            :font-family "serif"
                            :font-size 80
                            :fill #js [0xFFFFFF 0x7F7FFF]
                            )
           :anchor/x 0.5
           :anchor/y 0.5
           :x (* s-w 0.5)
           :y (* s-h 0.5)
           :name (name :tree/gameover-caption))
   (p/set! (util/make-button "RANKING" emit-ranking! :padding 32)
           :x (* s-w 0.8)
           :y (* s-h 0.1)
           :visible (can-use-ranking?)
           :name (name :tree/ranking-button))
   ])



(defn- update-status-label! [& [status-label]]
  (let [
        status-label (or status-label (:tree/status-label @a-state))
        ;{:keys [space-spd-x space-spd-y space-spd-z space-spd-yaw space-spd-pitch]} @a-state
        score (or (:score @a-state) 0)
        ^js player-layer (:tree/player-layer @a-state)
        travel-point (or
                       (when player-layer
                         (.-x player-layer))
                       0)
        text (str ""
                  "TRAVEL: " (int (/ travel-point 100)) "m"
                  "\n"
                  " SCORE: " score
                  )
        ]
    (p/set! status-label :text text)))


;;; TODO: defobjの定義が多くなるようなら名前空間を分けたい
;;; TODO: 本当はマクロ化した方がよい
(defonce a-object-defs (atom {}))
(defn- defobj [k & args]
  (swap! a-object-defs assoc k (util/args->map args)))




(def bounce-se-puyo!
  (util/make-play-se-periodically 0.2 :se/puyo-psg :volume 1))



(defn- emit-near-smoke! [x y]
  (util/effect-smoke! (:tree/scroll-near-effect-layer @a-state)
                      15
                      :x x
                      :y y
                      :size 64
                      ))



(defn- bounce-player! [^js o power & [bounce-direction]]
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
        objdef (@a-object-defs (p/get o :-object-type))
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
    (emit-near-smoke! e-x e-y)
    nil))

(defn- vibrate-obj-sp! [^js o power]
  (let [^js sp (aget (.-children o) 0)
        r-power (inc (* 2 power))]
    (set! (.-x sp) (- (rand-int r-power) power))
    (set! (.-y sp) (- (rand-int r-power) power))
    nil))


(defobj :pudding
  :anchor-x 0.5
  :anchor-y 0.95
  :scale 8
  :hit-w 128
  :hit-h 128
  :tex-fn dataurl/pudding
  :move-fn (fn [^js o delta-frames]
             ;(vibrate-obj-sp! o 4)
             )
  :collide-fn (fn [^js o]
                (bounce-se-puyo!)
                (bounce-player! o 4)
                (util/vibrate! 100)
                (tween/vibrate! (aget (.-children o) 0) 30 8)
                ;(p/set! (:tree/ui-action-button @a-state) :visible true)
                ;(putil/dea! o)
                )
  )






(defobj :lodge
  :anchor-x 0.5
  :anchor-y 0.5
  :scale 0.25
  :hit-w 256
  :hit-h 1024
  :tex-fn #(putil/->tex :lodge)
  :move-fn (fn [^js o delta-frames]
             (let [^js player-layer (:tree/player-layer @a-state)
                   p-x (.-x player-layer)
                   ;(- goal-pos p-x)
                   ;x (+ p-x (* 0.5 (- goal-pos p-x)))
                   x (+ (* 0.25 goal-pos) (* 0.75 p-x))
                   ]
               (p/set! o :x x)))
  :collide-fn (fn [o]
                (let [last-spd (:last-spd @a-state)]
                  (pos/set-x! last-spd 0))
                (if-not (:wait-stop? @a-state)
                  (swap! a-state assoc :wait-stop? true)
                  (let [^js player-sp (:tree/player-sp @a-state)]
                    (when (zero? (.-y player-sp))
                      (ending/emit! a-state o goal-pos update-status-label!)))))
  )




(defn- spawn-object! [k x y far?]
  (let [object-layer (if far?
                       (:tree/far-object-layer @a-state)
                       (:tree/near-object-layer @a-state))
        {:keys [anchor-x
                anchor-y
                scale
                hit-w
                hit-h
                tex-fn
                move-fn
                collide-fn
                ]} (@a-object-defs k)
        _ (assert (and anchor-x anchor-y scale hit-w hit-h tex-fn))
        ^js hit-info (pixi/Rectangle. anchor-x anchor-y hit-w hit-h)
        ^js obj (p/set! (pixi/Container.)
                        :x x
                        :y y
                        :-object-type k
                        :-hit-info hit-info)
        ^js sp (p/set! (putil/->sp (tex-fn))
                       :anchor/x anchor-x
                       :anchor/y anchor-y
                       :scale/x scale
                       :scale/y scale)
        ;; TODO: dsをつける事を検討
        ]
    (.addChild obj sp)
    (.addChild object-layer obj)
    obj))

(defn- emit-start! [b]
  (util/se! :se/submit-psg)
  (util/bgm! :bgm/bsbs__LE661501)
  (let [
        player-layer (:tree/player-layer @a-state)
        p-walk (tween/pp -256 0)
        ]
    (p/set! (:tree/title-layer @a-state) :visible false)
    (swap! a-state
           assoc
           :mode :game-op
           :space-spd-x 0
           :space-spd-y 0
           :space-spd-z -0.0002
           :space-spd-yaw 0.2
           :space-spd-pitch 0
           )
    (spawn-object! :pudding 1000 0 false)
    ;; TODO: ここは spawn-new-object! に統合する事
    (spawn-object! :lodge
                   goal-pos
                   -176
                   true)
    (p/set! player-layer :visible true)
    (tween/register! player-layer
                     30
                     (fn [^js player-layer progress]
                       (p/set! player-layer :x (tween/ap p-walk progress)))
                     (fn [^js player-layer]
                       (swap! a-state assoc :mode :game)
                       (p/set! (:tree/ui-action-button @a-state)
                               :visible false)
                       (p/set! (:tree/ui-layer @a-state)
                               :visible true)
                       ;; TODO
                       ))))

(defn- make-title-layer [s-w s-h]
  [:tree/title-layer
   (p/set! (util/make-label (str "version:" util/VERSION)
                            :font-family "serif"
                            :font-size 24
                            )
           :anchor/x 0
           :anchor/y 1
           :x 8
           :y (- s-h 8)
           :name (name :tree/version-label))
   (p/set! (util/make-label "宇宙エルフの帰宅"
                            :font-size 96
                            )
           :anchor/x 0.5
           :anchor/y 0.5
           :x (* s-w 0.5)
           :y (* s-h 0.2)
           :name (name :tree/title-caption))
   (p/set! (putil/->sp (dataurl/pudding))
           :anchor/x 0.5
           :anchor/y 0.5
           :scale/x 8
           :scale/y 8
           :x (* s-w 0.5)
           :y (* s-h 0.4)
           :name (name :tree/title-image))
   (p/set! (util/make-button "START"
                             emit-start!
                             :padding 64
                             )
           :x (* s-w 0.5)
           :y (* s-h 0.7)
           :name (name :tree/start-button))
   (p/set! (util/make-label "操作方法：マウス/タッチのみ"
                            :font-size 48
                            )
           :scale/x 0.5
           :scale/y 0.5
           :anchor/x 0.5
           :anchor/y 0.5
           :x (* s-w 0.5)
           :y (* s-h 0.9)
           :name (name :tree/play-instruction))
   ])






(defn- make-ui-layer [s-w s-h]
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
    (update-status-label! status-label)
    [:tree/ui-layer
     {:visible false}
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





(defn- set-scroll-pos! [^js sp x y]
  (let [^js tex (.-texture sp)
        ^js frame (.-frame tex)]
    (set! (.-x frame) x)
    (set! (.-y frame) y)
    (.updateUvs tex)
    nil))

(defn- move-ground! [^js sp x]
  (let [^js tex (.-texture sp)
        ^js frame (.-frame tex)
        loop-w (p/get sp :-loop-w)]
    (set-scroll-pos! sp (mod x loop-w) 0)))

(defn- scroll-all-ground! [^js layer delta-x]
  (let [old-offset-x (p/get layer :-offset-x)
        new-offset-x (+ old-offset-x delta-x)]
    (p/set! layer :-offset-x new-offset-x)
    (m/doseq-array-backward [child (.-children layer)]
      (move-ground! child new-offset-x))))

(defn- move-all-ground! [^js layer x]
  (let [offset-x (p/get layer :-offset-x)]
    (m/doseq-array-backward [child (.-children layer)]
      (move-ground! child (+ offset-x x)))))

(defn- make-ground [k s-w s-h adjust-h scale tint]
  (let [^js tex (.clone (putil/->tex :ground))
        ^js base-tex (.-baseTexture tex)
        orig-w 1024
        orig-h 184
        ;; MIRRORED_REPEATにすると折り返し部分が模様に見えてしまうので、
        ;; 通常のREPEATにする事にした
        ;wrap-mode pixi/WRAP_MODES.MIRRORED_REPEAT
        wrap-mode pixi/WRAP_MODES.REPEAT
        _ (p/set! base-tex :wrap-mode wrap-mode)
        _ (p/set! tex
                  :orig/width (/ orig-w scale)
                  :orig/height (/ orig-h scale)
                  )
        x (/ s-w 2)
        y (- s-h 184 adjust-h)
        w orig-w
        h orig-h
        ;h (* orig-h scale)
        sp (putil/->sp tex)]
    (.updateUvs tex)
    (p/set! sp
            :anchor/x 0.5
            :anchor/y 0
            :x x
            :y y
            :width w
            :height h
            :tint tint
            :name (name k)
            ;; pixi/WRAP_MODES.MIRRORED_REPEAT の場合は
            ;; 元の2倍の長さ指定が必要になる
            :-loop-w (* orig-w 2)
            :-loop-h (* orig-h 2)
            )
    sp))

(defn- make-background-space-layer [s-w s-h]
  (let [margin 64
        s-left (- 0 margin)
        s-right (+ s-w margin)
        s-top (- 0 margin)
        s-bottom (+ s-h margin)
        z-nearest -0.1
        z-near 0.1
        z-far 1
        z-farthest 1.2
        star-base-scale 0.2
        ;star-base-scale 0.1
        sightbox (space/make-sightbox s-left s-right s-top s-bottom
                                      z-nearest z-near z-far z-farthest
                                      star-base-scale)
        quantity 256
        ;quantity 1024
        ;quantity 32
        params {:star-quantity quantity
                :dont-sort? false
                :colorful-ratio 0.6
                ;:star-textures [...]
                }
        ]
    (p/set! (space/make sightbox params)
            :name (name :tree/background-space-layer))))

(defn- make-touchpanel [s-w s-h]
  (let [
        ;; TODO
        sp (p/set! (putil/sp16x16)
                   :width s-w
                   :height s-h
                   :alpha 0
                   :interactive true
                   :name (name :tree/touchpanel))
        h (fn [^js e]
            (when e
              (when (= :game (:mode @a-state))
                (when-let [^js data (.-data e)]
                  (let [b (.-button data)]
                  (when (or (nil? b) (zero? b))
                    (when-let [^js gpos (.-global data)]
                      (swap! a-state assoc :last-pressed? true)
                      (pos/set! (:last-touch-pos @a-state)
                                (.-x gpos)
                                (.-y gpos)))))))))
        ]
    (.on sp "mousedown" h)
    (.on sp "touchstart" h)
    ;(.on sp "mousemove" h2)
    ;(.on sp "touchmove" h2)
    sp))

(defn- make-player-layer []
  [:tree/player-layer
   {:visible false}
   (p/set! (putil/->sp (dataurl/sphere))
           :anchor/x 0.5
           :anchor/y 0.5
           :scale/x 6
           :scale/y 1
           :alpha 0.5
           :tint 0x000000
           :name (name :tree/player-ds))
   [:tree/player-effect-layer]
   (p/set! (putil/->sp (dataurl/p8elf))
           :anchor/x 0.5
           :anchor/y 0.95
           :scale/x -8
           :scale/y 8
           :name (name :tree/player-sp))
   ])

(defn- make-tree [s-w s-h]
  [:tree/root
   (make-touchpanel s-w s-h)
   (p/set! (putil/sp16x16)
           :name (name :tree/background-black)
           :tint 0x00001F
           :x -64
           :y -64
           :width (+ s-w 128)
           :height (+ s-h 128)
           )
   (make-background-space-layer s-w s-h)
   [:tree/back-layer
    ]
   (let [ground-base 96]
     [:tree/ground-layer
      {:-offset-x 0}
      (make-ground :tree/ground-far s-w s-h (+ ground-base 64 32) 0.25 0x7F7F7F)
      (make-ground :tree/ground-mid s-w s-h (+ ground-base 64) 0.5 0xBFBFBF)
      (make-ground :tree/ground-near s-w s-h (+ ground-base) 1 0xEFEFEF)
      (make-ground :tree/ground-nearest s-w s-h (+ ground-base -96) 2 0xFFFFFF)
      ])
   [:tree/far-effect-layer]
   [:tree/scroll-base-layer
    {:x player-screen-left
     :y player-screen-bottom}
    [:tree/scroll-layer
     [:tree/far-object-layer]
     [:tree/scroll-far-effect-layer]
     [:tree/ds-layer]
     (make-player-layer)
     [:tree/near-object-layer]
     [:tree/scroll-near-effect-layer]
     ]]
   (make-gameclear-layer s-w s-h)
   (make-ui-layer s-w s-h)
   (make-title-layer s-w s-h)
   [:tree/near-effect-layer]])

(defn- create! [app]
    (swap! a-state
           assoc
           :last-pressed? false
           :last-touch-pos (pos/make 0 0)
           :last-spd (pos/make 0 0)
           :space-spd-x 1
           :space-spd-y -2
           :space-spd-z 0.01
           :space-spd-yaw 0
           :space-spd-pitch 2
           )
  (let [^js renderer (:renderer app)
        s-w (.-width renderer)
        s-h (.-height renderer)
        ^js stage (putil/build-container-tree (make-tree s-w s-h))
        ]
    (preload-audio!)
    (util/bgm! nil)
    (swap! a-state
           assoc
           :mode :title
           :score 0
           )
    (putil/index-container-tree! stage a-state :tree/*)
    stage))


(defn- destroy! [app]
  (when-let [layer (:tree/background-space-layer @a-state)]
    (space/destroy! layer))
  (putil/dea! (:tree/root @a-state))
  ;; TODO: a-state内の破棄が必要なものが残ってないかないか再確認
  (reset! a-state {}))



(defonce ^js tmp-rect (pixi/Rectangle. 0 0 0 0))

(defn- spawn-new-object! [old-x new-x delta-frames]
  ;; 距離に応じて、生成するものの有無と種別と頻度を変更する必要がある
  ;; (一応delta-framesは取ってるけど、停止中にもspawnするケースは多分なさそう)
  (let [
        ]
    ;(spawn-object! k x y false)
    ))

(defn- move-object! [^js obj delta-frames]
  (let [object-type (p/get obj :-object-type)
        move-fn (:move-fn (@a-object-defs object-type))]
    (when move-fn
      (move-fn obj delta-frames))))

;;; NB: 衝突によって消滅しないobjは次フレームでもまた衝突している率が高いので、
;;;     連続衝突を避ける処理を入れる事！
(defn- collide-object! [^js obj]
  (let [object-type (p/get obj :-object-type)
        collide-fn (:collide-fn (@a-object-defs object-type))]
    (when collide-fn
      (collide-fn obj))))



(defn- process-object-layer! [layer delta-frames p-x p-y]
  (m/doseq-array-backward [^js obj (.-children layer)]
    (let [^js hit-info (p/get obj :-hit-info)
          too-far-distance 2048
          old-x (.-x obj)
          old-y (.-y obj)
          _ (move-object! obj delta-frames)
          new-x (.-x obj)
          new-y (.-y obj)
          hit-w (.-width hit-info)
          hit-h (.-height hit-info)
          ]
      ;; TODO: ↓フレーム飛びによる貫通対策も必要！プレイヤーの前座標と現座標の線分、オブジェクトの前座標と現座標の線分、それぞれが一定以上長かったら衝突判定を複数回に分ける感じで
      (p/set! tmp-rect
              :x (- new-x (* hit-w (.-x hit-info)))
              :y (- new-y (* hit-h (.-y hit-info)))
              :width hit-w
              :height hit-h)
      (when (.contains tmp-rect p-x p-y)
        (collide-object! obj))
      (when (< (+ (.-x tmp-rect) too-far-distance) p-x)
        (putil/dea! obj)))))


(defn- tick-player-ground-objs! [delta-frames]
  (let [{:keys [last-pressed? last-spd]} @a-state
        ^js player-layer (:tree/player-layer @a-state)
        ^js player-sp (:tree/player-sp @a-state)
        ;; TODO: もう少し汎用化とまとめを行う
        pressed? (and
                   last-pressed?
                   (pointer/pressed?)
                   (not (:wait-stop? @a-state)))
        max-spd-x 8
        min-spd-x (- max-spd-x)
        max-spd-y 8
        min-spd-y (- max-spd-y)
        base-acc-x 0.1
        base-acc-y 0.1
        top-p-y -1024
        initial-jump-spd-y -4
        old-p-x (.-x player-layer)
        old-p-y (.-y player-sp)
        old-spd-x (pos/->x last-spd)
        ;; pressed?時は減衰なし、非pressed?時は0に近付く方向に減衰
        delta-spd-x (* delta-frames (cond
                                      pressed? base-acc-x
                                      (pos? old-spd-x) (- base-acc-x)
                                      (neg? old-spd-x) base-acc-x
                                      :else 0))
        new-spd-x (max min-spd-x (min (+ old-spd-x delta-spd-x) max-spd-x))
        new-spd-x (cond
                    pressed? new-spd-x
                    (pos? old-spd-x) (max 0 new-spd-x)
                    (neg? old-spd-x) (min 0 new-spd-x)
                    :else new-spd-x)
        delta-p-x (* new-spd-x delta-frames)
        new-p-x (+ old-p-x delta-p-x)
        trigger-jump? (and
                        pressed?
                        (zero? old-p-y)
                        (not (zero? new-spd-x)))
        old-spd-y (pos/->y last-spd)
        new-spd-y (if trigger-jump?
                    initial-jump-spd-y
                    (+ old-spd-y (* base-acc-y delta-frames)))
        new-spd-y (max min-spd-y (min new-spd-y max-spd-y))
        delta-p-y (* new-spd-y delta-frames)
        new-p-y (max top-p-y (min (+ old-p-y delta-p-y) 0))
        just-landed? (and
                       (not (zero? old-p-y))
                       (zero? new-p-y))
        ;; 画面上でのプレイヤー位置(カメラのフォーカス位置)を決める
        ;; NB: 現状ではこれを動かすと、 :tree/far-object-layer 内の
        ;;     オブジェクトも移動してしまう問題がある、注意
        scroll-adjust-x (max 0 (min player-screen-left new-p-x))
        ]
    (when (and last-pressed? (not pressed?))
      (swap! a-state assoc :last-pressed? false))
    (pos/set! last-spd new-spd-x new-spd-y)
    (when-not (zero? delta-p-x)
      (p/set! player-layer :x new-p-x))
    (when-let [layer (:tree/ground-layer @a-state)]
      (move-all-ground! layer (- new-p-x scroll-adjust-x)))
    (when-not (= (int (/ old-p-x 100))
                 (int (/ new-p-x 100)))
      (update-status-label!))
    (when-let [layer (:tree/scroll-layer @a-state)]
      (p/set! layer :x (- scroll-adjust-x new-p-x)))
    (p/set! player-layer :x new-p-x)
    (p/set! player-sp :y new-p-y)
    ;(when just-landed?
    ;  (util/se! :se/landing)
    ;  (emit-near-smoke! new-p-x new-p-y))
    (when trigger-jump?
      (emit-near-smoke! old-p-x old-p-y)
      (util/vibrate! 50)
      (util/se! :se/puyo-psg :volume 0.5))
    (process-object-layer! (:tree/near-object-layer @a-state)
                           delta-frames new-p-x new-p-y)
    (process-object-layer! (:tree/far-object-layer @a-state)
                           delta-frames new-p-x new-p-y)
    ;; 新オブジェクトの生成(必要なら)
    (spawn-new-object! old-p-x new-p-x delta-frames)
    ;; TODO: 弾丸の処理？
    ))

(defn- tick-space! [layer delta-frames]
  (let [{:keys [space-spd-x space-spd-y space-spd-z
                space-spd-yaw space-spd-pitch]} @a-state
        ;; この移動量はカメラ側のもの。風景側は逆方向に動く事になる
        move-x (* delta-frames space-spd-x)
        move-y (* delta-frames space-spd-y)
        move-z (* delta-frames space-spd-z)
        yaw (* delta-frames space-spd-yaw)
        pitch (* delta-frames space-spd-pitch)
        ]
    (space/update! layer move-x move-y move-z yaw pitch)))

(defn- tick! [app delta-frames]
  (let [
        ]
    (when-let [layer (:tree/background-space-layer @a-state)]
      (tick-space! layer delta-frames))
    (case (:mode @a-state)
      :title (when-let [layer (:tree/ground-layer @a-state)]
               (scroll-all-ground! layer (* delta-frames 4)))
      :game (tick-player-ground-objs! delta-frames)
      nil)
    ;; TODO
    ))



(def lifecycle
  {:preload preload!
   :create create!
   :tick tick!
   :destroy destroy!})



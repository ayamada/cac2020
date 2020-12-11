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
            [cac2020.pixi.util :as putil]
            [cac2020.tween :as tween]
            [cac2020.game.status :as status]
            [cac2020.game.object :as object]
            [cac2020.game.ending :as ending]
            ))

;;; - git pushして記事の確認を行いましょう
;;; - バージョンを上げる等のリリース作業をしましょう
;;; - アツマールにリリース前最終チェックを行い公開設定をしましょう
;;; - qiitaで記事urlを設定しましょう

;;; TODO
;;; - cac2020.game.object/map-bg を完成させましょう
;;;     - defobj定義の完遂、詳細はcac2020.game.objectにて
;;;     - ゲームのメンテ自体は記事の公開後に続けても問題ないので…





;;; 画面上のこの座標が、プレイヤーの論理座標原点(0,0)になる
;;; (ただしyは下向きのままなので注意)
(def player-screen-left 128)
(def player-screen-bottom (- 800 32))





;;; TODO: singletonなのはよくない。lifecycle管理側からstateを渡せるようにするか、lifecycleそのものにatomを含めるようにする方向でなんとかするのを検討する事
(def a-state (atom {}))


(defn- preload-audio! []
  (util/load-audio! nil
                    ;; 使用される可能性の早い順に並べる事
                    :se/submit-psg
                    :se/caret-psg
                    :se/puyo-psg
                    :se/coin-psg
                    :bgm/bsbs__LE15000
                    :se/paan-psg
                    :se/grow-psg
                    :se/launch-psg ; endingで使用
                    :se/lvup-midi ; endingで使用
                    ;; TODO: 以下は使うかどうか分からない
                    ;:se/yarare-psg
                    ))

(defn- preload! [app cont]
  (putil/load-textures! cont
                        ;; 以下は利用中
                        :ground
                        :lodge
                        :flame1 ; endingで使用
                        :cube3
                        :nova
                        ;; 以下は使う予定だがまだ使ってない
                        :orange
                        :banana
                        ;; 以下は使うかどうか分からない
                        ;:driftcat2020
                        ;:korokke128x96x14
                        ;:dishkorokke112x68x14
                        ))




(defn- emit-ranking! [_]
  (util/se! :se/submit-psg)
  (let [score (or (:score @a-state) 0)]
    (cond
      (aget js/window "RPGAtsumaru") (util/invoke-atsumaru-ranking! 1 score)
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
   (p/set! (util/make-button "RANKING"
                             emit-ranking!
                             :padding 32
                             :anchor-x 1
                             :anchor-y 0
                             )
           :x (- 960 32)
           :y (+ 16)
           :visible (can-use-ranking?)
           :name (name :tree/ranking-button))
   ])





(defn- emit-start! [b]
  (util/se! :se/submit-psg)
  (util/bgm! :bgm/bsbs__LE15000)
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
           :start-msec (util/now-msec)
           :elapsed-sec 0
           :visited-distance-pos (pos/make 0 0)
           :object-map (object/make-map)
           )
    ;; NB: これはかなり早目にspawnしておく必要がある為、最初からspawnしておく
    (object/spawn! a-state :lodge status/lodge-pos -156 true)
    (p/set! player-layer :visible true)
    (status/update-status-label! a-state)
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
        loop-w (p/get sp :--loop-w)]
    (set-scroll-pos! sp (mod x loop-w) 0)))

(defn- scroll-all-ground! [^js layer delta-x]
  (let [old-offset-x (p/get layer :--offset-x)
        new-offset-x (+ old-offset-x delta-x)]
    (p/set! layer :--offset-x new-offset-x)
    (m/doseq-array-backward [child (.-children layer)]
      (move-ground! child new-offset-x))))

(defn- move-all-ground! [^js layer x]
  (let [offset-x (p/get layer :--offset-x)]
    (m/doseq-array-backward [child (.-children layer)]
      (move-ground! child (+ offset-x x)))))

(defn- make-ground [k base-x base-y scale tint]
  (let [ground-tex-w 1024
        ground-tex-h 256
        ^js tex (.clone (putil/->tex :ground))
        sp (putil/->sp tex)]
    ;(putil/set-wrap-mode! tex :mirrored-repeat)
    (putil/set-wrap-mode! tex :repeat)
    (p/set! tex
            :orig/width (/ ground-tex-w scale)
            :orig/height (/ ground-tex-h scale)
            )
    (.updateUvs tex)
    (p/set! sp
            :anchor/x 0.5
            :anchor/y 1
            :x base-x
            :y base-y
            :width ground-tex-w
            :height ground-tex-h
            :tint tint
            :name (name k)
            :--loop-w (* ground-tex-w 2)
            :--loop-h (* ground-tex-h 2)
            )))

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
  (let [sp (p/set! (putil/sp16x16)
                   :width s-w
                   :height s-h
                   :alpha 0
                   :interactive true
                   :name (name :tree/touchpanel))
        ;; mouseは左クリックチェックする、touchはチェックしない
        h-mouse (fn [^js e]
                  (when (= :game (:mode @a-state))
                    (when e
                      (when-let [^js data (.-data e)]
                        (let [b (.-button data)]
                          (when (or (nil? b) (zero? b))
                            (swap! a-state assoc :last-pressed? true)
                            (swap! a-state assoc :last-pressed? true)
                            (when-let [^js gpos (.-global data)]
                              (pos/set! (:last-touch-pos @a-state)
                                        (.-x gpos)
                                        (.-y gpos)))))))))
        h-touch (fn [^js e]
                  (when (= :game (:mode @a-state))
                    (when e
                      (when-let [^js data (.-data e)]
                        (swap! a-state assoc :last-pressed? true)
                        (when-let [^js gpos (.-global data)]
                          (pos/set! (:last-touch-pos @a-state)
                                    (.-x gpos)
                                    (.-y gpos)))))))
        ]
    (.on sp "mousedown" h-mouse)
    (.on sp "touchstart" h-touch)
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
   (p/set! (putil/->sp (putil/set-nearest! (dataurl/p8elf)))
           :anchor/x 0.45
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
   (let [base-x (/ s-w 2)
         base-y (+ s-h 88)]
     [:tree/ground-layer
      {:--offset-x 0}
      (make-ground :tree/ground-far base-x (- base-y 96 48 24) 0.25 0x7F7F7F)
      (make-ground :tree/ground-mid base-x (- base-y 96 48) 0.5 0xBFBFBF)
      (make-ground :tree/ground-near base-x (- base-y 96) 1 0xEFEFEF)
      (make-ground :tree/ground-nearest base-x (- base-y 0) 2 0xFFFFFF)
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
   (status/make-ui-layer a-state s-w s-h)
   (make-gameclear-layer s-w s-h)
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
      (status/update-status-label! a-state))
    (when-let [layer (:tree/scroll-layer @a-state)]
      (p/set! layer :x (- scroll-adjust-x new-p-x)))
    (p/set! player-layer :x new-p-x)
    (p/set! player-sp :y new-p-y)
    ;(when just-landed?
    ;  (util/se! :se/landing)
    ;  (util/vibrate! 50)
    ;  (object/emit-near-smoke! new-p-x new-p-y))
    (when trigger-jump?
      (object/emit-near-smoke! a-state old-p-x old-p-y)
      (util/vibrate! 50)
      (util/se! :se/puyo-psg :volume 0.5))
    (object/process-layer! a-state
                           (:tree/near-object-layer @a-state)
                           delta-frames new-p-x new-p-y)
    (object/process-layer! a-state
                           (:tree/far-object-layer @a-state)
                           delta-frames new-p-x new-p-y)
    ;; 新オブジェクトの生成(必要なら)
    (when (< old-p-x new-p-x)
      (object/update-visited-distance! a-state new-p-x delta-frames))
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
      :game (do
              (let [old-elapsed-sec (:elapsed-sec @a-state)
                    new-elapsed-sec (int (/ (- (util/now-msec)
                                               (:start-msec @a-state))
                                            1000))]
                (when-not (= old-elapsed-sec new-elapsed-sec)
                  (swap! a-state assoc :elapsed-sec new-elapsed-sec)
                  (status/update-status-label! a-state)))
              (tick-player-ground-objs! delta-frames)
              nil)
      nil)
    ;; TODO
    ))



(def lifecycle
  {:preload preload!
   :create create!
   :tick tick!
   :destroy destroy!})



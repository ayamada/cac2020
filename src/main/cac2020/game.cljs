(ns cac2020.game
  (:require [clojure.string :as string]
            ["pixi.js" :as pixi]
            ["va5" :as va5]
            [cac2020.util :as util :include-macros true]
            [cac2020.space :as space]
            [cac2020.dataurl :as dataurl]
            ))




;;; - プレイヤーを動かすルールを決めましょう
;;;     - マリオベースのスクロール？ガーディック的進行？
;;;     - ...
;;;     - ...







;;; - 短い内容かつ「マリオのコインブロックを連打するような楽しさ」のある奴がよい。具体的には？
;;;     - 自機からクリック位置に弾丸を連射するような奴？
;;;         - いか工船ベースで、画面内の敵を殲滅したら次の画面に移動する奴？
;;;     - ブロックの真下に移動してジャンプしまくる？
;;;     - cookie clickerのクッキーみたいなのが流れてきて、数十回だけクリックできて、また次のクッキーを求めて先に進む、みたいな奴
;;;         - この「クリックできる奴」および「クリックするとゲットできる奴」を決める必要がある
;;;             - ただ、無限に遊べるのはよくない。そうではないルールにする必要がある
;;;                 - 1分程度のタイムの中でスコアを稼ぐ
;;;                 - いかに早くステージをクリアできるかを競う
;;;                     - 全敵殲滅でクリア
;;;                     - ボスを倒すとクリア
;;;                     - ゴール到達でクリア
;;;                     - 配置アイテム全ゲットでクリア
;;;         - 「クリックしてゲットした奴」の利用方法がほしい
;;;             - スコア。最終的にランキング登録できる
;;;                 - 今回はこれだけで。複雑なゲーム内容にする余地はない



;;; - 縦スクロール？横スクロール？固定画面？
;;; - 左右移動
;;;     - dc-ui？トマトマ移動？
;;;     - スクロールあり？なし？
;;; - 剣が落ちてて、拾うと画面下部中央にボタン出現、これで剣を真上に射出できる。剣を拾うほど連射可
;;;     - 剣ではなく弓矢とかブーメランとかトマホークの方がよい？
;;;     - ジャンプブーツとかウイングにする？
;;; - 空に巨大フルーツが浮いている、そこに武器を打ち込むと小さいフルーツをゲット、一定回数で巨大フルーツは消える
;;;     - フルーツ収穫でどんないい事がある？
;;;         - スコア加算
;;;         - スコア倍率アップ
;;;         - パワーアップ
;;;     - 巨大フルーツの動きは？
;;;         - 縦スクロール追随？
;;;         - ネコゴのように横に流れる？
;;; - クリア条件は？
;;;     - 1分固定(それまでのスコアを稼ぐ)
;;;     - 最上部にいる敵ボスを倒す(それまでのタイムを競う)
;;;     - 画面内の敵を殲滅(それまでのタイムを競う)
;;;     - ...
;;;     - ...
;;;     - ...











;;; TODO: singletonなのはよくないが…どうやって状態を回し持つ？
;;;       普通にやるとしたら、lifecycleを即値mapで返すのではなく、
;;;       generatorで返すようにする(＝何個でも生成できる)
;;;       しかないのでは？もうちょっと考える…
(def a-state (atom {}))


(defn- preload-audio! []
  (util/load-audio! nil
                    :bgm/bsbs__LE661501
                    :se/caret-psg
                    :se/submit-psg
                    :se/lvup-midi
                    :se/yarare-psg
                    ;; TODO
                    ))

(defn- preload! [app cont]
  (util/load-textures! cont
                       :ground
                       :orange
                       :banana
                       :nova
                       :cube3
                       ;; 以下は使うかどうか分からない
                       :driftcat2020
                       :korokke128x96x14
                       :dishkorokke112x68x14
                       ))



(defn- emit-retry! [b]
  (util/se! :se/submit-psg)
  (util/set-property! (util/search-child (:root @a-state) :gameover-layer)
                      :visible false)
  ;; TODO
  (js/window.setTimeout (fn []
                          ;; TODO
                          )
                        500))

(defn- make-gameover-layer [s-w s-h]
  [:gameover-layer
   {:visible false}
   (util/set-properties! (util/make-label "Congratulations!"
                                          :font-family "serif"
                                          :font-size 80
                                          :fill #js [0xFFFFFF 0x7F7FFF]
                                          )
                         :anchor/x 0.5
                         :anchor/y 0.5
                         :x (* s-w 0.5)
                         :y (* s-h 0.4)
                         :name (name :gameover-caption))
   (util/set-properties! (util/make-button "再挑戦"
                                           emit-retry!
                                           :padding 64
                                           )
                         :x (* s-w 0.5)
                         :y (* s-h 0.7)
                         :name (name :replay-button))
   ])

(defn- emit-gameover! []
  (let [gameover-layer (util/search-child (:root @a-state) :gameover-layer)
        replay-button (util/search-child (:root @a-state) :replay-button)
        ]
    (util/se! :se/lvup-midi)
    (util/set-property! replay-button :visible false)
    (util/set-property! gameover-layer :visible true)
    ;; TODO
    (js/window.setTimeout (fn []
                            (util/set-property! replay-button :visible true)
                            ;; TODO
                            )
                          2000)))

(defn- emit-start! [b]
  (util/se! :se/submit-psg)
  (util/bgm! :bgm/bsbs__LE661501)
  (let [root (:root @a-state)]
    (util/set-property! (util/search-child root :title-layer)
                        :visible false)
    (when-let [player-layer (util/search-child root :player-layer)]
      (util/set-properties! player-layer
                            :x 128
                            :y (- 800 32)
                            :visible true)
      (util/register-tween! player-layer
                            30
                            (fn [^js player-layer progress]
                              (util/set-properties! player-layer
                                                    :x (+ -128
                                                          (* 256 progress)))
                              (when (= 1 progress)
                                (swap! a-state assoc :mode :game)
                                (util/set-property! (util/search-child root :ui-layer)
                                                    :visible true)
                                ;; TODO
                                ))))
    (swap! a-state
           assoc
           :mode :game-op
           :s-spd-x 0
           :s-spd-y 0
           :s-spd-z -0.0002
           :s-spd-yaw 0.2
           :s-spd-pitch 0
           )
    ;; TODO
    ;(js/window.setTimeout (fn []
    ;                        ;; TODO
    ;                        ;(emit-gameover!)
    ;                        )
    ;                      1000)
    ))

(defn- make-title-layer [s-w s-h]
  [:title-layer
   {:visible true}
   ;; TODO: タイトル背景
   ;(util/set-properties! (util/sp16x16)
   ;                      :width s-w
   ;                      :height s-h
   ;                      :tint 0xFF0000)
   (util/set-properties! (util/make-label (str "version:" util/VERSION)
                                          :font-family "serif"
                                          :font-size 24
                                          )
                         :anchor/x 0
                         :anchor/y 1
                         :x 8
                         :y (- s-h 8)
                         :name (name :version-label))
   (util/set-properties! (util/make-label "タイトル未定"
                                          :font-size 96
                                          )
                         :anchor/x 0.5
                         :anchor/y 0.5
                         :x (* s-w 0.5)
                         :y (* s-h 0.25)
                         :name (name :title-caption))
   (util/set-properties! (pixi/Sprite.from dataurl/pudding)
                         :anchor/x 0.5
                         :anchor/y 0.5
                         :scale/x 8
                         :scale/y 8
                         :x (* s-w 0.5)
                         :y (* s-h 0.45)
                         :name (name :start-button))
   (util/set-properties! (util/make-button "START"
                                           emit-start!
                                           :padding 64
                                           )
                         :x (* s-w 0.5)
                         :y (* s-h 0.8)
                         :name (name :start-button))
   ])


(defn- update-status-label! [& [status-label]]
  (let [
        ;{:keys [s-spd-x s-spd-y s-spd-z s-spd-yaw s-spd-pitch]} @a-state
        score (or (:score @a-state) 0)
        text (str "SCORE: " score
                  "\n"
                  ""
                  )
        ]
    (util/set-properties! status-label :text text)))




(defn- make-ui-layer [s-w s-h]
  (let [status-label (util/set-properties! (util/make-label ""
                                                            :font-size 32
                                                            :align "left"
                                                            )
                                          :anchor/x 0
                                          :anchor/y 0
                                          :x 16
                                          :y 16
                                          :name (name :status-label))
        b (fn [label xr yr f]
            (util/set-properties! (util/make-button label
                                                    f
                                                    :padding 32)
                                  :x (* s-w xr)
                                  :y (* s-h yr)))]
    (update-status-label! status-label)
    [:ui-layer
     {:visible false}
     status-label
     (b "ACTION"
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

(defn- scroll-ground! [^js sp delta-x delta-y]
  (let [^js tex (.-texture sp)
        ^js frame (.-frame tex)
        loop-w (util/property sp :-loop-w)
        loop-h (util/property sp :-loop-h)
        x (mod (+ delta-x (.-x frame)) loop-w)
        y (mod (+ delta-y (.-y frame)) loop-h)
        ]
    (set-scroll-pos! sp x y)))

(defn- scroll-all-ground! [^js layer delta-x delta-y]
  (let [^js children (.-children layer)]
    (dotimes [i (alength children)]
      (let [child (aget children i)]
        (scroll-ground! child delta-x delta-y)))))

(defn- make-ground [k s-w s-h adjust-h scale tint]
  (let [^js tex (.clone (util/->tex :ground))
        ^js base-tex (.-baseTexture tex)
        orig-w 1024
        orig-h 184
        ;; MIRRORED_REPEATにすると折り返し部分が模様に見えてしまうので、
        ;; 通常のREPEATにする事にした
        ;wrap-mode pixi/WRAP_MODES.MIRRORED_REPEAT
        wrap-mode pixi/WRAP_MODES.REPEAT
        _ (util/set-property! base-tex :wrap-mode wrap-mode)
        _ (util/set-properties! tex
                                :orig/width (/ orig-w scale)
                                :orig/height (/ orig-h scale)
                                )
        x (/ s-w 2)
        y (- s-h 184 adjust-h)
        w orig-w
        h orig-h
        ;h (* orig-h scale)
        sp (util/->sp tex)]
    (.updateUvs tex)
    (util/set-properties! sp
                          :anchor/x 0.5
                          :anchor/y 0
                          :x x
                          :y y
                          :width w
                          :height h
                          :tint tint
                          :name (name k)
                          ;; pixi/WRAP_MODES.MIRRORED_REPEAT なので、
                          ;; 元の2倍の長さ指定が必要になる
                          :-loop-w (* orig-w 2)
                          :-loop-h (* orig-h 2)
                          )
    sp))

(defn- make-tree [s-w s-h]
  [:root
   (util/set-properties! (util/sp16x16)
                         :name (name :background-black)
                         :tint 0x00001F
                         :x -64
                         :y -64
                         :width (+ s-w 128)
                         :height (+ s-h 128)
                         )
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
     (util/set-properties! (space/make sightbox params)
                           :name (name :background-space-layer)))
   [:back-layer
    ]
   (let [ground-base 96]
     [:ground-layer
      ;(make-ground :farthest s-w s-h (+ ground-base 64 32 16) 0.125 0x3F3F3F)
      (make-ground :far s-w s-h (+ ground-base 64 32) 0.25 0x7F7F7F)
      (make-ground :mid s-w s-h (+ ground-base 64) 0.5 0xBFBFBF)
      (make-ground :near s-w s-h (+ ground-base) 1 0xEFEFEF)
      (make-ground :nearest s-w s-h (+ ground-base -96) 2 0xFFFFFF)
      ])
   [:main-layer
    [:player-layer
     {
      :x 128
      :y (- 800 32)
      :visible false
      }
     (util/set-properties! (util/->sp dataurl/sphere)
                           :anchor/x 0.5
                           :anchor/y 0.5
                           :scale/x 6
                           :scale/y 1
                           :alpha 0.5
                           :tint 0x000000
                           :name (name :player-ds))
     (util/set-properties! (util/->sp dataurl/p8elf)
                           :anchor/x 0.5
                           :anchor/y 0.95
                           :scale/x -8
                           :scale/y 8
                           :name (name :player-sp))
     ]
    ]
   [:front-layer]
   (make-gameover-layer s-w s-h)
   (make-ui-layer s-w s-h)
   (make-title-layer s-w s-h)
   [:effect-layer]])

(defn- create! [app]
    (swap! a-state
           assoc
           :s-spd-x 1
           :s-spd-y -2
           :s-spd-z 0.01
           :s-spd-yaw 0
           :s-spd-pitch 2
           )
  (let [^js renderer (:renderer app)
        s-w (.-width renderer)
        s-h (.-height renderer)
        ^js stage (util/build-container-tree (make-tree s-w s-h))
        ]
    (preload-audio!)
    (util/bgm! nil)
    (swap! a-state
           assoc
           :mode :title
           :root stage
           :score 0
           )
    stage))


(defn- destroy! [app]
  ;(prn 'destroy!)
  ;; TODO: きちんとpixiの各インスタンスを破棄する(メモリリークの元になるので)
  ;; TODO: きちんとa-state内のものを破棄する
  (let [root (:root @a-state)
        ]
    (when-let [layer (util/get-child root :background-space-layer)]
      (space/destroy! layer))
    (util/dea! root))
  (reset! a-state {}))


(defn- tick! [app delta-frames]
  (util/tick-tween! delta-frames)
  (let [root (:root @a-state)
        ]
    (when-let [layer (util/get-child root :background-space-layer)]
      ;; 状況に応じて速度を変更したりする
      (let [
            {:keys [s-spd-x s-spd-y s-spd-z s-spd-yaw s-spd-pitch]} @a-state
            ;; この移動量はカメラ側のもの。風景側は逆方向に動く事になる
            move-x (* delta-frames s-spd-x)
            move-y (* delta-frames s-spd-y)
            move-z (* delta-frames s-spd-z)
            yaw (* delta-frames s-spd-yaw)
            pitch (* delta-frames s-spd-pitch)
            ]
        (space/update! layer move-x move-y move-z yaw pitch)))
    (when (= :title (:mode @a-state))
      (when-let [layer (util/get-child root :ground-layer)]
        (scroll-all-ground! layer
                            (* delta-frames 4)
                            0)))
    ;; TODO
    ))



(def lifecycle
  {:preload preload!
   :create create!
   :tick tick!
   :destroy destroy!})




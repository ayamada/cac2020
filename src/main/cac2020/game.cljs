(ns cac2020.game
  (:require [clojure.string :as string]
            ["pixi.js" :as pixi]
            ["va5" :as va5]
            [cac2020.util :as util]
            ))



;;; 背景どうする？
;;; - 星の流れる宇宙/夜空
;;; - 写真の背景(雲が流れる)


;;; - ゲーム内容候補
;;;     - もぐらたたき
;;;         - 入力にpixi.interactionを使えるので楽
;;;         - グラフィックの用意は必要
;;;         - これでいいと思う
;;;         - フルーツがニョキッと出てくるのをゲット、ただしゴリラはゲットしない、みたいなのでよいのでは？
;;;             - ゲット後はネコゴのようにスコア部へと飛んでいく形式
;;;         - 出現パターンを設定するのが面倒…どうする？
;;;             - 他のゲーム内容を検討する？クリックゲー？
;;;     - 画面奥からフルーツとゴリラが射出される、フルーツだけをクリックする
;;;         - かなりネコゴに近い奴だと思う



;;; TODO: singletonなのはよくないが…どうやって状態を回し持つ？
;;;       普通にやるとしたら、lifecycleを即値mapで返すのではなく、
;;;       generatorで返すようにする(＝何個でも生成できる)
;;;       しかないのでは？もうちょっと考える…
(def a-state (atom {}))


(defn- preload! []
  (util/load-audio! :se/caret-psg
                    :se/submit-psg
                    :se/lvup-midi
                    :se/yarare-psg
                    ;; TODO
                    )

  ;; TODO
  )



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
                         :name "gameover-caption")
   (util/set-properties! (util/make-button "再挑戦"
                                           emit-retry!
                                           :padding 64
                                           )
                         :x (* s-w 0.5)
                         :y (* s-h 0.7)
                         :name "replay-button")
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
  (util/set-property! (util/search-child (:root @a-state) :title-layer)
                      :visible false)
  ;; TODO
  (js/window.setTimeout (fn []
                          ;; TODO
                          (emit-gameover!)
                          )
                        1000))

(defn- make-title-layer [s-w s-h]
  [:title-layer
   {:visible true}
   ;; TODO: タイトル背景
   ;(util/set-properties! (util/sp16x16)
   ;                      :width s-w
   ;                      :height s-h
   ;                      :tint 0xFF0000)
   (util/set-properties! (util/make-label (str "version:"
                                               util/VERSION)
                                          :font-family "serif"
                                          :font-size 24
                                          )
                         :anchor/x 0
                         :anchor/y 1
                         :x 8
                         :y (- s-h 8)
                         :name "version-label")
   (util/set-properties! (util/make-label "タイトル未定"
                                          :font-size 96
                                          )
                         :anchor/x 0.5
                         :anchor/y 0.5
                         :x (* s-w 0.5)
                         :y (* s-h 0.3)
                         :name "title-caption")
   (util/set-properties! (util/make-button "START"
                                           emit-start!
                                           :padding 64
                                           )
                         :x (* s-w 0.5)
                         :y (* s-h 0.7)
                         :name "start-button")
   ])

(defn- make-tree [s-w s-h]
  [:root
   [:back-layer]
   [:main-layer]
   [:front-layer]
   (make-gameover-layer s-w s-h)
   [:ui-layer
    ;; TODO: 何を置くかは未定
    ]
   (make-title-layer s-w s-h)
   [:effect-layer]])

(defn- create! [app]
  (let [^js renderer (:renderer app)
        s-w (.-width renderer)
        s-h (.-height renderer)
        ^js stage (util/build-container-tree (make-tree s-w s-h))
        ]
    (preload!) ; TODO: これはlifecycle側に組み込むべき
    ;; TODO
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
  (reset! a-state {})
  )

(defn- tick! [app delta-msec]
  ;(prn 'tick! delta-msec)
  ;; TODO
  )



(def lifecycle
  {:create create!
   :tick tick!
   :destroy destroy!
   })




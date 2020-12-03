(ns cac2020.game
  (:require [clojure.string :as string]
            ["pixi.js" :as pixi]
            ["va5" :as va5]
            [cac2020.util :as util :include-macros true]
            [cac2020.space :as space]
            [cac2020.dataurl :as dataurl]
            [cac2020.pos :as pos]
            [cac2020.pointer :as pointer]
            ))



;;; - 歩き(走り)モーションをつけましょう
;;;     - ドットパターンを追加する？
;;;         - とりあえず↓だけで。物足りなければ追加を考える
;;;     - h変動、y変動、r変動、等々をする？skewでなんとかする？
;;;         - とりあえずy変動で、小さくジャンプさせたりする
;;;         - spdと同様に、ジャンプ用変数を用意する。spd-xとspd-yに分ける？
;;;             - できれば放物線にしたいが、デコジャンプでもokとする



;;; - ゲーム内容案候補
;;;     - 低空に食べ物がある、クリック連打(ジャンプ連打？)で食べ物たくさんゲット、一定時間(回数？)で消滅。ゲットした食べ物を投げて敵に攻撃。
;;;     - 敵を射撃して撃破すると食べ物が飛び散るのでそれをキャッチする
;;;     - ...
;;;     - ...
;;;     - ...
;;;     - ...


;;; - 武器を取ったら武器で攻撃できるようにしましょう
;;;     - 地面に武器が落ちてる、接触でゲットすると画面下部に武器攻撃ボタンが表示される
;;;     - 取れば取るほど連射数がアップ
;;;         - どこかに連射数(残弾数？)を表示させたい
;;;     - 攻撃ボタンの見た目を分かりやすくしたい
;;;         - ボタンに武器グラフィックを表示する？
;;;         - 押せない時の表示は？
;;; - 武器は何なのか決めましょう
;;;     - プリン？
;;;     - バナナ？
;;;     - オレンジ？
;;;     - コロッケ？
;;;     - 剣？
;;;     - ブーメラン？
;;;     - 立方体？

;;; - プレイヤーのゲーム内論理座標を記録しましょう
;;;     - アイテムや敵やゴールの出現管理に使う為に必要

;;; - 敵をspawn、表示、動かしましょう
;;;     - 動きは、上空を飛行する感じで
;;;     - どのテクスチャを使う？
;;;         - 立方体三種？
;;;         - psg？
;;;         - コロッケ？
;;;         - バナ？
;;;         - オレンジ？
;;;     - どういう攻撃をしてくる？
;;;         - sp16x16を射出。これでも残像エフェクトをつけるとそれっぽくはなる。これを検討





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











;;; TODO: singletonなのはよくない。lifecycle管理側からstateを渡せるようにするか、lifecycleそのものにatomを含めるようにする方向でなんとかするのを検討する事
(def a-state (atom {}))


(defn- preload-audio! []
  (util/load-audio! nil
                    :bgm/bsbs__LE661501
                    :se/caret-psg
                    :se/submit-psg
                    :se/lvup-midi
                    :se/yarare-psg
                    :se/puyo-psg
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
  (util/set-property! (:tree/gameover-layer @a-state)
                      :visible false)
  ;; TODO
  (js/window.setTimeout (fn []
                          ;; TODO
                          )
                        500))

(defn- make-gameover-layer [s-w s-h]
  [:tree/gameover-layer
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
                         :name (name :tree/gameover-caption))
   (util/set-properties! (util/make-button "再挑戦"
                                           emit-retry!
                                           :padding 64
                                           )
                         :x (* s-w 0.5)
                         :y (* s-h 0.7)
                         :name (name :tree/replay-button))
   ])

(defn- emit-gameover! []
  (let [gameover-layer (:tree/gameover-layer @a-state)
        replay-button (:tree/replay-button @a-state)
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
  (let [root (:tree/root @a-state)]
    (util/set-property! (:tree/title-layer @a-state)
                        :visible false)
    (when-let [player-layer (:tree/player-layer @a-state)]
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
                                (util/set-property! (:tree/ui-layer @a-state)
                                                    :visible true)
                                ;; TODO
                                ))))
    (swap! a-state
           assoc
           :mode :game-op
           :space-spd-x 0
           :space-spd-y 0
           :space-spd-z -0.0002
           :space-spd-yaw 0.2
           :space-spd-pitch 0
           )
    ;; TODO
    ;(js/window.setTimeout (fn []
    ;                        ;; TODO
    ;                        ;(emit-gameover!)
    ;                        )
    ;                      1000)
    ))

(defn- make-title-layer [s-w s-h]
  [:tree/title-layer
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
                         :name (name :tree/version-label))
   (util/set-properties! (util/make-label "タイトル未定"
                                          :font-size 96
                                          )
                         :anchor/x 0.5
                         :anchor/y 0.5
                         :x (* s-w 0.5)
                         :y (* s-h 0.25)
                         :name (name :tree/title-caption))
   (util/set-properties! (pixi/Sprite.from dataurl/pudding)
                         :anchor/x 0.5
                         :anchor/y 0.5
                         :scale/x 8
                         :scale/y 8
                         :x (* s-w 0.5)
                         :y (* s-h 0.45)
                         :name (name :tree/title-image))
   (util/set-properties! (util/make-button "START"
                                           emit-start!
                                           :padding 64
                                           )
                         :x (* s-w 0.5)
                         :y (* s-h 0.8)
                         :name (name :tree/start-button))
   ])


(defn- update-status-label! [& [status-label]]
  (let [
        ;{:keys [space-spd-x space-spd-y space-spd-z space-spd-yaw space-spd-pitch]} @a-state
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
                                          :name (name :tree/status-label))
        b (fn [label xr yr f]
            (util/set-properties! (util/make-button label
                                                    f
                                                    :padding 32)
                                  :x (* s-w xr)
                                  :y (* s-h yr)))]
    (update-status-label! status-label)
    [:tree/ui-layer
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
     ;(b "←"
     ;   0.1
     ;   0.9
     ;   (fn [b]
     ;     (util/cancel-button-effect!)
     ;     (when (and
     ;             (= :game (:mode @a-state))
     ;             true ; TODO
     ;             )
     ;       (let []
     ;         ;(util/se! :se/submit-psg)
     ;         ;; TODO
     ;         ))))
     ;(b "→"
     ;   0.9
     ;   0.9
     ;   (fn [b]
     ;     (util/cancel-button-effect!)
     ;     (when (and
     ;             (= :game (:mode @a-state))
     ;             true ; TODO
     ;             )
     ;       (let []
     ;         ;(util/se! :se/submit-psg)
     ;         ;; TODO
     ;         ))))
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
    (util/set-properties! (space/make sightbox params)
                          :name (name :tree/background-space-layer))))

(defn- make-touchpanel [s-w s-h]
  (let [
        ;; TODO
        sp (util/set-properties! (util/sp16x16)
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

(defn- make-tree [s-w s-h]
  [:tree/root
   (make-touchpanel s-w s-h)
   (util/set-properties! (util/sp16x16)
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
      (make-ground :tree/far s-w s-h (+ ground-base 64 32) 0.25 0x7F7F7F)
      (make-ground :tree/mid s-w s-h (+ ground-base 64) 0.5 0xBFBFBF)
      (make-ground :tree/near s-w s-h (+ ground-base) 1 0xEFEFEF)
      (make-ground :tree/nearest s-w s-h (+ ground-base -96) 2 0xFFFFFF)
      ])
   [:tree/main-layer
    [:tree/player-layer
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
                           :name (name :tree/player-ds))
     (util/set-properties! (util/->sp dataurl/p8elf)
                           :anchor/x 0.5
                           :anchor/y 0.95
                           :scale/x -8
                           :scale/y 8
                           :name (name :tree/player-sp))
     ]
    ]
   [:tree/front-layer]
   (make-gameover-layer s-w s-h)
   (make-ui-layer s-w s-h)
   (make-title-layer s-w s-h)
   [:tree/effect-layer]])

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
        ^js stage (util/build-container-tree (make-tree s-w s-h))
        ]
    (preload-audio!)
    (util/bgm! nil)
    (swap! a-state
           assoc
           :mode :title
           :score 0
           )
    (util/index-container-tree! stage a-state :tree/*)
    stage))


(defn- destroy! [app]
  ;(prn 'destroy!)
  ;; TODO: きちんとpixiの各インスタンスを破棄する(メモリリークの元になるので)
  ;; TODO: きちんとa-state内のものを破棄する
  (let [root (:tree/root @a-state)
        ]
    (when-let [layer (:tree/background-space-layer @a-state)]
      (space/destroy! layer))
    (util/dea! root))
  (reset! a-state {}))


(defn- tick! [app delta-frames]
  (util/tick-tween! delta-frames)
  (let [root (:tree/root @a-state)
        player-layer (:tree/player-layer @a-state)
        player-sp (:tree/player-sp @a-state)
        ]
    (when-let [layer (:tree/background-space-layer @a-state)]
      ;; 状況に応じて速度を変更したりする
      (let [
            {:keys [space-spd-x space-spd-y space-spd-z space-spd-yaw space-spd-pitch]} @a-state
            ;; この移動量はカメラ側のもの。風景側は逆方向に動く事になる
            move-x (* delta-frames space-spd-x)
            move-y (* delta-frames space-spd-y)
            move-z (* delta-frames space-spd-z)
            yaw (* delta-frames space-spd-yaw)
            pitch (* delta-frames space-spd-pitch)
            ]
        (space/update! layer move-x move-y move-z yaw pitch)))
    (when (= :title (:mode @a-state))
      (when-let [layer (:tree/ground-layer @a-state)]
        (scroll-all-ground! layer
                            (* delta-frames 4)
                            0)))
    (when (= :game (:mode @a-state))
      (let [{:keys [last-pressed? last-touch-pos last-spd]} @a-state
            pressed? (and last-pressed? (pointer/pressed?))
            ;last-touch-x (when pressed?
            ;               (pos/->x last-touch-pos))
            ;last-touch-y (when pressed?
            ;               (pos/->y last-touch-pos))
            max-spd-x 8
            base-acc-x 0.1
            base-acc-y 0.1
            max-jump 128
            initial-jump-spd-y -4
            ;; TODO: 可能なら速度は割合減少がよいが計算が面倒
            old-spd-x (pos/->x last-spd)
            delta-spd-x (* delta-frames
                           (if pressed?
                             base-acc-x
                             (- base-acc-x)))
            new-spd-x (max 0 (min (+ old-spd-x delta-spd-x) max-spd-x))
            old-player-jump (- (util/property player-sp :y))
            trigger-jump? (and
                            pressed?
                            (zero? old-player-jump)
                            (pos? new-spd-x))
            old-spd-y (pos/->y last-spd)
            new-spd-y (+ old-spd-y (* base-acc-y delta-frames))
            new-spd-y (if trigger-jump?
                        initial-jump-spd-y
                        new-spd-y)
            delta-player-jump (* new-spd-y delta-frames)
            new-player-jump (max 0 (min (- old-player-jump delta-player-jump)
                                        max-jump))
            ]
        (when (and last-pressed? (not pressed?))
          (swap! a-state assoc :last-pressed? false))
        (pos/set! last-spd new-spd-x new-spd-y)
        (when-let [layer (:tree/ground-layer @a-state)]
          (scroll-all-ground! layer (* delta-frames new-spd-x) 0))
        (util/set-property! player-sp :y (- new-player-jump))
        (when trigger-jump?
          ;; TODO: smokeを出す
          (util/se! :se/puyo-psg :volume 0.5)
          )
        ;; TODO: プレイヤー論理座標の更新も必要
        ))
    ;; TODO
    ))



(def lifecycle
  {:preload preload!
   :create create!
   :tick tick!
   :destroy destroy!})




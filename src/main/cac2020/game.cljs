(ns cac2020.game
  (:require [clojure.string :as string]
            ["pixi.js" :as pixi]
            ["va5" :as va5]
            [cac2020.util :as util :include-macros true]
            [cac2020.space :as space]
            [cac2020.effect :as effect]
            ))


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
;;;     - ゲームオーバー判定は？
;;;         - 一定時間経過？(この場合はフルーツゲットでタイム回復？
;;;         - ゴリラクリック？
;;;     - もうちょっとプレイヤーに「これできるんか～？」みたいなチャレンジを煽りたい。いいアイデアはある？
;;;         - ...
;;;         - ...
;;;         - ...
;;;         - ...


;;; - とりあえず「フルーツをクリック」という部分が動かないのであれば、フルーツ画像を投入すべきでは？
;;;     - バナナ、みかん、ドットチェリー、ドット葉っぱ、ドットプリン、コロッケ
;;;         - とりあえずこの辺は確定で
;;;             - とりあえずb3/b2にある筈だが…
;;;         - 配置位置を決める事
;;;             - imgとは分けた方が無難
;;;             - tex/ でいいと思う
;;;     - 他に使えそうなのはある？
;;;         - 爆弾
;;;         - 魚類、ドット魚類
;;;         - おはぎ
;;;         - トマトマ
;;;         - ...
;;; - フルーツはどこから出てくる？
;;;     - 画面外？
;;;     - 画面奥？
;;;     - UFOとかの乗り物が投げてくる？
;;;     - 星から飛んでくる？
;;;     - ？？？
;;;     - ？？？
;;;     - ？？？
;;; - 何の為にフルーツを集める？
;;;     - 右上にいる人/動物に食べさせる為？
;;;     - ？？？
;;;     - ？？？
;;;     - ？？？
;;;     - ？？？






;;; - 基本はpixi.interactionを使うのでクリックゲー/ボタンゲーにしかならない
;;;     - 虚無虚無タイプ？→これは短いゲームには合わないので今回はパスで
;;;     - おは鉱タイプ？
;;;     - リズムゲータイプ？ - 実はこれがよいのでは？
;;;     - cookie clickerタイプ？ - もうちょっとアクション性が必要



;;; - ベルトロール型砲台シューティングを検討
;;;     - 自機は左下砲台。画面クリックでその方向に弾丸射出。画面内に敵が何匹か飛んでいる。殲滅したら画面が横スクロールして次のステージへ。
;;;     - これ面白そうだけど、ステージと敵をたくさん用意するの無理では？
;;;     - あとパワーアップを用意したいけど、どうやって提供する？



;;; BGMを選別して投入しましょう。ライセンス面とサイズ面より、自作から短い奴を選ぶしかない
;;; - とりあえず仮にbsbsを設置したが、もっとムード(空気感？宇宙感？ドライブ感？)のある奴にしたい。ただ手持ちではこれが一番マシか？
;;;     - gbgあたりもよいが、ちょっと短いか？わさびのように複数の曲を切り替える内の一つならアリだが、今回は容量を増やしたくないので厳し目
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
;;;                     - ...
;;;                     - ...
;;;                     - ...
;;;                 - ...
;;;                 - ...
;;;                 - ...
;;;                 - ...
;;;             - ...
;;;             - ...
;;;             - ...
;;;         - 「クリックしてゲットした奴」の利用方法がほしい
;;;             - スコア。最終的にランキング登録できる
;;;                 - 今回はこれだけでよいのでは？
;;;             - (コンボ)ボーナス倍率。スコア換算という意味では↑と同じだが、倍率にかかるので重要になる
;;;             - 金。ショップで使う→ショップの景品が必要…
;;;             - 金。アップグレード購入に使う→アップグレードの実装が必要…
;;;             - パワーアップ要素。最大射出数、ショットパワー、同時射出数、ショット速度、スコア倍率、あたり
;;;             - パワーアップ要素。
;;;             - タイム回復。これはやめといた方がよい…
;;;             - ...
;;;             - ...
;;;             - ...
;;;             - ...
;;;         - ...
;;;         - ...
;;;         - ...
;;;     - ...
;;;     - ...
;;;     - ...
;;;     - ...


;;; - 地面どうする？
;;;     - おそらく地面が必要
;;;     - できればトミーパトロールみたいに多重スクロールさせる
;;;     - texの用意
;;;         - いつもの森の地面を使う？奥方向があまりよくない…
;;;         - 写真のどれかを加工して使う方向で
;;;         - 横スクロールテストのは？op0021 http://banker.tir.jp/op0021/
;;;             - ちょっと荒いか？
;;;             - これでもいい気はするが…
;;;             - 一応 public/tex/ground.png としてコピーした。が、ちょっとボケすぎていると思う。別の写真から用意したい



;;; 煙/雲。32x32。
(def dataurl-smoke32x32
  (util/str* "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf"
             "8/9hAAABNUlEQVQ4jYWToY6DUBBFGxIEAoHBYBAkiCpkTSUGi8GgVmFwqDW4ui"
             "pcDZ9Qi62v3B9Yvd9w1tyhb5vSJbnhpZ17eufNdLd78wA+EAAe4L+rNYMHBDoH"
             "QAEc9E43ITJmQALk0h7opFKgvWqCZ0AB1MAHUMnQAj3wKfX6vlSa0Mwx0KhgkE"
             "bpBJyBSZBBP1Cs7QChAFZ8BmbgKl2ARZBRLTVqxbcEpRN1kuEOfAHfOhtsVF0F"
             "xJYi0x2MSnCV+UcyyEVJOwFSA6S6IOt/3gBMzkXnQGLLcnAAvVLcBDHdBKhV7w"
             "OeJUickQ2Kuci0SLPStcBxHaOzdZUz+5MMszMZMxsgft7ERONp1E7P30XqeKx2"
             "zottjDSNTIWtk6rmsUDp2vuL/0SodiIlOuqzWOBt8wYw+q/mF2XSlkfEIbhWAA"
             "AAAElFTkSuQmCC"))

;;; さくらんぼ。16x16
(def dataurl-cherry16x16
  (util/str* "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf"
             "8/9hAAAAZElEQVQ4jWNgoCL4j4ZJ07y9URsFk2oQhkZyDIJrRPYKmkHEuwZJA3"
             "rYEDQIwxWvjZRRMCFDULyCrpmQIVgVEmsAXttg7P///5NvAEwz0QYg20ay/7HY"
             "RlQ0khxlWA0h1jaqAACvLONemBcg4QAAAABJRU5ErkJggg=="))

;;; 葉。16x16
(def dataurl-a
  (util/str* "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQAgMAAABi"
             "nRfyAAAADFBMVEVHcExRkBMAYhhtqRE0DJ/fAAAAAXRSTlMAQObYZgAAAEBJRE"
             "FUCNdjYEAA9qgJDAyxyxkYmG/VAHnpJQwM/NMDGBh4tzswMMiVAlWYAYUYJIFC"
             "DNJAIQZOkC42EMEEMwMAlKIIPdyq1roAAAAASUVORK5CYII="))

;;; プリン。16x16
(def dataurl-pudding16x16
  (util/str* "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQAgMAAABi"
             "nRfyAAAADFBMVEVHcEz/2QCiIgD4mQCvKmLyAAAAAXRSTlMAQObYZgAAADpJRE"
             "FUCNdjYEAArgUMDEyrVjUwMGatdGBgDA11YGANDQ1AIkRDQ0OQCNPQ0BgG/tDQ"
             "DwwM//9DzAAAJ4wP2HtEC58AAAAASUVORK5CYII="))

;;; p8金髪緑服。11x15。
(def dataurl-p8-elf
  (util/str* "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAsAAAAPCAYAAAAy"
             "PTUwAAAAe0lEQVQoka2QwQmAMAxFM1fFHXoxOIKDdBBPnoRO0hkcoEcvuZTvSb"
             "GYBkEDj1zeD0mIPhQUdLHk8YEWUMVWACWP2JeuwpT9KpAUISnCr2LLp3hiyi7w"
             "tYILbB85bUOFC6yLkiJc4ApJsfnrV1Oba1gyERFm7nHvv8lmHV7o1UPbOatgAA"
             "AAAElFTkSuQmCC"))






;;; TODO: singletonなのはよくないが…どうやって状態を回し持つ？
;;;       普通にやるとしたら、lifecycleを即値mapで返すのではなく、
;;;       generatorで返すようにする(＝何個でも生成できる)
;;;       しかないのでは？もうちょっと考える…
(def a-state (atom {}))


(defn- preload! []
  ;; TODO: きちんとロード完了を監視すべき
  (util/load-audio! :se/caret-psg
                    :se/submit-psg
                    :se/lvup-midi
                    :se/yarare-psg
                    :bgm/bsbs__LE661501
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
  (util/set-property! (util/search-child (:root @a-state) :title-layer)
                      :visible false)
  (util/set-property! (util/search-child (:root @a-state) :debug-ui-layer)
                      :visible true)
  ;; TODO
  (js/window.setTimeout (fn []
                          ;; TODO
                          ;(emit-gameover!)
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
                         :name (name :version-label))
   (util/set-properties! (util/make-label "タイトル未定"
                                          :font-size 96
                                          )
                         :anchor/x 0.5
                         :anchor/y 0.5
                         :x (* s-w 0.5)
                         :y (* s-h 0.25)
                         :name (name :title-caption))
   (util/set-properties! (pixi/Sprite.from dataurl-p8-elf)
                         :anchor/x 0.5
                         :anchor/y 0.5
                         :scale/x 8
                         :scale/y 8
                         :x (* s-w 0.5)
                         :y (* s-h 0.5)
                         :name (name :start-button))
   (util/set-properties! (util/make-button "START"
                                           emit-start!
                                           :padding 64
                                           )
                         :x (* s-w 0.5)
                         :y (* s-h 0.8)
                         :name (name :start-button))
   ])


(defn- make-debug-ui-layer [s-w s-h]
  (let [speed-label (util/set-properties! (util/make-label ""
                                                           :font-size 24
                                                           :align "left"
                                                           )
                                          :anchor/x 0
                                          :anchor/y 0
                                          :x 8
                                          :y 8
                                          :name (name :speed-label))
        upd! #(let [
                    {:keys [s-spd-x s-spd-y s-spd-z s-spd-yaw s-spd-pitch]} @a-state
                    text (str "　　　前進速度："
                              (js/Math.round (* s-spd-z 1000))
                              "\n"
                              "　　縦旋回速度："
                              s-spd-pitch
                              "\n"
                              "　　横旋回速度："
                              s-spd-yaw
                              "\n"
                              "縦水平移動速度："
                              s-spd-y
                              "\n"
                              "横水平移動速度："
                              s-spd-x
                              "\n"
                              )
                    ]
                (util/set-properties! speed-label :text text))
        b (fn [label xr yr f]
            (util/set-properties! (util/make-button label
                                                    (fn [b]
                                                      (util/se! :se/submit-psg)
                                                      (f)
                                                      (upd!))
                                                    :padding 16)
                                  :x (* s-w xr)
                                  :y (* s-h yr)))]
    (upd!)
    [:debug-ui-layer
     {:visible false}
     speed-label
     (b "前進" 0.45 0.7 #(swap! a-state update :s-spd-z (partial + 0.001)))
     (b "後退" 0.45 0.8 #(swap! a-state update :s-spd-z (partial + -0.001)))
     (b "↑旋回" 0.15 0.6 #(swap! a-state update :s-spd-pitch (partial + -1)))
     (b "↓旋回" 0.15 0.7 #(swap! a-state update :s-spd-pitch (partial + 1)))
     (b "←旋回" 0.15 0.8 #(swap! a-state update :s-spd-yaw (partial + -1)))
     (b "→旋回" 0.15 0.9 #(swap! a-state update :s-spd-yaw (partial + 1)))
     (b "↑水平移動" 0.8 0.6 #(swap! a-state update :s-spd-y (partial + -1)))
     (b "↓水平移動" 0.8 0.7 #(swap! a-state update :s-spd-y (partial + 1)))
     (b "←水平移動" 0.8 0.8 #(swap! a-state update :s-spd-x (partial + -1)))
     (b "→水平移動" 0.8 0.9 #(swap! a-state update :s-spd-x (partial + 1)))
     ]))


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
   [:main-layer]
   [:front-layer]
   (make-gameover-layer s-w s-h)
   [:ui-layer
    ;; TODO: 何を置くかは未定
    ]
   (make-debug-ui-layer s-w s-h)
   (make-title-layer s-w s-h)
   [:effect-layer]])

(defn- create! [app]
    (swap! a-state
           assoc
           :s-spd-x 0
           :s-spd-y 0
           :s-spd-z 0.001
           :s-spd-yaw 0
           :s-spd-pitch 0
           )
  (let [^js renderer (:renderer app)
        s-w (.-width renderer)
        s-h (.-height renderer)
        ^js stage (util/build-container-tree (make-tree s-w s-h))
        ]
    (preload!) ; TODO: これはlifecycle側に組み込むべき
    (util/bgm! nil)
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
  (let [root (:root @a-state)
        ]
    (when-let [layer (util/get-child root :background-space-layer)]
      (space/destroy! layer))
    (util/dea! root))
  (reset! a-state {}))

(defn- tick! [app delta-msec]
  (effect/tick! delta-msec)
  (let [root (:root @a-state)
        ]
    (when-let [layer (util/get-child root :background-space-layer)]
      ;; 状況に応じて速度を変更したりする
      (let [
            {:keys [s-spd-x s-spd-y s-spd-z s-spd-yaw s-spd-pitch]} @a-state
            ;; この移動量はカメラ側のもの。風景側は逆方向に動く事になる
            move-x (* delta-msec s-spd-x)
            move-y (* delta-msec s-spd-y)
            move-z (* delta-msec s-spd-z)
            yaw (* delta-msec s-spd-yaw)
            pitch (* delta-msec s-spd-pitch)
            ]
        (space/update! layer move-x move-y move-z yaw pitch)))
    ;; TODO
    ))



(def lifecycle
  {:create create!
   :tick tick!
   :destroy destroy!})




(ns cac2020.space
  (:refer-clojure :exclude [update!])
  (:require ["pixi.js" :as pixi]
            [cac2020.util :as util :include-macros true]
            [cac2020.dataurl :as dataurl]
            ))

;;; 星空表示用
;;; TODO: matrixを使うようにしたい






(defn- calc-s [dist apoapse]
  (let [s-min 0.000001
        s-max 100000
        s (if (pos? dist)
            (/ apoapse dist)
            s-max)]
    ;; NB: sは比率算出にも使うので、0にはできない
    (max s-min (min s s-max))))


;;; ... nearest:a=0 ... near:a=1 ... far:a=1 ... farthest:a=0 ...
(defn- calc-a [dist nearest near far farthest]
  (cond
    (<= dist nearest) 0
    (<= farthest dist) 0
    (<= dist near) (/ (- dist nearest)
                      (- near nearest))
    (<= far dist) (/ (- farthest dist)
                     (- farthest far))
    :else 1))





(defn- respawn-star! [sightbox ^js sp x y dist]
  (let [{:keys [left right top bottom
                nearest near far farthest
                w h focus-x focus-y
                star-base-scale]} sightbox
        ;; TODO: 要調整
        dist (or dist (let [min-dist-rate 0.1]
                        (* far
                           (+ min-dist-rate
                              (* (- 1 min-dist-rate)
                                 (js/Math.sqrt (rand)))))))
        x (or x (+ (rand w) left))
        y (or y (+ (rand h) top))
        s (calc-s dist far) ; TODO
        f-x (- (/ (- x focus-x) s) focus-x)
        f-y (- (/ (- y focus-y) s) focus-y)
        sp-s (* star-base-scale s)]
    ;; NB: 再配置後すぐに画面外に消える星が目立つ(ちらつく)ので、再配置直後は
    ;;     非表示にする事とした
    (util/set-properties! sp
                          :x x
                          :y y
                          :z-index (- dist)
                          :scale/x sp-s
                          :scale/y sp-s
                          :alpha 0
                          :-dist dist
                          :-f-x f-x
                          :-f-y f-y
                          )))


(defn- move-star! [^js sp sightbox
                   camera-move-x camera-move-y camera-move-z
                   yaw pitch]
  (let [{:keys [left right top bottom
                nearest near far farthest
                w h focus-x focus-y
                star-base-scale]} sightbox
        old-dist (util/property sp :-dist)
        old-f-x (util/property sp :-f-x)
        old-f-y (util/property sp :-f-y)
        angle (/ old-dist far)
        yaw-move-x (* yaw angle)
        pitch-move-y (* pitch angle)
        new-f-x (- old-f-x camera-move-x yaw-move-x)
        new-f-y (- old-f-y camera-move-y pitch-move-y)
        new-dist (- old-dist camera-move-z)
        new-s (calc-s new-dist far) ; TODO
        new-x (+ focus-x (* new-s (+ focus-x new-f-x)))
        new-y (+ focus-y (* new-s (+ focus-y new-f-y)))
        out-of-direction (if (< farthest new-dist)
                           :far
                           (let [out-left (- left new-x)
                                 out-right (- new-x right)
                                 out-top (- top new-y)
                                 out-bottom (- new-y bottom)
                                 out-x-key (cond
                                             (pos? out-left) :left
                                             (pos? out-right) :right
                                             :else nil)
                                 out-y-key (cond
                                             (pos? out-top) :top
                                             (pos? out-bottom) :bottom
                                             :else nil)]
                             (cond
                               (not out-x-key) out-y-key
                               (not out-y-key) out-x-key
                               (< (max out-left out-right)
                                  (max out-top out-bottom)) out-y-key
                               :else out-x-key)))
        out-of-direction (if out-of-direction
                           out-of-direction
                           (when (< new-dist nearest)
                             :near))
        out-of-direction (if (and
                               (#{:left :right :top :bottom} out-of-direction)
                               (pos? camera-move-z)
                               (< (/ new-dist far) 0.5))
                           :near
                           out-of-direction)
        respawn-position (when out-of-direction
                           (case out-of-direction
                             :left :right
                             :right :left
                             :top :bottom
                             :bottom :top
                             :far :near
                             :near :far
                             (throw (ex-info "unknown out-of-direction"
                                             {:d out-of-direction}))))
        ]
    (case respawn-position
      nil (let [new-a (calc-a new-dist nearest near far farthest)
                sp-s (* star-base-scale new-s)]
            (util/set-properties! sp
                                  :x new-x
                                  :y new-y
                                  :z-index (- new-dist)
                                  :scale/x sp-s
                                  :scale/y sp-s
                                  :alpha new-a
                                  :-dist new-dist
                                  :-f-x new-f-x
                                  :-f-y new-f-y
                                  ))
      :near (let [side? (zero? (rand-int 2))]
              (respawn-star! sightbox
                             sp
                             (when side?
                               (if (zero? (rand-int 2)) left right))
                             (when-not side?
                               (if (zero? (rand-int 2)) top bottom))
                             nil))
      :far (respawn-star! sightbox sp nil nil farthest)
      :left (respawn-star! sightbox sp left nil nil)
      :right (respawn-star! sightbox sp right nil nil)
      :top (respawn-star! sightbox sp nil top nil)
      :bottom (respawn-star! sightbox sp nil bottom nil)
      :else (throw (ex-info "inknown respawn-position"
                            {:d respawn-position})))))




;;; カメラが右に動けば(camera-move-xが正なら)、全ての星が左に動く
;;; カメラが下に動けば(camera-move-yが正なら)、全ての星が上に動く
;;; カメラが正面に動けば(camera-move-zが正なら)、全ての星がカメラに向かってくる
;;; カメラが下に向きを変えれば(pitchが正なら)、全ての星が上に動く
;;; (左上原点なので「ピッチの値を上げる」と「下を向く」事になるのに注意)
;;; カメラが右に向きを変えれば(yawが正なら)、全ての星が左に動く
;;; カメラのロールはなし(layerのrotationで実現可能なので)
(defn update!
  [^js layer camera-move-x camera-move-y camera-move-z yaw pitch]
  (let [sightbox (util/property layer :-sightbox)
        old-camera-x (util/property layer :-camera-x)
        old-camera-y (util/property layer :-camera-y)
        old-camera-z (util/property layer :-camera-z)
        new-camera-x (+ old-camera-x camera-move-x)
        new-camera-y (+ old-camera-y camera-move-y)
        new-camera-z (+ old-camera-z camera-move-z)
        {:keys [left right top bottom
                nearest near far farthest
                w h focus-x focus-y
                star-base-scale]} sightbox
        ^js children (.-children layer)
        ]
    (dotimes [i (alength children)]
      (let [^js sp (aget children i)]
        ;; TODO: ランダム星以外もこのレイヤに置く場合はここに分岐を追加
        (assert (= ::star (util/property sp :-type)))
        (move-star! sp sightbox
                    camera-move-x camera-move-y camera-move-z
                    yaw pitch)))
    (util/set-properties! layer
                          :-camera-x new-camera-x
                          :-camera-y new-camera-y
                          :-camera-z new-camera-z)))




;;; TODO: 最適化が必要(不要な要素を外し、キャッシュを多用したい要素を追加)
(defn make-sightbox [s-left s-right s-top s-bottom
                      z-nearest z-near z-far z-farthest
                      & [star-base-scale]]
  (assert (<= z-nearest z-near))
  (assert (<= z-near z-far))
  (assert (<= z-far z-farthest))
  (assert (< 0 z-far))
  {:left s-left
   :right s-right
   :top s-top
   :bottom s-bottom
   :nearest z-nearest
   :near z-near
   :far z-far
   :farthest z-farthest
   :w (- s-right s-left)
   :h (- s-bottom s-top)
   :focus-x (/ (+ s-left s-right) 2)
   :focus-y (/ (+ s-top s-bottom) 2)
   :star-base-scale (or star-base-scale 0.25)
   })


;;; TODO: make-sightboxと統合する？
(defn make
  [sightbox & args]
  (let [opts (util/args->map args)
        {:keys [star-quantity
                camera-x camera-y camera-z
                dont-sort?
                star-textures
                colorful-ratio]
         :or {star-quantity 64
              camera-x 0
              camera-y 0
              camera-z 0
              star-textures [dataurl/sphere]
              colorful-ratio 0.5}} opts
        {:keys [left right top bottom
                nearest near far farthest
                w h focus-x focus-y
                star-base-scale]} sightbox
        ^js layer (pixi/Container.)
        random-max (max 1 (min 0x100 (js/Math.round (* 0x100 colorful-ratio))))
        base-color (- 0x100 random-max)]
    (dotimes [i star-quantity]
      (let [^js sp (pixi/Sprite.from (rand-nth star-textures))
            tint (+ (* 0x10000 (+ base-color (rand-int random-max)))
                    (* 0x100 (+ base-color (rand-int random-max)))
                    (* 0x1 (+ base-color (rand-int random-max))))]
        (util/set-properties! sp
                              :anchor/x 0.5
                              :anchor/y 0.5
                              :tint tint
                              :-type ::star)
        (respawn-star! sightbox sp nil nil nil)
        (.addChild layer sp)))
    (util/set-properties! layer
                          :sortable-children (not dont-sort?)
                          :-sightbox sightbox
                          :-camera-x camera-x
                          :-camera-y camera-y
                          :-camera-z camera-z
                          )
    layer))


(defn destroy! [layer]
  (util/dea! layer))



(ns cac2020.space
  (:require ["pixi.js" :as pixi]
            [cac2020.util :as util :include-macros true]
            ))

;;; 星空表示用
;;; TODO: matrixを使うようにしたい


;;; 16x16を何倍にして標準サイズとするか
(def star-base-scale 0.25)



;;; distance field処理済の、中塗りされた円。16x16。
(def dataurl-sphere16x16
  (util/str* "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf"
             "8/9hAAAAvklEQVQ4ja3TMQrCQBCFYSHgCWxTew+xSGmTJhfwKlYinsQqR7DwJl"
             "apU30WjhBjXKNxYGBZ9v07b3Z2NksEMmSpM0OiOZYoUEUWsTf/JF6gxB41LpE1"
             "jgHLU+ItTmi8RhOg7QskvG5C3A6IH9EGpHqygxw7XBPibiUHLLuAddw+NmoU3f"
             "JLnL8AXMJGNh0QVaz8auFfTZz2jAODNFTJ+0HqQTZh5+Te2HOs91Kj3LOTuze2"
             "jFwb85newJLf+Qalm5IlQyJAsAAAAABJRU5ErkJggg=="))





(defn make-sightbox [s-left s-right s-top s-bottom
                      z-nearest z-near z-far z-farthest]
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
   :depth (- z-farthest z-nearest)
   })



(defn- respawn-star! [sightbox ^js sp x y dist]
  (let [{:keys [left right top bottom
                nearest near far farthest
                w h focus-x focus-y depth]} sightbox
        dist (or dist (+ (* depth (js/Math.sqrt (rand))) nearest))
        x (or x (+ (rand w) left))
        y (or y (+ (rand h) top))
        s (* star-base-scale
             (/ depth
                (- dist nearest)))
        s (max 0.000001 (min s 10000))
        a (cond
            (< dist near) (/ dist near)
            (< dist far) 1
            :else (/ (- farthest dist)
                     (- farthest far)))
        a (max 0 (min a 1))
        f-x (- (/ (- x focus-x) s) focus-x)
        f-y (- (/ (- y focus-y) s) focus-y)
        ]
    (util/set-properties! sp
                          :x x
                          :y y
                          :scale/x s
                          :scale/y s
                          :alpha a
                          :-dist dist
                          :-f-x f-x
                          :-f-y f-y
                          )))


(defn- move-star! [^js sp sightbox
                   camera-move-x camera-move-y camera-move-z
                   m-x-pow m-y-pow m-z-pow total-pow]
  (let [{:keys [left right top bottom
                nearest near far farthest
                w h focus-x focus-y depth]} sightbox
        old-dist (util/property sp :-dist)
        old-f-x (util/property sp :-f-x)
        old-f-y (util/property sp :-f-y)
        new-f-x (- old-f-x camera-move-x)
        new-f-y (- old-f-y camera-move-y)
        new-dist (- old-dist camera-move-z)
        new-s (* star-base-scale
                 (/ depth
                    (- new-dist nearest)))
        new-s (max 0.000001 (min new-s 10000))
        new-a (cond
                (< new-dist near) (/ new-dist near)
                (< new-dist far) 1
                :else (/ (- farthest new-dist)
                         (- farthest far)))
        new-a (max 0 (min new-a 1))
        new-x (+ focus-x (* new-s (+ focus-x new-f-x)))
        new-y (+ focus-y (* new-s (+ focus-y new-f-y)))
        out-of-direction (cond
                           (< farthest new-dist) :far
                           (< new-dist nearest) :near
                           (< new-x left) :left
                           (< right new-x) :right
                           (< new-y top) :top
                           (< bottom new-y) :bottom
                           :else nil)
        respawn-direction (when out-of-direction
                            (let [p (- (rand total-pow) m-z-pow)]
                              (cond
                                (neg? p) (if (= :far out-of-direction)
                                           :near
                                           :far)
                                (< p m-x-pow) (if (pos? camera-move-x)
                                                :right
                                                :left)
                                :else (if (pos? camera-move-y)
                                        :bottom
                                        :top))))]
    (case respawn-direction
      nil (util/set-properties! sp
                                :x new-x
                                :y new-y
                                :scale/x new-s
                                :scale/y new-s
                                :alpha new-a
                                :-dist new-dist
                                :-f-x new-f-x
                                :-f-y new-f-y
                                )
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
      :else (throw (ex-info "inknown respawn-direction"
                            {:d respawn-direction})))))


(defn- spawn-star! [sightbox]
  (let [{:keys [left right top bottom
                nearest near far farthest
                w h focus-x focus-y depth]} sightbox
        ^js sp (pixi/Sprite.from dataurl-sphere16x16)
        random-light 0x80
        base-light (- 0x100 random-light)
        tint (+ (* 0x10000 (+ base-light (rand-int random-light)))
                (* 0x100 (+ base-light (rand-int random-light)))
                (* 0x1 (+ base-light (rand-int random-light))))]
    (util/set-properties! sp
                          :anchor/x 0.5
                          :anchor/y 0.5
                          :tint tint
                          :-type ::star)
    (respawn-star! sightbox sp nil nil nil)
    sp))


(defn update-background-space-layer!
  [^js layer camera-move-x camera-move-y camera-move-z
   & [pitching yawing]]
  (let [pitching (or pitching 0)
        yawing (or yawing 0)
        sightbox (util/property layer :-sightbox)
        old-camera-x (util/property layer :-camera-x)
        old-camera-y (util/property layer :-camera-y)
        old-camera-z (util/property layer :-camera-z)
        new-camera-x (+ old-camera-x camera-move-x)
        new-camera-y (+ old-camera-y camera-move-y)
        new-camera-z (+ old-camera-z camera-move-z)
        {:keys [left right top bottom
                nearest near far farthest
                w h focus-x focus-y depth]} sightbox
        ;; TODO: pitching / yawing の値も考慮する事
        m-x-pow (js/Math.abs (/ camera-move-x w))
        m-y-pow (js/Math.abs (/ camera-move-y h))
        m-z-pow (* 10 (js/Math.abs (/ camera-move-z depth)))
        total-pow (+ m-x-pow m-y-pow m-z-pow)
        ^js children (.-children layer)
        ]
    (dotimes [i (alength children)]
      (let [^js sp (aget children i)]
        (when-not (zero? pitching)
          ;; TODO: pitching への対応
          )
        (when-not (zero? yawing)
          ;; TODO: yawing への対応
          )
        ;; TODO: ランダム星以外もこのレイヤに置く場合はここに分岐を追加
        (assert (= ::star (util/property sp :-type)))
        (move-star! sp sightbox
                    camera-move-x camera-move-y camera-move-z
                    m-x-pow m-y-pow m-z-pow total-pow)))
    (util/set-properties! layer
                          :-camera-x new-camera-x
                          :-camera-y new-camera-y
                          :-camera-z new-camera-z)))


(defn make-background-space-layer
  [sightbox camera-x camera-y camera-z & [star-quantity]]
  (let [{:keys [left right top bottom
                nearest near far farthest
                w h focus-x focus-y depth]} sightbox
        ^js layer (pixi/Container.)
        star-quantity (or star-quantity 64)
        ]
    (dotimes [i star-quantity]
      (.addChild layer (spawn-star! sightbox)))
    (util/set-properties! layer
                          :-sightbox sightbox
                          :-camera-x camera-x
                          :-camera-y camera-y
                          :-camera-z camera-z
                          )
    layer))






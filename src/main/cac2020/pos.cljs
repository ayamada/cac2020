(ns cac2020.pos
  (:refer-clojure :exclude [pos? set!]))

;;; 本当は (pixi/Pointer.) を使いたいが、
;;; pixiに依存したくはないレイヤがそここあるので、
;;; 自前で類似のオブジェクトを用意する
(defn make [x y]
  (assert (number? x))
  (assert (number? y))
  (array ::pos x y))

(defn ->x [^js pos]
  (when pos
    (assert (= ::pos (aget pos 0)))
    (aget pos 1)))

(defn ->y [^js pos]
  (when pos
    (assert (= ::pos (aget pos 0)))
    (aget pos 2)))

(defn set-x! [^js pos x]
  (assert (= ::pos (aget pos 0)))
  (aset pos 1 x)
  pos)

(defn set-y! [^js pos y]
  (assert (= ::pos (aget pos 0)))
  (aset pos 2 y)
  pos)

(defn set! [^js pos x y]
  (assert (= ::pos (aget pos 0)))
  (aset pos 1 x)
  (aset pos 2 y)
  pos)





(defn pos? [^js pos]
  (= ::pos (aget pos 0)))

(ns cac2020.effect
  (:require [clojure.string :as string]
            ["pixi.js" :as pixi]
            [cac2020.util :as util :include-macros true]
            ))


;;; - とりあえず派手に爆発とかさせるなら、簡易エフェクトエンジンを実装すべきでは？
;;;     - これがあればボタンにエフェクトもつけられるし
;;;     - 設計しましょう
;;;         - どこかからtick!をもらう必要あり
;;;             - 今回は自前でtick!をリレーしてくる形で(delta-msecも)
;;;         - progress算出する
;;;         - 今回はシーン切り替えないので
;;;         - パーティクルエミッタも考えない。単体パーティクルは考える
;;;         - progress終了ハンドルなし(全て勝手にdea!するのみ)
;;;         - ...


(defonce ^js all-entries (array))


(defn tick! [delta-msec]
  (let [
        ]
    ;; TODO
    ))


(defn register! [^js parent ^js dobj ttl-msec handle]
  (when parent
    (.addChild parent dobj))
  (let [^js entry (js-obj)
        ]
    ;; TODO
    (.push all-entries entry)
    nil))



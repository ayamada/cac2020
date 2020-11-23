(ns cac2020.game
  (:require [clojure.string :as string]
            ["pixi.js" :as pixi]
            ["va5" :as va5]
            [cac2020.util :as util]
            ))


;    - もぐらたたき
;        - 入力にpixi.interactionを使えるので楽
;        - グラフィックの用意は必要
;        - これでいいと思う
;        - フルーツがニョキッと出てくるのをゲット、ただしゴリラはゲットしない、みたいなのでよいのでは？
;            - ゲット後はネコゴのようにスコア部へと飛んでいく形式




(defn- create! [app]
  (let [^js stage (pixi/Container.)
        ]
    ;; TODO
    (.addChild stage
               (util/set-properties! (util/sp16x16)
                                     :width 256
                                     :height 256
                                     :tint 0xFF0000))
    (prn 'create!)
    (prn :version util/VERSION)
    stage))

(defn- destroy! [app]
  (prn 'destroy!)
  ;; TODO
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




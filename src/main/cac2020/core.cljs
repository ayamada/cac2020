(ns cac2020.core
  (:require [clojure.string :as string]
            ["pixi.js" :as pixi]
            [cac2020.game :as game]
            [cac2020.util :as util :include-macros true]
            ))



(def screen-w 960)
(def screen-h 960)

(def bg-url "bg.png")


;;; TODO: singleton実装なのはよくない、なおしたい

(defonce a-state (atom {}))

(defn- graft-true! [game-lifecycle]
  (swap! a-state assoc :can-tick? true)
  (when-let [^js loading-layer (:loading-layer @a-state)]
    (util/set-property! loading-layer :visible false))
  (when-let [^js root (:root-container @a-state)]
    (.removeChildren root))
  (let [^js root (:root-container @a-state)]
    (when-let [create-fn (:create game-lifecycle)]
      (when-let [^js game-stage (create-fn @a-state)]
        (.addChild root game-stage))))
  nil)

(defn- graft! [game-lifecycle]
  (swap! a-state assoc :game-lifecycle game-lifecycle :can-tick? false)
  (when-let [^js loading-layer (:loading-layer @a-state)]
    (util/set-property! loading-layer :visible true))
  (if-let [preload-fn (:preload (:game-lifecycle @a-state))]
    (let [cont #(graft-true! game-lifecycle)]
      (preload-fn @a-state cont))
    (graft-true! game-lifecycle))
  nil)

(defn- stub! []
  ;; TODO: もしまだ:preload中なら、キャンセルする必要がある、本当は
  (when-let [destroy-fn (:destroy (:game-lifecycle @a-state))]
    (destroy-fn @a-state))
  (when-let [^js root (:root-container @a-state)]
    (.removeChildren root))
  (swap! a-state dissoc :game-lifecycle :can-tick?)
  nil)

(defn- tick! [delta-frames]
  (when (:can-tick? @a-state)
    (when-let [tick-fn (:tick (:game-lifecycle @a-state))]
      (tick-fn @a-state delta-frames))))



(defn- add-bg-image! []
  (when bg-url
    (let [bg-image (str "url('" bg-url "')")]
      (set! js/document.body.style.backgroundImage bg-image)
      (set! js/document.documentElement.style.backgroundImage bg-image))))



(defn- make-loading-layer [s-w s-h]
  (let [c (pixi/Container.)
        bg (util/set-property! (util/sp16x16)
                               :width (+ s-w 16)
                               :height (+ s-h 16)
                               :anchor/x 0.5
                               :anchor/y 0.5
                               :x (/ s-w 2)
                               :y (/ s-h 2)
                               :tint 0x000000
                               )
        label (util/set-property! (util/make-label "LOADING"
                                                   :font-family "serif"
                                                   :font-size 128
                                                   :padding 32
                                                   :fill 0x3F3F3F
                                                   :stroke 0x0F0F0F
                                                   :stroke-thickness 8
                                                   )
                                  :anchor/x 0.5
                                  :anchor/y 0.5
                                  :x (/ s-w 2)
                                  :y (/ s-h 2)
                                  )
        ]
    (.addChild c bg)
    (.addChild c label)
    c))



(defn- bootstrap-stage2! [js-args]
  (util/init-audio!)
  (util/terraform!)
  (let [canvas-id "main-display"
        ^js app (pixi/Application. #js {"width" screen-w
                                        "height" screen-h})
        ^js renderer (.-renderer app)
        ^js ticker (.-ticker app)
        ^js stage (.-stage app)
        ^js canvas (.-view renderer)
        ^js root-container (pixi/Container.)
        ^js loading-layer (make-loading-layer screen-w screen-h)]
    (set! (.-id canvas) canvas-id)
    (.appendChild js/document.body canvas)
    (add-bg-image!)
    (util/setup-canvas! canvas)
    (.addChild stage root-container)
    (.addChild stage loading-layer)
    (swap! a-state
           assoc
           :canvas canvas
           :renderer renderer
           :ticker ticker
           :stage stage
           :root-container root-container
           :loading-layer loading-layer
           )
    (.add ticker tick!)
    (graft! game/lifecycle)))

(defn- bootstrap-stage1! [js-args]
  (pixi/utils.skipHello)
  (aset pixi/settings "FAIL_IF_MAJOR_PERFORMANCE_CAVEAT" false)
  (if-not (pixi/utils.isWebGLSupported)
    (util/fatal! "ブラウザのWebGL機能を有効にしてください")
    (bootstrap-stage2! js-args)))


(defn init []
  (aset js/window "cac2020version" util/VERSION)
  (aset js/window "cac2020bootstrap" (fn [& js-args]
                                       (bootstrap-stage1! js-args))))






(defn- ^:dev/before-load stop! []
  (try
    (stub!)
    (catch :default e
      (js/console.log e))))

(defn- ^:dev/after-load start! []
  (try
    (graft! game/lifecycle)
    (catch :default e
      (js/console.log e))))


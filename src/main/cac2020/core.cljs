(ns cac2020.core
  (:require [clojure.string :as string]
            ["pixi.js" :as pixi]
            [cac2020.game :as game]
            [cac2020.util :as util]
            ))



(def screen-w 960)
(def screen-h 960)



;;; TODO: singleton実装なのはよくない、なおしたい

(defonce a-state (atom {}))

(defn- graft! [game-lifecycle]
  (swap! a-state assoc :game-lifecycle game-lifecycle)
  (when-let [^js root (:root-container @a-state)]
    (.removeChildren root))
  (let [^js root (:root-container @a-state)]
    (when-let [create-fn (:create game-lifecycle)]
      (when-let [^js stage (create-fn @a-state)]
        (.addChild root stage))))
  nil)

(defn- stub! [game-lifecycle]
  (when-let [destroy-fn (:destroy (:game-lifecycle @a-state))]
    (destroy-fn @a-state))
  (when-let [^js root (:root-container @a-state)]
    (.removeChildren root))
  (swap! a-state dissoc :game-lifecycle)
  nil)

(defn- tick! [delta-msec]
  (when-let [tick-fn (:tick (:game-lifecycle @a-state))]
    (tick-fn @a-state delta-msec)))






(defn- bootstrap-stage2! [js-args]
  (util/init-audio!)
  (util/terraform!)
  (let [canvas-id "main-display"
        ^js app (pixi/Application. #js {"width" screen-w
                                        "height" screen-h})
        ^js renderer (.-renderer app)
        ^js ticker (.-ticker app)
        ^js canvas (.-view renderer)]
    (set! (.-id canvas) canvas-id)
    (.appendChild js/document.body canvas)
    (util/setup-canvas! canvas)
    (swap! a-state
           assoc
           :canvas canvas
           :renderer renderer
           :ticker ticker
           :root-container (.-stage app)
           ;:screen (.-screen app)
           ;:loader (.-loader app)
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
  (stub! game/lifecycle)
  (js/console.log "stubbed by hot-reloading"))

(defn- ^:dev/after-load start! []
  (graft! game/lifecycle)
  (js/console.log "grafted by hot-reloading"))


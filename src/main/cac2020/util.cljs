(ns cac2020.util
  (:require [clojure.string :as string]
            [cac2020.property :as p :include-macros true]
            ["pixi.js" :as pixi]
            ["va5" :as va5]
            [cac2020.pixi.util :as putil]
            [cac2020.dataurl :as dataurl]
            [cac2020.dom :as dom]
            [cac2020.tween :as tween]
            ))





(goog-define VERSION "0.0.0-FAILED_AUTOGENERATE")




(defn args->map [target-args]
  (case (count target-args)
    0 nil
    1 (first target-args)
    (apply hash-map target-args)))








(defn spy [v]
  (when ^boolean js/goog.DEBUG
    (prn :spy v))
  v)







(def vibrate!
  (if js/window.navigator.vibrate
    (fn [msec]
      (js/window.navigator.vibrate msec))
    (fn [msec] nil)))






;;; アツマール向け
(def ^js atsumaru (aget js/window "RPGAtsumaru"))

(defn- take-screenshot! [^js renderer stage mime-type quality]
  (js/Promise. (fn [res rej]
                 (let [w (.-width renderer)
                       h (.-height renderer)
                       ^js render-texture (pixi/RenderTexture.create w h)
                       _ (.render renderer stage render-texture)
                       dataurl (.base64 (.. renderer -plugins -extract)
                                        render-texture mime-type quality)]
                   (.destroy render-texture true)
                   (res dataurl)))))

(defn enable-atsumaru-screenshot! [renderer stage & [mime-type quality]]
  (when atsumaru
    (let [mime-type (or mime-type "image/jpeg")]
      (.setScreenshotHandler (.. atsumaru -screenshot)
                             #(take-screenshot! renderer
                                                stage
                                                mime-type
                                                quality)))))

(defn atsumaru-ui-volume []
  (if atsumaru
    (.getCurrentValue (.-volume atsumaru))
    1))

(defn subscribe-change-atsumaru-ui-volume! [f]
  (when atsumaru
    (.subscribe (.. atsumaru -volume -changed) f)))

(defn invoke-atsumaru-ranking! [board-id score]
  (when atsumaru
    (.then (.setRecord (.. atsumaru -scoreboards) board-id score)
           (fn [& _]
             (.then (.display (.. atsumaru -scoreboards) board-id)
                    (fn [e]
                      (js/window.console.log e)
                      nil)))
           (fn [e]
             (js/window.console.log e)
             nil))))







(def audio-dir "audio")
(defn resolve-audio-path [k]
  (if-not (or (keyword? k) (symbol? k))
    k
    (let [nsk (namespace k)
          nk (name k)]
      (if nsk
        (str audio-dir "/" nsk "/" nk ".m4a")
        (str audio-dir "/" nk ".m4a")))))

(defn load-audio! [& ks]
  (doseq [k ks]
    (when k
      (va5/load (resolve-audio-path k)))))

(defn bgm! [k & args]
  (if-not k
    (va5/stopBgm)
    (va5/bgm (resolve-audio-path k) (p/map->js-obj (args->map args)))))

(defn se! [k & args]
  (when k
    (va5/se (resolve-audio-path k) (p/map->js-obj (args->map args)))))

(defn make-play-se-periodically [interval-sec k & args]
  (va5/makePlaySePeriodically interval-sec
                              (resolve-audio-path k)
                              (p/map->js-obj (args->map args))))

(defn now-msec [] (va5/getNowMsec))

(defn init-audio! []
  (let [va5-debug? false]
    (va5/setConfig "is-output-error-log" va5-debug?)
    (va5/setConfig "is-output-debug-log" va5-debug?)
    (let [v (atsumaru-ui-volume)]
      (va5/setConfig "volume-master" v)
      (when va5-debug?
        (js/window.console.log "volume-master" v)))
    (va5/setConfig "is-pause-on-background" true)
    (va5/setConfig "is-unload-automatically-when-finished-bgm" true)
    (va5/init)
    (subscribe-change-atsumaru-ui-volume! #(va5/setConfig "volume-master" %))
    nil))








(defn stop-propagation! [^js e]
  (when (.-stopPropagation e)
    (.stopPropagation e)))


;;; effectは開始時にparentにaddChildし、完了後にdea!する機能のついたtween

(defn register-effect! [^js parent ^js dobj ttl-frames handle]
  (when parent
    (.addChild parent dobj))
  (tween/register! dobj ttl-frames handle putil/dea!))


















(defn make-label [label-string & args]
  (let [options (merge {:font-family "'Courier New', 'Courier', serif"
                        :fill 0xFFFFFF
                        :align "center"
                        :font-size 48
                        :padding 8
                        :stroke "rgba(0,0,0,0.5)"
                        :stroke-thickness 8
                        }
                       (args->map args))
        style (pixi/TextStyle. (p/map->js-obj options))
        label (pixi/Text. label-string style)]
    label))



(defonce a-need-button-effect? (atom false))
(defn cancel-button-effect! []
  (reset! a-need-button-effect? false))

(defn- button-effect-handle [^js sp progress]
  (let [start-w (p/get sp :--start-w)
        start-h (p/get sp :--start-h)
        delta-w (p/get sp :--delta-w)
        delta-h (p/get sp :--delta-h)]
    (p/set! sp
            :width (+ start-w (* delta-w progress))
            :height (+ start-h (* delta-h progress))
            :alpha (- 1 progress))
    nil))

(defn make-button [label-string click-handle & args]
  (let [options (merge {:anchor-x 0.5
                        :anchor-y 0.5
                        :border 2
                        :padding 16
                        :outline-color 0xFFFFFF
                        :bg-color 0x7F7FFF
                        }
                       (args->map args))
        {:keys [anchor-x anchor-y
                border padding
                outline-color bg-color fg-color
                label-options]} options
        label-options (merge {
                              ;; TODO
                              }
                             (when fg-color
                               {:fill fg-color})
                             (args->map label-options))
        ^js c (pixi/Container.)
        ^js label (make-label label-string label-options)
        _ (.updateText label true)
        label-w (.-width label)
        label-h (.-height label)
        inner-w (+ label-w padding padding)
        inner-h (+ label-h padding padding)
        outer-w (+ inner-w border border)
        outer-h (+ inner-h border border)
        ratio-x (- 0.5 anchor-x)
        ratio-y (- 0.5 anchor-y)
        center-x (* ratio-x outer-w)
        center-y (* ratio-y outer-h)
        ^js bg-inner (p/set! (putil/sp16x16)
                             :tint bg-color
                             :width inner-w
                             :height inner-h
                             :anchor/x 0.5
                             :anchor/y 0.5
                             :x center-x
                             :y center-y)
        ^js bg-outer (p/set! (putil/sp16x16)
                             :tint outline-color
                             :width outer-w
                             :height outer-h
                             :anchor/x 0.5
                             :anchor/y 0.5
                             :x center-x
                             :y center-y
                             :interactive true
                             :button-mode true)
        handle (fn [^js e]
                 (when (.-stopPropagation e)
                   (.stopPropagation e))
                 (reset! a-need-button-effect? true)
                 (when click-handle
                   (click-handle c))
                 (when @a-need-button-effect?
                   (let [sp (putil/sp16x16)
                         start-w outer-w
                         start-h outer-h
                         rate 1.5
                         end-w (* start-w rate)
                         end-h (* start-h rate)
                         delta-w (- end-w start-w)
                         delta-h (- end-h start-h)
                         ]
                     (p/set! sp
                             :anchor/x 0.5
                             :anchor/y 0.5
                             :x center-x
                             :y center-y
                             :--start-w start-w
                             :--start-h start-h
                             :--delta-w delta-w
                             :--delta-h delta-h
                             )
                     (register-effect! c sp 30 button-effect-handle))))
        ]
    (p/set! label
            :anchor/x 0.5
            :anchor/y 0.5
            :x center-x
            :y center-y
            )
    (.on bg-outer "mousedown" handle)
    (.on bg-outer "mouseup" stop-propagation!)
    (.on bg-outer "touchstart" handle)
    (.on bg-outer "touchend" stop-propagation!)
    (.addChild c bg-outer)
    (.addChild c bg-inner)
    (.addChild c label)
    c))







(defn effect-score! [^js parent score-text x y ttl-frames]
  (let [sp (make-label score-text
                       :font-family "'Courier New', 'Courier', serif"
                       :fill 0xFFFFFF
                       :align "center"
                       :font-size 48
                       :padding 8
                       ;:stroke "rgba(255,255,255,0.5)"
                       :stroke 0x7F7F7F
                       :stroke-thickness 8
                       )
        pp (tween/pp y (- y 128))
        h (fn [o p]
            (p/set! o
                    :alpha (Math/sqrt (- 1 p))
                    :y (tween/ap pp p)))
        done-h (fn [^js o]
                 (when-let [parent (.-parent o)]
                   (.removeChild parent o))
                 (.destroy o true)
                 nil)]
    (p/set! sp
            :anchor/x 0.5
            :anchor/y 1
            :scale/x 0.5
            :scale/y 1
            :x x
            )
    (.addChild parent sp)
    (tween/register! sp ttl-frames h done-h)
    nil))





(defn effect-smoke! [^js parent ttl-frames & args]
  (assert parent)
  (assert (pos? ttl-frames))
  (let [options (merge {:quantity 4
                        :size 128
                        :x 0
                        :y 0
                        :speed 1
                        :tex-fn dataurl/smoke
                        ;; TODO: もっとパラメータを設定できるようにする
                        }
                       (args->map args))
        {:keys [x y quantity size tex-fn speed]} options
        base-dist (* size speed)
        base-r (* (/ ttl-frames 150) 2 js/Math.PI)
        h (fn [^js sp progress]
            (when-not (putil/destroyed? sp)
              (p/set! sp
                      :x (+ (p/get sp :--start-x)
                            (* (p/get sp :--diff-x) progress))
                      :y (+ (p/get sp :--start-y)
                            (* (p/get sp :--diff-y) progress))
                      :rotation (+ (p/get sp :--start-r)
                                   (* (p/get sp :--diff-r)
                                      progress))
                      :alpha (- 1 progress))))]
    (dotimes [i quantity]
      (let [r (rand (* 2 js/Math.PI))
            diff-x (- (rand (* base-dist 2)) base-dist)
            diff-y (- (rand (* base-dist 2)) base-dist)
            diff-r (- (rand (* base-r 2)) base-r)
            sp (p/set! (putil/->sp (tex-fn))
                       :anchor/x 0.5
                       :anchor/y 0.5
                       :width size
                       :height size
                       :--start-x x
                       :--start-y y
                       :--start-r r
                       :--diff-x diff-x
                       :--diff-y diff-y
                       :--diff-r diff-r)]
        (register-effect! parent sp ttl-frames h)))))



(defn effect-nova! [^js parent ttl-frames x y & args]
  (assert parent)
  (assert (pos? ttl-frames))
  (let [options (merge {
                        :start-size 32
                        :end-size 128
                        :start-alpha 1
                        :end-alpha 0
                        :tex-fn #(putil/->tex :nova)
                        ;; TODO: もっとパラメータを設定できるようにする
                        }
                       (args->map args))
        {:keys [start-size end-size start-alpha end-alpha tex-fn]} options
        p-size (tween/pp start-size end-size)
        p-alpha (tween/pp start-alpha end-alpha)
        sp (p/set! (putil/->sp (tex-fn))
                   :anchor/x 0.5
                   :anchor/y 0.5
                   :x x
                   :y y
                   )
        ]
    (register-effect! parent
                      sp
                      ttl-frames
                      (fn [^js sp progress]
                        (when-not (putil/destroyed? sp)
                          (let [size (tween/ap p-size progress)]
                            (p/set! sp
                                    :width size
                                    :height size
                                    :rotation (rand (* 2 Math/PI))
                                    :alpha (tween/ap p-alpha progress))))))))



(defn sync-status-bar [^js bar rate]
  (let [fuel-sp (p/get bar :--fuel-sp)
        fuel-w (p/get bar :--fuel-w)
        rate (max 0 (min rate 1))]
    (p/set! fuel-sp :width (* fuel-w rate))
    (p/set! bar :--rate rate)
    bar))

;;; TODO: 右原点モード、上原点モード、下原点モードへの対応
(defn make-status-bar [& args]
  (let [options (merge {:w 128
                        :h 32
                        :anchor-x 0
                        :anchor-y 0
                        :outline-size 2
                        :empty-size 2
                        :outline-color 0xFFFFFF
                        :empty-color 0x000000
                        :fuel-color 0x9F9FFF
                        :initial-rate 1}
                       (args->map args))
        o-size (:outline-size options)
        e-size (:empty-size options)
        o-w (:w options)
        o-h (:h options)
        o-x (* o-w (:anchor-x options) -1)
        o-y (* o-h (:anchor-y options) -1)
        e-w (- o-w (* 2 o-size))
        e-h (- o-h (* 2 o-size))
        e-x (+ o-x o-size)
        e-y (+ o-y o-size)
        f-w (- e-w (* 2 e-size))
        f-h (- e-h (* 2 e-size))
        f-x (+ e-x e-size)
        f-y (+ e-y e-size)
        ^js bar (pixi/Container.)
        ^js outline-sp (putil/sp16x16)
        ^js empty-sp (putil/sp16x16)
        ^js fuel-sp (putil/sp16x16)]
    (.addChild bar outline-sp)
    (.addChild bar empty-sp)
    (.addChild bar fuel-sp)
    (p/set! outline-sp
            :anchor/x 0
            :anchor/y 0
            :width o-w
            :height o-h
            :x o-x
            :y o-y
            :tint (:outline-color options))
    (p/set! empty-sp
            :anchor/x 0
            :anchor/y 0
            :width e-w
            :height e-h
            :x e-x
            :y e-y
            :tint (:empty-color options))
    (p/set! fuel-sp
            :anchor/x 0
            :anchor/y 0
            :width f-w
            :height f-h
            :x f-x
            :y f-y
            :tint (:fuel-color options))
    (p/set! bar
            :--outline-sp outline-sp
            :--empty-sp empty-sp
            :--fuel-sp fuel-sp
            :--fuel-w f-w
            :--rate -1)
    (sync-status-bar bar (:initial-rate options))
    bar))

(defonce a-tex-defmap (atom {}))
(defn- deftex [k tex-key w h num-x num-y index quantity]
  (swap! a-tex-defmap assoc k [tex-key w h num-x num-y index quantity]))

(deftex :cube-w :cube3 32 32 8 8 0 18)
(deftex :cube-b :cube3 32 32 8 8 18 18)
(deftex :cube-c :cube3 32 32 8 8 36 18)
(deftex :flame1 :flame1 80 96 3 1 0 3)

(defn- -make-tex-array [k]
  (let [defs (@a-tex-defmap k)
        _ (assert defs (str "tex not defined " k))
        [tex-key w h num-x num-y index quantity] defs
        tex-src (putil/->tex tex-key)
        a (array)]
    (dotimes [i quantity]
      (let [true-i (+ index i)
            ^js t (.clone tex-src)
            ^js frame (.-frame t)
            x (* (mod true-i num-x) w)
            y (* (quot true-i num-x) h)]
        (p/set! frame
                :x x
                :y y
                :width w
                :height h)
        (.updateUvs t)
        (.push a t)))
    a))


(def make-tex-array (memoize -make-tex-array))

(defn make-cube-w-sp []
  (let [^js sp (p/set! (pixi/AnimatedSprite. (make-tex-array :cube-w))
                       :animation-speed 0.2
                       :anchor/x 0.5
                       :anchor/y 0.5)]
    (.play sp)
    sp))

(defn make-cube-b-sp []
  (let [^js sp (p/set! (pixi/AnimatedSprite. (make-tex-array :cube-b))
                       :animation-speed 0.2
                       :anchor/x 0.5
                       :anchor/y 0.5)]
    (.play sp)
    sp))

(defn make-cube-c-sp []
  (let [^js sp (p/set! (pixi/AnimatedSprite. (make-tex-array :cube-c))
                       :animation-speed 0.2
                       :anchor/x 0.5
                       :anchor/y 0.5)]
    (.play sp)
    sp))

(defn make-flame-sp [& [scale]]
  (let [scale (or scale 1)
        ^js sp (p/set! (pixi/AnimatedSprite. (make-tex-array :flame1))
                       :animation-speed 0.5
                       :anchor/x 0.5
                       :anchor/y 1
                       :scale/x scale
                       :scale/y (- scale))]
    (.play sp)
    sp))








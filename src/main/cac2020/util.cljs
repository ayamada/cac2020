(ns cac2020.util
  (:require [clojure.string :as string]
            [cac2020.property :as p]
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





(defn init-audio! []
  (let [va5-debug? false]
    (va5/setConfig "is-output-error-log" va5-debug?)
    (va5/setConfig "is-output-debug-log" va5-debug?))
  (va5/setConfig "volume-master" 0.6)
  (va5/setConfig "is-pause-on-background" true)
  (va5/setConfig "is-unload-automatically-when-finished-bgm" true)
  (va5/init)
  nil)






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
  (let [start-w (p/get sp :-start-w)
        start-h (p/get sp :-start-h)
        delta-w (p/get sp :-delta-w)
        delta-h (p/get sp :-delta-h)]
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
        bg-outer-x (* ratio-x outer-w)
        bg-outer-y (* ratio-y outer-h)
        bg-inner-x (* ratio-x inner-w)
        bg-inner-y (* ratio-y inner-h)
        label-x (* ratio-x label-w)
        label-y (* ratio-y label-h)
        ^js bg-inner (p/set! (putil/sp16x16)
                             :tint bg-color
                             :width inner-w
                             :height inner-h
                             :anchor/x 0.5
                             :anchor/y 0.5
                             :x bg-inner-x
                             :y bg-inner-y)
        ^js bg-outer (p/set! (putil/sp16x16)
                             :tint outline-color
                             :width outer-w
                             :height outer-h
                             :anchor/x 0.5
                             :anchor/y 0.5
                             :x bg-outer-x
                             :y bg-outer-y
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
                             :x bg-outer-x
                             :y bg-outer-y
                             :-start-w start-w
                             :-start-h start-h
                             :-delta-w delta-w
                             :-delta-h delta-h
                             )
                     (register-effect! c sp 30 button-effect-handle))))
        ]
    (p/set! label
            :anchor/x 0.5
            :anchor/y 0.5
            :x label-x
            :y label-y
            )
    (.on bg-outer "mousedown" handle)
    (.on bg-outer "mouseup" stop-propagation!)
    (.on bg-outer "touchstart" handle)
    (.on bg-outer "touchend" stop-propagation!)
    ;(.on bg-outer "mousemove" stop-propagation!)
    ;(.on bg-outer "touchmove" stop-propagation!)
    (.addChild c bg-outer)
    (.addChild c bg-inner)
    (.addChild c label)
    c))








(defn effect-smoke! [^js parent ttl-frames & args]
  (assert parent)
  (assert (pos? ttl-frames))
  (let [options (merge {:quantity 4
                        :size 128
                        :x 0
                        :y 0
                        ;; TODO: もっとパラメータを設定できるようにする
                        }
                       (args->map args))
        {:keys [x y quantity size]} options
        base-dist size
        base-r (* (/ ttl-frames 150) 2 js/Math.PI)
        h (fn [^js sp progress]
            (when-not (aget sp "_destroyed")
              (p/set! sp
                      :x (+ (p/get sp :-start-x)
                            (* (p/get sp :-diff-x) progress))
                      :y (+ (p/get sp :-start-y)
                            (* (p/get sp :-diff-y) progress))
                      :rotation (+ (p/get sp :-start-r)
                                   (* (p/get sp :-diff-r)
                                      progress))
                      :alpha (- 1 progress))))]
    (dotimes [i quantity]
      (let [r (rand (* 2 js/Math.PI))
            diff-x (- (rand (* base-dist 2)) base-dist)
            diff-y (- (rand (* base-dist 2)) base-dist)
            diff-r (- (rand (* base-r 2)) base-r)
            sp (p/set! (putil/->sp (dataurl/smoke))
                       :anchor/x 0.5
                       :anchor/y 0.5
                       :width size
                       :height size
                       :-start-x x
                       :-start-y y
                       :-start-r r
                       :-diff-x diff-x
                       :-diff-y diff-y
                       :-diff-r diff-r)]
        (register-effect! parent sp ttl-frames h)))))





(ns cac2020.util
  (:require [clojure.string :as string]
            #?(:cljs ["pixi.js" :as pixi])
            #?(:cljs ["va5" :as va5])
            ))


(defn kebab-string->camel-string [s]
  (assert (string? s))
  (let [s (name s)
        s (if-let [[_ m] (re-find #"^(.*)\?$" s)]
            (str "is-" m)
            s)
        s (string/replace s #"-." #(string/upper-case (subs % 1)))]
    s))

(defn name->camel-string [k]
  (if (or (keyword? k) (symbol? k))
    (kebab-string->camel-string (name k))
    k))


(defn args->map [target-args]
  (case (count target-args)
    0 nil
    1 (first target-args)
    (apply hash-map target-args)))



#?(:clj (do

;;; TODO: defmacro系をここに書く


          ))




#?(:cljs (do



(goog-define VERSION "0.0.0-FAILED_AUTOGENERATE")


(defn set-style! [^js dom & style-kvs]
  (let [^js style (.-style dom)]
    (loop [style-kvs style-kvs]
      (when-not (empty? style-kvs)
        (let [k (first style-kvs)
              v (second style-kvs)]
          ;; TODO: vにも何か補正をした方がよいが、css自体はkebab-caseであり "inline-block" のような値が入り得るので name->camel-string をかけるのはやめた方がよい。どうする？
          (aset style (name->camel-string k) v)
          (recur (nnext style-kvs)))))))


(defn fatal! [msg]
  (let [^js div (js/document.createElement "div")
        ^js span (js/document.createElement "span")]
    (while js/document.body.firstChild
      (js/document.body.removeChild js/document.body.firstChild))
    (set! (.-textContent span) msg)
    (set-style! span
                :display "inline-block"
                :font-weight "bold"
                :background-color "black"
                :color "white"
                :border "4px solid red"
                :margin "0"
                :padding "2em"
                :max-width "80%"
                :max-height "80%"
                :overflow-x "hidden"
                :overflow-y "auto"
                :font-family "serif")
    (set-style! div
                :position "absolute"
                :top "50%"
                :left "50%"
                :transform "translate(-50%,-50%)")
    (set-style! js/document.body :background-color "black")
    (.appendChild div span)
    (.appendChild js/document.body div)
    (throw (js/Error. msg))))




(defn merge-obj! [^js o m]
  (when o
    (doseq [[k v] m]
      (let [nsk (namespace k)
            nk (name k)
            o (if nsk
                (aget o (kebab-string->camel-string nsk))
                o)]
        (assert o)
        (aset o (kebab-string->camel-string nk) v)))
    o))


(defn set-properties! [o & args]
  (when o
    (if (= (count args) 1)
      (merge-obj! o (first args))
      (loop [kv-list args]
        (if (empty? kv-list)
          o
          (let [k (first kv-list)
                v (second kv-list)
                nsk (namespace k)
                nk (name k)
                o (if nsk
                    (aget o (kebab-string->camel-string nsk))
                    o)]
            (assert o)
            (aset o (kebab-string->camel-string nk) v)
            (recur (nnext kv-list))))))))

(def set-property! set-properties!)


(defn clj->js-obj [& args]
  (let [o (js-obj)]
    (merge-obj! o (args->map args))
    o))



(defn sp16x16 []
  (pixi/Sprite.from pixi/Texture.WHITE))



(defn build-container-tree [tree-src]
  (if-not (vector? tree-src)
    tree-src
    (let [^js c (pixi/Container.)
          c-name (first tree-src)
          c-name (when (or
                         (keyword? c-name)
                         (symbol? c-name)
                         (string? c-name))
                   (name c-name))
          children (if c-name
                     (next tree-src)
                     tree-src)]
      (when c-name
        (set-property! c :name c-name))
      (doseq [child children]
        (cond
          (nil? child) nil
          (map? child) (set-properties! c child)
          (or
            (keyword? child)
            (symbol? child)
            (string? child)
            (number? child)
            (boolean? child)) (throw (ex-info "found invalid child"
                                              {:child child}))
          (vector? child) (.addChild c (build-container-tree child))
          (coll? child) (doseq [child2 child]
                          (.addChild c (build-container-tree child2)))
          :else (.addChild c child)))
      c)))





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
        style (pixi/TextStyle. (clj->js-obj options))
        label (pixi/Text. label-string style)]
    label))



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
                outline-color bg-color
                label-options]} options
        label-options (merge {
                              ;; TODO
                              }
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
        ^js bg-inner (set-properties! (sp16x16)
                                      :tint bg-color
                                      :width inner-w
                                      :height inner-h
                                      :anchor/x 0.5
                                      :anchor/y 0.5
                                      :x bg-inner-x
                                      :y bg-inner-y)
        ^js bg-outer (set-properties! (sp16x16)
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
                 ;; TODO: エフェクトを出したい
                 (when (.-preventDefault e)
                   (.preventDefault e))
                 (when click-handle
                   (click-handle c)))
        ]
    (set-properties! label
                     :anchor/x 0.5
                     :anchor/y 0.5
                     :x label-x
                     :y label-y
                     )
    (.on bg-outer "mousedown" handle)
    (.on bg-outer "touchend" handle)
    (.addChild c bg-outer)
    (.addChild c bg-inner)
    (.addChild c label)
    c))


(defn get-child [^js root k] (.getChildByName root (name k)))
(defn search-child [^js root k] (.getChildByName root (name k) true))



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
    (va5/load (resolve-audio-path k))))

(defn bgm! [k & args]
  (if-not k
    (va5/stopBgm)
    (va5/bgm (resolve-audio-path k) (clj->js-obj args))))

(defn se! [k & args]
  (when k
    (va5/se (resolve-audio-path k) (clj->js-obj args))))





(defn init-audio! []
  (va5/setConfig "is-output-error-log" false)
  (va5/setConfig "is-output-debug-log" false)
  (va5/setConfig "volume-master" 0.6)
  (va5/setConfig "is-pause-on-background" true)
  (va5/setConfig "is-unload-automatically-when-finished-bgm" true)
  (va5/init)
  nil)




(defn terraform! []
  (set! js/document.documentElement.style.overflow "hidden")
  (set! js/document.body.style.overflow "hidden")
  (let [prevent-default! (fn [^js e]
                           (.preventDefault e)
                           false)
        focus-options #js {"preventScroll" true}
        prevent-default-with-focus! (fn [^js e]
                                      (.preventDefault e)
                                      (when-let [^js target (.-target e)]
                                        (when (.-focus target)
                                          (.focus target focus-options)))
                                      false)
        ]
    (aset js/window "oncontextmenu" prevent-default!)
    (doseq [k [:click :mousedown :mouseup
               :touchstart :touchend]]
      (.addEventListener js/document (name k) prevent-default-with-focus!))
    (doseq [k [:touchcancel]]
      (.addEventListener js/document (name k) prevent-default!))
    nil))


(defn setup-canvas! [^js canvas]
  (set-style! canvas
              :display "block"
              :position "fixed")
  (let [a-supervise-async! (atom nil)
        sleep-msec 1111
        anchor-x 0.5
        anchor-y 0.5
        style (.-style canvas)
        canvas-ar (/ (.-width canvas)
                     (.-height canvas))
        a-old-s-w (atom -1)
        a-old-s-h (atom -1)
        ;; NB: pixi標準のオートリサイズはcanvasの実サイズを変更するもので、
        ;;     canvas内の描画オブジェクトの座標系は変動しない問題がある。
        ;;     なのでcssだけいじってリサイズする。
        inspect-resize! (fn [& _]
                          (let [;; NB: iOSでは先にclientWidth等を無駄に
                                ;;     参照しないといけないらしい
                                _ (+ js/document.documentElement.clientWidth
                                     js/document.documentElement.clientHeight)
                                s-w js/window.innerWidth
                                s-h js/window.innerHeight]
                            (when (or
                                    (not= @a-old-s-w s-w)
                                    (not= @a-old-s-h s-h))
                              (reset! a-old-s-w s-w)
                              (reset! a-old-s-h s-h)
                              (let [s-ar (/ s-w s-h)
                                    margin-on-side? (< canvas-ar s-ar)
                                    css-w (if margin-on-side?
                                            (js/Math.round (* s-h canvas-ar))
                                            s-w)
                                    css-h (if margin-on-side?
                                            s-h
                                            (js/Math.round (/ s-w canvas-ar)))
                                    horizontal (if margin-on-side?
                                                 (- s-w css-w)
                                                 0)
                                    left (js/Math.round (* anchor-x horizontal))
                                    vertical (if margin-on-side?
                                               0
                                               (- s-h css-h))
                                    top (js/Math.round (* anchor-y vertical))]
                                (set! (.-width style) (str css-w "px"))
                                (set! (.-height style) (str css-h "px"))
                                (set! (.-left style) (str left "px"))
                                (set! (.-top style) (str top "px"))))
                            _))]
    (reset! a-supervise-async!
            (fn [& _]
              (js/window.setTimeout @a-supervise-async! sleep-msec)
              (inspect-resize!)))
    (@a-supervise-async!)
    (js/window.addEventListener "resize" inspect-resize!)
    (js/window.addEventListener "orientationchange" inspect-resize!)
    nil))


           ))

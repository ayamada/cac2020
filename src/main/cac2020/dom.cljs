(ns cac2020.dom
  (:require [clojure.string :as string]
            [cac2020.property :as p :include-macros true]
            ))


(defn set-style! [^js dom & style-kvs]
  (let [^js style (.-style dom)]
    (loop [style-kvs style-kvs]
      (when-not (empty? style-kvs)
        (let [k (first style-kvs)
              v (second style-kvs)]
          ;; TODO: vにも何か補正をした方がよいが、css自体はkebab-caseであり "inline-block" のような値が入り得るので name->camel-string をかけるのはやめた方がよい。どうする？
          (aset style (p/name->camel-string k) v)
          (recur (nnext style-kvs)))))))


(defn fatal! [msg & [dont-throw-exception?]]
  (let [^js layer (js/document.createElement "div")
        ^js div (js/document.createElement "div")
        ^js span (js/document.createElement "span")]
    (while js/document.body.firstChild
      (js/document.body.removeChild js/document.body.firstChild))
    (set! (.-textContent span) msg)
    (set-style! span
                :display "inline-block"
                :font-weight "bold"
                :margin "0"
                :padding "2em"
                :white-space "pre-line"
                :font-family "serif")
    (set-style! div
                :position "fixed"
                :max-width "98%"
                :max-height "98%"
                :background-color "black"
                :color "white"
                :border "4px solid red"
                :overflow-x "hidden"
                :overflow-y "auto"
                :top "50%"
                :left "50%"
                :transform "translate(-50%,-50%)")
    (set-style! layer
                :position "relative"
                :width "100%"
                :height "100%")
    (set-style! js/document.body :background-color "black")
    (.appendChild div span)
    (.appendChild layer div)
    (.appendChild js/document.body layer)
    (when-not dont-throw-exception?
      (throw (js/Error. msg)))))






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
                                        (when (and
                                                (.-focus target)
                                                (not (#{"HTML" "html"}
                                                       (.-tagName target))))
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










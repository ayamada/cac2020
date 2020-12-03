(ns cac2020.util
  #?(:cljs (:require-macros [cac2020.util :as m]))
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





(declare immediate-map?)

;;; 「引数が全て即値ならマクロ展開、一つでも変数やlazy-seq等を含むなら関数展開」
;;; を実装する際に使う判定関数
(defn immediate-value? [v]
  (cond
    (nil? v) true
    (true? v) true
    (false? v) true
    (keyword? v) true
    (string? v) true
    (number? v) true
    (symbol? v) false
    (vector? v) (every? immediate-value? v)
    (set? v) (every? immediate-value? v)
    (map? v) false ;(immediate-map? v) ; NB: 扱いがややこしいので除外する事に
    (coll? v) false
    :else true))

(defn immediate-map? [m]
  ;; immediate-mapの要件は「全てのkeyがimmediate-valueである事」。
  ;; (valはimmediate-valueである必要はない)
  (when (map? m)
    (every? immediate-value? (keys m))))


#?(:clj (do

(defmacro str* [& args]
  (assert (every? immediate-value? args))
  (apply str args))




(defmacro set-properties! [o & args]
  (assert (even? (count args)))
  (let [kv-list (partition 2 args)
        ok-list (filter (fn [[k v]] (keyword? k)) kv-list)
        ng-list (remove (fn [[k v]] (keyword? k)) kv-list)
        o' `o#
        ok-bodies (map (fn [[k v]]
                         (let [nsk-str (when-let [nsk (namespace k)]
                                         (kebab-string->camel-string nsk))
                               nk-str (kebab-string->camel-string (name k))]
                           (if nsk-str
                             `(when-let [o# (aget ~o' ~nsk-str)]
                                (aset o# ~nk-str ~v))
                             `(aset ~o' ~nk-str ~v))))
                       ok-list)
        ng-body (if (empty? ng-list)
                  o'
                  `(-set-properties! ~o' ~@(apply concat ng-list)))]
    `(let [~o' ~o]
       ~@ok-bodies
       ~ng-body)))



(defmacro set-property! [o & args] `(set-properties! ~o ~@args))



(defmacro property [o k]
  (if-not (keyword? k)
    `(-property ~o ~k)
    (let [nsk-str (when-let [nsk (namespace k)]
                    (kebab-string->camel-string nsk))
          nk-str (kebab-string->camel-string (name k))]
      (if nsk-str
        `(when-let [o# (aget ~o ~nsk-str)]
           (aget o# ~nk-str))
        `(aget ~o ~nk-str)))))






          ))




#?(:cljs (do



(goog-define VERSION "0.0.0-FAILED_AUTOGENERATE")




(defn spy [v]
  (when ^boolean js/goog.DEBUG
    (prn :spy v))
  v)









(defn -set-properties! [o & args]
  (when o
    (assert (even? (count args)))
    (loop [kv-list args]
      (when-not (empty? kv-list)
        (let [k (first kv-list)
              v (second kv-list)
              nsk (when (or (keyword? k) (symbol? k))
                    (namespace k))
              nk (name k)
              o (if nsk
                  (aget o (kebab-string->camel-string nsk))
                  o)]
          (assert o)
          (aset o (kebab-string->camel-string nk) v)
          (recur (nnext kv-list)))))
    o))


(defn -property [^js o k]
  (if (or (keyword? k) (symbol? k))
    (let [nsk (namespace k)
          nk (name k)
          o (if nsk
              (aget o (kebab-string->camel-string nsk))
              o)]
      (aget o (kebab-string->camel-string nk)))
    (aget o k)))


(defn merge-properties! [o m]
  (doseq [[k v] m]
    (-set-properties! o k v))
  o)

(defn map->js-obj [m]
  (let [o (js-obj)]
    (merge-properties! o m)
    o))







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
  (let [^js root (js/document.createElement "div")
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
    (set-style! root
                :position "relative"
                :width "100%"
                :height "100%")
    (set-style! js/document.body :background-color "black")
    (.appendChild div span)
    (.appendChild root div)
    (.appendChild js/document.body root)
    (throw (js/Error. msg))))







(def vibrate!
  (if js/window.navigator.vibrate
    (fn [msec]
      (js/window.navigator.vibrate msec))
    (fn [msec] nil)))


(declare destroy-them-all!)
(defn destroy-all-children! [^js dobj]
  (when dobj
    (let [^js children (.-children dobj)
          n (alength children)]
      (dotimes [i n]
        (let [i (- n i 1)
              ^js child (aget children i)]
          (destroy-them-all! child)))
      (.removeChildren dobj))))
(defn destroy-them-all! [^js dobj]
  (when (and dobj (not (aget dobj "_destroyed")))
    (when-let [parent (.-parent dobj)]
      (.removeChild parent dobj))
    (destroy-all-children! dobj)
    (.destroy dobj)))
(def dac! destroy-all-children!)
(def dta! destroy-them-all!)
(def dea! destroy-them-all!)









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
        (m/set-property! c :name c-name))
      (doseq [child children]
        (cond
          (nil? child) nil
          (map? child) (merge-properties! c child)
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


(defn traverse-container-tree! [container-tree indexer]
  (when container-tree
    (when-let [^js children (.-children container-tree)]
      (let [n (alength children)]
        (dotimes [i n]
          (let [i (- n i 1)
                ^js child (aget children i)]
            (traverse-container-tree! child indexer)))))
    (indexer container-tree)
    nil))

(defn index-container-tree! [container-tree a & [extra-namespace]]
  (let [kns (when extra-namespace
              (or
                (when (or
                        (keyword? extra-namespace)
                        (symbol? extra-namespace))
                  (namespace extra-namespace))
                (name extra-namespace)))
        f (fn [^js dobj]
            (when dobj
              (when-let [name-str (.-name dobj)]
                (let [k (keyword kns name-str)]
                  (assert (not (contains? @a k))
                          (str "found duplicated index key " k))
                  (swap! a assoc k dobj)))))]
    (traverse-container-tree! container-tree f)))





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
    (when k
      (va5/load (resolve-audio-path k)))))

(defn bgm! [k & args]
  (if-not k
    (va5/stopBgm)
    (va5/bgm (resolve-audio-path k) (map->js-obj (args->map args)))))

(defn se! [k & args]
  (when k
    (va5/se (resolve-audio-path k) (map->js-obj (args->map args)))))





(defn init-audio! []
  (va5/setConfig "is-output-error-log" false)
  (va5/setConfig "is-output-debug-log" false)
  (va5/setConfig "volume-master" 0.6)
  (va5/setConfig "is-pause-on-background" true)
  (va5/setConfig "is-unload-automatically-when-finished-bgm" true)
  (va5/init)
  nil)





(defn stop-propagation! [^js e]
  (when (.-stopPropagation e)
    (.stopPropagation e)))


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









;;; tweenは純粋に毎フレームでtarget-objを動かすだけ

(defonce ^js all-tween-entries (array))

(defn tick-tween! [delta-frames]
  (let [total (alength all-tween-entries)]
    (dotimes [i0 total]
      (let [i (- total i0 1)
            ^cljs entry (aget all-tween-entries i)
            target-obj (.-target-obj entry)
            now-frames (+ (.-now-frames entry) delta-frames)
            ttl-frames (.-ttl-frames entry)
            handle (.-handle entry)
            progress (max 0 (min (/ now-frames ttl-frames) 1))]
        (set! (.-now-frames entry) now-frames)
        (handle target-obj progress)
        (when (= 1 progress)
          (.splice all-tween-entries i 1))))
    nil))

(defn register-tween! [target-obj ttl-frames handle]
  (let [^cljs entry (js-obj)]
    (set! (.-target-obj entry) target-obj)
    (set! (.-now-frames entry) 0)
    (set! (.-ttl-frames entry) ttl-frames)
    (set! (.-handle entry) handle)
    (.push all-tween-entries entry)
    (handle target-obj 0)
    nil))









;;; effectは開始時にparentにaddChildし、完了後にdea!する機能のついたtween

(defn register-effect! [^js parent ^js dobj ttl-frames handle]
  (when parent
    (.addChild parent dobj))
  (let[h (fn [target-obj progress]
           (handle target-obj progress)
           (when (= 1 progress)
             (dea! target-obj)))]
    (register-tween! dobj ttl-frames h)))









(defn make-label [label-string & args]
  (let [options (merge {:font-family "'Courier New', 'Courier', serif"
                        :fill #js [0xFFFFFF]
                        :align "center"
                        :font-size 48
                        :padding 8
                        :stroke "rgba(0,0,0,0.5)"
                        :stroke-thickness 8
                        }
                       (args->map args))
        style (pixi/TextStyle. (map->js-obj options))
        label (pixi/Text. label-string style)]
    label))



(defonce a-need-button-effect? (atom false))
(defn cancel-button-effect! []
  (reset! a-need-button-effect? false))

(defn- button-effect-handle [^js sp progress]
  (let [start-w (m/property sp :-start-w)
        start-h (m/property sp :-start-h)
        delta-w (m/property sp :-delta-w)
        delta-h (m/property sp :-delta-h)]
    (m/set-properties! sp
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
        ^js bg-inner (m/set-properties! (sp16x16)
                                        :tint bg-color
                                        :width inner-w
                                        :height inner-h
                                        :anchor/x 0.5
                                        :anchor/y 0.5
                                        :x bg-inner-x
                                        :y bg-inner-y)
        ^js bg-outer (m/set-properties! (sp16x16)
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
                   (let [sp (sp16x16)
                         start-w outer-w
                         start-h outer-h
                         rate 1.5
                         end-w (* start-w rate)
                         end-h (* start-h rate)
                         delta-w (- end-w start-w)
                         delta-h (- end-h start-h)
                         ]
                     (m/set-properties! sp
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
    (m/set-properties! label
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




(def texture-dir "tex")
(defn resolve-texture-path [k]
  (if-not (or (keyword? k) (symbol? k))
    k
    (let [nsk (namespace k)
          nk (name k)]
      (if nsk
        (str texture-dir "/" nsk "/" nk ".png")
        (str texture-dir "/" nk ".png")))))

(defn ->tex [k] (pixi/Texture.from (resolve-texture-path k)))
(defn ->sp [k] (pixi/Sprite.from (resolve-texture-path k)))

(defn load-textures! [cont & ks]
  (if (empty? ks)
    (cont)
    (let [^js loader (pixi/Loader.)
          paths (map resolve-texture-path ks)]
      (doseq [path paths]
        (.add loader path path))
      (.add (.-onError loader)
            (fn [^js err _ ^js resource]
              (fatal! (str "ファイルのロードに失敗しました"
                           "\n"
                           ;(.-message err)
                           ;"\n"
                           (.-url resource)
                           ""))))
      (.load loader
             (fn [^js loader ^js resources]
               (let [ts (doall (map #(pixi/Texture.from %) paths))
                     a-done? (atom nil)]
                 (reset! a-done?
                         (fn []
                           (if (every? #(.-valid ^js %) ts)
                             (do
                               (reset! a-done? nil)
                               (cont))
                             (js/window.setTimeout @a-done?))))
                 (@a-done?)))))))





           ))

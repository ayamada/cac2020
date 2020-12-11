(ns cac2020.pixi.util
  (:require-macros [cac2020.macro :as m])
  (:require [clojure.string :as string]
            [cac2020.property :as p :include-macros true]
            ["pixi.js" :as pixi]
            [cac2020.dom :as dom]
            ))



(defn destroyed? [o] (aget o "_destroyed"))


(declare destroy-them-all!)
(defn destroy-all-children! [^js dobj]
  (when dobj
    (m/doseq-array-backward [child (.-children dobj)]
      (destroy-them-all! child))
    (.removeChildren dobj)))
(defn destroy-them-all! [^js dobj]
  (when dobj
    (destroy-all-children! dobj)
    (when-let [parent (.-parent dobj)]
      (.removeChild parent dobj))
    (when-not (destroyed? dobj)
      (.destroy dobj))))
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
        (set! (.-name c) c-name))
      (doseq [child children]
        (cond
          (nil? child) nil
          (map? child) (p/merge! c child)
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
      (m/doseq-array-backward [child children]
        (traverse-container-tree! child indexer)))
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



(defn get-child [^js container k] (.getChildByName container (name k)))
(defn search-child [^js container k] (.getChildByName container (name k) true))




;;; TODO: より分かりやすい名前にする
(defn calc-center-x [x w anchor-x] (+ x (* w (- 0.5 anchor-x))))
(def calc-center calc-center-x)
(def calc-center-y calc-center)












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
              (dom/fatal! (str "ファイルのロードに失敗しました"
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



(defn set-nearest! [^js sp-or-tex]
  (when sp-or-tex
    (let [^js tex (if (instance? pixi/Sprite sp-or-tex)
                    (.-texture sp-or-tex)
                    sp-or-tex)
          ^js btex (if (instance? pixi/Texture tex)
                     (.-baseTexture tex)
                     tex)]
      (assert (instance? pixi/BaseTexture btex))
      (set! (.-scaleMode btex) pixi/SCALE_MODES.NEAREST)
      (.update btex)
      sp-or-tex)))



;;; NB: safariでこれを機能させるには、base-texのオリジナルサイズが
;;;     power of twoでないといけないという強い制約がある、要注意
(defn set-wrap-mode! [^js tex mode]
  (assert (#{:clamp :repeat :mirrored-repeat} mode))
  (let [^js base-tex (p/get tex :base-texture)]
    (p/set! base-tex
            :wrap-mode
            (case mode
              :clamp pixi/WRAP_MODES.CLAMP
              :repeat pixi/WRAP_MODES.REPEAT
              :mirrored-repeat pixi/WRAP_MODES.MIRRORED_REPEAT))
    (.update base-tex)
    (.update tex)
    tex))






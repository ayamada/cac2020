(ns cac2020.tween
  (:require-macros [cac2020.macro :as m])
  (:require [clojure.string :as string]
            [cac2020.property :as p :include-macros true]
            ))




;;; TODO: tweenを一時停止する機能を提供



(defn prepare-parameter [start end]
  (assert (number? start))
  (assert (number? end))
  (let [diff (- end start)]
    (array ::pp start diff)))

(defn apply-progress [prepared-parameter progress]
  (assert (= ::pp (aget prepared-parameter 0)))
  (let [start (aget prepared-parameter 1)
        diff (aget prepared-parameter 2)]
    (+ start (* progress diff))))



(def pp prepare-parameter)
(def ap apply-progress)




;;; tweenは純粋に毎フレームでtarget-objを動かすだけ

(defonce ^js all-tween-entries (array))

;;; NB: ホットリロード等でツリー全体がdestroyされた場合はこれを呼ぶ事
(defn clear! [] (set! (.-length all-tween-entries) 0))


(defn tick! [delta-frames]
  (m/dotimes-backward [i (alength all-tween-entries)]
    (let [^cljs entry (aget all-tween-entries i)
          target-obj (.-target-obj entry)
          now-frames (+ (.-now-frames entry) delta-frames)
          ttl-frames (.-ttl-frames entry)
          tick-fn (.-tick-fn entry)
          progress (max 0 (min (/ now-frames ttl-frames) 1))
          done? (= 1 progress)]
      (when done?
        (.splice all-tween-entries i 1))
      (set! (.-now-frames entry) now-frames)
      (tick-fn target-obj progress)
      (when done?
        (when-let [done-fn (.-done-fn entry)]
          (done-fn target-obj)))))
  nil)

(defn register! [target-obj ttl-frames tick-fn & [done-fn]]
  (assert (instance? js/Object target-obj))
  ;; TODO: これをどうするかは悩むところ。今回はなしで。しかし「何度もregister!するが、有効なのは一個だけにしたい」要件は普通にあるので、あとでルールを考える事
  ;(m/dotimes-backward [i (alength all-tween-entries)]
  ;  (let [^cljs entry (aget all-tween-entries i)
  ;        o (.-target-obj entry)]
  ;    (when (= target-obj o)
  ;      (.splice all-tween-entries i 1))))
  (let [^cljs entry (js-obj)]
    (set! (.-target-obj entry) target-obj)
    (set! (.-now-frames entry) 0)
    (set! (.-ttl-frames entry) ttl-frames)
    (set! (.-tick-fn entry) tick-fn)
    (set! (.-done-fn entry) done-fn)
    (.push all-tween-entries entry)
    (tick-fn target-obj 0)
    nil))



(defn wait! [target-obj ttl-frames cont]
  (register! target-obj
             ttl-frames
             (fn [o progress] nil)
             cont))



(defn change-alpha! [^js target-obj ttl-frames new-alpha & [cont]]
  (let [pp-alpha (pp (.-alpha target-obj) new-alpha)]
    (register! target-obj
               ttl-frames
               (fn [o progress]
                 (set! (.-alpha o) (ap pp-alpha progress)))
               cont)))



(defn vibrate!
  [^js target-obj ttl-frames power-start & [power-end cont only-x? only-y?]]
  (let [pp-power (pp power-start (or power-end 0))
        total (alength all-tween-entries)]
    (loop [i (dec total)]
      (if-not (neg? i)
        (let [^cljs entry (aget all-tween-entries i)]
          (when-not (= target-obj (.-target-obj entry))
            (recur (dec i))))
        (let [orig-x (.-x target-obj)
              orig-y (.-y target-obj)
              h (fn [^js o progress]
                  (let [p (ap pp-power progress)
                        r-p (inc (* 2 p))]
                    (when-not only-y?
                      (set! (.-x o) (+ orig-x (- (rand r-p) p))))
                    (when-not only-x?
                      (set! (.-y o) (+ orig-y (- (rand r-p) p))))
                    nil))
              done-h (fn [^js o]
                        (set! (.-x o) orig-x)
                        (set! (.-y o) orig-y)
                        (when cont
                          (cont o)))]
          (register! target-obj ttl-frames h done-h))))))









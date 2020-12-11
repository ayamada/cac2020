(ns cac2020.pointer
  (:require [cac2020.pos :as pos]))

;;; TODO: `pointer` という名称が「スクリーン上のカーソル位置」の意味と
;;;       「jsのイベント種別」という意味とがあり、どちらなのか分かりづらい。
;;;       後者の名前は動かせないので、前者(およびこのnamespace自体も？)を
;;;       もっと良い名前に変更する事

;;; TODO: 現在は .-clientX .-clientY で座標を取得しているが、これは
;;;       canvasが position: fixed である事に依存する。
;;;       .-pageX .-pageY で取得するようにすべきかもしれない。
;;;       (.getBoundingClientRect canvas) の返すtopやleftが
;;;       page原点ではなくwindow原点のようなので、同じwindow原点となる
;;;       client系で取得した方がよいと思うので現状はこうしているが、
;;;       ある程度運用して判断したいところ。

;;; NB: これはpointer-idの指定/判定に使う
(def mouse-id-main 0)
(def mouse-id-wheel -1)
(def mouse-id-second -2)
(def mouse-id-fourth -3)
(def mouse-id-fifth -4)
(def mouse-id-misc -5)

(def support-pointer-event? (boolean js/window.PointerEvent))

(defonce last-pos (pos/make -9999 -9999))

(defonce a-pointer-table (atom {}))

(defonce ^:private a-pressed-handle-table (atom {}))
(defonce ^:private a-pressed-handle-table-array (atom (array)))
(defonce ^:private a-released-handle-table (atom {}))
(defonce ^:private a-released-handle-table-array (atom (array)))
(defonce ^:private a-moved-handle-table (atom {}))
(defonce ^:private a-moved-handle-table-array (atom (array)))
(defn register-pressed-handle! [k h]
  (swap! a-pressed-handle-table assoc k h)
  (reset! a-pressed-handle-table-array
          (to-array (vals @a-pressed-handle-table))))
(defn register-released-handle! [k h]
  (swap! a-released-handle-table assoc k h)
  (reset! a-released-handle-table-array
          (to-array (vals @a-released-handle-table))))
(defn register-moved-handle! [k h]
  (swap! a-moved-handle-table assoc k h)
  (reset! a-moved-handle-table-array
          (to-array (vals @a-moved-handle-table))))
(defn unregister-pressed-handle! [k]
  (swap! a-pressed-handle-table dissoc k)
  (reset! a-pressed-handle-table-array
          (to-array (vals @a-pressed-handle-table))))
(defn unregister-released-handle! [k]
  (swap! a-released-handle-table dissoc k)
  (reset! a-released-handle-table-array
          (to-array (vals @a-released-handle-table))))
(defn unregister-moved-handle! [k]
  (swap! a-moved-handle-table dissoc k)
  (reset! a-moved-handle-table-array
          (to-array (vals @a-moved-handle-table))))
;;; 登録された各handleに渡される引数について
;;; (h pointer-id x y original-event)
;;; - pointer-id は数値。タッチは1以上、左クリックは0、wheelクリックは-1、
;;;   右クリックは-2、ブラウザ戻るは-3、ブラウザ進むは-4。
;;;   通常は、左クリック系以外を無視する為に「マイナスなら無視」するとよい。
;;; - x y は数値。
;;; - original-event はnil/MouseEvent/TouchEvent/PointerEventのいずれか。
;;;   nilの場合がありえるので要注意
;;;   (元イベントなしでハンドルが呼ばれるパターンがある為)

(defn- process-pressed-handles! [pointer-id x y original-event]
  ;(prn 'process-pressed-handles! pointer-id x y)
  (let [a @a-pressed-handle-table-array]
    (dotimes [i (alength a)]
      (let [h (aget a i)]
        (h pointer-id x y original-event)))))

(defn- process-released-handles! [pointer-id x y original-event]
  ;(prn 'process-released-handles! pointer-id x y)
  (let [a @a-released-handle-table-array]
    (dotimes [i (alength a)]
      (let [h (aget a i)]
        (h pointer-id x y original-event)))))

(defn- process-moved-handles! [pointer-id x y original-event]
  ;(prn 'process-moved-handles! pointer-id x y)
  (let [a @a-moved-handle-table-array]
    (dotimes [i (alength a)]
      (let [h (aget a i)]
        (h pointer-id x y original-event)))))





(defn- mouse-button-number->mouse-id [mouse-button-number]
  (when ^boolean goog/DEBUG
    (when (nil? mouse-button-number)
      (prn ::found-nil-mouse-button-number)))
  (case mouse-button-number
    nil mouse-id-main ; タッチ環境などでありえる？とりあえず左クリック扱いに
    0 mouse-id-main
    1 mouse-id-wheel
    2 mouse-id-second
    3 mouse-id-fourth
    4 mouse-id-fifth
    (do
      (when ^boolean goog/DEBUG
        (prn ::found-unknown-mouse-button-number mouse-button-number))
      mouse-id-misc)))



(defn- process-touches! [^js e update-fn]
  ;; TODO: 今は全部処理しているが、これはよくないのでは？
  ;;       b3では「.-changedTouchesがない時は代わりに.-touchesを見る」かつ
  ;;       「配列の最後尾のもののみ処理する(他は無視する)」
  ;;       という実装になっていた。
  ;;       実際にある程度動かしてみて、問題があるようなら直したい。
  (let [touch-list (.-changedTouches e)]
    (dotimes [i (alength touch-list)]
      (let [^js touch (aget touch-list i)]
        (update-fn (.-identifier touch)
                   (.-clientX touch)
                   (.-clientY touch)
                   e)))))


(defn- update-pressed-pointer! [pointer-id x y original-event]
  ;(prn :pressed pointer-id)
  (if-let [pos (@a-pointer-table pointer-id)]
    (do
      (pos/set-x! pos x)
      (pos/set-y! pos y))
    (swap! a-pointer-table assoc pointer-id (pos/make x y)))
  (pos/set-x! last-pos x)
  (pos/set-y! last-pos y)
  (process-pressed-handles! pointer-id x y original-event)
  nil)

(defn- pressed-mouse! [^js e]
  (update-pressed-pointer! (mouse-button-number->mouse-id (.-button e))
                           (.-clientX e)
                           (.-clientY e)
                           e))

(defn- pressed-touch! [^js e]
  (process-touches! e update-pressed-pointer!))

(defn- update-released-pointer! [pointer-id x y original-event]
  ;(prn :released pointer-id)
  (swap! a-pointer-table dissoc pointer-id)
  ;; NB: releasedの時はlast-posを更新してはいけない
  ;;     (マルチタップ時に、離した座標よりも残ってる座標の方を優先してほしい)
  (process-released-handles! pointer-id x y original-event)
  nil)

(defn- released-mouse! [^js e]
  (update-released-pointer! (mouse-button-number->mouse-id (.-button e))
                            (.-clientX e)
                            (.-clientY e)
                            e))

(defn- released-touch! [^js e]
  (process-touches! e update-released-pointer!))

(defn- update-moved-pointer! [pointer-id x y original-event]
  ;; NB: pressedとは違い、個別pointerがない場合は何もしない
  ;; NB: releasedと同時に発生した場合にconflictを起こさないようにする必要あり！
  (when-let [pos (@a-pointer-table pointer-id)]
    (pos/set-x! pos x)
    (pos/set-y! pos y))
  (pos/set-x! last-pos x)
  (pos/set-y! last-pos y)
  (process-moved-handles! pointer-id x y original-event)
  nil)

(defn- moved-mouse! [^js e]
  (update-moved-pointer! (mouse-button-number->mouse-id (.-button e))
                         (.-clientX e)
                         (.-clientY e)
                         e))

(defn- moved-touch! [^js e]
  (process-touches! e update-moved-pointer!))


(defn- setup-event-listeners! []
  (let [^js root-window js/window]
    (doseq [k [:mousedown]]
      (.addEventListener root-window (name k) pressed-mouse!))
    (doseq [k [:touchstart]]
      (.addEventListener root-window (name k) pressed-touch!))
    (doseq [k [:mouseup]]
      (.addEventListener root-window (name k) released-mouse!))
    (doseq [k [:touchend :touchcancel]]
      (.addEventListener root-window (name k) released-touch!))
    (doseq [k [:mousemove]]
      (.addEventListener root-window (name k) moved-mouse!))
    (doseq [k [:touchmove]]
      (.addEventListener root-window (name k) moved-touch!))
    nil))



(defn- warn-unknown-device! [msg obj]
  (when-let [^js c js/window.console]
    (.log c msg obj))
  nil)

(def ^:private warn-unknown-device-once! (memoize warn-unknown-device!))

(defn- resolve-pointer-id [^js e]
  (case (.-pointerType e)
    "mouse" (mouse-button-number->mouse-id (.-button e))
    "pen" (mouse-button-number->mouse-id (.-button e))
    "touch" (.-pointerId e)
    ;; NB: 以下の判定をどうするかは悩むところ
    "" (mouse-button-number->mouse-id (.-button e))
    nil (mouse-button-number->mouse-id (.-button e))
    (do
      (warn-unknown-device-once! "unknown pointer device found"
                                 (.-pointerType e))
      ;; 悩むが、とりあえず強制的にマウス左ボタン固定にしておく
      mouse-id-main)))

(defn- pressed-pointer! [^js e]
  (update-pressed-pointer! (resolve-pointer-id e)
                           (.-clientX e)
                           (.-clientY e)
                           e))

(defn- released-pointer! [^js e]
  (update-released-pointer! (resolve-pointer-id e)
                            (.-clientX e)
                            (.-clientY e)
                            e))

(defn- moved-pointer! [^js e]
  (update-moved-pointer! (resolve-pointer-id e)
                         (.-clientX e)
                         (.-clientY e)
                         e))

(defn- setup-event-listeners-for-pointer-event! []
  (let [^js root-window js/window]
    (doseq [k [:pointerdown]]
      (.addEventListener root-window (name k) pressed-pointer!))
    (doseq [k [:pointerup :pointercancel]]
      (.addEventListener root-window (name k) released-pointer!))
    (doseq [k [:pointermove]]
      (.addEventListener root-window (name k) moved-pointer!))
    nil))



(defonce a-installed? (atom false))

(defn install! [& [use-experimental-pointer-event?]]
  (when-not @a-installed?
    (reset! a-installed? true)
    (if (and support-pointer-event? use-experimental-pointer-event?)
      (setup-event-listeners-for-pointer-event!)
      (setup-event-listeners!))
    nil))








;;; 補助ボタン系を除去する判定用
(defn id-auxiliary? [id]
  (if (nil? id)
    ;; TODO: nil時の判定をどうするかははとても悩む。ある程度運用してみて決める
    false ; or true ?
    (neg? id)))

(defn reset-all-status! []
  (doseq [[pointer-id pos] @a-pointer-table]
    (swap! a-pointer-table dissoc pointer-id)
    (when ^boolean goog/DEBUG
      (prn :freed-pointer-id pointer-id :by `reset-all-status!))
    ;; ここでもreleasedハンドルを実行するかはとても悩む
    ;; 考えた結果、今のところは実行しない事にした
    ))

;;; 引数なしだとシングル判定(補助ボタン系除く)、ありだとマルチ判定
(defn pressed? [& [pointer-id]]
  (if pointer-id
    (boolean (@a-pointer-table pointer-id))
    (or
      (pressed? mouse-id-main)
      (some (fn [[id pos]]
              (not (id-auxiliary? id)))
            @a-pointer-table))))

;;; 引数なしだとシングル判定、ありだとマルチ判定
(defn last-x [& [pointer-id]]
  (pos/->x (if pointer-id
             (@a-pointer-table pointer-id)
             last-pos)))

;;; 引数なしだとシングル判定、ありだとマルチ判定
(defn last-y [& [pointer-id]]
  (pos/->y (if pointer-id
             (@a-pointer-table pointer-id)
             last-pos)))




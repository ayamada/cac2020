(ns cac2020.property
  (:refer-clojure :exclude [get set! assoc!])
  (:require [clojure.string :as string]))

;;; TODO: kebab関連はnamespaceを分けるべきか？
(defn kebab-string->camel-string [s]
  (assert (string? s))
  (let [s (name s)
        s (if-let [[_ m] (re-find #"^(.*)\?$" s)]
            (str "is-" m)
            s)
        s (string/replace s #"(?<!^)-." #(string/upper-case (subs % 1)))]
    s))

(defn name->camel-string [k]
  (if (or (keyword? k) (symbol? k))
    (kebab-string->camel-string (name k))
    k))

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

(defmacro set! [o & args]
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
                  `(-set! ~o' ~@(apply concat ng-list)))]
    `(let [~o' ~o]
       ~@ok-bodies
       ~ng-body)))

(defmacro assoc! [o & args] `(set! ~o ~@args))

(defmacro get [o k]
  (if-not (keyword? k)
    `(-get ~o ~k)
    (let [nsk-str (when-let [nsk (namespace k)]
                    (kebab-string->camel-string nsk))
          nk-str (kebab-string->camel-string (name k))]
      (if nsk-str
        `(when-let [o# (aget ~o ~nsk-str)]
           (aget o# ~nk-str))
        `(aget ~o ~nk-str)))))

          )
   :cljs (do

(defn -set! [o & args]
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

(defn -get [^js o k]
  (if (or (keyword? k) (symbol? k))
    (let [nsk (namespace k)
          nk (name k)
          o (if nsk
              (aget o (kebab-string->camel-string nsk))
              o)]
      (aget o (kebab-string->camel-string nk)))
    (aget o k)))

(defn merge! [o m]
  (doseq [[k v] m]
    (-set! o k v))
  o)

(defn map->js-obj [m]
  (let [o (js-obj)]
    (merge! o m)
    o))

           ))

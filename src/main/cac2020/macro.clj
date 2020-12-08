(ns cac2020.macro
  (:require [clojure.string :as string]))


(defmacro str* [& args]
  (apply str args))


(defmacro dotimes-backward [bindings & bodies]
  (assert (vector? bindings))
  (assert (= 2 (count bindings)))
  `(let [n# ~(second bindings)]
     (dotimes [i0# n#]
       (let [~(first bindings) (- n# i0# 1)]
         ~@bodies))))



(defmacro doseq-array-backward [bindings & bodies]
  (assert (vector? bindings))
  (assert (= 2 (count bindings)))
  `(let [arr# ~(second bindings)
         total# (alength arr#)]
     (dotimes [i0# total#]
       (let [i# (- total# i0# 1)
             ~(first bindings) (aget arr# i#)]
         ~@bodies))))





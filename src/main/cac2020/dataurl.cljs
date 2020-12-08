(ns cac2020.dataurl
  (:require-macros [cac2020.macro :as m])
  (:require ["pixi.js" :as pixi]))

(defn- t [dataurl]
  (pixi/Texture.from dataurl))

;;; 煙/雲。32x32。
(defn smoke []
  (t (m/str* "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf"
             "8/9hAAABNUlEQVQ4jYWToY6DUBBFGxIEAoHBYBAkiCpkTSUGi8GgVmFwqDW4ui"
             "pcDZ9Qi62v3B9Yvd9w1tyhb5vSJbnhpZ17eufNdLd78wA+EAAe4L+rNYMHBDoH"
             "QAEc9E43ITJmQALk0h7opFKgvWqCZ0AB1MAHUMnQAj3wKfX6vlSa0Mwx0KhgkE"
             "bpBJyBSZBBP1Cs7QChAFZ8BmbgKl2ARZBRLTVqxbcEpRN1kuEOfAHfOhtsVF0F"
             "xJYi0x2MSnCV+UcyyEVJOwFSA6S6IOt/3gBMzkXnQGLLcnAAvVLcBDHdBKhV7w"
             "OeJUickQ2Kuci0SLPStcBxHaOzdZUz+5MMszMZMxsgft7ERONp1E7P30XqeKx2"
             "zottjDSNTIWtk6rmsUDp2vuL/0SodiIlOuqzWOBt8wYw+q/mF2XSlkfEIbhWAA"
             "AAAElFTkSuQmCC")))

;;; さくらんぼ。16x16
(defn cherry []
  (t (m/str* "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf"
             "8/9hAAAAZElEQVQ4jWNgoCL4j4ZJ07y9URsFk2oQhkZyDIJrRPYKmkHEuwZJA3"
             "rYEDQIwxWvjZRRMCFDULyCrpmQIVgVEmsAXttg7P///5NvAEwz0QYg20ay/7HY"
             "RlQ0khxlWA0h1jaqAACvLONemBcg4QAAAABJRU5ErkJggg==")))

;;; 葉。16x16
(defn leaf []
  (t (m/str* "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQAgMAAABi"
             "nRfyAAAADFBMVEVHcExRkBMAYhhtqRE0DJ/fAAAAAXRSTlMAQObYZgAAAEBJRE"
             "FUCNdjYEAA9qgJDAyxyxkYmG/VAHnpJQwM/NMDGBh4tzswMMiVAlWYAYUYJIFC"
             "DNJAIQZOkC42EMEEMwMAlKIIPdyq1roAAAAASUVORK5CYII=")))

;;; プリン。16x16
(defn pudding []
  (t (m/str* "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQAgMAAABi"
             "nRfyAAAADFBMVEVHcEz/2QCiIgD4mQCvKmLyAAAAAXRSTlMAQObYZgAAADpJRE"
             "FUCNdjYEAArgUMDEyrVjUwMGatdGBgDA11YGANDQ1AIkRDQ0OQCNPQ0BgG/tDQ"
             "DwwM//9DzAAAJ4wP2HtEC58AAAAASUVORK5CYII=")))

;;; p8金髪緑服。11x15。
(defn p8elf []
  (t (m/str* "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAsAAAAPCAYAAAAy"
             "PTUwAAAAe0lEQVQoka2QwQmAMAxFM1fFHXoxOIKDdBBPnoRO0hkcoEcvuZTvSb"
             "GYBkEDj1zeD0mIPhQUdLHk8YEWUMVWACWP2JeuwpT9KpAUISnCr2LLp3hiyi7w"
             "tYILbB85bUOFC6yLkiJc4ApJsfnrV1Oba1gyERFm7nHvv8lmHV7o1UPbOatgAA"
             "AAAElFTkSuQmCC")))

;;; distance field処理済の、中塗りされた円。16x16。
(defn sphere []
  (t (m/str* "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf"
             "8/9hAAAAvklEQVQ4ja3TMQrCQBCFYSHgCWxTew+xSGmTJhfwKlYinsQqR7DwJl"
             "apU30WjhBjXKNxYGBZ9v07b3Z2NksEMmSpM0OiOZYoUEUWsTf/JF6gxB41LpE1"
             "jgHLU+ItTmi8RhOg7QskvG5C3A6IH9EGpHqygxw7XBPibiUHLLuAddw+NmoU3f"
             "JLnL8AXMJGNh0QVaz8auFfTZz2jAODNFTJ+0HqQTZh5+Te2HOs91Kj3LOTuze2"
             "jFwb85newJLf+Qalm5IlQyJAsAAAAABJRU5ErkJggg==")))





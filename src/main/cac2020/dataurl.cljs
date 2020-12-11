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

;;; p8金髪緑服。12x15。
(defn p8elf []
  (t (m/str* "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAwAAAAPCAMAAADn"
             "P957AAAAAXNSR0IArs4c6QAAAB5QTFRFAAAA/+wn////AOQ2/8yqAIdR/6MAq1"
             "I2Ka3/AAAANYLk7wAAAAp0Uk5TAP///////////36JFFYAAABDSURBVAiZbY1R"
             "CsAwCEMtjZbc/8KLw9oy9vzJU1GzD0x2HgmvXMYRooVAdvgKRE88wvvCFF4ZLl"
             "CPeAa1tsW4VL/SPL+fAfagHZb8AAAAAElFTkSuQmCC")))

;;; distance field処理済の、中塗りされた円。16x16。
(defn sphere []
  (t (m/str* "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf"
             "8/9hAAAAvklEQVQ4ja3TMQrCQBCFYSHgCWxTew+xSGmTJhfwKlYinsQqR7DwJl"
             "apU30WjhBjXKNxYGBZ9v07b3Z2NksEMmSpM0OiOZYoUEUWsTf/JF6gxB41LpE1"
             "jgHLU+ItTmi8RhOg7QskvG5C3A6IH9EGpHqygxw7XBPibiUHLLuAddw+NmoU3f"
             "JLnL8AXMJGNh0QVaz8auFfTZz2jAODNFTJ+0HqQTZh5+Te2HOs91Kj3LOTuze2"
             "jFwb85newJLf+Qalm5IlQyJAsAAAAABJRU5ErkJggg==")))

;;; 星。32x32。
(defn star []
  (t (m/str* "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgBAMAAACB"
             "VGfHAAAAMFBMVEVHcEz//wD//wD//wD//wD//wD//wD//wD//wD//wD//wD//w"
             "D//wD//wD//wD//wCz681+AAAAD3RSTlMA2grzGEg7dKu6YC/NII1F5mWjAAAA"
             "1ElEQVQoz2NgwAGYFNAEuBagCTiKoAnUf0fls8h/dEARYP//vwDViP//UQ2p//"
             "//O6oR//+jGKL+HwiKYG70Dn8pDBIwnFe6Behe1h7j/3BgeCKAgU3+PxL4mMDA"
             "EIgsIAo0g1UeVQEDQyJCQAxsDcIUiAIGho2oChBK/myAObQfIvADHp7noSpg4c"
             "prDxH4fAFmBkwgARbiMFtgIc8HZFs0A4kHUAGO//8tg1Qn///fABXQAfIZGIAi"
             "QlCBi5aLQJTWZFmowI4lENqrGyqgxIDOQAYAtaau45JxTnYAAAAASUVORK5CYI"
             "I=")))




これは [Clojure Advent Calendar 2020](https://qiita.com/advent-calendar/2020/clojure) 12日目の記事です。


# shadow-cljsあれこれ

TODO: もっとよい記事タイトルを考えましょう


## shadow-cljsについて

- https://github.com/thheller/shadow-cljs
    - cljsをビルドするやつ
    - ビルドツールとしてnpmを使い、leinやclojureコマンドなしで動く
        - `project.clj` や `deps.edn` の代わりに、npm用の `package.json` と専用設定の `shadow-cljs.edn` を書く
    - もっと詳しい情報は @iku000888 さんの去年のclojure-advent-calendarの記事 https://qiita.com/iku000888/items/5c12c999c0d49cc2c4b0 があります
    - あるいは公式マニュアルを順番に見ていくなど https://shadow-cljs.github.io/docs/UsersGuide.html


### shadow-cljsから感じ取れる思想について

- 公式には明記していないようだけれど、「jvmからの脱却」の意志を強く感じた。フロントエンドをcljsで書くのはもちろん、サーバサイドもnode向けcljsで書き、jvm依存なマクロをなるべく書かずにすむ機能を提供し、セルフホスティングcljsを見据えた設計方針、いずれも「jvmからの脱却」を指向している、と自分は感じた


### shadow-cljs雑感

どのへんがいいの？

- ↑の記事や公式ドキュメントを参照、いいところたくさんある。個人的には以下が重点
- jvmからの脱却を見据えた構成である事
    - 今はまだjavaが必要だが、そのうちjavaなしで動かせるようになると思う
- 豊富なnpmライブラリをまあまあ楽に利用できる
    - extern定義も比較的楽にできる
- node(とjava)さえ入ってればたとえwindows環境でも開発できる
    - ただし本気でwindows環境を考慮する場合は、うっかり"scripts"内にposixコマンドを書かないようにする必要はある
        - shっぽい挙動については大体npmがやってくれる。 `"node -e '...' && shadow-cljs release app"` とか書ける。ただしposixコマンドは使えないので、スクリプトエンジンとしてのnodeをフル活用する事になる


よくないところはある？

- まだまだ活発に開発継続してるので、たまに互換性のなくなるアップデートがあるかもしれない
- かなりcljs側にカスタマイズが入っていて、shadow-cljs専用の便利機能が色々ある反面、それに依存したcljsを書くとshadow-cljsから抜け出しづらくなってしまう
    - clojarsとかに置くcljsライブラリ開発に利用する場合は注意が必要かもしれない
    - 一部機能はcljs本家にパッチを送ったりしてるようだが、あんま取り込まれてないっぽい様子
- 微妙にマクロが使いづらい
    - shadow-cljsはコンパイル結果や中間状態を積極的にキャッシュするので、「ソースコード以外の情報をマクロで読み込んでコンパイル内容を左右する」系のは干渉しやすい。公式ドキュメントにも「避けた方がよい」と書かれている
        - マクロを書かずにそういう事をできるようにする機能がshadow-cljsにも組み込まれている(が、マクロ書く方がlisper的には楽ではある)
    - そもそもマクロはcljsでないcljで書くのが標準の為、どうしてもjvm依存となり、shadow-cljsの思想的に合わない
- `shadow-cljs.edn` 内の `:builds` や `:modules` あたりの設計がいまいちこなれていないように感じる。すぐ読みにくくなってしまう
    - とは言えlein-cljsbuildとかと比べるとマシになっているのではと思う
- `lein clean` に相当する操作がデフォルトで提供されていない(ので自分で用意する必要がある)
    - たまに `.shadow-cljs/` の内部状態がこわれて正常なコンパイルが行えなくなる時がある？ので `lein clean` 的な奴はほしい
    - これもnpmの流儀で解決できる。 `npm i rimraf --save-dev` して `rm -rf` 相当を実行してくれるコマンドをインストールし、 package.json に `"scripts": {"clean": "shadow-cljs stop && rimraf .shadow-cljs public/cljs (他に消したいファイルがあればここに列挙)"}` みたいな感じで書く。これで `npm run clean` できるようになる
        - `shadow-cljs stop` しているのは、 `.shadow-cljs/` 等を消す際に、他でwatchしているshadow-cljsプロセスが生きているとよくないので。prod版をビルドするコマンドでも同様にこれを実行するようにしておくといい
- 個人的にはwebpackの設計が好きじゃない。google closure compilerとの相性があんまりよくない気がする(厳密に調査した訳じゃないので、実際は問題ないのかもしれないが…)


総合的には？

- 総合的には非常に使えるレベルで、lein-cljsbuildやfigwheel-mainから乗り換える価値は十分にある。project.cljやdeps.edn連携も一応ある(自分は使った事ないのでどのくらいちゃんと動くかは不明)
- 「jvmからの脱却」を検討してしまうような人にオススメ
- ハードルは「ビルドツールがnpm」というところ。package.jsonのメンテをしなくてはならない
    - [lein-shadow](https://clojars.org/lein-shadow) とかを使えば避けられる？(よく知らない)
- 欲を言うなら、WebAssemblyで動くclojureがほしいところだけど…


## 実際につかってみよう

プロジェクトディレクトリの生成

- `npx create-cljs-project my-game-name`
- `cd my-game-name`


開発の前準備をしよう

- `vim package.json`
- `vim shadow-cljs.edn`
- `shadow-cljs.edn` の内容に合わせて、ディレクトリ配置等を調整する等する
- `vim src/main/cac2020/core.cljs` (shadow-cljs.edn 内の `:entries` や `:init-fn` に記入した部分のstubを用意)
- `vim public/index.html`
- `npm i`
- `npx shadow-cljs watch app`
- ブラウザから `http://localhost:8020/` を開く(port番号はshadow-cljs.ednの指定通りにする事)
- あとはひたすらインクリメンタル開発サイクルを回していく


開発しよう(開発した。具体的なコードはリポジトリの実ファイルを参照)

- jsライブラリを叩こう(pixi.js)
    - 生domも仮想domもめんどすぎる！だからcanvasにひきこもろう
    - TODO: 詳細を書きましょう
- ホットリロードしよう
    - 適切に `^:dev/before-load` `^:dev/after-load` を仕込む事で、ソースコード側の変更を即座にブラウザ側へと反映できる
- package.json内に書いたパッケージ名とバージョン番号をcljsから読もう
    - 「ゲームやアプリ内に自身のバージョン名を表示したい」という、よくある奴対応
    - まずコード内に `(goog-define VERSION "(fallback)")` とか書いておく。これは `def` と同じ挙動をするようなので、これで `VERSION` が参照できるようになった
    - 次に `shadow-cljs.edn` に `:closure-defines {cac2020.util/VERSION #shadow/env ["npm_package_version"]}` みたいな定義を追加する。これで環境変数 `npm_package_version` が `VERSION` の値として反映されるようになった
        - この環境変数はnpmが提供している。このプロジェクトのpackage.jsonのscriptsに `"env": "env"` を入れてあるので、 `npm run env|sort` して、どういう環境変数が設定されているのか見てみよう
        - もちろん自分で好きな環境変数を設定して渡してもok。npmでは `.env` というファイルで環境変数を渡せるようだ(詳細未確認)
- externしよう
    - shadow-cljsでは `:infer-externs :auto` すると、リリース版ビルド時にexternしていないjsのmethod/property参照は警告を出してくれるらしい。externするのはmethod/property元のオブジェクトに `^js` のtype hintをつけるだけ。かんたん！
    - これまで通り、別ファイルでextern定義を用意してもok


リリースしよう

- ゲームのversioningルールについて
    - http://rnkv.hatenadiary.jp/entry/2020/11/15/192228
- リリース版で開発向けコードの除去をする
    - `(when ^boolean js/goog.DEBUG ...)` とすれば、コンパイラが勝手にこのブロックを除去してくれるとの事。 `^boolean` のtype hintがないと `if (cljs.core._truth(goog.DEBUG)){...}` というjsに展開されてしまいコード除去されなくなるとの事
- リリースビルドのやりかた
    - lein clean相当のやりかた
        - `npm i rimraf --save-dev` して package.json の "scripts" 内に `"clean": "rimraf .shadow-cljs ..."` って書く。そして `npm run clean` を実行
    - リリースビルドしよう
        - `npx shadow-cljs release app`
- リリース版zipを生成しよう
    - node で rm, mkdir しよう
        - `npm i rimraf --save-dev`
        - `npm i mkdirp --save-dev`
    - zipに固めよう
        - `npm i archiver --save-dev`
        - 結構nodeスクリプトを書く必要がある…詳細は [scripts/pack-zip.js](scripts/pack-zip.js) を参照
            - 理論上は、こういう雑用スクリプトもcljsで書いてコンパイルできる筈だけど…
- できたzipをアツマールとかitch.ioとかにアップロードしよう
    - それぞれのサイトの説明を読もう！！！！！
    - TODO: アップロードしたurlを掲載

今後の課題

- electronでデスクトップアプリ化しよう → これは簡単
- サーバサイドもcljsのnode向け出力で書こう → jvm資産の代わりにnpm資産を使う必要あり、しんどい
- cordova系の何かでスマホアプリ化しよう → 未知の領域



## よくある問題

- なんかよくわからないけどエラーが出るようになった！
    - 多分 `.shadow-cljs/` がこわれてます。一回消してビルドし直してみましょう。このリポジトリでは `npm run clean` で消せます

- 自作spaアプリを自サイトに公開した！そしていろいろいじって更新した！そしたらなんか更新前のデータやファイルがブラウザにキャッシュされてる！
    - spaアプリ自体を設置するpathにリリース数値の層(ディレクトリ)を入れて、アップデート毎に別urlになるようにするのが無難です
    - shadow-cljsでは `:release-version "v1"` をつけておくと、生成されるjsファイルの名前が `cljs/main.v1.js` みたいになる。しかしこれをする場合は当然htmlの方にもjsファイル名変更指定が必要になるので面倒
    - そもそも大体これが問題になるのはどちらかというとjsファイルではなく、xhrでjsonとか画像とかロードする時。なのでxhrでロードするurlの末尾に `?12345678(タイムスタンプ)` みたいなのをつける手もある。でもこれはこれで別の問題になる事がたまにある。だから一番最初に書いたやり方が一番マシだと思う

- npm色々しんどい！
    - ワカル



## 個人的な主張

- みんなcljsでゲーム作ろうぜ
    - アツマールやitch.ioに置けるぜ
    - 大体のPCやスマホで遊べるぜ
    - デスクトップアプリ化して、dlsiteやsteamとかで販売できるぜ
    - スマホアプリ化して、google playやapp storeに置くのも視野に入るぜ(できるとは言ってない)
    - nintendo switch上で動かすのも視野に入るぜ(公式にはパブリッシャーを通す必要あり)
    - WebGLゲーなら生domや仮想domにあんまり悩まされないぜ(少しは悩まされる)
    - つくったゲームがおもしろい出来になるかにcljsが有利も不利もないけど、自分の好きな言語で素早く開発サイクルを回していけるなら品質は良くなるぜ(品質が良くてもゲームがおもしろいかはまた別)











<!-- vim:set ft=pandoc foldmethod=marker foldmarker=\<\!\-\-,\-\-\>: -->

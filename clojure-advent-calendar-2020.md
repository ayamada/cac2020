これは [Clojure Advent Calendar 2020](https://qiita.com/advent-calendar/2020/clojure) 12日目の記事です。


# shadow-cljsでゲームつくってみた

できたゲームはここから遊べます。

- https://game.nicovideo.jp/atsumaru/games/gm17554

この記事では、前半ではshadow-cljsについての簡単な説明や雑感を、後半ではこのゲームを実際にshadow-cljsで作ってみた際の大雑把な手順やメモを記入しています。
実際のコードはgithubのリポジトリを直に確認してください。


## shadow-cljsについて

- https://github.com/thheller/shadow-cljs
    - cljsのコードをdev実行したりホットリロードしたりprod出力したりできるやつ
    - ビルドツールとしてnpmを使い、leinやclojureコマンドなしで動く
        - `project.clj` や `deps.edn` の代わりに、npm用の `package.json` とcljs設定用の `shadow-cljs.edn` を書くのが基本
            - `project.clj` や `deps.edn` と連携するオプションやleiningenプラグインもあるよ(でも自分は詳しくないのでここでは割愛)
    - 過去の記事として [@iku000888 さんの記事](https://qiita.com/iku000888/items/5c12c999c0d49cc2c4b0) や [@lambda-knight さんの去年のclojure-advent-calendarの記事](https://qiita.com/lambda-knight/items/a69df35405b26f7a79cf) があります。概要とかはこっちで！
    - いつのまにか公式マニュアルが充実しているので、順番に見ていけば大体分かる(英語) → https://shadow-cljs.github.io/docs/UsersGuide.html


### shadow-cljsから感じ取れる思想について

- 公式には特に明記していないようだけれど、「jvmからの脱却」の意志を強く感じた。フロントエンドをcljsで書くのはもちろん、サーバサイドもnode向けcljsで書き、jvm依存なマクロをなるべく書かずにすむ機能を提供し、セルフホスティングcljsを見据えた設計方針、いずれも「jvmからの脱却」を指向している、と自分は感じた


## shadow-cljs雑感

### どのへんがいいの？

- ↑の記事や公式ドキュメントを参照、いいところたくさんある。個人的には以下が重点
- jvmからの脱却を見据えた構成である事
- 豊富なnpmライブラリをまあまあ楽に利用できる
    - extern定義も簡単だし、extern忘れに対する警告も出してくれる
- node(とjava)さえ入ってればたとえwindows環境でも開発できる
    - ただし本気でwindows環境を考慮する場合は、うっかり"scripts"内にposixコマンドを書かないようにする必要はある
        - shっぽい挙動については大体npmがやってくれる。 `"node -e '...' && shadow-cljs release app"` とか書ける。そして前述の通りposixコマンドが使えないので、スクリプトエンジンとしてのnodeをフル活用する事になる


### よくないところはある？

- まだまだ活発に開発継続してるので、たまに互換性のなくなるアップデートがあるかもしれない
- かなりcljs側にカスタマイズが入っていて、shadow-cljs専用の便利機能が色々ある反面、それに依存したcljsを書くとshadow-cljsから抜け出しづらくなってしまう
    - clojarsとかに置くcljsライブラリ開発に利用する場合は注意が必要かもしれない
    - 一部機能はcljs本家にパッチを送ったりしてるようだが、あんま取り込まれてないっぽい様子
- 微妙にマクロの使いづらいケースがある
    - shadow-cljsはコンパイル結果や中間状態を積極的にキャッシュするので、「ソースコード以外の情報をマクロで読み込んでコンパイル内容を左右する」系のは干渉しやすい。公式ドキュメントにも「避けた方がよい」と書かれている
        - マクロを書かずにそういう事をできるようにする機能がshadow-cljsにも組み込まれている(が、マクロ書く方がlisper的に楽ではある)
    - マクロ展開結果が関数式やスペシャルフォームの組み合わせになる、普通のマクロの使い方なら問題になる事は特にない
    - そもそもマクロはcljsでないcljで書くのが現状なので、どうしてもjvm依存となりがちで、shadow-cljsの思想的に合わない感がある
- `shadow-cljs.edn` 内の `:builds` や `:modules` あたりの設計がいまいちこなれていないように感じる。すぐ読みにくくなってしまう
    - とは言えlein-cljsbuildとかと比べるとマシになっているのではと思う
    - `npx shadow-cljs` の引数に `--config-merge '{EDN式}'` をつける事でビルド設定を一時的にmergeできる(merge実装はlein等と同様の[deep-merge](https://github.com/thheller/shadow-cljs/blob/023d9a2a8dbf008d1a26dc221468daade2746872/src/main/shadow/build/api.clj#L26))。 `shadow-cljs.edn` で細かくbuild-idやmoduleを分けるよりも、これをつけたコマンド列を `package.json` 内の `"scripts"` に定義した方がよいかもしれない
- `lein clean` に相当する操作がデフォルトで提供されていない(ので自分で用意する必要がある)
    - たまに `.shadow-cljs/` の内部状態がこわれて正常なコンパイルが行えなくなる時がある？ので `lein clean` 的な奴を用意しておきたい
    - これもnpmの流儀で解決できる。 `npm i rimraf --save-dev` して `rm -rf` 相当を実行してくれるコマンドをインストールし、 package.json に `"scripts": {"clean": "shadow-cljs stop && rimraf .shadow-cljs public/cljs (他に消したいファイルがあればここに列挙)"}` みたいな感じで書く。これで `npm run clean` できるようになる
        - `shadow-cljs stop` しているのは、 `.shadow-cljs/` 等を消す際に、他でwatchしているshadow-cljsプロセスが生きているとよくなさそうなので。prod版をビルドするコマンドでも同様にこれを実行するようにしておくといい
- 個人的にwebpackの設計が好きじゃない(これは好みの話。動作には大体問題ない筈)


### 総合的には？

- 総合的には非常に使えるレベルで、lein-cljsbuildやfigwheel-mainから乗り換える価値は十分にある
- 「jvmからの脱却」を検討してしまうタイプの人に特にオススメ
- 一番大きいハードルは「ビルドツールがnpm」というところ
    - [lein-shadow](https://gitlab.com/nikperic/lein-shadow)みたいなのは一応あるけど、自動生成されるpackage.jsonやshadow-cljs.ednの内容についての知識がある前提になってる感
- 欲を言うなら、WebAssemblyで動くclojure実装がほしい気はする
    - 筆者は「cljsでゲーム制作」を考えているので実行速度は可能な限り稼ぎたい。またゲーム以外でも深層学習や画像や動画やデータ加工のような処理をやらせるならほしいかも。逆に言うならそれ以外では不要か？


## 実際につかってみよう

### プロジェクトディレクトリの生成

- `npx create-cljs-project my-game-name`
- `cd my-game-name`


### 開発の前準備をしよう

- `vim package.json`
- `vim shadow-cljs.edn`
- `shadow-cljs.edn` の内容に合わせて、ディレクトリ配置等を調整する等する
- `vim src/main/cac2020/core.cljs` (shadow-cljs.edn 内の `:entries` や `:init-fn` に記入した部分のstubを用意)
- `vim public/index.html`
- `npm i`
- `npx shadow-cljs watch app`
- ブラウザから `http://localhost:8020/` を開く(port番号はshadow-cljs.ednで指定したものと同じにする)
- あとはひたすらインクリメンタル開発サイクルを回していく


### ゲーム開発しよう

(開発した。具体的なコードはリポジトリの実ファイルを参照。ゲームのコード部分はかなり雑です、すいません)

- jsライブラリを叩こう(pixi.js)
    - 生domも仮想domもめんどすぎる！だからcanvasにひきこもろう
    - 筆者は [pixi.js](https://github.com/pixijs/pixi.js) を選ぶけど、npm配布のcanvas描画ライブラリは色々あるので好きな奴を選べばok
    - `npm i pixi.js --save`
        - すいません、これ例があまりよくなくて、ここに含まれてる `.js` は拡張子でも何でもなくて、単なるnpmパッケージ名の一部です。以降で「pixi.js」という文字列が出てきた時もそう解釈してください
    - あとは各cljs内のnsでrequireするだけ
        - 基本的にはnpmインストールしたjsモジュールのパッケージ名を文字列指定でrequireすればok
            - このpixiでの例だと、cljs側のコードは `(ns foo.bar (:require ["pixi.js" :as pixi]))` みたいになる。これで `(pixi/Container.)` でpixiのContainerインスタンスを生成できる
        - しかし、requireしたいnpmパッケージ側のexports定義の形によってオプション指定に `:as` を使うべきか `:refer` を使うべきか `:default` を使うべきかが違ってくる。詳細は https://shadow-cljs.github.io/docs/UsersGuide.html#_using_npm_packages を参照。よく分からなかったら順番に試していく(そんなにパターンが多い訳でもないので)
- ホットリロードしよう
    - ソースコード更新したら全自動で、開きっぱなしのブラウザページに再度流し込まれる
        - この辺はlein-cljsbuildやfigwheel-main等と同じ。必要に応じて `defonce` とか使ったりするのを検討しよう
    - 適切に `^:dev/before-load` `^:dev/after-load` を設定したフック関数を用意すると、ホットリロード実行直前/直後に実行してくれる。async版もある
- package.json内に書いたパッケージ名とバージョン番号をcljsから読もう
    - 要は「ゲームやアプリ内に自身のバージョン名を表示したい」という、よくある奴対応
    - まずcljs内に `(goog-define VERSION "(fallback)")` とか書いておく。これは `def` と同じ挙動をするようなので、これで `VERSION` が参照できるようになった
    - 次に `shadow-cljs.edn` に `:closure-defines {cac2020.util/VERSION #shadow/env ["npm_package_version"]}` みたいな定義を追加する。これで環境変数 `npm_package_version` が `VERSION` の値として反映されるようになった
        - この環境変数はnpmが提供している。このプロジェクトのpackage.jsonのscriptsに `"env": "env"` を入れてあるので、 `npm run env|sort` して、どういう環境変数が設定されているのか見てみよう
        - もちろん自分で好きな環境変数を設定して渡してもok。npmでは `.env` というファイルで環境変数を渡せるようだ(詳細未確認)


### リリースしよう

- ゲームのversioningルールについての個人的見解
    - http://rnkv.hatenadiary.jp/entry/2020/11/15/192228
- externしよう
    - `shadow-cljs.edn` で `:infer-externs :auto` しておくと、externしていないjsのmethod/property参照は警告を出してくれる。そしてexternするのはmethod/property元のオブジェクトに `^js` のtype hintをつけるだけ。かんたん！
        - これで「prod版ビルドしたら `:optimizations :advanced` のname manglingで動かなくなったんだけど」という悩みから解放される
        - 滅多にないけど、敢えて「ここのコードは `(set! (.-foo bar) 123)`  みたいにしているけど、このbarはcljsで生成した `(js-obj)` なのでfooのところはname manglingしてもokというかむしろしてほしい」みたいなケースもある。この時は、ここのbarに `^cljs` のtype hintをつけると警告の抑制だけしてくれる(externはしない)
    - これまでのcljsと同じように、別ファイルでextern定義を用意してもok。でもtype hintつける方がclojure流儀に合ってるし楽
- リリース版で開発向けコードの除去をしよう
    - `(when ^boolean js/goog.DEBUG ...)` としておけば、google closure compilerが勝手にこのブロックを除去してくれる、との事。なお `^boolean` のtype hintをつけないと `if (cljs.core._truth(goog.DEBUG)){...}` というjsに展開されてしまいコード除去されなくなるらしい…
- リリースビルドのやりかた
    - lein clean相当のやりかた
        - 先にも書いたけど、標準では提供されてない！
        - `npm i rimraf --save-dev` して package.json の "scripts" 内に `"clean": "rimraf .shadow-cljs"` みたいに書く。そして `npm run clean` を実行
    - リリースビルドしよう
        - `npx shadow-cljs release app`
- リリース版zipを生成しよう
    - node で rm とか mkdir できるようにしよう
        - `npm i rimraf --save-dev`
        - `npm i mkdirp --save-dev`
    - zipに固めよう
        - `npm i archiver --save-dev`
        - ここは結構nodeスクリプトを書く必要がある。詳細は [scripts/pack-zip.js](scripts/pack-zip.js) を参照
            - 理論上は、こういう雑用スクリプトもcljsで書いてコンパイルできる筈だけど…
- できたzipをアツマールとかitch.ioとかにアップロードしよう
    - それぞれのサイトの説明を読んで！！！！！
        - アツマール : https://qa.nicovideo.jp/faq/show/6673
        - itch.io : https://itch.io/docs/creators/html5 (英語)
    - 大雑把には「html5ゲームはzipに固めてダッシュボードからアップロード」という形
    - あと大体「ゲーム起動およびリサイズイベントによって、ゲーム画面(canvas要素)を自動的にブラウザウィンドウ内全画面化する」必要がある。そういうコードを組み込んでおく事
    - 利用したライブラリ等のライセンスを見れるようにしよう
        - mitとかapache2のようなライセンスのライブラリしか使っていなければ、ゲームの公開サイトの説明欄にその名前とurlを列挙する(もしくはそういうページを別途用意してそのurlを貼る)だけでライセンス要件を満たせる
            - もちろんGPLとかの場合はもうちょっときちんとなんとかする必要あり
        - 今回はソースリポジトリのurl付きで公開しているのでサボってます(＝上記とほぼ同じ状態と言える筈なので)
        - 可能ならゲーム内でメニューから選択してライセンス文を確認できると品質高い感が出ると思います(面倒)
    - (optional)それぞれのサイト固有の機能を叩こう
        - アツマール : https://atsumaru.github.io/api-references/apis
        - itch.io : (未調査)
    - アップロードして最終動作確認を取り、ダッシュボードから「公開」状態にしよう
        - https://game.nicovideo.jp/atsumaru/games/gm17554


### 今後の課題

1. electronでデスクトップアプリ化しよう → これは簡単
2. サーバサイドもcljsのnode向け出力で書こう → java資産の代わりにnpm資産を使う必要あり。既存のclj資産もほとんど使えず、しんどい
3. cordova系の何かでスマホアプリ化しよう → 未知の領域
4. nintendo switchとかのゲーム専用機に進出しよう → これ可能なの？これを目指すのなら最初からunity向け.NET出力になる[Arcadia](https://arcadia-unity.github.io/)とかで始めた方がよいのでは？(筆者はインストールなし軽量起動が重要だと考えてるのでcljsを選ぶけど、そういう選択肢もある)


## よくある問題

- 開発を回している内に、なんかよくわからないけどエラーが出るようになった！
    - 多分 `.shadow-cljs/` がこわれてます。一回watchを停止し `.shadow-cljs/` を消してビルドし直してみましょう。このリポジトリでは `npm run clean` で消せます

- 自作spaアプリを自サイトに公開した！そしていろいろいじって更新した！そしたらなんか更新前のデータやファイルがブラウザにキャッシュされてる！
    - spaアプリ自体を設置するpathにリリース数値の層(ディレクトリ)を入れて、アップデート毎に別urlになるようにするのが無難です
    - shadow-cljsでは `:release-version "v1"` をつけておくと、生成されるjsファイルの名前が `cljs/main.v1.js` みたいになる。しかしこれをする場合は当然htmlの方にもjsファイル名変更指定が必要になるので面倒
    - そもそも大体これが問題になるのはどちらかというとjsファイルではなく、xhrでjsonとか画像とかロードする時。なのでxhrでロードするurlの末尾に `?12345678(タイムスタンプ)` みたいなのをつける手もある。でもこれはこれで別の問題になる事がたまにある
    - だから最初に書いたやり方が一番マシだと思う

- npm色々しんどい！
    - ワカル
    - とりあえず自分用のメモを置いときます
        - `npx shadow-cljs help` でサブコマンド一覧が見れる
        - `npm run` を引数なしで実行する事で、package.jsonで定義されたタスク一覧を確認できる
        - 新しいnpmパッケージをrequireしたい時は `npm i (パッケージ名) --save` する。requireしたい訳じゃなくてただ単にpackage.json内のscripts欄で使いたいだけの場合は `npm i (パッケージ名) --save-dev` する
        - `npm outdated` で各npmパッケージが最新版かどうかチェックできる
        - npm環境でのhttpサーバの立て方(ちょっと癖あり)
            - `npx http-server -a 127.0.0.1 ./public -p 8080`
                - オプション詳細 https://www.npmjs.com/package/http-server
            - prod版ビルド後の動作確認を簡単に取りたい時にこれを使う



## 個人的な主張

- みんなcljsでゲーム作ろうぜ
    - アツマールやitch.ioに置けるぜ
    - 大体のPCやスマホで遊べるぜ
    - デスクトップアプリ化して、dlsiteやsteamとかで販売できるぜ
    - スマホアプリ化して、google playやapp storeに置くのも視野に入るぜ(簡単にできるとは言ってない)
    - WebGLゲーなら生domや仮想domにあんまり悩まされないぜ(少しは悩まされる)
    - つくったゲームがおもしろい出来になるかにcljsが有利も不利もないけど、自分の好きな言語で素早く開発サイクルを回していけるのは強いぜ
    - GCに気をつけようぜ
        - うっかりオブジェクト生成を毎フレーム起こしてしまうとやばいぜ
        - 明示的に解放する必要のあるjsインスタンスに気をつけようぜ
        - ブラウザのjsエンジンによっては循環参照を持つ使い捨て無名関数がGCされないかもしれないぜ(手で循環参照を断ち切れるようにしておこうぜ)
    - ゲームは自由な発想で作らないと面白くならないぜ
    - 「そのゲームの一番見てほしいところ」をプレイヤーが体験する前にめげてやめてしまわないよう色々工夫しようぜ














<!-- vim:set ft=pandoc foldmethod=marker foldmarker=\<\!\-\-,\-\-\>: -->

(defproject DroidMage/DroidMage "0.0.1-SNAPSHOT"
  :description "Xmage android client"
  :url "http://example.com/FIXME"
  :license {:name "MIT"}

  :global-vars {*warn-on-reflection* true}

  :source-paths ["src/clojure" "src"]
  :java-source-paths ["src/java" "src/libs"]
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]

  :plugins [[lein-droid "0.4.0-alpha4"]]

  :repositories [["sliding-menu" {:url "http://jzaccone.github.io/SlidingMenu-aar"}]]
  ;; The SlidingMenu dependency is used to import *its* dependencies. Since lein
  ;; doesn't support aar files, we have to include SlidingMenu into our source code.
  :dependencies [[org.clojure-android/clojure "1.7.0-RC1" :use-resources true]
                 [neko/neko "4.0.0-alpha1"]
                 [org.clojars.malabarba/lazy-map "0.1.2"]
                 [com.jeremyfeinstein.slidingmenu/library "1.3" :extension "aar"]
                 ;; [org.mage/mage-common "1.4.0"]
                 [org.mage/mage-network "1.4.0"]
                 [org.mage/mage "1.4.0"]]
  ;; We hack our own log4j inside org.mage/mage, so we have to exclude it from jdbc
  ;; to avoid dexing conflicts.
  :exclusions [log4j]

  :profiles {:default [:dev]

             ;; :local-repl
             ;; [:dev
             ;;  {:dependencies [[compat/android "21"]]
             ;;   :target-path "target/local-repl"}]

             :dev
             [:android-common :android-user
              {:dependencies [[org.clojure/tools.nrepl "0.2.10"]]
               :target-path "target/debug"
               :android {:aot :all-with-unused
                         :rename-manifest-package "com.droidmage.debug"
                         :manifest-options {:app-name "DroidMage - debug"}}}]

             :release
             [:android-common
              {:target-path "target/release"
               :android {:ignore-log-priority [:debug :verbose]
                         :aot :all
                         :build-type :release}}]

             :lean
             [:release
              {:dependencies ^:replace [[org.skummet/clojure "1.7.0-RC1-r2" :use-resources true]
                                        [neko/neko "4.0.0-alpha1"]
                                        [org.clojars.malabarba/lazy-map "0.1.2"]
                                        [com.jeremyfeinstein.slidingmenu/library "1.3" :extension "aar"]
                                        ;; [org.mage/mage-common "1.4.0"]
                                        [org.mage/mage-network "1.4.0"]
                                        [org.mage/mage "1.4.0"]]
               :exclusions [[org.clojure/clojure]
                            [com.android.support/support-v4]
                            [org.clojure-android/clojure]
                            [log4j]]
               :jvm-opts ["-Dclojure.compile.ignore-lean-classes=true"]
               :global-vars ^:replace {clojure.core/*warn-on-reflection* true}
               :android {:lean-compile true
                         :use-debug-keystore true
                         :skummet-skip-vars ["#'neko.init/init"
                                             "#'neko.context/context"
                                             "#'neko.resource/package-name"
                                             "#'neko.-utils/keyword->static-field"
                                             "#'neko.-utils/keyword->setter"
                                             "#'neko.ui.traits/get-display-metrics"
                                             "#'com.droidmage.main/MainActivity-onCreate"
                                             "#'com.droidmage.main/MainActivity-init"]}}]}

  :android {
            :dex-opts ["-JXmx4096M" "--multi-dex"]

            :support-libraries ["v4"]
            :manifest-options {:app-name "@string/app_name"}
            :target-version 21
            :aot-exclude-ns ["clojure.parallel" "clojure.core.reducers"
                             "cljs-tooling.complete" "cljs-tooling.info"
                             "cljs-tooling.util.analysis" "cljs-tooling.util.misc"
                             "cider.nrepl" "cider-nrepl.plugin"]})

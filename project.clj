(defproject DroidMage/DroidMage "0.0.1-SNAPSHOT"
  :description "Xmage android client"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :global-vars {*warn-on-reflection* true}

  :source-paths ["src/clojure" "src"]
  :java-source-paths ["src/java"
                      "src/java/libs"
                      ]
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]

  :plugins [[lein-droid "0.4.0-SNAPSHOT"]]

  :repositories [["sliding-menu" {:url "http://jzaccone.github.io/SlidingMenu-aar"}]]
  ;; The SlidingMenu dependency is used to import *its* dependencies. Since lein
  ;; doesn't support aar files, we have to include SlidingMenu into our source code.
  :dependencies [[com.jeremyfeinstein.slidingmenu/library "1.3" :extension "aar"]
                 [neko/neko "4.0.0-SNAPSHOT"]
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
                         :build-type :release}}]}

  :android {
            :dex-opts ["-JXmx4096M" "--multi-dex"] ;; 

            :manifest-options {:app-name "@string/app_name"}
            :target-version 21
            :aot-exclude-ns ["clojure.parallel" "clojure.core.reducers"
                             "cljs-tooling.complete" "cljs-tooling.info"
                             "cljs-tooling.util.analysis" "cljs-tooling.util.misc"
                             "cider.nrepl" "cider-nrepl.plugin"]})

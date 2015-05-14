(defproject DroidMage/DroidMage "0.0.1-SNAPSHOT"
  :description "Xmage android client"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :global-vars {*warn-on-reflection* true}

  :source-paths ["src/clojure" "src"]
  :java-source-paths ["src/java"]
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]
  :plugins [[lein-droid "0.4.0-SNAPSHOT"]]

  :dependencies [[org.clojure-android/clojure "1.7.0-beta3" :use-resources true]
                 [neko/neko "3.2.0"]]
  :profiles {:default [:dev]

             :dev
             [:android-common :android-user
              {:dependencies [[org.clojure/tools.nrepl "0.2.10"]]
               :target-path "target/debug"
               :android {:aot :all-with-unused
                         :rename-manifest-package "com.endlessparentheses.droidmage.debug"
                         :manifest-options {:app-name "DroidMage - debug"}}}]

             :release
             [:android-common
              {:target-path "target/release"
               :android
               {
                :ignore-log-priority [:debug :verbose]
                :aot :all
                :build-type :release}}]}

  :android {
            :dex-opts ["-JXmx4096M"]

            :manifest-options {:app-name "@string/app_name"}
            :target-version 21
            :aot-exclude-ns ["clojure.parallel" "clojure.core.reducers"
                             "cljs-tooling.complete" "cljs-tooling.info"
                             "cljs-tooling.util.analysis" "cljs-tooling.util.misc"
                             "cider.nrepl" "cider-nrepl.plugin"]})

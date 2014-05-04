(defproject incanter "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [incanter "1.5.4"]
                 [lein-light-nrepl "0.0.10"]]
  :repl-options {:nrepl-middleware [lighttable.nrepl.handler/lighttable-ops]}
  :jvm-opts ["-Xmx1g"])

(defproject gyfu "0.1.0-SNAPSHOT"
  :description "Test your XML with Clojure and XPath."
  :url "http://github.com/eerohele/gyfu"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha15"]
                 [net.sf.saxon/Saxon-HE "9.7.0-15"]
                 [org.clojure/data.xml "0.2.0-alpha2"]
                 [org.clojure/data.zip "0.1.2"]
                 [clansi "1.0.0"]]
  :jar-exclusions [#".*pain_mdr\.clj" #".*resources/examples/.*\.xml"]
  :main ^:skip-aot gyfu.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[org.clojure/test.check "0.9.0"]
                                  [criterium "0.4.4"]
                                  [org.xmlunit/xmlunit-core "2.3.0"]
                                  [org.iban4j/iban4j "3.2.1"]
                                  [clj-time "0.13.0"]]}}
  :codox {:metadata {:doc/format :markdown}
          :output-path "target/doc"}
  :test-selectors {:default (complement :bench)
                   :bench :bench
                   :all (constantly true)}
  :plugins [[lein-codox "0.10.3"]
            [lein-kibit "0.1.3"]
            [lein-marginalia "0.9.0"]
            [com.jakemccrary/lein-test-refresh "0.19.0"]
            [jonase/eastwood "0.2.3"]])

(ns gyfu.core
  (:require [gyfu.utils :as u]
            [gyfu.xpath :as xpath]
            [gyfu.saxon :as saxon])
  (:refer-clojure :exclude [apply compile]))

(defn eval-bindings
  "Evaluate a set of XPath variable bindings defined in a Schematron element.

  Example:

  ```
  (rule \"match/a/node\"
        {:let {:one 1}
        ...)
  ```

  This function evaluates `1` as an XPath expression and transforms the map into
  a binding vector like this: `[:one 1]`. That's what the functions in the
  [xpath] namespace take as a parameter.

  If you need to set the value of the variable to a string, use single quotes
  like you would in an XSLT `@select` attribute. As in:

  ```
  {:let {:foo \"'bar'\"}}
  ```"
  [compiler context element]
  (mapcat
    (fn [[name value]] [name (xpath/select compiler context value)])
    (-> element :attributes :let)))

(defn apply-test
  "Apply a Schematron test (an `assert` or `report` element)."
  [compiler node bindings {:keys [test message]}]
  {:node          node
   :line-number   (.getLineNumber node)
   :column-number (.getColumnNumber node)
   :message       (if (u/function? message)
                    (message (str (.getNodeName node))
                             (partial xpath/value-of compiler node))
                    message)
   :success       (if (u/function? test)
                    (-> node saxon/zipper test boolean)
                    (xpath/is? compiler node test bindings))})

(defn get-active-patterns
  "Given a Schematron schema and the name of the active schematron phase, get
  the set of active patterns in the schema."
  [schema active-phase]
  (let [patterns (:patterns schema)
        phases (-> schema :attributes :phases)]
    (set (cond (nil? active-phase) patterns
               (nil? phases) nil
               :else (when-let [active-pattern-ids (active-phase phases)]
                       (if (empty? active-pattern-ids)
                         patterns
                         (filter #(some #{(-> % :attributes :id)}
                                        active-pattern-ids)
                                 patterns)))))))

(defn- xpath-compiler
  ([processor options]
   (xpath/compiler processor
                   (:default-xpath-namespace options)
                   (:xpath-namespaces options)))
  ([options]
   (xpath-compiler saxon/processor options)))

(defn- compile-tests
  [compiler patterns]
  (for [pattern patterns]
    (for [rule (:rules pattern)]
      (for [assertion (:tests rule)]
        (merge {:pattern       (dissoc pattern :rules)
                :rule          (dissoc rule :tests)
                :xpath-pattern (xpath/pattern compiler (:context rule))}
               assertion)))))

(defn- compile-patterns
  [compiler patterns]
  (->> (compile-tests compiler patterns)
       flatten
       (sort-by #(.getDefaultPriority (:xpath-pattern %)))
       vec))

(defn compile
  "Compile a schema.

  The purpose of compiling the schema is to make applying the schema faster and
  make subsequent processing easier."
  [schema options]
  (let [compiler (xpath-compiler options)]
    {:schema  (dissoc schema :patterns)
     :options options
     :tests   (let [patterns (get-active-patterns schema (:phase options))]
                (compile-patterns compiler patterns))}))

(defn- merge-bindings
  [compiler document node schema-bindings {:keys [pattern rule]}]
  (-> schema-bindings
      (merge (eval-bindings compiler document pattern))
      (merge (eval-bindings compiler node rule))))

(defn- apply-matching-test
  [compiler document tests schema-bindings node]
  (if-let [test (u/seek #(xpath/matches? compiler node (:xpath-pattern %)) tests)]
    (let [bindings (merge-bindings compiler document node schema-bindings test)]
      (merge (apply-test compiler node bindings test)
             (dissoc test :xpath-pattern :message :test)))))

(defn apply
  "Validate the given XML document against a schema.

  Use [[schema]], [[pattern]], [[rule]] and [[assert]] to compose a schema,
  [[compile]] to compile the resulting schema, then hand it off to me.

  Example:

  ```
  ;; load some xml
  (def my-input-xml (saxon/build \"<match><a><node>1</node></a></match>\"))

  ;; define a schema
  (def my-awesome-schema
  (schema \"My awesome schema\"
          [(pattern \"My awesome pattern\"
                    [(rule \"match/a/node\"
                           ;; assert that the int value of <node> is greater than 0
                           [(assert \"Something awesome\" \"xs:int(.) gt 0\")])])]))

  ;; compile a schema and apply it to your input xml
  (-> my-awesome-schema (compile nil) (apply my-input-xml))
  ```
  "
  [{:keys [schema options tests]} document]
  (let [compiler (xpath-compiler options)
        schema-bindings (eval-bindings compiler document schema)]
    {:schema schema
     :tests  (->> (xpath/->seq document)
                  (map (fn [node]
                         (apply-matching-test compiler document tests schema-bindings node)))
                  (remove nil?))}))

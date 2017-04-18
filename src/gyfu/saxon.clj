(ns gyfu.saxon
  (:require [clojure.string :as string]
            [clojure.data.xml :as xml]
            [clojure.zip :as zip])
  (:import (net.sf.saxon Configuration)
           (net.sf.saxon.s9api Serializer
                               Processor
                               QName
                               XdmAtomicValue
                               XdmNode
                               XdmValue)
           (javax.xml.transform Source)
           (javax.xml.transform.stream StreamSource)
           (java.io File StringReader PipedInputStream PipedOutputStream)
           (java.net URI URL)
           (clojure.lang Keyword)))

(def ^Configuration configuration
  "A default Saxon [Configuration](http://www.saxonica.com/html/documentation/javadoc/net/sf/saxon/Configuration.html)."
  (Configuration.))

(def processor
  "A default Saxon [Processor](http://www.saxonica.com/html/documentation/javadoc/net/sf/saxon/s9api/Processor.html)."
  (Processor. configuration))

(def ^:private builder
  (doto (.newDocumentBuilder processor) (.setLineNumbering true)))

(defprotocol QNameable
  "A protocol for things that can be converted into a
  [QName](http://www.saxonica.com/html/documentation/javadoc/net/sf/saxon/s9api/QName.html)."
  (->qname [name]))

(extend-protocol QNameable QName
  (->qname [qname] qname))

(extend-protocol QNameable String
  (->qname [qname] (QName. qname)))

(extend-protocol QNameable Keyword
  (->qname [qname] (-> qname name ->qname)))

(defprotocol XmlNode
  "A protocol for things that can be converted into a Saxon
  [XdmNode](http://www.saxonica.com/html/documentation/javadoc/net/sf/saxon/s9api/XdmNode.html)."
  (build [source]))

(extend-protocol XmlNode XdmNode (build [xdmnode] xdmnode))

(extend-protocol XmlNode String
  (build [xml-string]
    (.build builder (StreamSource. (StringReader. xml-string)))))

(extend-protocol XmlNode File
  (build [file] (.build builder file)))

(extend-protocol XmlNode Source
  (build [source] (.build builder source)))

(extend-protocol XmlNode URI
  (build [^URI uri] (.build builder (StreamSource. uri))))

(extend-protocol XmlNode URL
  (build [^URL url] (.build builder (StreamSource. (.toString url)))))

(defprotocol XmlValue
  "A protocol for things that can be converted into a Saxon
  [XdmValue](http://www.saxonica.com/html/documentation/javadoc/net/sf/saxon/s9api/XdmValue.html)."
  (->xdmvalue [value]))

(extend-protocol XmlValue XdmValue
  (->xdmvalue [xdmvalue] xdmvalue))

(extend-protocol XmlValue Integer
  (->xdmvalue [int] (long int)))

(extend-protocol XmlValue Object
  (->xdmvalue [obj] (XdmAtomicValue. obj)))

(defn zipper
  "Create a zipper from a Saxon
  [XdmNode](http://www.saxonica.com/html/documentation/javadoc/net/sf/saxon/s9api/XdmNode.html).

  You can then use the functions in [clojure.data.zip.xml](https://clojure.github.io/data.zip/#clojure.data.zip.xml)
  to manipulate the zipper."
  [xdmnode]
  (let [input-stream (PipedInputStream.)
        output-stream (PipedOutputStream. input-stream)
        ^Serializer serializer (.newSerializer processor output-stream)]
    (.serializeNode serializer xdmnode)
    (-> input-stream xml/parse zip/xml-zip)))

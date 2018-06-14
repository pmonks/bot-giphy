;
; Copyright 2018 Peter Monks
; SPDX-License-Identifier: Apache-2.0
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
;

(ns bot-giphy.giphy
  (:require [clojure.string        :as s]
            [clojure.pprint        :as pp]
            [clojure.tools.logging :as log]
            [mount.core            :as mnt :refer [defstate]]
            [clj-http.client       :as http]
            [cheshire.core         :as ch]
            [clj-symphony.message  :as sym]
            [bot-giphy.config      :as cfg]
            [bot-giphy.connection  :as cnxn]))

(defn- query-string-encode
  "Encodes the given string for inclusion in the query string of a URL."
  [^String s]
  (java.net.URLEncoder/encode s "UTF-8"))

(defstate api-key
          :start (get-in cfg/config [:giphy :api-key]))

(defstate rating
          :start (get-in cfg/config [:giphy :rating] "G"))

(defstate timeout-ms
          :start (get-in cfg/config [:giphy :timeout] 1000))

(defstate proxy-host
          :start (get-in cfg/config [:giphy :proxy-host]))

(defstate proxy-port
          :start (if-let [port-str ^String (get-in cfg/config [:giphy :proxy-port])]
                   (Integer/valueOf port-str)))

(defstate url-template
          :start (str "https://api.giphy.com/v1/gifs/random?api_key=" (query-string-encode api-key)
                      "&rating=" (query-string-encode rating)
                      "&tag="))

(defn- strip-nil-values
  "Strips entries with nil values from map m."
  [m]
  (apply dissoc
         m
         (for [[k v] m :when (nil? v)] k)))

(defn- clojurise-json-key
  "Converts nasty JSON String keys (e.g. \"fullName\") to nice Clojure keys (e.g. :full-name)."
  [k]
  (keyword
    (s/replace
      (s/join "-"
              (map s/lower-case
                   (s/split k #"(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")))
      "_"
      "-")))

(defn- http-get
  "'Friendly' form of http/get that adds request information to any exceptions that get thrown by clj-http."
  [{ url     :url
     options :options
     :as request }]
  (try
    (http/get url options)
    (catch clojure.lang.ExceptionInfo ei
      (throw (ex-info (.getMessage ei) { :request  request
                                         :response (ex-data ei)})))))

(defn- random-giphy-image
  "Returns the URL of a random Giphy image for the given search term(s), or nil if it doesn't exist or Giphy returns an error."
  [search-term]
  (log/debug "Requesting a random image from Giphy for search term" search-term)
  (try
    (let [url      (str url-template (query-string-encode search-term))
          request  { :url     url
                     :options (strip-nil-values { :accept           :json
                                                  :socket-timeout   timeout-ms
                                                  :conn-timeout     timeout-ms
                                                  :proxy-host       proxy-host
                                                  :proxy-port       proxy-port }) }
          response (http-get request)
          json     (ch/parse-string (:body response) clojurise-json-key)]
      (get-in json [:data :images :fixed-width :url]))
    (catch Exception e
      (log/error "Unexpected exception while searching Giphy for" search-term ":" (str e))
      nil)))

(defn process-message!
  "Determines if the given message is a /giphy command and if so executes it."
  [message-id stream-id text]
  (when text
    (let [text (s/lower-case (s/trim (sym/to-plain-text text)))]
      (if (s/starts-with? text "/giphy ")
        (let [search-term     (s/replace-first text "/giphy " "")
              giphy-image-url (random-giphy-image search-term)]
          (when giphy-image-url
            (let [message (str "<div data-format=\"PresentationML\" data-version=\"2.0\">"
                               "<p><img src=\"" giphy-image-url "\"/></p>"
                               "</div>")]
              (log/debug "Search term(s)" search-term "in message" message-id "had a random image in Giphy - posting image to stream" stream-id ":" message)
              (sym/send-message! cnxn/symphony-connection
                                 stream-id
                                 message))))))))

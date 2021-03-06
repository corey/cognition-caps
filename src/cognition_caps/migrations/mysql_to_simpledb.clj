(ns cognition-caps.migrations.mysql-to-simpledb
  (:require [cognition-caps.data :as data]
            [cognition-caps.data [mysql :as mysql] [simpledb :as simpledb]]
            [cognition-caps.migrations.images-to-s3 :as images]
            [cemerick.rummage :as sdb])
  (:use [cognition-caps [config :only (config)]]))

(declare add-sizes)

(defn migrate-data! []
  (println "Migrating...")
  (println "Populating defaults in SimpleDB")
  (simpledb/populate-defaults!)
  (println "Querying data from ExpressionEngine/MySQL")
  (let [mysql-count   (atom 0)
        sdb-count     (atom 0)
        mysql-data    (mysql/make-MySQLAccess) ; ExpressionEngine database for old site
        simpledb-data (:db-impl config)
        sizes         (data/get-sizes simpledb-data sdb-count)
        items         (->> (data/get-items mysql-data mysql-count)
                        (map #(add-sizes % sizes))
                        (map images/migrate-images!))
        blog          (->> (data/get-blog mysql-data mysql-count)
                        (map images/migrate-blog-image!))
        ]
    (println "Loaded" (count items) "items and" (count blog) "blog entries from MySQL with"
             @mysql-count "queries and" (count (data/get-items simpledb-data sdb-count))
             "from SimpleDB with" @sdb-count "queries")
    (data/put-items simpledb-data sdb-count items)
    (data/put-blog simpledb-data sdb-count blog)
    ))

(defn- add-sizes [item sizes]
  "We're not carrying over the size/price relations from ExpressionEngine,
   so set up each item with the default sizing for the new system.
   Note that individual size entries are coded SIZE_ID:QTY, with -1 representing
   'unlimited' inventory."
  (if (:item-type-cap (:tags item))
    (let [item-with-sizes (assoc item :sizes (apply vector (map #(str (:id %) ":-1") sizes)))]
    item-with-sizes)
    item))


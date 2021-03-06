(ns cognition-caps.data
  (:use [clj-time.core :only (now)]
        [clj-time.coerce :only (to-long)])
  (:require [clojure.string :as s]))

(defprotocol DataAccess
  "A protocol abstracting access to product data"
  (get-item   [this queryCount url-title]   "Provides the item corresponding to the given url-title")
  (get-items  [this queryCount] [this queryCount sort-key order]
             "Provides a sequence of all visible items currently stored")
  (get-items-range [this queryCount begin limit]
             "Provides a sequence of 'limit' visible items beginning with the given display-order")
  (get-items-range-filter [this queryCount filter-tag begin limit]
             "Provides a sequence of 'limit' visible items containing `filter-tag` in :tags, beginning with the given display-order")
  (get-visible-item-count [this queryCount filter-tag] "Provides a count of available items for viewing, optionally limited by :item-type tags")
  (get-disabled-items [this queryCount]     "Returns a list of items marked disabled")
  (put-items  [this queryCount items]       "Persists items")
  (get-sizes  [this queryCount]             "Provides a list of available sizes")
  (get-prices [this queryCount]             "Provides a list of price categories")
  (update-item [this queryCount url-title
               attr-name attr-value]        "Updates the given attribute")
  (get-blog  [this queryCount]              "Returns all blog entries starting with the latest")
  (get-blog-range [this queryCount begin limit]
             "Provides a sequence of 'limit' blog entries beginning with the given starting index")
  (get-blog-entry [this queryCount url-title] "Provides the blog entry corresponding to the given url-title")
  (put-blog  [this queryCount items]        "Persists blog items")
  (get-visible-blog-count [this queryCount] "Provides a count of available blog entries for viewing")
  (get-user    [this queryCount id]         "Returns a user matching the given id")
  (get-user-by [this queryCount attr value] "Queries for a user with attr=value")
  (get-users [this queryCount]              "Returns the users in the system")
  (put-user [this queryCount user]          "Persists a user"))

(defn make-stubbed-client []
  "Stubbed implementation of DataAccess for use in unit tests"
  (reify DataAccess
    (get-visible-item-count [this query-count filter-tag] 0)
    (get-items-range [this queryCount begin limit] [])))

;; =============================================================================
;; Field notes:
;; display-order - The order in which items will be displayed to users.
;;                 If nil, the item is hidden.
;; date-added - in seconds, Unix time
;; =============================================================================
(defrecord Item [id nom url-title description image-urls price-ids sizes tags user-id date-added display-order])
(defn make-Item
  "Creates an Item from the given map, setting defaults when not present"
  [{:keys [id nom url-title description image-urls price-ids sizes tags user-id date-added display-order]
     :or {url-title id date-added (long (/ (to-long (now)) 1000)) display-order nil}}]
    (Item. id nom url-title description image-urls price-ids sizes tags user-id date-added display-order))

; id is the legacy ExpressionEngine value
(defrecord BlogEntry [id display-order title url-title image-url body user-id date-added])
(defn make-BlogEntry [{:keys [id display-order title url-title image-url body user-id date-added]}]
  (BlogEntry. id display-order title url-title image-url body user-id date-added))

(defrecord User [id username email facebook-id])
(defn make-User [{:keys [id username email facebook-id]}]
  "Creates a user from the given map"
  (User. id username email facebook-id))

(defrecord Size [id nom])

(defrecord Price [id price qty description])

;; =============================================================================
;; Default data 
;; =============================================================================
(def default-size (long 2))
(def default-sizes [{:id 1 :nom "Small-ish"}
                    {:id 2 :nom "One Size Fits Most"}
                    {:id 3 :nom "Large"}])

(def default-prices [{:id 1  :price "22.00" :description "Basic Cap"}
                     {:id 2  :price "24.00" :description "Cap w/Ribbon"}
                     {:id 3  :price "25.00" :description "Screenprinted"}
                     {:id 4  :price "27.00" :description "Wool"}
                     {:id 5  :price "30.00" :description "Winter Wool w/Earflaps"}
                     {:id 6  :price "33.00" :description "Gnome Fest"}
                     {:id 7  :price "35.00" :description "Faux Fur"}
                     {:id 8  :price "20.00" :description "FixedRiders.com"}
                     {:id 9  :price "1.00"  :description "Stickers & Buttons"}])

(def default-users [{:id 1 :username "Lyle" :email "lyle@wearcognition.com"}
                    {:id 2 :username "Kelly" :email "kelly@wearcognition.com"}])

;; =============================================================================
;; Utility functions
;; =============================================================================
(defn url-title [nom]
  "Returns the url-title for the given item name"
  (-> nom
      s/trim
      s/lower-case
      (s/replace #"'" "")
      (s/replace #"[\W&&[^\.]]+" "-")))

(defn hidden? [item]
  "Returns whether the item is hidden from the main listing and unavailable for sale"
  (= "-" (:display-order item)))

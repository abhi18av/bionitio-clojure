;;; Module:      bionitio.core
;;; Description: Reads one or more FASTA files and computes various
;;;              simple statistics about them. Intended as an example
;;;              bioinformatics command line tool.
;;; Copyright:   (c) BIONITIO_AUTHOR, BIONITIO_DATE 
;;; License:     BIONITIO_LICENSE 
;;; Maintainer:  BIONITIO_EMAIL  
;;; Stability:   stable
;;; Portability: POSIX
;;; The main parts of the program are:
;;; 1. Parse command line arguments.
;;; 2. Process each FASTA file in sequence.
;;; 3. Pretty print output for each file.
;;;
;;; If no FASTA filenames are specified on the command line then the program
;;; will try to read a FASTA file from standard input.

(ns bionitio.core
  (:require [bionitio.fasta :as fasta]
            [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [taoensso.timbre :as timbre]
            [cli-matic.core :refer [run-cmd]]
            [cli-matic.utils-v2 :as U2]
            [taoensso.timbre.appenders.core :as appenders])
  (:gen-class))

;; Set up logging infrastructure
(timbre/refer-timbre)


(defn init-logging
  "Initialise program logging if a log-filename has been specified, and also log
  a message indicating that the program has started. Log messages are configured to
  be written to the specified log file.

  Arguments:
    log-filename: Either a string indicating the name of the log file, or nil indicating
      that no logging should happen

  Result:
    nil"
  [log-filename]
  ;; Turn off the default logging to stdout.
  (timbre/merge-config! {:appenders {:println {:enabled? false}}})
  (when log-filename
      ;; Enable log messages to be written to the specified log file.
      (timbre/merge-config! {:appenders {:spit (appenders/spit-appender {:fname log-filename})}})
      ;; Set the minimum log level to "info"
      (timbre/set-level! :info))
    ;; Write a log message indicating that the program has started.
    (timbre/info "Program started")
    (timbre/info "Command line: " (string/join " " *command-line-args*)))


; Custom help text generation

(defn my-global-help
  "Generates custom help text for the command in general.
  This function takes cli-matic's configuration as its argument.
  cli-matic will print the string returned from this function verbatim.
  To walk through a CLI-matic config, there are plenty of useful fns in
  cli-matic.utils-v2
  "
  [cfg sub-cmd]
  (let [branch (U2/get-subcommand cfg sub-cmd)]
    (str
     "====== GENERIC 'global' HELP CONFIG: " sub-cmd "\n"
      branch)))

(defn echo-message
  [{:keys [message]}]
  (println message))

(def cli-configuration
  {:command      "helpgen"
   :description  "Demonstrates how to customize the generation of help text"
   :global-help  my-global-help
   :version      "0.1.18"
   :opts         []
   :subcommands  [{:command     "echo"
                   :description "echoes a message."
                   :opts        [{:option "message"
                                 :as "The message string to echo back."
                                 :type :string
                                 :default "oh-la-la"}]
                   :runs        echo-message}
                 ]})


(defn -main
  "Orchestrate the computation"
  [& args]
  (run-cmd args cli-configuration)

  #_(let [{:keys [options arguments errors summary]} (parse-opts args fasta/cli-options)]
    (cond
      (:help options)
         (do (println (fasta/usage summary))
               (System/exit fasta/exit-success))
      (:version options)
         (do (println (System/getProperty "bionitio.version"))
               (System/exit fasta/exit-success))
      errors (fasta/exit-with-error fasta/exit-failure-cli (fasta/error-msg errors)))
    (init-logging (:log options))
    (fasta/process-fasta-files (:minlen options) arguments)))

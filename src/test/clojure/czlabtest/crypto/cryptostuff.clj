;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.
;;
;; Copyright (c) 2013-2016, Kenneth Leung. All rights reserved.

(ns

  czlabtest.crypto.cryptostuff

  (:use [czlab.crypto.stores]
        [czlab.crypto.codec]
        [czlab.xlib.core]
        [czlab.xlib.str]
        [czlab.xlib.io]
        [clojure.test]
        [czlab.crypto.core])

  (:import
    [czlab.crypto Cryptor CryptoStoreAPI PasswordAPI]
    [java.security KeyPair Policy
     KeyStore
     SecureRandom
     MessageDigest
     KeyStore$PrivateKeyEntry
     KeyStore$TrustedCertificateEntry]
    [org.apache.commons.codec.binary Base64]
    [java.util Date GregorianCalendar]
    [java.io File]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(def ^:private ^chars C_KEY (.toCharArray "ed8xwl2XukYfdgR2aAddrg0lqzQjFhbs"))
(def ^:private ^chars B_KEY (bytesify "ed8xwl2XukYfdgR2aAddrg0lqzQjFhbs"))
(def ^:private TESTPWD (pwdify "secretsecretsecretsecretsecret"))
(def ^:private ENDDT (.getTime (GregorianCalendar. 2050 1 1)))
(def ^:private ROOTPFX (resBytes "czlab/crypto/test.pfx"))
(def ^:private ROOTJKS (resBytes "czlab/crypto/test.jks"))
(def ^:private HELPME (pwdify "helpme"))
(def ^:private SECRET (pwdify "secret"))

(def ^CryptoStoreAPI ^:private ROOTCS (cryptoStore (initStore! (getPkcsStore) ROOTPFX HELPME) HELPME))

(def ^CryptoStoreAPI ^:private ROOTKS (cryptoStore (initStore! (getJksStore) ROOTJKS HELPME) HELPME))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(deftest czlabtestcrypto-cryptostuff

(is (not (= "heeloo, how are you?" (caesarDecrypt (caesarEncrypt "heeloo, how are you?" 709394) 666))))
(is (= "heeloo, how are you?" (caesarDecrypt (caesarEncrypt "heeloo, how are you?" 709394) 709394)))
(is (= "heeloo, how are you?" (caesarDecrypt (caesarEncrypt "heeloo, how are you?" 13) 13)))

(is (= "heeloo" (let [c (jasyptCryptor)]
                  (.decrypt c C_KEY (.encrypt c C_KEY "heeloo")))))

(is (= "heeloo" (let [c (jasyptCryptor)
                      pkey (.toCharArray (nsb SECRET))]
                  (.decrypt c pkey (.encrypt c pkey "heeloo")))))

(is (= "heeloo" (let [c (javaCryptor)]
                  (stringify (.decrypt c B_KEY (.encrypt c B_KEY "heeloo"))))))

(is (= "heeloo" (let [c (javaCryptor)
                      pkey (bytesify (nsb TESTPWD))]
                  (stringify (.decrypt c pkey (.encrypt c pkey "heeloo"))))))

(is (= "heeloo" (let [c (bouncyCryptor)]
                  (stringify (.decrypt c B_KEY (.encrypt c B_KEY "heeloo"))))))

(is (= "heeloo" (let [c (bouncyCryptor)
                      pkey (bytesify (nsb TESTPWD))]
                  (stringify (.decrypt c pkey (.encrypt c pkey "heeloo"))))))

(is (= "heeloo" (let [pkey (bytesify (nsb TESTPWD))]
                  (stringify (bcDecr pkey (bcEncr pkey "heeloo" "AES") "AES")))))

(is (= "heeloo" (let [kp (asymKeyPair "RSA" 1024)
                      pu (.getEncoded (.getPublic kp))
                      pv (.getEncoded (.getPrivate kp))]
                  (stringify (asymDecr pv
                                       (asymEncr pu
                                                 (bytesify "heeloo")))))))

(is (= (.length ^String (.text (strongPwd 16))) 16))
(is (= (.length (randomStr 64)) 64))

(is (instance? PasswordAPI (pwdify "secret-text")))
(is (.startsWith ^String (.encoded ^PasswordAPI (pwdify "secret-text")) "crypt:"))


(is (= "SHA-512" (.getAlgorithm (msgDigest SHA_512))))
(is (= "MD5" (.getAlgorithm (msgDigest MD_5))))

(is (> (nextSerial) 0))

(is (> (.length (newAlias)) 0))

(is (= "PKCS12" (.getType (getPkcsStore))))
(is (= "JKS" (.getType (getJksStore))))

(is (instance? Policy (easyPolicy)))

(is (> (.length (genMac (bytesify "secret") "heeloo world")) 0))
(is (> (.length (genHash "heeloo world")) 0))

(is (not (nil? (asymKeyPair "RSA" 1024))))

(is (let [v (csrReQ 1024 "C=AU,ST=NSW,L=Sydney,O=Google,OU=HQ,CN=www.google.com" :PEM)]
      (and (= (count v) 2)
           (> (alength ^bytes (first v)) 0)
           (> (alength ^bytes (nth v 1)) 0))) )

(is (let [fout (tempFile "Kenneth Leung" ".p12")]
      (ssv1PKCS12 "C=AU,ST=NSW,L=Sydney,O=Google"
                  HELPME
                  fout
                  { :start (Date.) :end ENDDT :keylen 1024 })
      (> (.length fout) 0)))

(is (let [fout (tempFile "x" ".jks")]
      (ssv1JKS "C=AU,ST=NSW,L=Sydney,O=Google"
               SECRET
               fout
               { :start (Date.) :end ENDDT :keylen 1024 })
      (> (.length fout) 0)))

(is (let [^KeyStore$PrivateKeyEntry pke
          (.keyEntity ROOTCS
                      ^String (first (.keyAliases ROOTCS)) HELPME)
          fout (tempFile "x" ".p12")
          pk (.getPrivateKey pke)
          cs (.getCertificateChain pke)]
      (ssv3PKCS12 "C=AU,ST=NSW,L=Sydney,O=Google"
                  SECRET
                  fout
                  { :start (Date.) :end ENDDT :issuerCerts (seq cs) :issuerKey pk })
      (> (.length fout) 0)))

(is (let [^KeyStore$PrivateKeyEntry pke
          (.keyEntity ROOTKS
                      ^String (first (.keyAliases ROOTKS)) HELPME)
          fout (tempFile "x" ".jks")
          pk (.getPrivateKey pke)
          cs (.getCertificateChain pke)]
      (ssv3JKS "C=AU,ST=NSW,L=Sydney,O=Google"
               SECRET
               fout
               { :start (Date.) :end ENDDT :issuerCerts (seq cs) :issuerKey pk })
      (> (.length fout) 0)))

(is (let [^File fout (tempFile "x" ".p7b")]
      (exportPkcs7 (resUrl "czlab/crypto/test.pfx") HELPME fout)
      (> (.length fout) 0)))


)

;;(clojure.test/run-tests 'czlabtest.crypto.cryptostuff)


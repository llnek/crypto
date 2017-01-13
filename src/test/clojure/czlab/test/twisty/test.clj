;; Copyright (c) 2013-2017, Kenneth Leung. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns czlab.test.twisty.test

  (:use [czlab.twisty.stores]
        [czlab.twisty.codec]
        [czlab.twisty.ssl]
        [czlab.xlib.core]
        [czlab.xlib.meta]
        [czlab.xlib.str]
        [czlab.xlib.io]
        [clojure.test]
        [czlab.twisty.core])

  (:import [czlab.twisty PKeyGist Cryptor CryptoStore IPassword]
           [java.util Date GregorianCalendar]
           [java.io File]
           [java.math BigInteger]
           [java.security
            KeyPair
            Policy
            KeyStore
            SecureRandom
            MessageDigest
            KeyStore$PrivateKeyEntry
            KeyStore$TrustedCertificateEntry]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(def ^:private end-date (.getTime (GregorianCalendar. 2050 1 1)))

(def
  ^{:private true :tag (charsClass)}
  c-key (.toCharArray "ed8xwl2XukYfdgR2aAddrg0lqzQjFhbs"))

(def
  ^{:private true :tag (bytesClass)}
  b-key (bytesify "ed8xwl2XukYfdgR2aAddrg0lqzQjFhbs"))

(def
  ^{:private true :tag (charsClass)}
  test-pwd (.toCharArray "secretsecretsecretsecretsecret"))

(def
  ^{:private true :tag (bytesClass)}
  root-pfx (resBytes "czlab/test/twisty/test.pfx"))
(def
  ^{:private true :tag (bytesClass)}
  root-jks (resBytes "czlab/test/twisty/test.jks"))

(def
  ^{:private true :tag (charsClass)}
  help-me (.toCharArray "helpme"))

(def
  ^{:private true :tag (charsClass)}
  secret (.toCharArray "secret"))

(def ^:private ^CryptoStore
  root-cs (cryptoStore<> (initStore! (pkcsStore<>) root-pfx help-me) help-me))

(def ^:private ^CryptoStore
  root-ks (cryptoStore<> (initStore! (jksStore<>) root-jks help-me) help-me))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(deftest czlabtesttwisty-test

  (testing
    "related to: checking content-type"
    (is (isSigned? "application/x-pkcs7-mime; signed-data"))
    (is (isSigned? "multipart/signed"))
    (is (not (isSigned? "text/plain")))

    (is (isEncrypted? "application/x-pkcs7-mime; enveloped-data"))
    (is (not (isEncrypted? "text/plain")))

    (is (isCompressed? "application/pkcs7-mime; compressed-data"))
    (is (not (isCompressed? "text/plain"))))

  (is (not (jksFile? (resUrl "czlab/test/twisty/test.p12"))))
  (is (jksFile? (resUrl "czlab/test/twisty/test.jks")))

  (is (= "SHA-512" (.getAlgorithm (msgDigest "SHA-512"))))
  (is (= "MD5" (.getAlgorithm (msgDigest "MD5"))))

  (is (inst? BigInteger (nextSerial)))

  (is (not= (alias<>)(alias<>)))
  (is (string? (alias<>)))

  (testing
    "related to: crypto stores"

    (is (= "PKCS12" (.getType (pkcsStore<>))))
    (is (= "JKS" (.getType (jksStore<>))))

    (is (let [out (baos<>)
              x (.toCharArray "a")
              _ (.write root-cs out x)
              b (.toByteArray out)
              i (streamify b)
              s (cryptoStore<> (pkcsStore<> i x) x)]
          (some? (.intern s))))

    (is (let [a (.keyAliases root-cs)
              c (count a)
              n (first a)
              e (.keyEntity root-cs n help-me)]
          (and (== 1 c)
               (string? n)
               (inst? PKeyGist e))))

    (is (let [a (.certAliases root-cs) c (count a)] (== 0 c)))

    (is (let [g (convPKey (resUrl
                            "czlab/test/twisty/test.p12")
                          help-me
                          help-me)
              t (tempFile)
              t (exportPkcs7File g t)
              z (.length t)
              c (.cert g)
              b (exportCert c)]
          (deleteQ t)
          (and (> z 10)
               (> (alength b) 10)))))

  (is (some? (easyPolicy<>)))

  (testing
    "related to: mac & hash"
    (is (= (genMac b-key "hello world")
           (genMac b-key "hello world")))

    (is (not= (genMac b-key "hello maria")
              (genMac b-key "hello world")))

    (is (= (genHash "hello world")
           (genHash "hello world")))

    (is (not= (genHash "hello maria")
              (genHash "hello world"))))

  (testing
    "related to: keypairs"
    (is (let [kp (asymKeyPair<> "RSA" 1024)
              b (exportPEM kp secret)
              pub (.getPublic kp)
              prv (.getPrivate kp)
              b1 (exportPrivateKey prv secret)
              b2 (exportPublicKey pub)]
          (and (hgl? (stringify b))
               (hgl? (stringify b1))
               (hgl? (stringify b2))))))

  (testing
    "related to: cert service request"
    (is (let [[a b]
              (csreq<> "C=AU,O=Org,OU=OUnit,CN=joe" 1024)]
          (and (instBytes? a)
               (instBytes? b))))
    (is (let [v (csreq<>
                  "C=US,ST=CA,L=X,O=Z,OU=HQ,CN=joe" 1024 secret)]
          (and (= (count v) 2)
               (> (alength ^bytes (first v)) 0)
               (> (alength ^bytes (nth v 1)) 0)))))

  (is (let [s (session<> "joe" secret)
            s0 (session<>)
            b (resBytes "czlab/test/twisty/mime.eml")
            m (mimeMsg<> (streamify b))
            c (.getContent m)
            z (isDataCompressed? c)
            g (isDataSigned? c)
            e (isDataEncrypted? c)]
        (and (not z)(not g)(not e))))

  (is (some? (getCharset "text/plain; charset=utf-16")))

  (testing
    "related to: msg digest"
    (is (not= (digest<sha1> (bytesify "hello world"))
              (digest<md5> (bytesify "hello world"))))

    (is (= (digest<sha1> (bytesify "hello world"))
           (digest<sha1> (bytesify "hello world"))))

    (is (= (digest<md5> (bytesify "hello world"))
           (digest<md5> (bytesify "hello world")))))

  (is (let [b (resBytes "czlab/test/twisty/cert.crt")
            c (convCert b)
            g (certGist c)
            ok? (validCert? c)]
        (and (some? c) (some? g) ok?)))

  (is (some? (simpleTrustMgr<>)))

  (testing
    "related to: caesar crypto"
    (is (not= "heeloo, how are you?"
              (caesarDecrypt 666
                             (caesarEncrypt 709394
                                            "heeloo, how are you?"))))

    (is (= "heeloo, how are you?"
           (caesarDecrypt 709394
                          (caesarEncrypt 709394
                                         "heeloo, how are you?"))))

    (is (= "heeloo, how are you?"
           (caesarDecrypt 13 (caesarEncrypt 13 "heeloo, how are you?")))))

  (testing
    "related to: jasypt crypto"
    (is (= "heeloo"
           (let [c (jasyptCryptor<>)]
             (.decrypt c
                       c-key
                       (.encrypt c c-key "heeloo")))))

    (is (= "heeloo"
           (let [c (jasyptCryptor<>)
                 pkey secret]
             (.decrypt c
                       pkey
                       (.encrypt c pkey "heeloo"))))))

  (testing
    "related to: java crypto"
    (is (= "heeloo"
           (let [c (javaCryptor<>)]
             (stringify (.decrypt c
                                  b-key
                                  (.encrypt c b-key "heeloo"))))))

    (is (= "heeloo"
           (let [c (javaCryptor<>)
                 pkey (bytesify (String. test-pwd))]
             (stringify (.decrypt c
                                  pkey (.encrypt c pkey "heeloo")))))))

  (testing
    "related to: bouncycastle crypto"
    (is (= "heeloo"
           (let [c (bcastleCryptor<>)]
             (stringify (.decrypt c
                                  b-key
                                  (.encrypt c b-key "heeloo"))))))

    (is (= "heeloo"
           (let [c (bcastleCryptor<>)
                 pkey (bytesify (String. test-pwd))]
             (stringify (.decrypt c pkey (.encrypt c pkey "heeloo"))))))

    (is (= "heeloo"
           (let [kp (asymKeyPair<> "RSA" 1024)
                 pu (.getEncoded (.getPublic kp))
                 pv (.getEncoded (.getPrivate kp))]
             (stringify (asymDecr pv
                                  (asymEncr pu (bytesify "heeloo"))))))))

  (testing
    "related to: passwords"
    (is (= (.length (.text (strongPwd<> 16))) 16))
    (is (= (.length (randomStr 64)) 64))

    (is (inst? IPassword (passwd<> "secret-text")))

    (is (.startsWith (.encoded (passwd<> "secret-text")) "crypt:")))

  (testing
    "related to: keystores"

    (is (let [ks (ssv1PKCS12 "C=AU,ST=NSW,L=Sydney,O=Google"
                             secret {:end end-date :keylen 1024 })
              fout (tempFile "Joe Blogg" ".p12")
              ok? (inst? KeyStore ks)
              f (spitKeyStore ks fout help-me)
              len (.length f)]
          (deleteQ f)
          (and ok? (> len 0))))

    (is (let [ks (ssv1JKS "C=AU,ST=WA,L=X,O=Z" secret {:end end-date})
              fout (tempFile "xxxx" ".jks")
              ok? (inst? KeyStore ks)
              f (spitKeyStore ks fout help-me)
              len (.length f)]
          (deleteQ f)
          (and ok? (> len 0))))

    (is (let [r (.keyEntity root-cs help-me)
              fout (tempFile "xxxx" ".p12")
              ks (ssv3PKCS12 r
                             "C=AU,ST=WA,L=Z,O=X"
                             secret {:end end-date})
              ok? (inst? KeyStore ks)
              f (spitKeyStore ks fout help-me)
              len (.length f)]
          (deleteQ f)
          (and ok? (> len 0))))

    (is (let [r (.keyEntity root-ks help-me)
              fout (tempFile "xxxx" ".jks")
              ks (ssv3JKS r
                          "C=AU,ST=WA,L=Z,O=X"
                          secret {:end end-date})
              ok? (inst? KeyStore ks)
              f (spitKeyStore ks fout help-me)
              len (.length f)]
          (deleteQ f)
          (and ok? (> len 0))))

    (is (let [r (.keyEntity root-cs help-me)
              fout (tempFile "xxxx" ".p7b")
              b (exportPkcs7 r)
              f (exportPkcs7File r fout)
              len (.length f)]
          (and (instBytes? b) (> len 0)))))

  (is (string? "That's all folks!")))


;;(clojure.test/run-tests 'czlab.test.twisty.test)

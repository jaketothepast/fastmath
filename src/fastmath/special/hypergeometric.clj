(ns fastmath.special.hypergeometric
  "Internal Hypergeometric helper functions."
  (:require [fastmath.core :as m]
            [fastmath.special.poly :as spoly]
            [fastmath.polynomials :as poly]
            [fastmath.vector])
  (:import [org.apache.commons.math3.special Gamma]
           [fastmath.vector Vec4]))

(set! *unchecked-math* :warn-on-boxed)
(set! *warn-on-reflection* true)

;; https://github.com/JuliaMath/HypergeometricFunctions.jl/blob/master/src/weniger.jl#L226
(defn weniger-1F1
  ^double [^double a ^double b ^double x]
  (let [absa (m/abs a)
        zeta (m// x)
        nlo (m// (m/* b zeta) a)
        dlo nlo
        a0 (m/inc a)
        b0 (m/* 2.0 (m/inc b))
        b0zeta (m/* b0 zeta)
        dmid (m/* dlo (m/- b0zeta a0))
        nmid (m/+ dmid b0zeta)
        tmid (m// nmid dmid)]
    (if (m/< (m/abs a0) (m/ulp (m/inc absa)))
      tmid
      (let [nmid (m// nmid a0)
            dmid (m// dmid a0)
            a0 (m/+ a 2.0)
            b0 (m/* 6.0 (m/+ b 2.0))
            b1 (m/* -6.0 b)
            t0 (m/+ (m/* b0 zeta) (m/* 3.0 a))
            t1 (m/+ (m/* b1 zeta) (m/* 4.0 a) 2.0)
            nhi (m/+ (m/* t0 nmid) (m/* t1 nlo) (m/* b1 zeta))
            dhi (m/+ (m/* t0 dmid) (m/* t1 dlo))
            thi (m// nhi dhi)]
        (if (m/< (m/abs a0) (m/ulp (m/+ absa 2.0)))
          thi
          (let [nhi (m// nhi a0)
                dhi (m// dhi a0)]
            (loop [k (long 2)
                   tlo 1.0
                   tmid tmid
                   thi thi
                   nlo nlo
                   nmid nmid
                   nhi nhi
                   dlo dlo
                   dmid dmid
                   dhi dhi]
              (if (or (m/< k 5)
                      (and (m/< k 1048576) (m/> (m/abs (m/- tmid thi))
                                                (m/* m/MACHINE-EPSILON10 (m/max (m/abs tmid)
                                                                                (m/abs thi))))))
                (let [k2 (m/* 2.0 k)
                      a0 (m/+ a k 1.0)
                      a2 (m// (m/* (m/- a k -1.0) (m/inc k2)) (m/dec k2))
                      k42 (m/+ (m/* 4.0 k) 2.0)
                      b0 (m/* k42 (m/inc (m/+ k b)))
                      b1 (m/* k42 (m/dec (m/- k b)))
                      t0 (m/+ (m/* b0 zeta) a2)
                      t1 (m/+ (m/* b1 zeta) a0)
                      nnhi (m/- (m/+ (m/* t0 nhi) (m/* t1 nmid)) (m/* a2 nlo))
                      ndhi (m/- (m/+ (m/* t0 dhi) (m/* t1 dmid)) (m/* a2 dlo))
                      nthi (m// nnhi ndhi)]
                  (if (m/< (m/abs a0) (m/ulp (m/inc (m/+ absa k))))
                    nthi
                    (recur (m/inc k)
                           tmid thi nthi
                           nmid nhi (m// nnhi a0)
                           dmid dhi (m// ndhi a0))))
                (cond
                  (m/valid-double? thi) thi
                  (m/valid-double? tmid) tmid
                  :else tlo)))))))))

(defn cosnasinsqrt
  ^double [^double n ^double x]
  (if (m/pos? x)
    (m/cos (m/* n (m/asin (m/sqrt x))))
    (m/cosh (m/* n (m/asinh (m/sqrt (m/- x)))))))

(defn sinnasinsqrt
  ^double [^double n ^double x]
  (if (m/pos? x)
    (let [s (m/sqrt x)] (m// (m/sin (m/* n (m/asin s))) (m/* n s)))
    (let [s (m/sqrt (m/- x))] (m// (m/sinh (m/* n (m/asinh s))) (m/* n s)))))

(defn expnlog1pcoshatanhsqrt
  ^double [^double n ^double x]
  (let [v (m/exp (m/* 0.5 n (m/log1p (m/- x))))]
    (if (m/pos? x)
      (m/* v (m/cosh (m/* n (m/atanh (m/sqrt x)))))
      (m/* v (m/cos (m/* n (m/atan (m/sqrt (m/- x)))))))))

(defn sqrtasinsqrt
  ^double [^double x]
  (if (m/pos? x)
    (let [s (m/sqrt x)] (m// (m/asin s) s))
    (let [s (m/sqrt (m/- x))] (m// (m/asinh s) s))))

(defn sqrtatanhsqrt
  ^double [^double x]
  (if (m/pos? x)
    (let [s (m/sqrt x)] (m// (m/atanh s) s))
    (let [s (m/sqrt (m/- x))] (m// (m/atan s) s))))

(defn log1pover ^double [^double x] (m// (m/log1p x) x))
(defn expm1nlog1p ^double [^double n ^double x] (m// (m/expm1 (m/* n (m/log1p x))) (m/* n x)))

(defn maclaurin-2F1
  ^double [^double a ^double b ^double c ^double x]
  (loop [j (long 1)
         s0 1.0
         s1 (m/inc (m// (m/* a b x) c))]
    (if (or (m/delta-eq s0 s1 m/MACHINE-EPSILON10 m/MACHINE-EPSILON10)
            (m/== j 1048576))
      s1
      (let [rj (m// (m/* (m// (m/* (m/+ a j) x)
                              (m/inc j))
                         (m/+ b j))
                    (m/+ c j))]
        (recur (m/inc j) s1 (m/+ s1 (m/* (m/- s1 s0) rj)))))))

(defn recinfa0
  ^double [^Vec4 abcw ^long m ^double eps]
  (if (m/zero? eps)
    (let [res (m// (m/* (Gamma/gamma m) (Gamma/gamma (.z abcw)))
                   (m/* (Gamma/gamma (m/+ m (.x abcw)))
                        (Gamma/gamma (m/- (.z abcw) (.x abcw)))))]
      (if (m/even? m) res (m/- res)))
    (m// (Gamma/gamma (.z abcw))
         (m/* eps
              (Gamma/gamma (m/- 1.0 m eps))
              (Gamma/gamma (m/+ (.x abcw) m eps))
              (Gamma/gamma (m/- (.z abcw) (.x abcw)))))))

(defn reconea0
  ^double [^Vec4 abcw ^long m ^double eps]
  (if (m/zero? eps)
    (let [res (m// (m/* (Gamma/gamma m) (Gamma/gamma (.z abcw)))
                   (m/* (Gamma/gamma (m/+ m (.x abcw)))
                        (Gamma/gamma (m/+ m (.y abcw)))))]
      (if (m/even? m) res (m/- res)))
    (m// (Gamma/gamma (.z abcw))
         (m/* eps
              (Gamma/gamma (m/- 1.0 m eps))
              (Gamma/gamma (m/+ (.x abcw) m eps))
              (Gamma/gamma (m/+ (.y abcw) m eps))))))

(defn ainf
  ^double [^Vec4 abcw ^long m ^double eps]
  (let [m-2 (m/- m 2)]
    (if (m/not-pos? m)
      0.0
      (loop [a0 (recinfa0 abcw m eps)
             res a0
             n (long 0)]
        (if (m/> n m-2)
          res
          (let [na0 (m/* a0 (.w abcw) (m// (m/* (m/+ (.x abcw) n)
                                                (m/- (m/+ 1.0 (.x abcw) n) (.z abcw)))
                                           (m/* (m/inc n)
                                                (m/+ n (m/- 1.0 m eps)))))]
            (recur na0 (m/+ res na0) (m/inc n))))))))

(defn aone
  ^double [^Vec4 abcw ^long m ^double eps]
  (let [m-2 (m/- m 2)]
    (if (m/not-pos? m)
      0.0
      (loop [a0 (reconea0 abcw m eps)
             res a0
             n (long 0)]
        (if (m/> n m-2)
          res
          (let [na0 (m/* a0 (.w abcw) (m// (m/* (m/+ (.x abcw) n)
                                                (m/+ (.y abcw) n))
                                           (m/* (m/inc n)
                                                (m/+ n (m/- 1.0 m eps)))))]
            (recur na0 (m/+ res na0) (m/inc n))))))))

(defmacro ^:private lanczos-ratio-macro
  [x eps c0 & c]
  `(let [~'xpe (m/+ ~x ~eps)]
     ~(let [[n d] (reduce (fn [[num den] [k ck]]
                            (let [temp `(m// (m/+ ~x ~k))]
                              [`(m/muladd ~ck (m// ~temp (m/+ ~'xpe ~k)) ~num)
                               `(m/muladd ~ck ~temp ~den)]))
                          [0.0 c0] (map-indexed vector c))]
        `(m// ~n ~d))))

(defn lanczos-ratio
  ^double [^double x ^double eps]
  (lanczos-ratio-macro x eps 0.99999999999999709182, 57.156235665862923517, -59.597960355475491248, 14.136097974741747174, -0.49191381609762019978, 0.33994649984811888699E-4, 0.46523628927048575665E-4, -0.98374475304879564677E-4, 0.15808870322491248884E-3, -0.21026444172410488319E-3, 0.21743961811521264320E-3, -0.16431810653676389022E-3, 0.84418223983852743293E-4, -0.26190838401581408670E-4, 0.36899182659531622704E-5))

(defn H
  ^double [^double x ^double eps]
  (let [zm0p5 (m/- x 0.5)
        zpgm0p5 (m/+ zm0p5 4.7421875)]
    (if (m/>= x 0.5 )
      (if (m/zero? eps)
        (m/- (m/dec (m/+ (m// zm0p5 zpgm0p5) (m/log zpgm0p5))) (lanczos-ratio x eps))
        (m// (m/expm1 (m/- (m/+ (m/* zm0p5 (m/log1p (m// eps zpgm0p5)))
                                (m/* eps (m/log (m/+ zpgm0p5 eps)))
                                (m/log1p (m/* -1.0 eps (lanczos-ratio x eps)))) eps)) eps))
      (let [tpz (m/tanpi x)]
        (if (m/zero? eps)
          (m/- (H (m/- 1.0 x) eps) (m// m/PI tpz))
          (let [heps (m/* 0.5 eps)
                temp (m/- (m/+ (m/* (m/+ (m/cospi eps) (m// (m/sinpi eps) tpz)) (H (m/- 1.0 x) (m/- eps)))
                               (m/* heps (m/sq (m/* m/PI (m/sinc heps)))))
                          (m// (m/* m/PI (m/sinc eps)) tpz))]
            (m// temp (m/- 1.0 (m/* eps temp)))))))))

(defn G
  ^double [^double x ^double eps]
  (let [n (m/round x)
        zpe (m/+ x eps)]
    (cond
      (m/zero? eps) (if (and (m/== x n)
                             (m/not-pos? n))
                      (if (m/odd? n) (Gamma/gamma (m/- 1.0 n)) (m/- (Gamma/gamma (m/- 1.0 n))))
                      (m// (Gamma/digamma x) (Gamma/gamma x)))
      (m/> (m/abs eps) 0.1) (m// (m/- (m// (Gamma/gamma x))
                                      (m// (Gamma/gamma zpe))) eps)
      :else (let [m (m/round zpe)]
              (cond
                (and (m/== x n)
                     (m/not-pos? n)) (m/- (m// (m/* eps (Gamma/gamma zpe))))
                (and (m/== zpe m)
                     (m/not-pos? m)) (m// (m/* eps (Gamma/gamma x)))
                (m/< (m/abs (m/+ x (m/abs n)))
                     (m/abs (m/+ zpe (m/abs m)))) (m// (H x eps) (Gamma/gamma zpe))
                :else (m// (H zpe (m/- eps)) (Gamma/gamma x)))))))

(defn P
  ^double [^double x ^double eps ^long m]
  (let [n0 (m/- (m/round x))]
    (if (m/zero? eps)
      (if (and (m/not-neg? n0) (m/< n0 m))
        (loop [n (long 0)
               ret1 1.0
               ret2 0.0]
          (cond
            (m/== n m) (m/+ ret1 (m/* (m/rising-factorial-int m x) ret2))
            (m/== n n0) (recur (m/inc n) ret1 ret2)
            :else (recur (m/inc n) (m/* ret1 (m/+ x n)) (m/+ ret2 (m// (m/+ x n))))))
        (loop [n (long 0)
               ret 0.0]
          (if (m/== n m)
            (m/* (m/rising-factorial-int m x) ret)
            (recur (m/inc n) (m/+ ret (m// (m/+ x n)))))))
      (if (and (m/not-neg? n0) (m/< n0 m))
        (let [zpe (m/+ x eps)]
          (loop [n (long 0)
                 ret1 1.0
                 ret2 0.0]
            (cond
              (m/== n m) (m/+ ret1 (m// (m/* (m/rising-factorial-int m x) (m/expm1 ret2)) eps))
              (m/== n n0) (recur (m/inc n) ret1 ret2)
              :else (recur (m/inc n) (m/* ret1 (m/+ zpe n)) (m/+ ret2 (m/log1p (m// eps (m/+ x n))))))))
        (loop [n (long 0)
               ret 0.0]
          (if (m/== n m)
            (m// (m/* (m/rising-factorial-int m x) (m/expm1 ret)) eps)
            (recur (m/inc n) (m/+ ret (m/log1p (m// eps (m/+ x n)))))))))))

(defn E
  ^double [^double x ^double eps]
  (if (m/zero? eps) x
      (m// (m/expm1 (m/* eps x)) eps)))

(defn recinfb0
  ^double [^Vec4 abcw ^long m ^double eps]
  (let [a (.x abcw)        
        c (.z abcw)
        w (.w abcw)]
    (if (m/> (m/abs eps) 0.1)
      (m// (m/* (m/- (m// (m/* (m/rising-factorial-int m a)
                               (m/rising-factorial-int m (m/inc (m/- a c))))
                          (m/* (Gamma/gamma (m/- 1.0 eps))
                               (Gamma/gamma (m/+ a m eps))
                               (Gamma/gamma (m/- c a))
                               (Gamma/gamma (m/inc m))))
                     (m// (m/* (m/pow (m/- w) eps) (m/rising-factorial-int m (m/- (m/+ 1.0 a eps) c)))
                          (m/* (Gamma/gamma a)
                               (Gamma/gamma (m/- c a eps))
                               (Gamma/gamma (m/+ m 1.0 eps)))))
                (Gamma/gamma c)
                (m/fpow w m)) eps)
      (let [-eps (m/- eps)
            ph (m/rising-factorial-int m (m/- (m/+ 1.0 a eps) c))
            game (Gamma/gamma (m/+ a m eps))
            gca (Gamma/gamma (m/- c a))
            gm1e (Gamma/gamma (m/+ m 1.0 eps))]
        (m/* (m/+ (m// (m/- (m/* ph (G 1.0 -eps))
                            (m// (P (m/inc (m/- a c)) eps m) (Gamma/gamma (m/- 1.0 eps))))
                       (m/* gca game (Gamma/gamma (m/inc m))))
                  (m/* ph (m/- (m// (m/- (m// (G (m/inc m) eps) game)
                                         (m// (G (m/+ a m) eps) gm1e))
                                    gca)
                               (m// (m/- (G (m/- c a) -eps)
                                         (m// (E (m/- (m/log (m/- w))) -eps)
                                              (Gamma/gamma (m/- c a eps))))
                                    (m/* gm1e (Gamma/gamma (m/+ a m)))))))
             (Gamma/gamma c)
             (m/rising-factorial-int m a)
             (m/fpow w m))))))

(defn reconeb0
  ^double [^Vec4 abcw ^long m ^double eps]
  (let [a (.x abcw)
        b (.y abcw)
        c (.z abcw)
        w (.w abcw)]
    (if (m/> (m/abs eps) 0.1)
      (let [m+ (m/inc m)]
        (m// (m/* (m/- (m// (m/* (m/rising-factorial-int m a)
                                 (m/rising-factorial-int m b))
                            (m/* (Gamma/gamma (m/- 1.0 eps))
                                 (Gamma/gamma (m/+ a m eps))
                                 (Gamma/gamma (m/+ b m eps))
                                 (Gamma/gamma m+)))
                       (m// (m/pow w eps)
                            (m/* (Gamma/gamma a)
                                 (Gamma/gamma b)
                                 (Gamma/gamma (m/+ m+ eps)))))
                  (Gamma/gamma c)
                  (m/fpow w m)) eps))
      (let [-eps (m/- eps)
            m+ (m/inc m)
            gbme (Gamma/gamma (m/+ b m eps))
            gam (Gamma/gamma (m/+ a m))
            gm1eps (Gamma/gamma (m/+ m+ eps))]
        (m/* (m/- (m// (m/+ (m// (G 1.0 -eps) (Gamma/gamma m+)) (G m+ eps))
                       (m/* (Gamma/gamma (m/+ a m eps)) gbme))
                  (m// (m/+ (m// (G (m/+ a m) eps) gbme)
                            (m// (G (m/+ b m) eps) gam))
                       gm1eps)
                  (m// (E (m/log w) eps)
                       (m/* gam (Gamma/gamma (m/+ b m)) gm1eps)))
             (Gamma/gamma c)
             (m/rising-factorial-int m a)
             (m/rising-factorial-int m b)
             (m/fpow w m))))))

(defn recinfg0
  ^double [^Vec4 abcw ^long m ^double eps]
  (let [a (.x abcw)
        c (.z abcw)
        w (.w abcw)]
    (m// (m/* (Gamma/gamma c)
              (m/rising-factorial-int m a)
              (m/rising-factorial-int m (m/inc (m/- a c)))
              (m/pow w m))
         (m/* (Gamma/gamma (m/+ a m eps))
              (Gamma/gamma (m/- c a))
              (Gamma/gamma (m/inc m))
              (Gamma/gamma (m/- 1.0 eps))))))

(defn- reconeg0
  ^double [^Vec4 abcw ^long m ^double eps]
  (let [a (.x abcw)
        b (.y abcw)
        c (.z abcw)
        w (.w abcw)]
    (m// (m/* (Gamma/gamma c)
              (m/rising-factorial-int m a)
              (m/rising-factorial-int m b)
              (m/pow w m))
         (m/* (Gamma/gamma (m/+ a m eps))
              (Gamma/gamma (m/+ b m eps))
              (Gamma/gamma (m/inc m))
              (Gamma/gamma (m/- 1.0 eps))))))

(defn binf
  ^double [^Vec4 abcw ^long m ^double eps]
  (let [a (.x abcw)
        c (.z abcw)
        w (.w abcw)
        ame (m/+ a m eps)
        iame-c (m/inc (m/- ame c))]
    (loop [n (long 0)
           bn (recinfb0 abcw m eps)
           gn (m/* w (recinfg0 abcw m eps))
           ret bn]
      (if (or (and (m/pos? n )
                   (m/< (m/abs bn) (m/* 0.5 (m/abs ret) m/MACHINE-EPSILON)))
              (m/== n 1048576))
        ret        
        (let [n+ (m/inc n)
              v1 (m/* (m/+ ame n) (m/+ iame-c n))
              v2 (m/- (m/+ 1.0 a m n) c)
              nbn (m/+ (m/* (m// v1 (m/* (m/+ m n 1.0 eps) n+)) w bn)
                       (m// (m/* (m/+ (m/- (m// (m/* (m/+ a m n) v2)
                                                (m/+ m n 1))
                                           (m/+ a m n) v2 eps)
                                      (m// v1 n+)) gn) (m/* (m/+ m n 1.0 eps) (m/- n+ eps))))
              ngn (m/* gn w (m// (m/* (m/+ a m n) v2)
                                 (m/* (m/+ m n 1) (m/- n+ eps))))]
          (recur n+ nbn ngn (m/+ ret nbn)))))))

(defn bone
  ^double [^Vec4 abcw ^long m ^double eps]
  (let [a (.x abcw)
        b (.y abcw)
        w (.w abcw)
        ame (m/+ a m eps)
        bme (m/+ b m eps)
        m1e (m/+ m 1.0 eps)]
    (loop [n (long 0)
           bn (reconeb0 abcw m eps)
           gn (m/* w (reconeg0 abcw m eps))
           ret bn]
      (if (or (and (m/pos? n )
                   (m/< (m/abs bn) (m/* 0.5 (m/abs ret) m/MACHINE-EPSILON)))
              (m/== n 1048576))
        ret        
        (let [n+ (m/inc n)
              v1 (m/* (m/+ ame n) (m/+ bme n))
              mn1e (m/+ m1e n)
              amn (m/+ a m n)
              bmn (m/+ b m n)
              v2 (m/* amn bmn)
              nbn (m/+ (m/* (m// v1 (m/* mn1e n+)) w bn)
                       (m// (m/* (m/+ (m/- (m// v2 (m/+ m n 1))
                                           amn bmn eps)
                                      (m// v1 n+)) gn) (m/* mn1e (m/- n+ eps))))
              ngn (m/* gn w (m// v2 (m/* (m/+ m n 1) (m/- n+ eps))))]
          (recur n+ nbn ngn (m/+ ret nbn)))))))

(defn inf-2F1
  ^double [^double a ^double b ^double c ^double x]
  (let [b-a (m/- b a)
        m (m/round b-a)
        eps (m/- b-a m)
        w (m// x)
        abcw (Vec4. a b c w)
        res (m/* (m// (m/pow (m/- w) a) (m/sinc eps))
                 (m/+ (ainf abcw m eps) (binf abcw m eps)))]
    (if (m/even? m) res (m/- res))))

(defn one-2F1
  ^double [^double a ^double b ^double c ^double x]
  (let [c-b-a (m/- c b a)
        m (m/round c-b-a)
        eps (m/- c-b-a m)
        w (m/- 1.0 x)
        abcw (Vec4. a b c w)
        res (m/* (m// (m/sinc eps))
                 (m/+ (aone abcw m eps) (bone abcw m eps)))]
    (if (m/even? m) res (m/- res))))

(defn weniger-2F1
  ^double [^double a ^double b ^double c ^double x]
  (let [absa (m/abs a)
        absb (m/abs b)
        ab (m/* a b)]
    (if (or (m/< (m/abs x) m/MACHINE-EPSILON)
            (m/< (m/abs ab) (m/ulp (m/* absa absb))))
      1.0
      (let [ulo (m// ab c)
            rlo 1.0
            a0 (m/* (m/inc a) (m/inc b))
            b0 (m/* 2.0 (m/inc c))
            umid (m// (m/- b0 (m/* a0 x)))
            rmid (m/+ rlo (m/* b0 x umid ulo))]
        (if (m/< (m/abs a0) (m/ulp (m/* (m/inc absa) (m/inc absb))))
          rmid
          (let [umid (m/* umid a0)
                a0 (m/* (m/+ a 2.0) (m/+ b 2.0))
                b0 (m/* 6.0 (m/+ c 2.0))
                b1 (m/* -6.0 c)
                t0 (m/- b0 (m/* (m/- 6.0 (m/* 3.0 ab)) x))
                t1 (m/+ b1 (m/* 2.0 (m/+ (m/* 2.0 ab) a b -1.0) x))
                uhi (m// (m/+ t0 (m/* t1 x umid)))
                rhi (m/* (m/+ (m/* t0 rmid)
                              (m/* (m/+ (m/* t1 rlo)
                                        (m/* b1 x ulo)) x umid)) uhi)]
            (if (m/< (m/abs a0) (m/ulp (m/* (m/+ absa 2.0) (m/+ absb 2.0))))
              rhi
              (let [uhi (m/* uhi a0)
                    z2 (m/* x x)]
                (loop [k (long 2)
                       uhi uhi umid umid
                       rhi rhi rmid rmid rlo rlo]
                  (if (or (m/< k 5)                          
                          (and (m/< k 1048576) (m/> (m/abs (m/- rmid rhi)) (m/* m/MACHINE-EPSILON10
                                                                                (m/max (m/abs rmid)
                                                                                       (m/abs rhi))))))
                    (let [k+ (m/inc k)
                          k2 (m/* 2 k)
                          k3 (m/* 3 k)
                          k2+ (m/inc k2)
                          k2- (m/dec k2)
                          k42 (m/+ (m/* 4 k) 2)
                          a+b+ (m/* (m/inc a) (m/inc b))
                          a0 (m/* (m/+ a k+) (m/+ b k+))
                          a2 (m/* z2 (m// (m/* (m/inc (m/- a k)) (m/inc (m/- b k)) k2+) k2-))
                          b0 (m/* k42 (m/+ c k+))
                          b1 (m/* k42 (m/- k c 1.0))
                          t0 (m/- b0 (m/* x (m// (m/* (m/- (m/* k (m/+ a b k3)) a+b+) k2+) k2-)))
                          t1 (m/- b1 (m/* x (m/- (m/* k (m/- k3 a b)) a+b+)))
                          nuhi (m// (m/+ t0 (m/* (m/- t1 (m/* a2 umid)) x uhi)))
                          nrhi (m/* nuhi (m/+ (m/* t0 rhi)
                                              (m/* (m/- (m/* t1 rmid) (m/* a2 rlo umid)) x uhi)))]
                      (if (m/< (m/abs a0) (m/ulp (m/* (m/+ absa k+) (m/+ absb k+))))
                        nrhi
                        (recur (m/inc k) (m/* a0 nuhi) uhi nrhi rhi rmid)))
                    (cond
                      (m/valid-double? rhi) rhi
                      (m/valid-double? rmid) rmid
                      :else rlo)))))))))))

(defn general-2F1
  ^double [^double a ^double b ^double c ^double x]
  (cond
    (m/one? x) (let [v (m/- c a b)]
                 (cond
                   (m/pos? v) (m/exp (m/+ (m/- (Gamma/logGamma c)
                                               (Gamma/logGamma (m/- c a))
                                               (Gamma/logGamma (m/- c b)))
                                          (Gamma/logGamma v)))
                   (m/zero? v) (let [f (m/- (Gamma/logGamma c)
                                            (Gamma/logGamma a)
                                            (Gamma/logGamma b))]
                                 (if (m/valid-double? f) ##Inf ##NaN))
                   :else (let [f (m/+ (m/- (Gamma/logGamma c)
                                           (Gamma/logGamma a)
                                           (Gamma/logGamma b))
                                      (Gamma/logGamma (m/- v)))]
                           (if (m/valid-double? f) ##Inf ##NaN))))
    (and (m/== (m/rint a) a) (m/not-pos? a)
         (m/== (m/rint b) b) (m/not-pos? b)
         (m/< (m/abs x) 0.72)) (maclaurin-2F1 a b c x)
    (m/neg? (m/- c a b)) (m/* (m/exp (m/* (m/- c a b) (m/log1p (m/- x))))
                              (general-2F1 (m/- c a) (m/- c b) c x))
    (m/<= (m/abs (m// x (m/dec x))) 0.72) (m/* (m/exp (m/* (m/- a) (m/log1p (m/- x))))
                                               (maclaurin-2F1 a (m/- c b) c (m// x (m/dec x))))
    (m/<= (m/abs (m// x)) 0.72) (inf-2F1 a b c x)
    (m/<= (m/abs (m/- 1.0 (m// x))) 0.72) (m/* (m/exp (m/* (m/- a) (m/log1p (m/- x))))
                                               (inf-2F1 a (m/- c b) c (m// x (m/dec x))))
    (m/<= (m/abs (m/- 1.0 x)) 0.72) (one-2F1 a b c x)
    (m/<= (m/abs (m/- 1.0 (m/- 1.0 x))) 0.72) (m/* (m/exp (m/* (m/- a) (m/log1p (m/- x))))
                                                   (one-2F1 a (m/- c b) c (m// x (m/dec x))))
    :else (weniger-2F1 a b c x)))

(general-2F1 1.1 2.2 4.1 0.9)


(defn hypergeometric-2F1
  "Gauss's hypergeometric ₂F₁ function."
  ^double [^double a ^double b ^double c ^double x]
  (cond
    (m/zero? x) 1.0
    (m/< b a) (hypergeometric-2F1 b a c x)
    (m/== a c) (m/exp (m/* -1.0 b (m/log1p (m/- x))))
    (m/== b c) (m/exp (m/* -1.0 a (m/log1p (m/- x))))
    (m/== c 0.5) (let [a+b (m/+ a b)]
                   (cond
                     (m/zero? a+b) (cosnasinsqrt (m/* 2.0 b) x)
                     (m/one? a+b) (m/* (cosnasinsqrt (m/- 1.0 (m/* 2.0 b)) x)
                                       (m/exp (m/* -0.5 (m/log1p (m/- x)))))
                     (m/== (m/- b a) 0.5) (expnlog1pcoshatanhsqrt (m/* -2.0 a) x)
                     :else (general-2F1 a b c x)))
    (m/== c 1.5) (cond
                   (m/== a b 0.5) (sqrtasinsqrt x)
                   (m/== a b 1.0) (m/* (sqrtasinsqrt x)
                                       (m/exp (m/* -0.5 (m/log1p (m/- x)))))
                   (and (m/== a 0.5)
                        (m/one? b)) (sqrtatanhsqrt x)
                   (m/one? (m/+ a b)) (sinnasinsqrt (m/- 1.0 (m/* 2.0 b)) x)
                   (m/== (m/+ a b) 2.0) (m/* (sinnasinsqrt (m/- 2.0 (m/* 2.0 b)) x)
                                             (m/exp (m/* -0.5 (m/log1p (m/- x)))))
                   (m/== (m/- b a) 0.5) (expnlog1pcoshatanhsqrt (m/- 1.0 (m/* -2.0 a)) x)
                   :else (general-2F1 a b c x))
    (m/== c 2.0) (cond
                   (m/== a b 1.0) (log1pover (m/- x))
                   (and (m/one? b) (m/== (m/rint a) a)) (expm1nlog1p (m/- 1.0 a) (m/- x))
                   (and (m/one? a) (m/== (m/rint b) b)) (expm1nlog1p (m/- 1.0 b) (m/- x))
                   :else (general-2F1 a b c x))
    (and (m/== c 4.0)
         (m/== a b 2.0)) (if (m/> (m/abs x) 0.2)
                           (m// (m/* 6.0 (m/+ (m/* -2.0 x)
                                              (m/* (m/- x 2.0) (m/log1p (m/- x))))) (m/* x x x))
                           (poly/mevalpoly x 1.0, 1.0, 0.9, 0.8, 0.7142857142857143, 0.6428571428571429, 0.5833333333333334, 0.5333333333333333, 0.4909090909090909, 0.45454545454545453, 0.4230769230769231, 0.3956043956043956, 0.37142857142857144, 0.35, 0.33088235294117646, 0.3137254901960784, 0.2982456140350877, 0.28421052631578947, 0.2714285714285714, 0.2597402597402597))
    (and (m/== c 2.5)
         (m/one? a)
         (m/== b 1.5)) (cond
                         (m/> x 0.2) (let [s (m/sqrt x)]
                                       (m// (m/* 3.0 (m/- (m/atanh s) s))
                                            (m/* s s s)))
                         (m/< x -0.2) (let [s (m/sqrt (m/- x))]
                                        (m// (m/* 3.0 (m/- s (m/atan s)))
                                             (m/* s s s)))
                         :else (spoly/clenshaw-chebyshev (m/* 5.0 x) spoly/hg-2F1-poly))
    :else (general-2F1 a b c x)))

;;

(defn weniger-2F0
  ^double [^double a ^double b ^double x]
  (let [absa (m/abs a)
        absb (m/abs b)
        ab (m/* a b)]
    (if (or (m/< (m/abs x) m/MACHINE-EPSILON)
            (m/< (m/abs ab) (m/ulp (m/* absa absb))))
      1.0
      (let [ulo ab
            rlo 1.0
            a0 (m/* (m/inc a) (m/inc b))
            umid (m// (m/- 2.0 (m/* a0 x)))
            rmid (m/+ rlo (m/* 2.0 x umid ulo))]
        (if (m/< (m/abs a0) (m/ulp (m/* (m/inc absa) (m/inc absb))))
          rmid
          (let [umid (m/* umid a0)
                a0 (m/* (m/+ a 2.0) (m/+ b 2.0))
                t0 (m/- 6.0 (m/* x (m/- 6.0 (m/* 3.0 ab))))
                t1 (m/- 6.0 (m/* x (m/* 2.0 (m/+ (m/* 2.0 ab) a b -1.0))))
                uhi (m// (m/- t0 (m/* t1 x umid)))
                rhi (m/* uhi (m/- (m/* t0 rmid)
                                  (m/* x umid (m/+ (m/* t1 rlo)
                                                   (m/* 6.0 x ulo)))))]
            (if (m/< (m/abs a0) (m/ulp (m/* (m/+ absa 2.0) (m/+ absb 2.0))))
              rhi
              (let [uhi (m/* uhi a0)
                    z2 (m/* x x)]
                (loop [k (long 2)
                       uhi uhi umid umid
                       rhi rhi rmid rmid rlo rlo]
                  (if (or (m/< k 3)                          
                          (and (m/< k 1048576) (m/> (m/abs (m/- rmid rhi)) (m/* m/MACHINE-EPSILON10
                                                                                (m/max (m/abs rmid)
                                                                                       (m/abs rhi))))))
                    (let [k+ (m/inc k)
                          k2 (m/* 2 k)
                          k3 (m/* 3 k)
                          k2+ (m/inc k2)
                          k2- (m/dec k2)
                          k42 (m/+ (m/* 4 k) 2)
                          a+b+ (m/* (m/inc a) (m/inc b))
                          a0 (m/* (m/+ a k+) (m/+ b k+))
                          a2 (m/* (m// (m/* (m/- (m/inc a) k) (m/- (m/inc b) k) k2+) k2-) z2)
                          t0 (m/- k42 (m/* (m// (m/* (m/- (m/* k (m/+ a b k3)) a+b+) k2+) k2-) x))
                          t1 (m/+ k42 (m/* (m/- (m/* k (m/- k3 a b)) a+b+) x))
                          nuhi (m// (m/- t0 (m/* (m/+ t1 (m/* a2 umid)) x uhi)))
                          nrhi (m/* nuhi (m/- (m/* t0 rhi)
                                              (m/* (m/+ (m/* t1 rmid)
                                                        (m/* a2 rlo umid)) x uhi)))]
                      (if (m/< (m/abs a0) (m/ulp (m/* (m/+ absa k+) (m/+ absb k+))))
                        nrhi
                        (recur k+ (m/* nuhi a0) uhi nrhi rhi rmid)))
                    (cond
                      (m/valid-double? rhi) rhi
                      (m/valid-double? rmid) rmid
                      :else rlo)))))))))))

(defn weniger-0F2
  ^double [^double a ^double b ^double x]
  (if (m/< (m/abs x) m/MACHINE-EPSILON)
    1.0
    (let [zeta (m// x)
          nlo (m/* a b zeta)
          dlo nlo
          tlo 1.0
          b0 (m/* 2.0 (m/inc a) (m/inc b))
          v (m/* b0 zeta)
          dmid2 (m/* (m/dec v) dlo)
          nmid2 (m/+ dmid2 v)
          tmid2 (m// nmid2 dmid2)
          b0 (m/* 6.0 (m/+ a 2.0) (m/+ b 2.0))
          b1 (m/* 6.0 (m/+ a b 3.0))
          t0 (m/* b0 zeta)
          t1 (m/inc (m/* b1 zeta))
          nmid1 (m/+ (m/* t0 nmid2) (m/* t1 nlo) (m/* b1 zeta))
          dmid1 (m/+ (m/* t0 dmid2) (m/* t1 dlo))
          tmid1 (m// nmid1 dmid1)
          b0 (m/* 10.0 (m/+ a 3.0) (m/+ b 3.0))
          b1 (m/* -10.0 (m/- (m/* (m/dec a) b) a 11.0))
          t0 (m/+ (m/* b0 zeta) 1.6666666666666667)
          t1 (m/inc (m/* b1 zeta))
          z40 (m/* 40.0 zeta)
          t2 (m/- z40 1.6666666666666667)
          nhi (m/+ (m/* t0 nmid1) (m/* t1 nmid2) (m/* t2 nlo) z40)
          dhi (m/+ (m/* t0 dmid1) (m/* t1 dmid2) (m/* t2 dlo))
          thi (m// nhi dhi)]
      (loop [k (long 3)
             nhi nhi nmid1 nmid1 nmid2 nmid2 nlo nlo
             dhi dhi dmid1 dmid1 dmid2 dmid2 dlo dlo
             thi thi tmid1 tmid1 tmid2 tmid2 tlo tlo]
        (if (or (m/< k 6)                          
                (and (m/< k 1048576) (m/> (m/abs (m/- tmid1 thi)) (m/* m/MACHINE-EPSILON10
                                                                       (m/max (m/abs tmid1)
                                                                              (m/abs thi))))))
          (let [k- (m/dec k)
                k+ (m/inc k)
                k2+ (m/inc (m/* 2 k))
                k2- (m/dec (m/* 2 k))
                k42 (m/+ (m/* 4 k) 2)
                k-k23 (m/* k- (m/- (m/* 2 k) 3))
                den (m// (double k2+) k-)
                a3 (m// (m/* -1.0 k k2+) k-k23)
                b0 (m/* k42 (m/+ a k+) (m/+ b k+))
                b1 (m// (m/* (m/- (m/* k k-) (m/* (m/inc a) (m/inc b))) k2- k42) k-)
                b2 (m// (m/* (m/- k 2 a) (m/- k 2 b) k42 k) k-)
                t0 (m/+ (m/* b0 zeta) den)
                t1 (m/- (m/* b1 zeta) (m// (m/* 3.0 k2-) k-k23))
                t2 (m/- (m/* b2 zeta) den)
                nnhi (m/- (m/+ (m/* t0 nhi) (m/* t1 nmid1) (m/* t2 nmid2)) (m/* a3 nlo))
                ndhi (m/- (m/+ (m/* t0 dhi) (m/* t1 dmid1) (m/* t2 dmid2)) (m/* a3 dlo))]
            (recur k+
                   nnhi nhi nmid1 nmid2
                   ndhi dhi dmid1 dmid2
                   (m// nnhi ndhi) thi tmid1 tmid2))
          (cond
            (m/valid-double? thi) thi
            (m/valid-double? tmid1) tmid1
            (m/valid-double? tmid2) tmid2
            :else tlo))))))

(defn maclaurin-0F2
  ^double [^double a ^double b ^double x]
  (loop [j (long 1)
         s0 1.0
         s1 (m/inc (m// (m// x a) b))]
    (if (or (m/delta-eq s0 s1 m/MACHINE-EPSILON10 m/MACHINE-EPSILON10)
            (m/== j 1048576))
      s1
      (let [rj (m// (m// (m// x (m/inc j)) (m/+ a j)) (m/+ b j))]
        (recur (m/inc j) s1 (m/+ s1 (m/* (m/- s1 s0) rj)))))))
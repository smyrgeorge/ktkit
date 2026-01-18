@file:Suppress("unused")

package io.github.smyrgeorge.ktkit.util

//@formatter:off
data class Tuple2<T1, T2>(val r1: T1, val r2: T2)
data class Tuple3<T1, T2, T3>(val r1: T1, val r2: T2, val r3: T3)
data class Tuple4<T1, T2, T3, T4>(val r1: T1, val r2: T2, val r3: T3, val r4: T4)
data class Tuple5<T1, T2, T3, T4, T5>(val r1: T1, val r2: T2, val r3: T3, val r4: T4, val r5: T5)
data class Tuple6<T1, T2, T3, T4, T5, T6>(val r1: T1, val r2: T2, val r3: T3, val r4: T4, val r5: T5, val r6: T6)
data class Tuple7<T1, T2, T3, T4, T5, T6, T7>(val r1: T1, val r2: T2, val r3: T3, val r4: T4, val r5: T5, val r6: T6, val r7: T7)
data class Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>(val r1: T1, val r2: T2, val r3: T3, val r4: T4, val r5: T5, val r6: T6, val r7: T7, val r8: T8)
data class Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>(val r1: T1, val r2: T2, val r3: T3, val r4: T4, val r5: T5, val r6: T6, val r7: T7, val r8: T8, val r9: T9)
data class Tuple10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>(val r1: T1, val r2: T2, val r3: T3, val r4: T4, val r5: T5, val r6: T6, val r7: T7, val r8: T8, val r9: T9, val r10: T10)
data class Tuple11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>(val r1: T1, val r2: T2, val r3: T3, val r4: T4, val r5: T5, val r6: T6, val r7: T7, val r8: T8, val r9: T9, val r10: T10, val r11: T11)
data class Tuple12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(val r1: T1, val r2: T2, val r3: T3, val r4: T4, val r5: T5, val r6: T6, val r7: T7, val r8: T8, val r9: T9, val r10: T10, val r11: T11, val r12: T12)
data class Tuple13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(val r1: T1, val r2: T2, val r3: T3, val r4: T4, val r5: T5, val r6: T6, val r7: T7, val r8: T8, val r9: T9, val r10: T10, val r11: T11, val r12: T12, val r13: T13)
data class Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val r1: T1, val r2: T2, val r3: T3, val r4: T4, val r5: T5, val r6: T6, val r7: T7, val r8: T8, val r9: T9, val r10: T10, val r11: T11, val r12: T12, val r13: T13, val r14: T14)
data class Tuple15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val r1: T1, val r2: T2, val r3: T3, val r4: T4, val r5: T5, val r6: T6, val r7: T7, val r8: T8, val r9: T9, val r10: T10, val r11: T11, val r12: T12, val r13: T13, val r14: T14, val r15: T15)
data class Tuple16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val r1: T1, val r2: T2, val r3: T3, val r4: T4, val r5: T5, val r6: T6, val r7: T7, val r8: T8, val r9: T9, val r10: T10, val r11: T11, val r12: T12, val r13: T13, val r14: T14, val r15: T15, val r16: T16)
data class Tuple17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val r1: T1, val r2: T2, val r3: T3, val r4: T4, val r5: T5, val r6: T6, val r7: T7, val r8: T8, val r9: T9, val r10: T10, val r11: T11, val r12: T12, val r13: T13, val r14: T14, val r15: T15, val r16: T16, val r17: T17)
data class Tuple18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val r1: T1, val r2: T2, val r3: T3, val r4: T4, val r5: T5, val r6: T6, val r7: T7, val r8: T8, val r9: T9, val r10: T10, val r11: T11, val r12: T12, val r13: T13, val r14: T14, val r15: T15, val r16: T16, val r17: T17, val r18: T18)
data class Tuple19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val r1: T1, val r2: T2, val r3: T3, val r4: T4, val r5: T5, val r6: T6, val r7: T7, val r8: T8, val r9: T9, val r10: T10, val r11: T11, val r12: T12, val r13: T13, val r14: T14, val r15: T15, val r16: T16, val r17: T17, val r18: T18, val r19: T19)
data class Tuple20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val r1: T1, val r2: T2, val r3: T3, val r4: T4, val r5: T5, val r6: T6, val r7: T7, val r8: T8, val r9: T9, val r10: T10, val r11: T11, val r12: T12, val r13: T13, val r14: T14, val r15: T15, val r16: T16, val r17: T17, val r18: T18, val r19: T19, val r20: T20)
//@formatter:on
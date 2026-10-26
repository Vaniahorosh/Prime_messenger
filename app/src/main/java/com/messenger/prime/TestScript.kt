package com.messenger.prime
class TestScript { fun test() { android.animation.ValueAnimator::class.java.getMethod("setDurationScale", Float::class.javaPrimitiveType) } }
fun test2() { try { val m = android.animation.ValueAnimator::class.java.getMethod("setDurationScale", Float::class.javaPrimitiveType); m.invoke(null, 0f) } catch(e: Exception) { e.printStackTrace() } }

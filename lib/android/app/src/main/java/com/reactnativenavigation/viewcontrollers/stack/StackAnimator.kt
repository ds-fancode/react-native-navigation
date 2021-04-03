package com.reactnativenavigation.viewcontrollers.stack

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.content.Context
import android.view.View
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.core.animation.doOnEnd
import com.reactnativenavigation.options.FadeInAnimation
import com.reactnativenavigation.options.FadeOutAnimation
import com.reactnativenavigation.options.Options
import com.reactnativenavigation.options.StackAnimationOptions
import com.reactnativenavigation.options.params.Bool
import com.reactnativenavigation.utils.awaitRender
import com.reactnativenavigation.utils.resetViewProperties
import com.reactnativenavigation.viewcontrollers.common.BaseAnimator
import com.reactnativenavigation.viewcontrollers.viewcontroller.ViewController
import com.reactnativenavigation.views.element.TransitionAnimatorCreator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

open class StackAnimator @JvmOverloads constructor(
        context: Context,
        private val transitionAnimatorCreator: TransitionAnimatorCreator = TransitionAnimatorCreator()
) : BaseAnimator(context) {
    private val runningPIPAnimations: MutableMap<View, Animator> = HashMap()

    @VisibleForTesting
    val runningPushAnimations: MutableMap<ViewController<*>, AnimatorSet> = HashMap()

    @VisibleForTesting
    val runningPopAnimations: MutableMap<ViewController<*>, AnimatorSet> = HashMap()

    @VisibleForTesting
    val runningSetRootAnimations: MutableMap<ViewController<*>, AnimatorSet> = HashMap()

    fun cancelPushAnimations() = runningPushAnimations.values.forEach(Animator::cancel)

    open fun isChildInTransition(child: ViewController<*>?): Boolean {
        return runningPushAnimations.containsKey(child) ||
                runningPopAnimations.containsKey(child) ||
                runningSetRootAnimations.containsKey(child)
    }

    fun setRoot(
            appearing: ViewController<*>,
            disappearing: ViewController<*>,
            options: Options,
            additionalAnimations: List<Animator>,
            onAnimationEnd: Runnable
    ) {
        val set = createSetRootAnimator(appearing, onAnimationEnd)
        runningSetRootAnimations[appearing] = set
        val setRoot = options.animations.setStackRoot
        if (setRoot.waitForRender.isTrue) {
            appearing.view.alpha = 0f
            appearing.addOnAppearedListener {
                appearing.view.alpha = 1f
                animateSetRoot(set, setRoot, appearing, disappearing, additionalAnimations)
            }
        } else {
            animateSetRoot(set, setRoot, appearing, disappearing, additionalAnimations)
        }
    }

    fun push(
            appearing: ViewController<*>,
            disappearing: ViewController<*>,
            resolvedOptions: Options,
            additionalAnimations: List<Animator>,
            onAnimationEnd: Runnable
    ) {
        val set = createPushAnimator(appearing, onAnimationEnd)
        runningPushAnimations[appearing] = set
        if (resolvedOptions.animations.push.sharedElements.hasValue()) {
            pushWithElementTransition(appearing, disappearing, resolvedOptions, set)
        } else {
            pushWithoutElementTransitions(appearing, disappearing, resolvedOptions, set, additionalAnimations)
        }
    }

    open fun pop(
            appearing: ViewController<*>,
            disappearing: ViewController<*>,
            disappearingOptions: Options,
            additionalAnimations: List<Animator>,
            onAnimationEnd: Runnable
    ) {
        if (runningPushAnimations.containsKey(disappearing)) {
            runningPushAnimations[disappearing]!!.cancel()
            onAnimationEnd.run()
        } else {
            animatePop(
                    appearing,
                    disappearing,
                    disappearingOptions,
                    additionalAnimations,
                    onAnimationEnd
            )
        }
    }

    open fun pipIn(pipContainer: View, pip: ViewController<*>, options: Options, onAnimationEnd: Runnable?) {
        GlobalScope.launch(Dispatchers.Main.immediate) {
            val set = createPIPAnimator(pip, onAnimationEnd!!)
            runningPIPAnimations[pipContainer] = set
            if (options.animations.pipIn.elementTransitions.hasValue()) {
                pipInElementTransition(pipContainer, pip, options, set, object : TransitionAnimatorCreator.CreatorResultCallback() {
                    override fun onError() {
                        pipInWithoutElementTransitions(pipContainer, pip, options, set)
                    }
                })
            } else {
                pipInWithoutElementTransitions(pipContainer, pip, options, set)
            }
        }
    }

    private fun animatePop(
            appearing: ViewController<*>,
            disappearing: ViewController<*>,
            disappearingOptions: Options,
            additionalAnimations: List<Animator>,
            onAnimationEnd: Runnable
    ) {
        GlobalScope.launch(Dispatchers.Main.immediate) {
            val set = createPopAnimator(disappearing, onAnimationEnd)
            if (disappearingOptions.animations.pop.sharedElements.hasValue()) {
                popWithElementTransitions(appearing, disappearing, disappearingOptions, set)
            } else {
                popWithoutElementTransitions(appearing, disappearing, disappearingOptions, set, additionalAnimations)
            }
        }
    }

    private suspend fun popWithElementTransitions(appearing: ViewController<*>, disappearing: ViewController<*>, resolvedOptions: Options, set: AnimatorSet) {
        val pop = resolvedOptions.animations.pop
        val fade = if (pop.content.exit.isFadeAnimation()) pop else FadeOutAnimation()
        val transitionAnimators = transitionAnimatorCreator.create(pop, fade.content.exit, disappearing, appearing)
        set.playTogether(fade.content.exit.getAnimation(disappearing.view), transitionAnimators)
        transitionAnimators.listeners.forEach { listener: Animator.AnimatorListener -> set.addListener(listener) }
        transitionAnimators.removeAllListeners()
        set.start()
    }

    private fun popWithoutElementTransitions(
            appearing: ViewController<*>,
            disappearing: ViewController<*>,
            disappearingOptions: Options,
            set: AnimatorSet,
            additionalAnimations: List<Animator>
    ) {
        val pop = disappearingOptions.animations.pop
        val animators = mutableListOf(pop.content.exit.getAnimation(disappearing.view, getDefaultPopAnimation(disappearing.view)))
        animators.addAll(additionalAnimations)
        if (pop.content.enter.hasValue()) {
            animators.add(pop.content.enter.getAnimation(appearing.view))
        }

        set.playTogether(animators.toList())
        set.start()
    }

    private fun createPopAnimator(disappearing: ViewController<*>, onAnimationEnd: Runnable): AnimatorSet {
        val set = createAnimatorSet()
        runningPopAnimations[disappearing] = set
        set.addListener(object : AnimatorListenerAdapter() {
            private var cancelled = false
            override fun onAnimationCancel(animation: Animator) {
                cancelled = true
                runningPopAnimations.remove(disappearing)
            }

            override fun onAnimationEnd(animation: Animator) {
                runningPopAnimations.remove(disappearing)
                if (!cancelled) onAnimationEnd.run()
            }
        })
        return set
    }

    private fun createPushAnimator(appearing: ViewController<*>, onAnimationEnd: Runnable): AnimatorSet {
        val set = createAnimatorSet()
        set.addListener(object : AnimatorListenerAdapter() {
            private var isCancelled = false
            override fun onAnimationCancel(animation: Animator) {
                isCancelled = true
                runningPushAnimations.remove(appearing)
                onAnimationEnd.run()
            }

            override fun onAnimationEnd(animation: Animator) {
                if (!isCancelled) {
                    runningPushAnimations.remove(appearing)
                    onAnimationEnd.run()
                }
            }
        })
        return set
    }

    private fun createSetRootAnimator(appearing: ViewController<*>, onAnimationEnd: Runnable): AnimatorSet {
        val set = createAnimatorSet()
        set.addListener(object : AnimatorListenerAdapter() {
            private var isCancelled = false
            override fun onAnimationCancel(animation: Animator) {
                isCancelled = true
                runningSetRootAnimations.remove(appearing)
                onAnimationEnd.run()
            }

            override fun onAnimationEnd(animation: Animator) {
                if (!isCancelled) {
                    runningSetRootAnimations.remove(appearing)
                    onAnimationEnd.run()
                }
            }
        })
        return set
    }

    private fun createPIPAnimator(appearing: ViewController<*>, onAnimationEnd: Runnable): AnimatorSet {
        val set = AnimatorSet()
        set.addListener(object : AnimatorListenerAdapter() {
            private var isCancelled = false
            override fun onAnimationCancel(animation: Animator) {
                isCancelled = true
                if (!appearing.isDestroyed) {
                    runningPIPAnimations.remove(appearing.view)
                }
                onAnimationEnd.run()
            }

            override fun onAnimationEnd(animation: Animator) {
                if (!isCancelled) {
                    if (!appearing.isDestroyed) {
                        runningPIPAnimations.remove(appearing.view)
                    }
                    onAnimationEnd.run()
                }
            }
        })
        return set
    }

    private fun pushWithElementTransition(
            appearing: ViewController<*>,
            disappearing: ViewController<*>,
            options: Options,
            set: AnimatorSet
    ) = GlobalScope.launch(Dispatchers.Main.immediate) {
        appearing.setWaitForRender(Bool(true))
        appearing.view.alpha = 0f
        appearing.awaitRender()
        val fade = if (options.animations.push.content.enter.isFadeAnimation()) options.animations.push.content.enter else FadeInAnimation().content.enter
        val transitionAnimators = transitionAnimatorCreator.create(options.animations.push, fade, disappearing, appearing)
        set.playTogether(fade.getAnimation(appearing.view), transitionAnimators)
        transitionAnimators.listeners.forEach { listener: Animator.AnimatorListener -> set.addListener(listener) }
        transitionAnimators.removeAllListeners()
        set.start()
    }

    private fun pushWithoutElementTransitions(
            appearing: ViewController<*>,
            disappearing: ViewController<*>,
            resolvedOptions: Options,
            set: AnimatorSet,
            additionalAnimations: List<Animator>
    ) {
        val push = resolvedOptions.animations.push
        if (push.waitForRender.isTrue) {
            appearing.view.alpha = 0f
            appearing.addOnAppearedListener {
                appearing.view.alpha = 1f
                animatePushWithoutElementTransitions(set, push, appearing, disappearing, additionalAnimations)
            }
        } else {
            animatePushWithoutElementTransitions(set, push, appearing, disappearing, additionalAnimations)
        }
    }

    private fun animatePushWithoutElementTransitions(
            set: AnimatorSet,
            push: StackAnimationOptions,
            appearing: ViewController<*>,
            disappearing: ViewController<*>,
            additionalAnimations: List<Animator>
    ) {
        val animators = mutableListOf(push.content.enter.getAnimation(appearing.view, getDefaultPushAnimation(appearing.view)))
        animators.addAll(additionalAnimations)
        if (push.content.exit.hasValue()) {
            animators.add(push.content.exit.getAnimation(disappearing.view))
        }
        set.playTogether(animators.toList())
        set.doOnEnd {
            if (!disappearing.isDestroyed) disappearing.view.resetViewProperties()
        }
        set.start()
    }

    private suspend fun pipInElementTransition(pipContainer: View, pipIn: ViewController<*>, options: Options, set: AnimatorSet, callback: TransitionAnimatorCreator.CreatorResultCallback) {
        val fade = if (options.animations.pipIn.enter.isFadeAnimation()) options.animations.pipIn.enter else FadeInAnimation().content.enter
        transitionAnimatorCreator.createPIPTransitions(
                options.animations.pipIn,
                fade,
                pipContainer,
                pipIn,
                object : TransitionAnimatorCreator.CreatorResultCallback(callback) {
                    override fun onSuccess(transitionAnimators: AnimatorSet) {
                        set.playTogether(options.animations.pipIn.enter.getAnimation(pipContainer, getDefaultPopAnimation(pipContainer)), transitionAnimators)
                        transitionAnimators.listeners.forEach { listener: Animator.AnimatorListener -> set.addListener(listener) }
                        transitionAnimators.removeAllListeners()
                        set.start()
                    }
                }
        )
    }

    private fun pipInWithoutElementTransitions(pipContainer: View, appearing: ViewController<*>, options: Options, set: AnimatorSet) {
        if (options.animations.pipIn.enter.waitForRender.isTrue) {
            appearing.view.alpha = 0f
            appearing.addOnAppearedListener {
                appearing.view.alpha = 1f
                set.playTogether(options.animations.pipIn.enter.getAnimation(pipContainer, getDefaultPopAnimation(pipContainer)))
                set.start()
            }
        } else {
            set.playTogether(options.animations.pipIn.enter.getAnimation(pipContainer, getDefaultPopAnimation(pipContainer)))
            set.start()
        }
    }

    open fun pipOut(pipContainer: View, pip: ViewController<*>, options: Options, onAnimationEnd: Runnable?) {
        GlobalScope.launch(Dispatchers.Main.immediate) {
            val set = createPIPAnimator(pip, onAnimationEnd!!)
            runningPIPAnimations[pipContainer] = set
            if (options.animations.pipOut.elementTransitions.hasValue()) {
                pipOutElementTransition(pipContainer, pip, options, set, object : TransitionAnimatorCreator.CreatorResultCallback() {
                    override fun onError() {
                        pipOutWithoutElementTransitions(pipContainer, pip, options, set)
                    }
                })
            } else {
                pipOutWithoutElementTransitions(pipContainer, pip, options, set)
            }
        }
    }

    private fun pipOutWithoutElementTransitions(pipContainer: View, appearing: ViewController<*>, options: Options, set: AnimatorSet) {
        if (options.animations.pipOut.exit.waitForRender.isTrue) {
            appearing.view.alpha = 0f
            appearing.addOnAppearedListener {
                appearing.view.alpha = 1f
                set.playTogether(options.animations.pipOut.exit.getAnimation(pipContainer, getDefaultPushAnimation(pipContainer)))
                set.start()
            }
        } else {
            set.playTogether(options.animations.pipOut.exit.getAnimation(pipContainer, getDefaultPushAnimation(pipContainer)))
            set.start()
        }
    }

    private suspend fun pipOutElementTransition(pipContainer: View, pipOut: ViewController<*>, options: Options, set: AnimatorSet, callback: TransitionAnimatorCreator.CreatorResultCallback) {
        val fade = if (options.animations.pipOut.exit.isFadeAnimation()) options.animations.pipOut.exit else FadeOutAnimation().content.exit
        transitionAnimatorCreator.createPIPOutTransitions(
                options.animations.pipOut,
                fade,
                pipContainer,
                pipOut,
                object : TransitionAnimatorCreator.CreatorResultCallback(callback) {
                    override fun onSuccess(transitionAnimators: AnimatorSet) {
                        set.playTogether(options.animations.pipOut.exit.getAnimation(pipContainer, getDefaultPopAnimation(pipContainer)), transitionAnimators)
                        transitionAnimators.listeners.forEach { listener: Animator.AnimatorListener -> set.addListener(listener) }
                        transitionAnimators.removeAllListeners()
                        set.start()
                    }
                }
        )
    }

    private fun animateSetRoot(
            set: AnimatorSet,
            setRoot: StackAnimationOptions,
            appearing: ViewController<*>,
            disappearing: ViewController<*>,
            additionalAnimations: List<Animator>
    ) {
        val animators = mutableListOf(setRoot.content.enter.getAnimation(
                appearing.view,
                getDefaultSetStackRootAnimation(appearing.view)
        ))
        animators.addAll(additionalAnimations)
        if (setRoot.content.exit.hasValue()) {
            animators.add(setRoot.content.exit.getAnimation(disappearing.view))
        }
        set.playTogether(animators.toList())
        set.start()
    }

    protected open fun createAnimatorSet(): AnimatorSet = AnimatorSet()

    @RestrictTo(RestrictTo.Scope.TESTS)
    fun endPushAnimation(view: ViewController<*>) {
        if (runningPushAnimations.containsKey(view)) {
            runningPushAnimations[view]!!.end()
        }
    }
}
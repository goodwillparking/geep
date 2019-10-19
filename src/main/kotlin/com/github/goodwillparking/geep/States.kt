package com.github.goodwillparking.geep

import java.time.Duration

typealias Receive = PartialFunction<Any, out Next>

typealias ChildReceive = PartialFunction<Any, out AbsoluteNext>

typealias ReceiveBuilder = PFBuilder<Any, Next>

typealias ChildReceiveBuilder = PFBuilder<Any, AbsoluteNext>

interface State {

    val receive: Receive

    fun onStart(): Next {
        return Stay()
    }

    fun onEnd() {}

    // TODO: this should take the previously focused state as a param and vice versa.
    fun onFocusGained(): Next {
        return Stay()
    }

    fun onFocusLost() {}
}

// TODO: generic type???
interface ParentState : State {
    val childState: ChildState
}

// TODO: This parent/child naming is a bit counter intuitive if viewed from the perspective of a tree.
//  FallbackState?
// TODO: focus handlers don't make sense for child states
interface ChildState : State {
    override val receive: ChildReceive

    override fun onStart(): AbsoluteNext {
        return Stay()
    }

    override fun onFocusGained(): AbsoluteNext {
        return Stay()
    }
}

sealed class Next

sealed class RelativeNext : Next()

sealed class AbsoluteNext : Next()

interface AsyncNext {
    val asyncUpdate: AsyncUpdate?

    fun withAsync(asyncUpdate: AsyncUpdate?): AsyncNext

    fun async(asyncTasks: Iterable<AsyncTask>): AsyncNext {
        val async = asyncTasks.fold(asyncUpdate) { acc, update ->
            val async = acc ?: AsyncUpdate()
            when (update) {
                is TimerUpdate -> async.withTimerUpdate(update)
                is ExecuteAsync -> async.withAsyncExecution(update)
            }
        }
        return withAsync(async)
    }

    fun async(vararg asyncTasks: AsyncTask): AsyncNext = async(asyncTasks.toList())
}

// TODO: private constructors an no data classes. Maybe not, probably is fine to expose all properties
data class Stay constructor(override val asyncUpdate: AsyncUpdate? = null) : AbsoluteNext(), AsyncNext {

    override fun withAsync(asyncUpdate: AsyncUpdate?) = copy(asyncUpdate = asyncUpdate)

    override fun async(asyncTasks: Iterable<AsyncTask>) = super.async(asyncTasks) as Stay

    override fun async(vararg asyncTasks: AsyncTask) = async(asyncTasks.toList())
}

data class Goto(val state: State) : RelativeNext()

data class Start(
    val state: State,
    val position: RelativePosition = RelativePosition.Above,
    override val asyncUpdate: AsyncUpdate? = null
) : RelativeNext(), AsyncNext {

    override fun withAsync(asyncUpdate: AsyncUpdate?) = copy(asyncUpdate = asyncUpdate)

    override fun async(asyncTasks: Iterable<AsyncTask>) = super.async(asyncTasks) as Start

    override fun async(vararg asyncTasks: AsyncTask) = async(asyncTasks.toList())
}

data class AbsoluteStart(
    val state: State,
    val position: AbsolutePosition = AbsolutePosition.Top,
    override val asyncUpdate: AsyncUpdate? = null
) : AbsoluteNext(), AsyncNext {

    override fun withAsync(asyncUpdate: AsyncUpdate?) = copy(asyncUpdate = asyncUpdate)

    override fun async(asyncTasks: Iterable<AsyncTask>) = super.async(asyncTasks) as AbsoluteStart

    override fun async(vararg asyncTasks: AsyncTask) = async(asyncTasks.toList())
}

object Done : RelativeNext()

// TODO: nullable state for clear?
data class Clear constructor(
    val state: State,
    val range: RelativeRange =  RelativeRange.Below(true),
    override val asyncUpdate: AsyncUpdate? = null
) : RelativeNext(), AsyncNext {

    override fun withAsync(asyncUpdate: AsyncUpdate?) = copy(asyncUpdate = asyncUpdate)

    override fun async(asyncTasks: Iterable<AsyncTask>) = super.async(asyncTasks) as Clear

    override fun async(vararg asyncTasks: AsyncTask) = async(asyncTasks.toList())
}

data class AbsoluteClear(val state: State) : AbsoluteNext()

sealed class AbsolutePosition {
    object Top : AbsolutePosition()
    object Bottom : AbsolutePosition()
}

sealed class RelativePosition {
    object Above : RelativePosition()
    object Below : RelativePosition()
}

sealed class RelativeRange(val inclusive: Boolean) {
    class Above(inclusive: Boolean) : RelativeRange(inclusive)
    class Below(inclusive: Boolean) : RelativeRange(inclusive)
}

data class AsyncUpdate(
    val timerUpdates: Map<Any, TimerUpdate> = emptyMap(),
    val asyncExecutions: List<ExecuteAsync> = emptyList()
) {
    fun withTimerUpdate(timerUpdate: TimerUpdate) = copy(timerUpdates = timerUpdates + (timerUpdate.key to timerUpdate))

    fun withAsyncExecution(executeAsync: ExecuteAsync) = copy(asyncExecutions = asyncExecutions + executeAsync)
}

sealed class AsyncTask

sealed class TimerUpdate : AsyncTask() {
    abstract val key: Any
}

data class CancelTimer(override val key: Any) : TimerUpdate()

// TODO: Global vs local timers? Maybe not. Client could always use some base child state to serve as the global state.
interface SetTimer {
    val key: Any
    val duration: Duration
    val message: Any
    val passiveSet: Boolean
}

data class SetSingleTimer(
    override val key: Any,
    override val duration: Duration,
    override val message: Any,
    override val passiveSet: Boolean = false
) : TimerUpdate(), SetTimer

data class SetPeriodicTimer(
    override val key: Any,
    override val duration: Duration,
    override val message: Any,
    override val passiveSet: Boolean = false,
    val initialDelay: Duration = Duration.ZERO
) : TimerUpdate(), SetTimer

// TODO: global async execution
data class ExecuteAsync(val task: () -> Any, val failureMapper: (Throwable) -> Any) : AsyncTask() {
    constructor(task: () -> Any) : this(task, { Failure(it) })

    fun onFailure(failureMapper: (Throwable) -> Any) = copy(failureMapper = failureMapper)
}

data class Failure(val cause: Throwable)

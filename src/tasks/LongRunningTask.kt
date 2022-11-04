package tasks

import gui.UpdateDialog

abstract class LongRunningTask() {
    abstract fun execute()
    abstract fun cancel()
}
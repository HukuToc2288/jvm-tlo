package utils

import java.sql.ResultSet

// work with DB results as Iterable
abstract class CachingIterator<T>(private val resultSet: ResultSet) : Iterator<T> {
    var cachedHasNext = false
    override fun hasNext(): Boolean {
        if (!cachedHasNext)
            cachedHasNext = resultSet.next()
        return cachedHasNext
    }

    override fun next(): T {
        if (!cachedHasNext)
            resultSet.next()
        cachedHasNext = false
        return processResult(resultSet)
    }

    abstract fun processResult(resultSet: ResultSet): T
}
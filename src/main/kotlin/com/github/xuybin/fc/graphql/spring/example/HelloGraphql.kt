package com.github.xuybin.example

import com.github.xuybin.fc.graphql.*
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class HelloGraphql : GSchema(), GQuery, GMutation {
    override fun resolverMap(): Map<String, (Context) -> Any?> {
        return mapOf(
            ::hello.name to { gcontext: Context ->
                hello(gcontext[0])
            },
            ::newUserId.name to { gcontext: Context ->
                newUserId()
            }
        )
    }

    val userIdSet = mutableSetOf<String>()

    fun hello(userId: String): String {
        logger.debug("log->$logger hello Thread->${Thread.currentThread().id} HelloGraphql.this->${this@HelloGraphql}")
        return if (userIdSet.contains(userId)) {
            "hello $userId"
        } else {
            throw GErr.NotFound("not find userId:$userId")
        }
    }

    fun newUserId(): String {
        logger.debug("log->$logger newUserId Thread->${Thread.currentThread().id} HelloGraphql.this->${this@HelloGraphql}")
        return Random.nextInt(100, 999).toString().let {
            userIdSet.add(it)
            it
        }
    }
}